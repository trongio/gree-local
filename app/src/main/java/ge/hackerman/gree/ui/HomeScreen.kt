package ge.hackerman.gree.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.Image
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import ge.hackerman.gree.DeviceUi
import ge.hackerman.gree.R
import ge.hackerman.gree.protocol.GreeMode
import ge.hackerman.gree.ui.theme.AlbertSans
import ge.hackerman.gree.ui.theme.Gree
import ge.hackerman.gree.ui.theme.PlexMono

@Composable
fun HomeScreen(
    subnetLabel: String,
    devices: List<DeviceUi>,
    scanning: Boolean,
    progress: Float,
    hostCount: Int,
    onScan: () -> Unit,
    onAdd: () -> Unit,
    onSelect: (String) -> Unit,
    onForget: (String) -> Unit,
    onTogglePower: (String) -> Unit,
    onRename: (String) -> Unit,
) {
    val c = Gree.colors

    Column(Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 6.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    subnetLabel,
                    fontFamily = PlexMono,
                    fontSize = 11.sp,
                    lineHeight = 11.sp,
                    letterSpacing = 0.06.em,
                    color = c.ink2,
                )
                Text(
                    "Units",
                    fontFamily = AlbertSans,
                    fontWeight = FontWeight.W600,
                    fontSize = 34.sp,
                    lineHeight = 34.sp,
                    letterSpacing = (-0.02).em,
                    color = c.ink,
                )
            }
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(c.card)
                    .border(1.dp, c.line, CircleShape)
                    .clickable(onClick = onAdd),
                contentAlignment = Alignment.Center,
            ) {
                Symbol(Sym.ADD, 22.sp, c.ink, contentDescription = "Add by IP")
            }
        }

        if (devices.isEmpty()) {
            EmptyState(Modifier.weight(1f))
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(devices, key = { it.device.mac }) { ui ->
                    DeviceCard(
                        ui = ui,
                        onClick = { onSelect(ui.device.mac) },
                        onLongClick = { onRename(ui.device.mac) },
                        onForget = { onForget(ui.device.mac) },
                        onTogglePower = { onTogglePower(ui.device.mac) },
                    )
                }
            }
        }

        Box(Modifier.padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 20.dp)) {
            ScanButton(scanning = scanning, progress = progress, hostCount = hostCount, onScan = onScan)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DeviceCard(
    ui: DeviceUi,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onForget: () -> Unit,
    onTogglePower: () -> Unit,
) {
    val c = Gree.colors
    val state = ui.state
    val power = state?.power == true
    val mode = state?.mode ?: GreeMode.AUTO

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(c.card)
            // Long-press renames; the design has no dedicated affordance for it.
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                ui.device.name,
                fontFamily = AlbertSans,
                fontWeight = FontWeight.W600,
                fontSize = 20.sp,
                letterSpacing = (-0.01).em,
                color = c.ink,
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                Box(
                    Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(if (ui.online) c.accent else c.warm),
                )
                Text(
                    if (ui.online) "online" else "offline",
                    fontFamily = PlexMono,
                    fontSize = 11.sp,
                    lineHeight = 11.sp,
                    letterSpacing = 0.06.em,
                    color = c.ink2,
                )
            }
        }

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom,
        ) {
            Text(
                text = if (power && state != null) "${state.targetTemp}°" else "Off",
                fontFamily = AlbertSans,
                fontWeight = FontWeight.W300,
                fontSize = 48.sp,
                lineHeight = 48.sp,
                letterSpacing = (-0.03).em,
                color = if (power) c.ink else c.ink2,
            )
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(bottom = 4.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Symbol(mode.symbol, 16.sp, c.ink, filled = true)
                    Text(
                        mode.label,
                        fontFamily = AlbertSans,
                        fontWeight = FontWeight.W500,
                        fontSize = 13.sp,
                        color = c.ink,
                    )
                }
                Text(
                    state?.roomTemp?.let { "room $it°" } ?: "room —",
                    fontFamily = PlexMono,
                    fontSize = 12.sp,
                    lineHeight = 12.sp,
                    color = c.ink2,
                )
            }
        }

        Box(Modifier.fillMaxWidth().height(1.dp).background(c.line))

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "${ui.device.ip} · ${ui.device.mac}",
                fontFamily = PlexMono,
                fontSize = 11.sp,
                lineHeight = 11.sp,
                color = c.ink2,
                modifier = Modifier.weight(1f, fill = false),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .clickable(onClick = onForget),
                    contentAlignment = Alignment.Center,
                ) {
                    Symbol(Sym.DELETE, 20.sp, c.ink2, contentDescription = "Forget ${ui.device.name}")
                }
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(if (power) c.accent else Color.Transparent)
                        .clickable(onClick = onTogglePower),
                    contentAlignment = Alignment.Center,
                ) {
                    Symbol(
                        Sym.POWER,
                        20.sp,
                        if (power) c.onAccent else c.ink,
                        contentDescription = if (power) "Turn off" else "Turn on",
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    val c = Gree.colors
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(
            painter = painterResource(R.drawable.mark),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier.width(132.dp).padding(bottom = 8.dp),
        )
        Text(
            "No units yet",
            fontFamily = AlbertSans,
            fontWeight = FontWeight.W600,
            fontSize = 24.sp,
            letterSpacing = (-0.01).em,
            color = c.ink,
        )
        Spacer(Modifier.height(10.dp))
        Text(
            "Make sure your phone is on the same Wi-Fi as the air conditioner, then scan.",
            fontFamily = AlbertSans,
            fontSize = 14.sp,
            lineHeight = 21.sp,
            color = c.ink2,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = 260.dp),
        )
    }
}

@Composable
private fun ScanButton(scanning: Boolean, progress: Float, hostCount: Int, onScan: () -> Unit) {
    val c = Gree.colors

    // A shimmer travelling across the fill, so a slow sweep still looks alive.
    val transition = rememberInfiniteTransition(label = "sweep")
    val shimmer by transition.animateFloat(
        initialValue = -1f,
        targetValue = 3f,
        animationSpec = infiniteRepeatable(tween(1400), RepeatMode.Restart),
        label = "shimmer",
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp)
            .clip(CircleShape)
            .background(c.ink)
            .clickable(enabled = !scanning, onClick = onScan)
            .then(
                if (!scanning) Modifier else Modifier.drawWithContent {
                    drawContent()
                    drawRect(
                        color = c.accent.copy(alpha = 0.28f),
                        size = Size(size.width * progress.coerceIn(0f, 1f), size.height),
                    )
                    val band = size.width / 3f
                    drawRect(
                        brush = Brush.horizontalGradient(
                            listOf(Color.Transparent, Color.White.copy(alpha = 0.14f), Color.Transparent),
                            startX = shimmer * band,
                            endX = shimmer * band + band,
                        ),
                        topLeft = Offset(shimmer * band, 0f),
                        size = Size(band, size.height),
                    )
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Symbol(if (scanning) Sym.WIFI_FIND else Sym.RADAR, 22.sp, c.bg)
            Text(
                text = if (scanning) {
                    "Sweeping $hostCount hosts · ${(progress * 100).toInt()}%"
                } else {
                    "Scan network"
                },
                fontFamily = AlbertSans,
                fontWeight = FontWeight.W600,
                fontSize = 16.sp,
                color = c.bg,
            )
        }
    }
}
