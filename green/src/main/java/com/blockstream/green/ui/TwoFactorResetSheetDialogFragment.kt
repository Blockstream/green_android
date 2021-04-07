package com.blockstream.green.ui

import android.content.Intent
import android.os.Bundle
import com.blockstream.green.R
import com.blockstream.green.databinding.ListItemHelpBinding
import com.blockstream.green.ui.items.HelpListItem
import com.greenaddress.Bridge
import com.greenaddress.greenbits.ui.preferences.ResetActivePreferenceFragment
import com.greenaddress.greenbits.ui.preferences.SettingsActivity
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.adapters.ItemAdapter
import com.mikepenz.fastadapter.binding.listeners.addClickListener
import com.mikepenz.fastadapter.ui.utils.StringHolder

class TwoFactorResetSheetDialogFragment : AbstractHelpBottomSheetDialogFragment() {

    private var days: Int = 0

    companion object{
        private const val DAYS = "DAYS"

        fun newInstance(days: Int): TwoFactorResetSheetDialogFragment = TwoFactorResetSheetDialogFragment().also {
            it.arguments = Bundle().also { bundle ->
                bundle.putInt(DAYS, days)
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

        days = arguments?.getInt(DAYS) ?: 0
    }

    override fun getTitle() = getString(R.string.id_2fa_reset_in_progress)

    override fun createFastAdapter(): FastAdapter<HelpListItem> {
        val itemAdapter = ItemAdapter<HelpListItem>()

        val list = mutableListOf<HelpListItem>()

        list += HelpListItem(
            StringHolder(getString(R.string.id_your_wallet_is_locked_for_a, days)),
            StringHolder(R.string.id_the_waiting_period_is_necessary),
        )

        list += HelpListItem(
            StringHolder(R.string.id_how_to_stop_this_reset),
            StringHolder(getString(R.string.id_if_you_have_access_to_a, days)),
            StringHolder(R.string.id_cancel_twofactor_reset)
        )

        list += HelpListItem(
            StringHolder(R.string.id_permanently_block_this_wallet),
            StringHolder(R.string.id_if_you_did_not_request_the)
        )

        itemAdapter.add(list)

        val fastAdapter = FastAdapter.with(itemAdapter)

        fastAdapter.addClickListener<ListItemHelpBinding, HelpListItem>({ binding -> binding.button }) { _, _, _, _ ->
            if(Bridge.useGreenModule){
                // TODO implement v4
            } else {
                requireActivity().startActivity(Intent(requireContext(), SettingsActivity::class.java).also {
                    it.putExtra(ResetActivePreferenceFragment.INITIATE_CANCEL, true)
                })

                dismiss()
            }
        }


        return fastAdapter
    }
}