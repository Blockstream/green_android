package com.blockstream.green.ui.add

import androidx.lifecycle.*
import com.blockstream.gdk.BTC_POLICY_ASSET
import com.blockstream.gdk.data.AccountType
import com.blockstream.gdk.data.Network
import com.blockstream.green.data.Countly
import com.blockstream.green.database.Wallet
import com.blockstream.green.database.WalletRepository
import com.blockstream.green.gdk.hasHistory
import com.blockstream.green.managers.SessionManager
import com.blockstream.green.ui.items.AccountTypeListItem
import com.mikepenz.fastadapter.GenericItem
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class ChooseAccountTypeViewModel @AssistedInject constructor(
    sessionManager: SessionManager,
    walletRepository: WalletRepository,
    countly: Countly,
    @Assisted wallet: Wallet,
    @Assisted initAssetId: String?
) : AbstractAddAccountViewModel(sessionManager, walletRepository, countly, wallet) {

    private var _assetIdLiveData = MutableLiveData<String>()
    val assetIdLiveData: LiveData<String>
        get() = _assetIdLiveData

    var assetId
        get() = _assetIdLiveData.value!!
        set(value) {
            _assetIdLiveData.value = value
            showAdvancedLiveData.value = false
        }

    var showAdvancedLiveData = MutableLiveData(false)
    var showAdvanced
        get() = showAdvancedLiveData.value!!
        set(value) {
            showAdvancedLiveData.value = value
        }

    private var _accountTypesLiveData = MutableLiveData<List<GenericItem>>()
    val accountTypesLiveData: LiveData<List<GenericItem>>
        get() = _accountTypesLiveData

    val accountTypes
        get() = _accountTypesLiveData.value!!

    init {
        _assetIdLiveData.value = initAssetId ?: (session.bitcoin ?: session.liquid ?: session.defaultNetwork).policyAsset

        combine(
            assetIdLiveData.asFlow(),
            showAdvancedLiveData.asFlow()
        ) { _, _ ->
            Unit
        }.onEach {
            val list = mutableListOf<GenericItem>()

            val isAmp = session.enrichedAssets[assetId]?.isAmp ?: false
            val isBitcoin = assetId == BTC_POLICY_ASSET

            // Check if singlesig networks are available in this session
            if (!isAmp && ((isBitcoin && session.bitcoinSinglesig != null) || (!isBitcoin && session.liquidSinglesig != null && !wallet.isHardware))) {
                list += AccountTypeListItem(AccountType.BIP49_SEGWIT_WRAPPED)

                if (showAdvanced) {
                    list += AccountTypeListItem(AccountType.BIP84_SEGWIT)
                }
            }

            // Check if multisig networks are available in this session
            if (!isAmp && ((isBitcoin && session.bitcoinMultisig != null) || (!isBitcoin && session.bitcoinMultisig != null))) {
                list += AccountTypeListItem(AccountType.STANDARD)

                if (showAdvanced) {
                    list += if (isBitcoin) {
                        AccountTypeListItem(AccountType.TWO_OF_THREE)
                    } else {
                        AccountTypeListItem(AccountType.AMP_ACCOUNT)
                    }
                }
            }

            if (isAmp) {
                list += AccountTypeListItem(AccountType.AMP_ACCOUNT)
            }

            _accountTypesLiveData.value = list

        }.launchIn(viewModelScope)
    }

    fun isAccountAlreadyArchived(network: Network, accountType: AccountType): Boolean {
        return session.allAccountsFlow.value.find { it.hidden && it.network == network && it.type == accountType && it.hasHistory(session)} != null
    }

    @dagger.assisted.AssistedFactory
    interface AssistedFactory {
        fun create(
            wallet: Wallet,
            initAssetId: String?,
        ): ChooseAccountTypeViewModel
    }

    companion object {
        fun provideFactory(
            assistedFactory: AssistedFactory,
            wallet: Wallet,
            initAssetId: String?
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return assistedFactory.create(wallet, initAssetId) as T
            }
        }
    }
}