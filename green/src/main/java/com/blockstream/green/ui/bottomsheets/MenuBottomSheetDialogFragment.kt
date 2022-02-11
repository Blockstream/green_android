package com.blockstream.green.ui.bottomsheets

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.blockstream.green.databinding.MenuBottomSheetBinding
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.GenericItem
import com.mikepenz.fastadapter.adapters.ItemAdapter
import com.mikepenz.itemanimators.SlideDownAlphaAnimator
import dagger.hilt.android.AndroidEntryPoint
import mu.KLogging

@AndroidEntryPoint
class MenuBottomSheetDialogFragment constructor(private val dataProvider: MenuDataProvider) : AbstractBottomSheetDialogFragment<MenuBottomSheetBinding>() {
    override val screenName: String? = null

    override fun inflate(layoutInflater: LayoutInflater) = MenuBottomSheetBinding.inflate(layoutInflater)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

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
    }

    companion object : KLogging() {
        fun show(dataProvider: MenuDataProvider, fragmentManager: FragmentManager){
            show(MenuBottomSheetDialogFragment(dataProvider), fragmentManager)
        }
    }
}

interface MenuDataProvider {
    fun getTitle(): String
    fun getSubtitle(): String?
    fun getMenuListItems(): List<GenericItem>
    fun menuItemClicked(item: GenericItem, position: Int)
}
