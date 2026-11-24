package com.example.ui.theme

import android.app.Activity
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.example.data.model.AccentColorPreset
import com.example.data.model.FontPreset
import com.example.data.model.ThemePreset

private val InstagramLightColors = lightColorScheme(
    primary = InstagramBlue,
    onPrimary = Color.White,
    secondary = InstagramMagenta,
    onSecondary = Color.White,
    tertiary = GhostPurple,
    onTertiary = Color.White,
    background = IgLightBg,
    onBackground = IgLightText,
    surface = IgLightSurface,
    onSurface = IgLightText,
    surfaceVariant = IgLightSurfaceVariant,
    onSurfaceVariant = IgLightTextMuted,
    outline = IgLightOutline,
    error = InstagramRed,
    onError = Color.White
)

private val AmoledDarkColors = darkColorScheme(
    primary = InstagramBlue,
    onPrimary = Color.White,
    secondary = InstagramMagenta,
    onSecondary = Color.White,
    tertiary = GhostPurple,
    onTertiary = Color.White,
    background = AmoledBg,
    onBackground = AmoledText,
    surface = AmoledSurface,
    onSurface = AmoledText,
    surfaceVariant = AmoledSurfaceVariant,
    onSurfaceVariant = AmoledTextMuted,
    outline = AmoledOutline,
    error = InstagramRed,
    onError = Color.White
)

private val SunsetRoseColors = lightColorScheme(
    primary = InstagramMagenta,
    onPrimary = Color.White,
    secondary = InstagramAmber,
    onSecondary = Color.White,
    tertiary = InstagramViolet,
    onTertiary = Color.White,
    background = RoseBg,
    onBackground = RoseText,
    surface = RoseSurface,
    onSurface = RoseText,
    surfaceVariant = RoseSurfaceVariant,
    onSurfaceVariant = RoseTextMuted,
    outline = RoseOutline,
    error = InstagramRed,
    onError = Color.White
)

private val MidnightVioletColors = darkColorScheme(
    primary = VioletPrimary,
    onPrimary = Color.White,
    secondary = InstagramMagenta,
    onSecondary = Color.White,
    tertiary = InstagramBlue,
    onTertiary = Color.White,
    background = VioletBg,
    onBackground = VioletText,
    surface = VioletSurface,
    onSurface = VioletText,
    surfaceVariant = VioletSurfaceVariant,
    onSurfaceVariant = VioletTextMuted,
    outline = VioletOutline,
    error = InstagramRed,
    onError = Color.White
)

private val EditorialSlateColors = lightColorScheme(
    primary = SlatePrimary,
    onPrimary = Color.White,
    secondary = InstagramBlue,
    onSecondary = Color.White,
    tertiary = GhostPurple,
    onTertiary = Color.White,
    background = SlateBg,
    onBackground = SlateText,
    surface = SlateSurface,
    onSurface = SlateText,
    surfaceVariant = SlateSurfaceVariant,
    onSurfaceVariant = SlateTextMuted,
    outline = SlateOutline,
    error = InstagramRed,
    onError = Color.White
)

fun colorSchemeForPreset(
    preset: ThemePreset,
    isDarkMode: Boolean = preset.isDark,
    accentPreset: AccentColorPreset = AccentColorPreset.INSTAGRAM_BLUE
): ColorScheme {
    val base = when {
        isDarkMode && preset == ThemePreset.MIDNIGHT_VIOLET -> MidnightVioletColors
        isDarkMode -> AmoledDarkColors
        preset == ThemePreset.SUNSET_ROSE -> SunsetRoseColors
        preset == ThemePreset.EDITORIAL_SLATE -> EditorialSlateColors
        else -> InstagramLightColors
    }

    val accent = accentPreset.primaryColor
    val onAccent = if (accent.luminance() > 0.55f) Color(0xFF111111) else Color.White
    val container = accent.copy(alpha = if (isDarkMode) 0.24f else 0.14f)
    val onContainer = if (isDarkMode) Color.White else accent

    return base.copy(
        primary = accent,
        onPrimary = onAccent,
        primaryContainer = container,
        onPrimaryContainer = onContainer,
        secondary = accentPreset.secondaryColor,
        onSecondary = Color.White
    )
}

@Composable
fun InstaPlusTheme(
    isDarkMode: Boolean = false,
    themePreset: ThemePreset = ThemePreset.INSTAGRAM_LIGHT,
    accentPreset: AccentColorPreset = AccentColorPreset.INSTAGRAM_BLUE,
    fontPreset: FontPreset = FontPreset.INTER,
    content: @Composable () -> Unit
) {
    val colorScheme = colorSchemeForPreset(
        preset = themePreset,
        isDarkMode = isDarkMode,
        accentPreset = accentPreset
    )
    val typography = buildInstaPlusTypography(fontPreset)
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars =
                    !isDarkMode
                WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars =
                    !isDarkMode
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = typography,
        content = content
    )
}
