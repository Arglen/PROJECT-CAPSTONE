package com.example.system_mini_capstone.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = PrimaryBlue,
    onPrimary = OnPrimaryWhite,
    background = BackgroundLight,
    surface = SurfaceLight,
    surfaceVariant = BackgroundLight,
    onBackground = TitleDark,
    onSurface = TitleDark,
    outline = OutlineBorder,
    error = ErrorRed,
    onError = OnPrimaryWhite,
    errorContainer = ErrorTint,
    onErrorContainer = ErrorRed,
    secondary = SecondaryGray,
    onSecondary = OnPrimaryWhite
)

@Composable
fun System_mini_capstoneTheme(
    content: @Composable () -> Unit
) {
    val colorScheme = LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).let { controller ->
                controller.isAppearanceLightStatusBars = true
                controller.isAppearanceLightNavigationBars = true
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}
