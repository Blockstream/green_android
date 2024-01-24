package com.blockstream.compose.sheets

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import cafe.adriel.voyager.koin.getScreenModel
import com.blockstream.common.models.sheets.RecoveryHelpViewModel
import com.blockstream.common.models.sheets.RecoveryHelpViewModelAbstract
import com.blockstream.common.models.sheets.RecoveryHelpViewModelPreview
import com.blockstream.compose.GreenPreview
import com.blockstream.compose.R
import com.blockstream.compose.components.GreenButton
import com.blockstream.compose.components.GreenColumn
import com.blockstream.compose.theme.bodyLarge
import com.blockstream.compose.theme.labelLarge
import com.blockstream.compose.utils.HandleSideEffect
import com.blockstream.compose.views.GreenBottomSheet


object RecoveryHelpBottomSheet : BottomScreen() {
    @Composable
    override fun Content() {
        val viewModel = getScreenModel<RecoveryHelpViewModel>()
        RecoveryHelpBottomSheet(viewModel = viewModel, onDismissRequest = onDismissRequest())
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RecoveryHelpBottomSheet(
    viewModel: RecoveryHelpViewModelAbstract,
    onDismissRequest: () -> Unit,
) {
    GreenBottomSheet(
        title = stringResource(id = R.string.id_help),
        viewModel = viewModel,
        onDismissRequest = onDismissRequest
    ) {

        HandleSideEffect(viewModel = viewModel)

        Text(text = stringResource(R.string.id_i_typed_all_my_recovery_phrase), style = labelLarge)

        Text(text = stringResource(R.string.id_1_double_check_all_of_your), style = bodyLarge)

        GreenButton(
            text = stringResource(id = R.string.id_visit_the_blockstream_help),
            modifier = Modifier.fillMaxWidth()
        ) {
            viewModel.postEvent(RecoveryHelpViewModel.LocalEvents.ClickHelp)
        }
    }
}

@Composable
@Preview
fun RecoveryHelpBottomSheetPreview() {
    GreenPreview {
        GreenColumn {
            RecoveryHelpBottomSheet(viewModel = RecoveryHelpViewModelPreview.preview(), {

            })
        }
    }
}