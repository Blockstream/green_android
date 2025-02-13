package com.blockstream.common.data

import kotlinx.serialization.Serializable

@Serializable
enum class TwoFactorSetupAction {
    SETUP, SETUP_EMAIL, RESET, CANCEL, DISPUTE, UNDO_DISPUTE
}