package com.ml.EdgeMind.docqa.ui.theme

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

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryBlue,
    onPrimary = Color.White,
    primaryContainer = PrimaryBlueDark,
    onPrimaryContainer = Color.White,
    
    secondary = SecondarySlate,
    onSecondary = Color.White,
    secondaryContainer = SecondarySlateDark,
    onSecondaryContainer = Color.White,
    
    tertiary = AccentPurple,
    onTertiary = Color.White,
    tertiaryContainer = AccentPurple.copy(alpha = 0.2f),
    onTertiaryContainer = AccentPurple,
    
    background = BackgroundDark,
    onBackground = TextPrimaryDark,
    surface = SurfaceDark,
    onSurface = TextPrimaryDark,
    surfaceVariant = AssistantMessageDark,
    onSurfaceVariant = TextSecondaryDark,
    
    error = ErrorRed,
    onError = Color.White,
    errorContainer = ErrorRed.copy(alpha = 0.2f),
    onErrorContainer = ErrorRed,
    
    outline = BorderDark,
    outlineVariant = BorderDark.copy(alpha = 0.5f),
    
    scrim = Color.Black.copy(alpha = 0.32f),
    surfaceTint = PrimaryBlue,
    inverseSurface = Color.White,
    inverseOnSurface = Color.Black,
    inversePrimary = PrimaryBlueLight
)

private val LightColorScheme = lightColorScheme(
    primary = PrimaryBlue,
    onPrimary = Color.White,
    primaryContainer = PrimaryBlueLight,
    onPrimaryContainer = Color.White,
    
    secondary = SecondarySlate,
    onSecondary = Color.White,
    secondaryContainer = SecondarySlateLight,
    onSecondaryContainer = Color.White,
    
    tertiary = AccentPurple,
    onTertiary = Color.White,
    tertiaryContainer = AccentPurple.copy(alpha = 0.1f),
    onTertiaryContainer = AccentPurple,
    
    background = BackgroundLight,
    onBackground = TextPrimaryLight,
    surface = SurfaceLight,
    onSurface = TextPrimaryLight,
    surfaceVariant = AssistantMessageLight,
    onSurfaceVariant = TextSecondaryLight,
    
    error = ErrorRed,
    onError = Color.White,
    errorContainer = ErrorRed.copy(alpha = 0.1f),
    onErrorContainer = ErrorRed,
    
    outline = BorderLight,
    outlineVariant = BorderLight.copy(alpha = 0.5f),
    
    scrim = Color.Black.copy(alpha = 0.32f),
    surfaceTint = PrimaryBlue,
    inverseSurface = Color.Black,
    inverseOnSurface = Color.White,
    inversePrimary = PrimaryBlueLight
)

@Composable
fun DocQATheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = false, // Disabled for consistent branding
    content: @Composable () -> Unit
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
