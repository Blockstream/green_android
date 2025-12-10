package com.blockstream.domain.send

import com.blockstream.common.data.EnrichedAsset
import com.blockstream.common.data.EnrichedAssetList
import com.blockstream.common.data.GreenWallet
import com.blockstream.common.extensions.tryCatch
import com.blockstream.common.gdk.GdkSession
import com.blockstream.common.gdk.data.AccountAsset
import com.blockstream.common.gdk.data.AccountAssetBalanceList
import com.blockstream.domain.boltz.BoltzUseCase
import com.blockstream.jade.Loggable

class GetSendFlowUseCase(
    private val boltzUseCase: BoltzUseCase,
    private val getSendAssetsUseCase: GetSendAssetsUseCase,
    private val getSendAccountsUseCase: GetSendAccountsUseCase,
    private val prepareTransactionUseCase: PrepareTransactionUseCase
) {

    suspend operator fun invoke(
        greenWallet: GreenWallet,
        session: GdkSession,
        address: String,
        asset: EnrichedAsset? = null,
        account: AccountAsset? = null,
    ): SendFlow {
        var asset = asset
        var account = account

        // Check if address is valid and get the appropriate assets
        val assets = getSendAssetsUseCase(session = session, address = address)

        when {
            assets.isEmpty() -> throw Exception("id_invalid_address")
            asset != null -> {
                // Check if can be a Swap, Liquid <> Lightning
                if (asset.isLiquidPolicyAsset(session) &&
                    assets.size == 1 &&
                    assets.first().isLightning &&
                    boltzUseCase.isAddressSwappableUseCase(address = address)
                ) {
                    // Change asset to Lightning
                    asset = assets.first()
                } else {
                    assets.find {
                        it.assetId == asset.assetId
                    } ?: throw Exception("id_invalid_address")
                }
            }

            assets.size > 1 -> {
                return SendFlow.SelectAsset(address = address, assets = EnrichedAssetList(assets))
            }

            else -> {
                asset = assets.first()
            }
        }

        if (account == null) {
            val accounts = getSendAccountsUseCase(session = session, wallet = greenWallet, asset = asset, address = address)

            when {
                accounts.isEmpty() -> {
                    // Asest is Lightning but address is not swappable (LNURL)
                    if (asset.isLightning && !boltzUseCase.isAddressSwappableUseCase(address = address)) {
                        throw Exception("id_invalid_address")
                    } else {
                        throw Exception("id_insufficient_funds")
                    }
                }
                accounts.size > 1 -> {
                    return SendFlow.SelectAccount(address = address, asset = asset, accounts = AccountAssetBalanceList(accounts))
                }

                else -> {
                    account = accounts.first().accountAsset
                }
            }
        }

        val isSwap = if (account.account.network.isLiquid) {
            tryCatch {
                assets.first().isLightning &&
                        boltzUseCase.isSwapsEnabledUseCase(wallet = greenWallet) &&
                        boltzUseCase.isAddressSwappableUseCase(address = address)
            } ?: false
        } else false

        if (isSwap) {
            val params = prepareTransactionUseCase(
                greenWallet = greenWallet,
                session = session,
                accountAsset = account,
                address = address,
            )

            val tx = session.createTransaction(account.account.network, params)

            if (tx.error.isNullOrBlank()) {
                return SendFlow.SendConfirmation(account = account, params = params, transaction = tx)
            } else {
                throw Exception(tx.error)
            }
        }

        return SendFlow.SelectAmount(address = address, account = account)
    }

    companion object : Loggable()
}
