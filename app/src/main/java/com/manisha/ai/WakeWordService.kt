
package com.manisha.ai

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer

class WakeWordService : Service() {

    companion object {
        const val ACTION_STATUS = "com.manisha.ai.WAKE_STATUS"
        const val ACTION_COMMAND = "com.manisha.ai.WAKE_COMMAND"
        const val EXTRA_TEXT = "recognized_text"

        const val CHANNEL_ID = "manisha_wake_channel"
        const val NOTIFICATION_ID = 101
    }

    private val handler = Handler(Looper.getMainLooper())
    private var recognizer: SpeechRecognizer? = null
    private var listening = false
    private var waitingForCommand = false

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(
            NOTIFICATION_ID,
            createNotification()
        )
        startRecognition()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "MANISHA Voice Assistant",
                NotificationManager.IMPORTANCE_LOW
            )
            channel.description = "MANISHA background voice listening"

            getSystemService(NotificationManager::class.java)
                .createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, CHANNEL_ID)
                .setContentTitle("MANISHA is active")
                .setContentText("Listening for the wake word MANISHA")
                .setSmallIcon(android.R.drawable.ic_btn_speak_now)
                .setOngoing(true)
                .build()
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
                .setContentTitle("MANISHA is active")
                .setContentText("Listening for MANISHA")
                .setSmallIcon(android.R.drawable.ic_btn_speak_now)
                .setOngoing(true)
                .build()
        }
    }

    private fun updateStatus(message: String) {
        sendBroadcast(
            Intent(ACTION_STATUS).setPackage(packageName)
                .putExtra(EXTRA_TEXT, message)
        )
    }

    private fun startRecognition() {
        if (listening) return

        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            updateStatus("Speech recognition is unavailable.")
            return
        }

        try {
            if (recognizer == null) {
                recognizer = SpeechRecognizer.createSpeechRecognizer(this)

                recognizer?.setRecognitionListener(
                    object : RecognitionListener {
                        override fun onReadyForSpeech(params: android.os.Bundle?) {
                            listening = true
                        }

                        override fun onBeginningOfSpeech() {}

                        override fun onRmsChanged(rmsdB: Float) {}

                        override fun onBufferReceived(buffer: ByteArray?) {}

                        override fun onEndOfSpeech() {
                            listening = false
                        }

                        override fun onError(error: Int) {
                            listening = false
                            scheduleRecognition()
                        }

                        override fun onResults(results: android.os.Bundle?) {
                            listening = false

                            val spoken = results
                                ?.getStringArrayList(
                                    SpeechRecognizer.RESULTS_RECOGNITION
                                )
                                ?.firstOrNull()
                                .orEmpty()

                            handleSpeech(spoken)
                        }

                        override fun onPartialResults(
                            partialResults: android.os.Bundle?
                        ) {}

                        override fun onEvent(
                            eventType: Int,
                            params: android.os.Bundle?
                        ) {}
                    }
                )
            }

            val intent = Intent(
                RecognizerIntent.ACTION_RECOGNIZE_SPEECH
            ).apply {
                putExtra(
                    RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                )
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-IN")
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
                putExtra(
                    RecognizerIntent.EXTRA_PARTIAL_RESULTS,
                    false
                )
            }

            recognizer?.startListening(intent)
            listening = true

        } catch (_: Exception) {
            listening = false
            scheduleRecognition()
        }
    }

    private fun handleSpeech(spoken: String) {
        val text = spoken.trim()

        if (text.isBlank()) {
            scheduleRecognition()
            return
        }

        if (!waitingForCommand) {
            val normalized = text.lowercase()
            val wakeIndex = normalized.indexOf("manisha")

            if (wakeIndex >= 0) {
                val command = text
                    .substring(wakeIndex + "manisha".length)
                    .trim()

                waitingForCommand = true
                updateStatus("MANISHA activated. Listening for your command.")

                if (command.isNotBlank()) {
                    sendCommand(command)
                } else {
                    scheduleRecognition()
                }
            } else {
                scheduleRecognition()
            }
        } else {
            sendCommand(text)
        }
    }

    private fun sendCommand(command: String) {
        waitingForCommand = false

        sendBroadcast(
            Intent(ACTION_COMMAND).setPackage(packageName)
                .putExtra(EXTRA_TEXT, command)
        )

        updateStatus("Command received: $command")
        scheduleRecognition()
    }

    private fun scheduleRecognition() {
        listening = false
        handler.postDelayed({
            if (recognizer != null || !isDestroyed) {
                startRecognition()
            }
        }, 900)
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int
    ): Int {
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        recognizer?.destroy()
        recognizer = null
        listening = false
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
