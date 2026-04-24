package ru.company.izhs_planner.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

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

val Typography = androidx.compose.material3.Typography()
    .run {
        displayLarge = androidx.compose.material3.MaterialTheme.typography.displayLarge.copy(
            fontFamily = androidx.compose.ui.text.font.FontFamily.Default
        )
        displayMedium = androidx.compose.material3.MaterialTheme.typography.displayMedium.copy(
            fontFamily = androidx.compose.ui.text.font.FontFamily.Default
        )
        displaySmall = androidx.compose.material3.MaterialTheme.typography.displaySmall.copy(
            fontFamily = androidx.compose.ui.text.font.FontFamily.Default
        )
        headlineLarge = androidx.compose.material3.MaterialTheme.typography.headlineLarge.copy(
            fontFamily = androidx.compose.ui.text.font.FontFamily.Default
        )
        headlineMedium = androidx.compose.material3.MaterialTheme.typography.headlineMedium.copy(
            fontFamily = androidx.compose.ui.text.font.FontFamily.Default
        )
        headlineSmall = androidx.compose.material3.MaterialTheme.typography.headlineSmall.copy(
            fontFamily = androidx.compose.ui.text.font.FontFamily.Default
        )
        titleLarge = androidx.compose.material3.MaterialTheme.typography.titleLarge.copy(
            fontFamily = androidx.compose.ui.text.font.FontFamily.Default
        )
        titleMedium = androidx.compose.material3.MaterialTheme.typography.titleMedium.copy(
            fontFamily = androidx.compose.ui.text.font.FontFamily.Default
        )
        titleSmall = androidx.compose.material3.MaterialTheme.typography.titleSmall.copy(
            fontFamily = androidx.compose.ui.text.font.FontFamily.Default
        )
        bodyLarge = androidx.compose.material3.MaterialTheme.typography.bodyLarge.copy(
            fontFamily = androidx.compose.ui.text.font.FontFamily.Default
        )
        bodyMedium = androidx.compose.material3.MaterialTheme.typography.bodyMedium.copy(
            fontFamily = androidx.compose.ui.text.font.FontFamily.Default
        )
        bodySmall = androidx.compose.material3.MaterialTheme.typography.bodySmall.copy(
            fontFamily = androidx.compose.ui.text.font.FontFamily.Default
        )
        labelLarge = androidx.compose.material3.MaterialTheme.typography.labelLarge.copy(
            fontFamily = androidx.compose.ui.text.font.FontFamily.Default
        )
        labelMedium = androidx.compose.material3.MaterialTheme.typography.labelMedium.copy(
            fontFamily = androidx.compose.ui.text.font.FontFamily.Default
        )
        labelSmall = androidx.compose.material3.MaterialTheme.typography.labelSmall.copy(
            fontFamily = androidx.compose.ui.text.font.FontFamily.Default
        )
        this
    }