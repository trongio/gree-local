package ge.hackerman.gree.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AcUnit
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ge.hackerman.gree.data.GreeDevice
import ge.hackerman.gree.protocol.Gree
import ge.hackerman.gree.protocol.GreeFan
import ge.hackerman.gree.protocol.GreeMode
import ge.hackerman.gree.protocol.GreeState
import ge.hackerman.gree.protocol.GreeSwing

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ControlScreen(
    device: GreeDevice,
    state: GreeState?,
    online: Boolean,
    snackbarHostState: SnackbarHostState,
    onBack: () -> Unit,
    onSend: (Array<out Pair<String, Int>>) -> Unit,
) {
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(device.name)
                        Text(
                            if (online) device.ip else "${device.ip} · offline",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (online) MaterialTheme.colorScheme.onSurfaceVariant
                            else MaterialTheme.colorScheme.error,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        if (state == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            TemperatureDial(state = state, onSend = onSend)
            PowerRow(state = state, onSend = onSend)

            Section("Mode") {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    GreeMode.entries.forEach { mode ->
                        FilterChip(
                            selected = state.mode == mode,
                            onClick = { onSend(arrayOf(Gree.MODE to mode.raw)) },
                            label = { Text(mode.label) },
                        )
                    }
                }
            }

            Section("Fan") {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    GreeFan.entries.forEach { fan ->
                        FilterChip(
                            selected = state.fan == fan,
                            onClick = { onSend(arrayOf(Gree.FAN_SPEED to fan.raw)) },
                            label = { Text(fan.label) },
                        )
                    }
                }
            }

            Section("Vertical swing") {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    GreeSwing.entries.forEach { swing ->
                        FilterChip(
                            selected = state.swingVertical == swing,
                            onClick = { onSend(arrayOf(Gree.SWING_VERTICAL to swing.raw)) },
                            label = { Text(swing.label) },
                        )
                    }
                }
            }

            Section("Options") {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Toggle("Turbo", state.turbo) { onSend(arrayOf(Gree.TURBO to it)) }
                    Toggle("Quiet", state.quiet) { onSend(arrayOf(Gree.QUIET to it)) }
                    Toggle("Sleep", state.sleep) { onSend(arrayOf(Gree.SLEEP to it)) }
                    Toggle("Display", state.light) { onSend(arrayOf(Gree.LIGHT to it)) }
                    Toggle("X-Fan", state.xfan) { onSend(arrayOf(Gree.XFAN to it)) }
                    Toggle("Health", state.health) { onSend(arrayOf(Gree.HEALTH to it)) }
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun TemperatureDial(state: GreeState, onSend: (Array<out Pair<String, Int>>) -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                FilledTonalIconButton(
                    onClick = {
                        onSend(
                            arrayOf(
                                Gree.SET_TEMP to (state.targetTemp - 1).coerceAtLeast(Gree.MIN_TEMP),
                            ),
                        )
                    },
                    enabled = state.targetTemp > Gree.MIN_TEMP,
                ) {
                    Icon(Icons.Rounded.Remove, contentDescription = "Cooler")
                }

                Text(
                    text = "${state.targetTemp}°",
                    fontSize = 72.sp,
                    fontWeight = FontWeight.Light,
                    modifier = Modifier.padding(horizontal = 24.dp),
                )

                FilledTonalIconButton(
                    onClick = {
                        onSend(
                            arrayOf(
                                Gree.SET_TEMP to (state.targetTemp + 1).coerceAtMost(Gree.MAX_TEMP),
                            ),
                        )
                    },
                    enabled = state.targetTemp < Gree.MAX_TEMP,
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = "Warmer")
                }
            }

            state.roomTemp?.let {
                Text(
                    "Room $it°C",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun PowerRow(state: GreeState, onSend: (Array<out Pair<String, Int>>) -> Unit) {
    val tint by animateColorAsState(
        targetValue = if (state.power) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.surfaceVariant,
        label = "power",
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FilledIconButton(
            onClick = { onSend(arrayOf(Gree.POWER to if (state.power) 0 else 1)) },
            modifier = Modifier.size(72.dp),
            colors = IconButtonDefaults.filledIconButtonColors(containerColor = tint),
        ) {
            Icon(
                Icons.Rounded.PowerSettingsNew,
                contentDescription = if (state.power) "Turn off" else "Turn on",
                modifier = Modifier.size(32.dp),
            )
        }
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        content()
    }
}

@Composable
private fun Toggle(label: String, active: Boolean, onChange: (Int) -> Unit) {
    FilterChip(
        selected = active,
        onClick = { onChange(if (active) 0 else 1) },
        label = { Text(label) },
    )
}
