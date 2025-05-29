package com.blockstream.domain.hardware

import com.blockstream.common.CountlyBase
import com.blockstream.common.gdk.GdkSession
import com.blockstream.common.gdk.data.Account
import com.blockstream.common.gdk.data.Address

class VerifyAddressUseCase(private val countly: CountlyBase) {
    suspend operator fun invoke(
        session: GdkSession,
        account: Account,
        address: Address
    ) {
        countly.verifyAddress(session, account)

        session.gdkHwWallet?.apply {
            getGreenAddress(
                network = account.network,
                hwInteraction = null,
                account = account,
                path = address.userPath ?: listOf(),
                csvBlocks = address.subType ?: 0
            ).also {
                if (it != address.address) {
                    throw Exception("id_the_addresses_dont_match")
                }
            }
        }
    }
}