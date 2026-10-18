package com.example.service

import com.example.model.ConnectionStatus
import com.example.model.ControlMode
import com.example.model.DailyDistance
import com.example.model.Direction
import com.example.model.LocationPoint
import com.example.model.ObstacleSensors
import com.example.model.UserRole
import com.example.model.WheelchairTelemetry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin

/**
 * ============================================================================
 * REAL HARDWARE INTEGRATION GUIDE (HOW TO PLUG IN REAL PHYSICAL HARDWARE):
 * ============================================================================
 * To replace this simulation with a physical smart wheelchair:
 *
 * 1. BLUETOOTH LOW ENERGY (BLE) GATT SERVICE:
 *    - Connect to the wheelchair micro-controller (e.g. ESP32, STM32, or Arduino)
 *      running a BLE peripheral advertising a custom Service UUID (e.g., 0000FF00-...).
 *    - Write commands via sendCommand(direction, speed) to a Control Characteristic:
 *      Bytes: [HEADER 0xAA, DIR_BYTE, SPEED_PWM (0-255), CHECKSUM 0x55]
 *    - Subscribe to notifications on a Telemetry Characteristic for:
 *      [BATTERY %, SPEED, ULTRASONIC_FRONT_CM, BACK_CM, LEFT_CM, RIGHT_CM, STATUS_FLAGS]
 *
 * 2. SERIAL / CAN BUS / ROS2 MICRO-ROS:
 *    - Modern electric wheelchairs often use dynamic controls (Penny & Giles / Curtis /
 *      RoboClaw) via CAN bus (CANopen profile DS401/DS402).
 *    - An onboard USB-to-CAN bridge or micro-ROS bridge can subscribe to a geometry_msgs/Twist
 *      topic (`cmd_vel`) and publish wheel encoder odometry (`odom`).
 *
 * 3. ULTRASONIC SENSOR ARRAY:
 *    - Four HC-SR04 or waterproof JSN-SR04T sensors mounted on Front, Back, Left, Right
 *      connect to the GPIO pins. Distance readings update every 50ms and emit emergency
 *      interrupts whenever a distance < 35cm is detected.
 *
 * 4. AUTO-CONNECT BEHAVIOR:
 *    - The companion app runs an Android BluetoothLeScanner with a ScanFilter matching
 *      the wheelchair's advertised UUID. As soon as the wheelchair is switched on,
 *      advertising starts, triggering the onScanResult callback to connect immediately.
 * ============================================================================
 */
interface IWheelchairService {
  val telemetryFlow: StateFlow<WheelchairTelemetry>
  fun sendCommand(direction: Direction, speedPercent: Float = 0.5f, source: String = "rider")
  fun emergencyStop(reason: String = "Emergency Stop Triggered")
  fun resetEmergencyStop()
  fun setCaretakerOverride(override: Boolean)
  fun setControlMode(mode: ControlMode)
  fun setUserRole(role: UserRole)
  fun registerBlink()
  fun updateGazeCoordinates(normX: Float, normY: Float, activeZone: Direction?)
  fun processVoiceCommand(text: String): String
  fun handleJoystickInput(x: Float, y: Float)
  fun setObstacleOverride(direction: Direction, distanceCm: Float)
  fun resetObstacles()
  fun togglePower()
  fun reconnect()
  fun toggleDriveEngage()
  fun setBatteryPercent(percent: Int)
  fun toggleCharging()
  fun updateGpsLocation(lat: Double, lng: Double, alt: Double, acc: Float)
  fun updateTilt(pitch: Float, roll: Float, dir: Direction)
  fun connectBleDevice(device: com.example.model.BleDeviceInfo)
  fun disconnectBleDevice()
  fun sendBleRawCommand(commandStr: String)
}

