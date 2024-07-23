package com.blockstream.common.models

import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.id_account_has_been_archived
import blockstream_green.common.generated.resources.id_account_has_been_removed
import blockstream_green.common.generated.resources.id_auto_logout_timeout_expired
import blockstream_green.common.generated.resources.id_could_not_recognized_qr_code
import blockstream_green.common.generated.resources.id_could_not_recognized_the_uri
import blockstream_green.common.generated.resources.id_swap_is_in_progress
import blockstream_green.common.generated.resources.id_unstable_internet_connection
import blockstream_green.common.generated.resources.id_you_dont_have_a_lightning
import blockstream_green.common.generated.resources.id_your_device_was_disconnected
import breez_sdk.InputType
import cafe.adriel.voyager.core.model.ScreenModel
import co.touchlab.kermit.Logger
import com.blockstream.common.AddressInputType
import com.blockstream.common.CountlyBase
import com.blockstream.common.ViewModelView
import com.blockstream.common.ZendeskSdk
import com.blockstream.common.crypto.GreenKeystore
import com.blockstream.common.data.AppInfo
import com.blockstream.common.data.Banner
import com.blockstream.common.data.CredentialType
import com.blockstream.common.data.DenominatedValue
import com.blockstream.common.data.Denomination
import com.blockstream.common.data.ErrorReport
import com.blockstream.common.data.GreenWallet
import com.blockstream.common.data.LogoutReason
import com.blockstream.common.data.NavData
import com.blockstream.common.data.Redact
import com.blockstream.common.data.TwoFactorResolverData
import com.blockstream.common.data.toSerializable
import com.blockstream.common.database.Database
import com.blockstream.common.di.ApplicationScope
import com.blockstream.common.events.Event
import com.blockstream.common.events.EventWithSideEffect
import com.blockstream.common.events.Events
import com.blockstream.common.extensions.cleanup
import com.blockstream.common.extensions.createLoginCredentials
import com.blockstream.common.extensions.ifConnected
import com.blockstream.common.extensions.isNotBlank
import com.blockstream.common.extensions.isPolicyAsset
import com.blockstream.common.extensions.launchIn
import com.blockstream.common.extensions.logException
import com.blockstream.common.gdk.GdkSession
import com.blockstream.common.gdk.TwoFactorResolver
import com.blockstream.common.gdk.data.Account
import com.blockstream.common.gdk.data.AccountAsset
import com.blockstream.common.gdk.data.AuthHandlerStatus
import com.blockstream.common.gdk.data.Network
import com.blockstream.common.gdk.device.DeviceBrand
import com.blockstream.common.gdk.device.DeviceResolver
import com.blockstream.common.gdk.device.GdkHardwareWallet
import com.blockstream.common.gdk.device.HardwareWalletInteraction
import com.blockstream.common.managers.SessionManager
import com.blockstream.common.managers.SettingsManager
import com.blockstream.common.navigation.NavigateDestination
import com.blockstream.common.navigation.NavigateDestinations
import com.blockstream.common.navigation.PopTo
import com.blockstream.common.sideeffects.SideEffect
import com.blockstream.common.sideeffects.SideEffects
import com.blockstream.common.utils.Loggable
import com.blockstream.common.utils.StringHolder
import com.rickclephas.kmp.nativecoroutines.NativeCoroutines
import com.rickclephas.kmp.nativecoroutines.NativeCoroutinesIgnore
import com.rickclephas.kmp.nativecoroutines.NativeCoroutinesState
import com.rickclephas.kmp.observableviewmodel.MutableStateFlow
import com.rickclephas.kmp.observableviewmodel.ViewModel
import com.rickclephas.kmp.observableviewmodel.coroutineScope
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlin.native.ObjCName

class SimpleGreenViewModel(
    greenWalletOrNull: GreenWallet? = null,
    accountAssetOrNull: AccountAsset? = null,
    val screenName: String? = null
) : GreenViewModel(greenWalletOrNull = greenWalletOrNull, accountAssetOrNull = accountAssetOrNull) {

    override fun screenName(): String? = screenName

    init {
        bootstrap()
    }
}

