package com.blockstream.compose.screens.twofactor

import android.os.Parcelable
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.core.screen.Screen
import com.arkivanov.essenty.parcelable.Parcelize
import com.blockstream.common.data.GreenWallet
import com.blockstream.common.gdk.data.AccountAsset
import com.blockstream.common.models.GreenViewModel
import com.blockstream.common.models.twofactor.ReEnable2FAViewModel
import com.blockstream.common.models.twofactor.ReEnable2FAViewModelAbstract
import com.blockstream.common.models.twofactor.ReEnable2FAViewModelPreview
import com.blockstream.common.navigation.NavigateDestinations
import com.blockstream.compose.GreenPreview
import com.blockstream.compose.R
import com.blockstream.compose.components.GreenAccountAsset
import com.blockstream.compose.components.GreenButton
import com.blockstream.compose.components.GreenButtonType
import com.blockstream.compose.components.GreenColumn
import com.blockstream.compose.theme.bodyLarge
import com.blockstream.compose.utils.AppBar
import com.blockstream.compose.utils.HandleSideEffect
import cafe.adriel.voyager.koin.koinScreenModel
import org.koin.core.parameter.parametersOf


@Parcelize
data class ReEnable2FAScreen(
    val greenWallet: GreenWallet,
) : Parcelable, Screen {
    @Composable
    override fun Content() {
        val viewModel = koinScreenModel<ReEnable2FAViewModel>(){
            parametersOf(greenWallet)
        }

        val navData by viewModel.navData.collectAsStateWithLifecycle()

        AppBar(navData)

        ReEnable2FAScreen(viewModel = viewModel)
    }
}

@Composable
fun ReEnable2FAScreen(
    viewModel: ReEnable2FAViewModelAbstract
) {

    HandleSideEffect(viewModel = viewModel)

    val accounts by viewModel.accounts.collectAsStateWithLifecycle()

    GreenColumn(padding = 0, space = 0) {

        GreenColumn(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(R.drawable.re_enable_two_factor),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            )

            Text(
                modifier = Modifier.padding(horizontal = 24.dp),
                text = stringResource(R.string.id_some_coins_in_your_wallet),
                style = bodyLarge,
                textAlign = TextAlign.Center
            )
        }

        GreenColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            for (account in accounts) {
                GreenAccountAsset(
                    accountAssetBalance = account.accountAssetBalance,
                    message = stringResource(R.string.id_redeposit_expired_2fa_coins),
                    withAsset = false,
                    withArrow = true,
                    session = viewModel.sessionOrNull,
                    onClick = {
                        viewModel.postEvent(ReEnable2FAViewModel.LocalEvents.SelectAccount(account))
                    }
                )
            }

            GreenButton(
                text = stringResource(R.string.id_learn_more),
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