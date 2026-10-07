package com.example

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PhoneCallback
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.ui.screens.ContactsScreen
import com.example.ui.screens.DialerScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.util.DefaultDialerHelper
import com.example.util.PhoneNumberFormatter

class MainActivity : ComponentActivity() {

    private val requestPermissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        // Permissions handled
    }

    private var incomingNumberState = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val permissions = mutableListOf(
            Manifest.permission.CALL_PHONE,
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.WRITE_CONTACTS,
            Manifest.permission.READ_CALL_LOG
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            permissions.add(Manifest.permission.READ_PHONE_NUMBERS)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissions.add(Manifest.permission.BLUETOOTH_CONNECT)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        requestPermissionsLauncher.launch(permissions.toTypedArray())

        extractIncomingNumber(intent)

        val startTab = intent.getStringExtra("tab") ?: "dialer"
        setContent {
            MyApplicationTheme {
                MainScreen(
                    startTab = startTab,
                    externalDialNumber = incomingNumberState.value
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        extractIncomingNumber(intent)
    }

    private fun extractIncomingNumber(intent: Intent?) {
        if (intent == null) return
        val data = intent.data
        var extractedNumber: String? = null
        if (data != null) {
            if (data.scheme == "tel") {
                extractedNumber = data.schemeSpecificPart
            } else if (data.getQueryParameter("number") != null) {
                extractedNumber = data.getQueryParameter("number")
            }
        }
        if (extractedNumber.isNullOrBlank()) {
            extractedNumber = intent.getStringExtra("dial_number")
                ?: intent.getStringExtra("phone_number")
                ?: intent.getStringExtra("number")
                ?: intent.getStringExtra(Intent.EXTRA_PHONE_NUMBER)
        }
        if (!extractedNumber.isNullOrBlank()) {
            incomingNumberState.value = PhoneNumberFormatter.toNationalDigits(applicationContext, extractedNumber)
        }
    }

    override fun onStop() {
        super.onStop()
        val app = applicationContext as? PhonebookApplication
        app?.spamSyncRepository?.checkAndTriggerBackgroundSync()
    }
}

@Composable
fun MainScreen(
    startTab: String = "dialer",
    externalDialNumber: String? = null
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val navController = rememberNavController()

    var isDefaultDialer by remember { mutableStateOf(DefaultDialerHelper.isDefaultDialer(context)) }
    var showDefaultDialerBanner by remember { mutableStateOf(true) }

    // アプリ復帰時にデフォルトダイヤラー状態を再確認
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isDefaultDialer = DefaultDialerHelper.isDefaultDialer(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val defaultDialerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        isDefaultDialer = DefaultDialerHelper.isDefaultDialer(context)
    }

    val navItems = listOf(
        NavigationItem("ダイヤル", "dialer", Icons.Default.Dialpad),
        NavigationItem("連絡先", "contacts", Icons.Default.Contacts),
        NavigationItem("履歴", "history", Icons.Default.History),
        NavigationItem("設定", "settings", Icons.Default.Settings)
    )

    // 外部からのtel:呼び出しがあった場合、ダイヤラーに遷移
    LaunchedEffect(externalDialNumber) {
        if (!externalDialNumber.isNullOrBlank()) {
            navController.navigate("dialer?number=${Uri.encode(externalDialNumber)}") {
                popUpTo("dialer") { inclusive = true }
            }
        }
    }

    Scaffold(
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination
                navItems.forEach { item ->
                    val isSelected = currentDestination?.hierarchy?.any { 
                        it.route == item.route || it.route?.startsWith("${item.route}?") == true 
                    } == true
                    NavigationBarItem(
                        icon = { Icon(item.icon, contentDescription = item.label) },
                        label = { Text(item.label) },
                        selected = isSelected,
                        onClick = {
                            navController.navigate(item.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // デフォルト電話アプリ未設定時の案内バナー
            if (!isDefaultDialer && showDefaultDialerBanner) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhoneCallback,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "標準の電話アプリに設定",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "発信プレフィックスや安全な通話機能を使うため、デフォルトの電話アプリに設定してください",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Button(
                            onClick = {
                                val intent = DefaultDialerHelper.createRequestDefaultDialerIntent(context)
                                if (intent != null) {
                                    defaultDialerLauncher.launch(intent)
                                }
                            },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text("設定")
                        }
                        IconButton(
                            onClick = { showDefaultDialerBanner = false },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "閉じる",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                color = MaterialTheme.colorScheme.background
            ) {
                NavHost(
                    navController = navController,
                    startDestination = if (externalDialNumber != null) "dialer?number=${Uri.encode(externalDialNumber)}" else "dialer"
                ) {
                    composable("contacts") {
                        ContactsScreen(
                            onCallContact = { number ->
                                val nationalNum = com.example.util.PhoneNumberFormatter.toNationalDigits(context, number)
                                navController.navigate("dialer?number=${Uri.encode(nationalNum)}")
                            }
                        )
                    }
                    composable(
                        route = "dialer?number={number}",
                        arguments = listOf(
                            navArgument("number") {
                                type = NavType.StringType
                                nullable = true
                                defaultValue = null
                            }
                        )
                    ) { backStackEntry ->
                        val number = backStackEntry.arguments?.getString("number")
                        val nationalNum = com.example.util.PhoneNumberFormatter.toNationalDigits(context, number)
                        DialerScreen(initialPhoneNumber = nationalNum)
                    }
                    composable("dialer") {
                        DialerScreen()
                    }
                    composable("history") {
                        HistoryScreen(
                            onCallHistory = { number ->
                                val nationalNum = com.example.util.PhoneNumberFormatter.toNationalDigits(context, number)
                                navController.navigate("dialer?number=${Uri.encode(nationalNum)}")
                            }
                        )
                    }
                    composable("settings") { SettingsScreen() }
                }
            }
        }
    }
}

data class NavigationItem(
    val label: String,
    val route: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)
