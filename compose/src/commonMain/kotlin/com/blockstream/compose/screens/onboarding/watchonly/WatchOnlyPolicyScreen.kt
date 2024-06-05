package com.blockstream.compose.screens.onboarding.watchonly

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.id_choose_the_security_policy_that
import blockstream_green.common.generated.resources.id_enter_your_xpub_to_add_a
import blockstream_green.common.generated.resources.id_log_in_to_your_multisig_shield
import blockstream_green.common.generated.resources.id_multisig_shield
import blockstream_green.common.generated.resources.id_select_watchonly_type
import blockstream_green.common.generated.resources.id_singlesig
import blockstream_green.common.generated.resources.key_multisig
import blockstream_green.common.generated.resources.key_singlesig
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import com.blockstream.common.models.onboarding.watchonly.WatchOnlyPolicyViewModel
import com.blockstream.common.models.onboarding.watchonly.WatchOnlyPolicyViewModelAbstract
import com.blockstream.compose.components.GreenColumn
import com.blockstream.compose.components.GreenContentCard
import com.blockstream.compose.theme.bodyLarge
import com.blockstream.compose.theme.displayMedium
import com.blockstream.compose.utils.AppBar
import com.blockstream.compose.utils.HandleSideEffect
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource


object WatchOnlyPolicyScreen : Screen {
    @Composable
    override fun Content() {
        val viewModel = koinScreenModel<WatchOnlyPolicyViewModel>()

        val navData by viewModel.navData.collectAsStateWithLifecycle()

        AppBar(navData)

        WatchOnlyPolicyScreen(viewModel = viewModel)
    }
}

@Composable
fun WatchOnlyPolicyScreen(
    viewModel: WatchOnlyPolicyViewModelAbstract
) {

    HandleSideEffect(viewModel = viewModel)

    Column(
        modifier = Modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        Text(
            text = stringResource(Res.string.id_select_watchonly_type),
            style = displayMedium,
        )

        Text(
            text = stringResource(Res.string.id_choose_the_security_policy_that),
            style = bodyLarge,
        )
        GreenColumn(
            padding = 0,
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {

            GreenContentCard(
                title = stringResource(Res.string.id_singlesig),
                message = stringResource(Res.string.id_enter_your_xpub_to_add_a),
                painter = painterResource(Res.drawable.key_singlesig)
            ) {
                viewModel.postEvent(WatchOnlyPolicyViewModel.LocalEvents.SelectPolicy(isSinglesig = true))
            }

            GreenContentCard(
                title = stringResource(Res.string.id_multisig_shield),
                message = stringResource(Res.string.id_log_in_to_your_multisig_shield),
                painter = painterResource(Res.drawable.key_multisig)
            ) {
                viewModel.postEvent(WatchOnlyPolicyViewModel.LocalEvents.SelectPolicy(isSinglesig = false))
            }
        }
    }
}