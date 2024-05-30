package com.blockstream.green.ui.bottomsheets

import androidx.fragment.app.FragmentManager
import com.blockstream.common.managers.SettingsManager
import com.blockstream.green.R
import com.blockstream.green.ui.items.AbstractBindingItem
import com.blockstream.green.ui.items.ActionListItem
import com.blockstream.green.utils.StringHolder
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.adapters.ItemAdapter
import mu.KLogging
import org.koin.android.ext.android.inject


class ComingSoonBottomSheetDialogFragment: RecyclerBottomSheetDialogFragment() {

    override val screenName = "ComingSoon"

    private val settingsManager: SettingsManager by inject()

    override fun createFastAdapter(): FastAdapter<AbstractBindingItem<*>> {
        val itemAdapter = ItemAdapter<ActionListItem>()

        val list = mutableListOf<ActionListItem>()

        list += ActionListItem(
            message = StringHolder(requireContext(),R.string.id_this_feature_is_coming_soon),
        )

        itemAdapter.add(list)

        @Suppress("UNCHECKED_CAST")
        return FastAdapter.with(itemAdapter) as FastAdapter<AbstractBindingItem<*>>
    }

    override fun getTitle() = getString(R.string.id_coming_soon)

    companion object : KLogging() {
        fun show(fragmentManager: FragmentManager){
            show(ComingSoonBottomSheetDialogFragment(), fragmentManager)
        }
    }
}