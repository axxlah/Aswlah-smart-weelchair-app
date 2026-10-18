package com.example.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import com.example.model.Direction
import kotlin.math.abs

/**
 * DeviceTiltSensorManager:
 * Monitors tablet / phone Gyroscope & Accelerometer sensors to drive wheelchair motion:
 * - Tilt Forward / Up -> FORWARD
 * - Tilt Backward / Down -> BACKWARD
 * - Tilt Left -> LEFT
 * - Tilt Right -> RIGHT
 * - Level (within deadzone) -> STOP
 */
class DeviceTiltSensorManager(
  private val context: Context,
  private val onTiltChanged: (pitchDeg: Float, rollDeg: Float, dir: Direction) -> Unit
) : SensorEventListener {

  private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
  private val rotationSensor: Sensor? = sensorManager?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
    ?: sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

  private var isListening = false
  private var zeroPitch = 0f
  private var zeroRoll = 0f
  private var deadzoneDeg = 10f

  fun start() {
    if (isListening || sensorManager == null || rotationSensor == null) return
    sensorManager.registerListener(this, rotationSensor, SensorManager.SENSOR_DELAY_UI)
    isListening = true
  }

  fun stop() {
    if (!isListening || sensorManager == null) return
    sensorManager.unregisterListener(this)
    isListening = false
    onTiltChanged(0f, 0f, Direction.STOP)
  }

  fun calibrateZero(currentPitch: Float, currentRoll: Float) {
    zeroPitch = currentPitch
    zeroRoll = currentRoll
  }

  fun setDeadzone(degrees: Float) {
    deadzoneDeg = degrees.coerceIn(5f, 25f)
  }

  override fun onSensorChanged(event: SensorEvent) {
    var rawPitch = 0f
    var rawRoll = 0f

    if (event.sensor.type == Sensor.TYPE_ROTATION_VECTOR) {
      val rotationMatrix = FloatArray(9)
      SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
      val orientation = FloatArray(3)
      SensorManager.getOrientation(rotationMatrix, orientation)
      // orientation[1] is pitch in radians, orientation[2] is roll in radians
      rawPitch = Math.toDegrees(orientation[1].toDouble()).toFloat()
      rawRoll = Math.toDegrees(orientation[2].toDouble()).toFloat()
    } else if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
      val ax = event.values[0]
      val ay = event.values[1]
      val az = event.values[2]
      rawPitch = (Math.atan2(ay.toDouble(), az.toDouble()) * 180.0 / Math.PI).toFloat()
      rawRoll = (Math.atan2(-ax.toDouble(), Math.sqrt(ay * ay + az * az.toDouble())) * 180.0 / Math.PI).toFloat()
    }

    val effectivePitch = rawPitch - zeroPitch
    val effectiveRoll = rawRoll - zeroRoll

    // Determine direction based on dominant tilt axis outside the deadzone
    val absPitch = abs(effectivePitch)
    val absRoll = abs(effectiveRoll)

    val dir = when {
      absPitch < deadzoneDeg && absRoll < deadzoneDeg -> Direction.STOP
      absPitch >= absRoll -> {
        // Dominant tilt is pitch (tilt forward/backward)
        // Tilting tablet forward (top edge pitched forward/downwards relative to neutral)
        if (effectivePitch < -deadzoneDeg) Direction.FORWARD else Direction.BACKWARD
      }
      else -> {
        // Dominant tilt is roll (tilt left/right)
        if (effectiveRoll > deadzoneDeg) Direction.RIGHT else Direction.LEFT
      }
    }

    onTiltChanged(effectivePitch, effectiveRoll, dir)
  }

  override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
