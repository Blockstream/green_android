package com.blockstream.green.ui.overview

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asLiveData
import breez_sdk.HealthCheckStatus
import com.blockstream.common.data.Denomination
import com.blockstream.common.data.GreenWallet
import com.blockstream.common.gdk.JsonConverter.Companion.JsonDeserializer
import com.blockstream.common.gdk.data.Account
import com.blockstream.common.gdk.data.Assets
import com.blockstream.common.gdk.data.Network
import com.blockstream.common.gdk.data.SwapProposal
import com.blockstream.common.gdk.data.Transaction
import com.blockstream.common.gdk.device.DeviceResolver
import com.blockstream.green.ui.items.AlertType
import com.blockstream.green.ui.wallet.AbstractWalletViewModel
import com.rickclephas.kmm.viewmodel.coroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.withContext
import org.koin.android.annotation.KoinViewModel
import org.koin.core.annotation.InjectedParam
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

@KoinViewModel
class WalletOverviewViewModel constructor(
    @InjectedParam initWallet: GreenWallet
) : AbstractWalletViewModel(initWallet) {
    val isWatchOnly: LiveData<Boolean> = MutableLiveData(wallet.isWatchOnly)

    private val _systemMessageLiveData: MutableLiveData<AlertType?> = MutableLiveData()
    private val _twoFactorStateLiveData: MutableLiveData<AlertType?> = MutableLiveData()
    private val _failedNetworkLoginsLiveData: MutableLiveData<List<Network>> = MutableLiveData()
    private val _lspHeath: MutableLiveData<HealthCheckStatus> = MutableLiveData(HealthCheckStatus.OPERATIONAL)

    private val _alertsLiveData: LiveData<List<AlertType>> by lazy {
        MediatorLiveData<List<AlertType>>().apply {
            val combine = { _: Any? ->
                value = listOfNotNull(
                    _twoFactorStateLiveData.value,
                    _systemMessageLiveData.value,
                    if (wallet.isBip39Ephemeral) AlertType.EphemeralBip39 else null,
                    banner.value?.let { banner -> AlertType.Banner(banner) },
                    if (session.isTestnet) AlertType.TestnetWarning else null,
                    if (_failedNetworkLoginsLiveData.value.isNullOrEmpty()) null else AlertType.FailedNetworkLogin,
                    if (_lspHeath.value == HealthCheckStatus.OPERATIONAL) null else AlertType.LspStatus(
                        maintenance = _lspHeath.value == HealthCheckStatus.MAINTENANCE
                    )
                )
            }
            addSource(_failedNetworkLoginsLiveData, combine)
            addSource(_twoFactorStateLiveData, combine)
            addSource(_systemMessageLiveData, combine)
            addSource(_lspHeath, combine)
            addSource(banner.asLiveData(), combine)
        }
    }
    val alertsLiveData: LiveData<List<AlertType>> get() = _alertsLiveData

    private val _archivedAccountsLiveData: MutableLiveData<Int> = MutableLiveData(0)
    val archivedAccountsLiveData: LiveData<Int> get() = _archivedAccountsLiveData

    val walletTotalBalanceFlow: StateFlow<Long> get() = session.walletTotalBalance

    val walletAssetsFlow: StateFlow<Assets> get() = session.walletAssets

    val walletTransactionsFlow: StateFlow<List<Transaction>> get() = session.walletTransactions

    val zeroAccounts: StateFlow<Boolean>
        get() = session.zeroAccounts


    val balanceDenomination: MutableStateFlow<Denomination> = MutableStateFlow(Denomination.default(session))

    init {

        session
            .allAccounts
            .onEach { accounts ->
                _archivedAccountsLiveData.value = accounts.count { it.hidden }
            }.launchIn(viewModelScope.coroutineScope)

        session.systemMessage.onEach {
            if(it.isEmpty()){
                _systemMessageLiveData.value = null
            }else{
                _systemMessageLiveData.value = AlertType.SystemMessage(it.first().first, it.first().second)
            }
        }.launchIn(viewModelScope.coroutineScope)

        session.lightningSdkOrNull?.also {
            it.healthCheckStatus.onEach { status ->
                _lspHeath.value = status
            }.launchIn(viewModelScope.coroutineScope)
        }

        // Support only for Bitcoin
        session.bitcoinMultisig?.let { network ->
            session.twoFactorReset(network).onEach {
                _twoFactorStateLiveData.postValue(
                    if (it != null && it.isActive == true) {
                        if (it.isDisputed == true) {
                            AlertType.Dispute2FA(network, it)
                        } else {
                            AlertType.Reset2FA(network, it)
                        }
                    } else {
                        null
                    }
                )
            }.launchIn(viewModelScope.coroutineScope)
        }

        session.failedNetworks.onEach {
            _failedNetworkLoginsLiveData.value = it
        }.launchIn(viewModelScope.coroutineScope)
    }

    fun changeDenomination(){
        balanceDenomination.value = (when (balanceDenomination.value) {
            is Denomination.FIAT -> {
                Denomination.default(session)
            }
            else -> {
                Denomination.fiat(session)
            }
        }) ?: Denomination.BTC

        countly.balanceConvert(session)
    }

    fun refresh(){
        session.updateAccountsAndBalances(refresh = true)
        session.updateWalletTransactions()
        session.updateLiquidAssets()
    }

    fun dismissSystemMessage(){
        _systemMessageLiveData.postValue(null)
    }

    fun archiveAccount(account: Account) {
        super.updateAccountVisibility(account, true, null)
    }

    fun removeAccount(account: Account) {
        super.removeAccount(account, null)
    }

    suspend fun downloadProposal(link: String): SwapProposal? = withContext(Dispatchers.IO) {
        return@withContext URL(link)
            .openConnection()
            .let {
                it as HttpURLConnection
            }.apply {
                requestMethod = "GET"
                setRequestProperty("Accept", "application/json")
            }.let {
                if (it.responseCode == 200) it.inputStream else it.errorStream
            }.let { streamToRead ->
                BufferedReader(InputStreamReader(streamToRead)).use {
                    val response = StringBuffer()

                    var inputLine = it.readLine()
                    while (inputLine != null) {
                        response.append(inputLine)
                        inputLine = it.readLine()
                    }
                    it.close()
                    response.toString()
                }
            }.let {
                JsonDeserializer.decodeFromString<SwapProposal>(it)
            }
    }

    fun tryFailedNetworks() {
        session.tryFailedNetworks(hardwareWalletResolver = session.device?.let { device ->
            DeviceResolver.createIfNeeded(
                device.gdkHardwareWallet,
                this
            )
        })
    }
}