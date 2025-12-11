package com.blockstream.compose.screens.add

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.id_existing_recovery_phrase
import blockstream_green.common.generated.resources.id_generate_a_new_recovery_phrase
import blockstream_green.common.generated.resources.id_new_recovery_phrase
import blockstream_green.common.generated.resources.id_select_your_recovery_key
import blockstream_green.common.generated.resources.id_use_a_public_key
import blockstream_green.common.generated.resources.id_use_an_existing_recovery_phrase
import blockstream_green.common.generated.resources.id_use_an_xpub_for_which_you_own
import com.blockstream.common.models.add.Account2of3ViewModel
import com.blockstream.common.models.add.Account2of3ViewModelAbstract
import com.blockstream.compose.components.GreenContentCard
import com.blockstream.compose.theme.displayMedium
import com.blockstream.compose.utils.SetupScreen
import com.blockstream.compose.components.GreenColumn
import org.jetbrains.compose.resources.stringResource

@Composable
fun Account2of3Screen(
    viewModel: Account2of3ViewModelAbstract
) {

    SetupScreen(viewModel = viewModel, withPadding = false) {

        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            GreenColumn(
                padding = 0,
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            ) {

                Text(stringResource(Res.string.id_select_your_recovery_key), style = displayMedium)

//            GreenContentCard(
//                title = stringResource(Res.string.id_hardware_wallet),
//                message = stringResource(Res.string.id_use_a_hardware_wallet_as_your),
//            ) {
//
//            }

                GreenContentCard(
                    title = stringResource(Res.string.id_new_recovery_phrase),
                    message = stringResource(Res.string.id_generate_a_new_recovery_phrase),
                ) {
                    viewModel.postEvent(Account2of3ViewModel.LocalEvents.NewRecovery)
                }

                GreenContentCard(
                    title = stringResource(Res.string.id_existing_recovery_phrase),
                    message = stringResource(Res.string.id_use_an_existing_recovery_phrase),
                ) {
                    viewModel.postEvent(Account2of3ViewModel.LocalEvents.ExistingRecovery)
                }

                GreenContentCard(
                    title = stringResource(Res.string.id_use_a_public_key),
                    message = stringResource(Res.string.id_use_an_xpub_for_which_you_own),
                ) {
                    viewModel.postEvent(Account2of3ViewModel.LocalEvents.Xpub)
                }
            }
        }
    }
}
