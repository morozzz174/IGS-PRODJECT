package ru.company.izhs_planner.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF1B5E42),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFC8E6D0),
    onPrimaryContainer = Color(0xFF0D3123),
    secondary = Color(0xFF4E6353),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD0E4D8),
    onSecondaryContainer = Color(0xFF1C261E),
    tertiary = Color(0xFF3E6B50),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFD0E4D8),
    onTertiaryContainer = Color(0xFF0D311F),
    background = Color(0xFFFAFAF8),
    onBackground = Color(0xFF1C1B1B),
    surface = Color(0xFFFAFAF8),
    onSurface = Color(0xFF1C1B1B),
    surfaceVariant = Color(0xFFE0E6E1),
    onSurfaceVariant = Color(0xFF434B47),
    outline = Color(0xFF72796E),
    error = Color(0xFFB3261E),
    onError = Color.White,
    errorContainer = Color(0xFFF9DEDC),
    onErrorContainer = Color(0xFF410E0B)
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF81C784),
    onPrimary = Color(0xFF0D3123),
    primaryContainer = Color(0xFF2E5D3B),
    onPrimaryContainer = Color(0xFFC8E6D0),
    secondary = Color(0xFFA5D6A7),
    onSecondary = Color(0xFF1C261E),
    secondaryContainer = Color(0xFF3A5D42),
    onSecondaryContainer = Color(0xFFD0E4D8),
    tertiary = Color(0xFF81C784),
    onTertiary = Color(0xFF0D311F),
    tertiaryContainer = Color(0xFF2E5D3B),
    onTertiaryContainer = Color(0xFFD0E4D8),
    background = Color(0xFF1C1C1B),
    onBackground = Color(0xFFE0E0DE),
    surface = Color(0xFF1C1C1B),
    onSurface = Color(0xFFE0E0DE),
    surfaceVariant = Color(0xFF434B47),
    onSurfaceVariant = Color(0xFFC4C9C5),
    outline = Color(0xFF8E938A),
    error = Color(0xFFF2B8B5),
    onError = Color(0xFF601410),
    errorContainer = Color(0xFF8C1D18),
    onErrorContainer = Color(0xFFF9DEDC)
)

@Composable
fun IzhsPlannerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    themeMode: ThemeModePreference = ThemeModePreference.SYSTEM,
    content: @Composable () -> Unit
) {
    val useDarkTheme = when (themeMode) {
        ThemeModePreference.LIGHT -> false
        ThemeModePreference.DARK -> true
        ThemeModePreference.SYSTEM -> darkTheme
    }
    
    val colorScheme = if (useDarkTheme) DarkColorScheme else LightColorScheme
    
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography(),
        content = content
    )
}

enum class ThemeModePreference {
    LIGHT,
    DARK,
    SYSTEM
}

val Typography = Typography(
    displayLarge = Typography().displayLarge.copy(fontFamily = FontFamily.Default),
    displayMedium = Typography().displayMedium.copy(fontFamily = FontFamily.Default),
    displaySmall = Typography().displaySmall.copy(fontFamily = FontFamily.Default),
    headlineLarge = Typography().headlineLarge.copy(fontFamily = FontFamily.Default),
    headlineMedium = Typography().headlineMedium.copy(fontFamily = FontFamily.Default),
    headlineSmall = Typography().headlineSmall.copy(fontFamily = FontFamily.Default),
    titleLarge = Typography().titleLarge.copy(fontFamily = FontFamily.Default),
    titleMedium = Typography().titleMedium.copy(fontFamily = FontFamily.Default),
    titleSmall = Typography().titleSmall.copy(fontFamily = FontFamily.Default),
    bodyLarge = Typography().bodyLarge.copy(fontFamily = FontFamily.Default),
    bodyMedium = Typography().bodyMedium.copy(fontFamily = FontFamily.Default),
    bodySmall = Typography().bodySmall.copy(fontFamily = FontFamily.Default),
    labelLarge = Typography().labelLarge.copy(fontFamily = FontFamily.Default),
    labelMedium = Typography().labelMedium.copy(fontFamily = FontFamily.Default),
    labelSmall = Typography().labelSmall.copy(fontFamily = FontFamily.Default)
)