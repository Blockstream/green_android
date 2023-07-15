package com.blockstream.green.ui.add

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asFlow
import com.blockstream.common.BTC_POLICY_ASSET
import com.blockstream.common.data.GreenWallet
import com.blockstream.common.extensions.hasHistory
import com.blockstream.common.gdk.data.AccountType
import com.blockstream.common.gdk.data.Network
import com.blockstream.green.data.Countly
import com.blockstream.green.ui.items.AccountTypeListItem
import com.blockstream.green.utils.AppKeystore
import com.rickclephas.kmm.viewmodel.coroutineScope
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.koin.android.annotation.KoinViewModel
import org.koin.core.annotation.InjectedParam

@KoinViewModel
class ChooseAccountTypeViewModel constructor(
    @InjectedParam wallet: GreenWallet,
    @InjectedParam initAssetId: String?
) : AbstractAddAccountViewModel(wallet) {

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

            val isAmp = session.enrichedAssets.value[assetId]?.isAmp ?: false
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
        }.launchIn(viewModelScope.coroutineScope)

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

        }.launchIn(viewModelScope.coroutineScope)
    }

    fun isAccountAlreadyArchived(network: Network, accountType: AccountType): Boolean {
        return session.allAccounts.value.find { it.hidden && it.network == network && it.type == accountType && it.hasHistory(session)} != null
    }
}