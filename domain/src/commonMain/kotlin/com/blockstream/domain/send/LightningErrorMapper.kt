package com.blockstream.domain.send

import lwk.LwkException

fun mapLightningSendError(error: Throwable): String {
    error.message?.takeIf { it.startsWith("id_") }?.let { return it }

    when (error) {
        is LwkException.SwapExpired -> return "id_swap_expired_please_retry"
        is LwkException.BoltzBackendHttpException ->
            return if (error.status.toInt() in 500..599) {
                "id_lightning_service_unavailable"
            } else {
                "id_your_transaction_could_not_reach_network"
            }
        is LwkException.NoBoltzUpdate -> return "id_lightning_payment_timed_out"
    }

    val msg = error.message?.lowercase() ?: return "id_your_transaction_could_not_reach_network"
    return when {
        "no route" in msg || "noroute" in msg ||
            "insufficient route" in msg || "insufficient routing" in msg -> "id_no_route_to_recipient"
        "timeout" in msg || "timed out" in msg -> "id_lightning_payment_timed_out"
        "expired" in msg -> "id_invoice_expired"
        "already paid" in msg -> "id_invoice_already_paid"
        "insufficient funds" in msg || "insufficient balance" in msg -> "id_insufficient_funds"
        "invalid amount" in msg -> "id_invalid_amount"
        else -> "id_your_transaction_could_not_reach_network"
    }
}
