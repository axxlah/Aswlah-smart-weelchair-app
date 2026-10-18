package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.RemoveRedEye
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.theme.CalmingTeal
import com.example.ui.theme.MedicalBluePrimary
import com.example.ui.theme.SafeGreen
import com.example.ui.viewmodel.CalibrationStep

@Composable
fun CalibrationDialog(
  step: CalibrationStep,
  onNextStep: () -> Unit,
  onDismiss: () -> Unit
) {
  Dialog(onDismissRequest = onDismiss) {
    Card(
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp)
        .testTag("calibration_dialog"),
      shape = RoundedCornerShape(24.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
      border = androidx.compose.foundation.BorderStroke(1.5.dp, CalmingTeal)
    ) {
      Column(
        modifier = Modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "Gaze & Blink Calibration",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
          )
          Box(
            modifier = Modifier
              .clip(RoundedCornerShape(8.dp))
              .background(CalmingTeal.copy(alpha = 0.2f))
              .padding(horizontal = 8.dp, vertical = 2.dp)
          ) {
            Text(
              text = "Step ${step.ordinal + 1}/7",
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold,
              color = CalmingTeal
            )
          }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Step-specific visual guide
        AnimatedContent(targetState = step, label = "calibration_content") { currentStep ->
          Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
          ) {
            when (currentStep) {
              CalibrationStep.INTRO -> {
                Icon(
                  imageVector = Icons.Default.RemoveRedEye,
                  contentDescription = "Calibration Intro",
                  modifier = Modifier.size(56.dp),
                  tint = CalmingTeal
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                  text = "Welcome to Eye Tracking Calibration",
                  style = MaterialTheme.typography.titleSmall,
                  fontWeight = FontWeight.Bold,
                  textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                  text = "Position your phone securely in front of your face at eye level (around 40-50 cm away). We'll calibrate your center, horizontal, downward gaze, and double-blink sensitivity.",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  textAlign = TextAlign.Center
                )
              }
              CalibrationStep.CENTER -> {
                TargetDot(alignment = Alignment.Center, label = "Look directly at Center")
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                  text = "Look straight ahead into the center target without moving your head.",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  textAlign = TextAlign.Center
                )
              }
              CalibrationStep.LOOK_LEFT -> {
                TargetDot(alignment = Alignment.CenterStart, label = "Look Left (Turn Left)")
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                  text = "Cast your gaze to the left corner. This engages the Left steering zone.",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  textAlign = TextAlign.Center
                )
              }
              CalibrationStep.LOOK_RIGHT -> {
                TargetDot(alignment = Alignment.CenterEnd, label = "Look Right (Turn Right)")
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                  text = "Cast your gaze to the right corner. This engages the Right steering zone.",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  textAlign = TextAlign.Center
                )
              }
              CalibrationStep.LOOK_DOWN -> {
                TargetDot(alignment = Alignment.BottomCenter, label = "Look Down (Reverse)")
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                  text = "Cast your gaze downward. This engages Backward / Reverse navigation.",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  textAlign = TextAlign.Center
                )
              }
              CalibrationStep.DOUBLE_BLINK -> {
                Icon(
                  imageVector = Icons.Default.RemoveRedEye,
                  contentDescription = "Double Blink",
                  modifier = Modifier.size(56.dp),
                  tint = SafeGreen
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                  text = "Rapid Double-Blink Test",
                  style = MaterialTheme.typography.titleSmall,
                  fontWeight = FontWeight.Bold,
                  textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                  text = "Blink your eyes twice in quick succession. This is your master emergency power/drive toggle switch to prevent accidental wheelchair movement.",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  textAlign = TextAlign.Center
                )
              }
              CalibrationStep.COMPLETE -> {
                Icon(
                  imageVector = Icons.Default.Check,
                  contentDescription = "Calibration Done",
                  modifier = Modifier.size(56.dp),
                  tint = SafeGreen
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                  text = "Calibration Successfully Applied!",
                  style = MaterialTheme.typography.titleSmall,
                  fontWeight = FontWeight.Bold,
                  textAlign = TextAlign.Center,
                  color = SafeGreen
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                  text = "Your dwell threshold (1.0 sec) and rapid-blink sensitivity have been saved to memory.",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  textAlign = TextAlign.Center
                )
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Action Buttons
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          OutlinedButton(
            onClick = onDismiss,
            modifier = Modifier.weight(1f)
          ) {
            Text("Cancel")
          }

          Button(
            onClick = onNextStep,
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.buttonColors(containerColor = MedicalBluePrimary)
          ) {
            Text(if (step == CalibrationStep.COMPLETE) "Done" else "Next")
          }
        }
      }
    }
  }
}

@Composable
private fun TargetDot(
  alignment: Alignment,
  label: String
) {
  Box(
    modifier = Modifier
      .fillMaxWidth()
      .height(90.dp)
      .clip(RoundedCornerShape(12.dp))
      .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
      .padding(8.dp)
  ) {
    Box(
      modifier = Modifier
        .align(alignment)
        .size(34.dp)
        .clip(CircleShape)
        .background(CalmingTeal)
        .border(3.dp, Color.White, CircleShape),
      contentAlignment = Alignment.Center
    ) {
      Box(
        modifier = Modifier
          .size(10.dp)
          .clip(CircleShape)
          .background(Color.White)
      )
    }

    Text(
      text = label,
      fontSize = 11.sp,
      fontWeight = FontWeight.Bold,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      modifier = Modifier.align(Alignment.Center)
    )
  }
}
