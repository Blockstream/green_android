package com.blockstream.green.looks

import com.blockstream.gdk.data.UtxoView
import com.blockstream.green.databinding.TransactionUtxoLayoutBinding
import kotlinx.coroutines.CoroutineScope

interface ITransactionLook {
    suspend fun fee() : String
    suspend fun feeFiat(): String?
    fun feeRate() : String

    val utxoSize : Int
    fun getUtxoView(index: Int): UtxoView?
    fun setTransactionUtxoToBinding(scope: CoroutineScope, index: Int, binding: TransactionUtxoLayoutBinding)
}