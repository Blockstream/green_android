package com.blockstream.green.ui.bottomsheets

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.blockstream.gdk.params.BalanceParams
import com.blockstream.green.databinding.SelectUtxosBottomSheetBinding
import com.blockstream.green.gdk.observable
import com.blockstream.green.ui.items.UtxoListItem
import com.blockstream.green.ui.send.SendViewModel
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.adapters.ItemAdapter
import com.mikepenz.itemanimators.SlideDownAlphaAnimator
import dagger.hilt.android.AndroidEntryPoint
import io.reactivex.rxjava3.kotlin.subscribeBy
import mu.KLogging

// WIP
@AndroidEntryPoint
class SelectUtxosBottomSheetDialogFragment : WalletBottomSheetDialogFragment<SelectUtxosBottomSheetBinding, SendViewModel>() {
    override val screenName = "SelectUTXO"

    override fun inflate(layoutInflater: LayoutInflater) = SelectUtxosBottomSheetBinding.inflate(layoutInflater)

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

    companion object : KLogging() {
        fun show(fragmentManager: FragmentManager){
            show(SelectUtxosBottomSheetDialogFragment(), fragmentManager)
        }
    }
}