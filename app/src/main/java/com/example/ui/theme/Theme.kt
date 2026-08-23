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

/**
 * Phase 4: Refined dark scheme — tonal mapping from Vibrant, not ad-hoc.
 * Keeps HUD midnight (#0A101A) but surfaces now have proper container hierarchy.
 */
private val DarkColorScheme =
  darkColorScheme(
    primary = VibrantPrimaryDark,
    onPrimary = VibrantOnPrimaryDark,
    primaryContainer = VibrantPrimaryContainerDark,
    onPrimaryContainer = VibrantOnPrimaryContainerDark,
    secondary = SecureGreen,
    onSecondary = Color.White,
    secondaryContainer = SecureGreenContainer,
    background = VibrantBackgroundDark,
    onBackground = VibrantOnSurfaceDark,
    surface = VibrantSurfaceDark,
    onSurface = VibrantOnSurfaceDark,
    surfaceVariant = VibrantSurfaceVariantDark,
    onSurfaceVariant = VibrantOnSurfaceVariantDark,
    surfaceContainer = VibrantSurfaceContainerDark,
    outline = VibrantOutline,
    outlineVariant = VibrantOutlineVariant,
    error = AlertRed,
    errorContainer = AlertRedContainer
  )

private val LightColorScheme =
  lightColorScheme(
    primary = VibrantPrimary,
    onPrimary = VibrantOnPrimary,
    primaryContainer = VibrantPrimaryContainer,
    onPrimaryContainer = VibrantOnPrimaryContainer,
    secondary = SecureGreen,
    onSecondary = Color.White,
    secondaryContainer = SecureGreenContainer,
    background = VibrantBackground,
    onBackground = VibrantOnSurface,
    surface = VibrantSurface,
    onSurface = VibrantOnSurface,
    surfaceVariant = VibrantSurfaceVariant,
    onSurfaceVariant = VibrantOnSurfaceVariant,
    surfaceContainer = VibrantSurfaceVariant,
    outline = VibrantOutline,
    outlineVariant = VibrantOutlineVariant,
    error = AlertRed,
    errorContainer = AlertRedContainer
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  // Disabling dynamic systems coloring by default to force the pristine Vibrant Palette
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }

      darkTheme -> DarkColorScheme
      else -> LightColorScheme
    }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
