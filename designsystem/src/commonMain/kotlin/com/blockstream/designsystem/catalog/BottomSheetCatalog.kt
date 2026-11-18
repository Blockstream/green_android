package com.blockstream.designsystem.catalog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.blockstream.designsystem.AppTheme
import com.blockstream.designsystem.components.AppBottomSheet
import com.blockstream.designsystem.components.AppBottomSheetState
import com.blockstream.designsystem.components.AppBottomSheetType
import com.blockstream.designsystem.components.AppBottomSheetValue
import com.blockstream.designsystem.components.AppButton
import com.blockstream.designsystem.components.AppButtonVariant
import com.blockstream.designsystem.components.AppText
import com.blockstream.designsystem.components.AppTextField
import com.blockstream.designsystem.components.rememberAppBottomSheetState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

private enum class CatalogSheet {
    Modal,
    Default,
    LongList,
    Keyboard,
    RefusesToClose,
    PartiallyExpanded
}

@Composable
fun BottomSheetCatalog() {
    var open by rememberSaveable { mutableStateOf<CatalogSheet?>(null) }
    val close = { open = null }

    Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
        CatalogSheetButton("Modal, short content") { open = CatalogSheet.Modal }
        CatalogSheetButton("Default type, no dimming") { open = CatalogSheet.Default }
        CatalogSheetButton("Long list, the list scrolls") { open = CatalogSheet.LongList }
        CatalogSheetButton("Text field, keyboard") { open = CatalogSheet.Keyboard }
        CatalogSheetButton("Refuses to close") { open = CatalogSheet.RefusesToClose }
        CatalogSheetButton("Opens half height") { open = CatalogSheet.PartiallyExpanded }
    }

    when (open) {
        CatalogSheet.Modal -> ShortSheet(type = AppBottomSheetType.Modal, onClose = close)
        CatalogSheet.Default -> ShortSheet(type = AppBottomSheetType.Default, onClose = close)
        CatalogSheet.LongList -> ListSheet(skipPartiallyExpanded = true, onClose = close)
        CatalogSheet.Keyboard -> KeyboardSheet(onClose = close)
        CatalogSheet.RefusesToClose -> RefusesToCloseSheet(onClose = close)
        CatalogSheet.PartiallyExpanded -> ListSheet(skipPartiallyExpanded = false, onClose = close)
        null -> Unit
    }
}

@Composable
private fun CatalogSheetButton(label: String, onClick: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        CatalogLabel(label)
        AppButton(text = "Open", onClick = onClick, variant = AppButtonVariant.Secondary, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun ShortSheet(type: AppBottomSheetType, onClose: () -> Unit) {
    val state = rememberAppBottomSheetState()
    val scope = rememberCoroutineScope()
    AppBottomSheet(onDismissRequest = onClose, sheetState = state, type = type) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            AppText("Receive to", style = AppTheme.typography.title.mSemiBold)
            AppText("Drag down, tap outside or press back to close.", color = AppTheme.colors.text.subtle)
            AppButton(text = "Close", onClick = { scope.hideThenClose(state, onClose) }, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun ListSheet(skipPartiallyExpanded: Boolean, onClose: () -> Unit) {
    val state = rememberAppBottomSheetState(skipPartiallyExpanded = skipPartiallyExpanded)
    AppBottomSheet(onDismissRequest = onClose, sheetState = state, contentPadding = PaddingValues(bottom = 24.dp)) {
        AppText(
            text = "Transactions",
            style = AppTheme.typography.title.mSemiBold,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        LazyColumn {
            items(50) { index ->
                AppText(
                    text = "Transaction ${index + 1}",
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)
                )
            }
        }
    }
}

@Composable
private fun KeyboardSheet(onClose: () -> Unit) {
    val state = rememberAppBottomSheetState()
    val scope = rememberCoroutineScope()
    var name by rememberSaveable { mutableStateOf("") }
    AppBottomSheet(onDismissRequest = onClose, sheetState = state) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            AppText("Rename wallet", style = AppTheme.typography.title.mSemiBold)
            AppTextField(value = name, onValueChange = { name = it }, placeholder = "Wallet name")
            AppButton(text = "Save", onClick = { scope.hideThenClose(state, onClose) }, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun RefusesToCloseSheet(onClose: () -> Unit) {
    var canClose by remember { mutableStateOf(false) }
    val state = rememberAppBottomSheetState(confirmValueChange = { it != AppBottomSheetValue.Hidden || canClose })
    val scope = rememberCoroutineScope()
    AppBottomSheet(onDismissRequest = onClose, sheetState = state) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            AppText("Finish setup", style = AppTheme.typography.title.mSemiBold)
            AppText("Back, tapping outside and dragging down do not close this sheet. Only the button does.", color = AppTheme.colors.text.subtle)
            AppButton(
                text = "Close",
                onClick = {
                    canClose = true
                    scope.hideThenClose(state, onClose)
                },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

private fun CoroutineScope.hideThenClose(state: AppBottomSheetState, onClose: () -> Unit) {
    launch { state.hide() }.invokeOnCompletion { if (!state.isVisible) onClose() }
}
