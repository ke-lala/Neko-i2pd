package org.purplei2p.i2pd

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Typography

private val MinimalLightColors = lightColorScheme(
    primary = Color(0xFF111111),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFE8E8E8),
    onPrimaryContainer = Color(0xFF111111),
    inversePrimary = Color(0xFFF5F5F5),
    secondary = Color(0xFF444444),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFEDEDED),
    onSecondaryContainer = Color(0xFF171717),
    tertiary = Color(0xFF555555),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFF0F0F0),
    onTertiaryContainer = Color(0xFF202020),
    background = Color(0xFFFFFFFF),
    onBackground = Color(0xFF111111),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF111111),
    surfaceVariant = Color(0xFFF1F1F1),
    onSurfaceVariant = Color(0xFF444444),
    surfaceTint = Color(0xFF111111),
    inverseSurface = Color(0xFF202020),
    inverseOnSurface = Color(0xFFF5F5F5),
    error = Color(0xFF555555),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFEAEAEA),
    onErrorContainer = Color(0xFF222222),
    outline = Color(0xFF777777),
    outlineVariant = Color(0xFFD4D4D4),
    scrim = Color(0x99000000),
    surfaceBright = Color(0xFFFFFFFF),
    surfaceDim = Color(0xFFE7E7E7),
    surfaceContainer = Color(0xFFF5F5F5),
    surfaceContainerHigh = Color(0xFFEFEFEF),
    surfaceContainerHighest = Color(0xFFE8E8E8),
    surfaceContainerLow = Color(0xFFFAFAFA),
    surfaceContainerLowest = Color(0xFFFFFFFF),
)

private val MinimalDarkColors = darkColorScheme(
    primary = Color(0xFFF5F5F5),
    onPrimary = Color(0xFF111111),
    primaryContainer = Color(0xFF333333),
    onPrimaryContainer = Color(0xFFF5F5F5),
    inversePrimary = Color(0xFF222222),
    secondary = Color(0xFFCCCCCC),
    onSecondary = Color(0xFF111111),
    secondaryContainer = Color(0xFF292929),
    onSecondaryContainer = Color(0xFFEAEAEA),
    tertiary = Color(0xFFBDBDBD),
    onTertiary = Color(0xFF111111),
    tertiaryContainer = Color(0xFF303030),
    onTertiaryContainer = Color(0xFFEAEAEA),
    background = Color(0xFF000000),
    onBackground = Color(0xFFF2F2F2),
    surface = Color(0xFF000000),
    onSurface = Color(0xFFF2F2F2),
    surfaceVariant = Color(0xFF202020),
    onSurfaceVariant = Color(0xFFD0D0D0),
    surfaceTint = Color(0xFFF2F2F2),
    inverseSurface = Color(0xFFEAEAEA),
    inverseOnSurface = Color(0xFF181818),
    error = Color(0xFFCCCCCC),
    onError = Color(0xFF111111),
    errorContainer = Color(0xFF383838),
    onErrorContainer = Color(0xFFEAEAEA),
    outline = Color(0xFF888888),
    outlineVariant = Color(0xFF444444),
    scrim = Color(0x99000000),
    surfaceBright = Color(0xFF292929),
    surfaceDim = Color(0xFF000000),
    surfaceContainer = Color(0xFF111111),
    surfaceContainerHigh = Color(0xFF1A1A1A),
    surfaceContainerHighest = Color(0xFF222222),
    surfaceContainerLow = Color(0xFF0A0A0A),
    surfaceContainerLowest = Color(0xFF000000),
)

private val MinimalTypography = Typography(
    labelSmall = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 12.sp, lineHeight = 16.sp),
    labelMedium = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 13.sp, lineHeight = 18.sp),
    labelLarge = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 14.sp, lineHeight = 20.sp),
)

private val MinimalShapes = Shapes(
    extraSmall = RoundedCornerShape(2.dp),
    small = RoundedCornerShape(2.dp),
    medium = RoundedCornerShape(3.dp),
    large = RoundedCornerShape(4.dp),
    extraLarge = RoundedCornerShape(4.dp),
)

@Composable
internal fun MinimalTheme(dark: Boolean, content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (dark) MinimalDarkColors else MinimalLightColors,
        typography = MinimalTypography,
        shapes = MinimalShapes,
        content = content,
    )
}
