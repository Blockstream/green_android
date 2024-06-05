package com.blockstream.compose.sheets

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.id_scan_qr_code
import cafe.adriel.voyager.koin.koinScreenModel
import com.blockstream.common.Parcelable
import com.blockstream.common.Parcelize
import com.blockstream.common.data.ScanResult
import com.blockstream.common.data.SetupArgs
import com.blockstream.common.models.camera.CameraViewModel
import com.blockstream.common.models.camera.CameraViewModelAbstract
import com.blockstream.common.sideeffects.SideEffects
import com.blockstream.compose.components.GreenBottomSheet
import com.blockstream.compose.components.GreenScanner
import com.blockstream.compose.navigation.getNavigationResult
import com.blockstream.compose.navigation.setNavigationResult
import org.jetbrains.compose.resources.stringResource
import org.koin.core.parameter.parametersOf


@Parcelize
data class CameraBottomSheet(
    val isDecodeContinuous: Boolean = false,
    val parentScreenName: String? = null,
    val setupArgs: SetupArgs? = null
) : BottomScreen(), Parcelable {
    @Composable
    override fun Content() {
        val viewModel = koinScreenModel<CameraViewModel>() {
            parametersOf(isDecodeContinuous, parentScreenName, setupArgs)
        }
        CameraBottomSheet(viewModel = viewModel, onDismissRequest = onDismissRequest())
    }

    companion object {
        @Composable
        fun getResult(fn: (ScanResult) -> Unit) =
            getNavigationResult(this::class, fn)

        internal fun setResult(result: ScanResult) =
            setNavigationResult(this::class, result)
    }
}

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
                    CameraBottomSheet.setResult(it)
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