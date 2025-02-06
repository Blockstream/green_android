package com.blockstream.common.data

import com.blockstream.common.devices.DeviceModel
import com.blockstream.common.gdk.GdkSession
import com.blockstream.common.gdk.data.Network

data class ErrorReport(
    val subject: String? = null,
    val error: String,
    val supportId: String? = null,
    val paymentHash: String? = null,
    val zendeskSecurityPolicy: String? = null,
    val zendeskHardwareWallet: String? = null
) {
    companion object {
        fun create(throwable: Throwable, network: Network? = null, paymentHash: String? = null, session: GdkSession? = null): ErrorReport{
            return ErrorReport(
                error = throwable.message ?: "Undefined Error",
                supportId = session?.supportId(),
                paymentHash = paymentHash,
                zendeskSecurityPolicy = network?.let {
                    when {
                        it.isSinglesig -> "singlesig__green_"
                        it.isMultisig -> "multisig_shield__green_"
                        it.isLightning -> "lightning__green_"
                        else -> ""
                    }
                },
                zendeskHardwareWallet = session?.let {
                    session.gdkHwWallet?.model?.let {
                        when(it) {
                            DeviceModel.BlockstreamGeneric, DeviceModel.BlockstreamJade -> "jade"
                            DeviceModel.BlockstreamJadePlus -> "jade_plus"
                            DeviceModel.TrezorModelT -> "trezor_t"
                            DeviceModel.TrezorModelOne -> "trezor_one"
                            DeviceModel.LedgerNanoS -> "ledger_nano_s"
                            DeviceModel.LedgerNanoX -> "ledger_nano_x"
                            DeviceModel.TrezorGeneric -> "trezor"
                            DeviceModel.LedgerGeneric -> "ledger"
                            DeviceModel.Generic -> "generic"
                            DeviceModel.SatochipGeneric -> "satochip"
                        }
                    }
                }
            )
        }

        fun createForMultisig(subject: String?): ErrorReport{
            return ErrorReport(subject = subject, error = "", zendeskSecurityPolicy = "multisig_shield__green_")
        }
    }
}