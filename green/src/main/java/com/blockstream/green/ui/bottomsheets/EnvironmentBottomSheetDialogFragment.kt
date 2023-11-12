package com.blockstream.green.ui.bottomsheets

import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.blockstream.common.gdk.Gdk
import com.blockstream.common.gdk.data.Network
import com.blockstream.green.R
import com.blockstream.green.databinding.MenuBottomSheetBinding
import com.blockstream.green.ui.items.MenuListItem
import com.blockstream.green.utils.StringHolder
import com.google.android.material.divider.MaterialDividerItemDecoration
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.GenericItem
import com.mikepenz.fastadapter.adapters.ItemAdapter
import com.mikepenz.itemanimators.SlideDownAlphaAnimator
import mu.KLogging
import org.koin.android.ext.android.inject

class EnvironmentBottomSheetDialogFragment : AbstractBottomSheetDialogFragment<MenuBottomSheetBinding>() {

    override val screenName: String? = null

    private val gdk: Gdk by inject()

    override fun inflate(layoutInflater: LayoutInflater) = MenuBottomSheetBinding.inflate(layoutInflater)

    private var isTestnet : Boolean? = null
    private var customNetwork : Network? = null

    private val withCustomNetwork
        get() = requireArguments().getBoolean(WITH_CUSTOM_NETWORK, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.title = getString(R.string.id_select_network)

        val customNetworkMenu = gdk.networks().customNetwork.takeIf { withCustomNetwork }?.let {
            MenuListItem(
                icon = R.drawable.ic_pencil_simple_line,
                title = StringHolder(it.name)
            )
        }

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
            ) + listOfNotNull(customNetworkMenu))

        val fastAdapter = FastAdapter.with(itemAdapter)

        fastAdapter.onClickListener = { _, _, _, position ->
            isTestnet = position == 1
            customNetwork = if(position == 2) gdk.networks().customNetwork else null

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
        (requireParentFragment() as? EnvironmentListener)?.onEnvironmentSelected(isTestnet, customNetwork)
    }

    companion object : KLogging() {
        private const val WITH_CUSTOM_NETWORK = "WITH_CUSTOM_NETWORK"

        fun show(withCustomNetwork: Boolean = false, fragmentManager: FragmentManager){
            show(EnvironmentBottomSheetDialogFragment().also {
                it.arguments = Bundle().apply {
                    putBoolean(WITH_CUSTOM_NETWORK, withCustomNetwork)
                }
            }, fragmentManager)
        }
    }
}

interface EnvironmentListener {
    fun onEnvironmentSelected(isTestnet: Boolean?, customNetwork: Network?)
}