class WheelchairService(
  private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default),
  private val autoStartSimulation: Boolean = true
) : IWheelchairService {

  private val _telemetry = MutableStateFlow(WheelchairTelemetry())
  override val telemetryFlow: StateFlow<WheelchairTelemetry> = _telemetry.asStateFlow()

  private var simulationJob: Job? = null
  private var lastBlinkTime: Long = 0L
  private var dwellStartTime: Long = 0L
  private var dwellZoneCandidate: Direction? = null

  init {
    if (autoStartSimulation) {
      startSimulationLoop()
    }
  }

  fun stopSimulation() {
    simulationJob?.cancel()
    simulationJob = null
  }

  private fun startSimulationLoop() {
    simulationJob?.cancel()
    simulationJob = scope.launch {
      var tick = 0
      while (isActive) {
        delay(100) // 10Hz simulation update tick
        tick++
        updateSimulationState(tick)
      }
    }
  }

  private fun updateSimulationState(tick: Int) {
    _telemetry.update { current ->
      if (!current.isPowerOn || current.connectionStatus != ConnectionStatus.CONNECTED) {
        return@update current.copy(speedKmh = 0f, currentDirection = Direction.STOP)
      }

      // Check emergency stop and obstacle safety rule
      val isEStopped = current.isEmergencyStopped
      val targetDir = if (isEStopped) Direction.STOP else current.currentDirection
      val targetSpeedSetting = current.speedPercentSetting

      // Check obstacle proximity in current direction of motion
      val hasHazard = current.obstacles.isHazardInDirection(targetDir)
      val effectiveDir = if (hasHazard || !current.isDriveEngaged) Direction.STOP else targetDir

      val targetSpeed = when (effectiveDir) {
        Direction.STOP -> 0f
        Direction.FORWARD -> current.maxSpeedKmh * targetSpeedSetting
        Direction.BACKWARD -> current.maxSpeedKmh * targetSpeedSetting * 0.5f // slower in reverse
        Direction.LEFT -> current.maxSpeedKmh * targetSpeedSetting * 0.7f
        Direction.RIGHT -> current.maxSpeedKmh * targetSpeedSetting * 0.7f
      }

      // Smooth acceleration / deceleration
      val newSpeed = current.speedKmh + (targetSpeed - current.speedKmh) * 0.35f

      // Update location and heading
      var newHeading = current.currentLocation.headingDegrees
      if (newSpeed > 0.1f) {
        when (effectiveDir) {
          Direction.LEFT -> newHeading = (newHeading - 5f + 360f) % 360f
          Direction.RIGHT -> newHeading = (newHeading + 5f) % 360f
          else -> {}
        }
      }

      // Kinematics displacement: distance = speed * dt
      val dtHours = 0.1 / 3600.0 // 100ms in hours
      val distDeltaKm = (newSpeed * dtHours).toFloat()
      val rad = Math.toRadians(newHeading.toDouble())
      val moveDirSign = if (effectiveDir == Direction.BACKWARD) -1.0 else 1.0

      // Map coordinate scale (1km ~ 500 units)
      val dx = (sin(rad) * moveDirSign * newSpeed * 0.8).toFloat()
      val dy = (-cos(rad) * moveDirSign * newSpeed * 0.8).toFloat()

      // Clamp map boundaries
      val newX = (current.currentLocation.x + dx).coerceIn(20f, 480f)
      val newY = (current.currentLocation.y + dy).coerceIn(20f, 480f)
      val newLocation = LocationPoint(x = newX, y = newY, headingDegrees = newHeading)

      // Accumulate trip history every 5 ticks (~500ms) if moving
      val updatedHistory = if (tick % 5 == 0 && newSpeed > 0.2f) {
        (current.tripHistory + newLocation).takeLast(60)
      } else {
        current.tripHistory
      }

      // Dynamic ultrasonic sensor variation (slight sensor jitter + simulated proximity)
      val jitter = ((tick % 7) - 3) * 0.5f
      val updatedObstacles = current.obstacles.copy(
        frontCm = (current.obstacles.frontCm + jitter).coerceIn(10f, 250f),
        backCm = (current.obstacles.backCm - jitter).coerceIn(10f, 250f),
        leftCm = (current.obstacles.leftCm + jitter * 0.5f).coerceIn(10f, 250f),
        rightCm = (current.obstacles.rightCm - jitter * 0.5f).coerceIn(10f, 250f)
      )

      // Active alert notifications
      val alerts = mutableListOf<String>()
      if (current.isEmergencyStopped) {
        alerts.add("EMERGENCY STOP ACTIVE")
      }
      if (hasHazard && targetDir != Direction.STOP) {
        alerts.add("OBSTACLE DETECTED! Motion halted by proximity safety")
      }
      if (current.isCaretakerOverriding) {
        alerts.add("CARETAKER HAS TAKEN OVER CONTROL")
      }
      if (current.batteryPercent <= 20) {
        alerts.add("LOW BATTERY WARNING (${current.batteryPercent}%)")
      }
      if (!current.isDriveEngaged && current.isPowerOn) {
        alerts.add("DRIVE IN STANDBY: Blink twice rapidly to engage drive")
      }

      current.copy(
        speedKmh = if (newSpeed < 0.05f) 0f else newSpeed,
        currentDirection = effectiveDir,
        currentLocation = newLocation,
        tripHistory = updatedHistory,
        distanceTodayKm = current.distanceTodayKm + distDeltaKm,
        distanceWeekKm = current.distanceWeekKm + distDeltaKm,
        distanceTotalKm = current.distanceTotalKm + distDeltaKm,
        obstacles = updatedObstacles,
        activeAlerts = alerts
      )
    }
  }

  override fun sendCommand(direction: Direction, speedPercent: Float, source: String) {
    _telemetry.update { current ->
      // Rule: Emergency Stop overrides everything
      if (current.isEmergencyStopped) {
        return@update current
      }

      // Rule: Caretaker override overrides rider input
      if (current.isCaretakerOverriding && source == "rider") {
        return@update current
      }

      // Proximity safety check
      if (current.obstacles.isHazardInDirection(direction) && direction != Direction.STOP) {
        val alerts = current.activeAlerts + "Obstacle in direction of travel! Command blocked."
        return@update current.copy(
          currentDirection = Direction.STOP,
          activeAlerts = alerts.distinct()
        )
      }

      current.copy(
        currentDirection = direction,
        speedPercentSetting = speedPercent.coerceIn(0.1f, 1.0f)
      )
    }
  }

  override fun emergencyStop(reason: String) {
    _telemetry.update { current ->
      current.copy(
        isEmergencyStopped = true,
        currentDirection = Direction.STOP,
        speedKmh = 0f,
        isDriveEngaged = false,
        activeAlerts = (current.activeAlerts + reason).distinct()
      )
    }
  }

  override fun resetEmergencyStop() {
    _telemetry.update { current ->
      current.copy(
        isEmergencyStopped = false,
        activeAlerts = current.activeAlerts.filter { !it.contains("EMERGENCY") }
      )
    }
  }

  override fun setCaretakerOverride(override: Boolean) {
    _telemetry.update { current ->
      current.copy(
        isCaretakerOverriding = override,
        // If caretaker takes over or releases, halt current motion for safety
        currentDirection = Direction.STOP
      )
    }
  }

  override fun setControlMode(mode: ControlMode) {
    _telemetry.update { current ->
      current.copy(
        activeMode = mode,
        currentDirection = Direction.STOP, // Safe transition
        dwellProgress = 0f,
        gazeZone = null
      )
    }
  }

  override fun setUserRole(role: UserRole) {
    _telemetry.update { current ->
      current.copy(currentRole = role)
    }
  }

  /**
   * User prompt requirement:
   * "the wheelchair will start if the rider blinks his eyes rapidly two time"
   */
  override fun registerBlink() {
    val now = System.currentTimeMillis()
    val diff = now - lastBlinkTime
    lastBlinkTime = now

    if (diff in 1..850) {
      // Rapid double blink confirmed!
      _telemetry.update { current ->
        val newDriveState = !current.isDriveEngaged
        val alert = if (newDriveState) "DRIVE ENGAGED: Double-blink detected" else "DRIVE DISENGAGED: Double-blink detected"
        current.copy(
          isDriveEngaged = newDriveState,
          blinkCountRecent = 2,
          activeAlerts = (current.activeAlerts + alert).distinct()
        )
      }
    } else {
      _telemetry.update { it.copy(blinkCountRecent = 1) }
    }
  }

  override fun toggleDriveEngage() {
    _telemetry.update { current ->
      val next = !current.isDriveEngaged
      current.copy(
        isDriveEngaged = next,
        currentDirection = if (!next) Direction.STOP else current.currentDirection
      )
    }
  }

  /**
   * Eye tracking dwell zone management:
   * Looking at on-screen zones (Forward, Back, Left, Right, Stop) for ~1 second triggers movement.
   */
  override fun updateGazeCoordinates(normX: Float, normY: Float, activeZone: Direction?) {
    val now = System.currentTimeMillis()

    if (activeZone == null) {
      dwellZoneCandidate = null
      dwellStartTime = 0L
      _telemetry.update {
        it.copy(
          gazeCursorX = normX,
          gazeCursorY = normY,
          gazeZone = null,
          dwellProgress = 0f
        )
      }
      return
    }

    if (dwellZoneCandidate != activeZone) {
      dwellZoneCandidate = activeZone
      dwellStartTime = now
      _telemetry.update {
        it.copy(
          gazeCursorX = normX,
          gazeCursorY = normY,
          gazeZone = activeZone,
          dwellProgress = 0.05f
        )
      }
    } else {
      val elapsedMs = now - dwellStartTime
      val dwellTargetMs = 1000f // 1 second dwell duration
      val progress = (elapsedMs / dwellTargetMs).coerceIn(0f, 1f)

      if (progress >= 1.0f) {
        // Dwell click triggered!
        sendCommand(activeZone, 0.5f, source = "rider")
        // Reset dwell progress to avoid continuous re-triggering
        dwellStartTime = now + 400L // 400ms refractory period
        _telemetry.update {
          it.copy(
            gazeCursorX = normX,
            gazeCursorY = normY,
            gazeZone = activeZone,
            dwellProgress = 1f
          )
        }
      } else {
        _telemetry.update {
          it.copy(
            gazeCursorX = normX,
            gazeCursorY = normY,
            gazeZone = activeZone,
            dwellProgress = progress
          )
        }
      }
    }
  }

  /**
   * Voice command recognition:
   * "forward", "back", "left", "right", "stop", "slow down", "speed up"
   * "Stop" must always work immediately.
   */
  override fun processVoiceCommand(text: String): String {
    val clean = text.trim().lowercase()
    var recognized = "Unrecognized"

    when {
      clean.contains("stop") || clean.contains("halt") || clean.contains("freeze") -> {
        sendCommand(Direction.STOP, 0f, source = "rider")
        recognized = "STOP"
      }
      clean.contains("forward") || clean.contains("front") || clean.contains("ahead") || clean.contains("go") -> {
        sendCommand(Direction.FORWARD, _telemetry.value.speedPercentSetting, source = "rider")
        recognized = "FORWARD"
      }
      clean.contains("back") || clean.contains("backward") || clean.contains("reverse") -> {
        sendCommand(Direction.BACKWARD, _telemetry.value.speedPercentSetting, source = "rider")
        recognized = "BACKWARD"
      }
      clean.contains("left") || clean.contains("turn left") -> {
        sendCommand(Direction.LEFT, _telemetry.value.speedPercentSetting, source = "rider")
        recognized = "LEFT"
      }
      clean.contains("right") || clean.contains("turn right") -> {
        sendCommand(Direction.RIGHT, _telemetry.value.speedPercentSetting, source = "rider")
        recognized = "RIGHT"
      }
      clean.contains("speed up") || clean.contains("faster") -> {
        val current = _telemetry.value.speedPercentSetting
        val newSpeed = (current + 0.15f).coerceAtMost(1.0f)
        _telemetry.update { it.copy(speedPercentSetting = newSpeed) }
        recognized = "SPEED UP (${(newSpeed * 100).toInt()}%)"
      }
      clean.contains("slow down") || clean.contains("slower") -> {
        val current = _telemetry.value.speedPercentSetting
        val newSpeed = (current - 0.15f).coerceAtLeast(0.2f)
        _telemetry.update { it.copy(speedPercentSetting = newSpeed) }
        recognized = "SLOW DOWN (${(newSpeed * 100).toInt()}%)"
      }
    }

    _telemetry.update {
      it.copy(
        voiceTranscript = text,
        lastCommand = recognized
      )
    }
    return recognized
  }

  override fun handleJoystickInput(x: Float, y: Float) {
    // x and y in [-1.0, 1.0]
    val mag = kotlin.math.sqrt(x * x + y * y).coerceIn(0f, 1f)
    if (mag < 0.15f) {
      sendCommand(Direction.STOP, 0f, source = "rider")
      return
    }

    val angleDeg = (Math.toDegrees(kotlin.math.atan2(y.toDouble(), x.toDouble())) + 360) % 360

    // Determine direction from angle
    val dir = when {
      angleDeg in 45.0..135.0 -> Direction.BACKWARD
      angleDeg in 135.0..225.0 -> Direction.LEFT
      angleDeg in 225.0..315.0 -> Direction.FORWARD
      else -> Direction.RIGHT
    }

    sendCommand(dir, mag, source = "rider")
  }

  override fun setObstacleOverride(direction: Direction, distanceCm: Float) {
    _telemetry.update { current ->
      val updated = when (direction) {
        Direction.FORWARD -> current.obstacles.copy(frontCm = distanceCm)
        Direction.BACKWARD -> current.obstacles.copy(backCm = distanceCm)
        Direction.LEFT -> current.obstacles.copy(leftCm = distanceCm)
        Direction.RIGHT -> current.obstacles.copy(rightCm = distanceCm)
        Direction.STOP -> current.obstacles
      }
      current.copy(obstacles = updated)
    }
  }

  override fun resetObstacles() {
    _telemetry.update {
      it.copy(obstacles = ObstacleSensors(frontCm = 150f, backCm = 180f, leftCm = 120f, rightCm = 140f))
    }
  }

  override fun togglePower() {
    _telemetry.update { current ->
      val newPower = !current.isPowerOn
      if (!newPower) {
        current.copy(
          isPowerOn = false,
          isConnected = false,
          connectionStatus = ConnectionStatus.DISCONNECTED,
          speedKmh = 0f,
          currentDirection = Direction.STOP,
          isDriveEngaged = false
        )
      } else {
        // Auto connect when powered on as requested
        current.copy(
          isPowerOn = true,
          connectionStatus = ConnectionStatus.CONNECTED,
          isConnected = true,
          isEmergencyStopped = false
        )
      }
    }
  }

  override fun reconnect() {
    scope.launch {
      _telemetry.update { it.copy(connectionStatus = ConnectionStatus.CONNECTING) }
      delay(800)
      _telemetry.update {
        it.copy(
          connectionStatus = ConnectionStatus.CONNECTED,
          isConnected = true,
          isPowerOn = true
        )
      }
    }
  }

  override fun setBatteryPercent(percent: Int) {
    _telemetry.update { current ->
      val clamped = percent.coerceIn(0, 100)
      val volt = 21.0f + (clamped / 100f) * 5.0f
      val range = (clamped / 100f) * 25.0f
      val remMin = ((clamped / 100f) * 300).toInt()
      current.copy(
        batteryPercent = clamped,
        batteryVoltage = volt,
        estimatedRangeKm = range,
        estimatedRemainingMinutes = remMin
      )
    }
  }

  override fun toggleCharging() {
    _telemetry.update { current ->
      val next = !current.isCharging
      current.copy(isCharging = next)
    }
  }

  override fun updateGpsLocation(lat: Double, lng: Double, alt: Double, acc: Float) {
    _telemetry.update { current ->
      current.copy(
        latitude = lat,
        longitude = lng,
        altitudeMeters = alt,
        gpsAccuracyMeters = acc,
        isGpsActive = true
      )
    }
  }

  override fun updateTilt(pitch: Float, roll: Float, dir: Direction) {
    _telemetry.update { current ->
      current.copy(
        pitchDegrees = pitch,
        rollDegrees = roll
      )
    }
    // If active mode is Gyroscope Tilt, automatically drive movement!
    if (_telemetry.value.activeMode == ControlMode.GYROSCOPE_TILT && _telemetry.value.isDriveEngaged) {
      sendCommand(dir, _telemetry.value.speedPercentSetting, source = "rider")
    }
  }

  override fun connectBleDevice(device: com.example.model.BleDeviceInfo) {
    _telemetry.update { current ->
      val updatedList = current.availableBleDevices.map {
        it.copy(isConnected = it.macAddress == device.macAddress)
      }
      val newLog = "[BLE LINK] Connected to ${device.name} [${device.macAddress}] @ ${device.rssi} dBm (MTU 247)"
      current.copy(
        isConnected = true,
        connectionStatus = ConnectionStatus.CONNECTED,
        deviceName = device.name,
        bleMacAddress = device.macAddress,
        connectionRssi = device.rssi,
        availableBleDevices = updatedList,
        blePacketLogs = (listOf(newLog) + current.blePacketLogs).take(25)
      )
    }
  }

  override fun disconnectBleDevice() {
    _telemetry.update { current ->
      val updatedList = current.availableBleDevices.map { it.copy(isConnected = false) }
      val newLog = "[BLE LINK] Disconnected from ${current.deviceName}"
      current.copy(
        isConnected = false,
        connectionStatus = ConnectionStatus.DISCONNECTED,
        speedKmh = 0f,
        currentDirection = Direction.STOP,
        availableBleDevices = updatedList,
        blePacketLogs = (listOf(newLog) + current.blePacketLogs).take(25)
      )
    }
  }

  override fun sendBleRawCommand(commandStr: String) {
    _telemetry.update { current ->
      val txLog = "[BLE TX] $commandStr"
      current.copy(
        blePacketLogs = (listOf(txLog) + current.blePacketLogs).take(25)
      )
    }

    // Process high-level intention if matching commands
    when {
      commandStr.contains("FORWARD") -> sendCommand(Direction.FORWARD, 0.5f, source = "caretaker")
      commandStr.contains("STOP") -> sendCommand(Direction.STOP, 0f, source = "caretaker")
      commandStr.contains("LEFT") -> sendCommand(Direction.LEFT, 0.5f, source = "caretaker")
      commandStr.contains("RIGHT") -> sendCommand(Direction.RIGHT, 0.5f, source = "caretaker")
      commandStr.contains("EMERGENCY") || commandStr.contains("0xEE") -> emergencyStop("BLE EMERGENCY BRAKE RECEIVED")
    }
  }
}
