package com.example.ui.components

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.widget.Toast
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * 振動フィードバック
 */
fun performHaptic(context: Context) {
    try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator?.vibrate(
                VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE)
            )
        } else {
            @Suppress("DEPRECATION")
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(
                    VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE)
                )
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(50)
            }
        }
    } catch (_: Exception) {}
}

/**
 * スライド式発信ボタン (Slide to Call)
 * ユーザーが右端までドラッグすることで発信を実行。誤タップを確実に防ぎます。
 */
@Composable
fun SlideToCallButton(
    onSlideComplete: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    label: String = "スライドして発信"
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val thumbSize = 52.dp
    val thumbSizePx = with(density) { thumbSize.toPx() }
    val trackHeight = 60.dp

    var trackWidthPx by remember { mutableFloatStateOf(0f) }
    val offsetX = remember { Animatable(0f) }
    val coroutineScope = rememberCoroutineScope()

    val maxDragPx = (trackWidthPx - thumbSizePx - with(density) { 8.dp.toPx() }).coerceAtLeast(0f)
    val progress = if (maxDragPx > 0f) (offsetX.value / maxDragPx).coerceIn(0f, 1f) else 0f

    val trackColor = if (!enabled) {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    } else {
        lerp(
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
            Color(0xFF2E7D32).copy(alpha = 0.25f),
            progress
        )
    }

    val thumbColor = if (!enabled) {
        MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
    } else {
        lerp(
            MaterialTheme.colorScheme.primary,
            Color(0xFF2E7D32),
            progress
        )
    }

    Box(
        modifier = modifier
            .height(trackHeight)
            .clip(RoundedCornerShape(30.dp))
            .background(trackColor)
            .border(
                width = 1.5.dp,
                color = if (enabled) MaterialTheme.colorScheme.primary.copy(alpha = 0.4f) else Color.Transparent,
                shape = RoundedCornerShape(30.dp)
            )
            .onSizeChanged { size ->
                trackWidthPx = size.width.toFloat()
            },
        contentAlignment = Alignment.CenterStart
    ) {
        // ドラッグに連動した背景フィル
        if (enabled && progress > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(with(density) { (offsetX.value + thumbSizePx).toDp() })
                    .clip(RoundedCornerShape(30.dp))
                    .background(Color(0xFF2E7D32).copy(alpha = 0.25f))
            )
        }

        // 中央ラベルテキスト
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 48.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (progress > 0.8f) "離して発信！" else label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = if (enabled) {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f - (progress * 0.4f))
                } else {
                    MaterialTheme.colorScheme.outline
                }
            )
        }

        // スライド用サム (丸い発信アイコン)
        Box(
            modifier = Modifier
                .offset { IntOffset(offsetX.value.roundToInt() + with(density) { 4.dp.roundToPx() }, 0) }
                .size(thumbSize)
                .clip(CircleShape)
                .background(thumbColor)
                .pointerInput(enabled, maxDragPx) {
                    if (!enabled || maxDragPx <= 0f) return@pointerInput
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            coroutineScope.launch {
                                if (offsetX.value >= maxDragPx * 0.72f) {
                                    // 完了位置までアニメーションして発信
                                    offsetX.animateTo(
                                        maxDragPx,
                                        animationSpec = spring(stiffness = Spring.StiffnessMedium)
                                    )
                                    performHaptic(context)
                                    onSlideComplete()
                                    // 初期位置に戻す
                                    offsetX.snapTo(0f)
                                } else {
                                    // 途中キャンセル時は開始位置に戻す
                                    offsetX.animateTo(
                                        0f,
                                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
                                    )
                                }
                            }
                        },
                        onDragCancel = {
                            coroutineScope.launch {
                                offsetX.animateTo(0f)
                            }
                        },
                        onHorizontalDrag = { _, dragAmount ->
                            coroutineScope.launch {
                                val newOffset = (offsetX.value + dragAmount).coerceIn(0f, maxDragPx)
                                offsetX.snapTo(newOffset)
                            }
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Call,
                contentDescription = "スライドして発信",
                tint = Color.White,
                modifier = Modifier.size(26.dp)
            )
        }
    }
}

