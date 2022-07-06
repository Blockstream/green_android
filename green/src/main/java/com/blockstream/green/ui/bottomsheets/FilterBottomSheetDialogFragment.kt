package com.blockstream.green.ui.bottomsheets

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.blockstream.green.databinding.FilterBottomSheetBinding
import com.blockstream.green.utils.makeItConstant
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.GenericItem
import com.mikepenz.fastadapter.adapters.ModelAdapter
import com.mikepenz.itemanimators.SlideDownAlphaAnimator
import dagger.hilt.android.AndroidEntryPoint
import mu.KLogging

@AndroidEntryPoint
class FilterBottomSheetDialogFragment: AbstractBottomSheetDialogFragment<FilterBottomSheetBinding>(){
    override val screenName = "Filter"

    override fun inflate(layoutInflater: LayoutInflater) = FilterBottomSheetBinding.inflate(layoutInflater)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Keep the height of the window always constant
        makeItConstant()

        // QATesterActivity is the only activity that uses it without Fragment
        val parent = (parentFragment ?: activity) as FilterableDataProvider

        val modelAdapter = parent.getModelAdapter()

        val fastAdapter = FastAdapter.with(modelAdapter)

        fastAdapter.onClickListener = { _, _, item, position ->
            parent.filteredItemClicked(item, position)
            dismiss()
            true
        }

        binding.recycler.apply {
            layoutManager = LinearLayoutManager(context)
            itemAnimator = SlideDownAlphaAnimator()
            adapter = fastAdapter
            addItemDecoration(
                DividerItemDecoration(
                    requireContext(),
                    DividerItemDecoration.VERTICAL
                )
            )
        }

        binding.editTextSearch.addTextChangedListener {
            modelAdapter.filter(it)
        }
    }

    companion object : KLogging() {
        fun show(fragmentManager: FragmentManager){
            show(FilterBottomSheetDialogFragment(), fragmentManager)
        }
    }
}

interface FilterableDataProvider{
    fun getModelAdapter() : ModelAdapter<*, *>
    fun filteredItemClicked(item: GenericItem, position: Int)
}