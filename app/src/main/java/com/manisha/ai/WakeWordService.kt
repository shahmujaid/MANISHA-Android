
package com.manisha.ai

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.Manifest
import org.json.JSONArray
import org.json.JSONObject
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.Locale
import java.util.concurrent.TimeUnit

class WakeWordService : Service(), TextToSpeech.OnInitListener {

    companion object {
        const val ACTION_STATUS = "com.manisha.ai.STATUS"
        const val ACTION_COMMAND = "com.manisha.ai.COMMAND"
        const val ACTION_REPLY = "com.manisha.ai.REPLY"
        const val ACTION_STOP = "com.manisha.ai.STOP"

        const val EXTRA_TEXT = "text"

        private const val CHANNEL_ID = "MIRA_voice_channel"
        private const val NOTIFICATION_ID = 7001
    }

    private val handler = Handler(Looper.getMainLooper())

    private var recognizer: SpeechRecognizer? = null
    private var tts: TextToSpeech? = null

    private var ttsReady = false
    private var waitingForCommand = false
    private var isProcessing = false
    private var pendingReply: String? = null
    private var languageTag = "en-IN"

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .build()

    override fun onCreate() {
        super.onCreate()

        createNotificationChannel()

        val notification = Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("MIRA AI is active")
            .setContentText("Listening for MIRA")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setOngoing(true)
            .build()

        startForeground(NOTIFICATION_ID, notification)

        val prefs = getSharedPreferences("manisha_settings", MODE_PRIVATE)
        languageTag = getLanguageTag(
            prefs.getString("language", "English") ?: "English"
        )

        tts = TextToSpeech(this, this)

        broadcastStatus("MIRA voice service starting")

        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            broadcastStatus("Speech recognition is not available")
            stopSelf()
            return
        }

        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {
            broadcastStatus("Microphone permission is required")
            stopSelf()
            return
        }

