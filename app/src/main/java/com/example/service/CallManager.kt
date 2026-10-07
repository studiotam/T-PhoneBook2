package com.example.service

import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.ContactsContract
import android.telecom.Call
import android.telecom.CallAudioState
import android.telecom.VideoProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class CallStateInfo(
    val phoneNumber: String,
    val contactName: String?,
    val company: String? = null,
    val photoUri: String? = null,
    val state: Int,
    val isMuted: Boolean = false,
    val isSpeakerOn: Boolean = false,
    val isBluetoothOn: Boolean = false,
    val isBluetoothAvailable: Boolean = false,
    val isWiredHeadsetOn: Boolean = false,
    val audioRoute: Int = CallAudioState.ROUTE_EARPIECE,
    val supportedAudioRoutes: Int = CallAudioState.ROUTE_EARPIECE or CallAudioState.ROUTE_SPEAKER,
    val bluetoothDeviceName: String? = null,
    val connectTimeMillis: Long = 0L,
    val isPreview: Boolean = false,
    val isOutgoing: Boolean = false,
    val prefixName: String? = null,
    val simSlot: Int? = null
)

object CallManager {

    private var currentCall: Call? = null
    private var inCallServiceInstance: AppInCallService? = null

    private var isCurrentCallIncoming = false
    private var isCurrentCallAnswered = false
    private var isCurrentCallUserRejected = false
    private var lastCallerNumber = ""
    private var lastCallerName: String? = null
    private var lastCallTimestamp = 0L

    private val _callStateInfo = MutableStateFlow<CallStateInfo?>(null)
    val callStateInfo: StateFlow<CallStateInfo?> = _callStateInfo.asStateFlow()

    private val callCallback = object : Call.Callback() {
        override fun onStateChanged(call: Call, state: Int) {
            super.onStateChanged(call, state)
            if (state == Call.STATE_ACTIVE || state == Call.STATE_HOLDING) {
                isCurrentCallAnswered = true
            }
            updateCallState(call)
            if (state == Call.STATE_DISCONNECTED) {
                inCallServiceInstance?.let { CallRecorder.onCallEnded(it) }

                // 不在着信判定
                val disconnectCode = call.details?.disconnectCause?.code
                val isUserRejected = isCurrentCallUserRejected || disconnectCode == android.telecom.DisconnectCause.REJECTED
                val isMissed = isCurrentCallIncoming && !isCurrentCallAnswered && !isUserRejected

                if (isMissed && lastCallerNumber.isNotBlank()) {
                    val appContext = inCallServiceInstance?.applicationContext
                    if (appContext != null) {
                        MissedCallNotifier.showMissedCallNotification(
                            context = appContext,
                            phoneNumber = lastCallerNumber,
                            contactName = lastCallerName,
                            timestamp = if (lastCallTimestamp > 0L) lastCallTimestamp else System.currentTimeMillis()
                        )
                    }
                }

                _callStateInfo.value = null
                currentCall = null
            }
        }

        override fun onDetailsChanged(call: Call, details: Call.Details) {
            super.onDetailsChanged(call, details)
            updateCallState(call)
        }
    }

    fun registerInCallService(service: AppInCallService) {
        inCallServiceInstance = service
        // サービス接続時に現在の音声状態を即座に反映
        val audioState = service.callAudioState
        if (audioState != null) {
            updateAudioState(audioState)
        }
    }

    fun unregisterInCallService(service: AppInCallService) {
        if (inCallServiceInstance == service) {
            inCallServiceInstance = null
        }
    }

    fun setCall(call: Call, context: Context) {
        currentCall?.unregisterCallback(callCallback)
        currentCall = call
        call.registerCallback(callCallback)
        val newNum = call.details?.handle?.schemeSpecificPart ?: ""
        lastCallerNumber = newNum
        lastCallTimestamp = System.currentTimeMillis()
        val isIncoming = call.state == Call.STATE_RINGING ||
                (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && call.details?.callDirection == Call.Details.DIRECTION_INCOMING) ||
                (call.details?.callDirection != Call.Details.DIRECTION_OUTGOING && call.state != Call.STATE_DIALING && call.state != Call.STATE_CONNECTING)
        isCurrentCallIncoming = isIncoming
        isCurrentCallAnswered = call.state == Call.STATE_ACTIVE || call.state == Call.STATE_HOLDING
        isCurrentCallUserRejected = false

        if (_callStateInfo.value?.phoneNumber != newNum) {
            _callStateInfo.value = null
        }
        updateCallState(call, context)
    }

