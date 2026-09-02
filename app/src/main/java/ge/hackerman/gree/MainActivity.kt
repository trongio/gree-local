package ge.hackerman.gree

import android.os.Bundle
import androidx.activity.ComponentActivity
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
import ge.hackerman.gree.ui.AddByIpSheet
import ge.hackerman.gree.ui.ControlScreen
import ge.hackerman.gree.ui.HomeScreen
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
    val devices by viewModel.devices.collectAsStateWithLifecycle()
    val selectedMac by viewModel.selectedMac.collectAsStateWithLifecycle()
    val scanning by viewModel.scanning.collectAsStateWithLifecycle()
    val progress by viewModel.progress.collectAsStateWithLifecycle()
    val hostCount by viewModel.hostCount.collectAsStateWithLifecycle()
    val subnetLabel by viewModel.subnetLabel.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()

    var sheetOpen by remember { mutableStateOf(false) }
    val colors = Gree.colors
    val selected = devices.firstOrNull { it.mac == selectedMac }

    Box(
        Modifier
            .fillMaxSize()
            .background(colors.bg)
            .systemBarsPadding(),
    ) {
        if (selected == null) {
            HomeScreen(
                subnetLabel = subnetLabel,
                devices = devices.map(viewModel::uiFor),
                scanning = scanning,
                progress = progress,
                hostCount = hostCount,
                onScan = viewModel::scan,
                onAdd = { sheetOpen = true },
                onSelect = viewModel::select,
                onForget = viewModel::forget,
                onTogglePower = viewModel::togglePower,
            )
        } else {
            ControlScreen(
                ui = viewModel.uiFor(selected),
                onBack = { viewModel.select(null) },
                onSend = { viewModel.send(selected.mac, it) },
            )
        }

        Toast(message)
    }

    if (sheetOpen) {
        AddByIpSheet(
            onDismiss = { sheetOpen = false },
            onConfirm = {
                sheetOpen = false
                viewModel.addByIp(it)
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
