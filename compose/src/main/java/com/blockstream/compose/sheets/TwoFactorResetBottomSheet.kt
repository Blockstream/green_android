package com.blockstream.compose.sheets

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.koin.getScreenModel
import com.arkivanov.essenty.parcelable.Parcelable
import com.arkivanov.essenty.parcelable.Parcelize
import com.blockstream.common.data.GreenWallet
import com.blockstream.common.data.TwoFactorMethod
import com.blockstream.common.data.TwoFactorSetupAction
import com.blockstream.common.events.Events
import com.blockstream.common.extensions.previewNetwork
import com.blockstream.common.gdk.data.Network
import com.blockstream.common.gdk.data.Transaction
import com.blockstream.common.gdk.data.TwoFactorReset
import com.blockstream.common.models.GreenViewModel
import com.blockstream.common.models.SimpleGreenViewModel
import com.blockstream.common.models.SimpleGreenViewModelPreview
import com.blockstream.common.models.sheets.TransactionDetailsViewModel
import com.blockstream.common.models.sheets.TransactionDetailsViewModelAbstract
import com.blockstream.common.models.sheets.TransactionDetailsViewModelPreview
import com.blockstream.common.navigation.NavigateDestinations
import com.blockstream.compose.GreenPreview
import com.blockstream.compose.R
import com.blockstream.compose.components.GreenBottomSheet
import com.blockstream.compose.components.GreenButton
import com.blockstream.compose.components.GreenColumn
import com.blockstream.compose.components.GreenRow
import com.blockstream.compose.extensions.onValueChange
import com.blockstream.compose.theme.bodyLarge
import com.blockstream.compose.theme.bodyMedium
import com.blockstream.compose.theme.bodySmall
import com.blockstream.compose.theme.labelLarge
import com.blockstream.compose.theme.labelMedium
import com.blockstream.compose.theme.titleSmall
import com.blockstream.compose.theme.whiteHigh
import com.blockstream.compose.theme.whiteMedium
import com.blockstream.compose.utils.copyToClipboard
import com.blockstream.compose.utils.noRippleClickable
import com.blockstream.compose.utils.stringResourceId
import org.koin.core.parameter.parametersOf

@Parcelize
data class TwoFactorResetBottomSheet(
    val greenWallet: GreenWallet,
    val network: Network,
    val twoFactorReset: TwoFactorReset,
) : BottomScreen(), Parcelable {
    @Composable
    override fun Content() {
        val viewModel = getScreenModel<SimpleGreenViewModel> {
            parametersOf(greenWallet, null, "TwoFactorReset")
        }

        TwoFactorResetBottomSheet(
            viewModel = viewModel,
            network = network,
            twoFactorReset = twoFactorReset,
            onDismissRequest = onDismissRequest()
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TwoFactorResetBottomSheet(
    viewModel: GreenViewModel,
    network: Network,
    twoFactorReset: TwoFactorReset,
    onDismissRequest: () -> Unit,
) {

    GreenBottomSheet(
        title = stringResource(if (twoFactorReset.isDisputed == true) R.string.id_2fa_dispute_in_progress else R.string.id_2fa_reset_in_progress),
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        viewModel = viewModel,
        onDismissRequest = onDismissRequest
    ) {

        GreenColumn(
            padding = 0,
            space = 24,
            modifier = Modifier
                .padding(top = 16.dp)
                .verticalScroll(
                    rememberScrollState()
                )
        ) {

            if (twoFactorReset.isDisputed == true) {

                GreenColumn(padding = 0, space = 8) {
                    Text(
                        stringResource(
                            R.string.id_your_wallet_is_locked_under_2fa
                        ), style = labelLarge
                    )
                    Text(
                        stringResource(R.string.id_the_1_year_2fa_reset_process),
                        style = bodyMedium
                    )
                }

                GreenColumn(padding = 0, space = 8) {
                    Text(
                        stringResource(R.string.id_if_you_are_the_rightful_owner),
                        style = bodyMedium
                    )

                    GreenButton(
                        text = stringResource(R.string.id_cancel_2fa_reset),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        viewModel.postEvent(
                            NavigateDestinations.TwoFactorSetup(
                                method = TwoFactorMethod.EMAIL,
                                action = TwoFactorSetupAction.CANCEL,
                                network = network
                            )
                        )
                        onDismissRequest()
                    }
                }

                GreenColumn(padding = 0, space = 8) {

                    Text(
                        stringResource(R.string.id_if_you_initiated_the_2fa_reset),
                        style = bodyMedium
                    )

                    GreenButton(
                        text = stringResource(R.string.id_undo_2fa_dispute),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        viewModel.postEvent(
                            NavigateDestinations.TwoFactorSetup(
                                method = TwoFactorMethod.EMAIL,
                                action = TwoFactorSetupAction.UNDO_DISPUTE,
                                network = network
                            )
                        )
                        onDismissRequest()
                    }
                }

            } else {
                GreenColumn(padding = 0, space = 8) {
                    Text(
                        stringResource(
                            R.string.id_your_wallet_is_locked_for_a,
                            twoFactorReset.daysRemaining
                        ), style = labelLarge
                    )
                    Text(
                        stringResource(R.string.id_the_waiting_period_is_necessary),
                        style = bodyMedium
                    )
                }

                GreenColumn(padding = 0, space = 8) {
                    Text(
                        stringResource(
                            R.string.id_how_to_stop_this_reset,
                        ), style = labelLarge
                    )
                    Text(
                        stringResource(R.string.id_if_you_have_access_to_a),
                        style = bodyMedium
                    )

                    GreenButton(
                        text = stringResource(R.string.id_cancel_2fa_reset),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        viewModel.postEvent(
                            NavigateDestinations.TwoFactorSetup(
                                method = TwoFactorMethod.EMAIL,
                                action = TwoFactorSetupAction.CANCEL,
                                network = network
                            )
                        )
                        onDismissRequest()
                    }
                }

                GreenColumn(padding = 0, space = 8) {
                    Text(
                        stringResource(
                            R.string.id_permanently_block_this_wallet,
                        ), style = labelLarge
                    )
                    Text(
                        stringResource(R.string.id_if_you_did_not_request_the),
                        style = bodyMedium
                    )

                    GreenButton(
                        text = stringResource(R.string.id_dispute_twofactor_reset),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        viewModel.postEvent(
                            NavigateDestinations.TwoFactorSetup(
                                method = TwoFactorMethod.EMAIL,
                                action = TwoFactorSetupAction.DISPUTE,
                                network = network
                            )
                        )
                        onDismissRequest()
                    }
                }
            }
        }
    }
}

@Composable
@Preview
fun TwoFactorResetBottomSheetPreview() {
    GreenPreview {
        TwoFactorResetBottomSheet(
            viewModel = SimpleGreenViewModelPreview(),
            network = previewNetwork(),
            twoFactorReset = TwoFactorReset(),
            onDismissRequest = { }
        )
    }
}