package com.blockstream.domain.send

import com.blockstream.data.gdk.GdkSession
import com.blockstream.data.gdk.data.AccountAsset
import com.blockstream.data.gdk.data.AccountType
import com.blockstream.data.gdk.data.Utxo
import kotlinx.serialization.json.JsonElement

data class SpendableUtxo(
    val assetId: String,
    val utxo: Utxo,
    val isDust: Boolean,
    val isExpired: Boolean,
    val isLegacyRecovery: Boolean,
    // GDK expects the original UTXO JSON in CreateTransactionParams.
    val rawUtxo: JsonElement
) {
    val id: String = "${utxo.txHash}:${utxo.index}"
}
class GetSpendableUtxosUseCase {
    suspend operator fun invoke(
        session: GdkSession,
        accountAsset: AccountAsset
    ): List<SpendableUtxo> {
        val unspentOutputs = session.getUnspentOutputs(accountAsset.account)
        val blockHeight = session.block(accountAsset.account.network).value.height
        val canExpire2FA = accountAsset.account.type != AccountType.TWO_OF_THREE &&
            accountAsset.account.type != AccountType.AMP_LEGACY_ACCOUNT

        return unspentOutputs.unspentOutputs[accountAsset.assetId].orEmpty().mapIndexedNotNull { index, rawUtxo ->
                val utxo = unspentOutputs.unspentOutputsAsUtxo[accountAsset.assetId]?.getOrNull(index)
                    ?: return@mapIndexedNotNull null

                SpendableUtxo(
                    assetId = accountAsset.assetId,
                    utxo = utxo,
                    isDust = accountAsset.account.isBitcoin && utxo.satoshi < DUST_COIN_THRESHOLD_SATS,
                    isExpired = canExpire2FA && utxo.expiryHeight?.let { it <= blockHeight } == true,
                    isLegacyRecovery = accountAsset.account.isBitcoin &&
                        accountAsset.account.type == AccountType.STANDARD &&
                        utxo.addressType != CSV_ADDRESS_TYPE,
                    rawUtxo = rawUtxo
                )
            }
            .filterNot { it.utxo.userStatus == LOCKED_USER_STATUS }
            .sortedBy { it.utxo.blockHeight ?: 0L }
    }

    companion object {
        // App coin-control threshold used by Desktop/iOS for the Dust filter.
        // This is not GDK's network dust limit.
        const val DUST_COIN_THRESHOLD_SATS = 1092L
        private const val LOCKED_USER_STATUS = 1
        private const val CSV_ADDRESS_TYPE = "csv"
    }
}
