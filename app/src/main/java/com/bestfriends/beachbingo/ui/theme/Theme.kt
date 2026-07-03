package com.bestfriends.beachbingo.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val BeachColorScheme = darkColorScheme(
    primary            = OceanBlue,
    onPrimary          = TextPrimary,
    primaryContainer   = OceanBlueDark,
    onPrimaryContainer = TextPrimary,
    secondary          = SandGold,
    onSecondary        = BgDark,
    secondaryContainer = SandGoldDark,
    onSecondaryContainer = TextPrimary,
    tertiary           = Coral,
    onTertiary         = TextPrimary,
    background         = BgDark,
    onBackground       = TextPrimary,
    surface            = SurfaceDark,
    onSurface          = TextPrimary,
    surfaceVariant     = Surface2Dark,
    onSurfaceVariant   = TextSub,
    outline            = BorderColor,
    error              = Danger,
    onError            = TextPrimary,
)

@Composable
fun BeachbingoTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = BeachColorScheme,
        typography = Typography,
        content = content
    )
}
