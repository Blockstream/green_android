package com.blockstream.gdk.data


enum class AccountType(val gdkType: String) {
    // Mutlisig
    STANDARD("2of2"),
    AMP_ACCOUNT("2of2_no_recovery"),
    TWO_OF_THREE("2of3"),

    // Singlesig
    BIP44_LEGACY("p2pkh"),
    BIP49_SEGWIT_WRAPPED("p2sh-p2wpkh"),
    BIP84_SEGWIT("p2wpkh"),

    UNKNOWN("unknown");

    override fun toString(): String = gdkType

    companion object {
        fun byGDKType(name: String) = when(name){
            "2of2" -> STANDARD
            "2of2_no_recovery" -> AMP_ACCOUNT
            "2of3" -> TWO_OF_THREE
            "p2pkh" -> BIP44_LEGACY
            "p2sh-p2wpkh" -> BIP49_SEGWIT_WRAPPED
            "p2wpkh" -> BIP84_SEGWIT
            else -> UNKNOWN
        }
    }
}