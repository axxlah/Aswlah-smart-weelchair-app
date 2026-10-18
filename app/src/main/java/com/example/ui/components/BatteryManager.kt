package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Power
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.WheelchairTelemetry
import com.example.ui.theme.CalmingTeal
import com.example.ui.theme.EmergencyRed
import com.example.ui.theme.HighContrastAmber
import com.example.ui.theme.MedicalBluePrimary
import com.example.ui.theme.SafeGreen

/**
 * BatteryManager: Monitors the chair's power level and displays a low-battery alert
 * in the main dashboard UI using an animated radial progress indicator.
 */
@Composable
fun BatteryManager(
  telemetry: WheelchairTelemetry,
  onSetBatteryPercent: (Int) -> Unit,
  onToggleCharging: () -> Unit,
  modifier: Modifier = Modifier,
  showControls: Boolean = true
) {
  val isLowBattery = telemetry.batteryPercent <= 20
  val isCritical = telemetry.batteryPercent <= 10

  val batteryFraction = (telemetry.batteryPercent / 100f).coerceIn(0f, 1f)
  val animatedSweep by animateFloatAsState(
    targetValue = batteryFraction * 360f,
    animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
    label = "batterySweep"
  )

  // Pulsing animation for low battery alert
  val infiniteTransition = rememberInfiniteTransition(label = "pulse")
  val pulseAlpha by infiniteTransition.animateFloat(
    initialValue = 0.25f,
    targetValue = 0.85f,
    animationSpec = infiniteRepeatable(
      animation = tween(800, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "pulseAlpha"
  )

  val statusColor by animateColorAsState(
    targetValue = when {
      telemetry.isCharging -> CalmingTeal
      isLowBattery -> EmergencyRed
      telemetry.batteryPercent <= 50 -> HighContrastAmber
      else -> SafeGreen
    },
    label = "batteryStatusColor"
  )

  var showTestingSliders by remember { mutableStateOf(false) }

  Card(
    modifier = modifier
      .fillMaxWidth()
      .testTag("battery_manager_card"),
    shape = RoundedCornerShape(22.dp),
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.surface
    ),
    border = androidx.compose.foundation.BorderStroke(
      width = if (isLowBattery) 2.dp else 1.dp,
      color = if (isLowBattery) EmergencyRed else MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
    )
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      // 1. Header: Title and charging status pill
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column {
          Text(
            text = "Battery Power Manager",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
          )
          Text(
            text = if (telemetry.isCharging) "Charging via 24V Dock" else "24V Dual Deep-Cycle Pack",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }

        // Charging / Low Alert Badge
        Row(
          modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(statusColor.copy(alpha = if (isLowBattery) pulseAlpha else 0.18f))
            .border(1.dp, statusColor, RoundedCornerShape(10.dp))
            .clickable { onToggleCharging() }
            .padding(horizontal = 10.dp, vertical = 5.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Icon(
            imageVector = if (telemetry.isCharging) Icons.Default.ElectricBolt else if (isLowBattery) Icons.Default.BatteryAlert else Icons.Default.Power,
            contentDescription = "Power State",
            tint = if (isLowBattery && !telemetry.isCharging) Color.White else statusColor,
            modifier = Modifier.size(16.dp)
          )
          Spacer(modifier = Modifier.width(5.dp))
          Text(
            text = if (telemetry.isCharging) "CHARGING" else if (isLowBattery) "LOW BATTERY" else "NORMAL",
            fontSize = 11.sp,
            fontWeight = FontWeight.Black,
            color = if (isLowBattery && !telemetry.isCharging) Color.White else statusColor
          )
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      // 2. High-Visibility Low-Battery Alert Banner (Animated Visibility)
      AnimatedVisibility(
        visible = isLowBattery && !telemetry.isCharging,
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut()
      ) {
        Card(
          modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 14.dp)
            .testTag("low_battery_alert_banner"),
          shape = RoundedCornerShape(14.dp),
          colors = CardDefaults.cardColors(
            containerColor = EmergencyRed.copy(alpha = 0.15f)
          ),
          border = androidx.compose.foundation.BorderStroke(1.5.dp, EmergencyRed)
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Box(
              modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(EmergencyRed),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = Icons.Default.BatteryAlert,
                contentDescription = "Low Battery Warning",
                tint = Color.White,
                modifier = Modifier.size(24.dp)
              )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "LOW BATTERY ALERT (${telemetry.batteryPercent}%)",
                fontWeight = FontWeight.Black,
                fontSize = 13.sp,
                color = EmergencyRed
              )
              Text(
                text = "Estimated range: %.1f km (~%d mins). Return to charge dock.".format(
                  telemetry.estimatedRangeKm,
                  telemetry.estimatedRemainingMinutes
                ),
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurface
              )
            }

            Button(
              onClick = onToggleCharging,
              colors = ButtonDefaults.buttonColors(containerColor = EmergencyRed),
              shape = RoundedCornerShape(8.dp)
            ) {
              Text("Plug In", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
          }
        }
      }

      // 3. Central Radial Progress Indicator
      RadialBatteryGauge(
        percent = telemetry.batteryPercent,
        sweepAngle = animatedSweep,
        isCharging = telemetry.isCharging,
        statusColor = statusColor,
        isLowBattery = isLowBattery,
        voltage = telemetry.batteryVoltage,
        estimatedRangeKm = telemetry.estimatedRangeKm,
        modifier = Modifier.size(190.dp)
      )

      Spacer(modifier = Modifier.height(16.dp))

      // 4. Power Telemetry Metrics (Voltage, Range, Cell Temp)
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        BatteryMetricPill(
          label = "Voltage",
          value = "%.1f V".format(telemetry.batteryVoltage),
          icon = Icons.Default.FlashOn,
          color = MaterialTheme.colorScheme.primary,
          modifier = Modifier.weight(1f)
        )

        BatteryMetricPill(
          label = "Est. Range",
          value = "%.1f km".format(telemetry.estimatedRangeKm),
          icon = Icons.Default.Speed,
          color = CalmingTeal,
          modifier = Modifier.weight(1f)
        )

        BatteryMetricPill(
          label = "Pack Temp",
          value = "%.1f °C".format(telemetry.batteryTempC),
          icon = Icons.Default.Thermostat,
          color = if (telemetry.batteryTempC > 45f) EmergencyRed else MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.weight(1f)
        )
      }

      // 5. Interactive Simulation / Test Section
      if (showControls) {
        Spacer(modifier = Modifier.height(12.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "Battery Test Simulator:",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )

          Text(
            text = if (showTestingSliders) "Hide Controls" else "Simulate Drain/Charge",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = CalmingTeal,
            modifier = Modifier.clickable { showTestingSliders = !showTestingSliders }
          )
        }

        AnimatedVisibility(visible = showTestingSliders) {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(top = 8.dp)
          ) {
            Slider(
              value = telemetry.batteryPercent.toFloat(),
              onValueChange = { onSetBatteryPercent(it.toInt()) },
              valueRange = 0f..100f,
              colors = SliderDefaults.colors(
                thumbColor = statusColor,
                activeTrackColor = statusColor
              ),
              modifier = Modifier.fillMaxWidth()
            )

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              QuickBatteryButton(
                label = "15% (Test Low)",
                isSelected = telemetry.batteryPercent == 15,
                onClick = { onSetBatteryPercent(15) },
                modifier = Modifier.weight(1f),
                isDanger = true
              )
              QuickBatteryButton(
                label = "50% (Normal)",
                isSelected = telemetry.batteryPercent == 50,
                onClick = { onSetBatteryPercent(50) },
                modifier = Modifier.weight(1f)
              )
              QuickBatteryButton(
                label = "95% (Full)",
                isSelected = telemetry.batteryPercent == 95,
                onClick = { onSetBatteryPercent(95) },
                modifier = Modifier.weight(1f)
              )
            }
          }
        }
      }
    }
  }
}

