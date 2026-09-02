package ge.hackerman.gree.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import ge.hackerman.gree.protocol.Gree
import ge.hackerman.gree.protocol.GreeMode
import ge.hackerman.gree.protocol.GreeState
import ge.hackerman.gree.ui.theme.AlbertSans
import ge.hackerman.gree.ui.theme.PlexMono
import ge.hackerman.gree.ui.theme.Gree as GreeTheme
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.tan

private val BAR_HEIGHT = 104.dp
private val BAR_RADIUS = 32.dp

/**
 * Target temperature as one fat pill: the fill is the setpoint, the coral tick is the
 * room, and the hatched band between them is the work the unit still has to do.
 * Drag anywhere on the bar, or tap a position.
 */
@Composable
fun TemperatureBar(
    state: GreeState,
    caption: String,
    onSetTemp: (Int) -> Unit,
    onGlowAnchor: (Offset) -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = GreeTheme.colors
    val span = (Gree.MAX_TEMP - Gree.MIN_TEMP).toFloat()

    // The value is tracked locally through the gesture and committed on release: the
    // prototype sends on every change, but each one here is a datagram to the unit, and
    // a single drag would fire a dozen of them.
    var dragging by remember { mutableStateOf<Int?>(null) }
    val target = dragging ?: state.targetTemp
    val room = state.roomTemp

    val targetFrac by animateFloatAsState(
        targetValue = (target - Gree.MIN_TEMP) / span,
        animationSpec = tween(if (dragging != null) 0 else 120),
        label = "targetFrac",
    )
    // The prototype dims to .3, which works against its light card; on the dark card the
    // bar all but disappears, so idle is carried by a flatter fill rather than by fading.
    val barAlpha by animateFloatAsState(if (state.power) 1f else 0.55f, tween(300), label = "barAlpha")

    val warmFill = state.power && state.mode == GreeMode.HEAT
    val fill by animateColorAsState(
        targetValue = when {
            // c.line is a hairline colour: as a fill it leaves nothing to see, and the
            // thumb ends up floating with no edge to sit against.
            !state.power -> c.ink.copy(alpha = 0.22f)
            state.mode == GreeMode.HEAT -> c.warm
            state.mode == GreeMode.COOL -> c.accent
            else -> c.ink2
        },
        animationSpec = tween(300),
        label = "fill",
    )

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                "TARGET",
                fontFamily = PlexMono,
                fontWeight = FontWeight.W500,
                fontSize = 11.sp,
                lineHeight = 11.sp,
                letterSpacing = 0.12.em,
                color = c.ink2,
            )
            Text(
                caption,
                fontFamily = PlexMono,
                fontSize = 12.sp,
                lineHeight = 12.sp,
                letterSpacing = 0.06.em,
                color = c.ink2,
            )
        }

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(BAR_HEIGHT)
                .alpha(barAlpha)
                .clip(RoundedCornerShape(BAR_RADIUS))
                .background(c.card)
                .border(1.dp, c.line, RoundedCornerShape(BAR_RADIUS))
                .onGloballyPositioned {
                    val p = it.positionInRoot()
                    onGlowAnchor(Offset(p.x + it.size.width / 2f, p.y + it.size.height / 2f))
                },
        ) {
            val barWidth = maxWidth
            val widthPx = with(LocalDensity.current) { barWidth.toPx() }
            fun tempAt(x: Float) =
                (Gree.MIN_TEMP + (x / widthPx).coerceIn(0f, 1f) * span).roundToInt()
                    .coerceIn(Gree.MIN_TEMP, Gree.MAX_TEMP)

            // A room reading outside the settable range still has to land on the bar.
            val roomFrac = room?.let {
                ((it.coerceIn(Gree.MIN_TEMP, Gree.MAX_TEMP) - Gree.MIN_TEMP) / span)
            }

            Box(
                Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .pointerInput(widthPx) { detectTapGestures { onSetTemp(tempAt(it.x)) } }
                    .pointerInput(widthPx) {
                        detectHorizontalDragGestures(
                            onDragStart = { dragging = tempAt(it.x) },
                            onDragEnd = { dragging?.let(onSetTemp); dragging = null },
                            onDragCancel = { dragging = null },
                        ) { change, _ -> dragging = tempAt(change.position.x) }
                    },
            ) {
                // Which side of the fill the gap falls on decides whether it paints
                // under or over: cooling, the band sits beyond the fill and needs to show
                // through its rounded corners; heating, the band lies inside the fill and
                // would be buried unless it goes on top.
                val heating = roomFrac != null && roomFrac < targetFrac
                val lo = roomFrac?.let { minOf(targetFrac, it) } ?: 0f
                val hi = roomFrac?.let { maxOf(targetFrac, it) } ?: 0f
                // The band means "what the unit still has to close". A unit that is off
                // is closing nothing, and the caption already reads STANDBY.
                val hasGap = roomFrac != null && state.power && hi - lo > 0.001f

                if (hasGap && !heating) {
                    val left = (barWidth * lo - BAR_RADIUS).coerceAtLeast(0.dp)
                    Hatch(
                        Modifier
                            .offset(x = left)
                            .width(barWidth * hi - left)
                            .fillMaxHeight(),
                    )
                }

                Box(
                    Modifier
                        .width(barWidth * targetFrac)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(topEnd = BAR_RADIUS, bottomEnd = BAR_RADIUS))
                        .background(fill),
                ) {
                    // Nested inside the fill rather than laid over it, so the fill's own
                    // rounded clip trims the band instead of it bleeding into the corners.
                    if (hasGap && heating) {
                        Hatch(
                            Modifier
                                .offset(x = barWidth * lo)
                                .width(barWidth * (hi - lo))
                                .fillMaxHeight(),
                            // Coral stripes vanish on a coral fill, which is exactly what
                            // heat mode paints, so shade with ink there instead.
                            stripe = if (warmFill) {
                                c.ink.copy(alpha = 0.30f)
                            } else {
                                c.warm.copy(alpha = 0.8f)
                            },
                        )
                    }
                }

                Box(
                    Modifier
                        .offset(x = barWidth * targetFrac - 3.dp)
                        .padding(vertical = 26.dp)
                        .width(6.dp)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(3.dp))
                        .background(c.ink),
                )

                if (roomFrac != null) {
                    Box(
                        Modifier
                            .offset(x = barWidth * roomFrac - 1.5.dp)
                            .padding(vertical = 30.dp)
                            .width(3.dp)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(2.dp))
                            .background(c.warm),
                    )
                    Column(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(end = 22.dp),
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            "ROOM",
                            fontFamily = PlexMono,
                            fontSize = 10.sp,
                            lineHeight = 10.sp,
                            letterSpacing = 0.1.em,
                            color = c.ink2,
                        )
                        Text(
                            "$room°",
                            fontFamily = AlbertSans,
                            fontSize = 26.sp,
                            lineHeight = 26.sp,
                            color = c.ink,
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = 24.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "$target",
                        fontFamily = AlbertSans,
                        fontWeight = FontWeight.W300,
                        fontSize = 64.sp,
                        lineHeight = 64.sp,
                        letterSpacing = (-0.04).em,
                        // Tabular figures stop the number jittering as it changes width.
                        style = LocalTextStyle.current.copy(fontFeatureSettings = "tnum"),
                        color = c.ink,
                    )
                    Text(
                        "°",
                        fontFamily = AlbertSans,
                        fontWeight = FontWeight.W300,
                        fontSize = 24.sp,
                        lineHeight = 24.sp,
                        color = c.ink,
                        // Rides the cap height of the digits rather than their centre.
                        modifier = Modifier.align(Alignment.Top).padding(top = 8.dp),
                    )
                }
            }
        }

        Row(
            Modifier.fillMaxWidth().padding(horizontal = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Scale("${Gree.MIN_TEMP}°")
            GapLabel(state = state, target = target, room = room)
            Scale("${Gree.MAX_TEMP}°")
        }
    }
}

