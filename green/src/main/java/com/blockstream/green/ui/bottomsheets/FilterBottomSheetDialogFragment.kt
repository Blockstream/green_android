package com.blockstream.green.ui.bottomsheets

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.blockstream.green.R
import com.blockstream.green.databinding.FilterBottomSheetBinding
import com.blockstream.green.extensions.endIconCustomMode
import com.blockstream.green.extensions.makeItConstant
import com.google.android.material.divider.MaterialDividerItemDecoration
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.GenericItem
import com.mikepenz.fastadapter.adapters.GenericFastItemAdapter
import com.mikepenz.fastadapter.adapters.ModelAdapter
import com.mikepenz.itemanimators.AlphaCrossFadeAnimator
import dagger.hilt.android.AndroidEntryPoint
import mu.KLogging

@AndroidEntryPoint
open class FilterBottomSheetDialogFragment: AbstractBottomSheetDialogFragment<FilterBottomSheetBinding>(){

    override val screenName = "Filter"

    open val withDivider: Boolean get() = arguments?.getBoolean(WITH_DIVIDER , true) ?: true
    open val withSearch: Boolean get() = arguments?.getBoolean(WITH_SEARCH , true) ?: true

    val requestCode by lazy { arguments?.getInt(REQUEST_CODE) ?: 0 }

    protected lateinit var fastAdapter: FastAdapter<GenericItem>

    override fun inflate(layoutInflater: LayoutInflater) = FilterBottomSheetBinding.inflate(layoutInflater)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.showLoader = false
        binding.showSearch = withSearch


        // Keep the height of the window always constant
        makeItConstant()

        // QATesterActivity is the only activity that uses it without Fragment
        val parent = if(this is FilterableDataProvider) this else (parentFragment ?: activity) as FilterableDataProvider

        val modelAdapter = parent.getFilterAdapter(requestCode)
        val headerAdapter = parent.getFilterHeaderAdapter(requestCode)
        val footerAdapter = parent.getFilterFooterAdapter(requestCode)

        fastAdapter = FastAdapter.with(
            listOfNotNull(
                headerAdapter,
                modelAdapter,
                footerAdapter
            )
        )

        fastAdapter.onClickListener = { _, _, item, position ->
            parent.filteredItemClicked(requestCode, item, position)
            dismiss()
            true
        }

        binding.showDivider = withDivider

        binding.recycler.apply {
            layoutManager = LinearLayoutManager(context)
            itemAnimator = AlphaCrossFadeAnimator()
            adapter = fastAdapter
            if(withDivider) {
                addItemDecoration(
                    MaterialDividerItemDecoration(
                        requireContext(),
                        DividerItemDecoration.VERTICAL
                    ).also {
                        it.isLastItemDecorated = footerAdapter == null
                    }
                )
            }
        }

        binding.searchTextInputLayout.endIconCustomMode(R.drawable.ic_baseline_search_24)

        binding.searchInputEditText.addTextChangedListener {
            modelAdapter.filter(it)
            footerAdapter?.itemAdapter?.active = it.isNullOrBlank()
        }
    }

    companion object : KLogging() {
        const val REQUEST_CODE = "REQUEST_CODE"
        const val WITH_DIVIDER = "WITH_DIVIDER"
        const val WITH_SEARCH = "WITH_SEARCH"

        fun show(requestCode: Int = 0, withDivider: Boolean = true, withSearch: Boolean = true, fragmentManager: FragmentManager){
            showSingle(FilterBottomSheetDialogFragment().also {
                it.arguments = Bundle().also { bundle ->
                    bundle.putInt(REQUEST_CODE, requestCode)
                    bundle.putBoolean(WITH_DIVIDER, withDivider)
                    bundle.putBoolean(WITH_SEARCH, withSearch)
                }
            }, fragmentManager)
        }
    }
}

interface FilterableDataProvider{
    fun getFilterAdapter(requestCode: Int) : ModelAdapter<*, *>
    fun getFilterHeaderAdapter(requestCode: Int) : GenericFastItemAdapter?
    fun getFilterFooterAdapter(requestCode: Int) : GenericFastItemAdapter?
    fun filteredItemClicked(requestCode: Int, item: GenericItem, position: Int)
}