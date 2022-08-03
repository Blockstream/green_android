package com.blockstream.green.ui.overview

import androidx.lifecycle.*
import com.blockstream.gdk.GdkBridge
import com.blockstream.gdk.data.Account
import com.blockstream.gdk.data.Network
import com.blockstream.gdk.data.SwapProposal
import com.blockstream.gdk.data.Transaction
import com.blockstream.green.data.Countly
import com.blockstream.green.database.Wallet
import com.blockstream.green.database.WalletRepository
import com.blockstream.green.devices.DeviceResolver
import com.blockstream.green.managers.SessionManager
import com.blockstream.green.gdk.WalletBalances
import com.blockstream.green.ui.items.AlertType
import com.blockstream.green.ui.wallet.AbstractWalletViewModel
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.serialization.decodeFromString
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

class WalletOverviewViewModel @AssistedInject constructor(
    sessionManager: SessionManager,
    walletRepository: WalletRepository,
    countly: Countly,
    @Assisted initWallet: Wallet
) : AbstractWalletViewModel(sessionManager, walletRepository, countly, initWallet) {
//    var expandedAccount: MutableLiveData<Account?> = MutableLiveData(session.activeAccountOrNull)
    val isWatchOnly: LiveData<Boolean> = MutableLiveData(wallet.isWatchOnly)

    private val _appReviewLiveData: MutableLiveData<Boolean> = MutableLiveData(false)
    private val _systemMessageLiveData: MutableLiveData<AlertType?> = MutableLiveData()
    private val _twoFactorStateLiveData: MutableLiveData<AlertType?> = MutableLiveData()
    private val _failedNetworkLoginsLiveData: MutableLiveData<List<Network>> = MutableLiveData()

    private val _alertsLiveData: LiveData<List<AlertType>> by lazy {
        MediatorLiveData<List<AlertType>>().apply {
            val combine = { _: Any? ->
                value = listOfNotNull(
                    _twoFactorStateLiveData.value,
                    _systemMessageLiveData.value,
                    if (wallet.isEphemeral && !wallet.isHardware) AlertType.EphemeralBip39 else null,
                    banner.value?.let { banner -> AlertType.Banner(banner) },
                    if (_appReviewLiveData.value == true) AlertType.AppReview else null,
                    if (session.isTestnet) AlertType.TestnetWarning else null,
                    if (_failedNetworkLoginsLiveData.value.isNullOrEmpty()) null else AlertType.FailedNetworkLogin
                )
            }
            addSource(_failedNetworkLoginsLiveData, combine)
            addSource(_twoFactorStateLiveData, combine)
            addSource(_systemMessageLiveData, combine)
            addSource(_appReviewLiveData, combine)
            addSource(banner, combine)
        }
    }
    val alertsLiveData: LiveData<List<AlertType>> get() = _alertsLiveData

    private val _archivedAccountsLiveData: MutableLiveData<Int> = MutableLiveData(0)
    val archivedAccountsLiveData: LiveData<Int> get() = _archivedAccountsLiveData

    val walletTotalBalanceFlow: StateFlow<Long> get() = session.walletTotalBalanceFlow

    val walletAssetsFlow: StateFlow<WalletBalances> get() = session.walletAssetsFlow

    val walletTransactionsFlow: StateFlow<List<Transaction>> get() = session.walletTransactionsFlow

    init {
        session
            .allAccountsFlow
            .onEach { accounts ->
                _archivedAccountsLiveData.value = accounts.count { it.hidden }
            }.launchIn(lifecycleScope)

        session.systemMessageFlow.onEach {
            if(it.isEmpty()){
                _systemMessageLiveData.value = null
            }else{
                _systemMessageLiveData.value = AlertType.SystemMessage(it.first().first, it.first().second)
            }
        }.launchIn(viewModelScope)

        session.failedNetworksFlow.onEach {
            _failedNetworkLoginsLiveData.value = it
        }.launchIn(viewModelScope)
    }

    fun refresh(){
        session.updateAccountsAndBalances(refresh = true)
        session.updateWalletTransactions()
        session.updateLiquidAssets()
    }

    fun setAppReview(showAppReview: Boolean){
        _appReviewLiveData.postValue(showAppReview)
    }

    fun dismissSystemMessage(){
        _systemMessageLiveData.postValue(null)
    }

    fun archiveAccount(account: Account) {
        super.updateAccountVisibility(account, true, null)
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
                GdkBridge.JsonDeserializer.decodeFromString<SwapProposal>(it)
            }
    }

    fun tryFailedNetworks() {
        session.tryFailedNetworks(hardwareWalletResolver = session.device?.let { device ->
            DeviceResolver(
                device.hwWallet,
                this
            )
        })
    }

    @dagger.assisted.AssistedFactory
    interface AssistedFactory {
        fun create(
            wallet: Wallet
        ): WalletOverviewViewModel
    }

    companion object {
        fun provideFactory(
            assistedFactory: AssistedFactory,
            wallet: Wallet
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return assistedFactory.create(wallet) as T
            }
        }
    }
}