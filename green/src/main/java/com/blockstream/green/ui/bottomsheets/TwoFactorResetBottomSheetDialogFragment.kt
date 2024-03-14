package com.blockstream.green.ui.bottomsheets

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.core.os.BundleCompat
import androidx.fragment.app.FragmentManager
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.blockstream.common.gdk.data.Network
import com.blockstream.common.gdk.data.TwoFactorReset
import com.blockstream.common.models.GreenViewModel
import com.blockstream.green.NavGraphDirections
import com.blockstream.green.R
import com.blockstream.common.data.TwoFactorMethod
import com.blockstream.common.data.TwoFactorSetupAction
import com.blockstream.green.databinding.ListItemActionBinding
import com.blockstream.green.databinding.RecyclerBottomSheetBinding
import com.blockstream.green.extensions.navigate
import com.blockstream.green.ui.items.ActionListItem
import com.blockstream.green.utils.StringHolder
import com.blockstream.green.utils.toPixels
import com.blockstream.green.views.SpaceItemDecoration
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.adapters.ItemAdapter
import com.mikepenz.fastadapter.binding.listeners.addClickListener
import com.mikepenz.itemanimators.SlideDownAlphaAnimator

class TwoFactorResetBottomSheetDialogFragment : WalletBottomSheetDialogFragment<RecyclerBottomSheetBinding, GreenViewModel>() {
    private lateinit var twoFactorReset: TwoFactorReset

    private var cancelItem : ActionListItem? = null
    private var disputeItem : ActionListItem? = null

    override val screenName = "TwoFactorReset"

    override val network by lazy { BundleCompat.getParcelable(requireArguments(), NETWORK, Network::class.java)!! }

    override fun inflate(layoutInflater: LayoutInflater) = RecyclerBottomSheetBinding.inflate(layoutInflater)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        BundleCompat.getParcelable(requireArguments(), TWO_FACTOR_RESET, TwoFactorReset::class.java)?.let {
            twoFactorReset = it
        } ?: run {
            dismiss()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.title = if(twoFactorReset.isDisputed == true) getString(R.string.id_2fa_dispute_in_progress) else getString(R.string.id_2fa_reset_in_progress)

        binding.recycler.apply {
            layoutManager = LinearLayoutManager(context)
            itemAnimator = SlideDownAlphaAnimator()
            adapter = createFastAdapter()
            addItemDecoration(SpaceItemDecoration(toPixels(24)))
        }

        binding.buttonClose.setOnClickListener {
            dismiss()
        }
    }


    private fun createFastAdapter(): FastAdapter<ActionListItem> {
        val itemAdapter = ItemAdapter<ActionListItem>()

        val list = mutableListOf<ActionListItem>()

        if(twoFactorReset.isDisputed == true){
            list += ActionListItem(
                StringHolder(R.string.id_your_wallet_is_locked_under_2fa),
                StringHolder(R.string.id_the_1_year_2fa_reset_process),
            )

            list += ActionListItem(
                message = StringHolder(R.string.id_if_you_are_the_rightful_owner),
                button = StringHolder(R.string.id_cancel_2fa_reset),
            ).also {
                cancelItem = it
            }

            list += ActionListItem(
                message = StringHolder(R.string.id_if_you_initiated_the_2fa_reset),
                button = StringHolder(R.string.id_undo_2fa_dispute),
            ).also {
                disputeItem = it
            }

        }else{

            list += ActionListItem(
                title = StringHolder(getString(R.string.id_your_wallet_is_locked_for_a, twoFactorReset.daysRemaining)),
                message = StringHolder(R.string.id_the_waiting_period_is_necessary),
            )

            list += ActionListItem(
                title = StringHolder(R.string.id_how_to_stop_this_reset),
                message = StringHolder(getString(R.string.id_if_you_have_access_to_a, twoFactorReset.daysRemaining)),
                button = StringHolder(R.string.id_cancel_twofactor_reset)
            ).also {
                cancelItem = it
            }

            list += ActionListItem(
                title = StringHolder(R.string.id_permanently_block_this_wallet),
                message = StringHolder(R.string.id_if_you_did_not_request_the),
                button = StringHolder(R.string.id_dispute_twofactor_reset),
            ).also {
                disputeItem = it
            }
        }

        itemAdapter.add(list)

        val fastAdapter = FastAdapter.with(itemAdapter)

        fastAdapter.addClickListener<ListItemActionBinding, ActionListItem>({ binding -> binding.button }) { _, _, _, item ->
            val twoFactorSetupAction = if(item == cancelItem){
                TwoFactorSetupAction.CANCEL
            }else{
                if(twoFactorReset.isDisputed == true) {
                    TwoFactorSetupAction.UNDO_DISPUTE
                }else{
                    TwoFactorSetupAction.DISPUTE
                }
            }

            val directions = NavGraphDirections.actionGlobalTwoFactorSetupFragment(
                wallet = viewModel.greenWallet,
                method = TwoFactorMethod.EMAIL,
                action = twoFactorSetupAction,
                network = network
            )

            navigate(findNavController(), directions.actionId, directions.arguments, false)

            dismiss()
        }

        return fastAdapter
    }

    companion object{
        private const val NETWORK = "NETWORK"
        private const val TWO_FACTOR_RESET = "TWO_FACTOR_RESET"

        fun show(network: Network, twoFactorReset: TwoFactorReset, fragmentManager: FragmentManager) {
            show(TwoFactorResetBottomSheetDialogFragment().also {
                it.arguments = Bundle().also { bundle ->
                    bundle.putParcelable(NETWORK, network)
                    bundle.putParcelable(TWO_FACTOR_RESET, twoFactorReset)
                }
            }, fragmentManager)
        }
    }
}