    fun removeCall(call: Call) {
        if (currentCall == call) {
            call.unregisterCallback(callCallback)
            currentCall = null
            _callStateInfo.value = null
        }
    }

    private fun updateCallState(call: Call, context: Context? = inCallServiceInstance) {
        val number = call.details?.handle?.schemeSpecificPart ?: ""
        if (number.isNotBlank()) {
            lastCallerNumber = number
        }
        val cachedName = call.details?.callerDisplayName
        val contactInfo = if (context != null && number.isNotBlank()) {
            lookupContactInfo(context, number)
        } else null

        val name = if (!cachedName.isNullOrBlank()) {
            cachedName
        } else {
            contactInfo?.name
        }
        lastCallerName = name

        val audioState = inCallServiceInstance?.callAudioState
        val isMuted = audioState?.isMuted ?: false
        val audioRoute = audioState?.route ?: CallAudioState.ROUTE_EARPIECE
        val supportedRoutes = audioState?.supportedRouteMask ?: (CallAudioState.ROUTE_EARPIECE or CallAudioState.ROUTE_SPEAKER)
        val isBluetoothAvailable = (supportedRoutes and CallAudioState.ROUTE_BLUETOOTH) != 0
        val isBluetoothOn = audioRoute == CallAudioState.ROUTE_BLUETOOTH
        val isSpeaker = audioRoute == CallAudioState.ROUTE_SPEAKER
        val isWired = audioRoute == CallAudioState.ROUTE_WIRED_HEADSET

        var btName: String? = null
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            try {
                btName = audioState?.activeBluetoothDevice?.name
            } catch (_: SecurityException) {
                // BLUETOOTH_CONNECT permission
            }
        }

