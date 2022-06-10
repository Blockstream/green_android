package com.blockstream.green.ui.onboarding

import android.os.Bundle
import android.view.View
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.blockstream.gdk.GreenWallet
import com.blockstream.gdk.data.Network
import com.blockstream.green.R
import com.blockstream.green.data.OnboardingOptions
import com.blockstream.green.databinding.ChooseNetworkFragmentBinding
import com.blockstream.green.ui.bottomsheets.ComingSoonBottomSheetDialogFragment
import com.blockstream.green.ui.items.NetworkListItem
import com.blockstream.green.ui.items.TitleExpandableListItem
import com.blockstream.green.utils.isDevelopmentFlavor
import com.mikepenz.fastadapter.GenericItem
import com.mikepenz.fastadapter.adapters.FastItemAdapter
import com.mikepenz.fastadapter.expandable.getExpandableExtension
import com.mikepenz.fastadapter.ui.utils.StringHolder
import com.mikepenz.itemanimators.SlideDownAlphaAnimator
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class ChooseNetworkFragment :
    AbstractOnboardingFragment<ChooseNetworkFragmentBinding>(
        R.layout.choose_network_fragment,
        menuRes = 0
    ) {

    @Inject
    lateinit var greenWallet: GreenWallet

    private val args: ChooseNetworkFragmentArgs by navArgs()

    override val screenName = "OnBoardChooseNetwork"

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        options = args.onboardingOptions

        val fastItemAdapter = createNetworkAdapter()

        fastItemAdapter.onClickListener = { _, _, item: GenericItem, _ ->
            when (item) {
                is NetworkListItem -> {
                    options?.apply {
                        if(isRestoreFlow){
                            navigate(createCopyForNetwork(greenWallet, item.network, isSinglesig == true))
                        }else{
                            navigate(copy(networkType = item.network))
                        }
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

        fastItemAdapter.add(NetworkListItem(Network.GreenMainnet,"Bitcoin", getCaption(Network.GreenMainnet)))
        fastItemAdapter.add(NetworkListItem(Network.GreenLiquid, "Liquid", getCaption(Network.GreenLiquid)))

        if(settingsManager.getApplicationSettings().testnet) {
            val expandable = TitleExpandableListItem(StringHolder(R.string.id_additional_networks))
            expandable.subItems.add(
                NetworkListItem(
                    Network.GreenTestnet,
                    "Testnet",
                    getCaption("testnet")
                )
            )

            expandable.subItems.add(
                NetworkListItem(
                    Network.GreenTestnetLiquid,
                    "Testnet Liquid",
                    getCaption("testnet-liquid")
                )
            )

            greenWallet.networks.customNetwork?.let {
                expandable.subItems.add(
                    NetworkListItem(
                        it.id,
                        it.name,
                        "Force usage of custom network. Multisig/Singlesig selection is irrelevant."
                    )
                )
            }

            fastItemAdapter.add(expandable)
        }

        return fastItemAdapter
    }

    private fun getCaption(network: String): String {
        return when (network) {
            Network.GreenMainnet -> getString(R.string.id_bitcoin_is_the_worlds_leading)
            Network.GreenLiquid -> getString(R.string.id_the_liquid_network_is_a_bitcoin)
            else -> ""
        }
    }

    private fun navigate(options: OnboardingOptions) {
        if(options.isRestoreFlow){
            if(options.isWatchOnly){
                if(isDevelopmentFlavor || options.network?.isLiquid != true){
                    navigate(
                        ChooseNetworkFragmentDirections.actionChooseNetworkFragmentToLoginWatchOnlyFragment(
                            options
                        )
                    )
                }else{
                    ComingSoonBottomSheetDialogFragment.show(childFragmentManager)
                }
            }else{
                navigate(
                    ChooseNetworkFragmentDirections.actionChooseNetworkFragmentToChooseRecoveryPhraseFragment(
                        options
                    )
                )
            }
        }else {
            navigate(
                ChooseNetworkFragmentDirections.actionChooseNetworkFragmentToChooseSecurityFragment(
                    options
                )
            )
        }
    }
}
