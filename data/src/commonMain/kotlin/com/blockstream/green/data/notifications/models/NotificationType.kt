package com.blockstream.green.data.notifications.models

enum class NotificationType {
    MELD_TRANSACTION, UNKNOWN;

    companion object {
        fun valueOfOrUnknown(value: String?): NotificationType {
            return when (value) {
                "MELD_TRANSACTION" -> MELD_TRANSACTION
                else -> UNKNOWN
            }
        }


    }
}