/**
 * Radial Battery Gauge with Animated Sweep Arc & Glow
 */
@Composable
fun RadialBatteryGauge(
  percent: Int,
  sweepAngle: Float,
  isCharging: Boolean,
  statusColor: Color,
  isLowBattery: Boolean,
  voltage: Float,
  estimatedRangeKm: Float,
  modifier: Modifier = Modifier,
  sizeDp: Dp = 190.dp
) {
  val trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)

  Box(
    modifier = modifier.size(sizeDp),
    contentAlignment = Alignment.Center
  ) {
    Canvas(modifier = Modifier.fillMaxSize()) {
      val strokeWidth = 14.dp.toPx()
      val diameter = size.minDimension - strokeWidth
      val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
      val arcSize = Size(diameter, diameter)

      // 1. Full 360-degree background track
      drawArc(
        color = trackColor,
        startAngle = -90f,
        sweepAngle = 360f,
        useCenter = false,
        topLeft = topLeft,
        size = arcSize,
        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
      )

      // 2. Active colored progress sweep arc
      if (sweepAngle > 0.5f) {
        val sweepBrush = Brush.sweepGradient(
          colors = listOf(
            statusColor.copy(alpha = 0.7f),
            statusColor,
            statusColor
          ),
          center = Offset(size.width / 2f, size.height / 2f)
        )

        drawArc(
          brush = sweepBrush,
          startAngle = -90f,
          sweepAngle = sweepAngle,
          useCenter = false,
          topLeft = topLeft,
          size = arcSize,
          style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )
      }
    }

    // Center content: Large percentage readout, charging indicator, and range
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center
    ) {
      if (isCharging) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            imageVector = Icons.Default.BatteryChargingFull,
            contentDescription = "Charging",
            tint = CalmingTeal,
            modifier = Modifier.size(20.dp)
          )
          Spacer(modifier = Modifier.width(3.dp))
          Text(
            text = "FAST CHARGE",
            fontSize = 10.sp,
            fontWeight = FontWeight.Black,
            color = CalmingTeal
          )
        }
      }

      Text(
        text = "$percent%",
        fontSize = 38.sp,
        fontWeight = FontWeight.Black,
        color = statusColor
      )

      Text(
        text = if (isLowBattery) "RECHARGE SOON" else "%.1f km remaining".format(estimatedRangeKm),
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        color = if (isLowBattery) EmergencyRed else MaterialTheme.colorScheme.onSurfaceVariant
      )

      Text(
        text = "%.1f V DC".format(voltage),
        fontSize = 10.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
      )
    }
  }
}

