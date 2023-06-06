package com.blockstream.common.gdk

import com.blockstream.common.gdk.JsonConverter.Companion.JsonDeserializer
import com.blockstream.common.gdk.data.AuthHandlerStatus
import com.blockstream.common.gdk.data.DeviceRequiredData
import com.blockstream.common.gdk.data.Network
import com.blockstream.common.gdk.device.DeviceResolver
import com.blockstream.common.gdk.device.GdkHardwareWallet
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.decodeFromJsonElement

interface TwoFactorResolver {
    suspend fun selectMethod(availableMethods: List<String>): CompletableDeferred<String>

    suspend fun getCode(authHandlerStatus: AuthHandlerStatus): CompletableDeferred<String>
}

interface HardwareWalletResolver {
    fun requestDataFromDevice(network: Network, requiredData: DeviceRequiredData): CompletableDeferred<String>
}

class AuthHandler constructor(
    private var gaAuthHandler: GAAuthHandler,
    private val network: Network,
    private val gdkHwWallet: GdkHardwareWallet?,
    private val gdk: Gdk
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

    fun resolve(
        twoFactorResolver: TwoFactorResolver? = null,
        hardwareWalletResolver: HardwareWalletResolver? = null
    ): AuthHandler {
        try {
            while (!isCompleted) {

                val authHandlerStatus: AuthHandlerStatus = gdk.getAuthHandlerStatus(gaAuthHandler)

                when (authHandlerStatus.status) {
                    CALL -> {
                        call()
                    }
                    REQUEST_CODE -> {
                        twoFactorResolver?.also {

                            if(authHandlerStatus.methods.size == 1) {
                                requestCode(authHandlerStatus.methods.first())
                            }else{
                                try {
                                    requestCode(runBlocking { it.selectMethod(authHandlerStatus.methods).await() })
                                }catch (e: Exception){
                                    throw Exception("id_action_canceled")
                                }
                            }

                        } ?: run {
                            throw RuntimeException("TwoFactorMethodResolver was not provided")
                        }

                    }
                    RESOLVE_CODE -> {
                        if (authHandlerStatus.requiredData == null) {
                            twoFactorResolver?.also {
                                try {
                                    resolveCode(runBlocking {
                                        it.getCode(authHandlerStatus).await()
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
                                    dataFromDevice = runBlocking{ it.requestDataFromDevice(network, authHandlerStatus.requiredData).await() }
                                } catch (e: Exception){
                                    // eg. signing a message in Trezor on testnet network
                                    if(e.message?.lowercase()?.contains("cancelled") == true){
                                        throw Exception("id_action_canceled")
                                    }else{
                                        throw e
                                    }
                                }

                                try {
                                    resolveCode(dataFromDevice)
                                }catch (e: Exception){
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

    inline fun <reified T> result(
        twoFactorResolver: TwoFactorResolver? = null,
        hardwareWalletResolver: HardwareWalletResolver? = null
    ): T {
        if (!isCompleted) {
            resolve(twoFactorResolver, hardwareWalletResolverOrDefault(hardwareWalletResolver))
        }

        return result?.let { result ->
            JsonDeserializer.decodeFromJsonElement<T>(result).let{
                if(it is GdkJson<*> && it.keepJsonElement) {
                    it.jsonElement = result
                }
                it
            }
        } ?: throw RuntimeException("This call does not provide any result")
    }

    companion object {
        const val CALL = "call"
        const val DONE = "done"
        const val ERROR = "error"
        const val REQUEST_CODE = "request_code"
        const val RESOLVE_CODE = "resolve_code"
    }
}
