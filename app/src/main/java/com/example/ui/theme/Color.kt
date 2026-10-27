package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// Core Dark AMOLED Palette
val AmoledBackground = Color(0xFF030712) // Pure deep night
val AmoledSurface = Color(0xFF090E1A)    // High-tech dark panel
val AmoledSurfaceVariant = Color(0xFF111827) // Card surface
val AmoledBorder = Color(0xFF1E293B)     // Subtle stroke

// Glowing Accents - Pro Audio Studio
val CyanAccent = Color(0xFF00E5FF)       // Electric Cyan
val CyanAccentGlow = Color(0x6600E5FF)   // Glowing bloom
val TealAccent = Color(0xFF14B8A6)       // Studio Teal
val TealMuted = Color(0xFF0D9488)

val VioletAccent = Color(0xFFA855F7)     // Subwoofer Violet
val VioletAccentGlow = Color(0x66A855F7)
val AmberAccent = Color(0xFFF59E0B)      // Tube Warmth Amber
val AmberAccentGlow = Color(0x66F59E0B)
val EmeraldAccent = Color(0xFF10B981)    // Signal Active Green

// Text & Sliders
val TextPrimary = Color(0xFFF8FAFC)
val TextSecondary = Color(0xFF94A3B8)
val TextMuted = Color(0xFF64748B)

val SliderTrackInactive = Color(0xFF1E293B)
val SliderTrackActive = Color(0xFF00E5FF)
val SliderThumb = Color(0xFFFFFFFF)

// Accent Themes Available for User Customization
enum class AccentTheme(
    val title: String,
    val primary: Color,
    val glow: Color,
    val secondary: Color
) {
    NEON_CYAN("Neon Cyan", CyanAccent, CyanAccentGlow, TealAccent),
    AURORA_VIOLET("Aurora Violet", VioletAccent, VioletAccentGlow, Color(0xFFEC4899)),
    CYBER_AMBER("Cyber Amber", AmberAccent, AmberAccentGlow, Color(0xFFEF4444)),
    EMERALD_PULSE("Emerald Pulse", EmeraldAccent, Color(0x6610B981), CyanAccent)
}
