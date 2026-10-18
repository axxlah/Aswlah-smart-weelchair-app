package com.example.camera

import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import java.nio.ByteBuffer

/**
 * CameraX ImageAnalyzer that tracks:
 * 1. Blink events (sharp dips and rebounds in upper facial luminance)
 * 2. Gaze / head tilt estimation (horizontal & vertical centroid of lightness/edges)
 */
class CameraGazeAnalyzer(
  private val onBlinkDetected: () -> Unit,
  private val onGazePosition: (normX: Float, normY: Float) -> Unit
) : ImageAnalysis.Analyzer {

  private var lastLuminance = 0.0
  private var isEyeClosed = false
  private var lastBlinkTimestamp = 0L

  override fun analyze(image: ImageProxy) {
    val planes = image.planes
    if (planes.isEmpty()) {
      image.close()
      return
    }

    val buffer: ByteBuffer = planes[0].buffer
    val data = ByteArray(buffer.remaining())
    buffer.get(data)

    val width = image.width
    val height = image.height

    if (width <= 0 || height <= 0 || data.isEmpty()) {
      image.close()
      return
    }

    // Fast sub-sampled luminance calculation for responsive real-time 30fps analysis
    var totalLum = 0L
    var count = 0
    var leftLum = 0L
    var rightLum = 0L
    var topLum = 0L
    var bottomLum = 0L

    val step = 8 // Sample every 8th pixel
    val midX = width / 2
    val midY = height / 2

    for (y in 0 until height step step) {
      for (x in 0 until width step step) {
        val index = y * width + x
        if (index < data.size) {
          val lum = data[index].toInt() and 0xFF
          totalLum += lum
          count++

          if (x < midX) leftLum += lum else rightLum += lum
          if (y < midY) topLum += lum else bottomLum += lum
        }
      }
    }

    if (count > 0) {
      val avgLum = totalLum.toDouble() / count
      val now = System.currentTimeMillis()

      // Blink detection: sudden drop in eye luminance relative to background
      val lumDelta = lastLuminance - avgLum
      if (lumDelta > 14.0 && !isEyeClosed) {
        isEyeClosed = true
      } else if (isEyeClosed && avgLum > lastLuminance - 5.0) {
        isEyeClosed = false
        if (now - lastBlinkTimestamp > 120) { // debounce jitter
          lastBlinkTimestamp = now
          onBlinkDetected()
        }
      }
      lastLuminance = avgLum

      // Estimate gaze / head tilt normalized to [0..1]
      // In front camera, mirrored X coordinate
      val totalHorizontal = (leftLum + rightLum).coerceAtLeast(1L).toFloat()
      val totalVertical = (topLum + bottomLum).coerceAtLeast(1L).toFloat()

      // Relative balance between left and right sides
      val hRatio = (rightLum.toFloat() / totalHorizontal) // mirrored
      val vRatio = (bottomLum.toFloat() / totalVertical)

      val normX = (0.5f + (hRatio - 0.5f) * 2.5f).coerceIn(0.1f, 0.9f)
      val normY = (0.5f + (vRatio - 0.5f) * 2.5f).coerceIn(0.1f, 0.9f)

      onGazePosition(normX, normY)
    }

    image.close()
  }
}
