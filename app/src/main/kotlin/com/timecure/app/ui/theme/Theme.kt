package com.timecure.app.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

enum class AppThemePalette(
    val displayName: String,
    val tagline: String,
    val previewPrimary: Color,
    val previewSecondary: Color,
    val previewTertiary: Color
) {
    OCEAN_DAWN("Ocean Dawn", "Crisp blue & warm amber", Color(0xFF0061A4), Color(0xFF535F70), Color(0xFF8B5000)),
    EMERALD_FOREST("Emerald", "Botanical green & gold", Color(0xFF006B54), Color(0xFF4B635B), Color(0xFF8A5A00)),
    SUNSET_CORAL("Sunset Coral", "Energetic coral & warm clay", Color(0xFFB33B15), Color(0xFF77574D), Color(0xFF6B5E2F)),
    ROYAL_AMETHYST("Amethyst", "Deep violet & teal accent", Color(0xFF6342C9), Color(0xFF5F5B71), Color(0xFF00696E)),
    MIDNIGHT_SLATE("Graphite", "Minimal slate & cobalt", Color(0xFF334E68), Color(0xFF52606D), Color(0xFF00639A)),
    CRIMSON_PULSE("Crimson", "High-focus ruby & amber", Color(0xFFA6243F), Color(0xFF75565B), Color(0xFF875200));

    companion object {
        fun fromString(raw: String?): AppThemePalette =
            entries.firstOrNull { it.name == raw } ?: OCEAN_DAWN
    }
}

val TimecureLightColors: ColorScheme = lightColorScheme(
    primary = primaryLight, onPrimary = onPrimaryLight,
    primaryContainer = primaryContainerLight, onPrimaryContainer = onPrimaryContainerLight,
    secondary = secondaryLight, onSecondary = onSecondaryLight,
    secondaryContainer = secondaryContainerLight, onSecondaryContainer = onSecondaryContainerLight,
    tertiary = tertiaryLight, onTertiary = onTertiaryLight,
    tertiaryContainer = tertiaryContainerLight, onTertiaryContainer = onTertiaryContainerLight,
    error = errorLight, onError = onErrorLight,
    errorContainer = errorContainerLight, onErrorContainer = onErrorContainerLight,
    background = backgroundLight, onBackground = onBackgroundLight,
    surface = surfaceLight, onSurface = onSurfaceLight,
    surfaceVariant = surfaceVariantLight, onSurfaceVariant = onSurfaceVariantLight,
    outline = outlineLight, outlineVariant = outlineVariantLight,
    inverseSurface = inverseSurfaceLight, inverseOnSurface = inverseOnSurfaceLight,
    inversePrimary = inversePrimaryLight,
    surfaceDim = surfaceDimLight, surfaceBright = surfaceBrightLight,
    surfaceContainerLowest = surfaceContainerLowestLight, surfaceContainerLow = surfaceContainerLowLight,
    surfaceContainer = surfaceContainerLight, surfaceContainerHigh = surfaceContainerHighLight,
    surfaceContainerHighest = surfaceContainerHighestLight
)

val TimecureDarkColors: ColorScheme = darkColorScheme(
    primary = primaryDark, onPrimary = onPrimaryDark,
    primaryContainer = primaryContainerDark, onPrimaryContainer = onPrimaryContainerDark,
    secondary = secondaryDark, onSecondary = onSecondaryDark,
    secondaryContainer = secondaryContainerDark, onSecondaryContainer = onSecondaryContainerDark,
    tertiary = tertiaryDark, onTertiary = onTertiaryDark,
    tertiaryContainer = tertiaryContainerDark, onTertiaryContainer = onTertiaryContainerDark,
    error = errorDark, onError = onErrorDark,
    errorContainer = errorContainerDark, onErrorContainer = onErrorContainerDark,
    background = backgroundDark, onBackground = onBackgroundDark,
    surface = surfaceDark, onSurface = onSurfaceDark,
    surfaceVariant = surfaceVariantDark, onSurfaceVariant = onSurfaceVariantDark,
    outline = outlineDark, outlineVariant = outlineVariantDark,
    inverseSurface = inverseSurfaceDark, inverseOnSurface = inverseOnSurfaceDark,
    inversePrimary = inversePrimaryDark,
    surfaceDim = surfaceDimDark, surfaceBright = surfaceBrightDark,
    surfaceContainerLowest = surfaceContainerLowestDark, surfaceContainerLow = surfaceContainerLowDark,
    surfaceContainer = surfaceContainerDark, surfaceContainerHigh = surfaceContainerHighDark,
    surfaceContainerHighest = surfaceContainerHighestDark
)

