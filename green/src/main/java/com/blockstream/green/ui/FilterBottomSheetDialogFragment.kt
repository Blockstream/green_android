package com.blockstream.green.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.blockstream.green.databinding.FilterBottomSheetBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.GenericItem
import com.mikepenz.fastadapter.adapters.ModelAdapter
import com.mikepenz.itemanimators.SlideDownAlphaAnimator
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class FilterBottomSheetDialogFragment: BottomSheetDialogFragment(){

    private lateinit var binding: FilterBottomSheetBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FilterBottomSheetBinding.inflate(layoutInflater)

        // Keep the height of the window always constant
        val params = binding.linear.layoutParams as FrameLayout.LayoutParams
        params.height = resources.displayMetrics.heightPixels
        binding.linear.layoutParams = params

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

        return binding.root
    }
}

interface FilterableDataProvider{
    fun getModelAdapter() : ModelAdapter<*, *>
    fun filteredItemClicked(item: GenericItem, position: Int)
}