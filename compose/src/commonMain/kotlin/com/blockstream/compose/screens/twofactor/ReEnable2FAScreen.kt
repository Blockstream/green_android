package com.blockstream.compose.screens.twofactor

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.id_2fa_protected_accounts_are_2of2_wallets
import blockstream_green.common.generated.resources.id_learn_more
import blockstream_green.common.generated.resources.id_redeposit_expired_2fa_coins
import blockstream_green.common.generated.resources.re_enable_two_factor
import com.blockstream.common.models.twofactor.ReEnable2FAViewModel
import com.blockstream.common.models.twofactor.ReEnable2FAViewModelAbstract
import com.blockstream.common.models.twofactor.ReEnable2FAViewModelPreview
import com.blockstream.compose.GreenPreview
import com.blockstream.compose.components.GreenAccountAsset
import com.blockstream.compose.components.GreenButton
import com.blockstream.compose.components.GreenButtonType
import com.blockstream.compose.theme.bodyLarge
import com.blockstream.compose.utils.SetupScreen
import com.blockstream.ui.components.GreenColumn
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun ReEnable2FAScreen(
    viewModel: ReEnable2FAViewModelAbstract
) {
    val accounts by viewModel.accounts.collectAsStateWithLifecycle()

    SetupScreen(viewModel = viewModel) {
        GreenColumn(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                imageVector = vectorResource(Res.drawable.re_enable_two_factor),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            )

            Text(
                modifier = Modifier.padding(horizontal = 24.dp),
                text = stringResource(Res.string.id_2fa_protected_accounts_are_2of2_wallets),
                style = bodyLarge,
                textAlign = TextAlign.Center
            )
        }

        GreenColumn(
            padding = 0,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            for (account in accounts) {
                GreenAccountAsset(
                    accountAssetBalance = account.accountAssetBalance,
                    message = stringResource(Res.string.id_redeposit_expired_2fa_coins),
                    withAsset = false,
                    withArrow = true,
                    session = viewModel.sessionOrNull,
                    onClick = {
                        viewModel.postEvent(ReEnable2FAViewModel.LocalEvents.SelectAccount(account))
                    }
                )
            }

            GreenButton(
                text = stringResource(Res.string.id_learn_more),
                type = GreenButtonType.TEXT
            ) {
                viewModel.postEvent(ReEnable2FAViewModel.LocalEvents.LearnMore)
            }
        }
    }
}

@Composable
@Preview
fun ReEnable2FAScreenPreview() {
    GreenPreview {
        ReEnable2FAScreen(viewModel = ReEnable2FAViewModelPreview.preview())
    }
}