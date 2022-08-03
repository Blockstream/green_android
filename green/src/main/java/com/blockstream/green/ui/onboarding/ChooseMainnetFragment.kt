package com.blockstream.green.ui.onboarding

import android.os.Bundle
import android.view.View
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.blockstream.gdk.GdkBridge
import com.blockstream.gdk.data.Network
import com.blockstream.green.R
import com.blockstream.green.data.OnboardingOptions
import com.blockstream.green.databinding.ChooseNetworkFragmentBinding
import com.blockstream.green.ui.items.NetworkListItem
import com.blockstream.green.ui.items.TitleExpandableListItem
import com.mikepenz.fastadapter.GenericItem
import com.mikepenz.fastadapter.adapters.FastItemAdapter
import com.mikepenz.fastadapter.expandable.getExpandableExtension
import com.mikepenz.fastadapter.ui.utils.StringHolder
import com.mikepenz.itemanimators.SlideDownAlphaAnimator
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class ChooseMainnetFragment :
    AbstractOnboardingFragment<ChooseNetworkFragmentBinding>(
        R.layout.choose_network_fragment,
        menuRes = 0
    ) {

    @Inject
    lateinit var gdkBridge: GdkBridge

    private val args: ChooseMainnetFragmentArgs by navArgs()

    override val screenName = "OnBoardChooseNetwork"

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        options = args.onboardingOptions

        val fastItemAdapter = createNetworkAdapter()

        fastItemAdapter.onClickListener = { _, _, item: GenericItem, _ ->
            when (item) {
                is NetworkListItem -> {
                    options?.apply {
                        navigate(copy(isTestnet = Network.isTestnet(item.network)))
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

    private fun createNetworkAdapter(): FastItemAdapter<GenericItem> {
        val fastItemAdapter = FastItemAdapter<GenericItem>()
        fastItemAdapter.getExpandableExtension()

        fastItemAdapter.add(NetworkListItem(Network.GreenMainnet,"Mainnet", getString(R.string.id_bitcoin_is_the_worlds_leading)))
        fastItemAdapter.add(NetworkListItem(Network.GreenTestnet, "Testnet", ""))

        gdkBridge.networks.customNetwork?.let {
            val expandable = TitleExpandableListItem(StringHolder(R.string.id_additional_networks))
            expandable.subItems.add(
                NetworkListItem(
                    it.id,
                    it.name,
                    "Force usage of custom network. Multisig/Singlesig selection is irrelevant."
                )
            )

            fastItemAdapter.add(expandable)
        }

        return fastItemAdapter
    }

    private fun navigate(options: OnboardingOptions) {
        navigate(
            if(options.isRestoreFlow){
                ChooseMainnetFragmentDirections.actionChooseMainnetFragmentToEnterRecoveryPhraseFragment(
                    onboardingOptions = options
                )
            }else{
                ChooseMainnetFragmentDirections.actionChooseNetworkFragmentToRecoveryIntroFragment(
                    wallet = null, onboardingOptions = options
                )
            }

        )
    }
}
