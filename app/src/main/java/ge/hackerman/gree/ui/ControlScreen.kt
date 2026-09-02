package ge.hackerman.gree.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import ge.hackerman.gree.DeviceUi
import ge.hackerman.gree.protocol.Gree
import ge.hackerman.gree.protocol.GreeFan
import ge.hackerman.gree.protocol.GreeMode
import ge.hackerman.gree.protocol.GreeState
import ge.hackerman.gree.ui.theme.AlbertSans
import ge.hackerman.gree.ui.theme.PlexMono
import ge.hackerman.gree.ui.theme.Gree as GreeTheme

/** The six toggles the design exposes, in its order. */
private val OPTIONS = listOf(
    Triple(Gree.TURBO, "Turbo", Sym.BOLT),
    Triple(Gree.QUIET, "Quiet", Sym.VOLUME_OFF),
    Triple(Gree.SLEEP, "Sleep", Sym.BEDTIME),
    Triple(Gree.LIGHT, "Display", Sym.LIGHTBULB),
    Triple(Gree.XFAN, "X-Fan", Sym.DRY),
    Triple(Gree.HEALTH, "Health", Sym.ECO),
)

@Composable
fun ControlScreen(
    ui: DeviceUi,
    onBack: () -> Unit,
    onSend: (Pair<String, Int>) -> Unit,
    onRename: () -> Unit,
) {
    val c = GreeTheme.colors
    val state = ui.state

    // The glow used to be drawn inside the scrolling column, which clips at its top
    // edge, slicing the wash off flat under the header. It now lives behind the whole
    // screen and is anchored to wherever the number actually is, so it can bleed up
    // past the header and still follow the number as the content scrolls.
    var rootOrigin by remember { mutableStateOf(Offset.Zero) }
    var glowCenter by remember { mutableStateOf<Offset?>(null) }

    val tint = when {
        state == null || !state.power -> Color.Transparent
        state.mode == GreeMode.COOL -> c.accent.copy(alpha = 0.4f)
        state.mode == GreeMode.HEAT -> c.warm.copy(alpha = 0.4f)
        else -> c.ink.copy(alpha = 0.12f)
    }
    val glow by animateColorAsState(tint, tween(400), label = "heroTint")

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onGloballyPositioned { rootOrigin = it.positionInRoot() }
            .drawBehind {
                val center = glowCenter ?: return@drawBehind
                val radius = size.width * 0.52f
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(glow, Color.Transparent),
                        center = center,
                        radius = radius,
                    ),
                    radius = radius,
                    center = center,
                )
            },
    ) {
    Column(Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, end = 22.dp, top = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onBack),
                contentAlignment = Alignment.Center,
            ) {
                Symbol(Sym.CHEVRON_LEFT, 26.sp, c.ink, contentDescription = "Back")
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onRename)
                    .padding(vertical = 2.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    ui.device.name,
                    fontFamily = AlbertSans,
                    fontWeight = FontWeight.W600,
                    fontSize = 18.sp,
                    letterSpacing = (-0.01).em,
                    color = c.ink,
                )
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(
                        Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(if (ui.online) c.accent else c.warm),
                    )
                    Text(
                        "${ui.device.ip} · ${if (ui.online) "online" else "offline"}",
                        fontFamily = PlexMono,
                        fontSize = 11.sp,
                        lineHeight = 11.sp,
                        letterSpacing = 0.04.em,
                        color = c.ink2,
                    )
                }
            }
            PowerButton(on = state?.power == true) {
                onSend(Gree.POWER to if (state?.power == true) 0 else 1)
            }
        }

        if (state == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Connecting…", fontFamily = AlbertSans, fontSize = 15.sp, color = c.ink2)
            }
            return@Column
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 20.dp, bottom = 6.dp),
            ) {
                TemperatureBar(
                    state = state,
                    caption = heroCaption(state),
                    onSetTemp = { onSend(Gree.SET_TEMP to it) },
                    onGlowAnchor = { glowCenter = it - rootOrigin },
                )
            }

            ModeRow(state = state, onSend = onSend)
            FanRow(state = state, onSend = onSend)
            OptionsGrid(state = state, onSend = onSend)

            SwingCard(
                title = "Vertical swing",
                valueLabel = state.swingVertical.label,
                raw = state.raw[Gree.SWING_VERTICAL] ?: 0,
                horizontal = false,
                onPick = { onSend(Gree.SWING_VERTICAL to it) },
            )
            SwingCard(
                title = "Horizontal swing",
                valueLabel = state.swingHorizontal.label,
                raw = state.raw[Gree.SWING_HORIZONTAL] ?: 0,
                horizontal = true,
                onPick = { onSend(Gree.SWING_HORIZONTAL to it) },
            )
        }
    }
    }
}

