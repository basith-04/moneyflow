@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.moneyflow.android.ui

import android.app.UiModeManager
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.moneyflow.android.data.Appearance
import com.moneyflow.android.data.Settings

private val Light = lightColorScheme(
    primary = Color(0xFF285347), onPrimary = Color.White,
    primaryContainer = Color(0xFFCEEADF), onPrimaryContainer = Color(0xFF102E25),
    secondary = Color(0xFF4E6359), onSecondary = Color.White,
    secondaryContainer = Color(0xFFD8E9DE), onSecondaryContainer = Color(0xFF1E342A),
    tertiary = Color(0xFF765834), onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFDE3BE), onTertiaryContainer = Color(0xFF38280F),
    background = Color(0xFFF6FBF6), onBackground = Color(0xFF171D19),
    surface = Color(0xFFF6FBF6), onSurface = Color(0xFF171D19),
    surfaceVariant = Color(0xFFE0E8E1), onSurfaceVariant = Color(0xFF414A43),
    surfaceContainerLow = Color(0xFFEFF5EF), surfaceContainer = Color(0xFFE9EFE9),
    surfaceContainerHigh = Color(0xFFE3EAE4), surfaceContainerHighest = Color(0xFFDDE5DE),
    outline = Color(0xFF6E7970), outlineVariant = Color(0xFFC0CAC1),
)

private val Dark = darkColorScheme(
    primary = Color(0xFFA9D3C1), onPrimary = Color(0xFF15362C),
    primaryContainer = Color(0xFF2E4F43), onPrimaryContainer = Color(0xFFD3EFE0),
    secondary = Color(0xFFB5CBBE), onSecondary = Color(0xFF23382D),
    secondaryContainer = Color(0xFF354B40), onSecondaryContainer = Color(0xFFD8E9DE),
    tertiary = Color(0xFFE8C397), onTertiary = Color(0xFF432D10),
    tertiaryContainer = Color(0xFF5B4326), onTertiaryContainer = Color(0xFFFDE3BE),
    background = Color(0xFF111713), onBackground = Color(0xFFE0E8E0),
    surface = Color(0xFF111713), onSurface = Color(0xFFE0E8E0),
    surfaceVariant = Color(0xFF414A43), onSurfaceVariant = Color(0xFFC0CAC1),
    surfaceContainerLow = Color(0xFF191F1A), surfaceContainer = Color(0xFF1D241F),
    surfaceContainerHigh = Color(0xFF272E28), surfaceContainerHighest = Color(0xFF323A33),
    outline = Color(0xFF899489), outlineVariant = Color(0xFF414A43),
)

private val MoneyShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

@Composable
internal fun MoneyTheme(settings: Settings = Settings(), content: @Composable (dark: Boolean) -> Unit) {
    val context = LocalContext.current
    val dark = when (settings.appearance) {
        Appearance.System -> isSystemInDarkTheme()
        Appearance.Light -> false
        Appearance.Dark -> true
    }
    var contrast by remember { mutableFloatStateOf(0f) }
    if (Build.VERSION.SDK_INT >= 34) {
        DisposableEffect(context) {
            val manager = context.getSystemService(UiModeManager::class.java)
                ?: return@DisposableEffect onDispose {}
            val listener = UiModeManager.ContrastChangeListener { contrast = it }
            contrast = manager.contrast
            manager.addContrastChangeListener(context.mainExecutor, listener)
            onDispose { manager.removeContrastChangeListener(listener) }
        }
    }
    val baseline = when {
        settings.dynamicColor && dark -> dynamicDarkColorScheme(context)
        settings.dynamicColor -> dynamicLightColorScheme(context)
        dark -> Dark
        else -> Light
    }
    val palette = if (contrast > 0f) baseline.copy(
        onSurfaceVariant = baseline.onSurface,
        outlineVariant = baseline.outline,
    ) else baseline

    MaterialExpressiveTheme(
        colorScheme = palette,
        motionScheme = MotionScheme.expressive(),
        shapes = MoneyShapes,
    ) { content(dark) }
}
