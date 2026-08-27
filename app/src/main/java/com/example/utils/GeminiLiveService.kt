package com.example.utils

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Base64
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import okhttp3.*
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class GeminiLiveService(private val apiKey: String) {

    private var webSocket: WebSocket? = null
    private val client = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .build()

    private var audioRecord: AudioRecord? = null
    private var isRecording = false
    private val scope = CoroutineScope(Dispatchers.IO + Job())

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val connectionState: StateFlow<ConnectionState> = _connectionState

    enum class ConnectionState {
        Disconnected, Connecting, Connected, Error
    }

    fun connect() {
        if (_connectionState.value == ConnectionState.Connecting || _connectionState.value == ConnectionState.Connected) {
            return
        }
        
        _connectionState.value = ConnectionState.Connecting
        // Live API WebSocket Endpoint
        val url = "wss://generativelanguage.googleapis.com/ws/google.ai.generativelanguage.v1alpha.GenerativeService.BidiGenerateContent?key=$apiKey"
        
        val request = Request.Builder().url(url).build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                _connectionState.value = ConnectionState.Connected
                sendInitialSetup()
                startRecordingAndStreaming()
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                // Handle text messages from Gemini
                // Parse JSON, handle model turns, extract audio data to play if present
                handleMessage(text)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                _connectionState.value = ConnectionState.Disconnected
                stopRecording()
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                _connectionState.value = ConnectionState.Error
                stopRecording()
            }
        })
    }

    private fun sendInitialSetup() {
        val setupMessage = JSONObject().apply {
            put("setup", JSONObject().apply {
                put("model", "models/gemini-3.1-flash-live-preview")
                put("generationConfig", JSONObject().apply {
                    put("responseModalities", org.json.JSONArray().put("AUDIO"))
                })
            })
        }
        val clientContent = JSONObject().apply {
            put("clientContent", setupMessage)
        }
        webSocket?.send(clientContent.toString())
    }

    @SuppressLint("MissingPermission") // Requires Manifest.permission.RECORD_AUDIO
    private fun startRecordingAndStreaming() {
        val sampleRate = 16000
        val bufferSize = AudioRecord.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )

        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            bufferSize
        )

        audioRecord?.startRecording()
        isRecording = true

        scope.launch {
            val buffer = ByteArray(bufferSize)
            while (isRecording && _connectionState.value == ConnectionState.Connected) {
                val readResult = audioRecord?.read(buffer, 0, buffer.size) ?: 0
                if (readResult > 0) {
                    val base64Audio = Base64.encodeToString(buffer.copyOfRange(0, readResult), Base64.NO_WRAP)
                    sendRealtimeAudioData(base64Audio)
                }
            }
        }
    }

    private fun sendRealtimeAudioData(base64Audio: String) {
        val realtimeInput = JSONObject().apply {
            put("realtimeInput", JSONObject().apply {
                put("mediaChunks", org.json.JSONArray().apply {
                    put(JSONObject().apply {
                        put("mimeType", "audio/pcm;rate=16000")
                        put("data", base64Audio)
                    })
                })
            })
        }
        val clientContent = JSONObject().apply {
            put("clientContent", realtimeInput)
        }
        webSocket?.send(clientContent.toString())
    }
    
    private fun handleMessage(jsonString: String) {
        // Implement parsing of Gemini Live response JSON and play audio chunk if returned
    }

    fun disconnect() {
        isRecording = false
        stopRecording()
        webSocket?.close(1000, "User disconnected")
        _connectionState.value = ConnectionState.Disconnected
    }

    private fun stopRecording() {
        isRecording = false
        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null
    }
}
