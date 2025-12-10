package com.blockstream.domain.send

import com.blockstream.common.data.EnrichedAsset
import com.blockstream.common.data.EnrichedAssetList
import com.blockstream.common.gdk.data.AccountAsset
import com.blockstream.common.gdk.data.AccountAssetBalanceList
import com.blockstream.common.gdk.data.CreateTransaction
import com.blockstream.common.gdk.params.CreateTransactionParams
import kotlinx.serialization.Serializable

@Serializable
sealed class SendFlow {
    @Serializable
    data class SelectAsset(val address: String, val assets: EnrichedAssetList) : SendFlow()
    data class SelectAccount(val address: String, val asset: EnrichedAsset, val accounts: AccountAssetBalanceList) : SendFlow()
    data class SelectAmount(val address: String, val account: AccountAsset) : SendFlow()

    data class SendConfirmation(val account: AccountAsset, val params: CreateTransactionParams, val transaction: CreateTransaction) :
        SendFlow()
}