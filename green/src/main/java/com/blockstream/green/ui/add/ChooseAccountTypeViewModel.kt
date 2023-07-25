package com.blockstream.green.ui.add

import androidx.lifecycle.*
import com.blockstream.common.BTC_POLICY_ASSET
import com.blockstream.common.gdk.data.AccountType
import com.blockstream.common.gdk.data.Network
import com.blockstream.common.managers.SettingsManager
import com.blockstream.green.data.Countly
import com.blockstream.green.database.Wallet
import com.blockstream.green.database.WalletRepository
import com.blockstream.green.gdk.hasHistory
import com.blockstream.green.managers.SessionManager
import com.blockstream.green.ui.items.AccountTypeListItem
import com.blockstream.green.utils.AppKeystore
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class ChooseAccountTypeViewModel @AssistedInject constructor(
    appKeystore: AppKeystore,
    sessionManager: SessionManager,
    walletRepository: WalletRepository,
    settingsManager: SettingsManager,
    countly: Countly,
    @Assisted wallet: Wallet,
    @Assisted initAssetId: String?
) : AbstractAddAccountViewModel(appKeystore, sessionManager, walletRepository, countly, wallet) {

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

    private var _accountTypesLiveData = MutableLiveData<List<AccountTypeListItem>>()
    val accountTypesLiveData: LiveData<List<AccountTypeListItem>>
        get() = _accountTypesLiveData


    val allAccountTypes = MutableLiveData<List<AccountTypeListItem>>()
    val filteredAccountTypes = MutableLiveData<List<AccountTypeListItem>>()

    init {
        _assetIdLiveData.value = initAssetId ?: (session.bitcoin ?: session.liquid ?: session.defaultNetwork).policyAsset

        assetIdLiveData.asFlow().onEach {
            val list = mutableListOf<AccountType>()

            val isAmp = session.enrichedAssets[assetId]?.isAmp ?: false
            val isBitcoin = assetId == BTC_POLICY_ASSET

            if(isAmp){
                list += AccountType.AMP_ACCOUNT
            }else {
                // Check if singlesig networks are available in this session
                if ((isBitcoin && session.bitcoinSinglesig != null) || (!isBitcoin && session.liquidSinglesig != null)) {
                    list += listOf(AccountType.BIP84_SEGWIT, AccountType.BIP49_SEGWIT_WRAPPED)
                    if (isBitcoin && !session.isHardwareWallet && !session.hasLightning && settingsManager.getApplicationSettings().experimentalFeatures && !session.isTestnet && settingsManager.isLightningEnabled(countly)) {
                        list += AccountType.LIGHTNING
                    }
                }

                // Check if multisig networks are available in this session
                if ((isBitcoin && session.bitcoinMultisig != null) || (!isBitcoin && session.liquidMultisig != null)) {
                    list += AccountType.STANDARD

                    list += if (isBitcoin) {
                        AccountType.TWO_OF_THREE
                    } else {
                        AccountType.AMP_ACCOUNT
                    }
                }
            }

            allAccountTypes.value = list.map {
                AccountTypeListItem(it)
            }

            filteredAccountTypes.value = list.filter {
                it == AccountType.BIP84_SEGWIT || it == AccountType.STANDARD || it == AccountType.LIGHTNING || (it == AccountType.AMP_ACCOUNT && isAmp)
            }.map {
                AccountTypeListItem(it)
            }
        }.launchIn(viewModelScope)

        combine(
            allAccountTypes.asFlow(),
            showAdvancedLiveData.asFlow()
        ) { _, _ ->
            Unit
        }.onEach {
            val list = if(showAdvanced){
                allAccountTypes.value
            }else{
                filteredAccountTypes.value
            }

            list?.also {
                _accountTypesLiveData.value = it
            }

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