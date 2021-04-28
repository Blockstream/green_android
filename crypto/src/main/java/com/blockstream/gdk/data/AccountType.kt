package com.blockstream.gdk.data


enum class AccountType(val gdkType: String) {
    STANDARD("2of2"), AMP_ACCOUNT("2of2_no_recovery"), TWO_OF_THREE("2of3"), UNKNOWN("unknown");

    override fun toString(): String = gdkType

    companion object {
        fun byGDKType(name: String) = when(name){
            "2of2" -> STANDARD
            "2of2_no_recovery" -> AMP_ACCOUNT
            "2of3" -> TWO_OF_THREE
            else -> UNKNOWN
        }
    }
}