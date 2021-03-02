package com.blockstream.gdk

import com.blockstream.gdk.GreenWallet.Companion.JsonDeserializer
import com.blockstream.gdk.data.TwoFactorStatus
import com.blockstream.libgreenaddress.GAAuthHandler
import io.reactivex.rxjava3.core.Single
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.decodeFromJsonElement

interface TwoFactorResolver {
    fun selectMethod(availableMethods: List<String>): Single<String>
    fun getCode(method: String): Single<String>
}

interface HardwareWalletResolver {
    fun dataFromDevice(method: String): Single<String>
}

class AuthHandler(
    private val greenWallet: GreenWallet,
    private var gaAuthHandler: GAAuthHandler
) {
    private var isDone = false
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
        while (!isDone) {
            val twoFactorStatus: TwoFactorStatus = JsonDeserializer.decodeFromJsonElement(
                greenWallet.getAuthHandlerStatus(gaAuthHandler)
            )

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
                                destroy()
                                throw Exception("id_action_canceled")
                            }
                        }

                    } ?: run {
                        TODO("TwoFactorMethodResolver was not provided")
                    }

                }
                RESOLVE_CODE -> {

                    twoFactorResolver?.also {

                        try {
                            resolveCode(it.getCode(twoFactorStatus.method).blockingGet())
                        }catch (e: Exception){
                            destroy()
                            throw Exception("id_action_canceled")
                        }

                    } ?: run {
                        TODO("TwoFactorCodeResolver was not provided")
                    }
                }
                ERROR -> {
                    destroy()
                    isDone = true
                    throw Exception(twoFactorStatus.error)
                }
                DONE -> {
                    destroy()
                    isDone = true
                    result = twoFactorStatus.result
                }
            }
        }

        return this
    }

    fun isCompleted() = isDone

    inline fun <reified T> result(
        twoFactorResolver: TwoFactorResolver? = null,
        hardwareWalletResolver: HardwareWalletResolver? = null
    ): T {
        if (!isCompleted()) {
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