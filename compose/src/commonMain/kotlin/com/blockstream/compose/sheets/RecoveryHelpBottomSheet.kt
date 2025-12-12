package com.blockstream.compose.sheets

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.id_1_double_check_all_of_your
import blockstream_green.common.generated.resources.id_help
import blockstream_green.common.generated.resources.id_i_typed_all_my_recovery_phrase
import blockstream_green.common.generated.resources.id_visit_the_blockstream_help
import com.blockstream.compose.models.sheets.RecoveryHelpViewModel
import com.blockstream.compose.models.sheets.RecoveryHelpViewModelAbstract
import com.blockstream.compose.components.GreenBottomSheet
import com.blockstream.compose.components.GreenButton
import com.blockstream.compose.theme.bodyLarge
import com.blockstream.compose.theme.labelLarge
import com.blockstream.compose.utils.HandleSideEffect
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecoveryHelpBottomSheet(
    viewModel: RecoveryHelpViewModelAbstract,
    onDismissRequest: () -> Unit,
) {
    GreenBottomSheet(
        title = stringResource(Res.string.id_help),
        viewModel = viewModel,
        onDismissRequest = onDismissRequest
    ) {

        HandleSideEffect(viewModel = viewModel)

        Text(text = stringResource(Res.string.id_i_typed_all_my_recovery_phrase), style = labelLarge)

        Text(text = stringResource(Res.string.id_1_double_check_all_of_your), style = bodyLarge)

        GreenButton(
            text = stringResource(Res.string.id_visit_the_blockstream_help),
            modifier = Modifier.fillMaxWidth()
        ) {
            viewModel.postEvent(RecoveryHelpViewModel.LocalEvents.ClickHelp)
        }
    }
}