class SimpleGreenViewModelPreview(greenWalletOrNull: GreenWallet? = null, accountAssetOrNull: AccountAsset? = null): GreenViewModel(greenWalletOrNull, accountAssetOrNull)

open class GreenViewModel constructor(
    val greenWalletOrNull: GreenWallet? = null,
    accountAssetOrNull: AccountAsset? = null,
) : ScreenModel, ViewModel(), KoinComponent, ViewModelView, HardwareWalletInteraction, TwoFactorResolver {
    val appInfo: AppInfo by inject()
    protected val database: Database by inject()
    protected val countly: CountlyBase by inject()
    protected val sessionManager: SessionManager by inject()
    val settingsManager: SettingsManager by inject()
    protected val applicationScope: ApplicationScope by inject()
    protected val greenKeystore: GreenKeystore by inject()
    val zendeskSdk: ZendeskSdk by inject()

    private val isPreview by lazy { this::class.simpleName?.contains("Preview") == true }

    private val _event: MutableSharedFlow<Event> = MutableSharedFlow()
    private val _sideEffect: Channel<SideEffect> = Channel()
    private val _sideEffectAppFragment: Channel<SideEffect> = Channel()
    // Check with Don't Keep activities if the logout event is persisted

    // Used only for AppFragment Navigation from dialogs, tabs or bottom sheets
    var parentViewModel: GreenViewModel? = null

    // Helper method to force the use of MutableStateFlow(viewModelScope)
    @NativeCoroutinesIgnore
    protected fun <T> MutableStateFlow(value: T) = MutableStateFlow(viewModelScope, value)

    @NativeCoroutines
    val sideEffect = _sideEffect.receiveAsFlow()

    @NativeCoroutines
    val sideEffectAppFragment = _sideEffectAppFragment.receiveAsFlow()

    @NativeCoroutinesState
    val onProgress = MutableStateFlow(viewModelScope, false)

    @NativeCoroutinesState
    val onProgressDescription = MutableStateFlow<String?>(null)

    protected val _navData = MutableStateFlow(NavData())
    @NativeCoroutinesState
    val navData: StateFlow<NavData> = _navData

    // Main action validation
    internal val _isValid = MutableStateFlow(viewModelScope, isPreview)
    //@NativeCoroutinesState
    val isValid: StateFlow<Boolean> = _isValid

    // Main button enabled flag
    private val _buttonEnabled = MutableStateFlow(isPreview)
    @NativeCoroutinesState
    val buttonEnabled : StateFlow<Boolean> = _buttonEnabled

    override fun screenName(): String? = null
    override fun segmentation(): HashMap<String, Any>? = sessionOrNull?.let { countly.sessionSegmentation(session = session) }

    internal var _greenWallet: GreenWallet? = null
    val greenWallet: GreenWallet
        get() = _greenWallet ?: greenWalletOrNull!!

    @NativeCoroutines
    val greenWalletFlow: Flow<GreenWallet?> by lazy {
        greenWalletOrNull?.let {
            if (it.isEphemeral) {
                flowOf(it)
            } else {
                database.getWalletFlowOrNull(it.id)
            }
        } ?: flowOf(null)
    }

    @NativeCoroutinesState
    val accountAsset: MutableStateFlow<AccountAsset?> = MutableStateFlow(accountAssetOrNull)

    val account: Account
        get() = accountAsset.value!!.account

    val accountOrNull: Account?
        get() = accountAsset.value?.account

    val sessionOrNull: GdkSession? by lazy {
        if(isPreview) return@lazy null
        greenWalletOrNull?.let { sessionManager.getWalletSessionOrNull(it) }
    }

    val session: GdkSession by lazy {
        if (greenWalletOrNull == null) {
            // If GreenWallet is null, we can create an onboarding session
            sessionManager.getWalletSessionOrOnboarding(greenWalletOrNull)
        } else {
            sessionManager.getWalletSessionOrCreate(greenWalletOrNull)
        }
    }


    protected val _denomination by lazy {
        MutableStateFlow(sessionOrNull?.ifConnected { Denomination.default(session) } ?: Denomination.BTC)
    }

    @NativeCoroutinesState
    val denomination: StateFlow<Denomination> by lazy { _denomination.asStateFlow() }

    @NativeCoroutines
    val banner: MutableStateFlow<Banner?> = MutableStateFlow(null)
    val closedBanners = mutableListOf<Banner>()

    private var _deviceRequest: CompletableDeferred<String>? = null
    private var _bootstrapped: Boolean = false

    open val isLoginRequired: Boolean = greenWalletOrNull != null

    init {
        // It's better to initiate the ViewModel with a bootstrap() call
        // https://kotlinlang.org/docs/inheritance.html#derived-class-initialization-order
    }

    protected open fun bootstrap(){
        logger.d { "Bootstrap ${this::class.simpleName}" }

        _bootstrapped = true
        if (greenWalletOrNull != null) {
            if (isLoginRequired) {
                session.isConnectedState.onEach { isConnected ->
                    if (!isConnected) {
                        (session.logoutReason ?: LogoutReason.USER_ACTION).also {
                            logoutSideEffect(it)
                        }
                    }
                }.launchIn(this)
            }

            greenWalletFlow.onEach {
                if (it == null) {
                    postSideEffect(SideEffects.WalletDelete)
                } else {
                    _greenWallet = it
                }
            }.launchIn(this)

            // Update account (eg. rename)
            session.accounts.drop(1).onEach { accounts ->
                accountAsset.value?.also { accountAsset ->
                    accounts.firstOrNull { it.id == accountAsset.account.id}?.also {
                        this.accountAsset.value = accountAsset.copy(
                            account = it
                        )
                    }
                }

            }.launchIn(this)
        }

        _event.onEach {
            handleEvent(it)
        }.launchIn(this)

        countly.viewModel(this)

        // If session is connected, listen for network events
        if(sessionOrNull?.isConnected == true){
            listenForNetworksEvents()
        }

        combine(_isValid, onProgress) { isValid, onProgress ->
            isValid && !onProgress
        }.onEach {
            _buttonEnabled.value = it
        }.launchIn(this)

        initBanner()
    }

    @ObjCName(name = "post", swiftName = "postEvent")
    fun postEvent(@ObjCName(swiftName = "_") event: Event) {
        if(!_bootstrapped){
            if(isPreview){
                logger.i { "postEvent() Preview ViewModel detected"}
                return
            }
            throw RuntimeException("ViewModel wasn't bootstrapped")
        }

        if(event is Redact){
            if(appInfo.isDebug){
                Logger.d { "postEvent: Redacted(${event::class.simpleName}) Debug: $event" }
            }else{
                Logger.d { "postEvent: Redacted(${event::class.simpleName})" }
            }
        }else{
            Logger.d { "postEvent: $event" }
        }

        viewModelScope.coroutineScope.launch { _event.emit(event) }
    }

    protected fun postSideEffect(sideEffect: SideEffect) {
        if (sideEffect is Redact) {
            if(appInfo.isDebug){
                Logger.d { "postSideEffect: Redacted(${sideEffect::class.simpleName}) Debug: $sideEffect" }
            }else{
                Logger.d { "postSideEffect: Redacted(${sideEffect::class.simpleName})" }
            }
        } else {
            Logger.d { "postSideEffect: $sideEffect" }
        }

        // If navigate event forward side effect to parent viewmodel
        parentViewModel?.takeIf { sideEffect is SideEffects.NavigateTo }?.also {
            Logger.d { "forward to parentViewModel: $sideEffect" }
            it.postSideEffect(sideEffect)
        } ?: run {
            viewModelScope.coroutineScope.launch {
                _sideEffect.send(sideEffect)
            }
            viewModelScope.coroutineScope.launch {
                _sideEffectAppFragment.send(sideEffect)
            }
        }
    }

    private fun listenForNetworksEvents(){
        session.networkErrors.onEach {
            postSideEffect(SideEffects.ErrorDialog(Exception("id_your_personal_electrum_server|${it.first.canonicalName}")))
        }.launchIn(this)
    }

    private fun initBanner() {
        countly.remoteConfigUpdateEvent.onEach {
            val oldBanner = banner.value
            countly.getRemoteConfigValueForBanners()
                // Filter
                ?.filter {
                    // Filter closed banners
                    !closedBanners.contains(it) &&

                            // Filter networks
                            (!it.hasNetworks || ((it.networks ?: listOf()).intersect((sessionOrNull?.activeSessions?.map { it.network } ?: setOf()).toSet()).isNotEmpty()))  &&

                            // Filter based on screen name
                            (it.screens?.contains(screenName()) == true || it.screens?.contains("*") == true)
                }
                ?.shuffled()
                ?.let {
                    // Search for the already displayed banner, else give priority to those with screen name, else "*"
                    it.find { it == oldBanner } ?: it.find { it.screens?.contains(screenName()) == true } ?: it.firstOrNull()
                }.also {
                    // Set banner to ViewModel
                    banner.value = it
                }
        }.launchIn(this)
    }

    open suspend fun handleEvent(event: Event) {
        when(event){
            is Events.NotificationPermissionGiven -> {
                // Todo update notifications
                // notificationManager.notificationPermissionGiven()
            }
            is Events.SetAccountAsset -> {
                accountAsset.value = event.accountAsset
                if(event.setAsActive){
                    setActiveAccount(event.accountAsset.account)
                }
            }
            is Events.RenameAccount -> {
                renameAccount(account = event.account, name = event.name)
            }
            is Events.ArchiveAccount -> {
                updateAccount(account = event.account, isHidden = true)
            }
            is Events.UnArchiveAccount -> {
                updateAccount(account = event.account, isHidden = false, navigateToRoot = event.navigateToRoot)
            }
            is Events.RemoveAccount -> {
                 removeAccount(account = event.account)
            }
            is Events.RemoveLightningShortcut -> {
                removeLightningShortcut(greenWallet = event.wallet ?: greenWallet)
            }
            is Events.AskRemoveLightningShortcut -> {
                postSideEffect(SideEffects.AskRemoveLightningShortcut(wallet = event.wallet ?: greenWallet))
            }
            is EventWithSideEffect -> {
                postSideEffect(event.sideEffect)
            }
            is NavigateDestination -> {
                postSideEffect(SideEffects.NavigateTo(event))
            }
            is Events.BannerDismiss -> {
                banner.value?.also {
                    closedBanners += it
                }
                banner.value = null
            }
            is Events.BannerAction -> {
                banner.value?.also { banner ->
                    banner.link?.takeIf { it.isNotBlank() }?.also { url ->
                        postSideEffect(SideEffects.OpenBrowser(url))
                    }
                }
            }
            is Events.DeleteWallet -> {
                doAsync(action = {
                    sessionManager.destroyWalletSession(event.wallet)
                    database.deleteWallet(event.wallet.id)
                }, onSuccess = {
                    countly.deleteWallet()
                })
            }
            is Events.RenameWallet -> {
                doAsync(action = {
                    event.name.cleanup().takeIf { it.isNotBlank() }?.also { name ->
                        event.wallet.name = name
                        database.updateWallet(event.wallet)
                    } ?: throw Exception("Name should not be blank")
                }, onSuccess = {
                    countly.renameWallet()
                })
            }
            is Events.DeviceRequestResponse -> {
                if(event.data == null){
                    _deviceRequest?.completeExceptionally(Exception("id_action_canceled"))
                }else{
                    _deviceRequest?.complete(event.data)
                }
            }
            is Events.SelectDenomination -> {
                viewModelScope.coroutineScope.launch {
                    denominatedValue()?.also {
                        if(it.assetId.isPolicyAsset(session)){
                            postSideEffect(SideEffects.OpenDenomination(it))
                        }
                    }
                }
            }
            is Events.SetDenominatedValue -> {
                setDenominatedValue(event.denominatedValue)
            }
            is Events.Logout -> {
                logoutSideEffect(event.reason)
                sessionOrNull?.disconnectAsync(event.reason)
            }
            is Events.SubmitErrorReport -> {
                val subject = screenName()?.let { "Android Issue in $it" } ?: "Android Error Report"
                zendeskSdk.submitNewTicket(
                    subject = subject,
                    email = event.email,
                    message = event.message,
                    errorReport = event.errorReport
                )

                if(event.errorReport.paymentHash.isNotBlank()) {
                    sessionOrNull?.takeIf { it.isConnected }?.also {
                        it.reportLightningError(event.errorReport.paymentHash ?: "")
                    }
                }
            }
            is Events.SelectTwoFactorMethod -> {
                _twoFactorDeferred?.takeIf { !it.isCompleted }?.also {
                    if(event.method == null){
                        it.completeExceptionally(Exception("id_action_canceled"))
                    }else{
                        it.complete(event.method)
                    }
                }
                _twoFactorDeferred = null
            }
            is Events.AckSystemMessage -> {
                ackSystemMessage(event.network, event.message)
            }

            is Events.ReconnectFailedNetworks -> {
                tryFailedNetworks()
            }
            is Events.HandleUserInput -> {
                handleUserInput(event.data, event.isQr)
            }
            is Events.Transaction -> {
                if(event.transaction.isLightningSwap && !event.transaction.isRefundableSwap){
                    postSideEffect(SideEffects.Snackbar(StringHolder.create(Res.string.id_swap_is_in_progress)))
                }else{
                    postSideEffect(
                        SideEffects.NavigateTo(
                            NavigateDestinations.Transaction(
                                transaction = event.transaction
                            )
                        )
                    )
                }
            }
            is Events.ChooseAccountType -> {
                postSideEffect(
                    SideEffects.NavigateTo(
                        NavigateDestinations.ChooseAccountType(popTo = event.popTo)
                    )
                )

                if(event.isFirstAccount){
                    countly.firstAccount(session)
                }
            }
        }
    }

    private fun logoutSideEffect(reason: LogoutReason){
        postSideEffect(SideEffects.Logout(reason))
        when(reason){
            LogoutReason.CONNECTION_DISCONNECTED -> {
                postSideEffect(SideEffects.Snackbar(StringHolder.create(Res.string.id_unstable_internet_connection)))
            }
            LogoutReason.AUTO_LOGOUT_TIMEOUT -> {
                postSideEffect(SideEffects.Snackbar(StringHolder.create(Res.string.id_auto_logout_timeout_expired)))
            }
            LogoutReason.DEVICE_DISCONNECTED -> {
                postSideEffect(SideEffects.Snackbar(StringHolder.create(Res.string.id_your_device_was_disconnected)))
            }
            else -> {}
        }
    }

    protected fun <T: Any?> doAsync(
        action: suspend () -> T,
        mutex: Mutex? = null,
        timeout: Long = 0,
        preAction: (() -> Unit)? = {
            onProgress.value = true
        },
        postAction: ((Exception?) -> Unit)? = {
            onProgress.value = false
        },
        onSuccess: suspend (T) -> Unit,
        onError: suspend ((Throwable) -> Unit) = {
            if (appInfo.isDebug) {
                it.printStackTrace()
            }
            postSideEffect(SideEffects.ErrorDialog(it, errorReport = errorReport(it)))
        }
    ): Job {
        return viewModelScope.coroutineScope.launch {
            (mutex ?: Mutex()).withLock {
                try {
                    preAction?.invoke()

                    withContext(context = Dispatchers.IO) {
                        if (timeout <= 0L) {
                            action.invoke()
                        } else {
                            withTimeout(timeout) {
                                action.invoke()
                            }
                        }
                    }.also {
                        if (this.isActive) {
                            postAction?.invoke(null)
                            onSuccess.invoke(it)
                        }
                    }

                } catch (e: Exception) {
                    if (this.isActive) {
                        countly.recordException(e)
                        postAction?.invoke(e)
                        onError.invoke(e)
                    }
                }
            }
        }
    }

    private fun renameAccount(account: Account, name: String){
        if (name.isBlank()) return

        doAsync({
            session.updateAccount(account = account, name = name.cleanup() ?: account.name).also { updatedAccount ->
                accountAsset.value?.let {
                    // Update active account if needed
                    if(it.account.id == updatedAccount.id){
                        accountAsset.value = it.copy(account = updatedAccount)
                    }
                }
            }
        }, onSuccess = {
            countly.renameAccount(session, it)
            postSideEffect(SideEffects.Dismiss)
        })
    }

    private fun updateAccount(account: Account, isHidden: Boolean, navigateToRoot: Boolean = false){
        doAsync({
            session.updateAccount(account = account, isHidden = isHidden, userInitiated = true)
        }, onSuccess = {
            if(isHidden){
                // Update active account from Session if it was archived
                session.activeAccount.value?.also {
                    setActiveAccount(it)
                }

                postSideEffect(SideEffects.AccountArchived(account = account))
                postSideEffect(SideEffects.Snackbar(StringHolder.create(Res.string.id_account_has_been_archived)))
                
                // This only have effect on AccountOverview
                postSideEffect(SideEffects.NavigateToRoot())
            }else{
                // Make it active
                setActiveAccount(account)
                postSideEffect(SideEffects.AccountUnarchived(account = account))
                if(navigateToRoot){
                    postSideEffect(SideEffects.NavigateToRoot())
                }
            }
        })
    }

    private fun removeAccount(account: Account){
        if(account.isLightning) {
            doAsync({
                database.deleteLoginCredentials(greenWallet.id, CredentialType.KEYSTORE_GREENLIGHT_CREDENTIALS)
                database.deleteLoginCredentials(greenWallet.id, CredentialType.LIGHTNING_MNEMONIC)
                session.removeAccount(account)
            }, onSuccess = {
                // Update active account from Session if it was archived
                setActiveAccount(session.activeAccount.value!!)
                postSideEffect(SideEffects.Snackbar(StringHolder.create(Res.string.id_account_has_been_removed)))

                // This only have effect on AccountOverview
                postSideEffect(SideEffects.NavigateToRoot())
            })
        }
    }

    private fun removeLightningShortcut(greenWallet: GreenWallet) {
        doAsync({
            database.deleteLoginCredentials(greenWallet.id, CredentialType.LIGHTNING_MNEMONIC)
        }, onSuccess = {

        })
    }

    private fun setActiveAccount(account: Account) {
        session.setActiveAccount(account)

        greenWallet.also {
            it.activeNetwork = account.networkId
            it.activeAccount = account.pointer

            if(!it.isEphemeral) {
                viewModelScope.coroutineScope.launch(context = logException(countly)){
                    database.updateWallet(it)
                }
            }
        }
    }

    private fun tryFailedNetworks() {
        session.tryFailedNetworks(hardwareWalletResolver = session.device?.let { device ->
            DeviceResolver.createIfNeeded(device.gdkHardwareWallet, this)
        })
    }

    private fun ackSystemMessage(network: Network, message : String){
        doAsync({
            session.ackSystemMessage(network, message)
            session.updateSystemMessage()
        }, onSuccess = {
            postSideEffect(SideEffects.Dismiss)
        })
    }

    protected fun handleUserInput(data: String, isQr: Boolean) {
        doAsync({
            val checkedInput = session.parseInput(data)

            if (checkedInput != null) {

                when (val inputType = checkedInput.second) {
                    is InputType.LnUrlAuth -> {
                        if (session.hasLightning) {
                            postSideEffect(
                                SideEffects.NavigateTo(
                                    NavigateDestinations.LnUrlAuth(
                                        lnUrlAuthRequest = inputType.data.toSerializable()
                                    )
                                )
                            )
                        } else {
                            postSideEffect(SideEffects.Snackbar(StringHolder.create(Res.string.id_you_dont_have_a_lightning)))
                        }
                    }

                    is InputType.LnUrlWithdraw -> {
                        if (session.hasLightning) {
                            postSideEffect(
                                SideEffects.NavigateTo(
                                    NavigateDestinations.LnUrlWithdraw(
                                        lnUrlWithdrawRequest = inputType.data.toSerializable()
                                    )
                                )
                            )
                        }else{
                            postSideEffect(SideEffects.Snackbar(StringHolder.create(Res.string.id_you_dont_have_a_lightning)))
                        }
                    }

                    else -> {
                        session.activeAccount.value?.also { activeAccount ->
                            var account = activeAccount

                            // Different network
                            if(!account.network.isSameNetwork(checkedInput.first)){
                                session.allAccounts.value.find {
                                    it.network.isSameNetwork(
                                        checkedInput.first
                                    )
                                }?.also {
                                    account = it
                                }
                            }

                            postSideEffect(
                                SideEffects.NavigateTo(
                                    NavigateDestinations.Send(
                                        address = data,
                                        addressType = if (isQr) AddressInputType.SCAN else AddressInputType.BIP21
                                    )
                                )
                            )
                        }
                    }
                }

            } else {
                postSideEffect(SideEffects.Snackbar(StringHolder.create(if (isQr) Res.string.id_could_not_recognized_qr_code else Res.string.id_could_not_recognized_the_uri)))
            }
        }, onSuccess = {

        })
    }

    protected suspend fun _enableLightningShortcut() {
        sessionOrNull?.also { session ->
            val encryptedData = withContext(context = Dispatchers.IO) {
                session.deriveLightningMnemonic().let {
                    greenKeystore.encryptData(it.encodeToByteArray())
                }
            }

            database.replaceLoginCredential(
                createLoginCredentials(
                    walletId = greenWallet.id,
                    network = session.lightning!!.id,
                    credentialType = CredentialType.LIGHTNING_MNEMONIC,
                    encryptedData = encryptedData
                )
            )
        }
    }

    final override fun interactionRequest(
        hw: GdkHardwareWallet,
        completable: CompletableDeferred<Boolean>?,
        text: String?
    ) {
        postSideEffect(SideEffects.DeviceInteraction(hw.device, text, completable))
    }

    final override fun requestPassphrase(deviceBrand: DeviceBrand?): String {
        return CompletableDeferred<String>().let {
            _deviceRequest = it
            postSideEffect(SideEffects.DeviceRequestPassphrase)
            runBlocking { it.await() }
        }
    }

    final override fun requestPinMatrix(deviceBrand: DeviceBrand?): String? {
        return CompletableDeferred<String>().let {
            _deviceRequest = it
            postSideEffect(SideEffects.DeviceRequestPin)
            runBlocking { it.await() }
        }
    }

    protected open suspend fun denominatedValue(): DenominatedValue? = null
    protected open fun setDenominatedValue(denominatedValue: DenominatedValue) { }
    protected open fun errorReport(exception: Throwable): ErrorReport? { return null}

    var _twoFactorDeferred: CompletableDeferred<String>? = null
    override suspend fun withSelectMethod(availableMethods: List<String>): CompletableDeferred<String> {
        return CompletableDeferred<String>().also {
            _twoFactorDeferred = it
            postSideEffect(SideEffects.TwoFactorResolver(TwoFactorResolverData.selectMethod(availableMethods)))
        }
    }

    override suspend fun getCode(
        network: Network,
        enable2faCallMethod: Boolean,
        authHandlerStatus: AuthHandlerStatus
    ): CompletableDeferred<String> {
        return CompletableDeferred<String>().also {
            _twoFactorDeferred = it
            postSideEffect(SideEffects.TwoFactorResolver(TwoFactorResolverData.getCode(network,enable2faCallMethod, authHandlerStatus)))
        }
    }

    companion object: Loggable(){
        fun preview() = object : GreenViewModel() { }
    }
}
