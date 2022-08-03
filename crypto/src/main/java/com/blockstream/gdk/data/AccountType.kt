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
    BIP86_TAPROOT("p2tr"),

    LIGHTNING("lightning"),

    UNKNOWN("unknown");

    override fun toString(): String = when(this){
        BIP44_LEGACY -> "Legacy"
        BIP49_SEGWIT_WRAPPED -> "Legacy"
        BIP84_SEGWIT -> "SegWit"
        BIP86_TAPROOT -> "Taproot"
        LIGHTNING -> "Lightning"
        else -> gdkType
    }

    fun isSinglesig() = when (this) {
        BIP44_LEGACY, BIP49_SEGWIT_WRAPPED, BIP84_SEGWIT, BIP86_TAPROOT -> true
        else -> false
    }

    fun isMutlisig() = !isSinglesig()

    companion object {
        fun byGDKType(name: String) = when(name){
            "2of2" -> STANDARD
            "2of2_no_recovery" -> AMP_ACCOUNT
            "2of3" -> TWO_OF_THREE
            "p2pkh" -> BIP44_LEGACY
            "p2sh-p2wpkh" -> BIP49_SEGWIT_WRAPPED
            "p2wpkh" -> BIP84_SEGWIT
            "p2tr" -> BIP86_TAPROOT
            else -> UNKNOWN
        }
    }
}