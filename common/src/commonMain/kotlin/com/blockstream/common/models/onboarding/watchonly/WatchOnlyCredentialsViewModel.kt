package com.blockstream.common.models.onboarding.watchonly

import com.blockstream.common.crypto.PlatformCipher
import com.blockstream.common.data.CredentialType
import com.blockstream.common.data.EncryptedData
import com.blockstream.common.data.GreenWallet
import com.blockstream.common.data.SetupArgs
import com.blockstream.common.data.WatchOnlyCredentials
import com.blockstream.common.database.LoginCredentials
import com.blockstream.common.events.Event
import com.blockstream.common.events.Events
import com.blockstream.common.extensions.createLoginCredentials
import com.blockstream.common.extensions.launchIn
import com.blockstream.common.extensions.objectId
import com.blockstream.common.gdk.data.AccountType
import com.blockstream.common.models.GreenViewModel
import com.blockstream.common.navigation.NavigateDestinations
import com.blockstream.common.platformFileSystem
import com.blockstream.common.sideeffects.SideEffect
import com.blockstream.common.sideeffects.SideEffects
import com.blockstream.common.utils.generateWalletName
import com.rickclephas.kmp.nativecoroutines.NativeCoroutinesState
import com.rickclephas.kmp.observableviewmodel.MutableStateFlow
import com.rickclephas.kmp.observableviewmodel.coroutineScope
import com.rickclephas.kmp.observableviewmodel.stateIn
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onEach
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okio.Path.Companion.toPath
import okio.Source
import okio.buffer
import okio.use

abstract class WatchOnlyCredentialsViewModelAbstract(val setupArgs: SetupArgs) : GreenViewModel() {
    override fun screenName(): String = "OnBoardWatchOnlyCredentials"

    override fun segmentation(): HashMap<String, Any>? =
        setupArgs.let { countly.onBoardingSegmentation(setupArgs = it) }

    @NativeCoroutinesState
    abstract val isSinglesig: StateFlow<Boolean>

    @NativeCoroutinesState
    abstract val isLiquid: StateFlow<Boolean>

    @NativeCoroutinesState
    abstract val isLoginEnabled: StateFlow<Boolean>

    @NativeCoroutinesState
    abstract val canUseBiometrics: StateFlow<Boolean>

    @NativeCoroutinesState
    abstract val username: MutableStateFlow<String>

    @NativeCoroutinesState
    abstract val password: MutableStateFlow<String>

    @NativeCoroutinesState
    abstract val watchOnlyDescriptor: MutableStateFlow<String>

    @NativeCoroutinesState
    abstract val isOutputDescriptors: MutableStateFlow<Boolean>

    @NativeCoroutinesState
    abstract val isRememberMe: MutableStateFlow<Boolean>

    @NativeCoroutinesState
    abstract val withBiometrics: MutableStateFlow<Boolean>
}

