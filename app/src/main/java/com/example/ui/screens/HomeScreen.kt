package com.example.ui.screens

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Accessible
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Contrast
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.SupervisorAccount
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.location.MobileLocationManager
import com.example.model.ConnectionStatus
import com.example.model.UserRole
import com.example.ui.components.BatteryManager
import com.example.ui.components.BluetoothWheelchairManager
import com.example.ui.components.RadialBatteryGauge
import com.example.ui.theme.CalmingTeal
import com.example.ui.theme.EmergencyRed
import com.example.ui.theme.HighContrastAmber
import com.example.ui.theme.MedicalBluePrimary
import com.example.ui.theme.SafeGreen
import com.example.ui.viewmodel.SmartChairViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
  viewModel: SmartChairViewModel,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val telemetry by viewModel.telemetry.collectAsState()
  val highContrast by viewModel.highContrastMode.collectAsState()
  val showTakeoverConfirm by viewModel.showCaretakerConfirm.collectAsState()
  val showCalibrationDialog by viewModel.showCalibrationDialog.collectAsState()
  val showBluetoothDialog by viewModel.showBluetoothDialog.collectAsState()
  val calibrationStep by viewModel.calibrationStep.collectAsState()
  var showBatteryModal by remember { mutableStateOf(false) }

  // 1. Real-time GPS Location tracking through the user's mobile device
  val locationPermissionLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestMultiplePermissions()
  ) { _ -> }

  DisposableEffect(Unit) {
    locationPermissionLauncher.launch(
      arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
      )
    )
    val locManager = MobileLocationManager(context) { lat, lng, alt, acc ->
      viewModel.updateGps(lat, lng, alt, acc)
    }
    locManager.startLocationUpdates()

    onDispose {
      locManager.stopLocationUpdates()
    }
  }

  // 2. Calibration Dialog
  if (showCalibrationDialog) {
    CalibrationDialog(
      step = calibrationStep,
      onNextStep = { viewModel.nextCalibrationStep() },
      onDismiss = { viewModel.dismissCalibration() }
    )
  }

  // 3. Battery Manager Modal Dialog
  if (showBatteryModal) {
    Dialog(onDismissRequest = { showBatteryModal = false }) {
      BatteryManager(
        telemetry = telemetry,
        onSetBatteryPercent = { viewModel.setBatteryPercent(it) },
        onToggleCharging = { viewModel.toggleCharging() },
        showControls = true
      )
    }
  }

  // 4. Bluetooth Wheelchair Connection & Control Window
  if (showBluetoothDialog) {
    BluetoothWheelchairManager(
      telemetry = telemetry,
      onConnectDevice = { viewModel.connectBle(it) },
      onDisconnect = { viewModel.disconnectBle() },
      onSendBleCommand = { viewModel.sendBleCommand(it) },
      onDismiss = { viewModel.closeBluetoothDialog() }
    )
  }

  Scaffold(
    topBar = {
      TopAppBar(
        title = {
          Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                imageVector = Icons.Default.Accessible,
                contentDescription = "SmartChair",
                tint = CalmingTeal,
                modifier = Modifier.size(22.dp)
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = "SmartChair Control",
                fontWeight = FontWeight.Black,
                fontSize = 18.sp
              )
            }
            Text(
              text = if (telemetry.isConnected) "BT Paired: ${telemetry.deviceName}" else "Bluetooth Offline • Tap to Connect",
              fontSize = 10.sp,
              color = if (telemetry.isConnected) SafeGreen else EmergencyRed,
              modifier = Modifier.clickable { viewModel.openBluetoothDialog() }
            )
          }
        },
        actions = {
          // PROMINENT CAREGIVER MENU BUTTON
          Button(
            onClick = {
              if (telemetry.currentRole == UserRole.RIDER) {
                viewModel.switchRole(UserRole.CARETAKER)
              } else {
                viewModel.switchRole(UserRole.RIDER)
              }
            },
            colors = ButtonDefaults.buttonColors(
              containerColor = if (telemetry.currentRole == UserRole.CARETAKER) HighContrastAmber else MedicalBluePrimary
            ),
            shape = RoundedCornerShape(10.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 4.dp),
            modifier = Modifier.testTag("topbar_caregiver_menu_button")
          ) {
            Icon(
              imageVector = Icons.Default.SupervisorAccount,
              contentDescription = "Caregiver Menu",
              tint = if (telemetry.currentRole == UserRole.CARETAKER) Color.Black else Color.White,
              modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
              text = if (telemetry.currentRole == UserRole.CARETAKER) "Caregiver" else "Caregiver Menu",
              color = if (telemetry.currentRole == UserRole.CARETAKER) Color.Black else Color.White,
              fontSize = 11.sp,
              fontWeight = FontWeight.ExtraBold
            )
          }

          Spacer(modifier = Modifier.width(4.dp))

          // Bluetooth manager window button
          IconButton(
            onClick = { viewModel.openBluetoothDialog() },
            modifier = Modifier.size(38.dp)
          ) {
            Icon(
              imageVector = Icons.Default.Bluetooth,
              contentDescription = "Bluetooth Wheelchair Window",
              tint = if (telemetry.isConnected) CalmingTeal else EmergencyRed
            )
          }

          Spacer(modifier = Modifier.width(2.dp))

          // Battery pill (tap to open radial battery manager)
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
              .clip(RoundedCornerShape(8.dp))
              .background(if (telemetry.batteryPercent <= 20) EmergencyRed.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant)
              .border(
                1.dp,
                if (telemetry.batteryPercent <= 20) EmergencyRed else Color.Transparent,
                RoundedCornerShape(8.dp)
              )
              .clickable { showBatteryModal = true }
              .padding(horizontal = 6.dp, vertical = 4.dp)
          ) {
            Icon(
              imageVector = Icons.Default.BatteryFull,
              contentDescription = "Battery",
              tint = if (telemetry.batteryPercent <= 20) EmergencyRed else SafeGreen,
              modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(2.dp))
            Text(
              text = "${telemetry.batteryPercent}%",
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold,
              color = if (telemetry.batteryPercent <= 20) EmergencyRed else MaterialTheme.colorScheme.onSurface
            )
          }

          Spacer(modifier = Modifier.width(2.dp))

          // High-contrast toggle
          IconButton(
            onClick = { viewModel.toggleHighContrast() },
            modifier = Modifier.size(36.dp)
          ) {
            Icon(
              imageVector = Icons.Default.Contrast,
              contentDescription = "Toggle High Contrast",
              tint = if (highContrast) HighContrastAmber else MaterialTheme.colorScheme.onSurface
            )
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(
          containerColor = MaterialTheme.colorScheme.surface
        )
      )
    },
    bottomBar = {
      NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 6.dp,
        modifier = Modifier.testTag("bottom_navigation_bar")
      ) {
        NavigationBarItem(
          selected = telemetry.currentRole == UserRole.RIDER && !showBatteryModal,
          onClick = { viewModel.switchRole(UserRole.RIDER) },
          icon = { Icon(Icons.Default.Accessible, contentDescription = "Rider Controls") },
          label = { Text("Rider", fontSize = 10.sp, fontWeight = FontWeight.Bold) }
        )
        NavigationBarItem(
          selected = telemetry.currentRole == UserRole.CARETAKER && !showBatteryModal,
          onClick = { viewModel.switchRole(UserRole.CARETAKER) },
          icon = {
            BadgedBox(
              badge = {
                if (telemetry.isCaretakerOverriding) {
                  Badge(containerColor = HighContrastAmber) { Text("Active", fontSize = 8.sp, color = Color.Black) }
                }
              }
            ) {
              Icon(Icons.Default.SupervisorAccount, contentDescription = "Caregiver Menu")
            }
          },
          label = { Text("Caregiver", fontSize = 10.sp, fontWeight = FontWeight.Bold) }
        )
        NavigationBarItem(
          selected = showBluetoothDialog,
          onClick = { viewModel.openBluetoothDialog() },
          icon = {
            BadgedBox(
              badge = {
                if (telemetry.isConnected) {
                  Badge(containerColor = SafeGreen) { Text("BT", fontSize = 8.sp) }
                }
              }
            ) {
              Icon(Icons.Default.Bluetooth, contentDescription = "Bluetooth Chair")
            }
          },
          label = { Text("BT Window", fontSize = 10.sp, fontWeight = FontWeight.Bold) }
        )
        NavigationBarItem(
          selected = false,
          onClick = {
            if (telemetry.currentRole != UserRole.CARETAKER) {
              viewModel.switchRole(UserRole.CARETAKER)
            }
          },
          icon = { Icon(Icons.Default.Map, contentDescription = "Live GPS Map") },
          label = { Text("Live Map", fontSize = 10.sp, fontWeight = FontWeight.Bold) }
        )
        NavigationBarItem(
          selected = showBatteryModal,
          onClick = { showBatteryModal = true },
          icon = {
            BadgedBox(
              badge = {
                if (telemetry.batteryPercent <= 20) {
                  Badge(containerColor = EmergencyRed) { Text("!", fontSize = 8.sp) }
                }
              }
            ) {
              Icon(Icons.Default.BatteryFull, contentDescription = "Battery Manager")
            }
          },
          label = { Text("${telemetry.batteryPercent}% Power", fontSize = 10.sp, fontWeight = FontWeight.Bold) }
        )
      }
    },
    modifier = modifier.fillMaxSize()
  ) { innerPadding ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
    ) {
      // Role Switcher Bar (Rider vs Caretaker)
      RoleSwitcherBar(
        currentRole = telemetry.currentRole,
        isCaretakerOverriding = telemetry.isCaretakerOverriding,
        onRoleSelected = { role -> viewModel.switchRole(role) }
      )

      // Persistent Low-Battery Alert Bar in Main Dashboard UI
      AnimatedVisibility(
        visible = telemetry.batteryPercent <= 20 && !telemetry.isCharging,
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut()
      ) {
        Card(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .clickable { showBatteryModal = true }
            .testTag("dashboard_low_battery_alert"),
          shape = RoundedCornerShape(14.dp),
          colors = CardDefaults.cardColors(containerColor = EmergencyRed.copy(alpha = 0.16f)),
          border = androidx.compose.foundation.BorderStroke(1.5.dp, EmergencyRed)
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(10.dp),
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
              modifier = Modifier.size(52.dp),
              sizeDp = 52.dp
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
                text = "Est. range: %.1f km (~%d mins). Tap to manage.".format(
                  telemetry.estimatedRangeKm,
                  telemetry.estimatedRemainingMinutes
                ),
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurface
              )
            }
            Button(
              onClick = { viewModel.toggleCharging() },
              colors = ButtonDefaults.buttonColors(containerColor = EmergencyRed),
              shape = RoundedCornerShape(8.dp)
            ) {
              Text("Plug In", fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
          }
        }
      }

      // Main content switches directly between Rider and Caretaker
      Crossfade(
        targetState = telemetry.currentRole,
        label = "role_crossfade",
        modifier = Modifier.weight(1f)
      ) { role ->
        when (role) {
          UserRole.RIDER -> {
            RiderView(
              telemetry = telemetry,
              highContrast = highContrast,
              onSwitchMode = { mode -> viewModel.switchMode(mode) },
              onDirectionCommand = { dir -> viewModel.sendDirection(dir, source = "rider") },
              onEmergencyStop = { viewModel.triggerEmergencyStop() },
              onResetEmergencyStop = { viewModel.resetEmergencyStop() },
              onBlink = { viewModel.registerBlink() },
              onToggleDrive = { viewModel.toggleDriveEngage() },
              onGazeUpdate = { nx, ny, zone -> viewModel.updateGaze(nx, ny, zone) },
              onVoiceCommand = { cmd -> viewModel.processVoiceCommand(cmd) },
              onJoystickMove = { x, y -> viewModel.handleJoystick(x, y) },
              onTiltCommand = { p, r, dir -> viewModel.updateTilt(p, r, dir) },
              onStartCalibration = { viewModel.startCalibration() },
              onSimulateObstacle = { dir, cm -> viewModel.setObstacleDistance(dir, cm) },
              onResetObstacles = { viewModel.resetObstacles() },
              onToggleCharging = { viewModel.toggleCharging() },
              onSwitchToCaregiver = { viewModel.switchRole(UserRole.CARETAKER) },
              onOpenBluetoothManager = { viewModel.openBluetoothDialog() },
              onOpenBatteryManager = { showBatteryModal = true }
            )
          }

          UserRole.CARETAKER -> {
            CaretakerView(
              telemetry = telemetry,
              showTakeoverConfirm = showTakeoverConfirm,
              onConfirmTakeover = { viewModel.confirmCaretakerTakeover() },
              onDismissTakeoverConfirm = { viewModel.dismissCaretakerConfirm() },
              onReleaseTakeover = { viewModel.releaseCaretakerControl() },
              onDirectionCommand = { dir -> viewModel.sendDirection(dir, source = "caretaker") },
              onEmergencyStop = { viewModel.triggerEmergencyStop() },
              onResetEmergencyStop = { viewModel.resetEmergencyStop() },
              onSpeedPercentChange = { pct -> viewModel.setSpeedPercent(pct) },
              onSimulateObstacle = { dir, cm -> viewModel.setObstacleDistance(dir, cm) },
              onResetObstacles = { viewModel.resetObstacles() },
              onTogglePower = { viewModel.togglePower() },
              onReconnect = { viewModel.reconnect() },
              onSetBatteryPercent = { viewModel.setBatteryPercent(it) },
              onToggleCharging = { viewModel.toggleCharging() },
              onOpenBluetoothManager = { viewModel.openBluetoothDialog() },
              onReturnToRider = { viewModel.switchRole(UserRole.RIDER) }
            )
          }
        }
      }
    }
  }
}

