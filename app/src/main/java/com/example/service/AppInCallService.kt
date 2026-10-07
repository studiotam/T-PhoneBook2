package com.example.service

import android.content.Intent
import android.telecom.Call
import android.telecom.CallAudioState
import android.telecom.InCallService
import com.example.PhonebookApplication
import com.example.ui.call.InCallActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class AppInCallService : InCallService() {

    private val serviceScope = CoroutineScope(Dispatchers.Main)

    override fun onCreate() {
        super.onCreate()
        CallManager.registerInCallService(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        CallManager.unregisterInCallService(this)
    }

    override fun onCallAdded(call: Call) {
        super.onCallAdded(call)
        CallManager.setCall(call, applicationContext)

        val app = applicationContext as? PhonebookApplication
        serviceScope.launch {
            val isIncoming = call.state == Call.STATE_RINGING ||
                    (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q && call.details?.callDirection == Call.Details.DIRECTION_INCOMING) ||
                    (call.details?.callDirection != Call.Details.DIRECTION_OUTGOING && call.state != Call.STATE_DIALING && call.state != Call.STATE_CONNECTING)
            val isEnabled = if (isIncoming) {
                val incomingConfig = app?.settingsRepository?.incomingCallDisplayConfigFlow?.first()
                incomingConfig?.enabled ?: true
            } else {
                val outgoingConfig = app?.settingsRepository?.outgoingCallDisplayConfigFlow?.first()
                outgoingConfig?.enabled ?: true
            }

            if (isEnabled) {
                try {
                    val intent = Intent(this@AppInCallService, InCallActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                                Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or
                                Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
                    }
                    startActivity(intent)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    override fun onCallRemoved(call: Call) {
        super.onCallRemoved(call)
        CallManager.removeCall(call)
    }

    override fun onCallAudioStateChanged(audioState: CallAudioState) {
        super.onCallAudioStateChanged(audioState)
        CallManager.updateAudioState(audioState)
    }
}