class WatchOnlyCredentialsViewModel(setupArgs: SetupArgs) :
    WatchOnlyCredentialsViewModelAbstract(setupArgs = setupArgs) {
    override val isSinglesig: MutableStateFlow<Boolean> =
        MutableStateFlow(viewModelScope, setupArgs.isSinglesig == true)

    override val isLiquid: MutableStateFlow<Boolean> =
        MutableStateFlow(viewModelScope, setupArgs.network?.isLiquid == true)

    override val username: MutableStateFlow<String> = MutableStateFlow(viewModelScope, "")
    override val password: MutableStateFlow<String> = MutableStateFlow(viewModelScope, "")
    override val watchOnlyDescriptor: MutableStateFlow<String> =
        MutableStateFlow(viewModelScope, "")
    override val isOutputDescriptors: MutableStateFlow<Boolean> =
        MutableStateFlow(viewModelScope, setupArgs.network?.isLiquid == true)
    override val isRememberMe: MutableStateFlow<Boolean> = MutableStateFlow(viewModelScope, true)

    override val isLoginEnabled: StateFlow<Boolean>

    override val canUseBiometrics: MutableStateFlow<Boolean>
    override val withBiometrics: MutableStateFlow<Boolean>

    private var biometricsPlatformCipher: CompletableDeferred<PlatformCipher>? = null

    class LocalEvents {
        data class AppendWatchOnlyDescriptor(val value: String) : Event
        class ImportFile(val source: Source) : Event
    }

    class LocalSideEffects {
        object RequestCipher : SideEffect
    }

    init {
        greenKeystore.canUseBiometrics().also {
            canUseBiometrics = MutableStateFlow(viewModelScope, it)
            withBiometrics = MutableStateFlow(viewModelScope, it)
        }

        watchOnlyDescriptor.onEach {
            if (it.contains("(")) {
                isOutputDescriptors.value = true
            }
        }.launchIn(this)

        isLoginEnabled = combine(
            watchOnlyDescriptor,
            username,
            password,
            onProgress
        ) { watchOnlyDescriptor, username, password, onProgress ->
            if (!onProgress) {
                if (isSinglesig.value) {
                    watchOnlyDescriptor.isNotBlank()
                } else {
                    username.isNotBlank() && password.isNotBlank()
                }
            } else {
                false
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), false)

        bootstrap()
    }

    override suspend fun handleEvent(event: Event) {
        super.handleEvent(event)
        when (event) {
            is Events.Continue -> {
                createNewWatchOnlyWallet(
                    biometricsCipherProvider = viewModelScope.coroutineScope.async(
                        start = CoroutineStart.LAZY
                    ) {
                        CompletableDeferred<PlatformCipher>().let {
                            biometricsPlatformCipher = it
                            postSideEffect(LocalSideEffects.RequestCipher)
                            it.await()
                        }
                    }
                )
            }

            is Events.ProvideCipher -> {
                event.platformCipher?.also {
                    biometricsPlatformCipher?.complete(it)
                }

                event.exception?.also {
                    biometricsPlatformCipher?.completeExceptionally(it)
                }
            }

            is LocalEvents.AppendWatchOnlyDescriptor -> {
                appendWatchOnlyDescriptor(event.value)
            }

            is LocalEvents.ImportFile -> {
                importFile(event.source)
            }
        }
    }

    private fun appendWatchOnlyDescriptor(vararg value: String) {
        watchOnlyDescriptor.value = watchOnlyDescriptor.value.trimMargin()
            .let { it + (if (it.isNotBlank()) ",\n" else "") + value.joinToString(",\n") }
    }

    private fun createNewWatchOnlyWallet(
        biometricsCipherProvider: Deferred<PlatformCipher>
    ) {
        doAsync({
            val watchOnlyDescriptors =
                watchOnlyDescriptor.value.takeIf { it.isNotBlank() }?.split("|", "\n")
                    ?.map { it.trim().trimIndent().trimMargin() }?.filter { it.isNotBlank() }
                    ?.toSet()
                    ?.toList()

            val watchOnlyCredentials = if (setupArgs.isSinglesig == true) {
                if (isOutputDescriptors.value || setupArgs.network?.isLiquid == true) {
                    WatchOnlyCredentials(coreDescriptors = watchOnlyDescriptors)
                } else {
                    WatchOnlyCredentials(slip132ExtendedPubkeys = watchOnlyDescriptors)
                }
            } else {
                WatchOnlyCredentials(password = password.value)
            }

            val network = setupArgs.network!!

            val loginData = session.loginWatchOnly(network, username.value, watchOnlyCredentials)

            // First get login credentials before creating the wallet
            val loginCredentials: LoginCredentials? =
                if (isRememberMe.value || setupArgs.isSinglesig == true) {
                    val credentialType: CredentialType
                    val encryptedData: EncryptedData
                    if (withBiometrics.value) {
                        encryptedData = greenKeystore.encryptData(
                            biometricsCipherProvider.await(),
                            watchOnlyCredentials.toString().encodeToByteArray()
                        )
                        credentialType = CredentialType.BIOMETRICS_WATCHONLY_CREDENTIALS
                    } else {
                        encryptedData = greenKeystore.encryptData(
                            watchOnlyCredentials.toString().encodeToByteArray()
                        )
                        credentialType = CredentialType.KEYSTORE_WATCHONLY_CREDENTIALS
                    }

                    createLoginCredentials(
                        walletId = objectId().toString(), // temp
                        network = network.id,
                        credentialType = credentialType,
                        encryptedData = encryptedData
                    )
                } else {
                    null
                }

            // Check if wallet already exists
            database.getWalletWithXpubHashId(
                xPubHashId = loginData.networkHashId,
                isTestnet = network.isTestnet,
                isHardware = false
            )?.also { wallet ->
                throw Exception("id_wallet_already_restored_s|${wallet.name}")
            }

            val wallet = GreenWallet.createWallet(
                name = generateWalletName(settingsManager),
                xPubHashId = loginData.networkHashId, // Use networkHashId as the watch-only is linked to a specific network
                activeNetwork = session.activeAccount.value?.networkId
                    ?: session.defaultNetwork.id,
                activeAccount = session.activeAccount.value?.pointer ?: 0,
                watchOnlyUsername = if (network.isSinglesig) "" else username.value, // empty string helps us hide the username and still identify it as a wo
                isTestnet = network.isTestnet
            ).also {
                database.insertWallet(it)
            }

            loginCredentials?.also {
                database.replaceLoginCredential(it.copy(wallet_id = wallet.id))
            }

            sessionManager.upgradeOnBoardingSessionToWallet(wallet)
            countly.importWallet(session)

            wallet
        }, onSuccess = {
            postSideEffect(SideEffects.NavigateTo(NavigateDestinations.WalletOverview(it)))
        })
    }

    private fun importFile(source: Source) {
        doAsync({
            val xpubs = mutableListOf<String>()
            source.use {
                it.buffer().use {
                    Json.parseToJsonElement(it.readUtf8()).jsonObject.also { json ->
                        val keys = json.keys

                        // Coldcard
                        keys.forEach { key ->

                            (json[key] as? JsonObject)?.also { inner ->
                                // Filter only supported account types
                                inner["name"]?.jsonPrimitive?.content?.also { name ->
                                    if (
                                        name == AccountType.BIP44_LEGACY.gdkType ||
                                        name == AccountType.BIP49_SEGWIT_WRAPPED.gdkType ||
                                        name == AccountType.BIP84_SEGWIT.gdkType ||
                                        name == AccountType.BIP86_TAPROOT.gdkType
                                    ) {
                                        ((inner["_pub"] as? JsonPrimitive)
                                            ?: (inner["xpub"] as? JsonPrimitive))?.content?.also { xpub ->
                                            xpubs += xpub
                                        }
                                    }
                                }
                            }
                        }

                        // Electrum
                        ((json["keystore"] as? JsonObject)?.get("xpub") as? JsonPrimitive)?.content?.also { xpub ->
                            xpubs += xpub
                        }
                    }
                }
            }

            if (xpubs.isEmpty()) {
                throw Exception("id_format_is_not_supported_or_no_data")
            }

            xpubs
        }, onSuccess = {
            appendWatchOnlyDescriptor(*it.toTypedArray())
        })
    }
}

