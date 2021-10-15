package com.blockstream.green.ui

import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import com.blockstream.gdk.params.BalanceParams
import com.blockstream.green.R
import com.blockstream.green.databinding.SelectUtxosBottomSheetBinding
import com.blockstream.green.gdk.observable
import com.blockstream.green.ui.items.UtxoListItem
import com.blockstream.green.ui.wallet.SendViewModel
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.adapters.ItemAdapter
import com.mikepenz.itemanimators.SlideDownAlphaAnimator
import io.reactivex.rxjava3.kotlin.subscribeBy

// WIP
class SelectUtxosBottomSheetDialogFragment :
    WalletBottomSheetDialogFragment<SelectUtxosBottomSheetBinding, SendViewModel>(
        layout = R.layout.select_utxos_bottom_sheet
    ) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val utxoAdapter = ItemAdapter<UtxoListItem>()

        viewModel.session.observable {
            val unspentOutputs = it.getUnspentOutputs(
                BalanceParams(
                    subaccount = viewModel.wallet.activeAccount,
                    confirmations = 0
                )
            )
            unspentOutputs

        }.subscribeBy(
            onSuccess = {

                utxoAdapter.set(
                    it.unspentOutputs[viewModel.session.policyAsset]?.map {
                        UtxoListItem(it)
                    } ?: listOf())
            },
            onError = {
                it.printStackTrace()

            }
        )


        binding.buttonClear.setOnClickListener {
            dismiss()
        }

        binding.buttonOK.setOnClickListener {
            dismiss()
        }

        binding.recycler.apply {
            layoutManager = LinearLayoutManager(context)
            itemAnimator = SlideDownAlphaAnimator()
            adapter = FastAdapter.with(utxoAdapter)
            // addItemDecoration(SpaceItemDecoration(toPixels(24)))
        }
    }
}