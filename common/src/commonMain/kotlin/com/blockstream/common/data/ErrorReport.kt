package com.blockstream.common.data

import com.blockstream.common.gdk.GdkSession
import com.blockstream.common.gdk.data.Network

data class ErrorReport private constructor(val error: String, val supportId: String? = null, val zendeskSecurityPolicy: String? = null, val zendeskHardwareWallet: String? = null){
    companion object {
        fun create(throwable: Throwable, network: Network? = null, session: GdkSession? = null): ErrorReport{
            return ErrorReport(
                error = throwable.message ?: "Undefined Error",
                supportId = session?.supportId(),
                zendeskSecurityPolicy = network?.let {
                    when {
                        it.isSinglesig -> "singlesig__green_"
                        it.isMultisig -> "multisig_shield__green_"
                        it.isLightning -> "lightning__green_"
                        else -> ""
                    }
                },
                zendeskHardwareWallet = session?.let {
                    session.gdkHwWallet?.model?.lowercase()?.let {
                        when{
                            it.contains("jade") -> "jade"
                            it.contains("ledger") && it.contains("s")-> "ledger_nano_s"
                            it.contains("ledger") && it.contains("x")-> "ledger_nano_x"
                            it.contains("trezor") && it.contains("one")-> "trezor_one"
                            it.contains("trezor") -> "trezor_t"
                            else -> null
                        }
                    }
                }
            )
        }

        fun createForMultisig(): ErrorReport{
            return ErrorReport(error = "", zendeskSecurityPolicy = "multisig_shield__green_")
        }
    }
}