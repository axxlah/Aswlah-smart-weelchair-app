package com.example.model

enum class UserRole {
  RIDER,
  CARETAKER
}

enum class ControlMode {
  GYROSCOPE_TILT,
  EYE_TRACKING,
  VOICE,
  JOYSTICK
}

enum class Direction {
  FORWARD,
  BACKWARD,
  LEFT,
  RIGHT,
  STOP
}

enum class ConnectionStatus {
  DISCONNECTED,
  SCANNING,
  CONNECTING,
  CONNECTED
}

data class BleDeviceInfo(
  val name: String,
  val macAddress: String,
  val rssi: Int,
  val isConnected: Boolean = false,
  val serviceUuids: List<String> = listOf("0xFFE0", "0x180F")
)

data class ObstacleSensors(
  val frontCm: Float = 150f,
  val backCm: Float = 180f,
  val leftCm: Float = 120f,
  val rightCm: Float = 140f,
  val hazardThresholdCm: Float = 35f,
  val warningThresholdCm: Float = 75f
) {
  fun isHazardInDirection(direction: Direction): Boolean {
    return when (direction) {
      Direction.FORWARD -> frontCm <= hazardThresholdCm
      Direction.BACKWARD -> backCm <= hazardThresholdCm
      Direction.LEFT -> leftCm <= hazardThresholdCm
      Direction.RIGHT -> rightCm <= hazardThresholdCm
      Direction.STOP -> false
    }
  }

  fun hasAnyHazard(): Boolean {
    return frontCm <= hazardThresholdCm ||
        backCm <= hazardThresholdCm ||
        leftCm <= hazardThresholdCm ||
        rightCm <= hazardThresholdCm
  }
}

data class LocationPoint(
  val x: Float,
  val y: Float,
  val headingDegrees: Float = 0f,
  val timestamp: Long = System.currentTimeMillis()
)

data class DailyDistance(
  val day: String,
  val distanceKm: Float
)

data class WheelchairTelemetry(
  val isPowerOn: Boolean = true,
  val isConnected: Boolean = true,
  val connectionStatus: ConnectionStatus = ConnectionStatus.CONNECTED,
  val connectionRssi: Int = -55,
  val deviceName: String = "SmartChair-BT42 (Auto-Linked)",
  val batteryPercent: Int = 88,
  val batteryVoltage: Float = 25.4f,
  val batteryTempC: Float = 28.5f,
  val estimatedRangeKm: Float = 18.5f,
  val estimatedRemainingMinutes: Int = 220,
  val isCharging: Boolean = false,
  val isDriveEngaged: Boolean = true, // Default to drive active for direct testing
  val isEmergencyStopped: Boolean = false,
  val isCaretakerOverriding: Boolean = false,
  val currentRole: UserRole = UserRole.RIDER,
  val activeMode: ControlMode = ControlMode.GYROSCOPE_TILT,
  val currentDirection: Direction = Direction.STOP,
  val speedKmh: Float = 0f,
  val maxSpeedKmh: Float = 6.5f,
  val speedPercentSetting: Float = 0.5f,
  val obstacles: ObstacleSensors = ObstacleSensors(),

  // Real Mobile GPS Location (Google Maps access through mobile)
  val latitude: Double = 37.774929,
  val longitude: Double = -122.419416,
  val altitudeMeters: Double = 16.5,
  val gpsAccuracyMeters: Float = 3.2f,
  val locationAddress: String = "789 Mission St, San Francisco, CA",
  val isGpsActive: Boolean = true,

  val currentLocation: LocationPoint = LocationPoint(x = 250f, y = 250f, headingDegrees = 0f),
  val tripHistory: List<LocationPoint> = listOf(
    LocationPoint(250f, 250f, 0f)
  ),
  val distanceTodayKm: Float = 2.45f,
  val distanceWeekKm: Float = 16.8f,
  val distanceTotalKm: Float = 148.2f,
  val dailyStats: List<DailyDistance> = listOf(
    DailyDistance("Mon", 2.8f),
    DailyDistance("Tue", 3.1f),
    DailyDistance("Wed", 1.9f),
    DailyDistance("Thu", 3.5f),
    DailyDistance("Fri", 2.2f),
    DailyDistance("Sat", 2.5f),
    DailyDistance("Sun", 0.8f)
  ),
  val activeAlerts: List<String> = emptyList(),

  // Gyroscope / Tablet Tilt Steering state
  val pitchDegrees: Float = 0f, // tilt forward (negative) / backward (positive)
  val rollDegrees: Float = 0f,  // tilt left (negative) / right (positive)
  val tiltThresholdDegrees: Float = 12f,
  val tiltNeutralCalibratedPitch: Float = 0f,
  val tiltNeutralCalibratedRoll: Float = 0f,

  // Eye tracking specific state
  val gazeZone: Direction? = null,
  val dwellProgress: Float = 0f, // 0.0 to 1.0
  val blinkCountRecent: Int = 0,
  val gazeCursorX: Float = 0.5f, // normalized 0..1
  val gazeCursorY: Float = 0.5f, // normalized 0..1
  val isCalibrated: Boolean = true,

  // Voice command specific state
  val isVoiceListening: Boolean = false,
  val voiceTranscript: String = "Ready for command",
  val lastCommand: String = "None",

  // Bluetooth & BLE Information and Control
  val bleMacAddress: String = "EC:64:C9:82:1A:4F",
  val bleFirmwareVersion: String = "v2.4.1 (CANopen DS402)",
  val bleMtu: Int = 247,
  val bleLatencyMs: Int = 14,
  val bleServiceUuid: String = "0000FFE0-0000-1000-8000-00805F9B34FB",
  val bleMotionCharUuid: String = "0000FFE1-0000-1000-8000-00805F9B34FB",
  val bleTelemetryCharUuid: String = "0000FFE2-0000-1000-8000-00805F9B34FB",
  val bleBatteryCharUuid: String = "00002A19-0000-1000-8000-00805F9B34FB",
  val blePacketLogs: List<String> = listOf(
    "[BLE TX] 0xAA 0x01 0x80 0x55 -> CMD: FORWARD @ 50%",
    "[BLE RX] 0x55 0x18 0x0F 0xAA <- BATTERY: 88%, TEMP: 28.5C",
    "[BLE RX] 0x55 0x02 0x96 0xAA <- ULTRASONIC: F:150cm B:180cm L:120cm R:140cm",
    "[BLE TX] 0xAA 0x00 0x00 0x55 -> CMD: STOP (ACK 0x06)",
    "[BLE RX] 0x55 0xODOM 0x0A 0xAA <- ENCODER: L:14200 R:14205"
  ),
  val availableBleDevices: List<BleDeviceInfo> = listOf(
    BleDeviceInfo("SmartChair-BT42 (Active)", "EC:64:C9:82:1A:4F", -52, isConnected = true),
    BleDeviceInfo("SmartChair-Pro (Station 2)", "F4:12:FA:90:3B:11", -76, isConnected = false),
    BleDeviceInfo("ESP32-CAN-Bridge", "30:AE:A4:05:8B:2A", -64, isConnected = false)
  )
)
