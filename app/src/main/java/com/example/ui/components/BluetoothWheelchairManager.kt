package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothConnected
import androidx.compose.material.icons.filled.BluetoothSearching
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.model.BleDeviceInfo
import com.example.model.ConnectionStatus
import com.example.model.Direction
import com.example.model.WheelchairTelemetry
import com.example.ui.theme.CalmingTeal
import com.example.ui.theme.EmergencyRed
import com.example.ui.theme.HighContrastAmber
import com.example.ui.theme.MedicalBluePrimary
import com.example.ui.theme.SafeGreen

/**
 * BluetoothWheelchairManager:
 * A dedicated window/dialog to connect, control, and access all hardware information
 * on the smart wheelchair through Bluetooth BLE.
 */
@Composable
fun BluetoothWheelchairManager(
  telemetry: WheelchairTelemetry,
  onConnectDevice: (BleDeviceInfo) -> Unit,
  onDisconnect: () -> Unit,
  onSendBleCommand: (String) -> Unit,
  onDismiss: () -> Unit,
  modifier: Modifier = Modifier
) {
  var selectedTab by remember { mutableStateOf(0) }
  var isScanning by remember { mutableStateOf(false) }

  Dialog(onDismissRequest = onDismiss) {
    Card(
      modifier = modifier
        .fillMaxWidth()
        .padding(10.dp)
        .testTag("bluetooth_wheelchair_manager_dialog"),
      shape = RoundedCornerShape(24.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
      border = androidx.compose.foundation.BorderStroke(1.5.dp, CalmingTeal)
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(16.dp)
      ) {
        // 1. Header with Close button
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
              modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(if (telemetry.isConnected) CalmingTeal else EmergencyRed.copy(alpha = 0.2f)),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = if (telemetry.isConnected) Icons.Default.BluetoothConnected else Icons.Default.Bluetooth,
                contentDescription = "Bluetooth Status",
                tint = if (telemetry.isConnected) Color.White else EmergencyRed,
                modifier = Modifier.size(20.dp)
              )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
              Text(
                text = "Bluetooth Wheelchair Manager",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
              )
              Text(
                text = if (telemetry.isConnected) "Paired to ${telemetry.deviceName}" else "Not Connected",
                style = MaterialTheme.typography.bodySmall,
                color = if (telemetry.isConnected) SafeGreen else EmergencyRed
              )
            }
          }

          IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Default.Close, contentDescription = "Close", tint = MaterialTheme.colorScheme.onSurface)
          }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 2. Navigation Tabs (Connect & Scan, Hardware Info, Direct Control, Packet Log)
        TabRow(
          selectedTabIndex = selectedTab,
          containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
          contentColor = MaterialTheme.colorScheme.primary,
          modifier = Modifier.clip(RoundedCornerShape(12.dp))
        ) {
          Tab(
            selected = selectedTab == 0,
            onClick = { selectedTab = 0 },
            text = { Text("Connect", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
          )
          Tab(
            selected = selectedTab == 1,
            onClick = { selectedTab = 1 },
            text = { Text("Info & GATT", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
          )
          Tab(
            selected = selectedTab == 2,
            onClick = { selectedTab = 2 },
            text = { Text("BLE Control", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
          )
          Tab(
            selected = selectedTab == 3,
            onClick = { selectedTab = 3 },
            text = { Text("Raw Log", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
          )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 3. Tab Contents
        when (selectedTab) {
          0 -> {
            // Connect & Scan Tab
            ConnectAndScanTab(
              telemetry = telemetry,
              isScanning = isScanning,
              onStartScan = { isScanning = true },
              onConnectDevice = onConnectDevice,
              onDisconnect = onDisconnect
            )
          }
          1 -> {
            // Hardware Info & GATT Services Tab
            HardwareInfoTab(telemetry = telemetry)
          }
          2 -> {
            // Direct BLE Control Tab
            DirectBleControlTab(
              telemetry = telemetry,
              onSendCommand = onSendBleCommand
            )
          }
          3 -> {
            // Raw Packet Log Terminal
            PacketLogTerminalTab(telemetry = telemetry)
          }
        }
      }
    }
  }
}

@Composable
private fun ConnectAndScanTab(
  telemetry: WheelchairTelemetry,
  isScanning: Boolean,
  onStartScan: () -> Unit,
  onConnectDevice: (BleDeviceInfo) -> Unit,
  onDisconnect: () -> Unit
) {
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .height(340.dp)
      .verticalScroll(rememberScrollState())
  ) {
    // Current Active Link Card
    Card(
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(14.dp),
      colors = CardDefaults.cardColors(
        containerColor = if (telemetry.isConnected) CalmingTeal.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant
      ),
      border = androidx.compose.foundation.BorderStroke(
        1.dp,
        if (telemetry.isConnected) CalmingTeal else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
      )
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column {
          Text(
            text = telemetry.deviceName,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp
          )
          Text(
            text = "MAC: ${telemetry.bleMacAddress} • ${telemetry.connectionRssi} dBm",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }

        if (telemetry.isConnected) {
          OutlinedButton(
            onClick = onDisconnect,
            shape = RoundedCornerShape(8.dp)
          ) {
            Text("Disconnect", fontSize = 11.sp, color = EmergencyRed)
          }
        } else {
          Button(
            onClick = {
              onConnectDevice(
                BleDeviceInfo("SmartChair-BT42 (Auto-Linked)", "EC:64:C9:82:1A:4F", -55, isConnected = true)
              )
            },
            colors = ButtonDefaults.buttonColors(containerColor = CalmingTeal),
            shape = RoundedCornerShape(8.dp)
          ) {
            Text("Connect", fontSize = 11.sp)
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(14.dp))

    // Discovered BLE Peripherals List
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text(
        text = "Nearby BLE Wheelchairs (${telemetry.availableBleDevices.size})",
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold
      )

      OutlinedButton(
        onClick = onStartScan,
        shape = RoundedCornerShape(8.dp)
      ) {
        Icon(Icons.Default.Refresh, contentDescription = "Scan", modifier = Modifier.size(14.dp))
        Spacer(modifier = Modifier.width(4.dp))
        Text(if (isScanning) "Scanning..." else "Scan BLE", fontSize = 11.sp)
      }
    }

    Spacer(modifier = Modifier.height(8.dp))

    telemetry.availableBleDevices.forEach { device ->
      Card(
        modifier = Modifier
          .fillMaxWidth()
          .padding(vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(10.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Bluetooth, contentDescription = null, tint = CalmingTeal, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Column {
              Text(device.name, fontWeight = FontWeight.Bold, fontSize = 12.sp)
              Text("MAC: ${device.macAddress} • ${device.rssi} dBm", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
          }

          Button(
            onClick = { onConnectDevice(device) },
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(
              containerColor = if (device.isConnected) SafeGreen else MedicalBluePrimary
            )
          ) {
            Text(if (device.isConnected) "Active" else "Pair", fontSize = 10.sp, fontWeight = FontWeight.Bold)
          }
        }
      }
    }
  }
}

@Composable
private fun HardwareInfoTab(telemetry: WheelchairTelemetry) {
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .height(340.dp)
      .verticalScroll(rememberScrollState()),
    verticalArrangement = Arrangement.spacedBy(8.dp)
  ) {
    Text(
      text = "Onboard Wheelchair Telemetry & Hardware Spec",
      style = MaterialTheme.typography.titleSmall,
      fontWeight = FontWeight.Bold,
      color = MaterialTheme.colorScheme.onSurface
    )

    InfoRow(label = "Hardware Model", value = "SmartChair Dynamic Drive V2")
    InfoRow(label = "Firmware Revision", value = telemetry.bleFirmwareVersion)
    InfoRow(label = "Bluetooth MAC", value = telemetry.bleMacAddress)
    InfoRow(label = "CANbus Bus ID", value = "0x580 (CANopen Profile 402)")
    InfoRow(label = "BLE MTU Payload", value = "${telemetry.bleMtu} Bytes")
    InfoRow(label = "Connection Latency", value = "${telemetry.bleLatencyMs} ms (High Priority)")
    InfoRow(label = "Motion Service UUID", value = telemetry.bleServiceUuid, isCode = true)
    InfoRow(label = "Motion Command Char", value = telemetry.bleMotionCharUuid, isCode = true)
    InfoRow(label = "Telemetry Char", value = telemetry.bleTelemetryCharUuid, isCode = true)
    InfoRow(label = "Battery Service UUID", value = telemetry.bleBatteryCharUuid, isCode = true)
  }
}

@Composable
private fun DirectBleControlTab(
  telemetry: WheelchairTelemetry,
  onSendCommand: (String) -> Unit
) {
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .height(340.dp)
      .verticalScroll(rememberScrollState())
  ) {
    Text(
      text = "Direct Bluetooth GATT Command Injector",
      style = MaterialTheme.typography.titleSmall,
      fontWeight = FontWeight.Bold
    )
    Text(
      text = "Sends verified byte sequences to the wheelchair controller over BLE Characteristic 0xFFE1",
      style = MaterialTheme.typography.bodySmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant
    )

    Spacer(modifier = Modifier.height(14.dp))

    // Direct Quick Command Buttons
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      Button(
        onClick = { onSendCommand("0xAA 0x01 0x80 0x55 (MOVE FORWARD 50%)") },
        modifier = Modifier.weight(1f),
        shape = RoundedCornerShape(10.dp),
        colors = ButtonDefaults.buttonColors(containerColor = MedicalBluePrimary)
      ) {
        Text("TX Forward", fontSize = 11.sp)
      }

      Button(
        onClick = { onSendCommand("0xAA 0x00 0x00 0x55 (NORMAL STOP)") },
        modifier = Modifier.weight(1f),
        shape = RoundedCornerShape(10.dp),
        colors = ButtonDefaults.buttonColors(containerColor = HighContrastAmber)
      ) {
        Text("TX Stop", fontSize = 11.sp, color = Color.Black)
      }
    }

    Spacer(modifier = Modifier.height(8.dp))

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      Button(
        onClick = { onSendCommand("0xAA 0x03 0x80 0x55 (TURN LEFT 50%)") },
        modifier = Modifier.weight(1f),
        shape = RoundedCornerShape(10.dp),
        colors = ButtonDefaults.buttonColors(containerColor = MedicalBluePrimary)
      ) {
        Text("TX Left", fontSize = 11.sp)
      }

      Button(
        onClick = { onSendCommand("0xAA 0x04 0x80 0x55 (TURN RIGHT 50%)") },
        modifier = Modifier.weight(1f),
        shape = RoundedCornerShape(10.dp),
        colors = ButtonDefaults.buttonColors(containerColor = MedicalBluePrimary)
      ) {
        Text("TX Right", fontSize = 11.sp)
      }
    }

    Spacer(modifier = Modifier.height(12.dp))

    // Master Emergency E-Brake Byte
    Button(
      onClick = { onSendCommand("0xEE 0xFF 0x00 0xEE (EMERGENCY BRAKE HARD CUT)") },
      modifier = Modifier
        .fillMaxWidth()
        .height(48.dp),
      shape = RoundedCornerShape(12.dp),
      colors = ButtonDefaults.buttonColors(containerColor = EmergencyRed)
    ) {
      Icon(Icons.Default.Stop, contentDescription = null, tint = Color.White)
      Spacer(modifier = Modifier.width(6.dp))
      Text("TX EMERGENCY BRAKE (0xEE)", fontWeight = FontWeight.Black, fontSize = 12.sp)
    }
  }
}

@Composable
private fun PacketLogTerminalTab(telemetry: WheelchairTelemetry) {
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .height(340.dp)
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text(
        text = "BLE Packet Console Feed",
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold
      )
      Badge(containerColor = SafeGreen) {
        Text("LIVE 10Hz", color = Color.Black, fontSize = 9.sp, fontWeight = FontWeight.Bold)
      }
    }

    Spacer(modifier = Modifier.height(8.dp))

    Box(
      modifier = Modifier
        .fillMaxWidth()
        .weight(1f)
        .clip(RoundedCornerShape(12.dp))
        .background(Color(0xFF070B14))
        .border(1.dp, CalmingTeal.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
        .padding(10.dp)
        .verticalScroll(rememberScrollState())
    ) {
      Column {
        telemetry.blePacketLogs.forEach { log ->
          Text(
            text = log,
            fontFamily = FontFamily.Monospace,
            fontSize = 10.sp,
            color = if (log.contains("TX")) CalmingTeal else if (log.contains("BRAKE") || log.contains("STOP")) EmergencyRed else SafeGreen,
            lineHeight = 15.sp
          )
        }
      }
    }
  }
}

@Composable
private fun InfoRow(label: String, value: String, isCode: Boolean = false) {
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(8.dp))
      .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
      .padding(horizontal = 10.dp, vertical = 6.dp)
  ) {
    Text(
      text = label,
      fontSize = 10.sp,
      fontWeight = FontWeight.Bold,
      color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Text(
      text = value,
      fontSize = if (isCode) 9.sp else 12.sp,
      fontFamily = if (isCode) FontFamily.Monospace else FontFamily.Default,
      fontWeight = FontWeight.SemiBold,
      color = if (isCode) CalmingTeal else MaterialTheme.colorScheme.onSurface
    )
  }
}
