package com.example.mybeat.ui.theme

import android.os.Build
import androidx.compose.material3.*
import androidx.compose.runtime.Composable

private val DarkColors = darkColorScheme(
    // Colori globali
    background   = GrayBg,
    onBackground = TextPrimary,
    surface      = GraySurface,
    onSurface    = TextPrimary,
    surfaceVariant = GraySurface2,
    outline      = OutlineDark,

    // Accent / tasti / slider
    primary      = PurpleAccent,
    onPrimary    = TextPrimary,
    secondary    = TextSecondary,
    onSecondary  = TextPrimary,

    error        = ErrorRed
)

@Composable
fun MyBeatTheme(
    useDarkTheme: Boolean = true,        // tienilo dark
    dynamicColor: Boolean = false,       // disattivato per non inquinare la palette custom
    content: @Composable () -> Unit
) {
    val cs =
        if (dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (useDarkTheme) dynamicDarkColorScheme(androidx.compose.ui.platform.LocalContext.current)
            else dynamicLightColorScheme(androidx.compose.ui.platform.LocalContext.current)
        } else DarkColors

    MaterialTheme(
        colorScheme = cs,
        typography = Typography,
        content = content
    )
}