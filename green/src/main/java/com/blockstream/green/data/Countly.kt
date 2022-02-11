package com.blockstream.green.data

import android.content.Context
import android.content.res.Configuration
import android.os.Parcelable
import com.blockstream.gdk.WalletBalances
import com.blockstream.gdk.data.SubAccount
import com.blockstream.green.R
import com.blockstream.green.database.LoginCredentials
import com.blockstream.green.database.Wallet
import com.blockstream.green.database.WalletRepository
import com.blockstream.green.gdk.GreenSession
import com.blockstream.green.settings.SettingsManager
import com.blockstream.green.ui.AppActivity
import kotlinx.parcelize.Parcelize
import mu.KLogging


class Countly constructor(
    private val context: Context,
    settingsManager: SettingsManager,
    walletRepository: WalletRepository,
) {
    val analyticsFeatureEnabled = context.resources.getBoolean(R.bool.feature_analytics)

    val deviceId: String = ""

    fun applicationOnCreate(){
        // stub
    }

    fun resetDeviceId(){
        // stub
    }

    fun recordException(throwable: Throwable) {
        // stub
    }

    fun onStart(activity: AppActivity) {
        // stub
    }

    fun onStop() {
        // stub
    }

    fun onConfigurationChanged(newConfig: Configuration) {
        // stub
    }

    fun screenView(view: ScreenView) {
        // stub
    }

    fun activeWallet(
        session: GreenSession,
        walletBalances: WalletBalances,
        subAccounts: List<SubAccount>
    ) {
        // stub
    }

    fun loginWallet(
        wallet: Wallet,
        session: GreenSession,
        loginCredentials: LoginCredentials? = null
    ) {
        // stub
    }

    fun startCreateWallet() {
        // stub
    }

    fun createWallet(session: GreenSession) {
        // stub
    }

    fun startRestoreWallet() {
        // stub
    }

    fun restoreWallet(session: GreenSession) {
        // stub
    }

    fun renameWallet() {
        // stub
    }

    fun deleteWallet() {
        // stub
    }

    fun renameAccount(session: GreenSession) {
        // stub
    }

    fun createAccount(session: GreenSession, subAccount: SubAccount) {
        // stub
    }

    fun startSendTransaction(){
        // stub
    }

    fun sendTransaction(
        session: GreenSession,
        subAccount: SubAccount?,
        transactionSegmentation: TransactionSegmentation,
        withMemo: Boolean
    ) {
        // stub
    }

    fun receiveAddress(
        isShare: Boolean = false,
        isUri: Boolean = false,
        isQR: Boolean = false,
        subAccount: SubAccount?,
        session: GreenSession
    ) {
        // stub
    }

    fun shareTransaction(session: GreenSession, isShare: Boolean = false) {
        // stub
    }

    fun failedWalletLogin(session: GreenSession, error: Throwable) {
        // stub
    }

    fun recoveryPhraseCheckFailed(networkId: String, page: Int) {
        // stub
    }

    fun failedTransaction(session: GreenSession, error: Throwable) {
        // stub
    }

    fun networkSegmentation(networkId: String): HashMap<String, Any> = hashMapOf()

    fun onBoardingSegmentation(onboardingOptions: OnboardingOptions): HashMap<String, Any> = hashMapOf()

    fun sessionSegmentation(session: GreenSession): HashMap<String, Any> = hashMapOf()

    fun subAccountSegmentation(session: GreenSession, subAccount: SubAccount?): HashMap<String, Any> = hashMapOf()

    fun twoFactorSegmentation(session: GreenSession, subAccount: SubAccount?, twoFactorMethod: TwoFactorMethod): HashMap<String, Any> = hashMapOf()


    companion object : KLogging() {
        const val TOR = "tor"
        const val PROXY = "proxy"
        const val TESTNET = "testnet"
        const val SPV = "spv"

        const val BLE = "ble"
        const val USB = "usb"

        const val CREATE = "create"
        const val RESTORE = "restore"
        const val WATCH_ONLY = "watch_only"

        const val SINGLESIG = "singlesig"
        const val MULTISIG = "multisig"
    }
}

enum class TransactionType(val string: String) {
    SEND("send"),
    SWEEP("sweep"),
    BUMP("bump");

    override fun toString(): String = string
}

enum class AddressInputType(val string: String) {
    PASTE("paste"),
    SCAN("scan"),
    BIP21("bip21");

    override fun toString(): String = string
}

@Parcelize
data class TransactionSegmentation constructor(
    val transactionType: TransactionType,
    val addressInputType: AddressInputType?,
    val sendAll: Boolean
) : Parcelable

interface ScreenView{
    var screenIsRecorded: Boolean
    val screenName: String?
    val segmentation: HashMap<String, Any>?
}