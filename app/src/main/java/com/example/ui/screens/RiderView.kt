package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.PanTool
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RemoveRedEye
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.SupervisorAccount
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.model.ControlMode
import com.example.model.Direction
import com.example.model.WheelchairTelemetry
import com.example.speech.SpeechRecognitionHelper
import com.example.ui.components.CameraGazeOverlay
import com.example.ui.components.GyroscopeTiltScreen
import com.example.ui.components.RadialBatteryGauge
import com.example.ui.components.UltrasonicRadar
import com.example.ui.components.VirtualJoystick
import com.example.ui.theme.CalmingTeal
import com.example.ui.theme.EmergencyRed
import com.example.ui.theme.HighContrastAmber
import com.example.ui.theme.MedicalBluePrimary
import com.example.ui.theme.SafeGreen

@Composable
fun RiderView(
  telemetry: WheelchairTelemetry,
  highContrast: Boolean,
  onSwitchMode: (ControlMode) -> Unit,
  onDirectionCommand: (Direction) -> Unit,
  onEmergencyStop: () -> Unit,
  onResetEmergencyStop: () -> Unit,
  onBlink: () -> Unit,
  onToggleDrive: () -> Unit,
  onGazeUpdate: (Float, Float, Direction?) -> Unit,
  onVoiceCommand: (String) -> Unit,
  onJoystickMove: (Float, Float) -> Unit,
  onTiltCommand: (Float, Float, Direction) -> Unit = { _, _, _ -> },
  onStartCalibration: () -> Unit,
  onSimulateObstacle: (Direction, Float) -> Unit,
  onResetObstacles: () -> Unit,
  onToggleCharging: () -> Unit = {},
  onSwitchToCaregiver: () -> Unit = {},
  onOpenBluetoothManager: () -> Unit = {},
  onOpenBatteryManager: () -> Unit = {},
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  var isAudioListening by remember { mutableStateOf(false) }
  var speechHelper by remember { mutableStateOf<SpeechRecognitionHelper?>(null) }

  var hasAudioPermission by remember {
    mutableStateOf(
      ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
    )
  }

  val audioPermissionLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestPermission()
  ) { granted ->
    hasAudioPermission = granted
  }

  // Initialize Speech Helper for Voice Mode
  DisposableEffect(hasAudioPermission) {
    if (hasAudioPermission) {
      speechHelper = SpeechRecognitionHelper(
        context = context,
        onSpeechResult = { text ->
          onVoiceCommand(text)
        },
        onListeningStateChanged = { listening ->
          isAudioListening = listening
        },
        onError = { /* fallback */ }
      )
    }
    onDispose {
      speechHelper?.destroy()
    }
  }

  Column(
    modifier = modifier
      .fillMaxSize()
      .testTag("rider_view_screen")
  ) {
    // Caregiver Menu & Bluetooth Quick Access Banner
    Card(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 8.dp, vertical = 4.dp)
        .testTag("rider_quick_header_card"),
      shape = RoundedCornerShape(12.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Text(
            text = "Rider Controls",
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = "• ${telemetry.activeMode.name.replace('_', ' ')}",
            fontSize = 11.sp,
            color = CalmingTeal,
            fontWeight = FontWeight.SemiBold
          )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          Button(
            onClick = onOpenBluetoothManager,
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(containerColor = if (telemetry.isConnected) CalmingTeal else EmergencyRed),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 4.dp),
            modifier = Modifier.testTag("rider_bluetooth_button")
          ) {
            Icon(Icons.Default.Bluetooth, contentDescription = null, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(if (telemetry.isConnected) "BT Connected" else "Connect BT", fontSize = 11.sp, fontWeight = FontWeight.Bold)
          }

          Button(
            onClick = onSwitchToCaregiver,
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MedicalBluePrimary),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp),
            modifier = Modifier.testTag("rider_open_caregiver_menu_button")
          ) {
            Icon(Icons.Default.SupervisorAccount, contentDescription = null, modifier = Modifier.size(15.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Caregiver Menu", fontSize = 11.sp, fontWeight = FontWeight.Black)
          }
        }
      }
    }
    // 1. Caretaker Override Banner (Rule: Alert rider if caretaker has taken control)
    AnimatedVisibility(
      visible = telemetry.isCaretakerOverriding,
      enter = fadeIn(),
      exit = fadeOut()
    ) {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .background(HighContrastAmber)
          .padding(vertical = 10.dp, horizontal = 16.dp),
        contentAlignment = Alignment.Center
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(Icons.Default.Security, contentDescription = "Override", tint = Color.Black, modifier = Modifier.size(20.dp))
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = "CARETAKER IS CONTROLLING THE WHEELCHAIR",
            fontWeight = FontWeight.Black,
            fontSize = 13.sp,
            color = Color.Black
          )
        }
      }
    }

    // 2. Obstacle / Emergency Stop Alert Banner
    if (telemetry.isEmergencyStopped || telemetry.obstacles.isHazardInDirection(telemetry.currentDirection)) {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .background(EmergencyRed)
          .padding(vertical = 10.dp, horizontal = 16.dp),
        contentAlignment = Alignment.Center
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(Icons.Default.Warning, contentDescription = "Safety Alert", tint = Color.White, modifier = Modifier.size(22.dp))
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = if (telemetry.isEmergencyStopped) "EMERGENCY STOP TRIGGERED" else "OBSTACLE DETECTED! MOVEMENT BLOCKED",
            fontWeight = FontWeight.ExtraBold,
            fontSize = 13.sp,
            color = Color.White
          )
          if (telemetry.isEmergencyStopped) {
            Spacer(modifier = Modifier.width(12.dp))
            Button(
              onClick = onResetEmergencyStop,
              colors = ButtonDefaults.buttonColors(containerColor = Color.White),
              shape = RoundedCornerShape(8.dp)
            ) {
              Text("Reset", color = EmergencyRed, fontWeight = FontWeight.Bold, fontSize = 11.sp)
            }
          }
        }
      }
    }

    // Low Battery Radial Alert Banner
    AnimatedVisibility(
      visible = telemetry.batteryPercent <= 20 && !telemetry.isCharging,
      enter = fadeIn(),
      exit = fadeOut()
    ) {
      Card(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 8.dp, vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = EmergencyRed.copy(alpha = 0.18f)),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, EmergencyRed)
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          RadialBatteryGauge(
            percent = telemetry.batteryPercent,
            sweepAngle = (telemetry.batteryPercent / 100f) * 360f,
            isCharging = telemetry.isCharging,
            statusColor = EmergencyRed,
            isLowBattery = true,
            voltage = telemetry.batteryVoltage,
            estimatedRangeKm = telemetry.estimatedRangeKm,
            modifier = Modifier.size(72.dp),
            sizeDp = 72.dp
          )
          Spacer(modifier = Modifier.width(10.dp))
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = "LOW BATTERY ALERT (${telemetry.batteryPercent}%)",
              fontWeight = FontWeight.Black,
              fontSize = 12.sp,
              color = EmergencyRed
            )
            Text(
              text = "Est. %.1f km remaining. Please connect charger.".format(telemetry.estimatedRangeKm),
              fontSize = 10.sp,
              color = MaterialTheme.colorScheme.onSurface
            )
          }
          Button(
            onClick = onToggleCharging,
            colors = ButtonDefaults.buttonColors(containerColor = EmergencyRed),
            shape = RoundedCornerShape(8.dp)
          ) {
            Text("Charge", fontSize = 10.sp, fontWeight = FontWeight.Bold)
          }
        }
      }
    }

    // 3. Top Mode Switcher Tabs (Tilt, Eye-Tracking, Voice, Joystick)
    androidx.compose.material3.ScrollableTabRow(
      selectedTabIndex = telemetry.activeMode.ordinal,
      containerColor = MaterialTheme.colorScheme.surface,
      contentColor = MaterialTheme.colorScheme.primary,
      edgePadding = 8.dp,
      modifier = Modifier.fillMaxWidth()
    ) {
      Tab(
        selected = telemetry.activeMode == ControlMode.GYROSCOPE_TILT,
        onClick = { onSwitchMode(ControlMode.GYROSCOPE_TILT) },
        text = {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Sensors, contentDescription = "Tilt", modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Tablet Tilt", fontWeight = FontWeight.Bold, fontSize = 12.sp)
          }
        }
      )
      Tab(
        selected = telemetry.activeMode == ControlMode.EYE_TRACKING,
        onClick = { onSwitchMode(ControlMode.EYE_TRACKING) },
        text = {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.RemoveRedEye, contentDescription = "Eye", modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Eye-Tracking", fontWeight = FontWeight.Bold, fontSize = 12.sp)
          }
        }
      )
      Tab(
        selected = telemetry.activeMode == ControlMode.VOICE,
        onClick = { onSwitchMode(ControlMode.VOICE) },
        text = {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Mic, contentDescription = "Voice", modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Voice Mode", fontWeight = FontWeight.Bold, fontSize = 12.sp)
          }
        }
      )
      Tab(
        selected = telemetry.activeMode == ControlMode.JOYSTICK,
        onClick = { onSwitchMode(ControlMode.JOYSTICK) },
        text = {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.PanTool, contentDescription = "Joystick", modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Joystick", fontWeight = FontWeight.Bold, fontSize = 12.sp)
          }
        }
      )
    }

    // 4. Main Active Mode Area
    Box(
      modifier = Modifier
        .weight(1f)
        .fillMaxWidth()
        .padding(8.dp)
    ) {
      when (telemetry.activeMode) {
        ControlMode.GYROSCOPE_TILT -> {
          GyroscopeTiltScreen(
            telemetry = telemetry,
            onTiltCommand = onTiltCommand,
            onDirectDirection = onDirectionCommand,
            onToggleDrive = onToggleDrive
          )
        }

        ControlMode.EYE_TRACKING -> {
          // Dedicated CameraX Eye Tracking & Interactive Dwell Zones Screen
          CameraGazeOverlay(
            currentDirection = telemetry.currentDirection,
            activeDwellZone = telemetry.gazeZone,
            dwellProgress = telemetry.dwellProgress,
            gazeCursorX = telemetry.gazeCursorX,
            gazeCursorY = telemetry.gazeCursorY,
            isDriveEngaged = telemetry.isDriveEngaged,
            blinkCount = telemetry.blinkCountRecent,
            onBlinkDetected = onBlink,
            onGazeUpdated = onGazeUpdate,
            onDirectionSelected = onDirectionCommand,
            onStartCalibration = onStartCalibration,
            onToggleDrive = onToggleDrive
          )
        }

        ControlMode.VOICE -> {
          VoiceModeScreen(
            telemetry = telemetry,
            isListening = isAudioListening,
            hasPermission = hasAudioPermission,
            onRequestPermission = { audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO) },
            onStartListening = { speechHelper?.startListening() },
            onStopListening = { speechHelper?.stopListening() },
            onSimulateVoiceCommand = { cmd -> onVoiceCommand(cmd) },
            onEmergencyStop = onEmergencyStop
          )
        }

        ControlMode.JOYSTICK -> {
          JoystickModeScreen(
            telemetry = telemetry,
            onJoystickMove = onJoystickMove,
            onDirectionCommand = onDirectionCommand,
            onEmergencyStop = onEmergencyStop
          )
        }
      }
    }

    // 5. Persistent Accessible Bottom Bar: Large Emergency STOP + Status Indicator
    Card(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 12.dp, vertical = 8.dp)
        .testTag("rider_bottom_bar"),
      shape = RoundedCornerShape(16.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
      border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        // Status indicator
        Column {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
              modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(if (telemetry.isConnected) SafeGreen else EmergencyRed)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = if (telemetry.isDriveEngaged) "DRIVE ENGAGED" else "DRIVE PAUSED",
              fontWeight = FontWeight.ExtraBold,
              fontSize = 12.sp,
              color = if (telemetry.isDriveEngaged) SafeGreen else HighContrastAmber
            )
          }
          Text(
            text = "Dir: ${telemetry.currentDirection.name} • Speed: %.1f km/h".format(telemetry.speedKmh),
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }

        // Giant Red Emergency Stop Button
        Button(
          onClick = onEmergencyStop,
          modifier = Modifier
            .height(52.dp)
            .testTag("rider_emergency_stop_button"),
          shape = RoundedCornerShape(14.dp),
          colors = ButtonDefaults.buttonColors(containerColor = EmergencyRed)
        ) {
          Icon(Icons.Default.Stop, contentDescription = "Emergency Stop", tint = Color.White)
          Spacer(modifier = Modifier.width(6.dp))
          Text("EMERGENCY STOP", fontWeight = FontWeight.Black, fontSize = 13.sp)
        }
      }
    }
  }
}

