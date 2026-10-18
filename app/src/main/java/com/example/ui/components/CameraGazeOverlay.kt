package com.example.ui.components

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RemoveRedEye
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.camera.CameraGazeAnalyzer
import com.example.model.Direction
import com.example.ui.theme.CalmingTeal
import com.example.ui.theme.CalmingTealLight
import com.example.ui.theme.EmergencyRed
import com.example.ui.theme.HighContrastAmber
import com.example.ui.theme.MedicalBluePrimary
import com.example.ui.theme.SafeGreen
import java.util.concurrent.Executors

@Composable
fun CameraGazeOverlay(
  currentDirection: Direction,
  activeDwellZone: Direction?,
  dwellProgress: Float,
  gazeCursorX: Float,
  gazeCursorY: Float,
  isDriveEngaged: Boolean,
  blinkCount: Int,
  onBlinkDetected: () -> Unit,
  onGazeUpdated: (normX: Float, normY: Float, zone: Direction?) -> Unit,
  onDirectionSelected: (Direction) -> Unit,
  onStartCalibration: () -> Unit,
  onToggleDrive: () -> Unit,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val lifecycleOwner = LocalLifecycleOwner.current

  var hasCameraPermission by remember {
    mutableStateOf(
      ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
    )
  }

  val permissionLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestPermission()
  ) { isGranted ->
    hasCameraPermission = isGranted
  }

  LaunchedEffect(Unit) {
    if (!hasCameraPermission) {
      permissionLauncher.launch(Manifest.permission.CAMERA)
    }
  }

  BoxWithConstraints(
    modifier = modifier
      .fillMaxSize()
      .testTag("camera_gaze_overlay")
  ) {
    val totalWidth = maxWidth
    val totalHeight = maxHeight

    // 1. CameraX Front-Facing Preview Container
    if (hasCameraPermission) {
      Box(
        modifier = Modifier
          .fillMaxSize()
          .clip(RoundedCornerShape(16.dp))
      ) {
        AndroidView(
          factory = { ctx ->
            val previewView = PreviewView(ctx).apply {
              implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            }
            val cameraExecutor = Executors.newSingleThreadExecutor()
            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)

            cameraProviderFuture.addListener({
              try {
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build().also {
                  it.surfaceProvider = previewView.surfaceProvider
                }

                val imageAnalysis = ImageAnalysis.Builder()
                  .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                  .build()
                  .also {
                    it.setAnalyzer(
                      cameraExecutor,
                      CameraGazeAnalyzer(
                        onBlinkDetected = onBlinkDetected,
                        onGazePosition = { nx, ny ->
                          // Determine active zone based on normalized coordinates
                          val zone = when {
                            ny < 0.32f -> Direction.FORWARD
                            ny > 0.68f -> Direction.BACKWARD
                            nx < 0.32f -> Direction.LEFT
                            nx > 0.68f -> Direction.RIGHT
                            else -> Direction.STOP
                          }
                          onGazeUpdated(nx, ny, zone)
                        }
                      )
                    )
                  }

                val cameraSelector = if (cameraProvider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA)) {
                  CameraSelector.DEFAULT_FRONT_CAMERA
                } else {
                  CameraSelector.DEFAULT_BACK_CAMERA
                }

                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview, imageAnalysis)
              } catch (e: Exception) {
                e.printStackTrace()
              }
            }, ContextCompat.getMainExecutor(ctx))

            previewView
          },
          modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(16.dp))
        )

        // Subtle dark scrim so high-contrast dwell buttons remain clearly legible over camera
        Box(
          modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.55f))
        )
      }
    } else {
      // Camera permission request or fallback container
      Box(
        modifier = Modifier
          .fillMaxSize()
          .clip(RoundedCornerShape(16.dp))
          .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
        contentAlignment = Alignment.Center
      ) {
        Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          modifier = Modifier.padding(24.dp)
        ) {
          Icon(
            imageVector = Icons.Default.CameraAlt,
            contentDescription = "Camera Permission",
            modifier = Modifier.size(48.dp),
            tint = CalmingTeal
          )
          Spacer(modifier = Modifier.height(8.dp))
          Text(
            text = "Camera Access for Eye & Blink Tracking",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
          )
          Text(
            text = "Track eye movement & blinks to navigate. Tap button below or use interactive dwell zones.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
          Spacer(modifier = Modifier.height(12.dp))
          Button(
            onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
            colors = ButtonDefaults.buttonColors(containerColor = MedicalBluePrimary)
          ) {
            Text("Grant Camera Permission")
          }
        }
      }
    }

    // 2. High-Contrast Interactive Dwell Zones
    // Top Zone: FORWARD (Look Front/Up)
    DwellZoneCard(
      title = "LOOK FORWARD",
      subtitle = "Move Ahead",
      icon = Icons.Default.ArrowUpward,
      direction = Direction.FORWARD,
      isActive = activeDwellZone == Direction.FORWARD,
      isCurrent = currentDirection == Direction.FORWARD,
      dwellProgress = if (activeDwellZone == Direction.FORWARD) dwellProgress else 0f,
      onHoverEnter = { onGazeUpdated(0.5f, 0.15f, Direction.FORWARD) },
      onDirectClick = { onDirectionSelected(Direction.FORWARD) },
      modifier = Modifier
        .align(Alignment.TopCenter)
        .padding(top = 12.dp)
        .fillMaxWidth(0.75f)
        .height(85.dp)
    )

    // Bottom Zone: BACKWARD (Look Down)
    DwellZoneCard(
      title = "LOOK DOWN",
      subtitle = "Reverse Motion",
      icon = Icons.Default.ArrowDownward,
      direction = Direction.BACKWARD,
      isActive = activeDwellZone == Direction.BACKWARD,
      isCurrent = currentDirection == Direction.BACKWARD,
      dwellProgress = if (activeDwellZone == Direction.BACKWARD) dwellProgress else 0f,
      onHoverEnter = { onGazeUpdated(0.5f, 0.85f, Direction.BACKWARD) },
      onDirectClick = { onDirectionSelected(Direction.BACKWARD) },
      modifier = Modifier
        .align(Alignment.BottomCenter)
        .padding(bottom = 72.dp)
        .fillMaxWidth(0.75f)
        .height(85.dp)
    )

    // Left Zone: LEFT (Look Left)
    DwellZoneCard(
      title = "LOOK LEFT",
      subtitle = "Turn Left",
      icon = Icons.Default.ArrowBack,
      direction = Direction.LEFT,
      isActive = activeDwellZone == Direction.LEFT,
      isCurrent = currentDirection == Direction.LEFT,
      dwellProgress = if (activeDwellZone == Direction.LEFT) dwellProgress else 0f,
      onHoverEnter = { onGazeUpdated(0.15f, 0.5f, Direction.LEFT) },
      onDirectClick = { onDirectionSelected(Direction.LEFT) },
      modifier = Modifier
        .align(Alignment.CenterStart)
        .padding(start = 12.dp)
        .width(115.dp)
        .height(130.dp)
    )

    // Right Zone: RIGHT (Look Right)
    DwellZoneCard(
      title = "LOOK RIGHT",
      subtitle = "Turn Right",
      icon = Icons.Default.ArrowForward,
      direction = Direction.RIGHT,
      isActive = activeDwellZone == Direction.RIGHT,
      isCurrent = currentDirection == Direction.RIGHT,
      dwellProgress = if (activeDwellZone == Direction.RIGHT) dwellProgress else 0f,
      onHoverEnter = { onGazeUpdated(0.85f, 0.5f, Direction.RIGHT) },
      onDirectClick = { onDirectionSelected(Direction.RIGHT) },
      modifier = Modifier
        .align(Alignment.CenterEnd)
        .padding(end = 12.dp)
        .width(115.dp)
        .height(130.dp)
    )

    // Center Zone: STOP
    DwellZoneCard(
      title = "CENTER / STOP",
      subtitle = "Halt Chair",
      icon = Icons.Default.Stop,
      direction = Direction.STOP,
      isActive = activeDwellZone == Direction.STOP,
      isCurrent = currentDirection == Direction.STOP,
      dwellProgress = if (activeDwellZone == Direction.STOP) dwellProgress else 0f,
      onHoverEnter = { onGazeUpdated(0.5f, 0.5f, Direction.STOP) },
      onDirectClick = { onDirectionSelected(Direction.STOP) },
      modifier = Modifier
        .align(Alignment.Center)
        .width(135.dp)
        .height(115.dp),
      isEmergencyStopStyle = true
    )

    // 3. Floating Gaze Reticle / Cursor
    val cursorXPixels = (gazeCursorX * totalWidth.value).dp
    val cursorYPixels = (gazeCursorY * totalHeight.value).dp

    Box(
      modifier = Modifier
        .offset {
          IntOffset(
            x = (gazeCursorX * (totalWidth.toPx() - 36.dp.toPx())).toInt(),
            y = (gazeCursorY * (totalHeight.toPx() - 36.dp.toPx())).toInt()
          )
        }
        .size(36.dp)
        .testTag("gaze_cursor_reticle")
    ) {
      Canvas(modifier = Modifier.fillMaxSize()) {
        val center = Offset(size.width / 2f, size.height / 2f)
        drawCircle(color = CalmingTealLight.copy(alpha = 0.25f), radius = size.width / 2f)
        drawCircle(color = CalmingTealLight, radius = size.width / 2f, style = Stroke(width = 2.dp.toPx()))
        drawCircle(color = Color.White, radius = 4.dp.toPx())
      }
    }

    // 4. Bottom Control Bar: Rapid Double-Blink Indicator + Calibration Button
    Card(
      modifier = Modifier
        .align(Alignment.BottomCenter)
        .fillMaxWidth()
        .padding(horizontal = 12.dp, vertical = 8.dp)
        .testTag("blink_control_bar"),
      shape = RoundedCornerShape(16.dp),
      colors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
      ),
      border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        // Drive status button (Blink 2x to toggle)
        Row(
          modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (isDriveEngaged) SafeGreen.copy(alpha = 0.2f) else EmergencyRed.copy(alpha = 0.15f))
            .clickable { onToggleDrive() }
            .padding(horizontal = 10.dp, vertical = 6.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Icon(
            imageVector = if (isDriveEngaged) Icons.Default.CheckCircle else Icons.Default.PlayArrow,
            contentDescription = "Drive Engagement",
            tint = if (isDriveEngaged) SafeGreen else EmergencyRed,
            modifier = Modifier.size(18.dp)
          )
          Spacer(modifier = Modifier.width(6.dp))
          Column {
            Text(
              text = if (isDriveEngaged) "DRIVE ACTIVE" else "DRIVE STANDBY",
              fontSize = 11.sp,
              fontWeight = FontWeight.Black,
              color = if (isDriveEngaged) SafeGreen else EmergencyRed
            )
            Text(
              text = "Blink 2x to toggle",
              fontSize = 9.sp,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }

        // Quick simulate double blink button
        OutlinedButton(
          onClick = {
            onBlinkDetected()
            // trigger quick haptic feedback if vibrator available
            try {
              val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
              if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(80, VibrationEffect.DEFAULT_AMPLITUDE))
              } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(80)
              }
            } catch (_: Exception) {}
          },
          modifier = Modifier.testTag("simulate_blink_button"),
          shape = RoundedCornerShape(8.dp),
          colors = ButtonDefaults.outlinedButtonColors(contentColor = CalmingTeal)
        ) {
          Icon(Icons.Default.RemoveRedEye, contentDescription = "Blink", modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("Simulate 2x Blink", fontSize = 11.sp)
        }

        // Calibration launcher
        IconButton(
          onClick = onStartCalibration,
          modifier = Modifier.size(36.dp)
        ) {
          Icon(
            imageVector = Icons.Default.Settings,
            contentDescription = "Eye Calibration",
            tint = MaterialTheme.colorScheme.primary
          )
        }
      }
    }
  }
}

