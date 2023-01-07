package com.blockstream.green.ui.bottomsheets

import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.blockstream.gdk.GdkBridge
import com.blockstream.green.R
import com.blockstream.green.databinding.MenuBottomSheetBinding
import com.blockstream.green.ui.items.MenuListItem
import com.blockstream.green.utils.StringHolder
import com.google.android.material.divider.MaterialDividerItemDecoration
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.GenericItem
import com.mikepenz.fastadapter.adapters.ItemAdapter
import com.mikepenz.itemanimators.SlideDownAlphaAnimator
import dagger.hilt.android.AndroidEntryPoint
import mu.KLogging
import javax.inject.Inject

@AndroidEntryPoint
class EnvironmentBottomSheetDialogFragment : AbstractBottomSheetDialogFragment<MenuBottomSheetBinding>() {

    @Inject
    lateinit var gdkBridge: GdkBridge

    override val screenName: String? = null

    override fun inflate(layoutInflater: LayoutInflater) = MenuBottomSheetBinding.inflate(layoutInflater)

    private var isTestnet : Boolean? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.title = getString(R.string.id_select_network)

        val itemAdapter = ItemAdapter<GenericItem>()
            .add(listOf(
                MenuListItem(
                    icon = R.drawable.ic_regular_currency_btc_24,
                    title = StringHolder("Mainnet")
                ),
                MenuListItem(
                    icon = R.drawable.ic_regular_flask_24,
                    title = StringHolder("Testnet")
                )
            ))

        val fastAdapter = FastAdapter.with(itemAdapter)

        fastAdapter.onClickListener = { _, _, _, position ->

            isTestnet = position == 1

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

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        (requireParentFragment() as? EnvironmentListener)?.onEnvironmentSelected(isTestnet)
    }

    companion object : KLogging() {
        fun show(fragmentManager: FragmentManager){
            show(EnvironmentBottomSheetDialogFragment(), fragmentManager)
        }
    }
}

interface EnvironmentListener {
    fun onEnvironmentSelected(isTestnet: Boolean?)
}