package ge.hackerman.gree.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import ge.hackerman.gree.ui.theme.AlbertSans
import ge.hackerman.gree.ui.theme.PlexMono
import ge.hackerman.gree.ui.theme.Gree as GreeTheme

/**
 * The one sheet shape the app needs: a title, an explanation, a single field and a
 * confirm. Used for both adding a unit by IP and renaming one.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TextInputSheet(
    title: String,
    description: String,
    placeholder: String,
    confirmLabel: String,
    initialValue: String = "",
    mono: Boolean = true,
    numeric: Boolean = true,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    val c = GreeTheme.colors
    var value by remember { mutableStateOf(initialValue) }
    val sheetState = rememberModalBottomSheetState()
    val focus = remember { FocusRequester() }
    val family: FontFamily = if (mono) PlexMono else AlbertSans

    LaunchedEffect(Unit) { focus.requestFocus() }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = c.card,
        dragHandle = {
            Box(Modifier.padding(top = 20.dp, bottom = 4.dp)) {
                Box(
                    Modifier
                        .size(width = 36.dp, height = 4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(c.line),
                )
            }
        },
    ) {
        Column(
            modifier = Modifier
                .imePadding()
                .padding(start = 24.dp, end = 24.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    title,
                    fontFamily = AlbertSans,
                    fontWeight = FontWeight.W600,
                    fontSize = 22.sp,
                    letterSpacing = (-0.01).em,
                    color = c.ink,
                )
                Text(
                    description,
                    fontFamily = AlbertSans,
                    fontSize = 13.sp,
                    lineHeight = 19.sp,
                    color = c.ink2,
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(c.bg)
                    .border(1.dp, c.line, RoundedCornerShape(16.dp))
                    .padding(horizontal = 18.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                BasicTextField(
                    value = value,
                    onValueChange = { value = it },
                    singleLine = true,
                    textStyle = TextStyle(fontFamily = family, fontSize = 18.sp, color = c.ink),
                    cursorBrush = SolidColor(c.accent),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = if (numeric) KeyboardType.Decimal else KeyboardType.Text,
                        imeAction = ImeAction.Done,
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = { if (value.isNotBlank()) onConfirm(value.trim()) },
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focus),
                    decorationBox = { inner ->
                        if (value.isEmpty()) {
                            Text(placeholder, fontFamily = family, fontSize = 18.sp, color = c.ink2)
                        }
                        inner()
                    },
                )
            }

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .height(48.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .clickable(onClick = onDismiss)
                        .padding(horizontal = 18.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "Cancel",
                        fontFamily = AlbertSans,
                        fontWeight = FontWeight.W600,
                        fontSize = 15.sp,
                        color = c.ink,
                    )
                }
                Box(
                    modifier = Modifier
                        .height(48.dp)
                        .alpha(if (value.isNotBlank()) 1f else 0.4f)
                        .clip(RoundedCornerShape(24.dp))
                        .background(c.ink)
                        .clickable(enabled = value.isNotBlank()) { onConfirm(value.trim()) }
                        .padding(horizontal = 24.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        confirmLabel,
                        fontFamily = AlbertSans,
                        fontWeight = FontWeight.W600,
                        fontSize = 15.sp,
                        color = c.bg,
                    )
                }
            }
        }
    }
}