/**
 * 長押し式発信ボタン (Long Press to Call)
 * ボタンを約1秒間長押しすることでプログレスが満了し発信。
 */
@Composable
fun LongPressCallButton(
    onLongPressComplete: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val progress = remember { Animatable(0f) }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(80.dp)
        ) {
            // 長押し進行中プログレスリング
            if (progress.value > 0f) {
                CircularProgressIndicator(
                    progress = { progress.value },
                    modifier = Modifier.size(80.dp),
                    strokeWidth = 5.dp,
                    color = Color(0xFF2E7D32),
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }

            Surface(
                shape = CircleShape,
                color = if (!enabled) {
                    MaterialTheme.colorScheme.surfaceVariant
                } else if (progress.value > 0.8f) {
                    Color(0xFF2E7D32)
                } else {
                    MaterialTheme.colorScheme.primaryContainer
                },
                modifier = Modifier
                    .size(66.dp)
                    .pointerInput(enabled) {
                        if (!enabled) return@pointerInput
                        detectTapGestures(
                            onPress = {
                                val job = coroutineScope.launch {
                                    progress.animateTo(
                                        targetValue = 1f,
                                        animationSpec = tween(durationMillis = 950, easing = LinearEasing)
                                    )
                                    if (progress.value >= 0.99f) {
                                        performHaptic(context)
                                        onLongPressComplete()
                                    }
                                }
                                tryAwaitRelease()
                                val reached = progress.value >= 0.99f
                                job.cancel()
                                if (!reached && progress.value > 0.1f) {
                                    Toast.makeText(context, "長押し（約1秒）して発信します", Toast.LENGTH_SHORT).show()
                                }
                                progress.snapTo(0f)
                            }
                        )
                    }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Call,
                        contentDescription = "長押しして発信",
                        tint = if (!enabled) {
                            MaterialTheme.colorScheme.outline
                        } else if (progress.value > 0.8f) {
                            Color.White
                        } else {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        },
                        modifier = Modifier.size(34.dp)
                    )
                }
            }
        }

        Text(
            text = if (enabled) "長押しで発信（約1秒）" else "番号を入力してください",
            style = MaterialTheme.typography.labelMedium,
            color = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
            fontWeight = FontWeight.Medium
        )
    }
}

/**
 * カウントダウン発信ダイアログ (Countdown to Call)
 * 3秒の猶予時間中にキャンセル可能、放置すれば自動発信。
 */
@Composable
fun CountdownCallDialog(
    targetDisplay: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    totalSeconds: Int = 3
) {
    var remainingSeconds by remember { mutableIntStateOf(totalSeconds) }
    var isFinished by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        while (remainingSeconds > 0 && !isFinished) {
            delay(1000L)
            remainingSeconds--
        }
        if (!isFinished && remainingSeconds <= 0) {
            isFinished = true
            onConfirm()
        }
    }

    AlertDialog(
        onDismissRequest = {
            isFinished = true
            onDismiss()
        },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Timer,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text("発信カウントダウン")
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = targetDisplay,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center
                )

                // カウントダウン円形インジケーター
                Box(contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(
                        progress = { (remainingSeconds.toFloat() / totalSeconds.toFloat()).coerceIn(0f, 1f) },
                        modifier = Modifier.size(88.dp),
                        strokeWidth = 6.dp,
                        color = Color(0xFF2E7D32),
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                    Text(
                        text = "$remainingSeconds",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2E7D32)
                    )
                }

                Text(
                    text = "${remainingSeconds}秒後に自動で発信します",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    isFinished = true
                    onConfirm()
                }
            ) {
                Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("今すぐ発信")
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    isFinished = true
                    onDismiss()
                }
            ) {
                Text("キャンセル")
            }
        }
    )
}
