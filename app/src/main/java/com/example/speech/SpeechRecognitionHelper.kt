package com.example.speech

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log

class SpeechRecognitionHelper(
  private val context: Context,
  private val onSpeechResult: (String) -> Unit,
  private val onListeningStateChanged: (Boolean) -> Unit,
  private val onError: (String) -> Unit
) {
  private var speechRecognizer: SpeechRecognizer? = null
  private var isListening = false

  init {
    try {
      if (SpeechRecognizer.isRecognitionAvailable(context)) {
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
          setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
              isListening = true
              onListeningStateChanged(true)
            }

            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {
              isListening = false
              onListeningStateChanged(false)
            }

            override fun onError(error: Int) {
              isListening = false
              onListeningStateChanged(false)
              val errorMsg = when (error) {
                SpeechRecognizer.ERROR_NO_MATCH -> "No speech detected"
                SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Speech timeout"
                SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
                SpeechRecognizer.ERROR_NETWORK -> "Network issue"
                else -> "Speech recognition paused"
              }
              onError(errorMsg)
            }

            override fun onResults(results: Bundle?) {
              isListening = false
              onListeningStateChanged(false)
              val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
              if (!matches.isNullOrEmpty()) {
                onSpeechResult(matches[0])
              }
            }

            override fun onPartialResults(partialResults: Bundle?) {
              val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
              if (!matches.isNullOrEmpty()) {
                onSpeechResult(matches[0])
              }
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}
          })
        }
      }
    } catch (e: Exception) {
      Log.e("SpeechHelper", "Failed to initialize SpeechRecognizer", e)
    }
  }

  fun startListening() {
    if (speechRecognizer == null) {
      onError("SpeechRecognizer unavailable on device")
      return
    }
    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
      putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
      putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
      putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
    }
    try {
      speechRecognizer?.startListening(intent)
      isListening = true
      onListeningStateChanged(true)
    } catch (e: Exception) {
      onError(e.localizedMessage ?: "Failed to start listening")
      isListening = false
      onListeningStateChanged(false)
    }
  }

  fun stopListening() {
    try {
      speechRecognizer?.stopListening()
    } catch (e: Exception) {
      Log.w("SpeechHelper", "Error stopping listening", e)
    }
    isListening = false
    onListeningStateChanged(false)
  }

  fun destroy() {
    try {
      speechRecognizer?.destroy()
    } catch (e: Exception) {
      Log.w("SpeechHelper", "Error destroying speechRecognizer", e)
    }
    speechRecognizer = null
  }
}
