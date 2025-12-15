package com.blockstream.data.meld.data

enum class MeldTransactionStatus {
    PENDING_CREATED,
    PENDING,
    PROCESSING,
    AUTHORIZED,
    AUTHORIZATION_EXPIRED,
    SETTLING,
    SETTLED,
    REFUNDED,
    DECLINED,
    CANCELLED,
    FAILED,
    ERROR,
    VOIDED,
    TWO_FA_REQUIRED,
    TWO_FA_PROVIDED
}