package com.blockstream.data.notifications.models

enum class MeldNotificationType {
    MELD_TRANSACTION, UNKNOWN;

    companion object Companion {
        fun valueOfOrUnknown(value: String?): MeldNotificationType {
            return when (value) {
                "MELD_TRANSACTION" -> MELD_TRANSACTION
                else -> UNKNOWN
            }
        }

    }
}

