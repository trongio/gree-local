package ge.hackerman.gree.ui

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

private val Teal = Color(0xFF00696E)
private val TealLight = Color(0xFF4FD1C5)
private val Deep = Color(0xFF0B3D4F)

private val LightScheme = lightColorScheme(
    primary = Teal,
    secondary = Deep,
    tertiary = Color(0xFF4A6267),
)

private val DarkScheme = darkColorScheme(
    primary = TealLight,
    secondary = Color(0xFFB1CBD0),
    tertiary = Color(0xFFA8CED3),
)

@Composable
fun GreeTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val context = LocalContext.current

    // Material You where the platform offers it, our own palette everywhere else.
    val colors = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)

        dark -> DarkScheme
        else -> LightScheme
    }

    MaterialTheme(colorScheme = colors, content = content)
}
