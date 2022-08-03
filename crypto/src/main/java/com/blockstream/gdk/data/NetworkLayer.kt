package com.blockstream.gdk.data

import android.os.Parcelable
import com.blockstream.gdk.BTC_POLICY_ASSET
import kotlinx.parcelize.Parcelize

@Parcelize
sealed class NetworkLayer : Parcelable {
    object Bitcoin : NetworkLayer()
    object Liquid : NetworkLayer()

    val isBitcoin: Boolean
        get() = when (this) {
            is Bitcoin -> {
                true
            }
            is NetworkPolicy -> {
                this.network.isBitcoin
            }
            else -> {
                false
            }
        }

    val isLiquid: Boolean
        get() = !isBitcoin

    val isSinglesig: Boolean
        get() = this is NetworkPolicy.Singlesig

    val isMultisig: Boolean
        get() = this is NetworkPolicy.Multisig

    val singlesig: NetworkPolicy
        get() = NetworkPolicy.Singlesig(this.networkLayer)

    val multisig: NetworkPolicy
        get() = NetworkPolicy.Multisig(this.networkLayer)

    val lightning: NetworkPolicy
        get() = NetworkPolicy.Lightning(this.networkLayer)

    val networkLayer: NetworkLayer
        get() = if(this is NetworkPolicy) this.network else this

    companion object {
        fun fromAsset(assetId: String?): NetworkLayer {
            return when (assetId) {
                BTC_POLICY_ASSET, null -> Bitcoin
                else -> Liquid
            }
        }

        fun fromNetwork(networkId: String): NetworkLayer {
            val networkLayer = if (Network.isBitcoin(networkId)) Bitcoin else Liquid

            return if (Network.isSinglesig(networkId)) {
                NetworkPolicy.Singlesig(networkLayer)
            } else if (Network.isMultisig(networkId)) {
                NetworkPolicy.Multisig(networkLayer)
            } else {
                TODO("Lightning not supported yet")
            }
        }
    }
}

@Parcelize
sealed class NetworkPolicy(open val network: NetworkLayer) : NetworkLayer(), Parcelable {
    data class Singlesig(override val network: NetworkLayer) : NetworkPolicy(network)
    data class Multisig(override val network: NetworkLayer) : NetworkPolicy(network)
    data class Lightning(override val network: NetworkLayer) : NetworkPolicy(network)

    companion object{
        val BitcoinMultisig = Multisig(Bitcoin)
        val LiquidMultisig = Multisig(Liquid)
    }
}

fun String.assetIdBelongsToLayer(layer: NetworkLayer): Boolean {
    return (this == BTC_POLICY_ASSET && layer.isBitcoin) || (this != BTC_POLICY_ASSET && layer.isLiquid)
}

fun Account.belongsToLayer(layer: NetworkLayer): Boolean {
    val layer1Check = this.isBitcoin == layer.isBitcoin

    var layer2Check = true
    if (layer is NetworkPolicy) {
        layer2Check = when (layer) {
            is NetworkPolicy.Singlesig -> {
                this.isSinglesig
            }
            is NetworkPolicy.Multisig -> {
                this.isMultisig
            }
            else -> {
                TODO("Lightning not supported yet")
            }
        }
    }

    return layer1Check && layer2Check
}