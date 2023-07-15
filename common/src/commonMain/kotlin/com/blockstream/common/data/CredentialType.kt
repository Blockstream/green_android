package com.blockstream.common.data

enum class CredentialType(val value: Long) {
    PIN_PINDATA(0),
    BIOMETRICS_PINDATA(1),
    KEYSTORE_PASSWORD(2), // Deprecated, use WatchOnlyCredentials
    PASSWORD_PINDATA(3), // It's a variable length PIN (string), based on greenbits v2
    KEYSTORE_WATCHONLY_CREDENTIALS(4),
    BIOMETRICS_WATCHONLY_CREDENTIALS(5),
    KEYSTORE_GREENLIGHT_CREDENTIALS(6),
    LIGHTNING_MNEMONIC(7);

    companion object{
        fun byPosition(position: Long): CredentialType {
            return CredentialType.entries[position.toInt()]
        }
    }
}
