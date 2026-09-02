package ge.hackerman.gree.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import ge.hackerman.gree.R

@OptIn(ExperimentalTextApi::class)
/** Albert Sans ships as a single variable file, so every weight is one axis setting. */
private fun albert(weight: FontWeight) = Font(
    resId = R.font.albert_sans,
    weight = weight,
    variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight)),
)

val AlbertSans = FontFamily(
    albert(FontWeight.W200),
    albert(FontWeight.W300),
    albert(FontWeight.W400),
    albert(FontWeight.W500),
    albert(FontWeight.W600),
    albert(FontWeight.W700),
)

val PlexMono = FontFamily(
    Font(R.font.ibm_plex_mono_regular, FontWeight.W400),
    Font(R.font.ibm_plex_mono_medium, FontWeight.W500),
)

/**
 * Material Symbols Rounded, subset to the glyphs this app draws. FILL is a variable
 * axis, so a filled and an outlined icon are the same glyph at different settings.
 */
@OptIn(ExperimentalTextApi::class)
fun symbolsFamily(filled: Boolean) = FontFamily(
    Font(
        resId = R.font.material_symbols_rounded,
        variationSettings = FontVariation.Settings(
            FontVariation.Setting("FILL", if (filled) 1f else 0f),
            FontVariation.weight(400),
        ),
    ),
)

/** Mono captions carry the "this is a network tool" tone; they are used verbatim a lot. */
val MonoCaption = TextStyle(
    fontFamily = PlexMono,
    fontWeight = FontWeight.W400,
    fontSize = 11.sp,
    letterSpacing = 0.06.em,
)

@Composable
fun greeTypography(): Typography {
    val base = Typography()
    return base.copy(
        displayLarge = base.displayLarge.copy(fontFamily = AlbertSans),
        displayMedium = base.displayMedium.copy(fontFamily = AlbertSans),
        displaySmall = base.displaySmall.copy(fontFamily = AlbertSans),
        headlineLarge = base.headlineLarge.copy(fontFamily = AlbertSans),
        headlineMedium = base.headlineMedium.copy(fontFamily = AlbertSans),
        headlineSmall = base.headlineSmall.copy(fontFamily = AlbertSans),
        titleLarge = base.titleLarge.copy(fontFamily = AlbertSans),
        titleMedium = base.titleMedium.copy(fontFamily = AlbertSans),
        titleSmall = base.titleSmall.copy(fontFamily = AlbertSans),
        bodyLarge = base.bodyLarge.copy(fontFamily = AlbertSans),
        bodyMedium = base.bodyMedium.copy(fontFamily = AlbertSans),
        bodySmall = base.bodySmall.copy(fontFamily = AlbertSans),
        labelLarge = base.labelLarge.copy(fontFamily = AlbertSans),
        labelMedium = base.labelMedium.copy(fontFamily = AlbertSans),
        labelSmall = base.labelSmall.copy(fontFamily = AlbertSans),
    )
}
