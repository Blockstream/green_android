package com.blockstream.compose.screens.add

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.id_add_new_account
import blockstream_green.common.generated.resources.id_hardware_wallet
import blockstream_green.common.generated.resources.id_network
import blockstream_green.common.generated.resources.id_recovery_key_type
import blockstream_green.common.generated.resources.id_recovery_phrase
import blockstream_green.common.generated.resources.id_review_account_information
import blockstream_green.common.generated.resources.id_xpub
import com.blockstream.common.events.Events
import com.blockstream.common.extensions.isNotBlank
import com.blockstream.common.models.add.ReviewAddAccountViewModelAbstract
import com.blockstream.common.navigation.NavigateDestinations
import com.blockstream.common.sideeffects.SideEffects
import com.blockstream.compose.components.GreenButton
import com.blockstream.compose.components.ScreenContainer
import com.blockstream.compose.theme.MonospaceFont
import com.blockstream.compose.theme.displayMedium
import com.blockstream.compose.theme.labelLarge
import com.blockstream.compose.theme.titleMedium
import com.blockstream.compose.theme.whiteMedium
import com.blockstream.compose.utils.HandleSideEffect
import com.blockstream.ui.components.GreenColumn
import com.blockstream.ui.navigation.setResult
import org.jetbrains.compose.resources.stringResource

@Composable
fun ReviewAddAccountScreen(
    viewModel: ReviewAddAccountViewModelAbstract
) {

    HandleSideEffect(viewModel) {
        when (it) {
            is SideEffects.AccountCreated -> {
                NavigateDestinations.ReviewAddAccount.setResult(it.accountAsset)
            }
        }
    }

    ScreenContainer(viewModel = viewModel, withPadding = false) {
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

                Text(
                    stringResource(Res.string.id_review_account_information),
                    style = displayMedium
                )

                HorizontalDivider()

                Column {
                    Text(
                        stringResource(Res.string.id_network),
                        color = whiteMedium,
                        style = labelLarge
                    )
                    Text(viewModel.setupArgs.network?.canonicalName ?: "", style = titleMedium)
                }

                Column {
                    Text(
                        stringResource(Res.string.id_recovery_key_type),
                        color = whiteMedium,
                        style = labelLarge
                    )
                    when {
                        viewModel.setupArgs.mnemonic.isNotBlank() -> Res.string.id_recovery_phrase
                        viewModel.setupArgs.xpub.isNotBlank() -> Res.string.id_xpub
                        else -> Res.string.id_hardware_wallet
                    }.also {
                        Text(stringResource(it), style = titleMedium)
                    }
                }

                viewModel.setupArgs.xpub?.also {
                    Column {
                        Text(
                            stringResource(Res.string.id_xpub),
                            color = whiteMedium,
                            style = labelLarge
                        )
                        Text(it, style = titleMedium, fontFamily = MonospaceFont())
                    }
                }
            }


            val buttonEnabled by viewModel.buttonEnabled.collectAsStateWithLifecycle()
            GreenButton(
                text = stringResource(Res.string.id_add_new_account),
                enabled = buttonEnabled,
                modifier = Modifier.fillMaxWidth(),
            ) {
                viewModel.postEvent(Events.Continue)
            }
        }
    }
}
