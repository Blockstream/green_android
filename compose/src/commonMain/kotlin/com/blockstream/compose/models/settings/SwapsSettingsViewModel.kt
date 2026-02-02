package com.blockstream.compose.models.settings

import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.id_swaps_disabled
import com.blockstream.compose.extensions.launchIn
import com.blockstream.compose.extensions.previewWallet
import com.blockstream.compose.models.GreenViewModel
import com.blockstream.compose.sideeffects.SideEffects
import com.blockstream.compose.utils.StringHolder
import com.blockstream.compose.utils.SwapUtils
import com.blockstream.data.data.CredentialType
import com.blockstream.data.data.GreenWallet
import com.blockstream.data.extensions.tryCatchNullSuspend
import com.blockstream.domain.swap.CanSwapsBeDisabledUseCase
import com.blockstream.domain.swap.IsSwapsEnabledUseCase
import com.blockstream.utils.Loggable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import org.koin.core.component.inject

data class SwapsSettingsUiState(
    val isEnabled: Boolean = false,
    val canBeDisabled: Boolean = true,
    val showConnectHardwareWallet: Boolean = false
)

abstract class SwapsSettingsViewModelAbstract(greenWallet: GreenWallet) :
    GreenViewModel(greenWalletOrNull = greenWallet) {
    override fun screenName(): String = "SwapsSettings"

    abstract val uiState: StateFlow<SwapsSettingsUiState>

    fun enable() {
        SwapUtils.navigateToDeviceScanOrJadeQr(this)
    }

    fun disable() {
        doAsync({
            database.deleteLoginCredentials(walletId = greenWallet.id, type = CredentialType.KEYSTORE_BOLTZ_MNEMONIC)
        }, onSuccess = {
            postSideEffect(SideEffects.Snackbar(StringHolder.create(Res.string.id_swaps_disabled)))
        })
    }
}

class SwapsSettingsViewModel(greenWallet: GreenWallet) :
    SwapsSettingsViewModelAbstract(greenWallet = greenWallet) {

    private val isSwapsEnabledUseCase: IsSwapsEnabledUseCase by inject()
    private val canSwapsBeDisabledUseCase: CanSwapsBeDisabledUseCase by inject()

    final override val uiState: StateFlow<SwapsSettingsUiState>
        field = MutableStateFlow(SwapsSettingsUiState())

    init {

        database.getLoginCredentialsFlow(greenWallet.id).onEach {
            uiState.update { uiState ->
                val isEnabled = tryCatchNullSuspend { isSwapsEnabledUseCase(greenWallet) } ?: false
                val canBeDisabled = tryCatchNullSuspend { canSwapsBeDisabledUseCase(greenWallet) } ?: true
                uiState.copy(
                    isEnabled = isEnabled,
                    canBeDisabled = canBeDisabled
                )
            }
        }.launchIn(this)

        session.isWatchOnly.onEach {
            uiState.update { uiState ->
                uiState.copy(
                    showConnectHardwareWallet = it
                )
            }
        }.launchIn(this)

        bootstrap()
    }

    companion object : Loggable()
}

class SwapsSettingsViewModelPreview(isEnabled: Boolean, greenWallet: GreenWallet) :
    SwapsSettingsViewModelAbstract(greenWallet = greenWallet) {
    override val uiState: StateFlow<SwapsSettingsUiState> =
        MutableStateFlow(SwapsSettingsUiState(isEnabled = isEnabled, canBeDisabled = false))

    companion object {
        fun preview(isEnabled: Boolean = true) = SwapsSettingsViewModelPreview(isEnabled = isEnabled, previewWallet())
    }

}