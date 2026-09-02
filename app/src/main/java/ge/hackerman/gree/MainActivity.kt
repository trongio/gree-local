package ge.hackerman.gree

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import ge.hackerman.gree.ui.ControlScreen
import ge.hackerman.gree.ui.DeviceListScreen
import ge.hackerman.gree.ui.GreeTheme

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
    val selected by viewModel.selected.collectAsStateWithLifecycle()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val scanning by viewModel.scanning.collectAsStateWithLifecycle()
    val online by viewModel.online.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(message) {
        message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.consumeMessage()
        }
    }

    val device = selected
    if (device == null) {
        DeviceListScreen(
            devices = devices,
            scanning = scanning,
            snackbarHostState = snackbarHostState,
            onScan = viewModel::scan,
            onAddByIp = viewModel::addByIp,
            onSelect = viewModel::select,
            onForget = viewModel::forget,
        )
    } else {
        ControlScreen(
            device = device,
            state = state,
            online = online,
            snackbarHostState = snackbarHostState,
            onBack = { viewModel.select(null) },
            onSend = { viewModel.send(*it) },
        )
    }
}
