package com.blockstream.green.ui.items

import android.view.LayoutInflater
import android.view.ViewGroup
import com.blockstream.green.R
import com.blockstream.green.databinding.ListItemTransactionFeeBinding
import com.blockstream.green.gdk.GdkSession
import com.blockstream.green.looks.ConfirmTransactionLook
import com.blockstream.green.looks.ITransactionLook
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class TransactionFeeListItem constructor(
    val session: GdkSession,
    val look: ITransactionLook,
    val confirmLook: ConfirmTransactionLook? = null,
) : AbstractBindingItem<ListItemTransactionFeeBinding>() {
    override val type: Int
        get() = R.id.fastadapter_transaction_fee_item_id

    init {
        identifier = "TransactionFeeListItem".hashCode().toLong()
    }

    override fun createScope(): CoroutineScope = session.createScope(dispatcher = Dispatchers.Main)

    override fun bindView(binding: ListItemTransactionFeeBinding, payloads: List<Any>) {
        binding.confirmStyle = confirmLook != null
        binding.feeRate = look.feeRate()

        scope.launch {
            binding.fee = withContext(context = Dispatchers.IO) { look.fee() }
            binding.feeFiat = withContext(context = Dispatchers.IO) { look.feeFiat() }

            if(confirmLook != null){
                binding.total = withContext(context = Dispatchers.IO) { confirmLook.total() }
                binding.totalFiat = withContext(context = Dispatchers.IO) { confirmLook.totalFiat() }
            }
        }


    }

    override fun createBinding(
        inflater: LayoutInflater,
        parent: ViewGroup?
    ): ListItemTransactionFeeBinding {
        return ListItemTransactionFeeBinding.inflate(inflater, parent, false)
    }
}