private fun paletteScheme(palette: AppThemePalette, dark: Boolean): ColorScheme = when (palette) {
    AppThemePalette.OCEAN_DAWN -> if (dark) TimecureDarkColors else TimecureLightColors

    AppThemePalette.EMERALD_FOREST -> if (dark) TimecureDarkColors.copy(
        primary = Color(0xFF6ADBB8), onPrimary = Color(0xFF00382B),
        primaryContainer = Color(0xFF00513F), onPrimaryContainer = Color(0xFF88F8D3),
        secondary = Color(0xFFB3CCC2), onSecondary = Color(0xFF1E352E),
        secondaryContainer = Color(0xFF344C44), onSecondaryContainer = Color(0xFFCFE9DE),
        tertiary = Color(0xFFF6BE48), onTertiary = Color(0xFF452B00),
        tertiaryContainer = Color(0xFF634000), onTertiaryContainer = Color(0xFFFFDF9E),
        background = Color(0xFF0E1513), surface = Color(0xFF0E1513),
        surfaceContainerLow = Color(0xFF161D1A), surfaceContainer = Color(0xFF1A211E),
        surfaceContainerHigh = Color(0xFF242C29), surfaceContainerHighest = Color(0xFF2F3733)
    ) else TimecureLightColors.copy(
        primary = Color(0xFF006B54), onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFF88F8D3), onPrimaryContainer = Color(0xFF002018),
        secondary = Color(0xFF4B635B), onSecondary = Color(0xFFFFFFFF),
        secondaryContainer = Color(0xFFCFE9DE), onSecondaryContainer = Color(0xFF082019),
        tertiary = Color(0xFF845400), onTertiary = Color(0xFFFFFFFF),
        tertiaryContainer = Color(0xFFFFDDB2), onTertiaryContainer = Color(0xFF2A1800),
        background = Color(0xFFF6FBF8), surface = Color(0xFFF6FBF8),
        surfaceContainerLow = Color(0xFFF0F5F2), surfaceContainer = Color(0xFFEAEFEC),
        surfaceContainerHigh = Color(0xFFE4E9E6), surfaceContainerHighest = Color(0xFFDFE4E1)
    )

    AppThemePalette.SUNSET_CORAL -> if (dark) TimecureDarkColors.copy(
        primary = Color(0xFFFFB59D), onPrimary = Color(0xFF5F1600),
        primaryContainer = Color(0xFF872506), onPrimaryContainer = Color(0xFFFFDBD0),
        secondary = Color(0xFFE7BDB0), onSecondary = Color(0xFF442A21),
        secondaryContainer = Color(0xFF5D4036), onSecondaryContainer = Color(0xFFFFDBD0),
        tertiary = Color(0xFFD6C78E), onTertiary = Color(0xFF393005),
        tertiaryContainer = Color(0xFF51471A), onTertiaryContainer = Color(0xFFF3E3A8),
        background = Color(0xFF1A1210), surface = Color(0xFF1A1210),
        surfaceContainerLow = Color(0xFF221A17), surfaceContainer = Color(0xFF271E1B),
        surfaceContainerHigh = Color(0xFF322825), surfaceContainerHighest = Color(0xFF3D3330)
    ) else TimecureLightColors.copy(
        primary = Color(0xFFB33B15), onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFFFFDBD0), onPrimaryContainer = Color(0xFF3B0B00),
        secondary = Color(0xFF77574D), onSecondary = Color(0xFFFFFFFF),
        secondaryContainer = Color(0xFFFFDBD0), onSecondaryContainer = Color(0xFF2C150E),
        tertiary = Color(0xFF6A5E2F), onTertiary = Color(0xFFFFFFFF),
        tertiaryContainer = Color(0xFFF3E3A8), onTertiaryContainer = Color(0xFF221B00),
        background = Color(0xFFFFF8F6), surface = Color(0xFFFFF8F6),
        surfaceContainerLow = Color(0xFFFFF1ED), surfaceContainer = Color(0xFFFCEAE5),
        surfaceContainerHigh = Color(0xFFF6E4DF), surfaceContainerHighest = Color(0xFFF0DED9)
    )

    AppThemePalette.ROYAL_AMETHYST -> if (dark) TimecureDarkColors.copy(
        primary = Color(0xFFCBBFFF), onPrimary = Color(0xFF330099),
        primaryContainer = Color(0xFF4B27B2), onPrimaryContainer = Color(0xFFE7DEFF),
        secondary = Color(0xFFC9C3DC), onSecondary = Color(0xFF312E41),
        secondaryContainer = Color(0xFF484459), onSecondaryContainer = Color(0xFFE5DFF9),
        tertiary = Color(0xFF7FD5D8), onTertiary = Color(0xFF003739),
        tertiaryContainer = Color(0xFF004F53), onTertiaryContainer = Color(0xFF9CF1F4),
        background = Color(0xFF13121A), surface = Color(0xFF13121A),
        surfaceContainerLow = Color(0xFF1B1A23), surfaceContainer = Color(0xFF1F1E27),
        surfaceContainerHigh = Color(0xFF2A2932), surfaceContainerHighest = Color(0xFF35333D)
    ) else TimecureLightColors.copy(
        primary = Color(0xFF6342C9), onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFFE7DEFF), onPrimaryContainer = Color(0xFF1E0060),
        secondary = Color(0xFF5F5B71), onSecondary = Color(0xFFFFFFFF),
        secondaryContainer = Color(0xFFE5DFF9), onSecondaryContainer = Color(0xFF1C192B),
        tertiary = Color(0xFF00696E), onTertiary = Color(0xFFFFFFFF),
        tertiaryContainer = Color(0xFF9CF1F4), onTertiaryContainer = Color(0xFF002022),
        background = Color(0xFFFDF8FF), surface = Color(0xFFFDF8FF),
        surfaceContainerLow = Color(0xFFF6F2FA), surfaceContainer = Color(0xFFF0ECF4),
        surfaceContainerHigh = Color(0xFFEBE6EF), surfaceContainerHighest = Color(0xFFE5E1E9)
    )

    AppThemePalette.MIDNIGHT_SLATE -> if (dark) TimecureDarkColors.copy(
        primary = Color(0xFFACC7E3), onPrimary = Color(0xFF14314B),
        primaryContainer = Color(0xFF2B4863), onPrimaryContainer = Color(0xFFD5E7FA),
        secondary = Color(0xFFBCC7D2), onSecondary = Color(0xFF26323B),
        secondaryContainer = Color(0xFF3C4852), onSecondaryContainer = Color(0xFFD8E3EE),
        tertiary = Color(0xFF8CCEFF), onTertiary = Color(0xFF00344E),
        tertiaryContainer = Color(0xFF004B6F), onTertiaryContainer = Color(0xFFCAE6FF),
        background = Color(0xFF0E1216), surface = Color(0xFF0E1216),
        surfaceContainerLow = Color(0xFF161B20), surfaceContainer = Color(0xFF1A2026),
        surfaceContainerHigh = Color(0xFF242B32), surfaceContainerHighest = Color(0xFF2F363E)
    ) else TimecureLightColors.copy(
        primary = Color(0xFF334E68), onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFFD5E7FA), onPrimaryContainer = Color(0xFF0A2033),
        secondary = Color(0xFF52606D), onSecondary = Color(0xFFFFFFFF),
        secondaryContainer = Color(0xFFD8E3EE), onSecondaryContainer = Color(0xFF131D27),
        tertiary = Color(0xFF00639A), onTertiary = Color(0xFFFFFFFF),
        tertiaryContainer = Color(0xFFCAE6FF), onTertiaryContainer = Color(0xFF001E31),
        background = Color(0xFFF7F9FB), surface = Color(0xFFF7F9FB),
        surfaceContainerLow = Color(0xFFF1F4F7), surfaceContainer = Color(0xFFEBEFF3),
        surfaceContainerHigh = Color(0xFFE5E9EE), surfaceContainerHighest = Color(0xFFDFE4E9)
    )

    AppThemePalette.CRIMSON_PULSE -> if (dark) TimecureDarkColors.copy(
        primary = Color(0xFFFFB2BC), onPrimary = Color(0xFF660021),
        primaryContainer = Color(0xFF891032), onPrimaryContainer = Color(0xFFFFD9DE),
        secondary = Color(0xFFE5BDC2), onSecondary = Color(0xFF43292D),
        secondaryContainer = Color(0xFF5C3F43), onSecondaryContainer = Color(0xFFFFD9DE),
        tertiary = Color(0xFFF7BA64), onTertiary = Color(0xFF472A00),
        tertiaryContainer = Color(0xFF663E00), onTertiaryContainer = Color(0xFFFFDDB3),
        background = Color(0xFF191113), surface = Color(0xFF191113),
        surfaceContainerLow = Color(0xFF22191B), surfaceContainer = Color(0xFF261D1F),
        surfaceContainerHigh = Color(0xFF312729), surfaceContainerHighest = Color(0xFF3C3234)
    ) else TimecureLightColors.copy(
        primary = Color(0xFFA6243F), onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFFFFD9DE), onPrimaryContainer = Color(0xFF3F0012),
        secondary = Color(0xFF75565B), onSecondary = Color(0xFFFFFFFF),
        secondaryContainer = Color(0xFFFFD9DE), onSecondaryContainer = Color(0xFF2B1519),
        tertiary = Color(0xFF875200), onTertiary = Color(0xFFFFFFFF),
        tertiaryContainer = Color(0xFFFFDDB3), onTertiaryContainer = Color(0xFF2B1700),
        background = Color(0xFFFFF8F8), surface = Color(0xFFFFF8F8),
        surfaceContainerLow = Color(0xFFFFF0F1), surfaceContainer = Color(0xFFFAEAEB),
        surfaceContainerHigh = Color(0xFFF4E4E5), surfaceContainerHighest = Color(0xFFEEE0E1)
    )
}

@Composable
fun TimecureTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    palette: AppThemePalette = AppThemePalette.OCEAN_DAWN,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colors = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        else -> paletteScheme(palette, darkTheme)
    }
    MaterialTheme(colorScheme = colors, typography = TimecureTypography, content = content)
}
