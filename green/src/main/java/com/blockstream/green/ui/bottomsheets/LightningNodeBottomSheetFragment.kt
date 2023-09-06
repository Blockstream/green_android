package com.blockstream.green.ui.bottomsheets

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.lifecycleScope
import com.blockstream.common.lightning.channelsBalanceSatoshi
import com.blockstream.common.lightning.inboundLiquiditySatoshi
import com.blockstream.common.lightning.maxPayableSatoshi
import com.blockstream.common.lightning.maxReceivableSatoshi
import com.blockstream.common.lightning.maxSinglePaymentAmountSatoshi
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
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.GenericItem
import com.mikepenz.fastadapter.adapters.FastItemAdapter
import com.mikepenz.fastadapter.binding.listeners.addClickListener
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach


class LightningNodeBottomSheetFragment :
    WalletBottomSheetDialogFragment<LightningNodeBottomSheetBinding, AbstractWalletViewModel>() {
    override val screenName = "LightningNodeState"

    override val expanded: Boolean = true
    
    override fun inflate(layoutInflater: LayoutInflater) =
        LightningNodeBottomSheetBinding.inflate(layoutInflater)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val itemAdapter = FastItemAdapter<GenericItem>()

        val showRecoveryPhrase = ActionListItem(
            buttonText = StringHolder(R.string.id_show_recovery_phrase),
        )
        val closeChannel = ActionListItem(
            buttonText = StringHolder(R.string.id_close_channel),
        )

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

            list += showRecoveryPhrase

            if(isDevelopmentFlavor) {
                if(it.channelsBalanceSatoshi() > 0){
                    list += closeChannel
                }
            }

            itemAdapter.set(list)

        }.launchIn(lifecycleScope)

        val fastAdapter = FastAdapter.with(itemAdapter)

        fastAdapter.addClickListener<ListItemActionBinding, GenericItem>({ binding -> binding.buttonText }) { view, _, _, item ->
            if (item == showRecoveryPhrase) {
                (parentFragment as? AccountOverviewFragment)?.showLightningRecoveryPhrase()
                dismiss()
            } else if (item == closeChannel) {
                (parentFragment as? AccountOverviewFragment)?.viewModel?.closeChannel()
                (view as? Button)?.isEnabled = false
            }
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
