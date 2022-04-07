package com.blockstream.green.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.blockstream.green.databinding.MenuBottomSheetBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.GenericItem
import com.mikepenz.fastadapter.adapters.ItemAdapter
import com.mikepenz.itemanimators.SlideDownAlphaAnimator
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MenuBottomSheetDialogFragment constructor(private val dataProvider: MenuDataProvider) :
    BottomSheetDialogFragment() {

    private lateinit var binding: MenuBottomSheetBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = MenuBottomSheetBinding.inflate(layoutInflater)
        
        binding.title = dataProvider.getTitle()
        binding.subtitle = dataProvider.getSubtitle()

        val itemAdapter = ItemAdapter<GenericItem>()
            .add(dataProvider.getMenuListItems())

        val fastAdapter = FastAdapter.with(itemAdapter)

        fastAdapter.onClickListener = { _, _, item, position ->
            dataProvider.menuItemClicked(item, position)
            dismiss()
            true
        }

        binding.buttonClose.setOnClickListener {
            dismiss()
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

        return binding.root
    }

    fun show(childFragmentManager: FragmentManager) {
        show(childFragmentManager, this.toString())
    }
}

interface MenuDataProvider {
    fun getTitle(): String
    fun getSubtitle(): String?
    fun getMenuListItems(): List<GenericItem>
    fun menuItemClicked(item: GenericItem, position: Int)
}