@Composable
private fun PowerButton(on: Boolean, onClick: () -> Unit) {
    val c = GreeTheme.colors
    val bg by animateColorAsState(if (on) c.accent else Color.Transparent, tween(250), label = "powerBg")
    Box(
        modifier = Modifier
            .size(52.dp)
            .clip(CircleShape)
            .background(bg)
            .border(1.dp, c.line, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Symbol(
            Sym.POWER,
            24.sp,
            if (on) c.onAccent else c.ink,
            contentDescription = if (on) "Turn off" else "Turn on",
        )
    }
}

/** Room, mode and fan in one mono line beside the TARGET label. */
private fun heroCaption(state: GreeState): String {
    val room = state.roomTemp?.let { "ROOM $it°C" } ?: "ROOM —"
    return if (state.power) {
        "$room · ${state.mode.label.uppercase()} · FAN ${state.fan.label.uppercase()}"
    } else {
        "$room · STANDBY"
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text.uppercase(),
        fontFamily = PlexMono,
        fontWeight = FontWeight.W500,
        fontSize = 11.sp,
        lineHeight = 11.sp,
        letterSpacing = 0.12.em,
        color = GreeTheme.colors.ink2,
    )
}

@Composable
private fun ModeRow(state: GreeState, onSend: (Pair<String, Int>) -> Unit) {
    val c = GreeTheme.colors
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionLabel("Mode")
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(c.card)
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            GreeMode.entries.forEach { mode ->
                val on = state.mode == mode
                val bg = when {
                    !on -> Color.Transparent
                    mode == GreeMode.COOL -> c.accent
                    mode == GreeMode.HEAT -> c.warm
                    else -> c.ink
                }
                val fg = when {
                    !on -> c.ink
                    mode == GreeMode.COOL || mode == GreeMode.HEAT -> c.onAccent
                    else -> c.bg
                }
                val bgAnim by animateColorAsState(bg, tween(200), label = "modeBg")

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .height(62.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(bgAnim)
                        .clickable { onSend(Gree.MODE to mode.raw) },
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Symbol(mode.symbol, 22.sp, fg, filled = on)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        mode.label,
                        fontFamily = AlbertSans,
                        fontWeight = FontWeight.W500,
                        fontSize = 11.sp,
                        lineHeight = 11.sp,
                        color = fg,
                    )
                }
            }
        }
    }
}

@Composable
private fun FanRow(state: GreeState, onSend: (Pair<String, Int>) -> Unit) {
    val c = GreeTheme.colors
    val current = state.raw[Gree.FAN_SPEED] ?: 0
    val auto = current == GreeFan.AUTO.raw

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            SectionLabel("Fan")
            Text(
                state.fan.label,
                fontFamily = AlbertSans,
                fontWeight = FontWeight.W500,
                fontSize = 13.sp,
                color = c.ink,
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth().height(44.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            // Auto is its own control: as one of the bars it just looked like "slowest".
            val autoBg by animateColorAsState(if (auto) c.ink else c.card, tween(200), label = "autoBg")
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(autoBg)
                    .clickable { onSend(Gree.FAN_SPEED to GreeFan.AUTO.raw) },
                contentAlignment = Alignment.Center,
            ) {
                Symbol(
                    Sym.HDR_AUTO,
                    20.sp,
                    if (auto) c.bg else c.ink,
                    filled = auto,
                    contentDescription = "Automatic fan speed",
                )
            }

            // The ramp now covers real speeds only, Low through High.
            val speeds = GreeFan.entries.filter { it != GreeFan.AUTO }
            speeds.forEachIndexed { index, fan ->
                val filled = !auto && current >= fan.raw
                val bg by animateColorAsState(if (filled) c.accent else c.card, tween(200), label = "fanBg")
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height((20 + index * 6).dp)
                        .clip(RoundedCornerShape(7.dp))
                        .background(bg)
                        .clickable { onSend(Gree.FAN_SPEED to fan.raw) },
                )
            }
        }
    }
}

@Composable
private fun OptionsGrid(state: GreeState, onSend: (Pair<String, Int>) -> Unit) {
    val c = GreeTheme.colors
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionLabel("Options")
        OPTIONS.chunked(3).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { (key, label, glyph) ->
                    val on = (state.raw[key] ?: 0) > 0
                    val bgAnim by animateColorAsState(if (on) c.ink else c.card, tween(200), label = "optBg")
                    val fg = if (on) c.bg else c.ink

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .height(74.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(bgAnim)
                            .clickable { onSend(key to if (on) 0 else 1) }
                            .padding(horizontal = 14.dp),
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Symbol(glyph, 22.sp, fg, filled = on)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            label,
                            fontFamily = AlbertSans,
                            fontWeight = FontWeight.W500,
                            fontSize = 13.sp,
                            lineHeight = 13.sp,
                            color = fg,
                        )
                    }
                }
            }
        }
    }
}
