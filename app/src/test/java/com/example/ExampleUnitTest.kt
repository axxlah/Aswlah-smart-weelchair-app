package com.example

import com.example.model.BleDeviceInfo
import com.example.model.ControlMode
import com.example.model.Direction
import com.example.model.ObstacleSensors
import com.example.model.UserRole
import com.example.service.WheelchairService
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ExampleUnitTest {

  @Test
  fun testEmergencyStopOverridesEverything() = runTest {
    val service = WheelchairService(scope = this, autoStartSimulation = false)
    service.sendCommand(Direction.FORWARD, 0.8f, source = "rider")

    // Trigger Emergency Stop
    service.emergencyStop("Emergency button engaged")
    var telemetry = service.telemetryFlow.value
    assertTrue("Wheelchair must be emergency stopped", telemetry.isEmergencyStopped)
    assertEquals(Direction.STOP, telemetry.currentDirection)

    // Attempt rider command while emergency stopped - should remain stopped
    service.sendCommand(Direction.FORWARD, 0.5f, source = "rider")
    telemetry = service.telemetryFlow.value
    assertEquals(Direction.STOP, telemetry.currentDirection)

    // Reset emergency stop
    service.resetEmergencyStop()
    telemetry = service.telemetryFlow.value
    assertFalse(telemetry.isEmergencyStopped)
  }

  @Test
  fun testCaretakerOverrideBlocksRiderInput() = runTest {
    val service = WheelchairService(scope = this, autoStartSimulation = false)

    // Caretaker takes over
    service.setCaretakerOverride(true)
    var telemetry = service.telemetryFlow.value
    assertTrue("Caretaker override should be active", telemetry.isCaretakerOverriding)

    // Rider tries to command
    service.sendCommand(Direction.FORWARD, 0.6f, source = "rider")
    telemetry = service.telemetryFlow.value
    assertEquals(Direction.STOP, telemetry.currentDirection)

    // Caretaker commands forward
    service.sendCommand(Direction.FORWARD, 0.6f, source = "caretaker")
    telemetry = service.telemetryFlow.value
    assertEquals(Direction.FORWARD, telemetry.currentDirection)
  }

  @Test
  fun testUltrasonicObstacleAutoStop() = runTest {
    val service = WheelchairService(scope = this, autoStartSimulation = false)

    // Place obstacle 20cm ahead (hazard threshold is 35cm)
    service.setObstacleOverride(Direction.FORWARD, 20f)
    var telemetry = service.telemetryFlow.value
    assertTrue(telemetry.obstacles.isHazardInDirection(Direction.FORWARD))

    // Attempt forward motion - should be blocked
    service.sendCommand(Direction.FORWARD, 0.5f, source = "rider")
    telemetry = service.telemetryFlow.value
    assertEquals(Direction.STOP, telemetry.currentDirection)
  }

  @Test
  fun testVoiceCommands() = runTest {
    val service = WheelchairService(scope = this, autoStartSimulation = false)

    val res1 = service.processVoiceCommand("please go forward")
    assertEquals("FORWARD", res1)

    val res2 = service.processVoiceCommand("stop immediately")
    assertEquals("STOP", res2)
    assertEquals(Direction.STOP, service.telemetryFlow.value.currentDirection)
  }

  @Test
  fun testRapidDoubleBlinkEngagesDrive() = runTest {
    val service = WheelchairService(scope = this, autoStartSimulation = false)
    if (service.telemetryFlow.value.isDriveEngaged) {
      service.toggleDriveEngage()
    }
    assertFalse(service.telemetryFlow.value.isDriveEngaged)

    // First blink
    service.registerBlink()
    delay(10)
    // Second rapid blink
    service.registerBlink()

    assertTrue("Double blink should engage drive", service.telemetryFlow.value.isDriveEngaged)
  }

  @Test
  fun testBatteryMonitoringAndLowAlert() = runTest {
    val service = WheelchairService(scope = this, autoStartSimulation = false)
    assertEquals(88, service.telemetryFlow.value.batteryPercent)
    assertFalse(service.telemetryFlow.value.isCharging)

    // Set battery to low level (15%)
    service.setBatteryPercent(15)
    var telemetry = service.telemetryFlow.value
    assertEquals(15, telemetry.batteryPercent)
    assertTrue("Voltage should drop below 23V", telemetry.batteryVoltage < 23.0f)
    assertTrue("Estimated range should be less than 5km", telemetry.estimatedRangeKm < 5.0f)

    // Toggle charging
    service.toggleCharging()
    telemetry = service.telemetryFlow.value
    assertTrue("Chair should be in charging state", telemetry.isCharging)
  }

  @Test
  fun testTiltSteeringGyroscope() = runTest {
    val service = WheelchairService(scope = this, autoStartSimulation = false)
    service.setControlMode(ControlMode.GYROSCOPE_TILT)

    // Pitch forward -> FORWARD
    service.updateTilt(-20f, 0f, Direction.FORWARD)
    assertEquals(Direction.FORWARD, service.telemetryFlow.value.currentDirection)

    // Roll right -> RIGHT
    service.updateTilt(0f, 20f, Direction.RIGHT)
    assertEquals(Direction.RIGHT, service.telemetryFlow.value.currentDirection)

    // Level neutral -> STOP
    service.updateTilt(0f, 0f, Direction.STOP)
    assertEquals(Direction.STOP, service.telemetryFlow.value.currentDirection)
  }

  @Test
  fun testBluetoothManagerConnectAndCommands() = runTest {
    val service = WheelchairService(scope = this, autoStartSimulation = false)
    val testBleDevice = BleDeviceInfo("SmartChair-BT42", "EC:64:C9:82:1A:4F", -50, isConnected = false)

    service.connectBleDevice(testBleDevice)
    var telemetry = service.telemetryFlow.value
    assertTrue("Should be connected", telemetry.isConnected)
    assertEquals("SmartChair-BT42", telemetry.deviceName)

    // Send BLE Raw command
    service.sendBleRawCommand("0xAA 0x01 0x80 0x55 (MOVE FORWARD 50%)")
    telemetry = service.telemetryFlow.value
    assertEquals(Direction.FORWARD, telemetry.currentDirection)

    // Disconnect
    service.disconnectBleDevice()
    telemetry = service.telemetryFlow.value
    assertFalse("Should be disconnected", telemetry.isConnected)
    assertEquals(Direction.STOP, telemetry.currentDirection)
  }
}