@Composable
private fun RoleSwitcherBar(
  currentRole: UserRole,
  isCaretakerOverriding: Boolean,
  onRoleSelected: (UserRole) -> Unit
) {
  Card(
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 12.dp, vertical = 6.dp)
      .testTag("role_switcher_bar"),
    shape = RoundedCornerShape(14.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(4.dp),
      horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      RolePill(
        title = "Rider View",
        subtitle = "Tilt, Eye, Voice, Stick",
        icon = Icons.Default.Accessible,
        isSelected = currentRole == UserRole.RIDER,
        onClick = { onRoleSelected(UserRole.RIDER) },
        modifier = Modifier.weight(1f)
      )

      RolePill(
        title = "Caretaker View",
        subtitle = if (isCaretakerOverriding) "Override Active" else "Maps, BLE & Remote",
        icon = Icons.Default.SupervisorAccount,
        isSelected = currentRole == UserRole.CARETAKER,
        onClick = { onRoleSelected(UserRole.CARETAKER) },
        accentWarning = isCaretakerOverriding,
        modifier = Modifier.weight(1f)
      )
    }
  }
}

@Composable
private fun RolePill(
  title: String,
  subtitle: String,
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  isSelected: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  accentWarning: Boolean = false
) {
  val bgColor = when {
    isSelected && accentWarning -> HighContrastAmber.copy(alpha = 0.25f)
    isSelected -> MedicalBluePrimary
    else -> Color.Transparent
  }
  val contentColor = when {
    isSelected && accentWarning -> HighContrastAmber
    isSelected -> Color.White
    else -> MaterialTheme.colorScheme.onSurfaceVariant
  }

  Box(
    modifier = modifier
      .clip(RoundedCornerShape(10.dp))
      .background(bgColor)
      .clickable { onClick() }
      .padding(vertical = 8.dp, horizontal = 10.dp),
    contentAlignment = Alignment.Center
  ) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      Icon(
        imageVector = icon,
        contentDescription = title,
        tint = contentColor,
        modifier = Modifier.size(20.dp)
      )
      Spacer(modifier = Modifier.width(6.dp))
      Column {
        Text(
          text = title,
          fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
          fontSize = 12.sp,
          color = contentColor
        )
        Text(
          text = subtitle,
          fontSize = 9.sp,
          color = contentColor.copy(alpha = 0.85f)
        )
      }
    }
  }
}
