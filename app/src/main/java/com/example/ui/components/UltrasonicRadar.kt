package com.example.ui.components

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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Badge
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Direction
import com.example.model.ObstacleSensors
import com.example.ui.theme.EmergencyRed
import com.example.ui.theme.HighContrastAmber
import com.example.ui.theme.SafeGreen

@Composable
fun UltrasonicRadar(
  obstacles: ObstacleSensors,
  onSimulateObstacle: (Direction, Float) -> Unit,
  onResetObstacles: () -> Unit,
  modifier: Modifier = Modifier
) {
  val hasHazard = obstacles.hasAnyHazard()
  val hazardColor by animateColorAsState(
    targetValue = if (hasHazard) EmergencyRed else MaterialTheme.colorScheme.primary,
    label = "hazardColor"
  )

  Card(
    modifier = modifier
      .fillMaxWidth()
      .testTag("ultrasonic_radar_card"),
    shape = RoundedCornerShape(20.dp),
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.surface
    ),
    border = androidx.compose.foundation.BorderStroke(
      width = if (hasHazard) 2.dp else 1.dp,
      color = if (hasHazard) EmergencyRed else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
    )
  ) {
    Column(
      modifier = Modifier.padding(16.dp),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column {
          Text(
            text = "Ultrasonic Proximity Sensors",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
          )
          Text(
            text = "4-Zone active collision avoidance (<35cm auto-stops)",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }

        if (hasHazard) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
              .clip(RoundedCornerShape(8.dp))
              .background(EmergencyRed.copy(alpha = 0.2f))
              .padding(horizontal = 8.dp, vertical = 4.dp)
          ) {
            Icon(
              imageVector = Icons.Default.Warning,
              contentDescription = "Hazard Alert",
              tint = EmergencyRed,
              modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
              text = "HAZARD",
              color = EmergencyRed,
              fontWeight = FontWeight.Bold,
              fontSize = 12.sp
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(16.dp))

      // Visual Radar Circle
      Box(
        modifier = Modifier
          .size(190.dp)
          .align(Alignment.CenterHorizontally),
        contentAlignment = Alignment.Center
      ) {
        val strokeColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)

        Canvas(modifier = Modifier.size(180.dp)) {
          val center = Offset(size.width / 2f, size.height / 2f)
          val rMax = size.width / 2f

          // Radar concentric range rings: 200cm, 100cm, 35cm (hazard)
          drawCircle(color = strokeColor, radius = rMax, center = center, style = Stroke(width = 1.5f))
          drawCircle(color = strokeColor, radius = rMax * 0.5f, center = center, style = Stroke(width = 1f))
          drawCircle(
            color = EmergencyRed.copy(alpha = 0.4f),
            radius = rMax * 0.25f,
            center = center,
            style = Stroke(width = 2f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f)))
          )

          // Crosshairs
          drawLine(color = strokeColor, start = Offset(center.x, 0f), end = Offset(center.x, size.height), strokeWidth = 1f)
          drawLine(color = strokeColor, start = Offset(0f, center.y), end = Offset(size.width, center.y), strokeWidth = 1f)
        }

        // Wheelchair center icon
        Box(
          modifier = Modifier
            .size(42.dp)
            .clip(CircleShape)
            .background(hazardColor.copy(alpha = 0.15f))
            .border(2.dp, hazardColor, CircleShape),
          contentAlignment = Alignment.Center
        ) {
          Text(
            text = "CHAIR",
            fontSize = 9.sp,
            fontWeight = FontWeight.Black,
            color = hazardColor
          )
        }

        // Top / Front sensor badge
        SensorBadge(
          label = "FRONT",
          distanceCm = obstacles.frontCm,
          hazard = obstacles.isHazardInDirection(Direction.FORWARD),
          modifier = Modifier.align(Alignment.TopCenter)
        )

        // Bottom / Back sensor badge
        SensorBadge(
          label = "BACK",
          distanceCm = obstacles.backCm,
          hazard = obstacles.isHazardInDirection(Direction.BACKWARD),
          modifier = Modifier.align(Alignment.BottomCenter)
        )

        // Left sensor badge
        SensorBadge(
          label = "LEFT",
          distanceCm = obstacles.leftCm,
          hazard = obstacles.isHazardInDirection(Direction.LEFT),
          modifier = Modifier.align(Alignment.CenterStart)
        )

        // Right sensor badge
        SensorBadge(
          label = "RIGHT",
          distanceCm = obstacles.rightCm,
          hazard = obstacles.isHazardInDirection(Direction.RIGHT),
          modifier = Modifier.align(Alignment.CenterEnd)
        )
      }

      Spacer(modifier = Modifier.height(14.dp))

      // Simulation trigger controls (helpful for testing obstacle auto-stop)
      Text(
        text = "Quick Test Obstacles (Tap to simulate obstacle proximity):",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )
      Spacer(modifier = Modifier.height(6.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        TestObstaclePill(
          title = "Front Block (25cm)",
          isDanger = obstacles.frontCm <= 35f,
          onClick = {
            if (obstacles.frontCm <= 35f) {
              onSimulateObstacle(Direction.FORWARD, 160f)
            } else {
              onSimulateObstacle(Direction.FORWARD, 25f)
            }
          },
          modifier = Modifier.weight(1f)
        )

        TestObstaclePill(
          title = "Right Block (20cm)",
          isDanger = obstacles.rightCm <= 35f,
          onClick = {
            if (obstacles.rightCm <= 35f) {
              onSimulateObstacle(Direction.RIGHT, 150f)
            } else {
              onSimulateObstacle(Direction.RIGHT, 20f)
            }
          },
          modifier = Modifier.weight(1f)
        )

        TestObstaclePill(
          title = "Clear All",
          isDanger = false,
          onClick = onResetObstacles,
          modifier = Modifier.weight(0.9f)
        )
      }
    }
  }
}

@Composable
private fun SensorBadge(
  label: String,
  distanceCm: Float,
  hazard: Boolean,
  modifier: Modifier = Modifier
) {
  val color = when {
    hazard -> EmergencyRed
    distanceCm < 75f -> HighContrastAmber
    else -> SafeGreen
  }

  Column(
    modifier = modifier
      .clip(RoundedCornerShape(8.dp))
      .background(color.copy(alpha = 0.18f))
      .border(1.dp, color, RoundedCornerShape(8.dp))
      .padding(horizontal = 6.dp, vertical = 2.dp),
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    Text(
      text = label,
      fontSize = 9.sp,
      fontWeight = FontWeight.Bold,
      color = color
    )
    Text(
      text = "${distanceCm.toInt()} cm",
      fontSize = 11.sp,
      fontWeight = FontWeight.ExtraBold,
      color = color
    )
  }
}

@Composable
private fun TestObstaclePill(
  title: String,
  isDanger: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  Box(
    modifier = modifier
      .clip(RoundedCornerShape(8.dp))
      .background(if (isDanger) EmergencyRed.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant)
      .border(
        1.dp,
        if (isDanger) EmergencyRed else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
        RoundedCornerShape(8.dp)
      )
      .clickable { onClick() }
      .padding(vertical = 8.dp, horizontal = 4.dp),
    contentAlignment = Alignment.Center
  ) {
    Text(
      text = title,
      fontSize = 11.sp,
      fontWeight = FontWeight.Medium,
      color = if (isDanger) EmergencyRed else MaterialTheme.colorScheme.onSurface
    )
  }
}
