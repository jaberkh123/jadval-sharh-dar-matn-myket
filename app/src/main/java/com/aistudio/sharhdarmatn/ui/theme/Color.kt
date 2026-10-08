package com.aistudio.sharhdarmatn.ui.theme

import androidx.compose.ui.graphics.Color

// Blue Light Palette
val LightBackground = Color(0xFFCAF0F8)
val LightSurface = Color(0xFFADE8F4)
val LightPrimary = Color(0xFF0077B6)
val LightOnPrimary = Color(0xFFCAF0F8)
val LightSecondary = Color(0xFF0096C7)
val LightOnSecondary = Color(0xFFCAF0F8)
val LightOnBackground = Color(0xFF03045E)
val LightOnSurface = Color(0xFF03045E)
val LightBlock = Color(0xFF111111) // خانهٔ سیاهِ جداکننده — مشکی توپ مثل نسخهٔ کاغذی

// Custom Dark Palette based on ["#04151f","#183a37","#efd6ac","#c44900","#432534"]
val DarkBackground = Color(0xFF04151F) // Very Dark Blue
val DarkSurface = Color(0xFF183A37) // Teal/Green
val DarkPrimary = Color(0xFFC44900) // Burnt Orange
val DarkOnPrimary = Color(0xFFEFD6AC) // Cream
val DarkSecondary = Color(0xFF432534) // Plum
val DarkOnSecondary = Color(0xFFEFD6AC) // Cream
val DarkOnBackground = Color(0xFFEFD6AC) // Cream
val DarkOnSurface = Color(0xFFEFD6AC) // Cream

val DarkBlock = Color(0xFF04151F)
val CellDarkBg = Color(0xFF183A37)

// General UI accent states
val IncorrectLight = Color(0xFF90E0EF)
val IncorrectDark = Color(0xFF432534) // Plum
val IncorrectTextLight = Color(0xFFFF5252)
val IncorrectTextDark = Color(0xFFC44900) // Burnt Orange

val CorrectLight = Color(0xFF48CAE4)
val CorrectDark = Color(0xFF183A37) // Teal/Green
val CorrectTextLight = Color(0xFF03045E)
val CorrectTextDark = Color(0xFFEFD6AC) // Cream

val EmptyCellLight = Color.White
val EmptyCellDark = Color(0xFF183A37) // Teal/Green

val SelectedLight = Color(0xFFA2D2FF)   // آبیِ پالتِ کاربر — خانهٔ فعال
val SelectedDark = Color(0xFFC44900) // Burnt Orange
val HighlightedLight = Color(0xFFBDE0FE) // آبیِ پاستلی — بقیهٔ خانه‌های لاینِ فعال
val HighlightedDark = Color(0xFF635A22) // Pale Yellow (Dark)

// Per-GameMode color palettes
data class GameModeColors(
    val primary: Color,
    val primaryLight: Color,
    val primaryContainer: Color,
    val cardBg: Color,
    val headerBg: Color,
    val accentText: Color,
    val starTint: Color,
)

val EasyModeColors = GameModeColors(
    primary = Color(0xFF0077B6),        // Blue
    primaryLight = Color(0xFFCAF0F8),    // Light blue
    primaryContainer = Color(0xFFADE8F4),
    cardBg = Color(0xFFCAF0F8),
    headerBg = Color(0xFFADE8F4),
    accentText = Color(0xFF0077B6),
    starTint = Color(0xFF0096C7),
)

val HardModeColors = GameModeColors(
    primary = Color(0xFFE65100),        // Light orange
    primaryLight = Color(0xFFFFE0B2),   // Light orange bg
    primaryContainer = Color(0xFFFFCC80),
    cardBg = Color(0xFFFFF3E0),
    headerBg = Color(0xFFFFE0B2),
    accentText = Color(0xFFE65100),
    starTint = Color(0xFFFF9800),
)

val BigGridModeColors = GameModeColors(
    primary = Color(0xFF6A1B9A),        // Purple
    primaryLight = Color(0xFFE1BEE7),   // Light purple bg
    primaryContainer = Color(0xFFCE93D8),
    cardBg = Color(0xFFF3E5F5),
    headerBg = Color(0xFFE1BEE7),
    accentText = Color(0xFF6A1B9A),
    starTint = Color(0xFFAB47BC),
)

