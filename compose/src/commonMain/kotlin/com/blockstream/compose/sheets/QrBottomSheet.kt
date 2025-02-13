package com.blockstream.compose.sheets

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.blockstream.common.models.GreenViewModel
import com.blockstream.compose.components.GreenBottomSheet
import com.blockstream.compose.components.GreenQR


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QrBottomSheet(
    viewModel: GreenViewModel,
    title: String? = null,
    subtitle: String? = null,
    data: String,
    onDismissRequest: () -> Unit,
) {
    GreenBottomSheet(
        title = title,
        subtitle = subtitle,
        viewModel = viewModel,
        sheetState = rememberModalBottomSheetState(
            skipPartiallyExpanded = true
        ),
        onDismissRequest = onDismissRequest
    ) {
        GreenQR(data = data, modifier = Modifier.align(Alignment.CenterHorizontally))
    }
}