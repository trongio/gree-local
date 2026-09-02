package ge.hackerman.gree.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.Canvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import ge.hackerman.gree.protocol.Gree
import ge.hackerman.gree.ui.theme.AlbertSans
import ge.hackerman.gree.ui.theme.PlexMono
import ge.hackerman.gree.ui.theme.Gree as GreeTheme

/** One louver ray: where it points, in the diagram's own 0..1 coordinate space. */
private data class Ray(val end: Offset)

/**
 * Both swing axes render the same way: a stylised unit with five selectable rays plus
 * Off and Full shortcuts. Only the geometry differs, so it is passed in.
 */
@Composable
fun SwingCard(
    title: String,
    valueLabel: String,
    raw: Int,
    horizontal: Boolean,
    onPick: (Int) -> Unit,
) {
    val c = GreeTheme.colors
    val full = raw == Gree.SWING_FULL

    // Full sweep pulses the whole fan of rays, matching the design's animation.
    val transition = rememberInfiniteTransition(label = "swing")
    val pulse by transition.animateFloat(
        initialValue = 1f,
        targetValue = 0.35f,
        animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse),
        label = "pulse",
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(c.card)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                title.uppercase(),
                fontFamily = PlexMono,
                fontWeight = FontWeight.W500,
                fontSize = 11.sp,
                lineHeight = 11.sp,
                letterSpacing = 0.12.em,
                color = c.ink2,
            )
            Text(
                valueLabel,
                fontFamily = AlbertSans,
                fontWeight = FontWeight.W500,
                fontSize = 13.sp,
                color = c.ink,
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            SwingDiagram(
                raw = raw,
                horizontal = horizontal,
                fullAlpha = if (full) pulse else 1f,
                onPick = onPick,
                modifier = Modifier.weight(1f),
            )
            Column(
                modifier = Modifier.width(100.dp).height(96.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                SwingChoice(
                    label = "Off",
                    glyph = Sym.BLOCK,
                    selected = raw == Gree.SWING_OFF,
                    onClick = { onPick(Gree.SWING_OFF) },
                    modifier = Modifier.weight(1f),
                )
                SwingChoice(
                    label = "Full",
                    glyph = if (horizontal) Sym.SWAP_HORIZ else Sym.SWAP_VERT,
                    selected = full,
                    onClick = { onPick(Gree.SWING_FULL) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun SwingChoice(
    label: String,
    glyph: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = GreeTheme.colors
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(if (selected) c.ink else c.bg)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Symbol(glyph, 18.sp, if (selected) c.bg else c.ink)
            Text(
                label,
                fontFamily = AlbertSans,
                fontWeight = FontWeight.W500,
                fontSize = 13.sp,
                color = if (selected) c.bg else c.ink,
            )
        }
    }
}

@Composable
private fun SwingDiagram(
    raw: Int,
    horizontal: Boolean,
    fullAlpha: Float,
    onPick: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = GreeTheme.colors
    val full = raw == Gree.SWING_FULL
    val selectedRay = raw - Gree.SWING_FIRST_FIXED

    // Normalised layout: the unit sits at the top (or top-left) and rays fan out from it.
    val origin = if (horizontal) Offset(0.5f, 0.23f) else Offset(0.38f, 0.42f)
    val rays = remember(horizontal) { rayGeometry(horizontal) }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(if (horizontal) 1.25f else 1.2f)
            .pointerInput(horizontal) {
                detectTapGestures { tap ->
                    val p = Offset(tap.x / size.width, tap.y / size.height)
                    val hit = rays.withIndex().minByOrNull { (_, ray) ->
                        distanceToSegment(p, origin, ray.end)
                    }
                    if (hit != null && distanceToSegment(p, origin, hit.value.end) < 0.12f) {
                        onPick(Gree.SWING_FIRST_FIXED + hit.index)
                    }
                }
            },
    ) {
        val w = size.width
        val h = size.height
        fun pt(o: Offset) = Offset(o.x * w, o.y * h)

        // The unit itself.
        if (horizontal) {
            val bodyW = w * 0.5f
            val bodyH = h * 0.145f
            val left = (w - bodyW) / 2f
            drawRoundRect(
                color = c.ink,
                topLeft = Offset(left, h * 0.08f),
                size = androidx.compose.ui.geometry.Size(bodyW, bodyH),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(bodyH * 0.36f),
            )
            drawLine(
                color = c.accent,
                start = Offset(left + bodyW * 0.1f, h * 0.08f + bodyH * 0.78f),
                end = Offset(left + bodyW * 0.9f, h * 0.08f + bodyH * 0.78f),
                strokeWidth = h * 0.026f,
                cap = StrokeCap.Round,
            )
        } else {
            val bodyW = w * 0.24f
            val bodyH = h * 0.22f
            val left = w * 0.22f
            val top = h * 0.2f
            drawRoundRect(
                color = c.ink,
                topLeft = Offset(left, top),
                size = androidx.compose.ui.geometry.Size(bodyW, bodyH),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(bodyH * 0.34f),
            )
            drawLine(
                color = c.accent,
                start = Offset(left + bodyW * 0.16f, top + bodyH * 0.8f),
                end = Offset(left + bodyW * 0.84f, top + bodyH * 0.8f),
                strokeWidth = h * 0.026f,
                cap = StrokeCap.Round,
            )
        }

        rays.forEachIndexed { index, ray ->
            val on = full || index == selectedRay
            drawLine(
                color = if (on) c.accent else c.ink2,
                alpha = if (full) fullAlpha else if (on) 1f else 0.3f,
                start = pt(origin),
                end = pt(ray.end),
                strokeWidth = if (index == selectedRay && !full) h * 0.05f else h * 0.035f,
                cap = StrokeCap.Round,
            )
        }
    }
}

/** Five evenly fanned rays, sweeping down-right for vertical and left-to-right otherwise. */
private fun rayGeometry(horizontal: Boolean): List<Ray> {
    val origin = if (horizontal) Offset(0.5f, 0.23f) else Offset(0.38f, 0.42f)
    val startDeg = if (horizontal) 146.0 else 10.0
    val endDeg = if (horizontal) 34.0 else 82.0
    // Kept short enough that the outermost rays stay inside the canvas: the horizontal
    // fan reaches widest at 146 degrees, which ran off the left edge at 0.66.
    val radius = if (horizontal) 0.50f else 0.54f

    return (0 until Gree.SWING_FIXED_COUNT).map { i ->
        val t = i / (Gree.SWING_FIXED_COUNT - 1).toDouble()
        val deg = startDeg + (endDeg - startDeg) * t
        val rad = Math.toRadians(deg)
        Ray(Offset(origin.x + (radius * Math.cos(rad)).toFloat(), origin.y + (radius * Math.sin(rad)).toFloat()))
    }
}

/** Perpendicular distance from [p] to segment [a]..[b], in normalised units. */
private fun distanceToSegment(p: Offset, a: Offset, b: Offset): Float {
    val abx = b.x - a.x
    val aby = b.y - a.y
    val lenSq = abx * abx + aby * aby
    if (lenSq == 0f) return (p - a).getDistance()
    val t = (((p.x - a.x) * abx + (p.y - a.y) * aby) / lenSq).coerceIn(0f, 1f)
    return (p - Offset(a.x + t * abx, a.y + t * aby)).getDistance()
}
