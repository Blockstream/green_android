package com.blockstream.common.gdk.data

import com.arkivanov.essenty.parcelable.Parcelable
import com.arkivanov.essenty.parcelable.Parcelize
import com.blockstream.common.data.Denomination
import com.blockstream.common.data.EnrichedAsset
import com.blockstream.common.extensions.isPolicyAsset
import com.blockstream.common.gdk.GdkSession
import com.blockstream.common.gdk.GreenJson
import com.blockstream.common.utils.toAmountLook
import kotlinx.serialization.Serializable

@Parcelize
@Serializable
data class AccountAssetBalance constructor(
    val account: Account,
    val asset: EnrichedAsset,
    val denomination: Denomination? = null,
    val balance: String? = null,
    val balanceExchange: String? = null
) : GreenJson<AccountAssetBalance>(), Parcelable {
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
            if(session == null){
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
                    )
                )
            }
        }

        suspend fun createIfBalance(
            accountAsset: AccountAsset,
            session: GdkSession?,
            denomination: Denomination? = null
        ): AccountAssetBalance? {
            if(session == null){
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
                    )
                )
            }
        }

        fun create(accountAsset: AccountAsset) =
            AccountAssetBalance(account = accountAsset.account, asset = accountAsset.asset)
    }
}