package ge.hackerman.gree.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text
import ge.hackerman.gree.ui.theme.symbolsFamily

/** Codepoints from the subset of Material Symbols Rounded bundled in res/font. */
object Sym {
    const val ADD = "\uE145"
    const val REMOVE = "\uE15B"
    const val DELETE = "\uE92E"
    const val POWER = "\uF8C7"
    const val CHEVRON_LEFT = "\uE5CB"
    const val RADAR = "\uF04E"
    const val WIFI_FIND = "\uEB31"
    const val BLOCK = "\uF08C"
    const val SWAP_VERT = "\uE8D5"
    const val SWAP_HORIZ = "\uE8D4"
    const val HDR_AUTO = "\uF01A"
    const val AC_UNIT = "\uEB3B"
    const val WATER_DROP = "\uE798"
    const val AIR = "\uEFD8"
    const val SUNNY = "\uE81A"
    const val BOLT = "\uEA0B"
    const val VOLUME_OFF = "\uE04F"
    const val BEDTIME = "\uF159"
    const val LIGHTBULB = "\uE90F"
    const val DRY = "\uF1B3"
    const val ECO = "\uEA35"
}

@Composable
fun Symbol(
    glyph: String,
    size: TextUnit = 22.sp,
    color: Color = Color.Unspecified,
    filled: Boolean = false,
    contentDescription: String? = null,
    modifier: Modifier = Modifier,
) {
    Text(
        text = glyph,
        color = color,
        style = TextStyle(
            fontFamily = symbolsFamily(filled),
            fontSize = size,
            // Symbols are drawn on their own baseline; matching line height keeps them centred.
            lineHeight = size,
        ),
        modifier = if (contentDescription != null) {
            modifier.semantics { this.contentDescription = contentDescription }
        } else {
            modifier
        },
    )
}

/** Mode and option glyphs live here so the protocol layer stays free of UI concerns. */
val ge.hackerman.gree.protocol.GreeMode.symbol: String
    get() = when (this) {
        ge.hackerman.gree.protocol.GreeMode.AUTO -> Sym.HDR_AUTO
        ge.hackerman.gree.protocol.GreeMode.COOL -> Sym.AC_UNIT
        ge.hackerman.gree.protocol.GreeMode.DRY -> Sym.WATER_DROP
        ge.hackerman.gree.protocol.GreeMode.FAN -> Sym.AIR
        ge.hackerman.gree.protocol.GreeMode.HEAT -> Sym.SUNNY
    }
