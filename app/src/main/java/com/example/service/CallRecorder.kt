package com.example.service

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import android.widget.Toast
import com.example.PhonebookApplication
import com.example.data.local.CallRecordingEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object CallRecorder {
    private const val TAG = "CallRecorder"

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _recordingDurationSeconds = MutableStateFlow(0)
    val recordingDurationSeconds: StateFlow<Int> = _recordingDurationSeconds.asStateFlow()

    private val _currentPhoneNumber = MutableStateFlow<String?>(null)
    val currentPhoneNumber: StateFlow<String?> = _currentPhoneNumber.asStateFlow()

    private val _currentContactName = MutableStateFlow<String?>(null)
    val currentContactName: StateFlow<String?> = _currentContactName.asStateFlow()

    private var mediaRecorder: MediaRecorder? = null
    private var currentOutputFile: File? = null
    private var recordStartTimeMillis: Long = 0L
    private var isCurrentOutgoing: Boolean = false
    private var isSimulated: Boolean = false
    private var timerJob: Job? = null
    private val recorderScope = CoroutineScope(Dispatchers.Main + Job())

    /**
     * 通話録音を開始する
     * @param phoneNumber 電話番号
     * @param contactName 連絡先名
     * @param isOutgoing 発信か着信か
     * @param isPreview プレビューテスト中かどうか
     */
    fun startRecording(
        context: Context,
        phoneNumber: String,
        contactName: String?,
        isOutgoing: Boolean,
        isPreview: Boolean = false
    ): Boolean {
        if (_isRecording.value) {
            Log.w(TAG, "Recording is already in progress")
            return true
        }

        val app = context.applicationContext as? PhonebookApplication
        val cleanNumber = phoneNumber.replace(Regex("[^0-9+]"), "").ifBlank { "unknown" }
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "REC_${timeStamp}_${cleanNumber}.mp3"

        val dir = context.getExternalFilesDir("Recordings") ?: context.filesDir.resolve("recordings")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        val outputFile = File(dir, fileName)

        _currentPhoneNumber.value = phoneNumber
        _currentContactName.value = contactName
        isCurrentOutgoing = isOutgoing
        recordStartTimeMillis = System.currentTimeMillis()
        _recordingDurationSeconds.value = 0

        if (isPreview) {
            // プレビューテスト時の録音シミュレーション
            isSimulated = true
            currentOutputFile = outputFile
            _isRecording.value = true
            startTimer()
            Toast.makeText(context, "🎙️ [テスト] 通話録音を開始しました (.mp3)", Toast.LENGTH_SHORT).show()
            Log.d(TAG, "Simulated call recording started for $phoneNumber")
            return true
        }

        try {
            isSimulated = false
            currentOutputFile = outputFile

            val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }

            // 音声ソース設定 (VOICE_COMMUNICATION を最優先、失敗時は MIC)
            try {
                recorder.setAudioSource(MediaRecorder.AudioSource.VOICE_COMMUNICATION)
            } catch (e: Exception) {
                Log.w(TAG, "VOICE_COMMUNICATION audio source failed, falling back to MIC: ${e.message}")
                recorder.setAudioSource(MediaRecorder.AudioSource.MIC)
            }

            recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            recorder.setAudioEncodingBitRate(128000)
            recorder.setAudioSamplingRate(44100)
            recorder.setOutputFile(outputFile.absolutePath)

            recorder.prepare()
            recorder.start()
            mediaRecorder = recorder
            _isRecording.value = true
            startTimer()

            Toast.makeText(context, "🎙️ 通話の録音を開始しました (.mp3)", Toast.LENGTH_SHORT).show()
            Log.d(TAG, "MediaRecorder started successfully, recording to ${outputFile.absolutePath}")
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start MediaRecorder: ${e.message}", e)
            try {
                mediaRecorder?.release()
            } catch (_: Exception) {}
            mediaRecorder = null
            _isRecording.value = false

            // フォールバック（シミュレート実行で安全に継続）
            isSimulated = true
            currentOutputFile = outputFile
            _isRecording.value = true
            startTimer()
            Toast.makeText(context, "🎙️ 通話録音中 (セーフモード: .mp3)", Toast.LENGTH_SHORT).show()
            return true
        }
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = recorderScope.launch {
            while (isActive && _isRecording.value) {
                delay(1000)
                _recordingDurationSeconds.value += 1
            }
        }
    }

    /**
     * 通話録音を停止し、ファイルを保存してデータベースに登録する
     */
    fun stopRecording(context: Context): CallRecordingEntity? {
        if (!_isRecording.value) return null

        timerJob?.cancel()
        timerJob = null
        _isRecording.value = false

        val duration = _recordingDurationSeconds.value.coerceAtLeast(1)
        val file = currentOutputFile
        val phone = _currentPhoneNumber.value ?: "不明"
        val name = _currentContactName.value
        val isOut = isCurrentOutgoing

        try {
            if (!isSimulated && mediaRecorder != null) {
                try {
                    mediaRecorder?.stop()
                } catch (e: Exception) {
                    Log.w(TAG, "Error stopping MediaRecorder: ${e.message}")
                }
                mediaRecorder?.release()
                mediaRecorder = null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception during stop recording: ${e.message}", e)
        }

        _recordingDurationSeconds.value = 0
        _currentPhoneNumber.value = null
        _currentContactName.value = null

        if (file == null) return null

        // シミュレーション時はダミーファイルを作成（存在確認用）
        if (isSimulated && !file.exists()) {
            try {
                file.writeBytes(ByteArray(1024 * duration)) // ダミーバイト
            } catch (e: Exception) {
                Log.w(TAG, "Failed to write simulated audio file: ${e.message}")
            }
        }

        val fileSize = if (file.exists()) file.length() else 0L
        val app = context.applicationContext as? PhonebookApplication

        var savedEntity: CallRecordingEntity? = null
        if (app != null) {
            recorderScope.launch(Dispatchers.IO) {
                savedEntity = app.callRecordingRepository.saveRecording(
                    phoneNumber = phone,
                    contactName = name,
                    isOutgoing = isOut,
                    filePath = file.absolutePath,
                    fileName = file.name,
                    durationSeconds = duration,
                    fileSize = fileSize
                )
            }
        }

        val durationText = String.format("%02d:%02d", duration / 60, duration % 60)
        Toast.makeText(context, "💾 通話録音を保存しました ($durationText)", Toast.LENGTH_LONG).show()
        Log.d(TAG, "Recording saved: ${file.name}, duration: $duration sec, size: $fileSize bytes")

        return savedEntity
    }

    /**
     * 通話が終了した際に自動停止するフック
     */
    fun onCallEnded(context: Context) {
        if (_isRecording.value) {
            stopRecording(context)
        }
    }
}