@Composable
private fun Scale(text: String) {
    Text(
        text,
        fontFamily = PlexMono,
        fontSize = 11.sp,
        lineHeight = 11.sp,
        color = GreeTheme.colors.ink2,
    )
}

/** Names what the hatched band means, or why there is not one. */
@Composable
private fun GapLabel(state: GreeState, target: Int, room: Int?) {
    val c = GreeTheme.colors

    val plain = when {
        room == null -> "room sensor unavailable"
        !state.power -> "standby"
        room == target -> "at target"
        else -> null
    }
    if (plain != null) {
        Text(plain, fontFamily = PlexMono, fontSize = 11.sp, lineHeight = 11.sp, color = c.ink2)
        return
    }

    val delta = room!! - target
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Hatch(Modifier.size(11.dp).clip(RoundedCornerShape(2.dp)), opaque = true)
        Text(
            "${abs(delta)}° to ${if (delta > 0) "cool" else "heat"}",
            fontFamily = PlexMono,
            fontSize = 11.sp,
            lineHeight = 11.sp,
            color = c.warm,
        )
    }
}

/** Diagonal warm stripes, the same treatment on the bar and in the legend swatch. */
@Composable
private fun Hatch(
    modifier: Modifier = Modifier,
    opaque: Boolean = false,
    stripe: Color? = null,
) {
    val c = GreeTheme.colors
    val onDark = c.bg.luminance() < 0.5f
    val color = stripe ?: c.warm.copy(
        alpha = when {
            opaque -> 0.55f
            // The design's value was picked against white; over the dark card it muddies.
            onDark -> 0.55f
            else -> 0.35f
        },
    )
    // Diagonal lines necessarily start and end outside the band, and Canvas does not
    // clip on its own, so without this they paint straight across the rest of the bar.
    Canvas(modifier.clipToBounds()) {
        // The legend swatch is only 11dp across, so the bar's pitch gives it one stripe
        // and it reads as a slash rather than a hatch.
        val stroke = (if (opaque) 2.dp else 4.dp).toPx()
        val period = (if (opaque) 4.5.dp else 10.dp).toPx()
        val angle = Math.toRadians(55.0)
        val run = (size.height / tan(angle)).toFloat()
        val step = (period / sin(angle)).toFloat()

        var x = -run
        while (x < size.width + run) {
            drawLine(
                color = color,
                start = Offset(x, size.height),
                end = Offset(x + run, 0f),
                strokeWidth = stroke,
                cap = StrokeCap.Butt,
            )
            x += step
        }
    }
}