        val direction = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            call.details?.callDirection ?: -1
        } else -1

        val isOutgoing = when {
            direction == Call.Details.DIRECTION_OUTGOING -> true
            direction == Call.Details.DIRECTION_INCOMING -> false
            call.state == Call.STATE_RINGING -> false
            call.state == Call.STATE_DIALING || call.state == Call.STATE_CONNECTING -> true
            else -> (_callStateInfo.value?.isOutgoing == true) && (_callStateInfo.value?.phoneNumber == number)
        }

        _callStateInfo.value = CallStateInfo(
            phoneNumber = number,
            contactName = name,
            company = contactInfo?.company,
            photoUri = contactInfo?.photoUri,
            state = call.state,
            isMuted = isMuted,
            isSpeakerOn = isSpeaker,
            isBluetoothOn = isBluetoothOn,
            isBluetoothAvailable = isBluetoothAvailable,
            isWiredHeadsetOn = isWired,
            audioRoute = audioRoute,
            supportedAudioRoutes = supportedRoutes,
            bluetoothDeviceName = btName,
            connectTimeMillis = call.details?.connectTimeMillis ?: 0L,
            isPreview = false,
            isOutgoing = isOutgoing,
            prefixName = _callStateInfo.value?.prefixName,
            simSlot = _callStateInfo.value?.simSlot
        )
    }

    fun updateAudioState(audioState: CallAudioState) {
        val current = _callStateInfo.value ?: return
        val audioRoute = audioState.route
        val supportedRoutes = audioState.supportedRouteMask
        val isBluetoothAvailable = (supportedRoutes and CallAudioState.ROUTE_BLUETOOTH) != 0
        val isBluetoothOn = audioRoute == CallAudioState.ROUTE_BLUETOOTH
        val isSpeaker = audioRoute == CallAudioState.ROUTE_SPEAKER
        val isWired = audioRoute == CallAudioState.ROUTE_WIRED_HEADSET

        var btName: String? = null
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            try {
                btName = audioState.activeBluetoothDevice?.name
            } catch (_: SecurityException) {
                // BLUETOOTH_CONNECT permission
            }
        }

        _callStateInfo.value = current.copy(
            isMuted = audioState.isMuted,
            isSpeakerOn = isSpeaker,
            isBluetoothOn = isBluetoothOn,
            isBluetoothAvailable = isBluetoothAvailable,
            isWiredHeadsetOn = isWired,
            audioRoute = audioRoute,
            supportedAudioRoutes = supportedRoutes,
            bluetoothDeviceName = btName ?: current.bluetoothDeviceName
        )
    }

    fun answer() {
        val current = _callStateInfo.value
        if (current?.isPreview == true) {
            _callStateInfo.value = current.copy(
                state = Call.STATE_ACTIVE,
                connectTimeMillis = System.currentTimeMillis()
            )
            return
        }
        currentCall?.answer(VideoProfile.STATE_AUDIO_ONLY)
    }

    fun reject() {
        isCurrentCallUserRejected = true
        val current = _callStateInfo.value
        if (current?.isPreview == true) {
            _callStateInfo.value = current.copy(state = Call.STATE_DISCONNECTED)
            return
        }
        if (currentCall?.state == Call.STATE_RINGING) {
            currentCall?.reject(false, null)
        } else {
            currentCall?.disconnect()
        }
    }

    fun disconnect() {
        val current = _callStateInfo.value
        if (current?.isPreview == true) {
            _callStateInfo.value = current.copy(state = Call.STATE_DISCONNECTED)
            return
        }
        currentCall?.disconnect()
    }

    fun toggleMute() {
        val current = _callStateInfo.value
        if (current?.isPreview == true) {
            _callStateInfo.value = current.copy(isMuted = !current.isMuted)
            return
        }
        val service = inCallServiceInstance ?: return
        val currentMuted = service.callAudioState?.isMuted ?: false
        service.setMuted(!currentMuted)
    }

    fun setAudioRoute(route: Int) {
        val current = _callStateInfo.value
        if (current?.isPreview == true) {
            _callStateInfo.value = current.copy(
                audioRoute = route,
                isSpeakerOn = route == CallAudioState.ROUTE_SPEAKER,
                isBluetoothOn = route == CallAudioState.ROUTE_BLUETOOTH,
                isWiredHeadsetOn = route == CallAudioState.ROUTE_WIRED_HEADSET
            )
            return
        }
        inCallServiceInstance?.setAudioRoute(route)
    }

    fun toggleBluetooth() {
        val current = _callStateInfo.value
        if (current?.isPreview == true) {
            val newBt = !current.isBluetoothOn
            _callStateInfo.value = current.copy(
                isBluetoothOn = newBt,
                isSpeakerOn = false,
                audioRoute = if (newBt) CallAudioState.ROUTE_BLUETOOTH else CallAudioState.ROUTE_EARPIECE
            )
            return
        }
        val service = inCallServiceInstance ?: return
        val currentRoute = service.callAudioState?.route ?: CallAudioState.ROUTE_EARPIECE
        if (currentRoute == CallAudioState.ROUTE_BLUETOOTH) {
            service.setAudioRoute(CallAudioState.ROUTE_EARPIECE)
        } else {
            service.setAudioRoute(CallAudioState.ROUTE_BLUETOOTH)
        }
    }

    fun toggleSpeaker() {
        val current = _callStateInfo.value
        if (current?.isPreview == true) {
            val newSp = !current.isSpeakerOn
            _callStateInfo.value = current.copy(
                isSpeakerOn = newSp,
                isBluetoothOn = false,
                audioRoute = if (newSp) CallAudioState.ROUTE_SPEAKER else CallAudioState.ROUTE_EARPIECE
            )
            return
        }
        val service = inCallServiceInstance ?: return
        val currentRoute = service.callAudioState?.route ?: CallAudioState.ROUTE_EARPIECE
        if (currentRoute == CallAudioState.ROUTE_SPEAKER) {
            if (current?.isBluetoothAvailable == true) {
                service.setAudioRoute(CallAudioState.ROUTE_BLUETOOTH)
            } else {
                service.setAudioRoute(CallAudioState.ROUTE_EARPIECE)
            }
        } else {
            service.setAudioRoute(CallAudioState.ROUTE_SPEAKER)
        }
    }

    fun sendDtmf(digit: Char) {
        val current = _callStateInfo.value
        if (current?.isPreview == true) return
        currentCall?.playDtmfTone(digit)
        currentCall?.stopDtmfTone()
    }

    /**
     * 着信画面のテスト・プレビュー起動
     */
    fun startPreviewCall(
        phoneNumber: String = "090-1234-5678",
        contactName: String? = "山田 太郎",
        company: String? = "株式会社サンプル 営業部",
        photoUri: String? = null
    ) {
        _callStateInfo.value = CallStateInfo(
            phoneNumber = phoneNumber,
            contactName = contactName,
            company = company,
            photoUri = photoUri,
            state = Call.STATE_RINGING,
            isMuted = false,
            isSpeakerOn = false,
            isBluetoothOn = false,
            isBluetoothAvailable = true,
            bluetoothDeviceName = "Pixel Buds Pro",
            connectTimeMillis = 0L,
            isPreview = true,
            isOutgoing = false
        )
    }

    /**
     * 通話中画面のテスト・プレビュー起動
     */
    fun startPreviewActiveCall(
        phoneNumber: String = "090-1234-5678",
        contactName: String? = "山田 太郎",
        company: String? = "株式会社サンプル 営業部",
        photoUri: String? = null
    ) {
        _callStateInfo.value = CallStateInfo(
            phoneNumber = phoneNumber,
            contactName = contactName,
            company = company,
            photoUri = photoUri,
            state = Call.STATE_ACTIVE,
            isMuted = false,
            isSpeakerOn = false,
            isBluetoothOn = true,
            isBluetoothAvailable = true,
            bluetoothDeviceName = "Pixel Buds Pro",
            connectTimeMillis = System.currentTimeMillis() - 83000, // 01:23経過
            isPreview = true,
            isOutgoing = false
        )
    }

    /**
     * 発信画面のテスト・プレビュー起動
     */
    fun startPreviewOutgoingCall(
        phoneNumber: String = "090-9876-5432",
        contactName: String? = "佐藤 花子",
        company: String? = "デザインパートナーズ 代表",
        photoUri: String? = null,
        prefixName: String? = "楽天でんわ (003768)",
        simSlot: Int? = 1
    ) {
        _callStateInfo.value = CallStateInfo(
            phoneNumber = phoneNumber,
            contactName = contactName,
            company = company,
            photoUri = photoUri,
            state = Call.STATE_DIALING,
            isMuted = false,
            isSpeakerOn = false,
            isBluetoothOn = false,
            isBluetoothAvailable = true,
            bluetoothDeviceName = "WF-1000XM4",
            connectTimeMillis = 0L,
            isPreview = true,
            isOutgoing = true,
            prefixName = prefixName,
            simSlot = simSlot
        )
    }

    fun clearPreview() {
        if (_callStateInfo.value?.isPreview == true) {
            _callStateInfo.value = null
        }
    }

    private data class ContactLookupResult(
        val name: String?,
        val company: String?,
        val photoUri: String?
    )

    private fun lookupContactInfo(context: Context, phoneNumber: String): ContactLookupResult? {
        return try {
            val uri = Uri.withAppendedPath(
                ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                Uri.encode(phoneNumber)
            )
            var contactId: Long? = null
            var name: String? = null
            var photoUri: String? = null

            context.contentResolver.query(
                uri,
                arrayOf(
                    ContactsContract.PhoneLookup._ID,
                    ContactsContract.PhoneLookup.DISPLAY_NAME,
                    ContactsContract.PhoneLookup.PHOTO_URI
                ),
                null,
                null,
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    contactId = cursor.getLong(0)
                    name = cursor.getString(1)
                    photoUri = cursor.getString(2)
                }
            }

            var company: String? = null
            if (contactId != null) {
                // 会社名検索
                context.contentResolver.query(
                    ContactsContract.Data.CONTENT_URI,
                    arrayOf(
                        ContactsContract.CommonDataKinds.Organization.COMPANY,
                        ContactsContract.CommonDataKinds.Organization.TITLE
                    ),
                    "${ContactsContract.Data.CONTACT_ID} = ? AND ${ContactsContract.Data.MIMETYPE} = ?",
                    arrayOf(
                        contactId.toString(),
                        ContactsContract.CommonDataKinds.Organization.CONTENT_ITEM_TYPE
                    ),
                    null
                )?.use { orgCursor ->
                    if (orgCursor.moveToFirst()) {
                        val comp = orgCursor.getString(0)
                        val title = orgCursor.getString(1)
                        company = when {
                            !comp.isNullOrBlank() && !title.isNullOrBlank() -> "$comp ($title)"
                            !comp.isNullOrBlank() -> comp
                            !title.isNullOrBlank() -> title
                            else -> null
                        }
                    }
                }
            }

            ContactLookupResult(name = name, company = company, photoUri = photoUri)
        } catch (_: Exception) {
            null
        }
    }
}
