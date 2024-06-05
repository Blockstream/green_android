package com.blockstream.compose.sheets

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.id_be_sure_your_recovery_phrase_is
import blockstream_green.common.generated.resources.id_do_you_have_the_backup
import blockstream_green.common.generated.resources.id_remove_wallet
import cafe.adriel.voyager.koin.koinScreenModel
import com.blockstream.common.Parcelable
import com.blockstream.common.Parcelize
import com.blockstream.common.data.GreenWallet
import com.blockstream.common.events.Events
import com.blockstream.common.models.wallet.WalletDeleteViewModel
import com.blockstream.common.models.wallet.WalletDeleteViewModelAbstract
import com.blockstream.compose.components.GreenBottomSheet
import com.blockstream.compose.components.GreenButton
import com.blockstream.compose.components.GreenButtonColor
import com.blockstream.compose.components.GreenButtonType
import com.blockstream.compose.theme.labelLarge
import org.jetbrains.compose.resources.stringResource
import org.koin.core.parameter.parametersOf

@OptIn(ExperimentalMaterial3Api::class)
@Parcelize
data class WalletDeleteBottomSheet(val greenWallet: GreenWallet) : BottomScreen(), Parcelable {
    @Composable
    override fun Content() {
        val viewModel = koinScreenModel<WalletDeleteViewModel>{
            parametersOf(greenWallet)
        }

        WalletDeleteBottomSheet(viewModel = viewModel, onDismissRequest = onDismissRequest())
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WalletDeleteBottomSheet(
    viewModel: WalletDeleteViewModelAbstract,
    sheetState: SheetState = rememberModalBottomSheetState(),
    onDismissRequest: () -> Unit,
) {
    GreenBottomSheet(
        title = stringResource(Res.string.id_remove_wallet),
        subtitle = viewModel.greenWallet.name,
        viewModel = viewModel,
        sheetState = sheetState,
        onDismissRequest = onDismissRequest) {

        Text(text = stringResource(Res.string.id_do_you_have_the_backup), style = labelLarge)
        Text(text = stringResource(Res.string.id_be_sure_your_recovery_phrase_is))

        var isConfirmed by remember {
            mutableStateOf(false)
        }

        if(isConfirmed){
            GreenButton(
                text = stringResource(Res.string.id_remove_wallet),
                modifier = Modifier.fillMaxWidth(),
                color = GreenButtonColor.RED,
            ) {
                viewModel.postEvent(Events.Continue)
            }
        }else{
            GreenButton(
                text = stringResource(Res.string.id_remove_wallet),
                modifier = Modifier.fillMaxWidth(),
                type = GreenButtonType.OUTLINE,
                color = GreenButtonColor.RED,
            ) {
                isConfirmed = true
            }
        }
    }
}
