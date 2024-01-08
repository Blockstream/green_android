package com.blockstream.green.ui.bottomsheets

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
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

        val buttonActions = ActionListItem(
            button = StringHolder(R.string.id_show_recovery_phrase)
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

            list += buttonActions

            buttonActions.buttonOutline = if(isDevelopmentOrDebug && it.channelsBalanceSatoshi() > 0) StringHolder(R.string.id_empty_lightning_account) else StringHolder()

            itemAdapter.set(list)

        }.launchIn(lifecycleScope)

        val fastAdapter = FastAdapter.with(itemAdapter)

        fastAdapter.addClickListener<ListItemActionBinding, GenericItem>({ binding -> binding.button }) { _, _, _, _ ->
            (parentFragment as? AccountOverviewFragment)?.showLightningRecoveryPhrase()
            dismiss()
        }

        fastAdapter.addClickListener<ListItemActionBinding, GenericItem>({ binding -> binding.buttonOutline }) { _, _, _, _ ->
            (parentFragment as? AccountOverviewFragment)?.showEmptyLightningAccount()
            dismiss()
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
