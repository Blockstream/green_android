package com.blockstream.gdk

import com.blockstream.gdk.GreenWallet.Companion.JsonDeserializer
import com.blockstream.gdk.data.DeviceRequiredData
import com.blockstream.gdk.data.TwoFactorStatus
import com.blockstream.libgreenaddress.GAAuthHandler
import com.greenaddress.greenapi.data.HWDeviceRequiredData
import io.reactivex.rxjava3.core.Single
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.decodeFromJsonElement

interface TwoFactorResolver {
    fun selectMethod(availableMethods: List<String>): Single<String>
    fun getCode(method: String, attemptsRemaining: Int?): Single<String>
}

interface HardwareWalletResolver {
    fun requestDataFromDeviceV3(requiredData: HWDeviceRequiredData): Single<String>
    fun requestDataFromDevice(requiredData: DeviceRequiredData): Single<String>
}

class AuthHandler(
    private val greenWallet: GreenWallet,
    private var gaAuthHandler: GAAuthHandler
) {
    var isCompleted = false
        private set

    var result: JsonElement? = null

    private fun call() = greenWallet.authHandlerCall(gaAuthHandler)
    private fun requestCode(method: String) =
        greenWallet.authHandlerRequestCode(method, gaAuthHandler)

    private fun resolveCode(code: String) = greenWallet.authHandlerResolveCode(code, gaAuthHandler)
    private fun destroy() = greenWallet.destroyAuthHandler(gaAuthHandler)

    fun resolve(
        twoFactorResolver: TwoFactorResolver? = null,
        hardwareWalletResolver: HardwareWalletResolver? = null
    ): AuthHandler {

        try {

            while (!isCompleted) {

                val twoFactorStatus: TwoFactorStatus =
                    greenWallet.getAuthHandlerStatus(gaAuthHandler).let { jsonElement ->
                        JsonDeserializer.decodeFromJsonElement<TwoFactorStatus>(jsonElement)
                            .also { twoFactorStatus ->

                                // Save the raw JsonElement for further processing if needed by v3 code
                                twoFactorStatus.rawJsonElement = jsonElement
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
                                    requestCode(it.selectMethod(twoFactorStatus.methods).blockingGet())
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
                                    resolveCode(it.getCode(twoFactorStatus.method, twoFactorStatus.attemptsRemaining).blockingGet())
                                } catch (e: Exception){
                                    e.printStackTrace()
                                    throw Exception("id_action_canceled")
                                }
                            } ?: run {
                                throw RuntimeException("TwoFactorCodeResolver was not provided")
                            }
                        }else{
                            hardwareWalletResolver?.also {
                                try {
                                    // Use v3 codebase for HWW handling
                                    resolveCode(it.requestDataFromDeviceV3(twoFactorStatus.getTwoFactorStatusDataV3().requiredData).blockingGet())

                                    // Needs v4 implementation
                                    // resolveCode(it.requestDataFromDevice(twoFactorStatus.requiredData!!).blockingGet())
                                }catch (e: Exception){
                                    e.printStackTrace()
                                    throw Exception("id_action_canceled")
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
            resolve(twoFactorResolver, hardwareWalletResolver)
        }

        return result?.let {
            JsonDeserializer.decodeFromJsonElement(it)
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
