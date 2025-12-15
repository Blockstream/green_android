package com.blockstream.domain.send

import com.blockstream.data.data.EnrichedAsset
import com.blockstream.data.data.EnrichedAssetList
import com.blockstream.data.gdk.data.AccountAsset
import com.blockstream.data.gdk.data.AccountAssetBalanceList
import com.blockstream.data.gdk.data.CreateTransaction
import com.blockstream.data.gdk.params.CreateTransactionParams
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