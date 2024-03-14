package com.blockstream.compose.sheets

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import cafe.adriel.voyager.koin.getScreenModel
import com.arkivanov.essenty.parcelable.Parcelable
import com.blockstream.common.data.ScanResult
import com.blockstream.common.data.SetupArgs
import com.blockstream.common.models.camera.CameraViewModel
import com.blockstream.common.models.camera.CameraViewModelAbstract
import com.blockstream.common.models.camera.CameraViewModelPreview
import com.blockstream.common.sideeffects.SideEffects
import com.blockstream.compose.GreenPreview
import com.blockstream.compose.R
import com.blockstream.compose.components.BarcodeScanner
import com.blockstream.compose.components.GreenBottomSheet
import com.blockstream.compose.navigation.resultKey
import com.blockstream.compose.navigation.setNavigationResult
import kotlinx.parcelize.Parcelize
import org.koin.core.parameter.parametersOf


@Parcelize
data class CameraBottomSheet(
    val isDecodeContinuous: Boolean = false,
    val parentScreenName: String? = null,
    val setupArgs: SetupArgs? = null
) : BottomScreen(), Parcelable {
    @Composable
    override fun Content() {
        val viewModel = getScreenModel<CameraViewModel>() {
            parametersOf(isDecodeContinuous, parentScreenName, setupArgs)
        }
        CameraBottomSheet(viewModel = viewModel, onDismissRequest = onDismissRequest())
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraBottomSheet(
    viewModel: CameraViewModelAbstract,
    onDismissRequest: () -> Unit,
) {
    GreenBottomSheet(
        title = stringResource(id = R.string.id_scan_qr_code),
        viewModel = viewModel,
        sheetState = rememberModalBottomSheetState(
            skipPartiallyExpanded = true,
        ),
        sideEffectHandler = {
            if (it is SideEffects.Success) {
                (it.data as? ScanResult)?.also {
                    setNavigationResult(CameraBottomSheet::class.resultKey, it)
                    onDismissRequest()
                }
            }
        },
        onDismissRequest = onDismissRequest
    ) {
        BarcodeScanner(
            isDecodeContinuous = viewModel.isDecodeContinuous,
            viewModel = viewModel
        )
    }
}




@Composable
@Preview
fun CameraBottomSheetPreview() {
    GreenPreview {
        BarcodeScanner(isDecodeContinuous = false, viewModel = CameraViewModelPreview.preview())
    }
}