package com.blockstream.green.ui.looks

import com.blockstream.gdk.data.Transaction
import com.blockstream.green.databinding.ListItemTransactionAssetBinding

interface AddreseeLookInterface {
    val txType: Transaction.Type

    fun isChange(index: Int): Boolean
    fun getAssetId(index: Int): String
    fun getAddress(index: Int): String?
    fun setAssetToBinding(index: Int, binding: ListItemTransactionAssetBinding)
}