package ge.hackerman.gree.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * The palette is fixed rather than derived from Material You: the design leans on a
 * specific teal/mint pairing that dynamic colour would wash out.
 */
@Immutable
data class GreeColors(
    val bg: Color,
    val card: Color,
    val ink: Color,
    val ink2: Color,
    val line: Color,
    val accent: Color,
) {
    /** Heat mode and offline markers, the one hue outside the teal family. */
    val warm: Color get() = Color(0xFFF0A58E)

    /** Text drawn on top of [accent] or [warm] fills. */
    val onAccent: Color get() = Color(0xFF0B3D4F)
}

private val LightColors = GreeColors(
    bg = Color(0xFFEEF4F5),
    card = Color(0xFFFFFFFF),
    ink = Color(0xFF0B3D4F),
    ink2 = Color(0xFF0B3D4F).copy(alpha = 0.58f),
    line = Color(0xFF0B3D4F).copy(alpha = 0.10f),
    accent = Color(0xFF4FD1C5),
)

private val DarkColors = GreeColors(
    bg = Color(0xFF071E27),
    card = Color(0xFF0E3341),
    ink = Color(0xFFEEF6F7),
    ink2 = Color(0xFFEEF6F7).copy(alpha = 0.60f),
    line = Color(0xFFEEF6F7).copy(alpha = 0.10f),
    accent = Color(0xFF4FD1C5),
)

val LocalGreeColors = staticCompositionLocalOf { LightColors }

/** Shorthand so screens can read `Gree.ink` instead of threading colours through. */
object Gree {
    val colors: GreeColors
        @Composable get() = LocalGreeColors.current
}

@Composable
fun GreeTheme(dark: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val colors = if (dark) DarkColors else LightColors

    CompositionLocalProvider(LocalGreeColors provides colors) {
        MaterialTheme(
            // Material components we still use (ripples, text selection) read from here.
            colorScheme = if (dark) {
                darkColorScheme(
                    primary = colors.accent,
                    background = colors.bg,
                    surface = colors.card,
                    onBackground = colors.ink,
                    onSurface = colors.ink,
                )
            } else {
                lightColorScheme(
                    primary = colors.accent,
                    background = colors.bg,
                    surface = colors.card,
                    onBackground = colors.ink,
                    onSurface = colors.ink,
                )
            },
            typography = greeTypography(),
            content = content,
        )
    }
}
