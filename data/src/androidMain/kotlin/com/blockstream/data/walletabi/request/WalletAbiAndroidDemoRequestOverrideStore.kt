package com.blockstream.data.walletabi.request

import android.content.Context

class WalletAbiAndroidDemoRequestOverrideStore(
    context: Context
) : WalletAbiDemoRequestOverrideStore {
    private val sharedPreferences = context.getSharedPreferences(
        SHARED_PREFERENCES_NAME,
        Context.MODE_PRIVATE
    )

    override fun consumeRequestEnvelope(): String? {
        val requestEnvelope = sharedPreferences.getString(KEY_REQUEST_ENVELOPE, null)
            ?.takeIf { it.isNotBlank() }
            ?: return null

        sharedPreferences.edit()
            .remove(KEY_REQUEST_ENVELOPE)
            .apply()
        return requestEnvelope
    }

    companion object {
        private const val SHARED_PREFERENCES_NAME = "wallet_abi_demo_request_override"
        private const val KEY_REQUEST_ENVELOPE = "request_envelope"

        fun writeRequestEnvelope(
            context: Context,
            requestEnvelope: String
        ) {
            context.getSharedPreferences(
                SHARED_PREFERENCES_NAME,
                Context.MODE_PRIVATE
            ).edit()
                .putString(KEY_REQUEST_ENVELOPE, requestEnvelope)
                .apply()
        }

        fun clear(context: Context) {
            context.getSharedPreferences(
                SHARED_PREFERENCES_NAME,
                Context.MODE_PRIVATE
            ).edit()
                .remove(KEY_REQUEST_ENVELOPE)
                .apply()
        }
    }
}
