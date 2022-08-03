package com.blockstream.green.ui.receive

import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import androidx.lifecycle.*
import androidx.savedstate.SavedStateRegistryOwner
import com.blockstream.gdk.data.AccountAsset
import com.blockstream.gdk.data.Address
import com.blockstream.green.data.Countly
import com.blockstream.green.database.Wallet
import com.blockstream.green.database.WalletRepository
import com.blockstream.green.managers.SessionManager
import com.blockstream.green.ui.wallet.AbstractAssetWalletViewModel
import com.blockstream.green.utils.ConsumableEvent
import com.blockstream.green.utils.createQrBitmap

import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class ReceiveViewModel @AssistedInject constructor(
    sessionManager: SessionManager,
    walletRepository: WalletRepository,
    countly: Countly,
    @Assisted private val savedStateHandle: SavedStateHandle,
    @Assisted wallet: Wallet,
    @Assisted initAccountAsset: AccountAsset,
) : AbstractAssetWalletViewModel(
    sessionManager,
    walletRepository,
    countly,
    wallet,
    initAccountAsset
) {
    var addressLiveData = MutableLiveData<Address>()
    val addressAsString: String get() = addressLiveData.value?.address ?: ""
    var addressUri = MutableLiveData<String>()

    val requestAmount = MutableLiveData<String?>()

    var addressQRBitmap = MutableLiveData<Bitmap?>()

    val isAddressUri = MutableLiveData(false)

    val deviceAddressValidationEvent = MutableLiveData<ConsumableEvent<Boolean?>>()

    // only show if we are on Liquid and we are using Ledger
    val showAssetWhitelistWarning = MutableLiveData(false)
    val canValidateAddressInDevice = MutableLiveData(false)

    val accountAssetLocked = MutableLiveData(false)

    init {
        accountAssetLiveData.asFlow()
            .distinctUntilChanged()
            .onEach {
                clearRequestAmount()
            }.launchIn(lifecycleScope)

        // Generate address when account & account type changes
        accountLiveData.asFlow().onEach {
            generateAddress()
        }.launchIn(lifecycleScope)
    }

    fun generateAddress() {
        logger.info { "Generating address for ${account.name}" }
        showAssetWhitelistWarning.value = account.isLiquid && session.device?.isLedger == true
        canValidateAddressInDevice.value = session.device?.let { device ->
            device.isJade ||
                    (device.isLedger && network.isLiquid && !network.isSinglesig) ||
                    (device.isLedger && !network.isLiquid && network.isSinglesig) ||
                    (device.isTrezor && !network.isLiquid && network.isSinglesig)
        } ?: false

        doUserAction({
            session.getReceiveAddress(account)
        }, onSuccess = {
            addressLiveData.value = it
            update()
        })
    }

    fun validateAddressInDevice() {
        countly.verifyAddress(session, account)

        addressLiveData.value?.let { address ->
            deviceAddressValidationEvent.value = ConsumableEvent(null)

            session.hwWallet?.let { hwWallet ->
                doUserAction({
                    hwWallet.getGreenAddress(
                        network,
                        null,
                        account,
                        address.userPath,
                        address.subType ?: 0
                    )
                }, preAction = null, postAction = null, timeout = 30, onSuccess = {
                    if (it == address.address) {
                        deviceAddressValidationEvent.value = ConsumableEvent(true)
                    } else {
                        deviceAddressValidationEvent.value = ConsumableEvent(false)
                    }
                })
            }
        }
    }

    private fun update() {
        updateAddressUri()
        updateQR()
    }

    private fun updateAddressUri() {
        if (requestAmount.value != null) {
            isAddressUri.value = true

            // Use 2 different builders, we are restricted by spec
            // https://stackoverflow.com/questions/8534899/is-it-possible-to-use-uri-builder-and-not-have-the-part

            val scheme = Uri.Builder().also {
                it.scheme(account.network.bip21Prefix)
                it.opaquePart(addressLiveData.value?.address)
            }.toString()

            val query = Uri.Builder().also {
                if (!requestAmount.value.isNullOrBlank()) {
                    it.appendQueryParameter("amount", requestAmount.value)
                }

                if (network.isLiquid) {
                    it.appendQueryParameter("assetid", accountAsset.assetId)
                }

            }.toString()

            addressUri.value = scheme + query
        } else {
            isAddressUri.value = false
            addressUri.value = addressLiveData.value?.address
        }
    }

    private fun updateQR() {
        addressQRBitmap.postValue(createQrBitmap(addressUri.value ?: ""))
    }

    fun setRequestAmount(amount: String?) {
        requestAmount.value = amount
        update()
    }

    fun clearRequestAmount() {
        setRequestAmount(null)
    }

    @dagger.assisted.AssistedFactory
    interface AssistedFactory {
        fun create(
            savedStateHandle: SavedStateHandle,
            wallet: Wallet,
            initAccountAsset: AccountAsset
        ): ReceiveViewModel
    }

    companion object {
        const val ADDRESS_TYPE = "ADDRESS_TYPE"

        fun provideFactory(
            assistedFactory: AssistedFactory,
            owner: SavedStateRegistryOwner,
            defaultArgs: Bundle? = null,
            wallet: Wallet,
            initAccountAsset: AccountAsset
        ): AbstractSavedStateViewModelFactory =
            object : AbstractSavedStateViewModelFactory(owner, defaultArgs) {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(
                    key: String,
                    modelClass: Class<T>,
                    handle: SavedStateHandle
                ): T {
                    return assistedFactory.create(
                        handle,
                        wallet,
                        initAccountAsset,
                    ) as T
                }
            }
    }
}
