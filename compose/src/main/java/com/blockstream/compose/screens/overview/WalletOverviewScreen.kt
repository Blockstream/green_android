package com.blockstream.compose.screens.overview

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.getScreenModel
import com.arkivanov.essenty.parcelable.Parcelable
import com.arkivanov.essenty.parcelable.Parcelize
import com.blockstream.common.data.GreenWallet
import com.blockstream.common.models.overview.WalletOverviewViewModel
import com.blockstream.common.models.overview.WalletOverviewViewModelAbstract
import com.blockstream.compose.GreenPreview
import com.blockstream.compose.components.GreenColumn
import com.blockstream.compose.utils.AppBar
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

        val navData by viewModel.navData.collectAsStateWithLifecycle()

        AppBar(navData)

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
    GreenPreview {
//            WalletOverviewScreen(viewModel = WalletOverviewViewModelPreview.previewWithPin().also {
//                it.onProgress.value = false
//            })
    }
}