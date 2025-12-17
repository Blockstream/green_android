package com.blockstream.data.gdk.data

import com.blockstream.data.data.Denomination
import com.blockstream.data.data.EnrichedAsset
import com.blockstream.data.extensions.isPolicyAsset
import com.blockstream.data.gdk.GdkSession
import com.blockstream.data.gdk.GreenJson
import com.blockstream.data.utils.toAmountLook
import kotlinx.serialization.Serializable

@Serializable
data class AccountAssetBalanceList constructor(val list: List<AccountAssetBalance>)

@Serializable
data class AccountAssetBalance constructor(
    val account: Account,
    val asset: EnrichedAsset,
    val denomination: Denomination? = null,
    val balance: String? = null,
    val balanceExchange: String? = null,
    val satoshi: Long? = null
) : GreenJson<AccountAssetBalance>() {
    override fun kSerializer() = serializer()

    val assetId
        get() = asset.assetId

    val accountAsset: AccountAsset
        get() = AccountAsset(account = account, asset = asset)

    fun accountBalance(session: GdkSession): AccountBalance? {
        return if (assetId.isPolicyAsset(session)) AccountBalance(
            account = account,
            denomination = denomination,
            balance = balance,
            balanceExchange = balanceExchange
        ) else null
    }

    fun balance(session: GdkSession) = session.accountAssets(account).value.balance(assetId)

    companion object {
        suspend fun create(
            accountAsset: AccountAsset,
            session: GdkSession?,
            denomination: Denomination? = null
        ): AccountAssetBalance {
            if (session == null) {
                return AccountAssetBalance.create(accountAsset)
            }
            return accountAsset.balance(session).let { balance ->
                AccountAssetBalance(
                    account = accountAsset.account,
                    asset = accountAsset.asset,
                    balance = balance.toAmountLook(
                        session = session,
                        assetId = accountAsset.assetId,
                        withUnit = true,
                        denomination = denomination
                    ),
                    balanceExchange = balance.toAmountLook(
                        session = session,
                        assetId = accountAsset.assetId,
                        withUnit = true,
                        denomination = Denomination.exchange(session, denomination),
                    ),
                    satoshi = balance
                )
            }
        }

        suspend fun createIfBalance(
            accountAsset: AccountAsset,
            session: GdkSession?,
            denomination: Denomination? = null
        ): AccountAssetBalance? {
            if (session == null) {
                return null
            }

            return accountAsset.balance(session).takeIf { it > 0 }?.let { balance ->
                AccountAssetBalance(
                    account = accountAsset.account,
                    asset = accountAsset.asset,
                    balance = balance.toAmountLook(
                        session = session,
                        assetId = accountAsset.assetId,
                        withUnit = true,
                        denomination = denomination
                    ),
                    balanceExchange = balance.toAmountLook(
                        session = session,
                        assetId = accountAsset.assetId,
                        withUnit = true,
                        denomination = Denomination.exchange(session, denomination)
                    ),
                    satoshi = balance
                )
            }
        }

        fun create(accountAsset: AccountAsset) =
            AccountAssetBalance(account = accountAsset.account, asset = accountAsset.asset)
    }
}