package com.blockstream.data.gdk

import com.blockstream.data.gdk.JsonConverter.Companion.JsonDeserializer
import com.blockstream.data.gdk.data.AuthHandlerStatus
import com.blockstream.data.gdk.data.DeviceRequiredData
import com.blockstream.data.gdk.data.Network
import com.blockstream.data.gdk.device.DeviceResolver
import com.blockstream.data.gdk.device.GdkHardwareWallet
import com.blockstream.utils.Loggable
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.decodeFromJsonElement

interface TwoFactorResolver {
    suspend fun selectTwoFactorMethod(availableMethods: List<String>): CompletableDeferred<String>

    suspend fun getTwoFactorCode(
        network: Network,
        enable2faCallMethod: Boolean,
        authHandlerStatus: AuthHandlerStatus
    ): CompletableDeferred<String>
}

fun TwoFactorResolver.selectTwoFactorMethod(method: String): TwoFactorResolver {
    return object : TwoFactorResolver by this {
        override suspend fun selectTwoFactorMethod(availableMethods: List<String>): CompletableDeferred<String> =
            CompletableDeferred(method)
    }
}

interface HardwareWalletResolver {
    fun requestDataFromDevice(network: Network, requiredData: DeviceRequiredData): CompletableDeferred<String>
}

interface BcurResolver {
    fun requestData(): CompletableDeferred<String>
    fun progress(progress: Int): Unit
}

class AuthHandler constructor(
    private val session: GdkSession,
    private var gaAuthHandler: GAAuthHandler,
    private val network: Network,
    private val gdkHwWallet: GdkHardwareWallet?,
    private val gdk: GdkBinding
) {
    var isCompleted = false
        private set

    var result: JsonElement? = null

    private fun call() = gdk.authHandlerCall(gaAuthHandler)
    private fun requestCode(method: String) =
        gdk.authHandlerRequestCode(method, gaAuthHandler)

    private fun resolveCode(code: String) = gdk.authHandlerResolveCode(code, gaAuthHandler)
    private fun destroy() = gdk.destroyAuthHandler(gaAuthHandler)

    fun hardwareWalletResolverOrDefault(hardwareWalletResolver: HardwareWalletResolver? = null): HardwareWalletResolver? {
        return hardwareWalletResolver ?: gdkHwWallet?.let { gdkHwWallet -> DeviceResolver(gdkHwWallet) }
    }

    suspend fun resolve(
        twoFactorResolver: TwoFactorResolver? = null,
        hardwareWalletResolver: HardwareWalletResolver? = null,
        bcurResolver: BcurResolver? = null
    ): AuthHandler {
        try {
            while (!isCompleted) {

                val authHandlerStatus: AuthHandlerStatus = gdk.getAuthHandlerStatus(gaAuthHandler)

                authHandlerStatus.progress?.also { bcurResolver?.progress(it) }

                when (authHandlerStatus.status) {
                    CALL -> {
                        call()
                    }

                    REQUEST_CODE -> {
                        twoFactorResolver?.also {

                            if (authHandlerStatus.methods.size == 1) {
                                requestCode(authHandlerStatus.methods.first())
                            } else {
                                try {
                                    requestCode(it.selectTwoFactorMethod(authHandlerStatus.methods).await())
                                } catch (e: Exception) {
                                    throw Exception("id_action_canceled")
                                }
                            }

                        } ?: run {
                            throw RuntimeException("TwoFactorMethodResolver was not provided")
                        }

                    }

                    RESOLVE_CODE -> {
                        if (authHandlerStatus.requiredData == null) {
                            bcurResolver?.also {
                                resolveCode(it.requestData().await())
                            } ?: twoFactorResolver?.also {
                                try {
                                    resolveCode(runBlocking {
                                        it.getTwoFactorCode(
                                            network = network,
                                            enable2faCallMethod = session.getTwoFactorConfig(
                                                network = network
                                            )?.enabledMethods?.let { it.size == 1 && it.firstOrNull() == "sms" } ?: false,
                                            authHandlerStatus = authHandlerStatus
                                        ).await()
                                    })
                                } catch (e: Exception) {
                                    throw Exception("id_action_canceled")
                                }
                            } ?: run {
                                throw RuntimeException("TwoFactorCodeResolver was not provided")
                            }
                        } else {
                            hardwareWalletResolverOrDefault(hardwareWalletResolver)?.also {
                                val dataFromDevice: String?

                                try {
                                    dataFromDevice =
                                        runBlocking { it.requestDataFromDevice(network, authHandlerStatus.requiredData).await() }
                                } catch (e: Exception) {
                                    // eg. signing a message in Trezor on testnet network
                                    if (e.message?.lowercase()?.contains("cancelled") == true) {
                                        throw Exception("id_action_canceled")
                                    } else {
                                        throw e
                                    }
                                }

                                try {
                                    resolveCode(dataFromDevice)
                                } catch (e: Exception) {
                                    throw e
                                }
                            } ?: run {
                                throw RuntimeException("TwoFactorCodeResolver was not provided")
                            }
                        }
                    }

                    ERROR -> {
                        isCompleted = true
                        throw Exception(authHandlerStatus.error)
                    }

                    DONE -> {
                        isCompleted = true
                        result = authHandlerStatus.result
                    }
                }
            }

        } finally {
            destroy()
        }

        return this
    }

    suspend inline fun <reified T> result(
        twoFactorResolver: TwoFactorResolver? = null,
        hardwareWalletResolver: HardwareWalletResolver? = null,
        bcurResolver: BcurResolver? = null
    ): T {
        if (!isCompleted) {
            resolve(twoFactorResolver, hardwareWalletResolverOrDefault(hardwareWalletResolver), bcurResolver)
        }

        return result?.let { result ->
            JsonDeserializer.decodeFromJsonElement<T>(result).let {
                if (it is GreenJson<*> && it.keepJsonElement()) {
                    it.jsonElement = result
                    it.processJsonElement()
                }
                it
            }
        } ?: throw RuntimeException("This call does not provide any result")
    }

    companion object : Loggable() {
        const val CALL = "call"
        const val DONE = "done"
        const val ERROR = "error"
        const val REQUEST_CODE = "request_code"
        const val RESOLVE_CODE = "resolve_code"
    }
}
