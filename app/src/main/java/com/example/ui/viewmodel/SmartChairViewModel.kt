package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.model.ControlMode
import com.example.model.Direction
import com.example.model.UserRole
import com.example.model.WheelchairTelemetry
import com.example.service.IWheelchairService
import com.example.service.WheelchairService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

enum class CalibrationStep {
  INTRO,
  CENTER,
  LOOK_LEFT,
  LOOK_RIGHT,
  LOOK_DOWN,
  DOUBLE_BLINK,
  COMPLETE
}

class SmartChairViewModel(
  private val wheelchairService: IWheelchairService = WheelchairService()
) : ViewModel() {

  val telemetry: StateFlow<WheelchairTelemetry> = wheelchairService.telemetryFlow
    .stateIn(viewModelScope, SharingStarted.Eagerly, WheelchairTelemetry())

  private val _highContrastMode = MutableStateFlow(false)
  val highContrastMode: StateFlow<Boolean> = _highContrastMode.asStateFlow()

  private val _showCaretakerConfirm = MutableStateFlow(false)
  val showCaretakerConfirm: StateFlow<Boolean> = _showCaretakerConfirm.asStateFlow()

  private val _showCalibrationDialog = MutableStateFlow(false)
  val showCalibrationDialog: StateFlow<Boolean> = _showCalibrationDialog.asStateFlow()

  private val _calibrationStep = MutableStateFlow(CalibrationStep.INTRO)
  val calibrationStep: StateFlow<CalibrationStep> = _calibrationStep.asStateFlow()

  private val _speechError = MutableStateFlow<String?>(null)
  val speechError: StateFlow<String?> = _speechError.asStateFlow()

  private val _showBluetoothDialog = MutableStateFlow(false)
  val showBluetoothDialog: StateFlow<Boolean> = _showBluetoothDialog.asStateFlow()

  fun toggleHighContrast() {
    _highContrastMode.update { !it }
  }

  fun switchRole(role: UserRole) {
    // Directly switch the view so caregiver menu is immediately accessible
    wheelchairService.setUserRole(role)
  }

  fun requestCaretakerTakeover() {
    _showCaretakerConfirm.value = true
  }

  fun confirmCaretakerTakeover() {
    _showCaretakerConfirm.value = false
    wheelchairService.setCaretakerOverride(true)
  }

  fun dismissCaretakerConfirm() {
    _showCaretakerConfirm.value = false
  }

  fun releaseCaretakerControl() {
    wheelchairService.setCaretakerOverride(false)
  }

  fun openBluetoothDialog() {
    _showBluetoothDialog.value = true
  }

  fun closeBluetoothDialog() {
    _showBluetoothDialog.value = false
  }

  fun connectBle(device: com.example.model.BleDeviceInfo) {
    wheelchairService.connectBleDevice(device)
  }

  fun disconnectBle() {
    wheelchairService.disconnectBleDevice()
  }

  fun sendBleCommand(cmd: String) {
    wheelchairService.sendBleRawCommand(cmd)
  }

  fun updateGps(lat: Double, lng: Double, alt: Double, acc: Float) {
    wheelchairService.updateGpsLocation(lat, lng, alt, acc)
  }

  fun updateTilt(pitch: Float, roll: Float, dir: Direction) {
    wheelchairService.updateTilt(pitch, roll, dir)
  }

  fun switchMode(mode: ControlMode) {
    wheelchairService.setControlMode(mode)
  }

  fun sendDirection(direction: Direction, source: String = "rider") {
    wheelchairService.sendCommand(direction, telemetry.value.speedPercentSetting, source)
  }

  fun triggerEmergencyStop() {
    wheelchairService.emergencyStop("EMERGENCY STOP BUTTON ENGAGED")
  }

  fun resetEmergencyStop() {
    wheelchairService.resetEmergencyStop()
  }

  fun registerBlink() {
    wheelchairService.registerBlink()
  }

  fun toggleDriveEngage() {
    wheelchairService.toggleDriveEngage()
  }

  fun updateGaze(normX: Float, normY: Float, zone: Direction?) {
    wheelchairService.updateGazeCoordinates(normX, normY, zone)
  }

  fun processVoiceCommand(text: String) {
    _speechError.value = null
    wheelchairService.processVoiceCommand(text)
  }

  fun setSpeechError(error: String) {
    _speechError.value = error
  }

  fun handleJoystick(x: Float, y: Float) {
    wheelchairService.handleJoystickInput(x, y)
  }

  fun setSpeedPercent(percent: Float) {
    wheelchairService.sendCommand(
      telemetry.value.currentDirection,
      percent,
      if (telemetry.value.currentRole == UserRole.CARETAKER) "caretaker" else "rider"
    )
  }

  fun setObstacleDistance(dir: Direction, cm: Float) {
    wheelchairService.setObstacleOverride(dir, cm)
  }

  fun resetObstacles() {
    wheelchairService.resetObstacles()
  }

  fun togglePower() {
    wheelchairService.togglePower()
  }

  fun reconnect() {
    wheelchairService.reconnect()
  }

  // Calibration flow
  fun startCalibration() {
    _calibrationStep.value = CalibrationStep.INTRO
    _showCalibrationDialog.value = true
  }

  fun nextCalibrationStep() {
    val next = when (_calibrationStep.value) {
      CalibrationStep.INTRO -> CalibrationStep.CENTER
      CalibrationStep.CENTER -> CalibrationStep.LOOK_LEFT
      CalibrationStep.LOOK_LEFT -> CalibrationStep.LOOK_RIGHT
      CalibrationStep.LOOK_RIGHT -> CalibrationStep.LOOK_DOWN
      CalibrationStep.LOOK_DOWN -> CalibrationStep.DOUBLE_BLINK
      CalibrationStep.DOUBLE_BLINK -> CalibrationStep.COMPLETE
      CalibrationStep.COMPLETE -> {
        _showCalibrationDialog.value = false
        CalibrationStep.INTRO
      }
    }
    _calibrationStep.value = next
  }

  fun dismissCalibration() {
    _showCalibrationDialog.value = false
    _calibrationStep.value = CalibrationStep.INTRO
  }

  fun setBatteryPercent(percent: Int) {
    wheelchairService.setBatteryPercent(percent)
  }

  fun toggleCharging() {
    wheelchairService.toggleCharging()
  }
}
