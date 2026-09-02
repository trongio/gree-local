package ge.hackerman.gree

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import ge.hackerman.gree.ui.ControlScreen
import ge.hackerman.gree.ui.HomeScreen
import ge.hackerman.gree.ui.TextInputSheet
import ge.hackerman.gree.ui.theme.Gree
import ge.hackerman.gree.ui.theme.GreeTheme
import ge.hackerman.gree.ui.theme.PlexMono
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GreeTheme {
                GreeApp()
            }
        }
    }
}

@Composable
private fun GreeApp(viewModel: GreeViewModel = viewModel()) {
    val devices by viewModel.deviceUis.collectAsStateWithLifecycle()
    val selected by viewModel.selectedUi.collectAsStateWithLifecycle()
    val scanning by viewModel.scanning.collectAsStateWithLifecycle()
    val progress by viewModel.progress.collectAsStateWithLifecycle()
    val hostCount by viewModel.hostCount.collectAsStateWithLifecycle()
    val subnetLabel by viewModel.subnetLabel.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()

    var addSheetOpen by remember { mutableStateOf(false) }
    var renaming by remember { mutableStateOf<String?>(null) }
    val colors = Gree.colors

    // Without this, the system back gesture leaves the app instead of leaving the unit.
    BackHandler(enabled = selected != null) { viewModel.select(null) }

    Box(
        Modifier
            .fillMaxSize()
            .background(colors.bg)
            .systemBarsPadding(),
    ) {
        val current = selected
        if (current == null) {
            HomeScreen(
                subnetLabel = subnetLabel,
                devices = devices,
                scanning = scanning,
                progress = progress,
                hostCount = hostCount,
                onScan = viewModel::scan,
                onAdd = { addSheetOpen = true },
                onSelect = viewModel::select,
                onForget = viewModel::forget,
                onTogglePower = viewModel::togglePower,
                onRename = { renaming = it },
            )
        } else {
            ControlScreen(
                ui = current,
                onBack = { viewModel.select(null) },
                onSend = { viewModel.send(current.device.mac, it) },
                onRename = { renaming = current.device.mac },
            )
        }

        Toast(message)
    }

    if (addSheetOpen) {
        TextInputSheet(
            title = "Add by IP",
            description = "For networks that block subnet sweeps. The unit must still be on this Wi-Fi.",
            placeholder = "192.168.0.199",
            confirmLabel = "Add",
            onDismiss = { addSheetOpen = false },
            onConfirm = {
                addSheetOpen = false
                viewModel.addByIp(it)
            },
        )
    }

    renaming?.let { mac ->
        val current = devices.firstOrNull { it.device.mac == mac }?.device
        TextInputSheet(
            title = "Rename unit",
            description = "Only stored on this phone. The unit itself keeps its own name.",
            placeholder = current?.mac?.takeLast(6) ?: "Living room",
            confirmLabel = "Save",
            initialValue = current?.name.orEmpty(),
            mono = false,
            numeric = false,
            onDismiss = { renaming = null },
            onConfirm = {
                renaming = null
                viewModel.rename(mac, it)
            },
        )
    }

    LaunchedEffect(message) {
        if (message != null) {
            delay(2600)
            viewModel.consumeMessage()
        }
    }
}

/** A mono pill above the scan button, matching the design's toast. */
@Composable
private fun androidx.compose.foundation.layout.BoxScope.Toast(message: String?) {
    val c = Gree.colors
    AnimatedVisibility(
        visible = message != null,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .padding(bottom = 96.dp),
    ) {
        Text(
            text = message.orEmpty(),
            fontFamily = PlexMono,
            fontWeight = FontWeight.W400,
            fontSize = 12.sp,
            lineHeight = 14.sp,
            color = c.bg,
            modifier = Modifier
                .clip(RoundedCornerShape(14.dp))
                .background(c.ink)
                .padding(horizontal = 16.dp, vertical = 12.dp),
        )
    }
}
