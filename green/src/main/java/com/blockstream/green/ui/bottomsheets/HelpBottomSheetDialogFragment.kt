package com.blockstream.green.ui.bottomsheets

import androidx.fragment.app.FragmentManager
import com.blockstream.green.R
import com.blockstream.base.Urls
import com.blockstream.green.databinding.ListItemHelpBinding
import com.blockstream.green.settings.SettingsManager
import com.blockstream.green.ui.items.HelpListItem
import com.blockstream.green.utils.StringHolder
import com.blockstream.green.utils.openBrowser
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.adapters.ItemAdapter
import com.mikepenz.fastadapter.binding.listeners.addClickListener
import dagger.hilt.android.AndroidEntryPoint
import mu.KLogging
import javax.inject.Inject

@AndroidEntryPoint
class HelpBottomSheetDialogFragment: RecyclerBottomSheetDialogFragment() {
    override val screenName = "Help"

    @Inject
    lateinit var settingsManager: SettingsManager

    override fun createFastAdapter(): FastAdapter<HelpListItem> {
        val itemAdapter = ItemAdapter<HelpListItem>()

        val list = mutableListOf<HelpListItem>()

        list += HelpListItem(
            StringHolder(R.string.id_i_typed_all_my_recovery_phrase),
            StringHolder(R.string.id_1_double_check_all_of_your),
            StringHolder(R.string.id_visit_the_blockstream_help)
        )

        itemAdapter.add(list)

        val fastAdapter = FastAdapter.with(itemAdapter)

        fastAdapter.addClickListener<ListItemHelpBinding, HelpListItem>({ binding -> binding.button }) { _, _, _, _ ->
            openBrowser(settingsManager.getApplicationSettings(), Urls.HELP_MNEMONIC_NOT_WORKING)
        }

        return fastAdapter
    }

    companion object : KLogging() {
        fun show(fragmentManager: FragmentManager){
            show(HelpBottomSheetDialogFragment(), fragmentManager)
        }
    }
}