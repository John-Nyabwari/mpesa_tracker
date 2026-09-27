package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColorScheme = lightColorScheme(
    primary = DarkGreen,
    onPrimary = SurfaceWhite,
    primaryContainer = BrandLime,
    onPrimaryContainer = DarkGreen,
    secondary = BrandGreen,
    onSecondary = SurfaceWhite,
    secondaryContainer = IncomeGreenBg,
    onSecondaryContainer = DarkGreen,
    background = SurfaceBg,
    surface = SurfaceWhite,
    surfaceVariant = SurfaceBg,
    onBackground = DarkGreen,
    onSurface = DarkGreen,
    onSurfaceVariant = TextMuted,
    outline = BorderSubtle,
    error = ExpenseRed,
    onError = SurfaceWhite
)

@Composable
fun MpesaTrackerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = Typography,
        content = content
    )
}

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = false,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    MpesaTrackerTheme(content = content)
}