/**
 * Voice Mode View:
 * Speech recognition with live transcript, listening indicator, quick trigger buttons,
 * and immediate STOP response.
 */
@Composable
private fun VoiceModeScreen(
  telemetry: WheelchairTelemetry,
  isListening: Boolean,
  hasPermission: Boolean,
  onRequestPermission: () -> Unit,
  onStartListening: () -> Unit,
  onStopListening: () -> Unit,
  onSimulateVoiceCommand: (String) -> Unit,
  onEmergencyStop: () -> Unit
) {
  Column(
    modifier = Modifier
      .fillMaxSize()
      .verticalScroll(rememberScrollState())
      .padding(8.dp),
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    // Listening indicator card
    Card(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(20.dp)),
      colors = CardDefaults.cardColors(
        containerColor = if (isListening) CalmingTeal.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface
      ),
      border = androidx.compose.foundation.BorderStroke(
        1.5.dp,
        if (isListening) CalmingTeal else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
      )
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        Box(
          modifier = Modifier
            .size(72.dp)
            .clip(CircleShape)
            .background(if (isListening) CalmingTeal else MaterialTheme.colorScheme.surfaceVariant)
            .clickable {
              if (!hasPermission) {
                onRequestPermission()
              } else if (isListening) {
                onStopListening()
              } else {
                onStartListening()
              }
            },
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = if (isListening) Icons.Default.Mic else Icons.Default.MicOff,
            contentDescription = "Microphone",
            tint = if (isListening) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(36.dp)
          )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
          text = if (isListening) "LISTENING FOR VOICE COMMAND..." else "TAP TO START VOICE RECOGNITION",
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Black,
          color = if (isListening) CalmingTeal else MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(6.dp))

        // Live transcript readout
        Text(
          text = "Heard: \"${telemetry.voiceTranscript}\"",
          style = MaterialTheme.typography.bodyMedium,
          fontWeight = FontWeight.Medium,
          color = MaterialTheme.colorScheme.primary,
          textAlign = TextAlign.Center
        )

        Text(
          text = "Recognized Command: ${telemetry.lastCommand}",
          style = MaterialTheme.typography.bodySmall,
          fontWeight = FontWeight.Bold,
          color = SafeGreen
        )
      }
    }

    Spacer(modifier = Modifier.height(16.dp))

    // Supported Voice Commands Grid (also serves as 1-tap simulation buttons for testing!)
    Text(
      text = "Supported Commands (Say aloud or tap to test):",
      style = MaterialTheme.typography.titleSmall,
      fontWeight = FontWeight.Bold,
      color = MaterialTheme.colorScheme.onSurface
    )

    Spacer(modifier = Modifier.height(10.dp))

    val commands = listOf(
      "FORWARD" to "front, go, ahead",
      "BACK" to "backward, reverse",
      "LEFT" to "turn left",
      "RIGHT" to "turn right",
      "STOP" to "immediate freeze",
      "SPEED UP" to "+15% speed",
      "SLOW DOWN" to "-15% speed"
    )

    commands.chunked(2).forEach { rowCmds ->
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        rowCmds.forEach { (cmd, hint) ->
          val isStop = cmd == "STOP"
          Button(
            onClick = {
              if (isStop) onEmergencyStop() else onSimulateVoiceCommand(cmd.lowercase())
            },
            modifier = Modifier
              .weight(1f)
              .height(64.dp)
              .testTag("voice_cmd_${cmd.lowercase()}"),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
              containerColor = if (isStop) EmergencyRed else MaterialTheme.colorScheme.surfaceVariant
            )
          ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
              Text(
                text = cmd,
                fontWeight = FontWeight.Black,
                fontSize = 14.sp,
                color = if (isStop) Color.White else MaterialTheme.colorScheme.onSurface
              )
              Text(
                text = hint,
                fontSize = 10.sp,
                color = if (isStop) Color.White.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }
        }
      }
      Spacer(modifier = Modifier.height(8.dp))
    }
  }
}