class WatchOnlyCredentialsViewModelPreview(setupArgs: SetupArgs, isLiquid : Boolean = false) :
    WatchOnlyCredentialsViewModelAbstract(setupArgs = setupArgs) {
        
    override val isSinglesig: StateFlow<Boolean> = MutableStateFlow(true)
    override val isLiquid: StateFlow<Boolean> = MutableStateFlow(isLiquid)
    override val isLoginEnabled: StateFlow<Boolean> = MutableStateFlow(false)
    override val canUseBiometrics: StateFlow<Boolean> = MutableStateFlow(true)
    override val username: MutableStateFlow<String> = MutableStateFlow("")
    override val password: MutableStateFlow<String> = MutableStateFlow("")
    override val watchOnlyDescriptor: MutableStateFlow<String> = MutableStateFlow("")
    override val isOutputDescriptors: MutableStateFlow<Boolean> = MutableStateFlow(isLiquid)
    override val isRememberMe: MutableStateFlow<Boolean> = MutableStateFlow(false)
    override val withBiometrics: MutableStateFlow<Boolean> = MutableStateFlow(false)

    companion object {
        fun preview(isSinglesig: Boolean = true, isLiquid: Boolean = false) =
            WatchOnlyCredentialsViewModelPreview(SetupArgs(mnemonic = "neutral inherit learn", isSinglesig = isSinglesig), isLiquid = isLiquid)
    }
}