@Composable
private fun BatteryMetricPill(
  label: String,
  value: String,
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  color: Color,
  modifier: Modifier = Modifier
) {
  Column(
    modifier = modifier
      .clip(RoundedCornerShape(12.dp))
      .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
      .padding(vertical = 8.dp, horizontal = 10.dp),
    horizontalAlignment = Alignment.Start
  ) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      Icon(icon, contentDescription = label, tint = color, modifier = Modifier.size(14.dp))
      Spacer(modifier = Modifier.width(4.dp))
      Text(label, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    Spacer(modifier = Modifier.height(2.dp))
    Text(value, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
  }
}

@Composable
private fun QuickBatteryButton(
  label: String,
  isSelected: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  isDanger: Boolean = false
) {
  val container = when {
    isSelected && isDanger -> EmergencyRed
    isSelected -> MaterialTheme.colorScheme.primary
    isDanger -> EmergencyRed.copy(alpha = 0.15f)
    else -> MaterialTheme.colorScheme.surfaceVariant
  }
  val textCol = when {
    isSelected -> Color.White
    isDanger -> EmergencyRed
    else -> MaterialTheme.colorScheme.onSurface
  }

  Box(
    modifier = modifier
      .clip(RoundedCornerShape(8.dp))
      .background(container)
      .clickable { onClick() }
      .padding(vertical = 6.dp),
    contentAlignment = Alignment.Center
  ) {
    Text(
      text = label,
      fontSize = 11.sp,
      fontWeight = FontWeight.Bold,
      color = textCol
    )
  }
}
