package com.blockstream.green.ui.bottomsheets

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import com.blockstream.green.R
import com.blockstream.green.databinding.RecyclerBottomSheetBinding
import com.blockstream.green.ui.items.HelpListItem
import com.blockstream.green.utils.toPixels
import com.blockstream.green.views.SpaceItemDecoration
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.itemanimators.SlideDownAlphaAnimator

abstract class RecyclerBottomSheetDialogFragment : AbstractBottomSheetDialogFragment<RecyclerBottomSheetBinding>() {

    override fun inflate(layoutInflater: LayoutInflater) = RecyclerBottomSheetBinding.inflate(layoutInflater)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

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
    }

    protected open fun getTitle() = getString(R.string.id_help)

    abstract fun createFastAdapter(): FastAdapter<HelpListItem>
}