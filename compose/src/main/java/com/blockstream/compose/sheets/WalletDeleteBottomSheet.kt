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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import cafe.adriel.voyager.koin.getScreenModel
import com.blockstream.common.data.GreenWallet
import com.blockstream.common.events.Events
import com.blockstream.common.models.wallet.WalletDeleteViewModel
import com.blockstream.common.models.wallet.WalletDeleteViewModelAbstract
import com.blockstream.common.models.wallet.WalletDeleteViewModelPreview
import com.blockstream.compose.R
import com.blockstream.compose.components.GreenButton
import com.blockstream.compose.components.GreenButtonColor
import com.blockstream.compose.components.GreenButtonType
import com.blockstream.compose.components.GreenColumn
import com.blockstream.compose.theme.GreenTheme
import com.blockstream.compose.theme.labelLarge
import com.blockstream.compose.views.GreenBottomSheet
import kotlinx.parcelize.Parcelize
import org.koin.core.parameter.parametersOf

@OptIn(ExperimentalMaterial3Api::class)
@Parcelize
data class WalletDeleteBottomSheet(val greenWallet: GreenWallet) : BottomScreen() {
    @Composable
    override fun Content() {
        val viewModel = getScreenModel<WalletDeleteViewModel>{
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
        title = stringResource(id = R.string.id_remove_wallet),
        subtitle = viewModel.greenWallet.name,
        viewModel = viewModel,
        sheetState = sheetState,
        onDismissRequest = onDismissRequest) {

        Text(text = stringResource(id = R.string.id_do_you_have_the_backup), style = labelLarge)
        Text(text = stringResource(id = R.string.id_be_sure_your_recovery_phrase_is))

        var isConfirmed by remember {
            mutableStateOf(false)
        }

        if(isConfirmed){
            GreenButton(
                text = stringResource(id = R.string.id_remove_wallet),
                modifier = Modifier.fillMaxWidth(),
                color = GreenButtonColor.RED,
            ) {
                viewModel.postEvent(Events.Continue)
            }
        }else{
            GreenButton(
                text = stringResource(id = R.string.id_remove_wallet),
                modifier = Modifier.fillMaxWidth(),
                type = GreenButtonType.OUTLINE,
                color = GreenButtonColor.RED,
            ) {
                isConfirmed = true
            }
        }
    }
}

@ExperimentalMaterial3Api
@Composable
@Preview
fun WalletDeleteBottomSheetPreview() {
    GreenTheme {
        GreenColumn {
            var showBottomSheet by remember { mutableStateOf(true) }

            GreenButton(text = "Show BottomSheet") {
                showBottomSheet = true
            }

            Text("WalletRenameBottomSheet")

            if(showBottomSheet) {
                WalletDeleteBottomSheet(
                    viewModel = WalletDeleteViewModelPreview.preview(),
                    onDismissRequest = {
                        showBottomSheet = false
                    }
                )
            }
        }
    }
}