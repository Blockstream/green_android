package com.blockstream.compose.extensions

import com.blockstream.data.data.EnrichedAsset
import com.blockstream.data.gdk.GdkSession
import com.blockstream.compose.utils.StringHolder

fun EnrichedAsset.nameStringHolderOrNull(session: GdkSession?): StringHolder? = nameOrNull(session)?.let { StringHolder.create(it) }

//fun EnrichedAsset.nameOrNull(session: GdkSession?): StringHolder? {
//    return if (isAnyAsset) {
//        StringHolder(stringResource = if (isAmp) Res.string.id_receive_any_amp_asset else Res.string.id_receive_any_liquid_asset)
//    } else if (session != null && assetId.isPolicyAsset(session)) {
//        when {
//            assetId.isBitcoinPolicyAsset() -> "Bitcoin"
//            assetId.isLightningPolicyAsset() -> "Bitcoin (Lightning)"
//            assetId.isPolicyAsset(session.liquid) -> "Liquid Bitcoin"
//            else -> throw Exception("No supported network")
//        }.let {
//            StringHolder(string = if (session.isTestnet) "Testnet $it" else it)
//        }
//    } else {
//        name?.let { StringHolder.create(it) }
//    }
//}

fun EnrichedAsset.nameStringHolder(session: GdkSession?): StringHolder = StringHolder.create(name(session = session))