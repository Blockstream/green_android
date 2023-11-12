package com.blockstream.compose.screens.overview

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.getScreenModel
import com.arkivanov.essenty.parcelable.Parcelable
import com.arkivanov.essenty.parcelable.Parcelize
import com.blockstream.common.data.GreenWallet
import com.blockstream.common.data.LogoutReason
import com.blockstream.common.events.Events
import com.blockstream.common.models.login.LoginViewModel
import com.blockstream.common.models.overview.WalletOverviewViewModel
import com.blockstream.common.models.overview.WalletOverviewViewModelAbstract
import com.blockstream.compose.R
import com.blockstream.compose.components.GreenColumn
import com.blockstream.compose.components.MenuEntry
import com.blockstream.compose.screens.login.LoginScreenCallbacks
import com.blockstream.compose.sheets.BottomSheetNavigatorM3
import com.blockstream.compose.theme.GreenTheme
import com.blockstream.compose.utils.AppBar
import com.blockstream.compose.utils.AppBarData
import com.blockstream.compose.utils.HandleSideEffect
import org.koin.core.parameter.parametersOf


@Parcelize
data class WalletOverviewScreen(
    val greenWallet: GreenWallet,
) : Screen, Parcelable {
    @Composable
    override fun Content() {
        val viewModel = getScreenModel<WalletOverviewViewModel>() {
            parametersOf(greenWallet)
        }

        AppBar {
            AppBarData(title = viewModel.greenWallet.name, menu = listOfNotNull(MenuEntry(
                title = stringResource(R.string.id_logout),
                iconRes = R.drawable.sign_out
            ) {
                viewModel.postEvent(Events.Logout(reason = LogoutReason.USER_ACTION))
            }))
        }

        WalletOverviewScreen(viewModel = viewModel)
    }
}

@Composable
fun WalletOverviewScreen(
    viewModel: WalletOverviewViewModelAbstract
) {
    HandleSideEffect(viewModel = viewModel)

    GreenColumn {
        val balance by viewModel.balancePrimary.collectAsStateWithLifecycle()
        Text("Balance: $balance")

        val transactions by viewModel.transactions.collectAsStateWithLifecycle()
        
        Text(text = "TRANSACTIONS")
        transactions.forEach { 
            Text(text = it.txHash)
        }
    }
}

@Composable
@Preview
fun WalletOverviewPreview() {
    GreenTheme {
        BottomSheetNavigatorM3 {
//            WalletOverviewScreen(viewModel = WalletOverviewViewModelPreview.previewWithPin().also {
//                it.onProgress.value = false
//            })
        }
    }
}