package com.blockstream.data.data

enum class CredentialType(val value: Long) {
    PIN_PINDATA(0),
    BIOMETRICS_PINDATA(1), // Crypto based
    KEYSTORE_PASSWORD(2), // Deprecated, use WatchOnlyCredentials
    PASSWORD_PINDATA(3), // It's a variable length PIN (string), based on greenbits v2
    KEYSTORE_WATCHONLY_CREDENTIALS(4),
    BIOMETRICS_WATCHONLY_CREDENTIALS(5), // Crypto based
    KEYSTORE_GREENLIGHT_CREDENTIALS(6),
    LIGHTNING_MNEMONIC(7),
    RICH_WATCH_ONLY(8),
    KEYSTORE_MNEMONIC(9), // not used
    BIOMETRICS_MNEMONIC(10), // Keystore based with user presence
    KEYSTORE_HW_WATCHONLY_CREDENTIALS(11),
    BOLTZ_MNEMONIC(12);

    companion object {
        fun byPosition(position: Long): CredentialType {
            return CredentialType.entries[position.toInt()]
        }
    }
}
