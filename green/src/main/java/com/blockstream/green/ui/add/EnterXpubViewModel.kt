package com.blockstream.green.ui.add;

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asFlow
import com.blockstream.common.data.GreenWallet
import com.blockstream.common.gdk.Wally
import com.blockstream.green.ui.wallet.AbstractWalletViewModel
import com.rickclephas.kmm.viewmodel.coroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.withContext
import org.koin.android.annotation.KoinViewModel
import org.koin.core.annotation.InjectedParam

@KoinViewModel
class EnterXpubViewModel constructor(
    wally: Wally,
    @InjectedParam wallet: GreenWallet,
) : AbstractWalletViewModel(wallet) {

    val xpub = MutableLiveData<String>()
    val isXpubValid = MutableLiveData(false)

    init {
        xpub
            .asFlow()
            .onEach {
                isXpubValid.value = true
                isXpubValid.value = withContext(Dispatchers.IO){
                    wally.isXpubValid(it)
                }
            }
            .launchIn(viewModelScope.coroutineScope)
    }
}