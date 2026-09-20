package com.elder.desktop.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// 与 demo/index.html 严格对齐的高对比配色
val EldBg = Color(0xFF16324F)
val EldBg2 = Color(0xFF1E4166)
val EldCard = Color(0xFFFFFFFF)
val EldInk = Color(0xFF1F2D3D)
val EldMuted = Color(0xFF6B8194)
val EldPhone = Color(0xFF2FA36B)
val EldFamily = Color(0xFFE8862E)
val EldPhoto = Color(0xFF3B82C4)
val EldSos = Color(0xFFE24B3A)
val EldWhite = Color(0xFFFFFFFF)
val EldSub = Color(0xFFA9C0D4)

private val EldColorScheme = darkColorScheme(
    primary = EldPhone,
    background = EldBg,
    surface = EldBg,
    onBackground = EldWhite,
    onSurface = EldWhite,
)

// 超大字体系：长辈看得清
val EldTypography = Typography(
    displayLarge = TextStyle(fontSize = 96.sp, fontWeight = FontWeight.Black, lineHeight = 100.sp),
    headlineLarge = TextStyle(fontSize = 34.sp, fontWeight = FontWeight.Bold),
    headlineMedium = TextStyle(fontSize = 30.sp, fontWeight = FontWeight.Bold),
    bodyLarge = TextStyle(fontSize = 28.sp, fontWeight = FontWeight.Bold),
    bodyMedium = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.Medium),
    labelLarge = TextStyle(fontSize = 28.sp, fontWeight = FontWeight.Black),
)

val EldShapes = Shapes(
    large = RoundedCornerShape(28.dp),
    medium = RoundedCornerShape(22.dp),
    small = RoundedCornerShape(16.dp),
)

@Composable
fun ElderDesktopTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = EldColorScheme,
        typography = EldTypography,
        shapes = EldShapes,
        content = content,
    )
}
