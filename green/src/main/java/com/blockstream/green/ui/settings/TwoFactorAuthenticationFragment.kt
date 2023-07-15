package com.blockstream.green.ui.settings

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.navArgs
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.blockstream.common.data.GreenWallet
import com.blockstream.common.gdk.data.Network
import com.blockstream.green.R
import com.blockstream.green.databinding.TwofactorAuthenticationFragmentBinding
import com.blockstream.green.ui.wallet.AbstractWalletFragment
import com.blockstream.green.ui.wallet.AbstractWalletViewModel
import com.blockstream.green.ui.wallet.WalletViewModel
import com.google.android.material.tabs.TabLayoutMediator
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf

class TwoFactorAuthenticationFragment : AbstractWalletFragment<TwofactorAuthenticationFragmentBinding>(
    R.layout.twofactor_authentication_fragment,
    0
) {
    val args: TwoFactorAuthenticationFragmentArgs by navArgs()
    override val walletOrNull by lazy { args.wallet }

    override val screenName = "WalletSettings2FA"

    override val subtitle: String
        get() = getString(R.string.id_multisig)

    val viewModel: WalletViewModel by viewModel {
        parametersOf(args.wallet)
    }

    override fun getWalletViewModel(): AbstractWalletViewModel = viewModel

    override fun onViewCreatedGuarded(view: View, savedInstanceState: Bundle?) {
        val networks = listOfNotNull(session.activeBitcoinMultisig, session.activeLiquidMultisig)

        binding.showTabs = networks.size > 1

        val adapter = TwoFactorAuthenticationPagerAdapter(
            wallet = wallet,
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