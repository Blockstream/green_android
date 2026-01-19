package com.blockstream.compose.models.recovery

import androidx.lifecycle.viewModelScope
import com.blockstream.compose.extensions.previewWallet
import com.blockstream.compose.models.GreenViewModel
import com.blockstream.compose.navigation.NavData
import com.blockstream.compose.sideeffects.SideEffects
import com.blockstream.data.data.GreenWallet
import com.blockstream.data.data.PopTo
import kotlinx.coroutines.launch

abstract class RecoverySuccessViewModelAbstract(greenWallet: GreenWallet) : GreenViewModel(greenWalletOrNull = greenWallet) {
    override fun screenName(): String = "RecoverySuccess"

    fun done() {
        postSideEffect(SideEffects.NavigateToRoot(popTo = PopTo.Root))
    }
}

class RecoverySuccessViewModel(greenWallet: GreenWallet) : RecoverySuccessViewModelAbstract(greenWallet = greenWallet) {

    init {
        viewModelScope.launch {
            _navData.value = NavData(onBackClicked = {
                postSideEffect(SideEffects.NavigateToRoot(popTo = PopTo.Root))
            })
        }

        bootstrap()
    }

}

class RecoverySuccessViewModelPreview(greenWallet: GreenWallet) : RecoverySuccessViewModelAbstract(greenWallet = greenWallet) {

    companion object {
        fun preview() = RecoverySuccessViewModelPreview(previewWallet())
    }
}