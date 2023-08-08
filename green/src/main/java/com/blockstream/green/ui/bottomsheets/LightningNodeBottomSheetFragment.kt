package com.blockstream.green.ui.bottomsheets

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.lifecycleScope
import com.blockstream.green.R
import com.blockstream.green.databinding.LightningNodeBottomSheetBinding
import com.blockstream.green.databinding.ListItemActionBinding
import com.blockstream.green.ui.items.ActionListItem
import com.blockstream.green.ui.items.OverlineTextListItem
import com.blockstream.green.ui.overview.AccountOverviewFragment
import com.blockstream.green.ui.wallet.AbstractWalletViewModel
import com.blockstream.green.utils.StringHolder
import com.blockstream.green.utils.isDevelopmentFlavor
import com.blockstream.green.utils.isDevelopmentOrDebug
import com.blockstream.green.utils.toAmountLookOrNa
import com.blockstream.lightning.channelsBalanceSatoshi
import com.blockstream.lightning.inboundLiquiditySatoshi
import com.blockstream.lightning.maxPayableSatoshi
import com.blockstream.lightning.maxReceivableSatoshi
import com.blockstream.lightning.maxSinglePaymentAmountSatoshi
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.GenericItem
import com.mikepenz.fastadapter.adapters.FastItemAdapter
import com.mikepenz.fastadapter.binding.listeners.addClickListener
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach


@AndroidEntryPoint
class LightningNodeBottomSheetFragment :
    WalletBottomSheetDialogFragment<LightningNodeBottomSheetBinding, AbstractWalletViewModel>() {
    override val screenName = "LightningNodeState"


    override fun inflate(layoutInflater: LayoutInflater) =
        LightningNodeBottomSheetBinding.inflate(layoutInflater)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val itemAdapter = FastItemAdapter<GenericItem>()

        session.lightningSdk.nodeInfoStateFlow.onEach {
            val list = mutableListOf<GenericItem>()
            list += OverlineTextListItem(StringHolder("ID"), StringHolder(it.id))

            list += OverlineTextListItem(
                StringHolder(R.string.id_account_balance),
                StringHolder(it.channelsBalanceSatoshi().toAmountLookOrNa(
                    session = session,
                    withUnit = true,
                    withGrouping = true
                ))
            )

            list += OverlineTextListItem(
                StringHolder(R.string.id_inbound_liquidity),
                StringHolder(it.inboundLiquiditySatoshi().toAmountLookOrNa(
                    session = session,
                    withUnit = true,
                    withGrouping = true
                ))
            )

            list += OverlineTextListItem(
                StringHolder(R.string.id_max_payable_amount),
                StringHolder(it.maxPayableSatoshi().toAmountLookOrNa(
                    session = session,
                    withUnit = true,
                    withGrouping = true
                ))
            )

            list += OverlineTextListItem(
                StringHolder(R.string.id_max_single_payment_amount),
                StringHolder(it.maxSinglePaymentAmountSatoshi().toAmountLookOrNa(
                    session = session,
                    withUnit = true,
                    withGrouping = true
                ))
            )

            list += OverlineTextListItem(
                StringHolder(R.string.id_max_receivable_amount),
                StringHolder(it.maxReceivableSatoshi().toAmountLookOrNa(
                    session = session,
                    withUnit = true,
                    withGrouping = true
                ))
            )

            if(isDevelopmentOrDebug){
                list += OverlineTextListItem(
                    StringHolder("Connected Peers"),
                    StringHolder(it.connectedPeers.joinToString(", "))
                )
            }

            if(isDevelopmentFlavor) {
                if(it.channelsBalanceSatoshi() > 0){
                    list += ActionListItem(
                        button = StringHolder(R.string.id_close_channel),
                    )
                }
            }

            itemAdapter.set(list)

        }.launchIn(lifecycleScope)


        val fastAdapter = FastAdapter.with(itemAdapter)

        fastAdapter.addClickListener<ListItemActionBinding, GenericItem>({ binding -> binding.button }) { view, _, _, _ ->
            (parentFragment as? AccountOverviewFragment)?.viewModel?.closeChannel()
            (view as? Button)?.isEnabled = false
        }

        binding.recycler.apply {
            adapter = fastAdapter
        }

        binding.buttonClose.setOnClickListener {
            dismiss()
        }
    }

    companion object {
        fun show(fragmentManager: FragmentManager) {
            show(LightningNodeBottomSheetFragment(), fragmentManager)
        }
    }
}
