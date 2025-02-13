package com.blockstream.compose.sheets

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.id_scan_qr_code
import com.blockstream.common.data.ScanResult
import com.blockstream.common.models.camera.CameraViewModelAbstract
import com.blockstream.common.navigation.NavigateDestinations
import com.blockstream.common.sideeffects.SideEffects
import com.blockstream.compose.components.GreenBottomSheet
import com.blockstream.compose.components.GreenScanner
import com.blockstream.ui.navigation.setResult
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraBottomSheet(
    viewModel: CameraViewModelAbstract,
    onDismissRequest: () -> Unit,
) {
    GreenBottomSheet(
        title = stringResource(Res.string.id_scan_qr_code),
        viewModel = viewModel,
        sheetState = rememberModalBottomSheetState(
            skipPartiallyExpanded = true,
        ),
        sideEffectHandler = {
            if (it is SideEffects.Success) {
                (it.data as? ScanResult)?.also {
                    NavigateDestinations.Camera.setResult(it)
                    onDismissRequest()
                }
            }
        },
        onDismissRequest = onDismissRequest
    ) {
        GreenScanner(
            isDecodeContinuous = viewModel.isDecodeContinuous,
            viewModel = viewModel
        )
    }
}