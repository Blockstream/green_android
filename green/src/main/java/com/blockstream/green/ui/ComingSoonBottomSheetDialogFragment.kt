package com.blockstream.green.ui

import com.blockstream.green.R
import com.blockstream.green.settings.SettingsManager
import com.blockstream.green.ui.items.HelpListItem
import com.blockstream.green.utils.StringHolder
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.adapters.ItemAdapter
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class ComingSoonBottomSheetDialogFragment : RecyclerBottomSheetDialogFragment() {

    @Inject
    lateinit var settingsManager: SettingsManager

    override fun createFastAdapter(): FastAdapter<HelpListItem> {
        val itemAdapter = ItemAdapter<HelpListItem>()

        val list = mutableListOf<HelpListItem>()

        list += HelpListItem(
            StringHolder(R.string.id_this_feature_is_coming_soon),
        )

        itemAdapter.add(list)

        return FastAdapter.with(itemAdapter)
    }

    override fun getTitle() = getString(R.string.id_coming_soon)
}