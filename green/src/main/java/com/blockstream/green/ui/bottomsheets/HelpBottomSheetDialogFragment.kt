package com.blockstream.green.ui.bottomsheets

import androidx.fragment.app.FragmentManager
import com.blockstream.common.Urls
import com.blockstream.common.managers.SettingsManager
import com.blockstream.green.R
import com.blockstream.green.databinding.ListItemActionBinding
import com.blockstream.green.ui.items.AbstractBindingItem
import com.blockstream.green.ui.items.ActionListItem
import com.blockstream.green.utils.StringHolder
import com.blockstream.green.utils.openBrowser
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.adapters.ItemAdapter
import com.mikepenz.fastadapter.binding.listeners.addClickListener
import mu.KLogging
import org.koin.android.ext.android.inject


class HelpBottomSheetDialogFragment: RecyclerBottomSheetDialogFragment() {
    override val screenName = "Help"

    private val settingsManager: SettingsManager by inject()

    override fun createFastAdapter(): FastAdapter<AbstractBindingItem<*>> {
        val itemAdapter = ItemAdapter<ActionListItem>()

        val list = mutableListOf<ActionListItem>()

        list += ActionListItem(
            StringHolder(R.string.id_i_typed_all_my_recovery_phrase),
            StringHolder(R.string.id_1_double_check_all_of_your),
            StringHolder(R.string.id_visit_the_blockstream_help)
        )

        itemAdapter.add(list)

        val fastAdapter = FastAdapter.with(itemAdapter)

        fastAdapter.addClickListener<ListItemActionBinding, ActionListItem>({ binding -> binding.button }) { _, _, _, _ ->
            openBrowser(settingsManager.getApplicationSettings(), Urls.HELP_MNEMONIC_NOT_WORKING)
        }

        @Suppress("UNCHECKED_CAST")
        return fastAdapter as FastAdapter<AbstractBindingItem<*>>
    }

    companion object : KLogging() {
        fun show(fragmentManager: FragmentManager){
            show(HelpBottomSheetDialogFragment(), fragmentManager)
        }
    }
}