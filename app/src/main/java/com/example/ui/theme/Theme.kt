package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
  primary = MedicalBlueLight,
  onPrimary = TechNavy900,
  primaryContainer = MedicalBlueDark,
  onPrimaryContainer = Color(0xFFE0F2FE),
  secondary = CalmingTealLight,
  onSecondary = TechNavy900,
  secondaryContainer = CalmingTealDark,
  onSecondaryContainer = Color(0xFFCCFBF1),
  tertiary = HighContrastAmber,
  error = EmergencyRedLight,
  background = SurfaceDark,
  surface = SurfaceCardDark,
  onBackground = TextPrimaryDark,
  onSurface = TextPrimaryDark,
  surfaceVariant = TechNavy700,
  onSurfaceVariant = TextSecondaryDark,
  outline = TechNavy600
)

private val LightColorScheme = lightColorScheme(
  primary = MedicalBluePrimary,
  onPrimary = Color.White,
  primaryContainer = Color(0xFFE0F2FE),
  onPrimaryContainer = MedicalBlueDark,
  secondary = CalmingTeal,
  onSecondary = Color.White,
  secondaryContainer = Color(0xFFCCFBF1),
  onSecondaryContainer = CalmingTealDark,
  tertiary = HighContrastAmber,
  error = EmergencyRed,
  background = SurfaceLight,
  surface = SurfaceCardLight,
  onBackground = TextPrimaryLight,
  onSurface = TextPrimaryLight,
  surfaceVariant = Color(0xFFE2E8F0),
  onSurfaceVariant = TextSecondaryLight,
  outline = Color(0xFFCBD5E1)
)

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  dynamicColor: Boolean = false, // disabled by default to maintain consistent medical-tech aesthetic
  content: @Composable () -> Unit,
) {
  val colorScheme = when {
    dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
      val context = LocalContext.current
      if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    }
    darkTheme -> DarkColorScheme
    else -> LightColorScheme
  }

  MaterialTheme(
    colorScheme = colorScheme,
    typography = Typography,
    content = content
  )
}
