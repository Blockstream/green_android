package com.blockstream.common

import cafe.adriel.voyager.core.lifecycle.JavaSerializable
import com.blockstream.common.data.AppInfo
import com.blockstream.common.data.ApplicationSettings
import com.blockstream.common.data.Banner
import com.blockstream.common.data.CountlyAsset
import com.blockstream.common.data.CredentialType
import com.blockstream.common.data.GreenWallet
import com.blockstream.common.data.Promo
import com.blockstream.common.data.SetupArgs
import com.blockstream.common.database.Database
import com.blockstream.common.database.LoginCredentials
import com.blockstream.common.devices.GreenDevice
import com.blockstream.common.di.ApplicationScope
import com.blockstream.common.gdk.GdkSession
import com.blockstream.common.gdk.JsonConverter
import com.blockstream.common.gdk.data.Account
import com.blockstream.common.gdk.data.AccountAsset
import com.blockstream.common.gdk.data.Network
import com.blockstream.common.managers.SettingsManager
import com.blockstream.common.utils.Loggable
import com.rickclephas.kmp.nativecoroutines.NativeCoroutines
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.datetime.Clock
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonArray
import kotlin.properties.Delegates

abstract class CountlyBase(
    private val appInfo: AppInfo,
    private val applicationScope: ApplicationScope,
    private val settingsManager: SettingsManager,
    private val database: Database,
) {
    abstract fun updateRemoteConfig(force: Boolean)
    abstract fun updateOffset()
    abstract fun updateDeviceId()
    abstract fun updateConsent(withUserConsent: Boolean)
    abstract fun viewRecord(viewName: String, segmentation: Map<String, Any>? = null)
    abstract fun eventRecord(key: String, segmentation: Map<String, Any>? = null)
    abstract fun eventStart(key: String)
    abstract fun eventCancel(key: String)
    abstract fun eventEnd(key: String, segmentation: Map<String, Any>? = null)
    abstract fun traceStart(key: String)
    abstract fun traceEnd(key: String)
    abstract fun setProxy(proxyUrl: String?)
    abstract fun updateUserWallets(wallets: Int)
    abstract fun getRemoteConfigValueAsString(key: String): String?
    abstract fun getRemoteConfigValueAsBoolean(key: String): Boolean?
    abstract fun getRemoteConfigValueAsNumber(key: String): Long?
    abstract fun recordExceptionImpl(throwable: Throwable)
    abstract fun recordFeedback(rating: Int, email: String?, comment: String)

    protected var _remoteConfigUpdate = Clock.System.now()
    private var _walletCount = 0
    private var _torProxy: String? = null
    private var _appSettingsAsString: String? = null
    private var _cachedBanners: List<Banner>? = null
    private var _cachedPromos: List<Promo>? = null
    private val _remoteConfigUpdateEvent =
        MutableSharedFlow<Unit>(replay = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    @NativeCoroutines
    val remoteConfigUpdateEvent = _remoteConfigUpdateEvent.asSharedFlow()

    var exceptionCounter = 0L
        protected set

    val countlyProxy: String?
        get() {
            val appSettings = settingsManager.appSettings
            return if (appSettings.tor) {
                // Use Orbot
                if (appSettings.proxyUrl?.startsWith("socks5://") == true) {
                    appSettings.proxyUrl
                } else {
                    _torProxy ?: "socks5://tor_not_initialized"
                }
            } else if (!appSettings.proxyUrl.isNullOrBlank()) {
                appSettings.proxyUrl
            } else {
                null
            }
        }

    var analyticsConsent: Boolean by Delegates.observable(settingsManager.appSettings.analytics) { _, oldValue, newValue ->
        if (oldValue != newValue) {
            updateConsent(newValue)

            if (!newValue) {
                resetDeviceId()
            }
        }
    }
        private set

    fun initBase() {
        settingsManager.appSettingsStateFlow.onEach {
            analyticsConsent = it.analytics
            _appSettingsAsString = appSettingsToString(it)

            setProxy(countlyProxy)
        }.launchIn(scope = CoroutineScope(context = Dispatchers.Default))

        // Set number of user software wallets
        database.getWalletsFlow(isHardware = false).onEach {
            _walletCount = it.size
            updateUserProfile()
        }.launchIn(scope = applicationScope)
    }

    private fun appSettingsToString(appSettings: ApplicationSettings): String {
        val settingsAsSet = mutableSetOf<String>()

        if (appSettings.enhancedPrivacy) {
            settingsAsSet.add(ENCHANCED_PRIVACY)
        }
        if (appSettings.tor) {
            settingsAsSet.add(TOR)
        }
        if (!appSettings.proxyUrl.isNullOrBlank()) {
            settingsAsSet.add(PROXY)
        }
        if (appSettings.testnet) {
            settingsAsSet.add(TESTNET)
        }
        if (appSettings.electrumNode) {
            settingsAsSet.add(ELECTRUM_SERVER)
        }
        if (appSettings.spv) {
            settingsAsSet.add(SPV)
        }

        return settingsAsSet.sorted().joinToString(",")
    }

    private fun updateUserProfile() {
        updateUserWallets(_walletCount)
    }

    fun remoteConfigUpdated() {
        logger.d { "remoteConfigUpdated" }
        _remoteConfigUpdate = Clock.System.now()
        _cachedBanners = null
        _cachedPromos = null
        _remoteConfigUpdateEvent.tryEmit(Unit)
    }

    fun getDeviceId(): String {
        return settingsManager.getCountlyDeviceId()
    }

    fun getOffset(): Long {
        return settingsManager.getCountlyOffset(if (appInfo.isDevelopment) MAX_OFFSET_DEVELOPMENT else MAX_OFFSET_PRODUCTION)
    }

    fun resetDeviceId() {
        logger.i { "Reset Device ID" }
        settingsManager.resetCountlyDeviceId()
        settingsManager.resetCountlyOffset()

        updateDeviceId()

        updateOffset()

        updateUserProfile()
    }

    fun updateTorProxy(proxy: String) {
        _torProxy = proxy
        setProxy(countlyProxy)

        // Update Remote Config when getting new Tor proxy
        updateRemoteConfig(force = true)
    }

    private fun baseSegmentation(): HashMap<String, Any> {
        return hashMapOf<String, Any>().also { segmentation ->
            _appSettingsAsString?.also {
                segmentation[USER_PROPERTY_APP_SETTINGS] = it
            }
        }
    }

    private fun networkSegmentation(session: GdkSession): HashMap<String, Any> {
        if (!session.isNetworkInitialized) {
            return hashMapOf()
        }

        val isMainnet = session.isMainnet

        val hasBitcoinOrLightning =
            session.accounts.value.any { it.isBitcoinOrLightning } // check for unarchived bitcoin/ln accounts
        val hasLiquid =
            session.accounts.value.any { it.isLiquid } // check for unarchived liquid accounts

        // "Networks: mainnet / liquid / mainnet-mixed / testnet / testnet-liquid / testnet-mixed
        val network = when {
            hasBitcoinOrLightning && !hasLiquid -> "mainnet".takeIf { isMainnet } ?: "testnet"
            !hasBitcoinOrLightning && hasLiquid -> "liquid".takeIf { isMainnet } ?: "testnet-liquid"
            hasBitcoinOrLightning && hasLiquid -> "mainnet-mixed".takeIf { isMainnet }
                ?: "testnet-mixed"

            else -> "none"
        }

        val hasSinglesig =
            session.accounts.value.any { it.isSinglesig } // check for unarchived singlesig accounts
        val hasMultisig =
            session.accounts.value.any { it.isMultisig } // check for unarchived multisig accounts
        val hasLightning =
            session.accounts.value.any { it.isLightning } // check for unarchived lightning accounts

        // Security: singlesig / multisig / lightning / single-multi / single-light / multi-light / single-multi-light"
        val security = mutableListOf<String>()

        if (hasSinglesig) {
            security += if (hasMultisig || hasLightning) "single" else "singlesig"
        }

        if (hasMultisig) {
            security += if (hasSinglesig || hasLightning) "multi" else "multisig"
        }

        if (hasLightning) {
            security += if (hasSinglesig || hasMultisig) "light" else "lightning"
        }


        return baseSegmentation().also {
            it[PARAM_WALLET_NETWORKS] = network
            it[PARAM_SECURITY] = security.joinToString("-")
        }
    }


    @Suppress("UNCHECKED_CAST")
    fun onBoardingSegmentation(setupArgs: SetupArgs): HashMap<String, Any> {
        return hashMapOf(
            PARAM_FLOW to when {
                setupArgs.isRestoreFlow -> RESTORE
                setupArgs.isWatchOnly -> WATCH_ONLY
                else -> CREATE
            },
        ).also {
            it[PARAM_MAINNET] = (setupArgs.isTestnet != true).toString()
        } as HashMap<String, Any>
    }

    private fun deviceSegmentation(
        device: GreenDevice,
        segmentation: HashMap<String, Any> = hashMapOf()
    ): HashMap<String, Any> {
        device.deviceBrand.brand.let { segmentation[PARAM_BRAND] = it }
        device.gdkHardwareWallet?.also {
            segmentation[PARAM_FIRMWARE] = it.firmwareVersion ?: ""
            segmentation[PARAM_MODEL] = it.model
        }
        segmentation[PARAM_CONNECTION] = if (device.isUsb) USB else BLE

        return segmentation
    }

    private fun promoSegmentation(
        session: GdkSession?,
        promo: Promo
    ): HashMap<String, Any> = (session?.let { sessionSegmentation(it) } ?: hashMapOf())
            .also { segmentation ->
                segmentation[PARAM_PROMO_ID] = promo.id
            }


    private fun transactionSegmentation(
        session: GdkSession,
        account: Account,
        transactionSegmentation: TransactionSegmentation
    ): HashMap<String, Any> =
        accountSegmentation(session, account)
            .also { segmentation ->
                segmentation[PARAM_TRANSACTION_TYPE] =
                    transactionSegmentation.transactionType.toString()
                segmentation[PARAM_ADDRESS_INPUT] =
                    transactionSegmentation.addressInputType.toString()
            }

    fun twoFactorSegmentation(
        session: GdkSession,
        network: Network,
        twoFactorMethod: String
    ): HashMap<String, Any> =
        networkSegmentation(session)
            .also { segmentation ->
                segmentation[PARAM_2FA] = twoFactorMethod
                segmentation[PARAM_ACCOUNT_NETWORK] = network.countlyId
            }

    fun sessionSegmentation(session: GdkSession): HashMap<String, Any> =
        networkSegmentation(session)
            .also { segmentation ->
                session.device?.also { device ->
                    deviceSegmentation(device, segmentation)
                }

                session.ephemeralWallet?.also {
                    if (it.isBip39Ephemeral) {
                        segmentation[PARAM_EPHEMERAL_BIP39] = true
                    }
                }
            }

    fun accountSegmentation(session: GdkSession, account: Account?): HashMap<String, Any> =
        accountSegmentation(segmentation = sessionSegmentation(session), account = account)

    fun accountSegmentation(
        segmentation: HashMap<String, Any>,
        account: Account?
    ): HashMap<String, Any> =
        segmentation
            .also {
                account?.also { account ->
                    segmentation[PARAM_ACCOUNT_TYPE] = account.type.gdkType
                    segmentation[PARAM_ACCOUNT_NETWORK] = account.countlyId
                }
            }

    fun walletSegmentation(
        session: GdkSession,
        walletHasFunds: Boolean,
        accountsFunded: Int,
        accounts: List<Account>
    ): HashMap<String, Any> {
        return sessionSegmentation(session).also { segmentation ->

            segmentation[PARAM_WALLET_FUNDED] = walletHasFunds
            segmentation[PARAM_ACCOUNTS_FUNDED] = accountsFunded // number of funded accounts

            segmentation[PARAM_ACCOUNTS] = accounts.size
            segmentation[PARAM_ACCOUNTS_TYPES] =
                accounts.map { it.type.gdkType }.toSet().sorted().joinToString(",")
        }
    }

    private fun apmEvent(event: Events): String {
        return if (settingsManager.appSettings.tor) {
            "${event}_tor"
        } else {
            "$event"
        }
    }

    fun screenView(view: ScreenView) {
        if (!view.screenIsRecorded) {
            view.screenName?.takeIf { it.isNotBlank() }?.let { viewName ->
                view.screenIsRecorded = true
                logger.d { "screenView: ${view.screenName}" }
                viewRecord(viewName, view.segmentation)
            }
        }
    }

    fun viewModel(viewModel: ViewModelView) {
        viewModel.screenName()?.takeIf { it.isNotBlank() }?.let { viewName ->
            logger.d { "viewModel: $viewName" }
            viewRecord(viewName, viewModel.segmentation())
        }
    }

    fun activeWalletStart() {
        traceStart(apmEvent(Events.WALLET_ACTIVE))
        eventCancel(Events.WALLET_ACTIVE.toString())
        eventStart(Events.WALLET_ACTIVE.toString())
    }

    fun activeWalletEnd(
        session: GdkSession,
        walletHasFunds: Boolean,
        accountsFunded: Int,
        accounts: List<Account>
    ) {
        traceEnd(apmEvent(Events.WALLET_ACTIVE))
        eventEnd(
            Events.WALLET_ACTIVE.toString(),
            walletSegmentation(
                session = session,
                walletHasFunds = walletHasFunds,
                accountsFunded = accountsFunded,
                accounts = accounts
            )
        )
    }

    fun loginWalletStart() {
        traceStart(apmEvent(Events.WALLET_LOGIN))
        eventCancel(Events.WALLET_LOGIN.toString())
        eventStart(Events.WALLET_LOGIN.toString())
    }

    fun loginWalletEnd(
        wallet: GreenWallet,
        session: GdkSession,
        loginCredentials: LoginCredentials? = null
    ) {
        traceEnd(apmEvent(Events.WALLET_LOGIN))
        eventEnd(Events.WALLET_LOGIN.toString(),
            sessionSegmentation(session).also { segmentation ->
                when {
                    loginCredentials?.credential_type == CredentialType.PIN_PINDATA -> {
                        LOGIN_TYPE_PIN
                    }

                    loginCredentials?.credential_type == CredentialType.BIOMETRICS_PINDATA -> {
                        LOGIN_TYPE_BIOMETRICS
                    }

                    session.isWatchOnly -> {
                        LOGIN_TYPE_WATCH_ONLY
                    }

                    wallet.isHardware -> {
                        LOGIN_TYPE_HARDWARE
                    }

                    else -> null
                }?.let { method ->
                    segmentation[PARAM_METHOD] = method
                }
            }
        )
    }

    fun loginLightningStart() {
        traceStart(apmEvent(Events.LIGHTNING_LOGIN))
    }

    fun loginLightningStop() {
        traceEnd(apmEvent(Events.LIGHTNING_LOGIN))
    }

    fun jadeInitialize() {
        eventRecord(Events.JADE_INITIALIZE.toString())
    }

    fun buyInitiate() {
        eventRecord(Events.BUY_INITIATE.toString())
    }

    fun buyRedirect() {
        eventRecord(Events.BUY_REDIRECT.toString())
    }

    fun addWallet() {
        eventRecord(Events.WALLET_ADD.toString())
    }

    fun hardwareWallet() {
        eventRecord(Events.WALLET_HWW.toString())
    }

    fun newWallet() {
        eventRecord(Events.WALLET_NEW.toString())
    }

    fun restoreWallet() {
        eventRecord(Events.WALLET_RESTORE.toString())
    }

    fun watchOnlyWallet() {
        eventRecord(Events.WALLET_WATCH_ONLY.toString())
    }

    fun createWallet(session: GdkSession) {
        eventRecord(Events.WALLET_CREATE.toString(), sessionSegmentation(session))
    }

    fun importWallet(session: GdkSession) {
        eventRecord(Events.WALLET_IMPORT.toString(), sessionSegmentation(session))
    }

    fun renameWallet() {
        eventRecord(Events.WALLET_RENAME.toString())
    }

    fun deleteWallet() {
        eventRecord(Events.WALLET_DELETE.toString())
    }

    fun renameAccount(session: GdkSession, account: Account?) {
        eventRecord(Events.ACCOUNT_RENAME.toString(), accountSegmentation(session, account))
    }

    fun firstAccount(session: GdkSession) {
        eventRecord(Events.ACCOUNT_FIRST.toString(), sessionSegmentation(session = session))
    }

    fun accountNew(session: GdkSession) {
        eventRecord(Events.ACCOUNT_NEW.toString(), sessionSegmentation(session = session))
    }

    fun accountSelect(session: GdkSession, accountAsset: AccountAsset) {
        eventRecord(
            Events.ACCOUNT_SELECT.toString(),
            accountSegmentation(session = session, account = accountAsset.account)
        )
    }

    fun accountEmptied(
        session: GdkSession,
        walletHasFunds: Boolean,
        accountsFunded: Int,
        accounts: List<Account>,
        account: Account?
    ) {
        eventRecord(
            Events.ACCOUNT_EMPTIED.toString(),
            accountSegmentation(
                segmentation = walletSegmentation(
                    session = session,
                    walletHasFunds = walletHasFunds,
                    accountsFunded = accountsFunded,
                    accounts = accounts
                ), account = account
            )
        )
    }

    fun assetChange(session: GdkSession) {
        eventRecord(
            Events.ASSET_CHANGE.toString(),
            sessionSegmentation(session = session)
        )
    }

    fun assetSelect(session: GdkSession) {
        eventRecord(
            Events.ASSET_SELECT.toString(),
            sessionSegmentation(session = session)
        )
    }

    fun createAccount(session: GdkSession, account: Account) {
        eventRecord(
            Events.ACCOUNT_CREATE.toString(),
            accountSegmentation(session, account = account)
        )
    }

    fun hideAmount(session: GdkSession) {
        eventRecord(
            Events.HIDE_AMOUNT.toString(),
            sessionSegmentation(session = session)
        )
    }

    fun preferredUnits(session: GdkSession) {
        eventRecord(
            Events.PREFERRED_UNITS.toString(),
            sessionSegmentation(session = session)
        )
    }

    fun balanceConvert(session: GdkSession) {
        eventRecord(
            Events.BALANCE_CONVERT.toString(),
            sessionSegmentation(session = session)
        )
    }

    fun startSendTransaction() {
        traceStart(apmEvent(Events.SEND_TRANSACTION))
        // Cancel any previous event
        eventCancel(Events.SEND_TRANSACTION.toString())
        eventStart(Events.SEND_TRANSACTION.toString())
    }

    fun endSendTransaction(
        session: GdkSession,
        account: Account,
        transactionSegmentation: TransactionSegmentation,
        withMemo: Boolean
    ) {
        traceEnd(apmEvent(Events.SEND_TRANSACTION))
        eventEnd(
            Events.SEND_TRANSACTION.toString(),
            transactionSegmentation(session, account, transactionSegmentation).also {
                it[PARAM_WITH_MEMO] = withMemo
            }
        )
    }

    fun receiveAddress(
        addressType: AddressType,
        mediaType: MediaType,
        isShare: Boolean = false,
        account: Account,
        session: GdkSession
    ) {
        eventRecord(
            Events.RECEIVE_ADDRESS.toString(),
            accountSegmentation(session, account).also {
                it[PARAM_TYPE] = addressType.toString()
                it[PARAM_MEDIA] = mediaType.toString()
                it[PARAM_METHOD] = SHARE.takeIf { isShare } ?: COPY
            }
        )
    }

    fun shareTransaction(session: GdkSession, account: Account?, isShare: Boolean = false) {
        eventRecord(
            Events.SHARE_TRANSACTION.toString(),
            accountSegmentation(session, account).also {
                it[PARAM_METHOD] = SHARE.takeIf { isShare } ?: COPY
            }
        )
    }

    fun qrScan(session: GdkSession?, setupArgs: SetupArgs?, screenName: String?) {
        if (screenName == null) return

        val segmentation = session?.let { sessionSegmentation(it) }
            ?: setupArgs?.let { onBoardingSegmentation(it) } ?: hashMapOf()

        eventRecord(
            Events.QR_SCAN.toString(),
            segmentation.also {
                it[PARAM_SCREEN] = screenName
            }
        )
    }

    fun appReview(session: GdkSession, account: Account?) {
        eventRecord(
            Events.APP_REVIEW.toString(),
            accountSegmentation(session, account)
        )
    }

    fun verifyAddress(session: GdkSession, account: Account?) {
        eventRecord(Events.VERIFY_ADDRESS.toString(), accountSegmentation(session, account))
    }

    fun failedWalletLogin(session: GdkSession, error: Throwable) {
        eventRecord(
            Events.FAILED_WALLET_LOGIN.toString(),
            sessionSegmentation(session)
                .also {
                    it[PARAM_ERROR] = error.message ?: "error"
                }
        )
    }

    fun recoveryPhraseCheckFailed(page: Int) {
        eventRecord(
            Events.FAILED_RECOVERY_PHRASE_CHECK.toString(),
            mapOf(
                PARAM_PAGE to page
            )
        )
    }

    fun startFailedTransaction() {
        traceStart(apmEvent(Events.FAILED_TRANSACTION))
        eventCancel(Events.FAILED_TRANSACTION.toString())
        eventStart(Events.FAILED_TRANSACTION.toString())
    }

    fun failedTransaction(
        session: GdkSession,
        account: Account,
        transactionSegmentation: TransactionSegmentation,
        error: Throwable
    ) {
        traceEnd(apmEvent(Events.FAILED_TRANSACTION))
        eventEnd(
            Events.FAILED_TRANSACTION.toString(),
            transactionSegmentation(session, account, transactionSegmentation).also { map ->
                map[PARAM_ERROR] = error.message ?: "error"
                session.lightningSdkOrNull?.nodeInfoStateFlow?.value?.id.takeIf { account.isLightning }
                    ?.also {
                        map[PARAM_NODE_ID] = it
                    }
            }
        )
    }

    fun getRemoteConfigValueAsJsonElement(key: String): JsonElement? {
        return getRemoteConfigValueAsString(key)?.let {
            Json.parseToJsonElement(it)
        }
    }

    fun getRemoteConfigValueAsJsonArray(key: String): JsonArray? {
        return getRemoteConfigValueAsJsonElement(key)?.jsonArray
    }

    fun getRemoteConfigValueForBanners(): List<Banner>? {
        logger.d { "getRemoteConfigValueForBanners" }
        if (_cachedBanners == null) {
            _cachedBanners = try {
                getRemoteConfigValueAsString("banners")?.let {
                    JsonConverter.JsonDeserializer.decodeFromString<List<Banner>>(it)
                } ?: listOf()
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }

        return _cachedBanners
    }

    fun getRemoteConfigValueForPromos(): List<Promo>? {
        if (_cachedPromos == null) {
            _cachedPromos = try {
                getRemoteConfigValueAsString("promos")?.let {
                    logger.d { "getRemoteConfigValueForPromos $it" }
                    JsonConverter.JsonDeserializer.decodeFromString<List<Promo>>(it)
                } ?: listOf()
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }

        return _cachedPromos
    }

    fun getRemoteConfigForOnOffRamps() = getRemoteConfigValueAsBoolean("feature_on_off_ramps")

    fun getRemoteConfigValueForAssets(key: String): List<CountlyAsset>? {
        return try {
            getRemoteConfigValueAsString(key)?.let {
                JsonConverter.JsonDeserializer.decodeFromString<List<CountlyAsset>>(it)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun promoDismiss(session: GdkSession?, promo: Promo) {
        eventRecord(Events.PROMO_DISMISS.toString(), promoSegmentation(session, promo))
    }

    fun promoAction(session: GdkSession?, promo: Promo) {
        eventRecord(Events.PROMO_ACTION.toString(), promoSegmentation(session, promo))
    }

    fun promoOpen(session: GdkSession?, promo: Promo) {
        eventRecord(Events.PROMO_OPEN.toString(), promoSegmentation(session, promo))
    }

    fun promoView(session: GdkSession?, promo: Promo) {
        eventRecord(Events.PROMO_IMPRESSION.toString(), promoSegmentation(session, promo))
    }

    fun hardwareConnect(device: GreenDevice) {
        eventRecord(Events.HWW_CONNECT.toString(), deviceSegmentation(device))
    }

    fun hardwareConnected(device: GreenDevice) {
        eventRecord(Events.HWW_CONNECTED.toString(), deviceSegmentation(device))
    }

    fun jadeOtaStart(device: GreenDevice, config: String, isDelta: Boolean, version: String) {
        eventRecord(
            Events.OTA_START.toString(),
            deviceSegmentation(device, baseSegmentation()).also { segmentation ->
                segmentation[PARAM_SELECTED_CONFIG] = config.lowercase()
                segmentation[PARAM_SELECTED_DELTA] = isDelta
                segmentation[PARAM_SELECTED_VERSION] = version
            })

        eventCancel(Events.OTA_COMPLETE.toString())
        eventStart(Events.OTA_COMPLETE.toString())
    }

    fun jadeOtaRefuse(device: GreenDevice, config: String, isDelta: Boolean, version: String) {
        eventRecord(
            Events.OTA_REFUSE.toString(),
            deviceSegmentation(device, baseSegmentation()).also { segmentation ->
                segmentation[PARAM_SELECTED_CONFIG] = config.lowercase()
                segmentation[PARAM_SELECTED_DELTA] = isDelta
                segmentation[PARAM_SELECTED_VERSION] = version
            })

        eventCancel(Events.OTA_COMPLETE.toString())
    }

    fun jadeOtaFailed(
        device: GreenDevice,
        error: String,
        config: String,
        isDelta: Boolean,
        version: String
    ) {
        eventRecord(
            Events.OTA_FAILED.toString(),
            deviceSegmentation(device, baseSegmentation()).also { segmentation ->
                segmentation[PARAM_ERROR] = error
                segmentation[PARAM_SELECTED_CONFIG] = config.lowercase()
                segmentation[PARAM_SELECTED_DELTA] = isDelta
                segmentation[PARAM_SELECTED_VERSION] = version
            })

        eventCancel(Events.OTA_COMPLETE.toString())
    }

    fun jadeOtaComplete(
        device: GreenDevice,
        config: String,
        isDelta: Boolean,
        version: String
    ) {
        eventEnd(
            Events.OTA_COMPLETE.toString(),
            deviceSegmentation(device, baseSegmentation()).also { segmentation ->
                segmentation[PARAM_SELECTED_CONFIG] = config
                segmentation[PARAM_SELECTED_DELTA] = isDelta
                segmentation[PARAM_SELECTED_VERSION] = version
            })
    }


    fun recordException(throwable: Throwable) {
        if (!skipExceptionRecording.contains(throwable.message)) {
            exceptionCounter++
            recordExceptionImpl(throwable)
        }
        if (appInfo.isDevelopmentOrDebug) {
            throwable.printStackTrace()
        }
    }

    enum class Events(val event: String) {
        HWW_CONNECT("hww_connect"),
        HWW_CONNECTED("hww_connected"),
        JADE_INITIALIZE("jade_initialize"),

        OTA_START("ota_start"),
        OTA_REFUSE("ota_refuse"),
        OTA_FAILED("ota_failed"),
        OTA_COMPLETE("ota_complete"),

        PROMO_IMPRESSION("promo_impression"),
        PROMO_DISMISS("promo_dismiss"),
        PROMO_OPEN("promo_open"),
        PROMO_ACTION("promo_action"),

        BUY_INITIATE("buy_initiate"),
        BUY_REDIRECT("buy_redirect"),

        WALLET_ADD("wallet_add"),
        WALLET_HWW("wallet_hww"),

        WALLET_NEW("wallet_new"),
        WALLET_RESTORE("wallet_restore"),
        WALLET_WATCH_ONLY("wallet_wo"),

        WALLET_LOGIN("wallet_login"),
        LIGHTNING_LOGIN("lightning_login"),

        WALLET_CREATE("wallet_create"),
        WALLET_IMPORT("wallet_import"),

        WALLET_RENAME("wallet_rename"),
        WALLET_DELETE("wallet_delete"),

        WALLET_ACTIVE("wallet_active"),

        FAILED_WALLET_LOGIN("failed_wallet_login"),

        ACCOUNT_FIRST("account_first"),
        ACCOUNT_NEW("account_new"),
        ACCOUNT_SELECT("account_select"),
        ACCOUNT_CREATE("account_create"),
        ACCOUNT_RENAME("account_rename"),
        ACCOUNT_EMPTIED("account_emptied"),

        HIDE_AMOUNT("hide_amount"),
        PREFERRED_UNITS("preferred_units"),

        BALANCE_CONVERT("balance_convert"),
        ASSET_CHANGE("asset_change"),
        ASSET_SELECT("asset_select"),

        RECEIVE_ADDRESS("receive_address"),

        SHARE_TRANSACTION("share_transaction"),
        QR_SCAN("qr_scan"),

        APP_REVIEW("app_review"),

        VERIFY_ADDRESS("verify_address"),

        SEND_TRANSACTION("send_transaction"),
        FAILED_TRANSACTION("failed_transaction"),
        FAILED_RECOVERY_PHRASE_CHECK("failed_recovery_phrase_check");

        override fun toString(): String = event
    }

    companion object : Loggable() {
        const val SERVER_URL = "https://countly.blockstream.com"
        const val SERVER_URL_ONION =
            "http://greciphd2z3eo6bpnvd6mctxgfs4sslx4hyvgoiew4suoxgoquzl72yd.onion/"

        const val PRODUCTION_APP_KEY = "351d316234a4a83169fecd7e760ef64bfd638d21"
        const val DEVELOPMENT_APP_KEY = "cb8e449057253add71d2f9b65e5f66f73c073e63"

        const val GOOGLE_PLAY_ORGANIC_PRODUCTION = "95d7943329b90c07d6d7d16b874f97de68fbf67c"
        const val GOOGLE_PLAY_ORGANIC_DEVELOPMENT = "fba90e3e3959c95c18cca2f173bdf31cfb934d47"

        const val REFERRER_KEY = "referrer"

        const val MAX_OFFSET_PRODUCTION = 12 * 60 * 60 * 1000L // 12 hours
        const val MAX_OFFSET_DEVELOPMENT = 30 * 60 * 1000L // 30 mins

        const val RATING_WIDGET_ID = "5f15c01425f83c169c33cb65"

        const val PARAM_WALLET_NETWORKS = "wallet_networks"
        const val PARAM_ACCOUNT_NETWORK = "account_network"
        const val PARAM_SECURITY = "security"
        const val PARAM_ACCOUNT_TYPE = "account_type"
        const val PARAM_2FA = "2fa"
        const val PARAM_TYPE = "type"
        const val PARAM_MEDIA = "media"
        const val PARAM_METHOD = "method"
        const val PARAM_SCREEN = "screen"
        const val PARAM_PAGE = "page"
        const val PARAM_BRAND = "brand"
        const val PARAM_MODEL = "model"
        const val PARAM_FIRMWARE = "firmware"
        const val PARAM_CONNECTION = "connection"
        const val PARAM_ERROR = "error"
        const val PARAM_NODE_ID = "NODE_ID"
        const val PARAM_FLOW = "flow"
        const val PARAM_EPHEMERAL_BIP39 = "ephemeral_bip39"
        const val PARAM_MAINNET = "mainnet"

        const val PARAM_SELECTED_CONFIG = "selected_config"
        const val PARAM_SELECTED_DELTA = "selected_delta"
        const val PARAM_SELECTED_VERSION = "selected_version"

        const val PARAM_TRANSACTION_TYPE = "transaction_type"
        const val PARAM_ADDRESS_INPUT = "address_input"
        const val PARAM_WITH_MEMO = "with_memo"

        const val PARAM_PROMO_ID = "promo_id"

        const val PARAM_WALLET_FUNDED = "wallet_funded"
        const val PARAM_ACCOUNTS = "accounts"
        const val PARAM_ACCOUNTS_TYPES = "accounts_types"
        const val PARAM_ACCOUNTS_FUNDED = "accounts_funded"

        const val LOGIN_TYPE_PIN = "pin"
        const val LOGIN_TYPE_BIOMETRICS = "biometrics"
        const val LOGIN_TYPE_WATCH_ONLY = "watch_only"
        const val LOGIN_TYPE_HARDWARE = "hardware"

        const val USER_PROPERTY_APP_SETTINGS = "app_settings"

        const val USER_PROPERTY_TOTAL_WALLETS = "total_wallets"

        const val TOR = "tor"
        const val PROXY = "proxy"
        const val TESTNET = "testnet"
        const val ELECTRUM_SERVER = "electrum_server"
        const val SPV = "spv"
        const val ENCHANCED_PRIVACY = "enhanced_privacy"

        const val BLE = "BLE"
        const val USB = "USB"

        const val CREATE = "create"
        const val RESTORE = "restore"
        const val WATCH_ONLY = "watch_only"

        const val SHARE = "share"
        const val COPY = "copy"

        const val ANALYTICS_GROUP = "analytics"

        val skipExceptionRecording = listOf(
            "id_invalid_amount",
            "id_invalid_address",
            "id_insufficient_funds",
            "id_invalid_private_key",
            "id_action_canceled",
            "id_login_failed",
            "id_error_parsing",
            "id_invalid_address",
            "id_invalid_asset_id"
        )
    }
}

enum class AddressType(val string: String) {
    ADDRESS("address"),
    URI("uri");

    override fun toString(): String = string
}

enum class MediaType(val string: String) {
    TEXT("text"),
    IMAGE("image");

    override fun toString(): String = string
}

enum class TransactionType(val string: String) {
    SEND("send"),
    SWEEP("sweep"),
    BUMP("bump"),
    SWAP("swap"),
    REDEPOSIT("redeposit");

    override fun toString(): String = string
}

@Parcelize
enum class AddressInputType constructor(val string: String) : Parcelable, JavaSerializable {
    PASTE("paste"),
    SCAN("scan"),
    BIP21("bip21");

    override fun toString(): String = string
}

@Parcelize
data class TransactionSegmentation constructor(
    val transactionType: TransactionType,
    val addressInputType: AddressInputType? = null,
    val sendAll: Boolean = false
) : Parcelable, JavaSerializable

interface ScreenView {
    var screenIsRecorded: Boolean // no need in ViewModel implementation
    val screenName: String?
    val segmentation: HashMap<String, Any>?
}

interface ViewModelView {
    fun screenName(): String?
    fun segmentation(): HashMap<String, Any>?
}