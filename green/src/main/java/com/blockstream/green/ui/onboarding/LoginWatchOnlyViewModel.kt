package com.blockstream.green.ui.onboarding

import android.annotation.SuppressLint
import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import androidx.lifecycle.*
import com.blockstream.gdk.data.AccountType
import com.blockstream.green.data.Countly
import com.blockstream.green.data.OnboardingOptions
import com.blockstream.green.database.WalletRepository
import com.blockstream.green.database.WatchOnlyCredentials
import com.blockstream.green.extensions.boolean
import com.blockstream.green.extensions.string
import com.blockstream.green.managers.SessionManager
import com.blockstream.green.utils.AppKeystore
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Deferred
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import javax.crypto.Cipher

class LoginWatchOnlyViewModel @AssistedInject constructor(
    @SuppressLint("StaticFieldLeak")
    @ApplicationContext val context: Context,
    sessionManager: SessionManager,
    walletRepository: WalletRepository,
    countly: Countly,
    private val appKeystore: AppKeystore,
    @Assisted val onboardingOptions: OnboardingOptions
) : OnboardingViewModel(sessionManager, walletRepository, countly, null) {

    val canUseBiometrics = appKeystore.canUseBiometrics(context)

    var username = MutableLiveData("")
    var password = MutableLiveData("")
    var watchOnlyDescriptor = MutableLiveData("")
    var isOutputDescriptors = MutableLiveData(false)
    val isRememberMe = MutableLiveData(true)
    val withBiometrics = MutableLiveData(canUseBiometrics)

    val isLoginEnabled: LiveData<Boolean> by lazy {
        MediatorLiveData<Boolean>().apply {
            val block = { _: Any? ->
                value = if (onboardingOptions.isSinglesig == true) {
                    !watchOnlyDescriptor.value.isNullOrBlank()
                } else {
                    !username.value.isNullOrBlank() && !password.value.isNullOrBlank() && !onProgress.value!!
                }
            }
            if (onboardingOptions.isSinglesig == true) {
                addSource(watchOnlyDescriptor, block)
            } else {
                addSource(username, block)
                addSource(password, block)
            }

            addSource(onProgress, block)
        }
    }

    fun appendWatchOnlyDescriptor(vararg value: String) {
        watchOnlyDescriptor.value = watchOnlyDescriptor.string().trimMargin()
            .let { it + (if (it.isNotBlank()) ",\n" else "") + value.joinToString(",\n") }

        if (watchOnlyDescriptor.string().startsWith("sh(")) {
            isOutputDescriptors.value = true
        }
    }

    fun createNewWatchOnlyWallet(biometricsCipherProvider: Deferred<Cipher>) {

        val watchOnlyDescriptors =
            watchOnlyDescriptor.string().takeIf { it.isNotBlank() }?.split(",", "\n")
                ?.map { it.trimIndent().trimMargin() }?.filter { it.isNotBlank() }?.toSet()
                ?.toList()

        val watchOnlyCredentials = if (onboardingOptions.isSinglesig == true) {
            if (isOutputDescriptors.boolean()) {
                WatchOnlyCredentials(coreDescriptors = watchOnlyDescriptors)
            } else {
                WatchOnlyCredentials(slip132ExtendedPubkeys = watchOnlyDescriptors)
            }
        } else {
            WatchOnlyCredentials(password = password.string())
        }

        createNewWatchOnlyWallet(
            appKeystore = appKeystore,
            options = onboardingOptions,
            username = username.string(),
            watchOnlyCredentials = watchOnlyCredentials,
            multisigSavePassword = isRememberMe.boolean(),
            isBiometrics = withBiometrics.boolean(),
            biometricsCipherProvider = biometricsCipherProvider
        )
    }

    fun importFile(contentResolver: ContentResolver, uri: Uri) {
        doUserAction({
            val xpubs = mutableListOf<String>()

            contentResolver.openInputStream(uri)?.use {
                it.bufferedReader().use {
                    Json.parseToJsonElement(it.readText()).jsonObject.also { json ->
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

            xpubs
        }, onSuccess = {
            appendWatchOnlyDescriptor(*it.toTypedArray())
        })
    }

    @dagger.assisted.AssistedFactory
    interface AssistedFactory {
        fun create(onboardingOptions: OnboardingOptions): LoginWatchOnlyViewModel
    }

    companion object {
        fun provideFactory(
            assistedFactory: AssistedFactory,
            onboardingOptions: OnboardingOptions
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return assistedFactory.create(onboardingOptions) as T
            }
        }
    }
}