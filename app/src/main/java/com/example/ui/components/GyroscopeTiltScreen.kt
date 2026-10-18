package com.example.ui.components

import android.content.Context
import androidx.compose.animation.animateColorAsState
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Stop
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Direction
import com.example.model.WheelchairTelemetry
import com.example.sensor.DeviceTiltSensorManager
import com.example.ui.theme.CalmingTeal
import com.example.ui.theme.EmergencyRed
import com.example.ui.theme.HighContrastAmber
import com.example.ui.theme.MedicalBluePrimary
import com.example.ui.theme.SafeGreen

@Composable
fun GyroscopeTiltScreen(
  telemetry: WheelchairTelemetry,
  onTiltCommand: (pitch: Float, roll: Float, dir: Direction) -> Unit,
  onDirectDirection: (Direction) -> Unit,
  onToggleDrive: () -> Unit,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current

  var livePitch by remember { mutableFloatStateOf(telemetry.pitchDegrees) }
  var liveRoll by remember { mutableFloatStateOf(telemetry.rollDegrees) }
  var deadzoneDeg by remember { mutableFloatStateOf(10f) }

  // Register real hardware Gyroscope & Accelerometer listener
  DisposableEffect(Unit) {
    val sensorManager = DeviceTiltSensorManager(context) { p, r, dir ->
      livePitch = p
      liveRoll = r
      onTiltCommand(p, r, dir)
    }
    sensorManager.start()

    onDispose {
      sensorManager.stop()
    }
  }

  val activeDir = telemetry.currentDirection

  Column(
    modifier = modifier
      .fillMaxSize()
      .verticalScroll(rememberScrollState())
      .padding(8.dp),
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    // Mode title and explanation
    Card(
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(16.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
      border = androidx.compose.foundation.BorderStroke(1.dp, CalmingTeal.copy(alpha = 0.4f))
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(Icons.Default.Sensors, contentDescription = "Gyro", tint = CalmingTeal)
          Spacer(modifier = Modifier.width(8.dp))
          Column {
            Text(
              text = "Tablet Tilt Steering (Gyroscope)",
              style = MaterialTheme.typography.titleSmall,
              fontWeight = FontWeight.Bold
            )
            Text(
              text = "Tilt Forward/Up = Forward • Tilt Right = Right • Tilt Left = Left",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }

        // Zero / Calibrate Level button
        OutlinedButton(
          onClick = {
            onTiltCommand(0f, 0f, Direction.STOP)
            livePitch = 0f
            liveRoll = 0f
          },
          shape = RoundedCornerShape(8.dp)
        ) {
          Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("Zero Level", fontSize = 10.sp)
        }
      }
    }

    Spacer(modifier = Modifier.height(14.dp))

    // 2. Central Visual Artificial Horizon / Bubble Level
    Box(
      modifier = Modifier
        .size(240.dp)
        .testTag("gyro_bubble_level"),
      contentAlignment = Alignment.Center
    ) {
      val primaryColor = MaterialTheme.colorScheme.primary
      val ringColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)

      Canvas(modifier = Modifier.size(230.dp)) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val maxR = size.width / 2f

        // Outer level circle
        drawCircle(color = ringColor, radius = maxR, center = center, style = Stroke(width = 2.dp.toPx()))
        drawCircle(color = ringColor, radius = maxR * 0.65f, center = center, style = Stroke(width = 1.dp.toPx()))

        // Center neutral deadzone ring
        drawCircle(
          color = SafeGreen.copy(alpha = 0.35f),
          radius = maxR * 0.28f,
          center = center,
          style = Stroke(width = 2.dp.toPx())
        )

        // Crosshairs
        drawLine(ringColor, Offset(center.x, 0f), Offset(center.x, size.height), strokeWidth = 1.dp.toPx())
        drawLine(ringColor, Offset(0f, center.y), Offset(size.width, center.y), strokeWidth = 1.dp.toPx())

        // Calculate bubble displacement based on pitch & roll
        // Normal range clamped to [-45..45]
        val normPitch = (livePitch / 45f).coerceIn(-1f, 1f)
        val normRoll = (liveRoll / 45f).coerceIn(-1f, 1f)

        val maxDisplacement = maxR * 0.75f
        val bubbleOffset = Offset(
          x = center.x + normRoll * maxDisplacement,
          y = center.y + normPitch * maxDisplacement
        )

        val bubbleColor = when (activeDir) {
          Direction.STOP -> SafeGreen
          else -> CalmingTeal
        }

        // Draw bubble shadow & body
        drawCircle(
          color = bubbleColor.copy(alpha = 0.3f),
          radius = 26.dp.toPx(),
          center = bubbleOffset
        )
        drawCircle(
          color = bubbleColor,
          radius = 18.dp.toPx(),
          center = bubbleOffset
        )
        drawCircle(
          color = Color.White,
          radius = 5.dp.toPx(),
          center = bubbleOffset
        )
      }

      // Directional Labels around the Horizon
      TiltZoneIndicator(
        label = "FORWARD",
        isActive = activeDir == Direction.FORWARD,
        modifier = Modifier.align(Alignment.TopCenter)
      )
      TiltZoneIndicator(
        label = "BACKWARD",
        isActive = activeDir == Direction.BACKWARD,
        modifier = Modifier.align(Alignment.BottomCenter)
      )
      TiltZoneIndicator(
        label = "LEFT",
        isActive = activeDir == Direction.LEFT,
        modifier = Modifier.align(Alignment.CenterStart)
      )
      TiltZoneIndicator(
        label = "RIGHT",
        isActive = activeDir == Direction.RIGHT,
        modifier = Modifier.align(Alignment.CenterEnd)
      )

      // Center Status Badge
      Column(
        modifier = Modifier
          .align(Alignment.Center)
          .clip(RoundedCornerShape(8.dp))
          .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.85f))
          .padding(horizontal = 6.dp, vertical = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        Text(
          text = if (activeDir == Direction.STOP) "LEVEL (STOP)" else activeDir.name,
          fontWeight = FontWeight.ExtraBold,
          fontSize = 11.sp,
          color = if (activeDir == Direction.STOP) SafeGreen else CalmingTeal
        )
      }
    }

    Spacer(modifier = Modifier.height(14.dp))

    // Real-Time Tilt Pitch & Roll Numbers
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      TiltMetricCard(
        label = "Pitch (Forward/Back)",
        value = "%.1f°".format(livePitch),
        status = if (livePitch < -deadzoneDeg) "FORWARD" else if (livePitch > deadzoneDeg) "BACK" else "LEVEL",
        modifier = Modifier.weight(1f)
      )
      TiltMetricCard(
        label = "Roll (Left/Right)",
        value = "%.1f°".format(liveRoll),
        status = if (liveRoll > deadzoneDeg) "RIGHT" else if (liveRoll < -deadzoneDeg) "LEFT" else "LEVEL",
        modifier = Modifier.weight(1f)
      )
    }

    Spacer(modifier = Modifier.height(14.dp))

    // Interactive Quick-Tilt Emulator Test Buttons (allows testing tilting without physical movement)
    Text(
      text = "Emulator Tilt Simulator (Tap to simulate tablet tilt):",
      style = MaterialTheme.typography.bodySmall,
      fontWeight = FontWeight.Bold,
      color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Spacer(modifier = Modifier.height(6.dp))

    Column(
      modifier = Modifier.fillMaxWidth(),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
      Button(
        onClick = {
          livePitch = -22f
          liveRoll = 0f
          onTiltCommand(-22f, 0f, Direction.FORWARD)
        },
        modifier = Modifier
          .fillMaxWidth(0.6f)
          .height(44.dp),
        shape = RoundedCornerShape(10.dp),
        colors = ButtonDefaults.buttonColors(
          containerColor = if (activeDir == Direction.FORWARD) CalmingTeal else MaterialTheme.colorScheme.surfaceVariant
        )
      ) {
        Icon(Icons.Default.ArrowUpward, contentDescription = null, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text("Tilt Forward / Up", fontSize = 12.sp, fontWeight = FontWeight.Bold)
      }

      Row(
        modifier = Modifier.fillMaxWidth(0.85f),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        Button(
          onClick = {
            livePitch = 0f
            liveRoll = -22f
            onTiltCommand(0f, -22f, Direction.LEFT)
          },
          modifier = Modifier
            .weight(1f)
            .height(44.dp),
          shape = RoundedCornerShape(10.dp),
          colors = ButtonDefaults.buttonColors(
            containerColor = if (activeDir == Direction.LEFT) CalmingTeal else MaterialTheme.colorScheme.surfaceVariant
          )
        ) {
          Icon(Icons.Default.ArrowBack, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("Tilt Left", fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }

        Button(
          onClick = {
            livePitch = 0f
            liveRoll = 0f
            onTiltCommand(0f, 0f, Direction.STOP)
          },
          modifier = Modifier
            .weight(1f)
            .height(44.dp),
          shape = RoundedCornerShape(10.dp),
          colors = ButtonDefaults.buttonColors(
            containerColor = if (activeDir == Direction.STOP) SafeGreen else MaterialTheme.colorScheme.surfaceVariant
          )
        ) {
          Icon(Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("Level (Stop)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }

        Button(
          onClick = {
            livePitch = 0f
            liveRoll = 22f
            onTiltCommand(0f, 22f, Direction.RIGHT)
          },
          modifier = Modifier
            .weight(1f)
            .height(44.dp),
          shape = RoundedCornerShape(10.dp),
          colors = ButtonDefaults.buttonColors(
            containerColor = if (activeDir == Direction.RIGHT) CalmingTeal else MaterialTheme.colorScheme.surfaceVariant
          )
        ) {
          Icon(Icons.Default.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("Tilt Right", fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
      }

      Button(
        onClick = {
          livePitch = 22f
          liveRoll = 0f
          onTiltCommand(22f, 0f, Direction.BACKWARD)
        },
        modifier = Modifier
          .fillMaxWidth(0.6f)
          .height(44.dp),
        shape = RoundedCornerShape(10.dp),
        colors = ButtonDefaults.buttonColors(
          containerColor = if (activeDir == Direction.BACKWARD) CalmingTeal else MaterialTheme.colorScheme.surfaceVariant
        )
      ) {
        Icon(Icons.Default.ArrowDownward, contentDescription = null, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text("Tilt Backward", fontSize = 12.sp, fontWeight = FontWeight.Bold)
      }
    }
  }
}

@Composable
private fun TiltZoneIndicator(
  label: String,
  isActive: Boolean,
  modifier: Modifier = Modifier
) {
  val bg by animateColorAsState(
    targetValue = if (isActive) CalmingTeal else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
    label = "zoneBg"
  )
  val textColor = if (isActive) Color.White else MaterialTheme.colorScheme.onSurfaceVariant

  Box(
    modifier = modifier
      .clip(RoundedCornerShape(6.dp))
      .background(bg)
      .padding(horizontal = 6.dp, vertical = 2.dp)
  ) {
    Text(
      text = label,
      fontSize = 9.sp,
      fontWeight = FontWeight.Black,
      color = textColor
    )
  }
}

@Composable
private fun TiltMetricCard(
  label: String,
  value: String,
  status: String,
  modifier: Modifier = Modifier
) {
  Column(
    modifier = modifier
      .clip(RoundedCornerShape(12.dp))
      .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
      .padding(10.dp)
  ) {
    Text(label, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text(value, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
      Text(status, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = CalmingTeal)
    }
  }
}
