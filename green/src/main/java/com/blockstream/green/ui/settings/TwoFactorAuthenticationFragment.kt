package com.blockstream.green.ui.settings

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.navArgs
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.blockstream.common.data.GreenWallet
import com.blockstream.common.gdk.data.Network
import com.blockstream.common.models.GreenViewModel
import com.blockstream.common.models.settings.TwoFactorAuthenticationViewModel
import com.blockstream.green.R
import com.blockstream.green.databinding.TwofactorAuthenticationFragmentBinding
import com.blockstream.green.ui.AppFragment
import com.google.android.material.tabs.TabLayoutMediator
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf

class TwoFactorAuthenticationFragment : AppFragment<TwofactorAuthenticationFragmentBinding>(
    R.layout.twofactor_authentication_fragment,
    0
) {
    val args: TwoFactorAuthenticationFragmentArgs by navArgs()

    override val subtitle: String
        get() = getString(R.string.id_multisig)

    val viewModel: TwoFactorAuthenticationViewModel by viewModel {
        parametersOf(args.wallet)
    }

    override fun getGreenViewModel(): GreenViewModel = viewModel

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val networks = listOfNotNull(viewModel.session.activeBitcoinMultisig, viewModel.session.activeLiquidMultisig)

        binding.showTabs = networks.size > 1

        val adapter = TwoFactorAuthenticationPagerAdapter(
            wallet = viewModel.greenWallet,
            fragment = this,
            networks = networks
        )

        binding.viewPager.adapter = adapter

        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = networks[position].canonicalName
        }.attach()

        args.network?.also {
            binding.viewPager.setCurrentItem(networks.indexOf(it), false)
        }
    }
}

class TwoFactorAuthenticationPagerAdapter(
    val wallet: GreenWallet,
    val fragment: Fragment,
    val networks: List<Network>
) : FragmentStateAdapter(fragment) {
    override fun getItemCount(): Int {
        return networks.size
    }

    override fun createFragment(position: Int): Fragment {
        return NetworkTwoFactorAuthenticationFragment().also {
            it.arguments = TwoFactorAuthenticationFragmentArgs(
                wallet = wallet,
                network = networks[position]
            ).toBundle()
        }
    }
}