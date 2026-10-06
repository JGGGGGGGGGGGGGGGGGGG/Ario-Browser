package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme =
  darkColorScheme(
    primary = NovaPrimaryDark,
    onPrimary = NovaOnPrimaryDark,
    primaryContainer = NovaPrimaryContainerDark,
    onPrimaryContainer = NovaOnPrimaryContainerDark,
    secondary = NovaSecondaryDark,
    onSecondary = NovaOnSecondaryDark,
    secondaryContainer = NovaSecondaryContainerDark,
    onSecondaryContainer = NovaOnSecondaryContainerDark,
    background = NovaBackgroundDark,
    onBackground = NovaOnBackgroundDark,
    surface = NovaSurfaceDark,
    onSurface = NovaOnSurfaceDark,
    surfaceVariant = NovaSurfaceVariantDark,
    onSurfaceVariant = NovaOnSurfaceVariantDark,
    outline = NovaOutlineDark,
  )

private val LightColorScheme =
  lightColorScheme(
    primary = NovaPrimaryLight,
    onPrimary = NovaOnPrimaryLight,
    primaryContainer = NovaPrimaryContainerLight,
    onPrimaryContainer = NovaOnPrimaryContainerLight,
    secondary = NovaSecondaryLight,
    onSecondary = NovaOnSecondaryLight,
    secondaryContainer = NovaSecondaryContainerLight,
    onSecondaryContainer = NovaOnSecondaryContainerLight,
    background = NovaBackgroundLight,
    onBackground = NovaOnBackgroundLight,
    surface = NovaSurfaceLight,
    onSurface = NovaOnSurfaceLight,
    surfaceVariant = NovaSurfaceVariantLight,
    onSurfaceVariant = NovaOnSurfaceVariantLight,
    outline = NovaOutlineLight,
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  content: @Composable () -> Unit,
) {
  val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}

