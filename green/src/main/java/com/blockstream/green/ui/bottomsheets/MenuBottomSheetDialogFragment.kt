package com.blockstream.green.ui.bottomsheets

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.blockstream.green.databinding.MenuBottomSheetBinding
import com.blockstream.green.ui.items.MenuListItem
import com.google.android.material.divider.MaterialDividerItemDecoration
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.GenericItem
import com.mikepenz.fastadapter.adapters.ItemAdapter
import com.mikepenz.itemanimators.SlideDownAlphaAnimator
import dagger.hilt.android.AndroidEntryPoint
import mu.KLogging

@AndroidEntryPoint
class MenuBottomSheetDialogFragment : AbstractBottomSheetDialogFragment<MenuBottomSheetBinding>() {
    override val screenName: String? = null

    override fun inflate(layoutInflater: LayoutInflater) = MenuBottomSheetBinding.inflate(layoutInflater)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val parent = (parentFragment ?: activity) as MenuDataProvider

        binding.title = arguments?.getString(TITLE)
        binding.subtitle = arguments?.getString(SUBTITLE)


        val itemAdapter = ItemAdapter<GenericItem>()
            .add(arguments?.getParcelableArrayList<MenuListItem>(MENU_ITEMS) ?: listOf())

        val fastAdapter = FastAdapter.with(itemAdapter)

        fastAdapter.onClickListener = { _, _, item, position ->
            parent.menuItemClicked(arguments?.getInt(REQUEST_CODE) ?: 0, item, position)
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
                MaterialDividerItemDecoration(
                    requireContext(),
                    DividerItemDecoration.VERTICAL
                ).also {
                    it.isLastItemDecorated = false
                }
            )
        }
    }

    companion object : KLogging() {
        const val REQUEST_CODE = "REQUEST_CODE"
        const val TITLE = "TITLE"
        const val SUBTITLE = "SUBTITLE"
        const val MENU_ITEMS = "MENU_ITEMS"

        fun show(requestCode: Int = 0, title: String? = null, subtitle: String? = null, menuItems: List<MenuListItem>, fragmentManager: FragmentManager){
            show(MenuBottomSheetDialogFragment().also {
                it.arguments = Bundle().also { bundle ->
                    bundle.putInt(REQUEST_CODE, requestCode)
                    bundle.putString(TITLE, title)
                    bundle.putString(SUBTITLE, subtitle)
                    bundle.putParcelableArrayList(MENU_ITEMS, ArrayList(menuItems))
                }
            }, fragmentManager)
        }

    }
}

interface MenuDataProvider {
    fun menuItemClicked(requestCode: Int, item: GenericItem, position: Int)
}
