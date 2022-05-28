package com.blockstream.green.ui.bottomsheets

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.fragment.app.FragmentManager
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.blockstream.gdk.data.TwoFactorReset
import com.blockstream.green.R
import com.blockstream.green.data.TwoFactorMethod
import com.blockstream.green.databinding.ListItemHelpBinding
import com.blockstream.green.databinding.RecyclerBottomSheetBinding
import com.blockstream.green.ui.items.HelpListItem
import com.blockstream.green.ui.overview.OverviewFragmentDirections
import com.blockstream.green.ui.settings.TwoFactorSetupAction
import com.blockstream.green.ui.wallet.AbstractWalletViewModel
import com.blockstream.green.utils.StringHolder
import com.blockstream.green.utils.navigate
import com.blockstream.green.utils.toPixels
import com.blockstream.green.views.SpaceItemDecoration
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.adapters.ItemAdapter
import com.mikepenz.fastadapter.binding.listeners.addClickListener
import com.mikepenz.itemanimators.SlideDownAlphaAnimator
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class TwoFactorResetBottomSheetDialogFragment : WalletBottomSheetDialogFragment<RecyclerBottomSheetBinding, AbstractWalletViewModel>() {
    private lateinit var twoFactorReset: TwoFactorReset

    var cancelItem : HelpListItem? = null
    var disputeItem : HelpListItem? = null

    override val screenName = "TwoFactorReset"

    override fun inflate(layoutInflater: LayoutInflater) = RecyclerBottomSheetBinding.inflate(layoutInflater)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        arguments?.getParcelable<TwoFactorReset>(TWO_FACTOR_RESET)?.let {
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


    private fun createFastAdapter(): FastAdapter<HelpListItem> {
        val itemAdapter = ItemAdapter<HelpListItem>()

        val list = mutableListOf<HelpListItem>()

        if(twoFactorReset.isDisputed == true){
            list += HelpListItem(
                StringHolder(R.string.id_your_wallet_is_locked_under_2fa),
                StringHolder(R.string.id_the_1_year_2fa_reset_process),
            )

            list += HelpListItem(
                message = StringHolder(R.string.id_if_you_are_the_rightful_owner),
                button = StringHolder(R.string.id_cancel_2fa_reset),
            ).also {
                cancelItem = it
            }

            list += HelpListItem(
                message = StringHolder(R.string.id_if_you_initiated_the_2fa_reset),
                button = StringHolder(R.string.id_undo_2fa_dispute),
            ).also {
                disputeItem = it
            }

        }else{

            list += HelpListItem(
                title = StringHolder(getString(R.string.id_your_wallet_is_locked_for_a, twoFactorReset.daysRemaining)),
                message = StringHolder(R.string.id_the_waiting_period_is_necessary),
            )

            list += HelpListItem(
                title = StringHolder(R.string.id_how_to_stop_this_reset),
                message = StringHolder(getString(R.string.id_if_you_have_access_to_a, twoFactorReset.daysRemaining)),
                button = StringHolder(R.string.id_cancel_twofactor_reset)
            ).also {
                cancelItem = it
            }

            list += HelpListItem(
                title = StringHolder(R.string.id_permanently_block_this_wallet),
                message = StringHolder(R.string.id_if_you_did_not_request_the),
                button = StringHolder(R.string.id_dispute_twofactor_reset),
            ).also {
                disputeItem = it
            }
        }

        itemAdapter.add(list)

        val fastAdapter = FastAdapter.with(itemAdapter)

        fastAdapter.addClickListener<ListItemHelpBinding, HelpListItem>({ binding -> binding.button }) { _, _, _, item ->
            val twoFactorSetupAction = if(item == cancelItem){
                TwoFactorSetupAction.CANCEL
            }else{
                if(twoFactorReset.isDisputed == true) {
                    TwoFactorSetupAction.UNDO_DISPUTE
                }else{
                    TwoFactorSetupAction.DISPUTE
                }
            }

            val directions = OverviewFragmentDirections.actionGlobalTwoFactorSetupFragment(
                wallet = viewModel.wallet,
                method = TwoFactorMethod.EMAIL,
                action = twoFactorSetupAction
            )

            navigate(findNavController(), directions.actionId, directions.arguments, false)

            dismiss()
        }

        return fastAdapter
    }

    companion object{
        private const val TWO_FACTOR_RESET = "TWO_FACTOR_RESET"

        fun show(twoFactorReset: TwoFactorReset, fragmentManager: FragmentManager) {
            show(TwoFactorResetBottomSheetDialogFragment().also {
                it.arguments = Bundle().also { bundle ->
                    bundle.putParcelable(TWO_FACTOR_RESET, twoFactorReset)
                }
            }, fragmentManager)
        }
    }
}