        startListening()
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int
    ): Int {

        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }

        return START_NOT_STICKY
    }

    private fun getLanguageTag(value: String): String {
        return when (value.lowercase()) {
            "hindi", "hi", "hi-in" -> "hi-IN"
            "marathi", "mr", "mr-in" -> "mr-IN"
            "gujarati", "gu", "gu-in" -> "gu-IN"
            else -> "en-IN"
        }
    }

    private fun startListening() {
        if (isProcessing) return

        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {
            broadcastStatus("Microphone permission missing")
            return
        }

        try {
            if (recognizer == null) {
                recognizer = SpeechRecognizer.createSpeechRecognizer(this)

                recognizer?.setRecognitionListener(
                    object : RecognitionListener {

                        override fun onReadyForSpeech(params: android.os.Bundle?) {
                            broadcastStatus(
                                if (waitingForCommand) {
                                    "Listening for your command"
                                } else {
                                    "Say MIRA to activate"
                                }
                            )
                        }

                        override fun onBeginningOfSpeech() {
                        }

                        override fun onRmsChanged(rmsdB: Float) {
                        }

                        override fun onBufferReceived(buffer: ByteArray?) {
                        }

                        override fun onEndOfSpeech() {
                        }

                        override fun onError(error: Int) {
    val errorMessage = when (error) {
        SpeechRecognizer.ERROR_AUDIO -> "Microphone audio error"
        SpeechRecognizer.ERROR_CLIENT -> "Speech recognition client error"
        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission denied"
        SpeechRecognizer.ERROR_NETWORK -> "Speech recognition network error"
        SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Speech recognition network timeout"
        SpeechRecognizer.ERROR_NO_MATCH -> "MIRA could not recognise your voice"
        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Speech recognizer is busy"
        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech detected"
        else -> "Speech recognition error: $error"
    }

    broadcastStatus(errorMessage)

    if (!isProcessing) {
        handler.postDelayed({
            startListening()
        }, 1500)
    }
}

                       override fun onResults(results: android.os.Bundle?) {
    val matches = results?.getStringArrayList(
        SpeechRecognizer.RESULTS_RECOGNITION
    )

    val spoken = matches?.firstOrNull()?.trim().orEmpty()

    if (spoken.isNotBlank()) {
        broadcastStatus("MIRA heard: $spoken")
        processRecognizedSpeech(spoken)
    } else if (!isProcessing) {
        broadcastStatus("No speech recognised")
        handler.postDelayed({
            startListening()
        }, 700)
    }
}

                        override fun onPartialResults(
                            partialResults: android.os.Bundle?
                        ) {
                        }

                        override fun onEvent(
                            eventType: Int,
                            params: android.os.Bundle?
                        ) {
                        }
                    }
                )
            }

            val speechIntent = Intent(
                RecognizerIntent.ACTION_RECOGNIZE_SPEECH
            ).apply {
                putExtra(
                    RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                )
                putExtra(
                    RecognizerIntent.EXTRA_LANGUAGE,
                    languageTag
                )
                putExtra(
                    RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE,
                    languageTag
                )
                putExtra(
                    RecognizerIntent.EXTRA_PARTIAL_RESULTS,
                    false
                )
                putExtra(
                    RecognizerIntent.EXTRA_MAX_RESULTS,
                    1
                )
            }

            recognizer?.startListening(speechIntent)

        } catch (e: Exception) {
            broadcastStatus("Could not start listening")
            handler.postDelayed({ startListening() }, 2000)
        }
    }

    private fun processRecognizedSpeech(spoken: String) {

        if (isProcessing) return

        val wakeWord = Regex(
    "\\b(m+i+r+a+|m+e+e+r+a+|m+y+r+a+)\\b",
    RegexOption.IGNORE_CASE
)

        if (waitingForCommand) {
            waitingForCommand = false
            sendCommandToGroq(spoken)
            return
        }

        val match = wakeWord.find(spoken)

        if (match != null) {
            val command = spoken
                .substring(match.range.last + 1)
                .trim()
                .trim(',', '.', '!', '?')

            if (command.isNotBlank()) {
                sendCommandToGroq(command)
            } else {
                waitingForCommand = true
                broadcastStatus("MIRA activated. Speak your command.")
                handler.postDelayed({
                    startListening()
                }, 500)
            }
        } else {
            handler.postDelayed({
                startListening()
            }, 500)
        }
    }

    private fun sendCommandToGroq(command: String) {

    if (isProcessing) return

    isProcessing = true

    broadcastStatus("MIRA is processing your command")

    val userIntent = Intent(ACTION_COMMAND).apply {
        setPackage(packageName)
        putExtra(EXTRA_TEXT, command)
    }

    sendBroadcast(userIntent)
}

        Thread {

            try {
                val prefs = getSharedPreferences(
                    "manisha_settings",
                    MODE_PRIVATE
                )

                val apiKey = prefs.getString("groq_key", "").orEmpty()

                val model = prefs.getString(
                    "model",
                    "openai/gpt-oss-20b"
                ) ?: "openai/gpt-oss-20b"

                if (apiKey.isBlank()) {
                    throw Exception("Groq API key is missing in MIRA settings")
                }

                val languageInstruction = when (languageTag) {
    "hi-IN" -> """
        Reply in natural, everyday Indian Hindi.
        Use casual spoken Hindi mixed naturally with common English words.
        Avoid formal, textbook or overly pure Hindi.
        Always use feminine grammar when referring to yourself.
        Say "Main bata deti hoon", "Main kar sakti hoon",
        and "Main samajh gayi".
        Never use masculine forms like "bata deta hoon" or "kar sakta hoon".
    """.trimIndent()

    "mr-IN" -> """
        Reply in natural, everyday conversational Marathi.
        Keep the language friendly and easy to understand.
        Maintain a feminine speaking identity.
    """.trimIndent()

    "gu-IN" -> """
        Reply in natural, everyday conversational Gujarati.
        Keep the language friendly and easy to understand.
        Maintain a feminine speaking identity.
    """.trimIndent()

    else -> """
        Reply in natural conversational English.
        Maintain a warm, friendly and feminine speaking identity.
    """.trimIndent()
}

                val girlfriendMode = prefs.getBoolean("girlfriend_mode", true)

val personality = if (girlfriendMode) {
    """
    You are MIRA, the user's affectionate AI girlfriend and personal assistant.
    Be romantic, caring, playful and emotionally supportive.
    Use sweet nicknames such as jaan, love, baby, or dear occasionally and naturally.
    Keep spoken replies warm, natural and concise.
    Never pretend to be a real human.
    """
} else {
    """
    You are MIRA, a professional, intelligent and respectful personal AI assistant.
    Be helpful, clear and task-focused.
    Do not use romantic language or pet names.
    Keep spoken replies natural and concise.
    Never pretend to be a real human.
    """
}

                val systemPrompt = """
    $personality
    $languageInstruction
""".trimIndent()

val messages = JSONArray()
    .put(
        JSONObject()
            .put("role", "system")
            .put("content", systemPrompt)
    )
    .put(
        JSONObject()
            .put("role", "user")
            .put("content", command)
    )
                val requestJson = JSONObject()
                    .put("model", model)
                    .put("messages", messages)
                    .put("temperature", 0.7)
                    .put("max_tokens", 700)

                val body = requestJson.toString().toRequestBody(
                    "application/json; charset=utf-8".toMediaType()
                )

                val request = Request.Builder()
                    .url("https://api.groq.com/openai/v1/chat/completions")
                    .addHeader("Authorization", "Bearer $apiKey")
                    .addHeader("Content-Type", "application/json")
                    .post(body)
                    .build()

                httpClient.newCall(request).execute().use { response ->

                    val responseBody = response.body?.string().orEmpty()

                    if (!response.isSuccessful) {
                        throw Exception("Groq request failed: HTTP ${response.code}")
                    }

                    val json = JSONObject(responseBody)

                    val reply = json
                        .getJSONArray("choices")
                        .getJSONObject(0)
                        .getJSONObject("message")
                        .getString("content")
                        .trim()

                    handler.post {
                        handleReply(reply)
                    }
                }

            } catch (e: Exception) {

                handler.post {
                    handleReply(
                        "Sorry, I could not complete that request. Please try again."
                    )
                    broadcastStatus(e.message ?: "AI request failed")
                }
            }

        }.start()
    }

    private fun handleReply(reply: String) {

        val replyIntent = Intent(ACTION_REPLY).apply {
            setPackage(packageName)
            putExtra(EXTRA_TEXT, reply)
        }
        sendBroadcast(replyIntent)

        if (ttsReady && tts != null) {
            speakReply(reply)
        } else {
            pendingReply = reply
        }
    }

    private fun speakReply(reply: String) {

        try {
            tts?.speak(
                reply,
                TextToSpeech.QUEUE_FLUSH,
                null,
                "MIRA_reply"
            )
        } catch (e: Exception) {
            isProcessing = false
            handler.postDelayed({ startListening() }, 1000)
        }
    }

    override fun onInit(status: Int) {

        if (status == TextToSpeech.SUCCESS) {

            ttsReady = true

            val result = tts?.setLanguage(
                Locale.forLanguageTag(languageTag)
            )

            if (result == TextToSpeech.LANG_MISSING_DATA ||
                result == TextToSpeech.LANG_NOT_SUPPORTED
            ) {
                tts?.language = Locale("en", "IN")
            }

            tts?.setOnUtteranceProgressListener(
                object : UtteranceProgressListener() {

                    override fun onStart(utteranceId: String?) {
                    }

                    override fun onDone(utteranceId: String?) {
                        handler.post {
                            isProcessing = false
                            broadcastStatus("Say MIRA to speak again")
                            handler.postDelayed({
                                startListening()
                            }, 700)
                        }
                    }

                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) {
                        handler.post {
                            isProcessing = false
                            handler.postDelayed({
                                startListening()
                            }, 1000)
                        }
                    }
                }
            )

            pendingReply?.let {
                val reply = it
                pendingReply = null
                speakReply(reply)
            }
        }
    }

    private fun broadcastStatus(message: String) {

        val intent = Intent(ACTION_STATUS).apply {
            setPackage(packageName)
            putExtra(EXTRA_TEXT, message)
        }

        sendBroadcast(intent)
    }

    private fun createNotificationChannel() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            val channel = NotificationChannel(
                CHANNEL_ID,
                "MIRA Voice Assistant",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Voice assistant listening notification"
            }

            val manager = getSystemService(
                NotificationManager::class.java
            )

            manager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {

        handler.removeCallbacksAndMessages(null)

        try {
            recognizer?.cancel()
            recognizer?.destroy()
            recognizer = null

            tts?.stop()
            tts?.shutdown()
            tts = null

        } catch (_: Exception) {
        }

        broadcastStatus("MIRA voice service stopped")

        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}
