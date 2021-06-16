package com.blockstream.green.ui

import android.content.Intent
import android.os.Bundle
import com.blockstream.gdk.data.TwoFactorReset
import com.blockstream.green.R
import com.blockstream.green.databinding.ListItemHelpBinding
import com.blockstream.green.ui.items.HelpListItem
import com.blockstream.green.utils.StringHolder
import com.greenaddress.Bridge
import com.greenaddress.greenbits.ui.preferences.ResetActivePreferenceFragment
import com.greenaddress.greenbits.ui.preferences.SettingsActivity
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.adapters.ItemAdapter
import com.mikepenz.fastadapter.binding.listeners.addClickListener

// TODO this could be a WalletBottomSheetDialogFragment in v4
class TwoFactorResetSheetDialogFragment : AbstractHelpBottomSheetDialogFragment() {

    private lateinit var twoFactorReset: TwoFactorReset

    var cancelItem : HelpListItem? = null
    var disputeItem : HelpListItem? = null

    companion object{
        private const val TWO_FACTOR_RESET = "TWO_FACTOR_RESET"

        fun newInstance(twoFactorReset: TwoFactorReset): TwoFactorResetSheetDialogFragment = TwoFactorResetSheetDialogFragment().also {
            it.arguments = Bundle().also { bundle ->
                bundle.putParcelable(TWO_FACTOR_RESET, twoFactorReset)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if(!Bridge.useGreenModule) {
            setStyle(
                STYLE_NORMAL,
                R.style.Green_BottomSheetDialogTheme
            )
        }

        arguments?.getParcelable<TwoFactorReset>(TWO_FACTOR_RESET)?.let {
            twoFactorReset = it
        } ?: run {
            dismiss()
        }
    }

    override fun getTitle() = if(twoFactorReset.isDisputed) getString(R.string.id_2fa_dispute_in_progress) else getString(R.string.id_2fa_reset_in_progress)

    override fun createFastAdapter(): FastAdapter<HelpListItem> {
        val itemAdapter = ItemAdapter<HelpListItem>()

        val list = mutableListOf<HelpListItem>()

        if(twoFactorReset.isDisputed){
            list += HelpListItem(
                StringHolder(R.string.id_your_wallet_locked_under_2fa),
                StringHolder(R.string.id_the_1_year_2fa_reset_process),
            )

            list += HelpListItem(
                message = StringHolder(R.string.id_if_you_are_the_rightful_owner),
                buttonText = StringHolder(R.string.id_cancel_2fa_reset),
            ).also {
                cancelItem = it
            }

            list += HelpListItem(
                message = StringHolder(R.string.id_if_you_initiated_the_2fa_reset),
                buttonText = StringHolder(R.string.id_undo_2fa_dispute),
            ).also {
                disputeItem = it
            }

        }else{

            list += HelpListItem(
                StringHolder(getString(R.string.id_your_wallet_is_locked_for_a, twoFactorReset.daysRemaining)),
                StringHolder(R.string.id_the_waiting_period_is_necessary),
            )

            list += HelpListItem(
                StringHolder(R.string.id_how_to_stop_this_reset),
                StringHolder(getString(R.string.id_if_you_have_access_to_a, twoFactorReset.daysRemaining)),
                StringHolder(R.string.id_cancel_twofactor_reset)
            )

            list += HelpListItem(
                StringHolder(R.string.id_permanently_block_this_wallet),
                StringHolder(R.string.id_if_you_did_not_request_the)
            )
        }

        itemAdapter.add(list)

        val fastAdapter = FastAdapter.with(itemAdapter)

        fastAdapter.addClickListener<ListItemHelpBinding, HelpListItem>({ binding -> binding.button }) { _, _, _, item ->
            if(Bridge.useGreenModule){
                // TODO implement v4
            } else {
                if(twoFactorReset.isDisputed){
                    if(item == disputeItem) {
                        initiateAction(ResetActivePreferenceFragment.INITIATE_UNDO_DISPUTE)
                    }else {
                        initiateAction(ResetActivePreferenceFragment.INITIATE_CANCEL)
                    }
                }else{
                    initiateAction(ResetActivePreferenceFragment.INITIATE_CANCEL)
                }

                dismiss()
            }
        }

        return fastAdapter
    }

    private fun initiateAction(action : String){
        requireActivity().startActivity(Intent(requireContext(), SettingsActivity::class.java).also {
            it.putExtra(action, true)
        })
    }
}