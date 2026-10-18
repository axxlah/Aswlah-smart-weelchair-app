package com.example.ui.components

import android.content.Context
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.location.MobileLocationManager
import com.example.model.LocationPoint
import com.example.ui.theme.CalmingTeal
import com.example.ui.theme.EmergencyRed
import com.example.ui.theme.MedicalBluePrimary
import com.example.ui.theme.SafeGreen
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun LiveWheelchairMap(
  currentLocation: LocationPoint,
  tripHistory: List<LocationPoint>,
  speedKmh: Float,
  latitude: Double = 37.774929,
  longitude: Double = -122.419416,
  accuracyMeters: Float = 3.2f,
  locationAddress: String = "789 Mission St, San Francisco, CA",
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  var zoomScale by remember { mutableStateOf(1.0f) }
  var isSatelliteMode by remember { mutableStateOf(false) }
  var destinationPoint by remember { mutableStateOf<Offset?>(Offset(360f, 180f)) }

  Card(
    modifier = modifier
      .fillMaxWidth()
      .height(310.dp)
      .testTag("live_wheelchair_map"),
    shape = RoundedCornerShape(20.dp),
    colors = CardDefaults.cardColors(
      containerColor = if (isSatelliteMode) Color(0xFF131D2E) else MaterialTheme.colorScheme.surface
    ),
    border = androidx.compose.foundation.BorderStroke(
      width = 1.dp,
      color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
    )
  ) {
    Box(modifier = Modifier.fillMaxSize()) {
      val gridColor = if (isSatelliteMode) Color.White.copy(alpha = 0.08f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
      val streetColor = if (isSatelliteMode) Color(0xFF1E2D45) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
      val pathColor = MedicalBluePrimary
      val wheelchairColor = CalmingTeal

      Canvas(
        modifier = Modifier
          .fillMaxSize()
          .pointerInput(Unit) {
            detectTapGestures { offset ->
              destinationPoint = offset
            }
          }
      ) {
        val w = size.width
        val h = size.height

        // 1. Draw Google Maps style street grid
        val gridSize = 40.dp.toPx() * zoomScale
        var gx = 0f
        while (gx < w) {
          drawLine(color = gridColor, start = Offset(gx, 0f), end = Offset(gx, h), strokeWidth = 1f)
          gx += gridSize
        }
        var gy = 0f
        while (gy < h) {
          drawLine(color = gridColor, start = Offset(0f, gy), end = Offset(w, gy), strokeWidth = 1f)
          gy += gridSize
        }

        // Draw campus pathways / avenue road blocks
        drawRect(
          color = streetColor,
          topLeft = Offset(0f, h * 0.36f),
          size = androidx.compose.ui.geometry.Size(w, 54.dp.toPx())
        )
        drawRect(
          color = streetColor,
          topLeft = Offset(w * 0.44f, 0f),
          size = androidx.compose.ui.geometry.Size(54.dp.toPx(), h)
        )

        // 2. Destination Waypoint Marker
        destinationPoint?.let { dest ->
          drawCircle(
            color = EmergencyRed.copy(alpha = 0.2f),
            radius = 18.dp.toPx(),
            center = dest
          )
          drawCircle(
            color = EmergencyRed,
            radius = 6.dp.toPx(),
            center = dest
          )
          drawLine(
            color = EmergencyRed.copy(alpha = 0.6f),
            start = dest,
            end = Offset(dest.x, dest.y - 14.dp.toPx()),
            strokeWidth = 2.dp.toPx()
          )
        }

        // 3. Movement history trail
        if (tripHistory.size > 1) {
          val trailPath = Path()
          val firstNorm = tripHistory.first()
          val startX = (firstNorm.x / 500f) * w
          val startY = (firstNorm.y / 500f) * h
          trailPath.moveTo(startX, startY)

          for (i in 1 until tripHistory.size) {
            val pt = tripHistory[i]
            val px = (pt.x / 500f) * w
            val py = (pt.y / 500f) * h
            trailPath.lineTo(px, py)
          }

          drawPath(
            path = trailPath,
            color = pathColor.copy(alpha = 0.7f),
            style = Stroke(width = 3.5.dp.toPx())
          )
        }

        // 4. Wheelchair GPS Position marker with heading and accuracy halo
        val chairX = (currentLocation.x / 500f) * w
        val chairY = (currentLocation.y / 500f) * h
        val chairPos = Offset(chairX, chairY)

        // GPS Accuracy circle
        drawCircle(
          color = wheelchairColor.copy(alpha = 0.16f),
          radius = 26.dp.toPx(),
          center = chairPos
        )
        drawCircle(
          color = wheelchairColor.copy(alpha = 0.5f),
          radius = 18.dp.toPx(),
          center = chairPos,
          style = Stroke(width = 1.5.dp.toPx())
        )
        drawCircle(
          color = wheelchairColor,
          radius = 7.dp.toPx(),
          center = chairPos
        )

        // Heading direction vector indicator
        val headRad = Math.toRadians(currentLocation.headingDegrees.toDouble())
        val arrowLen = 22.dp.toPx()
        val arrowEnd = Offset(
          x = chairPos.x + (sin(headRad) * arrowLen).toFloat(),
          y = chairPos.y - (cos(headRad) * arrowLen).toFloat()
        )
        drawLine(
          color = Color.White,
          start = chairPos,
          end = arrowEnd,
          strokeWidth = 3.dp.toPx()
        )
      }

      // Top map overlay HUD: Real Mobile GPS Telemetry
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
      ) {
        Column(
          modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.92f))
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Default.MyLocation,
              contentDescription = "GPS Active",
              tint = SafeGreen,
              modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
              text = "GPS: %.5f°, %.5f°".format(latitude, longitude),
              style = MaterialTheme.typography.bodySmall,
              fontWeight = FontWeight.Bold,
              fontSize = 11.sp,
              color = MaterialTheme.colorScheme.onSurface
            )
          }
          Text(
            text = "Acc: ±%.1fm • %s".format(accuracyMeters, locationAddress),
            fontSize = 9.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }

        // Map controls: Toggle Satellite & Zoom
        Row(
          modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.92f))
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
        ) {
          IconButton(
            onClick = { isSatelliteMode = !isSatelliteMode },
            modifier = Modifier.size(32.dp)
          ) {
            Icon(Icons.Default.Layers, contentDescription = "Satellite Mode", tint = if (isSatelliteMode) CalmingTeal else MaterialTheme.colorScheme.onSurface)
          }
          IconButton(
            onClick = { zoomScale = (zoomScale + 0.2f).coerceAtMost(2.0f) },
            modifier = Modifier.size(32.dp)
          ) {
            Icon(Icons.Default.ZoomIn, contentDescription = "Zoom In", tint = MaterialTheme.colorScheme.onSurface)
          }
          IconButton(
            onClick = { zoomScale = (zoomScale - 0.2f).coerceAtLeast(0.6f) },
            modifier = Modifier.size(32.dp)
          ) {
            Icon(Icons.Default.ZoomOut, contentDescription = "Zoom Out", tint = MaterialTheme.colorScheme.onSurface)
          }
        }
      }

      // Bottom Row: Direct Button to Open in Google Maps!
      Row(
        modifier = Modifier
          .align(Alignment.BottomCenter)
          .fillMaxWidth()
          .padding(8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Box(
          modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.88f))
            .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
          Text(
            text = "Tap map to set waypoint",
            fontSize = 9.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }

        // Dedicated Google Maps Button
        Button(
          onClick = {
            MobileLocationManager.openGoogleMaps(
              context = context,
              lat = latitude,
              lng = longitude,
              label = "SmartChair Real-Time Location"
            )
          },
          shape = RoundedCornerShape(10.dp),
          colors = ButtonDefaults.buttonColors(containerColor = MedicalBluePrimary),
          modifier = Modifier.height(34.dp)
        ) {
          Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(14.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("Open in Google Maps", fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
      }
    }
  }
}
