package com.blockstream.green

class TestData {

    companion object {

        const val RECOVERY_PHRASE_12 =
            "will isolate mixture scene enter pond horse squirrel color squirrel strategy sword"
        const val RECOVERY_PHRASE_18 =
            "audit will mean flush point laugh screen swift popular mean rival large jelly stock robot fame pretty achieve"
        const val RECOVERY_PHRASE_24 =
            "law midnight pear solar real excuse decrease cart budget can fork shine fantasy feature reduce width dress human mesh gravity royal similar surge guess"
        const val RECOVERY_PHRASE_GREEN_PASSWORD =
            "jealous awesome suit raise math chunk much doctor long client charge good error arrange estate zebra industry ancient attend barrel social welcome shell wall final loyal uphold"


        const val RECOVERY_PHRASE_12_INVALID =
            "will isolate mixture scene enter pond horse squirrel squirrel strategy sword color"

        val recoveryPhrases by lazy {
            listOf(RECOVERY_PHRASE_12, RECOVERY_PHRASE_18, RECOVERY_PHRASE_24)
        }

        val recoveryPhrasesInvalid by lazy {
            recoveryPhrases.map {
                it.split(" ").sorted().joinToString(" ")
            }
        }
    }
}