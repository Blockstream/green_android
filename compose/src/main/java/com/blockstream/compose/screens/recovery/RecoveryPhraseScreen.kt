package com.blockstream.compose.screens.recovery

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.getScreenModel
import com.arkivanov.essenty.parcelable.Parcelable
import com.blockstream.common.data.GreenWallet
import com.blockstream.common.gdk.data.Credentials
import com.blockstream.common.models.recovery.RecoveryIntroViewModel
import com.blockstream.common.models.recovery.RecoveryPhraseViewModel
import com.blockstream.common.models.recovery.RecoveryPhraseViewModelAbstract
import com.blockstream.common.models.recovery.RecoveryPhraseViewModelPreview
import com.blockstream.common.utils.AndroidKeystore
import com.blockstream.compose.LocalSnackbar
import com.blockstream.compose.R
import com.blockstream.compose.components.GreenColumn
import com.blockstream.compose.sideeffects.BiometricsState
import com.blockstream.compose.sideeffects.DialogHost
import com.blockstream.compose.sideeffects.DialogState
import com.blockstream.compose.theme.GreenTheme
import com.blockstream.compose.utils.AppBar
import com.blockstream.compose.utils.AppBarData
import com.blockstream.compose.utils.HandleSideEffect
import kotlinx.parcelize.Parcelize
import org.koin.compose.koinInject
import org.koin.core.parameter.parametersOf


@Parcelize
data class RecoveryPhraseScreen(val isLightning: Boolean,
                                val providedCredentials: Credentials?,
                                val greenWallet: GreenWallet?) : Screen, Parcelable {
    @Composable
    override fun Content() {
        val viewModel = getScreenModel<RecoveryPhraseViewModel>() {
            parametersOf(isLightning, providedCredentials, greenWallet)
        }

        AppBar {
            AppBarData(
                title = stringResource(R.string.id_backup_recovery_phrase),
                subtitle = if (viewModel.isLightning) stringResource(R.string.id_lightning) else null
            )
        }

        RecoveryPhraseScreen(viewModel = viewModel)
    }
}
@Composable
fun RecoveryPhraseScreen(
    viewModel: RecoveryPhraseViewModelAbstract
) {
    HandleSideEffect(viewModel)

    GreenColumn {
        Text(text = viewModel.mnemonic.value)
        Text(text = viewModel.passphrase.value ?: "")
    }

}


@Composable
@Preview
fun RecoveryPhraseScreenPreview() {
    GreenTheme {

        RecoveryPhraseScreen(viewModel = RecoveryPhraseViewModelPreview.preview())
    }
}