/**
 * Joystick Mode View:
 * Large virtual joystick + directional fallback buttons.
 */
@Composable
private fun JoystickModeScreen(
  telemetry: WheelchairTelemetry,
  onJoystickMove: (Float, Float) -> Unit,
  onDirectionCommand: (Direction) -> Unit,
  onEmergencyStop: () -> Unit
) {
  Column(
    modifier = Modifier
      .fillMaxSize()
      .padding(8.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center
  ) {
    Text(
      text = "Virtual Joystick Control",
      style = MaterialTheme.typography.titleMedium,
      fontWeight = FontWeight.Bold,
      color = MaterialTheme.colorScheme.onSurface
    )
    Text(
      text = "Drag stick for proportional speed & angle navigation",
      style = MaterialTheme.typography.bodySmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant
    )

    Spacer(modifier = Modifier.height(18.dp))

    // Virtual Joystick Widget
    VirtualJoystick(
      sizeDp = 220.dp,
      onMove = onJoystickMove,
      onRelease = { onDirectionCommand(Direction.STOP) }
    )

    Spacer(modifier = Modifier.height(20.dp))

    // Directional D-Pad Quick Fallback Buttons
    Row(
      horizontalArrangement = Arrangement.spacedBy(12.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Button(
        onClick = { onDirectionCommand(Direction.LEFT) },
        modifier = Modifier.size(56.dp),
        shape = RoundedCornerShape(14.dp)
      ) {
        Icon(Icons.Default.ArrowBack, contentDescription = "Left")
      }

      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(
          onClick = { onDirectionCommand(Direction.FORWARD) },
          modifier = Modifier.size(56.dp),
          shape = RoundedCornerShape(14.dp)
        ) {
          Icon(Icons.Default.ArrowUpward, contentDescription = "Forward")
        }
        Button(
          onClick = { onDirectionCommand(Direction.BACKWARD) },
          modifier = Modifier.size(56.dp),
          shape = RoundedCornerShape(14.dp)
        ) {
          Icon(Icons.Default.ArrowDownward, contentDescription = "Backward")
        }
      }

      Button(
        onClick = { onDirectionCommand(Direction.RIGHT) },
        modifier = Modifier.size(56.dp),
        shape = RoundedCornerShape(14.dp)
      ) {
        Icon(Icons.Default.ArrowForward, contentDescription = "Right")
      }
    }
  }
}
