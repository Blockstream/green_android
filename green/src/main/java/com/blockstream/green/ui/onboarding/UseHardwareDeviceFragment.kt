package com.blockstream.green.ui.onboarding

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.blockstream.common.models.GreenViewModel
import com.blockstream.common.models.onboarding.UseHardwareDeviceViewModel
import com.blockstream.common.navigation.NavigateDestinations
import com.blockstream.common.sideeffects.SideEffect
import com.blockstream.common.sideeffects.SideEffects
import com.blockstream.green.R
import com.blockstream.green.databinding.HardwarePageBinding
import com.blockstream.green.databinding.UseHardwareDeviceFragmentBinding
import com.blockstream.green.ui.AppFragment
import com.google.android.material.tabs.TabLayoutMediator
import mu.KLogging
import org.koin.androidx.viewmodel.ext.android.viewModel

class UseHardwareDeviceFragment : AppFragment<UseHardwareDeviceFragmentBinding>(
    R.layout.use_hardware_device_fragment,
    menuRes = 0
) {
    val viewModel: UseHardwareDeviceViewModel by viewModel()

    override fun getGreenViewModel(): GreenViewModel = viewModel

    override fun handleSideEffect(sideEffect: SideEffect) {
        super.handleSideEffect(sideEffect)

        if(sideEffect is SideEffects.NavigateTo){
            (sideEffect.destination as? NavigateDestinations.DeviceList)?.also {
                navigate(UseHardwareDeviceFragmentDirections.actionGlobalDeviceListFragment(isJade = it.isJade))
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.buttonConnectJade.setOnClickListener {
            viewModel.postEvent(UseHardwareDeviceViewModel.LocalEvents.ConnectJade)
        }

        binding.buttonConnectDifferentDevice.setOnClickListener {
            viewModel.postEvent(UseHardwareDeviceViewModel.LocalEvents.ConnectDifferentHardwareDevice)
        }

        binding.buttonStore.setOnClickListener {
            viewModel.postEvent(UseHardwareDeviceViewModel.LocalEvents.JadeStore)
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
                    it.subtitle.setText(R.string.id_jade_is_a_specialized_device)
                    it.imageView.setImageResource(R.drawable.jade_welcome)
                }
                1 -> {
                    it.title.setText(R.string.id_hardware_security)
                    it.subtitle.setText(R.string.id_your_bitcoin_and_liquid_assets)
                    it.imageView.setImageResource(R.drawable.hardware_security)
                }
                else -> {
                    it.title.setText(R.string.id_offline_key_storage)
                    it.subtitle.setText(R.string.id_jade_is_an_isolated_device_not)
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