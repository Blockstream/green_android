package com.blockstream.green.utils

import androidx.lifecycle.MutableLiveData
import com.blockstream.gdk.AssetQATester
import com.greenaddress.greenapi.HardwareQATester

/*
 * Emulate different scenarios, useful for QA
 */
class QATester : HardwareQATester, AssetQATester {
    val corruptedHardwareMessageSign = MutableLiveData(false)
    val corruptedHardwareTxSign = MutableLiveData(false)

    val assetsFetchDisabled = MutableLiveData(false)
    val assetsIconsFetchDisabled = MutableLiveData(false)
    val assetsGdkCacheDisabled = MutableLiveData(false)
    val assetsAppCacheDisabled = MutableLiveData(false)

    override fun getAntiExfilCorruptionForMessageSign(): Boolean {
        return corruptedHardwareMessageSign.value ?: false
    }

    override fun getAntiExfilCorruptionForTxSign(): Boolean {
        return corruptedHardwareTxSign.value ?: false
    }

    override fun isAssetAppCacheDisabled(): Boolean {
        return assetsAppCacheDisabled.value ?: false
    }

    override fun isAssetGdkCacheDisabled(): Boolean {
        return assetsGdkCacheDisabled.value ?: false
    }

    override fun isAssetFetchDisabled(): Boolean {
        return assetsFetchDisabled.value ?: false
    }

    override fun isAssetIconsFetchDisabled(): Boolean {
        return assetsIconsFetchDisabled.value ?: false
    }
}