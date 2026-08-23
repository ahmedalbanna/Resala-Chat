package com.example.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Phase 4: Consolidated Vibrant Palette — semantic tokens for light/dark + a11y contrast (WCAG AA).
 * Light scheme is primary; dark is tonal mapping (not ad-hoc) via Vibrant hues at 70-80% luminance.
 */

// Core Vibrant (Light)
val VibrantPrimary = Color(0xFF005FB0)       // Vibrant Brand Blue
val VibrantOnPrimary = Color(0xFFFFFFFF)
val VibrantPrimaryContainer = Color(0xFFD1E4FF) // Light Blue Container
val VibrantOnPrimaryContainer = Color(0xFF001D36) // Dark Deep Blue Title

val VibrantBackground = Color(0xFFF8F9FF)    // Crisp Soft Blue-White
val VibrantSurface = Color(0xFFFFFFFF)       // Clean sheets
val VibrantSurfaceVariant = Color(0xFFF1F0F4) // Warm Grey/Blue Surface
val VibrantOnSurface = Color(0xFF1A1C1E)      // Charcoal Dark Text
val VibrantOnSurfaceVariant = Color(0xFF44474E) // Medium Slate Grey

val VibrantOutline = Color(0xFF74777F)       // M3 outlines
val VibrantOutlineVariant = Color(0xFFC4C7C5)
val SecureGreen = Color(0xFF34A853)          // Vibrant Safety Green
val SecureGreenContainer = Color(0xFFC8F0D0)
val AlertRed = Color(0xFFBA1A1A)             // Elegant Material 3 Red
val AlertRedContainer = Color(0xFFFFDAD6)

// Dark — tonal mapping (Phase 4 refines ad-hoc #0A101A)
val VibrantPrimaryDark = Color(0xFF9ECAFF)
val VibrantOnPrimaryDark = Color(0xFF003258)
val VibrantPrimaryContainerDark = Color(0xFF00497D)
val VibrantOnPrimaryContainerDark = Color(0xFFD1E4FF)

val VibrantBackgroundDark = Color(0xFF0A101A) // Midnight keep for HUD
val VibrantSurfaceDark = Color(0xFF121B2D)
val VibrantSurfaceVariantDark = Color(0xFF192438)
val VibrantSurfaceContainerDark = Color(0xFF1D2A3A)
val VibrantOnSurfaceDark = Color(0xFFE3E2E6)
val VibrantOnSurfaceVariantDark = Color(0xFF94A3B8)

// Compatibility aliases
val EmeraldPrimary = SecureGreen
val EmeraldDark = VibrantPrimary
val EmeraldLight = SecureGreen

val SlateBackground = VibrantBackground
val SlateSurface = VibrantSurface
val SlatePanel = VibrantSurfaceVariant

val TextPrimary = VibrantOnSurface
val TextSecondary = VibrantOnSurfaceVariant
val TextMuted = Color(0xFF74777F)

val AccentBlue = VibrantPrimary
val SignalYellow = Color(0xFFFBBF24)

