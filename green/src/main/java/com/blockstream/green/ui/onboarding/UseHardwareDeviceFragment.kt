package com.blockstream.green.ui.onboarding

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.blockstream.base.Urls
import com.blockstream.green.R
import com.blockstream.green.databinding.HardwarePageBinding
import com.blockstream.green.databinding.UseHardwareDeviceFragmentBinding
import com.blockstream.green.utils.openBrowser
import com.google.android.material.tabs.TabLayoutMediator
import dagger.hilt.android.AndroidEntryPoint
import mu.KLogging

@AndroidEntryPoint
class UseHardwareDeviceFragment :
    AbstractOnboardingFragment<UseHardwareDeviceFragmentBinding>(
        R.layout.use_hardware_device_fragment,
        menuRes = 0
    ) {

    override val screenName = "UseHardwareDevice"

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.buttonConnectJade.setOnClickListener {
            navigate(UseHardwareDeviceFragmentDirections.actionGlobalDeviceListFragment(isJade = true))
        }

        binding.buttonConnectDifferentDevice.setOnClickListener {
            navigate(UseHardwareDeviceFragmentDirections.actionGlobalDeviceListFragment(isJade = false))
        }

        binding.buttonStore.setOnClickListener {
            openBrowser(settingsManager.getApplicationSettings(), Urls.JADE_STORE)
        }

        val adapter = PagerAdapter(this)
        binding.viewPager.adapter = adapter

        TabLayoutMediator(binding.tabLayout, binding.viewPager) { _, _ ->

        }.attach()
    }
}

class PageFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        return HardwarePageBinding.inflate(layoutInflater).also {

            when (requireArguments().getInt(PAGE, 0)) {
                0 -> {
                    it.title.setText(R.string.id_welcome_to_blockstream_jade)
                    it.subtitle.setText(R.string.id_jade_is_a_specialized_device_designed_to)
                    it.imageView.setImageResource(R.drawable.jade_welcome)
                }
                1 -> {
                    it.title.setText(R.string.id_hardware_security)
                    it.subtitle.setText(R.string.id_your_bitcoin_and_liquid_assets_are_store_on_the_blockchain)
                    it.imageView.setImageResource(R.drawable.hardware_security)
                }
                else -> {
                    it.title.setText(R.string.id_offline_key_storage)
                    it.subtitle.setText(R.string.id_jade_is_an_isolated_device_not_connected_to_the_internet)
                    it.imageView.setImageResource(R.drawable.offline_key_storage)
                }
            }

        }.also {
            it.lifecycleOwner = viewLifecycleOwner
        }.root
    }

    companion object : KLogging() {
        private const val PAGE = "PAGE"

        fun newInstance(page: Int): PageFragment {
            return PageFragment().also {
                it.arguments = Bundle().also { bundle ->
                    bundle.putInt(PAGE, page)
                }
            }
        }
    }
}

class PagerAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {
    override fun getItemCount(): Int = 3
    override fun createFragment(position: Int): Fragment = PageFragment.newInstance(position)
}