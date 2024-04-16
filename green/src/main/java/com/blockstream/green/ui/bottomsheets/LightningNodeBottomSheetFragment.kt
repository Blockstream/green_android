package com.blockstream.green.ui.bottomsheets

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.core.content.FileProvider
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.lifecycleScope
import com.blockstream.common.extensions.logException
import com.blockstream.common.lightning.LightningManager
import com.blockstream.common.lightning.channelsBalanceSatoshi
import com.blockstream.common.lightning.inboundLiquiditySatoshi
import com.blockstream.common.lightning.maxPayableSatoshi
import com.blockstream.common.lightning.maxReceivableSatoshi
import com.blockstream.common.lightning.maxSinglePaymentAmountSatoshi
import com.blockstream.common.models.GreenViewModel
import com.blockstream.common.utils.Loggable
import com.blockstream.green.R
import com.blockstream.green.databinding.LightningNodeBottomSheetBinding
import com.blockstream.green.databinding.ListItemActionBinding
import com.blockstream.green.extensions.shareTextFile
import com.blockstream.green.ui.items.ActionListItem
import com.blockstream.green.ui.items.OverlineTextListItem
import com.blockstream.green.ui.overview.AccountOverviewFragment
import com.blockstream.green.utils.StringHolder
import com.blockstream.green.utils.isDevelopmentOrDebug
import com.blockstream.green.utils.toAmountLookOrNa
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.GenericItem
import com.mikepenz.fastadapter.adapters.FastItemAdapter
import com.mikepenz.fastadapter.binding.listeners.addClickListener
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import java.io.File


class LightningNodeBottomSheetFragment :
    WalletBottomSheetDialogFragment<LightningNodeBottomSheetBinding, GreenViewModel>() {
    override val screenName = "LightningNodeState"

    override val expanded: Boolean = true
    
    override fun inflate(layoutInflater: LayoutInflater) =
        LightningNodeBottomSheetBinding.inflate(layoutInflater)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val itemAdapter = FastItemAdapter<GenericItem>()

        val buttonActions1 = ActionListItem(
            button = StringHolder(R.string.id_show_recovery_phrase)
        )

        val buttonActions2 = ActionListItem()

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

            list += buttonActions1

            list += buttonActions2

            buttonActions1.buttonOutline = if(it.channelsBalanceSatoshi() > 0) StringHolder(R.string.id_empty_lightning_account) else StringHolder()

            buttonActions2.buttonOutline = StringHolder("Rescan Swaps")

            buttonActions2.buttonText = StringHolder("Share Logs")

            itemAdapter.set(list)

        }.launchIn(lifecycleScope)

        val fastAdapter = FastAdapter.with(itemAdapter)

        fastAdapter.addClickListener<ListItemActionBinding, GenericItem>({ binding -> binding.button }) { _, _, _, item ->
            // (parentFragment as? AccountOverviewFragment)?.showLightningRecoveryPhrase()
            dismiss()
        }

        fastAdapter.addClickListener<ListItemActionBinding, GenericItem>({ binding -> binding.buttonOutline }) { _, _, _, item ->
            if(item == buttonActions1){
//                (parentFragment as? AccountOverviewFragment)?.showEmptyLightningAccount()
            }else{
//                (parentFragment as? AccountOverviewFragment)?.rescanSwaps()
            }
            dismiss()
        }

        fastAdapter.addClickListener<ListItemActionBinding, GenericItem>({ binding -> binding.buttonText }) { _, _, _, item ->
            lifecycleScope.launch(context = logException()) {
                val nodeId = session.lightningSdk.nodeInfoStateFlow.value.id
                val file = File(requireContext().cacheDir, "Greenlight_Logs_$nodeId.txt")

                // Delete the previous file as a procaution to share an old version
                file.delete()

                val lightningManager: LightningManager by inject()

                file.writeText(lightningManager.logs.toString())

                val fileUri = FileProvider.getUriForFile(
                    requireActivity(),
                    requireContext().packageName.toString() + ".provider",
                    file
                )

                shareTextFile(fileUri)
            }
        }

        binding.recycler.apply {
            adapter = fastAdapter
        }

        binding.buttonClose.setOnClickListener {
            dismiss()
        }
    }

    companion object: Loggable() {
        fun show(fragmentManager: FragmentManager) {
            show(LightningNodeBottomSheetFragment(), fragmentManager)
        }
    }
}
