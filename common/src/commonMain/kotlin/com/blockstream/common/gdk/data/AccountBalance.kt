package com.blockstream.common.gdk.data

import com.blockstream.common.Parcelable
import com.blockstream.common.Parcelize
import com.blockstream.common.data.Denomination
import com.blockstream.common.extensions.hasExpiredUtxos
import com.blockstream.common.extensions.hasHistory
import com.blockstream.common.extensions.hasTwoFactorReset
import com.blockstream.common.extensions.isPolicyAsset
import com.blockstream.common.extensions.needs2faActivation
import com.blockstream.common.gdk.GdkSession
import com.blockstream.common.gdk.GreenJson
import com.blockstream.common.utils.toAmountLook
import kotlinx.serialization.Serializable

@Parcelize
@Serializable
data class AccountBalance constructor(
    val account: Account,
    val denomination: Denomination? = null,
    val balance: String? = null,
    val balanceExchange: String? = null,
    val assets: List<String>? = null,
    val hasNoTwoFactor: Boolean = false,
    val hasExpiredUtxos: Boolean = false,
    val hasTwoFactorReset: Boolean = false,
) : GreenJson<AccountBalance>(), Parcelable {
    override fun kSerializer() = serializer()

    fun balance(session: GdkSession) = session.accountAssets(account).value.policyAsset

    val accountAsset: AccountAsset
        get() = account.accountAsset

    val asMasked: AccountBalance
        get() = copy(
            balance = "*****",
            balanceExchange = "*****"
        )

    companion object {

        fun create(account: Account) = AccountBalance(account = account)

        suspend fun create(
            account: Account,
            session: GdkSession?,
            denomination: Denomination? = null,
        ): AccountBalance {
            return create(
                account = account,
                session = session,
                denomination = denomination,
                createOnlyIfBalance = false
            )!!
        }

        suspend fun createIfBalance(
            account: Account,
            session: GdkSession?,
            denomination: Denomination? = null
        ): AccountBalance? {
            return create(account = account, session = session, denomination = denomination, createOnlyIfBalance = true)
        }

        private suspend fun create(
            account: Account,
            session: GdkSession?,
            denomination: Denomination? = null,
            createOnlyIfBalance : Boolean = false
        ): AccountBalance? {

            if (session == null) {
                return create(account)
            }

            val isLoading = session.accountAssets(account).value.isLoading

            return account.balance(session).takeIf { !createOnlyIfBalance || it > 0}?.let { balance ->

                AccountBalance(
                    account = account,
                    balance = if(isLoading) null else balance.toAmountLook(
                        session = session,
                        assetId = account.network.policyAssetOrNull,
                        withUnit = true,
                        denomination = denomination
                    ),
                    balanceExchange = if(isLoading) null else balance.toAmountLook(
                        session = session,
                        assetId = account.network.policyAssetOrNull,
                        withUnit = true,
                        denomination = Denomination.exchange(session, denomination)
                    ),
                    assets = session.accountAssets(account).value.assets.keys.map {
                        if (it.isPolicyAsset(session) || session.hasAssetIcon(it)) {
                            it
                        } else {
                            "unknown"
                        }
                    }.distinct().takeIf { account.hasHistory(session) },
                    hasNoTwoFactor = account.needs2faActivation(session),
                    hasExpiredUtxos = account.hasExpiredUtxos(session),
                    hasTwoFactorReset = account.hasTwoFactorReset(session)
                )
            }
        }
    }
}