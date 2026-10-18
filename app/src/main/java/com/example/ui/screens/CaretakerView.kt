package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.filled.Accessible
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.SupervisorAccount
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ConnectionStatus
import com.example.model.Direction
import com.example.model.WheelchairTelemetry
import com.example.ui.components.BatteryManager
import com.example.ui.components.DistanceChart
import com.example.ui.components.LiveWheelchairMap
import com.example.ui.components.UltrasonicRadar
import com.example.ui.theme.CalmingTeal
import com.example.ui.theme.EmergencyRed
import com.example.ui.theme.HighContrastAmber
import com.example.ui.theme.MedicalBluePrimary
import com.example.ui.theme.SafeGreen

@Composable
fun CaretakerView(
  telemetry: WheelchairTelemetry,
  showTakeoverConfirm: Boolean,
  onConfirmTakeover: () -> Unit,
  onDismissTakeoverConfirm: () -> Unit,
  onReleaseTakeover: () -> Unit,
  onDirectionCommand: (Direction) -> Unit,
  onEmergencyStop: () -> Unit,
  onResetEmergencyStop: () -> Unit,
  onSpeedPercentChange: (Float) -> Unit,
  onSimulateObstacle: (Direction, Float) -> Unit,
  onResetObstacles: () -> Unit,
  onTogglePower: () -> Unit,
  onReconnect: () -> Unit,
  onSetBatteryPercent: (Int) -> Unit = {},
  onToggleCharging: () -> Unit = {},
  onOpenBluetoothManager: () -> Unit = {},
  onReturnToRider: () -> Unit = {},
  modifier: Modifier = Modifier
) {
  // Takeover confirmation modal
  if (showTakeoverConfirm) {
    AlertDialog(
      onDismissRequest = onDismissTakeoverConfirm,
      icon = {
        Icon(
          Icons.Default.Security,
          contentDescription = "Takeover Confirmation",
          tint = HighContrastAmber,
          modifier = Modifier.size(36.dp)
        )
      },
      title = {
        Text(
          text = "Take Remote Control?",
          fontWeight = FontWeight.Bold,
          style = MaterialTheme.typography.titleMedium
        )
      },
      text = {
        Text(
          text = "This will immediately override rider inputs and grant remote steering authority to your caretaker device. A notification will appear on the rider's screen.",
          style = MaterialTheme.typography.bodyMedium
        )
      },
      confirmButton = {
        Button(
          onClick = onConfirmTakeover,
          colors = ButtonDefaults.buttonColors(containerColor = MedicalBluePrimary)
        ) {
          Text("Confirm Takeover")
        }
      },
      dismissButton = {
        OutlinedButton(onClick = onDismissTakeoverConfirm) {
          Text("Cancel")
        }
      }
    )
  }

  Column(
    modifier = modifier
      .fillMaxSize()
      .verticalScroll(rememberScrollState())
      .padding(12.dp)
      .testTag("caretaker_dashboard_screen"),
    verticalArrangement = Arrangement.spacedBy(14.dp)
  ) {
    // Return to Rider View Banner
    Card(
      modifier = Modifier
        .fillMaxWidth()
        .testTag("caretaker_return_to_rider_card"),
      shape = RoundedCornerShape(14.dp),
      colors = CardDefaults.cardColors(containerColor = MedicalBluePrimary.copy(alpha = 0.12f)),
      border = androidx.compose.foundation.BorderStroke(1.dp, MedicalBluePrimary.copy(alpha = 0.5f))
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(
          modifier = Modifier.weight(1f),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Icon(Icons.Default.SupervisorAccount, contentDescription = null, tint = MedicalBluePrimary, modifier = Modifier.size(22.dp))
          Spacer(modifier = Modifier.width(8.dp))
          Column {
            Text(
              text = "Caregiver Remote Dashboard",
              fontWeight = FontWeight.Black,
              fontSize = 13.sp,
              color = MaterialTheme.colorScheme.onSurface
            )
            Text(
              text = "Monitor telemetry, Google Maps & remote control",
              fontSize = 10.sp,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }
        Button(
          onClick = onReturnToRider,
          colors = ButtonDefaults.buttonColors(containerColor = MedicalBluePrimary),
          shape = RoundedCornerShape(8.dp),
          contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 6.dp)
        ) {
          Icon(Icons.Default.Accessible, contentDescription = null, modifier = Modifier.size(14.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("Rider View", fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
      }
    }
    // 1. Telemetry Status Panel Card
    CaretakerStatusPanel(
      telemetry = telemetry,
      onTogglePower = onTogglePower,
      onReconnect = onReconnect
    )

    // 2. Battery Manager with Radial Progress & Low-Battery Alert
    BatteryManager(
      telemetry = telemetry,
      onSetBatteryPercent = onSetBatteryPercent,
      onToggleCharging = onToggleCharging
    )

    // 2. Active Alerts Warning Box (if any alerts)
    if (telemetry.activeAlerts.isNotEmpty()) {
      Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = EmergencyRed.copy(alpha = 0.15f)),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, EmergencyRed)
      ) {
        Column(modifier = Modifier.padding(14.dp)) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Warning, contentDescription = "Alerts", tint = EmergencyRed)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = "ACTIVE SAFETY ALERTS (${telemetry.activeAlerts.size})",
              fontWeight = FontWeight.Black,
              fontSize = 13.sp,
              color = EmergencyRed
            )
          }
          Spacer(modifier = Modifier.height(6.dp))
          telemetry.activeAlerts.forEach { alert ->
            Text(
              text = "• $alert",
              fontSize = 12.sp,
              fontWeight = FontWeight.SemiBold,
              color = MaterialTheme.colorScheme.onSurface
            )
          }
        }
      }
    }

    // Bluetooth Hardware Manager Window
    Card(
      modifier = Modifier
        .fillMaxWidth()
        .clickable { onOpenBluetoothManager() }
        .testTag("caretaker_open_bluetooth_card"),
      shape = RoundedCornerShape(16.dp),
      colors = CardDefaults.cardColors(containerColor = CalmingTeal.copy(alpha = 0.12f)),
      border = androidx.compose.foundation.BorderStroke(1.dp, CalmingTeal)
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(
          modifier = Modifier.weight(1f),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Icon(Icons.Default.Bluetooth, contentDescription = "Bluetooth Window", tint = CalmingTeal, modifier = Modifier.size(26.dp))
          Spacer(modifier = Modifier.width(10.dp))
          Column {
            Text(
              text = "Bluetooth Wheelchair Manager",
              fontWeight = FontWeight.Bold,
              fontSize = 13.sp,
              color = MaterialTheme.colorScheme.onSurface
            )
            Text(
              text = if (telemetry.isConnected) "Connected: ${telemetry.deviceName} • Tap for GATT info & control" else "Disconnected • Tap to scan & connect",
              fontSize = 11.sp,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }

        Button(
          onClick = onOpenBluetoothManager,
          colors = ButtonDefaults.buttonColors(containerColor = CalmingTeal),
          shape = RoundedCornerShape(8.dp)
        ) {
          Text("Open Window", fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
      }
    }

    // Live Wheelchair Map with Real-Time Mobile Location & Google Maps
    LiveWheelchairMap(
      currentLocation = telemetry.currentLocation,
      tripHistory = telemetry.tripHistory,
      speedKmh = telemetry.speedKmh,
      latitude = telemetry.latitude,
      longitude = telemetry.longitude,
      accuracyMeters = telemetry.gpsAccuracyMeters,
      locationAddress = telemetry.locationAddress
    )

    // 4. Remote Control Panel (Priority: Emergency Stop > Caretaker Override > Rider)
    Card(
      modifier = Modifier
        .fillMaxWidth()
        .testTag("caretaker_remote_panel"),
      shape = RoundedCornerShape(20.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
      border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
    ) {
      Column(modifier = Modifier.padding(16.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column {
            Text(
              text = "Caretaker Remote Override",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSurface
            )
            Text(
              text = if (telemetry.isCaretakerOverriding) "OVERRIDE ACTIVE (Overrides rider inputs)" else "Standby (Rider has control)",
              style = MaterialTheme.typography.bodySmall,
              fontWeight = FontWeight.SemiBold,
              color = if (telemetry.isCaretakerOverriding) HighContrastAmber else MaterialTheme.colorScheme.onSurfaceVariant
            )
          }

          Switch(
            checked = telemetry.isCaretakerOverriding,
            onCheckedChange = { checked ->
              if (checked) onConfirmTakeover() else onReleaseTakeover()
            },
            colors = SwitchDefaults.colors(
              checkedThumbColor = Color.White,
              checkedTrackColor = HighContrastAmber
            )
          )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Large Red EMERGENCY STOP Button (Overrides Everything)
        Button(
          onClick = {
            if (telemetry.isEmergencyStopped) onResetEmergencyStop() else onEmergencyStop()
          },
          modifier = Modifier
            .fillMaxWidth()
            .height(58.dp)
            .testTag("caretaker_emergency_stop_button"),
          shape = RoundedCornerShape(16.dp),
          colors = ButtonDefaults.buttonColors(
            containerColor = if (telemetry.isEmergencyStopped) SafeGreen else EmergencyRed
          )
        ) {
          Icon(
            imageVector = if (telemetry.isEmergencyStopped) Icons.Default.Check else Icons.Default.Stop,
            contentDescription = "Emergency Stop",
            tint = Color.White
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = if (telemetry.isEmergencyStopped) "RESET EMERGENCY STOP" else "MASTER EMERGENCY STOP",
            fontSize = 15.sp,
            fontWeight = FontWeight.Black,
            color = Color.White
          )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Speed Limit Slider
        Text(
          text = "Speed Governor: ${(telemetry.speedPercentSetting * 100).toInt()}% (${"%.1f".format(telemetry.maxSpeedKmh * telemetry.speedPercentSetting)} km/h max)",
          style = MaterialTheme.typography.bodyMedium,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onSurface
        )
        Slider(
          value = telemetry.speedPercentSetting,
          onValueChange = onSpeedPercentChange,
          valueRange = 0.1f..1.0f,
          colors = SliderDefaults.colors(
            thumbColor = CalmingTeal,
            activeTrackColor = CalmingTeal
          ),
          modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Remote Directional D-Pad
        Text(
          text = "Remote Directional Pad:",
          style = MaterialTheme.typography.bodySmall,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))

        Column(
          modifier = Modifier.fillMaxWidth(),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Button(
            onClick = { onDirectionCommand(Direction.FORWARD) },
            modifier = Modifier.size(62.dp),
            shape = RoundedCornerShape(16.dp),
            enabled = telemetry.isCaretakerOverriding && !telemetry.isEmergencyStopped,
            colors = ButtonDefaults.buttonColors(containerColor = MedicalBluePrimary)
          ) {
            Icon(Icons.Default.ArrowUpward, contentDescription = "Forward")
          }

          Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Button(
              onClick = { onDirectionCommand(Direction.LEFT) },
              modifier = Modifier.size(62.dp),
              shape = RoundedCornerShape(16.dp),
              enabled = telemetry.isCaretakerOverriding && !telemetry.isEmergencyStopped,
              colors = ButtonDefaults.buttonColors(containerColor = MedicalBluePrimary)
            ) {
              Icon(Icons.Default.ArrowBack, contentDescription = "Left")
            }

            Button(
              onClick = { onDirectionCommand(Direction.STOP) },
              modifier = Modifier.size(62.dp),
              shape = RoundedCornerShape(16.dp),
              enabled = telemetry.isCaretakerOverriding,
              colors = ButtonDefaults.buttonColors(containerColor = EmergencyRed)
            ) {
              Icon(Icons.Default.Stop, contentDescription = "Halt")
            }

            Button(
              onClick = { onDirectionCommand(Direction.RIGHT) },
              modifier = Modifier.size(62.dp),
              shape = RoundedCornerShape(16.dp),
              enabled = telemetry.isCaretakerOverriding && !telemetry.isEmergencyStopped,
              colors = ButtonDefaults.buttonColors(containerColor = MedicalBluePrimary)
            ) {
              Icon(Icons.Default.ArrowForward, contentDescription = "Right")
            }
          }

          Button(
            onClick = { onDirectionCommand(Direction.BACKWARD) },
            modifier = Modifier.size(62.dp),
            shape = RoundedCornerShape(16.dp),
            enabled = telemetry.isCaretakerOverriding && !telemetry.isEmergencyStopped,
            colors = ButtonDefaults.buttonColors(containerColor = MedicalBluePrimary)
          ) {
            Icon(Icons.Default.ArrowDownward, contentDescription = "Reverse")
          }
        }
      }
    }

    // 5. Ultrasonic Proximity Radar
    UltrasonicRadar(
      obstacles = telemetry.obstacles,
      onSimulateObstacle = onSimulateObstacle,
      onResetObstacles = onResetObstacles
    )

    // 6. Distance Tracker & Daily Activity Chart
    DistanceChart(
      dailyStats = telemetry.dailyStats,
      todayKm = telemetry.distanceTodayKm,
      weekKm = telemetry.distanceWeekKm,
      totalKm = telemetry.distanceTotalKm
    )
  }
}

@Composable
private fun CaretakerStatusPanel(
  telemetry: WheelchairTelemetry,
  onTogglePower: () -> Unit,
  onReconnect: () -> Unit
) {
  Card(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(20.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column {
          Text(
            text = "SmartChair Telemetry Status",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
          )
          Text(
            text = telemetry.deviceName,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedButton(
            onClick = onTogglePower,
            shape = RoundedCornerShape(10.dp)
          ) {
            Icon(Icons.Default.PowerSettingsNew, contentDescription = "Power", modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(if (telemetry.isPowerOn) "Power ON" else "Power OFF", fontSize = 11.sp)
          }

          if (telemetry.connectionStatus != ConnectionStatus.CONNECTED) {
            Button(
              onClick = onReconnect,
              shape = RoundedCornerShape(10.dp),
              colors = ButtonDefaults.buttonColors(containerColor = CalmingTeal)
            ) {
              Icon(Icons.Default.Refresh, contentDescription = "Reconnect", modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("Connect", fontSize = 11.sp)
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      // Status Metrics Grid (Battery, Connection, Mode, Speed)
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        StatusPill(
          icon = Icons.Default.BatteryFull,
          label = "Battery",
          value = "${telemetry.batteryPercent}%",
          color = if (telemetry.batteryPercent <= 20) EmergencyRed else SafeGreen,
          modifier = Modifier.weight(1f)
        )

        StatusPill(
          icon = Icons.Default.Bluetooth,
          label = "Link RSSI",
          value = if (telemetry.isConnected) "${telemetry.connectionRssi} dBm" else "Offline",
          color = if (telemetry.isConnected) CalmingTeal else EmergencyRed,
          modifier = Modifier.weight(1f)
        )

        StatusPill(
          icon = Icons.Default.Speed,
          label = "Speed",
          value = "%.1f km/h".format(telemetry.speedKmh),
          color = MedicalBluePrimary,
          modifier = Modifier.weight(1f)
        )
      }
    }
  }
}

@Composable
private fun StatusPill(
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  label: String,
  value: String,
  color: Color,
  modifier: Modifier = Modifier
) {
  Column(
    modifier = modifier
      .clip(RoundedCornerShape(12.dp))
      .background(color.copy(alpha = 0.12f))
      .border(1.dp, color.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
      .padding(10.dp)
  ) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      Icon(icon, contentDescription = label, tint = color, modifier = Modifier.size(16.dp))
      Spacer(modifier = Modifier.width(4.dp))
      Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    Spacer(modifier = Modifier.height(4.dp))
    Text(value, fontSize = 14.sp, fontWeight = FontWeight.Black, color = color)
  }
}
