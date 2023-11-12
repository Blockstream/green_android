package com.blockstream.green.ui.onboarding

import android.os.Bundle
import android.view.View
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.blockstream.common.data.SetupArgs
import com.blockstream.common.gdk.data.Network
import com.blockstream.common.models.GreenViewModel
import com.blockstream.common.models.onboarding.ChooseNetworkViewModel
import com.blockstream.common.sideeffects.SideEffect
import com.blockstream.common.sideeffects.SideEffects
import com.blockstream.green.R
import com.blockstream.green.databinding.ChooseNetworkFragmentBinding
import com.blockstream.green.ui.items.NetworkListItem
import com.blockstream.green.ui.items.TitleExpandableListItem
import com.mikepenz.fastadapter.GenericItem
import com.mikepenz.fastadapter.adapters.FastItemAdapter
import com.mikepenz.fastadapter.expandable.getExpandableExtension
import com.mikepenz.fastadapter.ui.utils.StringHolder
import com.mikepenz.itemanimators.SlideDownAlphaAnimator
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf

class ChooseNetworkFragment :
    AbstractOnboardingFragment<ChooseNetworkFragmentBinding>(
        R.layout.choose_network_fragment,
        menuRes = 0
    ) {

    private val args: ChooseNetworkFragmentArgs by navArgs()


    val viewModel: ChooseNetworkViewModel by viewModel {
        parametersOf(args.setupArgs)
    }

    override fun getGreenViewModel(): GreenViewModel = viewModel

    override fun handleSideEffect(sideEffect: SideEffect) {
        super.handleSideEffect(sideEffect)

        if(sideEffect is SideEffects.Navigate){
            (sideEffect.data as? SetupArgs)?.also {
                navigate(
                    ChooseNetworkFragmentDirections.actionChooseNetworkFragmentToWatchOnlyCredentialsFragment(
                        it
                    )
                )
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupArgs = args.setupArgs

        val fastItemAdapter = FastItemAdapter<GenericItem>()
        fastItemAdapter.getExpandableExtension()

        updateNetworkAdapter(fastItemAdapter)

        fastItemAdapter.onClickListener = { _, _, item: GenericItem, _ ->
            when (item) {
                is NetworkListItem -> {
                    setupArgs?.apply {
                        navigate(copy(network = item.network))
                    }
                    true
                }
                else -> false
            }
        }

        binding.recycler.apply {
            layoutManager = LinearLayoutManager(context)
            itemAnimator = SlideDownAlphaAnimator()
            adapter = fastItemAdapter
        }
    }

    private fun updateNetworkAdapter(fastItemAdapter: FastItemAdapter<GenericItem>) {
        val list = mutableListOf<GenericItem>()

        list += viewModel.mainNetworks.value.map {
            NetworkListItem(it, getCaption(it))
        }

        viewModel.additionalNetworks.value.also {
            if(it.isNotEmpty()){
                val expandable = TitleExpandableListItem(StringHolder(R.string.id_additional_networks))

                it.forEach {
                    expandable.subItems.add(NetworkListItem(it, getCaption(it)))
                }

                list += expandable
            }
        }

        fastItemAdapter.set(list)
    }

    private fun getCaption(network: Network): String {
        return (if(network.isBitcoin){
            getString(R.string.id_bitcoin_is_the_worlds_leading)
        }else if(network.isLiquid){
            getString(R.string.id_the_liquid_network_is_a_bitcoin)
        }else{
            ""
        })
    }

    private fun navigate(setupArgs: SetupArgs) {
        navigate(
            ChooseNetworkFragmentDirections.actionChooseNetworkFragmentToWatchOnlyCredentialsFragment(
                setupArgs
            )
        )
    }
}