@Composable
private fun DwellZoneCard(
  title: String,
  subtitle: String,
  icon: ImageVector,
  direction: Direction,
  isActive: Boolean,
  isCurrent: Boolean,
  dwellProgress: Float,
  onHoverEnter: () -> Unit,
  onDirectClick: () -> Unit,
  modifier: Modifier = Modifier,
  isEmergencyStopStyle: Boolean = false
) {
  val baseBorderColor = when {
    isEmergencyStopStyle -> EmergencyRed
    isCurrent -> CalmingTeal
    isActive -> MedicalBluePrimary
    else -> Color.White.copy(alpha = 0.4f)
  }

  val containerColor = when {
    isEmergencyStopStyle && isCurrent -> EmergencyRed.copy(alpha = 0.35f)
    isEmergencyStopStyle -> EmergencyRed.copy(alpha = 0.18f)
    isCurrent -> CalmingTeal.copy(alpha = 0.4f)
    isActive -> MedicalBluePrimary.copy(alpha = 0.25f)
    else -> Color.Black.copy(alpha = 0.5f)
  }

  val animatedProgress by animateFloatAsState(
    targetValue = dwellProgress,
    label = "dwellProgress_$title"
  )

  Card(
    modifier = modifier
      .testTag("dwell_zone_${direction.name.lowercase()}")
      .clickable { onDirectClick() }
      .pointerInput(Unit) {
        detectTapGestures(
          onPress = {
            onHoverEnter()
          }
        )
      },
    shape = RoundedCornerShape(18.dp),
    colors = CardDefaults.cardColors(containerColor = containerColor),
    border = androidx.compose.foundation.BorderStroke(
      width = if (isActive || isCurrent) 2.5.dp else 1.5.dp,
      color = baseBorderColor
    )
  ) {
    Box(modifier = Modifier.fillMaxSize()) {
      // Dwell progress ring overlay
      if (animatedProgress > 0.01f) {
        Canvas(modifier = Modifier.fillMaxSize()) {
          val sweep = animatedProgress * 360f
          drawArc(
            color = if (isEmergencyStopStyle) EmergencyRed else CalmingTealLight,
            startAngle = -90f,
            sweepAngle = sweep,
            useCenter = false,
            style = Stroke(width = 4.dp.toPx())
          )
        }
      }

      Column(
        modifier = Modifier
          .fillMaxSize()
          .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
      ) {
        Icon(
          imageVector = icon,
          contentDescription = title,
          tint = if (isEmergencyStopStyle) EmergencyRed else Color.White,
          modifier = Modifier.size(28.dp)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
          text = title,
          fontSize = 12.sp,
          fontWeight = FontWeight.Black,
          color = Color.White
        )
        Text(
          text = subtitle,
          fontSize = 10.sp,
          color = Color.White.copy(alpha = 0.8f)
        )

        if (isActive && animatedProgress > 0.05f) {
          Text(
            text = "Dwell: ${(animatedProgress * 100).toInt()}%",
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = CalmingTealLight
          )
        }
      }
    }
  }
}
