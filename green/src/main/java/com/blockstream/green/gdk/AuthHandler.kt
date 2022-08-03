package com.blockstream.green.gdk

import com.blockstream.gdk.GAJson
import com.blockstream.gdk.GdkBridge
import com.blockstream.gdk.GdkBridge.Companion.JsonDeserializer
import com.blockstream.gdk.data.DeviceRequiredData
import com.blockstream.gdk.data.Network
import com.blockstream.gdk.data.TwoFactorStatus
import com.blockstream.green.devices.DeviceResolver
import com.blockstream.libgreenaddress.GAAuthHandler
import io.reactivex.rxjava3.core.Single
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.decodeFromJsonElement

interface TwoFactorResolver {
    suspend fun selectMethod(availableMethods: List<String>): CompletableDeferred<String>

    suspend fun getCode(twoFactorStatus: TwoFactorStatus): CompletableDeferred<String>
}

interface HardwareWalletResolver {
    fun requestDataFromDevice(network: Network, requiredData: DeviceRequiredData): Single<String>
}

class AuthHandler constructor(
    private val network: Network,
    private val session: GdkSession,
    private val gdkBridge: GdkBridge,
    private var gaAuthHandler: GAAuthHandler
) {
    var isCompleted = false
        private set

    var result: JsonElement? = null

    private fun call() = gdkBridge.authHandlerCall(gaAuthHandler)
    private fun requestCode(method: String) =
        gdkBridge.authHandlerRequestCode(method, gaAuthHandler)

    private fun resolveCode(code: String) = gdkBridge.authHandlerResolveCode(code, gaAuthHandler)
    private fun destroy() = gdkBridge.destroyAuthHandler(gaAuthHandler)

    fun hardwareWalletResolverOrDefault(hardwareWalletResolver: HardwareWalletResolver? = null): HardwareWalletResolver? {
        return hardwareWalletResolver ?: session.device?.let { device -> DeviceResolver(device.hwWallet) }
    }

    fun resolve(
        twoFactorResolver: TwoFactorResolver? = null,
        hardwareWalletResolver: HardwareWalletResolver? = null
    ): AuthHandler {
        try {
            while (!isCompleted) {

                val twoFactorStatus: TwoFactorStatus =
                    gdkBridge.getAuthHandlerStatus(gaAuthHandler).let { jsonElement ->
                        JsonDeserializer.decodeFromJsonElement<TwoFactorStatus>(jsonElement)
                            .also { twoFactorStatus ->

                                // Save the raw JsonElement for further processing if needed by v3 code
                                twoFactorStatus.jsonElement = jsonElement
                            }
                    }


                when (twoFactorStatus.status) {
                    CALL -> {
                        call()
                    }
                    REQUEST_CODE -> {
                        twoFactorResolver?.also {

                            if(twoFactorStatus.methods.size == 1) {
                                requestCode(twoFactorStatus.methods.first())
                            }else{
                                try {
                                    requestCode(runBlocking { it.selectMethod(twoFactorStatus.methods).await() })
                                }catch (e: Exception){
                                    throw Exception("id_action_canceled")
                                }
                            }

                        } ?: run {
                            throw RuntimeException("TwoFactorMethodResolver was not provided")
                        }

                    }
                    RESOLVE_CODE -> {
                        if(twoFactorStatus.requiredData == null){
                            twoFactorResolver?.also {
                                try {
                                    resolveCode(runBlocking { it.getCode(twoFactorStatus).await() })
                                } catch (e: Exception){
                                    throw Exception("id_action_canceled")
                                }
                            } ?: run {
                                throw RuntimeException("TwoFactorCodeResolver was not provided")
                            }
                        }else{
                            hardwareWalletResolverOrDefault(hardwareWalletResolver)?.also {
                                val dataFromDevice: String?

                                try {
                                     dataFromDevice = it.requestDataFromDevice(network, twoFactorStatus.requiredData!!).blockingGet()
                                }catch (e: Exception){
                                    // TODO Handle all cancel exceptions so that we can catch the exceptions from the hardware wallet.
                                    // eg. signing a message in Trezor on testnet network
                                    if(e.message?.lowercase()?.contains("cancelled") == true){
                                        throw Exception("id_action_canceled")
                                    }else{
                                        e.printStackTrace()
                                        throw e
                                    }
                                }

                                try {
                                    resolveCode(dataFromDevice)
                                }catch (e: Exception){
                                    e.printStackTrace()
                                    throw e
                                }
                            } ?: run {
                                throw RuntimeException("TwoFactorCodeResolver was not provided")
                            }
                        }
                    }
                    ERROR -> {
                        isCompleted = true
                        throw Exception(twoFactorStatus.error)
                    }
                    DONE -> {
                        isCompleted = true
                        result = twoFactorStatus.result
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
                if(it is GAJson<*> && it.keepJsonElement) {
                    it.jsonElement = result
                }
                it
            }
        } ?: throw Exception("nothing is resolved")
    }

    companion object {
        const val CALL = "call"
        const val DONE = "done"
        const val ERROR = "error"
        const val REQUEST_CODE = "request_code"
        const val RESOLVE_CODE = "resolve_code"
    }
}
