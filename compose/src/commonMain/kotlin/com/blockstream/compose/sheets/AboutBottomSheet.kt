package com.blockstream.compose.sheets

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.blockstream.compose.components.GreenBottomSheet
import com.blockstream.compose.components.PopupState
import com.blockstream.compose.models.about.AboutViewModelAbstract
import com.blockstream.compose.screens.about.AboutContent
import com.blockstream.compose.sideeffects.SideEffects

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutBottomSheet(
    viewModel: AboutViewModelAbstract,
    onDismissRequest: () -> Unit
) {
    val popupState = remember { PopupState() }

    GreenBottomSheet(
        viewModel = viewModel,
        onDismissRequest = onDismissRequest,
        withHorizontalPadding = false,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        sideEffectHandler = {
            if (it is SideEffects.OpenMenu) {
                popupState.isContextMenuVisible.value = true
            }
        }
    ) {
        AboutContent(viewModel = viewModel, popupState = popupState)
    }
}
