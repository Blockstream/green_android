package com.blockstream.green.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.LinearLayoutManager
import com.blockstream.green.R
import com.blockstream.green.Urls
import com.blockstream.green.databinding.HelpBottomSheetBinding
import com.blockstream.green.databinding.ListItemHelpBinding
import com.blockstream.green.ui.items.HelpListItem
import com.blockstream.green.utils.openBrowser
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.adapters.ItemAdapter
import com.mikepenz.fastadapter.binding.listeners.addClickListener
import com.mikepenz.fastadapter.ui.utils.StringHolder
import com.mikepenz.itemanimators.SlideDownAlphaAnimator
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class HelpBottomSheetDialogFragment : BottomSheetDialogFragment() {

    private lateinit var binding: HelpBottomSheetBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DataBindingUtil.inflate(
            layoutInflater,
            R.layout.help_bottom_sheet,
            container,
            false
        )

        binding.buttonClose.setOnClickListener {
            dismiss()
        }

        //create the ItemAdapter holding your Items
        val itemAdapter = ItemAdapter<HelpListItem>()
        //create the managing FastAdapter, by passing in the itemAdapter

        //set the items to your ItemAdapter
        val fastAdapter = createHelpForMnemonic(itemAdapter)

        binding.recycler.apply {
            layoutManager = LinearLayoutManager(context)
            itemAnimator = SlideDownAlphaAnimator()
            adapter = fastAdapter
        }

        return binding.root
    }

    private fun createHelpForMnemonic(itemAdapter: ItemAdapter<HelpListItem>): FastAdapter<HelpListItem> {

        // TODO Make this configurable for every screen
        val list = mutableListOf<HelpListItem>()

        list += HelpListItem(
            StringHolder(R.string.id_i_typed_all_my_recovery_phrase),
            StringHolder(R.string.id_1_double_check_all_of_your),
            StringHolder(R.string.id_visit_the_blockstream_help)
        )

        itemAdapter.add(list)

        val fastAdapter = FastAdapter.with(itemAdapter)

        fastAdapter.addClickListener<ListItemHelpBinding, HelpListItem>({ binding -> binding.button }) { _, _, _, _ ->
            // do something
            openBrowser(requireContext(), Urls.HELP_MNEMONIC_NOT_WORKING)
        }


        return fastAdapter
    }

}