package com.blockstream.compose.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.id_change_pin
import blockstream_green.common.generated.resources.id_pins_do_not_match_please_try
import blockstream_green.common.generated.resources.id_verify_your_pin
import blockstream_green.common.generated.resources.id_youll_need_your_pin_to_log_in
import com.blockstream.common.models.settings.WalletSettingsViewModel
import com.blockstream.common.models.settings.WalletSettingsViewModelAbstract
import com.blockstream.compose.LocalSnackbar
import com.blockstream.compose.components.OnProgressStyle
import com.blockstream.compose.theme.bodyLarge
import com.blockstream.compose.theme.titleLarge
import com.blockstream.compose.utils.SetupScreen
import com.blockstream.compose.views.PinView
import com.blockstream.ui.components.GreenColumn
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource

@Composable
fun ChangePinScreen(
    viewModel: WalletSettingsViewModelAbstract
) {
    var isVerify by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    val snackbar = LocalSnackbar.current

    SetupScreen(
        viewModel = viewModel,
        withPadding = false,
        onProgressStyle = OnProgressStyle.Full()
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            GreenColumn(padding = 0, space = 8) {
                Text(
                    stringResource(if (isVerify) Res.string.id_verify_your_pin else Res.string.id_change_pin),
                    style = titleLarge
                )
                Text(
                    stringResource(Res.string.id_youll_need_your_pin_to_log_in),
                    style = bodyLarge
                )
            }

            PinView(modifier = Modifier.weight(1f), isVerifyMode = true, onPinNotVerified = {
                scope.launch {
                    snackbar.showSnackbar(getString(Res.string.id_pins_do_not_match_please_try))
                }
            }, onModeChange = {
                isVerify = it
            }) { pin ->
                if(isVerify && pin.length == 6) {
                    viewModel.postEvent(WalletSettingsViewModel.LocalEvents.SetPin(pin = pin))
                }
            }
        }
    }
}
