package com.blockstream.green.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.LinearLayoutManager
import com.blockstream.green.R
import com.blockstream.green.databinding.HelpBottomSheetBinding
import com.blockstream.green.ui.items.HelpListItem
import com.blockstream.green.utils.toPixels
import com.blockstream.green.views.SpaceItemDecoration
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.itemanimators.SlideDownAlphaAnimator

abstract class AbstractHelpBottomSheetDialogFragment : BottomSheetDialogFragment() {

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

        binding.title = getTitle()

        binding.buttonClose.setOnClickListener {
            dismiss()
        }

        binding.recycler.apply {
            layoutManager = LinearLayoutManager(context)
            itemAnimator = SlideDownAlphaAnimator()
            adapter = createFastAdapter()
            addItemDecoration(SpaceItemDecoration(toPixels(24)))
        }

        return binding.root
    }

    protected open fun getTitle() = getString(R.string.id_help)

    abstract fun createFastAdapter(): FastAdapter<HelpListItem>
}