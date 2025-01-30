package com.blockstream.compose.screens.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.id_enable_twofactor_authentication
import blockstream_green.common.generated.resources.id_tip_we_recommend_you_enable
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.koin.koinScreenModel
import com.blockstream.common.Parcelable
import com.blockstream.common.Parcelize
import com.blockstream.common.data.GreenWallet
import com.blockstream.common.gdk.data.Network
import com.blockstream.common.models.settings.WalletSettingsSection
import com.blockstream.common.models.settings.WalletSettingsViewModel
import com.blockstream.common.models.settings.WalletSettingsViewModelAbstract
import com.blockstream.ui.components.GreenColumn
import com.blockstream.compose.theme.bodyMedium
import com.blockstream.compose.theme.titleMedium
import com.blockstream.compose.theme.whiteMedium
import com.blockstream.compose.utils.HandleSideEffect
import org.jetbrains.compose.resources.stringResource
import org.koin.core.parameter.parametersOf

@Parcelize
data class NetworkTwoFactorAuthenticationScreen(
    val greenWallet: GreenWallet,
    val network: Network
) : Screen, Parcelable {

    override val key: ScreenKey
        get() = "${super.key}:${network.id.hashCode()}"

    @Composable
    override fun Content() {
        val viewModel = koinScreenModel<WalletSettingsViewModel> {
            parametersOf(greenWallet, network, WalletSettingsSection.TwoFactor)
        }

        NetworkTwoFactorAuthenticationScreen(viewModel = viewModel, network = network)
    }
}

@Composable
fun NetworkTwoFactorAuthenticationScreen(
    viewModel: WalletSettingsViewModelAbstract,
    network: Network
) {

    HandleSideEffect(viewModel)

    Column {
        GreenColumn {
            Text(
                text = stringResource(Res.string.id_enable_twofactor_authentication),
                style = titleMedium,
            )

            Text(
                text = stringResource(Res.string.id_tip_we_recommend_you_enable),
                style = bodyMedium,
                color = whiteMedium,
            )
        }

        Text(viewModel.screenName())
    }
}
