package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

@Composable
fun VirtualJoystick(
  modifier: Modifier = Modifier,
  sizeDp: Dp = 220.dp,
  onMove: (x: Float, y: Float) -> Unit,
  onRelease: () -> Unit
) {
  var thumbOffset by remember { mutableStateOf(Offset.Zero) }
  val primaryColor = MaterialTheme.colorScheme.primary
  val secondaryColor = MaterialTheme.colorScheme.secondary

  Box(
    modifier = modifier
      .size(sizeDp)
      .testTag("virtual_joystick")
      .pointerInput(Unit) {
        val maxRadius = size.width / 2.6f
        detectDragGestures(
          onDragStart = { offset ->
            val center = Offset(size.width / 2f, size.height / 2f)
            val delta = offset - center
            val dist = sqrt(delta.x * delta.x + delta.y * delta.y)
            val clampedDist = dist.coerceAtMost(maxRadius)
            val angle = atan2(delta.y, delta.x)
            val clampedOffset = Offset(
              x = cos(angle) * clampedDist,
              y = sin(angle) * clampedDist
            )
            thumbOffset = clampedOffset
            onMove(clampedOffset.x / maxRadius, clampedOffset.y / maxRadius)
          },
          onDrag = { change, dragAmount ->
            change.consume()
            val newRaw = thumbOffset + dragAmount
            val dist = sqrt(newRaw.x * newRaw.x + newRaw.y * newRaw.y)
            val clampedDist = dist.coerceAtMost(maxRadius)
            val angle = atan2(newRaw.y, newRaw.x)
            val clampedOffset = Offset(
              x = cos(angle) * clampedDist,
              y = sin(angle) * clampedDist
            )
            thumbOffset = clampedOffset
            onMove(clampedOffset.x / maxRadius, clampedOffset.y / maxRadius)
          },
          onDragEnd = {
            thumbOffset = Offset.Zero
            onRelease()
          },
          onDragCancel = {
            thumbOffset = Offset.Zero
            onRelease()
          }
        )
      },
    contentAlignment = Alignment.Center
  ) {
    Canvas(modifier = Modifier.size(sizeDp)) {
      val center = Offset(size.width / 2f, size.height / 2f)
      val radius = size.width / 2.6f

      // Outer base ring background
      drawCircle(
        brush = Brush.radialGradient(
          colors = listOf(
            primaryColor.copy(alpha = 0.15f),
            primaryColor.copy(alpha = 0.05f)
          ),
          center = center,
          radius = radius
        ),
        radius = radius,
        center = center
      )

      // Outer border ring
      drawCircle(
        color = primaryColor.copy(alpha = 0.5f),
        radius = radius,
        center = center,
        style = Stroke(width = 3.dp.toPx())
      )

      // Dead-zone center ring
      drawCircle(
        color = secondaryColor.copy(alpha = 0.3f),
        radius = radius * 0.25f,
        center = center,
        style = Stroke(width = 1.5.dp.toPx())
      )

      // Direction guidelines
      val guideColor = primaryColor.copy(alpha = 0.2f)
      drawLine(guideColor, Offset(center.x - radius, center.y), Offset(center.x + radius, center.y), strokeWidth = 1.dp.toPx())
      drawLine(guideColor, Offset(center.x, center.y - radius), Offset(center.x, center.y + radius), strokeWidth = 1.dp.toPx())

      // Active thumb knob
      val thumbCenter = center + thumbOffset
      val thumbRadius = radius * 0.45f

      // Thumb shadow / glow
      drawCircle(
        color = secondaryColor.copy(alpha = 0.25f),
        radius = thumbRadius + 6.dp.toPx(),
        center = thumbCenter
      )

      // Thumb knob solid body
      drawCircle(
        brush = Brush.radialGradient(
          colors = listOf(
            secondaryColor,
            primaryColor
          ),
          center = thumbCenter,
          radius = thumbRadius
        ),
        radius = thumbRadius,
        center = thumbCenter
      )

      // Inner knob accent ring
      drawCircle(
        color = Color.White.copy(alpha = 0.8f),
        radius = thumbRadius * 0.35f,
        center = thumbCenter
      )
    }
  }
}
