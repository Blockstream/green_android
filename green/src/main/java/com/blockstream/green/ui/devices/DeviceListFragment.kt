package com.blockstream.green.ui.devices

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback
import com.blockstream.common.Urls
import com.blockstream.common.devices.GreenDevice
import com.blockstream.common.models.GreenViewModel
import com.blockstream.common.sideeffects.SideEffect
import com.blockstream.common.sideeffects.SideEffects
import com.blockstream.common.utils.Loggable
import com.blockstream.green.R
import com.blockstream.green.databinding.DeviceListFragmentBinding
import com.blockstream.green.databinding.HwConnectStepBinding
import com.blockstream.green.databinding.JadeConnectStepBinding
import com.blockstream.green.devices.DeviceManagerAndroid
import com.blockstream.green.ui.items.DeviceListItem
import com.blockstream.green.utils.observeList
import com.blockstream.green.utils.openBrowser
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.adapters.ModelAdapter
import com.mikepenz.itemanimators.SlideDownAlphaAnimator
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf
import kotlin.time.DurationUnit
import kotlin.time.toDuration


class DeviceListFragment : AbstractDeviceFragment<DeviceListFragmentBinding>(
    layout = R.layout.device_list_fragment,
    menuRes = 0
) {
    private val args: DeviceListFragmentArgs by navArgs()

    override val screenName = "DeviceList"

    override val viewModel: DeviceListViewModel by viewModel {
        parametersOf(args.isJade)
    }

    override fun getGreenViewModel(): GreenViewModel = viewModel

    private val deviceManager: DeviceManagerAndroid by inject()

    private var changeStepJob: Job? = null

    override val title: String
        get() = if (args.isJade) "Blockstream Jade" else ""

    override suspend fun handleSideEffect(sideEffect: SideEffect) {
        super.handleSideEffect(sideEffect)
        if (sideEffect is SideEffects.Navigate){
            (sideEffect.data as? GreenDevice)?.also {
                selectDevice(it)
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.vm = viewModel

        val devicesAdapter = ModelAdapter<GreenDevice, DeviceListItem>() {
            DeviceListItem(it)
        }.observeList(viewLifecycleOwner, viewModel.devices)

        val fastAdapter = FastAdapter.with(devicesAdapter)

        fastAdapter.onClickListener = { _, _, item, _ ->

            // Handle Jade as an already Bonded device
            if (item.device.hasPermissions()) {
                selectDevice(item.device)
            } else {
                viewModel.askForPermission(item.device)
            }

            true
        }

        binding.recycler.apply {
            layoutManager = LinearLayoutManager(context)
            itemAnimator = SlideDownAlphaAnimator()
            adapter = fastAdapter
            isNestedScrollingEnabled = false
        }

        binding.buttonEnableBluetooth.setOnClickListener {
            enableBluetooth()
        }

        binding.buttonRequestPermission.setOnClickListener {
            requestPermissions()
        }

        binding.buttonEnableLocationService.setOnClickListener {
            enableLocationService()
        }

        binding.buttonLocationServiceMoreInfo.setOnClickListener {
            openBrowser(Urls.BLUETOOTH_PERMISSIONS)
        }

        binding.buttonTroubleshoot.setOnClickListener {
            openBrowser(Urls.JADE_TROUBLESHOOT)
        }

        binding.swipeRefreshLayout.setOnRefreshListener {
            binding.swipeRefreshLayout.isRefreshing = false
            deviceManager.refreshDevices()
        }


        binding.viewPager.adapter = PagerAdapter(isJade = args.isJade, fragment =this)

        binding.viewPager.registerOnPageChangeCallback(object : OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                changeStep()
            }
        })

        binding.buttonConnectViaQr.setOnClickListener {
            navigate(DeviceListFragmentDirections.actionGlobalJadeQrFragment())
        }
    }

    private fun changeStep() {
        changeStepJob?.cancel()

        val itemCount = binding.viewPager.adapter?.itemCount ?: 1

        if (itemCount < 2) {
            return
        }

        @Suppress("DEPRECATION")
        changeStepJob = lifecycleScope.launchWhenResumed {
            delay(5.toDuration(DurationUnit.SECONDS))

            binding.viewPager.setCurrentItem((binding.viewPager.currentItem + 1).takeIf {
                it < itemCount
            } ?: 0, true)
        }
    }

    override fun updateToolbar() {
        super.updateToolbar()

        if (args.isJade) {
            toolbar.setButton(button = getString(R.string.id_setup_guide)) {
                navigate(DeviceListFragmentDirections.actionDeviceListFragmentToJadeGuideFragment())
            }
        }
    }

    private fun selectDevice(device: GreenDevice) {
        navigate(DeviceListFragmentDirections.actionDeviceListFragmentToDeviceInfoFragment(deviceId = device.connectionIdentifier))
    }
}

class JadePageFragment : Fragment() {
    private var binding: JadeConnectStepBinding? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        return JadeConnectStepBinding.inflate(layoutInflater).also {
            requireArguments().getInt(PAGE, 0).also { page ->
                it.step.text = getString(R.string.id_step_s, "${page + 1}").uppercase()

                when (page) {
                    0 -> {
                        it.title.setText(R.string.id_power_on_jade)
                        it.subtitle.setText(R.string.id_hold_the_green_button_on_the)
                        it.rive.setRiveResource(R.raw.jade_power)
                    }

                    1 -> {
                        it.title.setText(R.string.id_follow_the_instructions_on_jade)
                        it.subtitle.setText(R.string.id_select_initialize_and_choose_to)
                        it.rive.setRiveResource(R.raw.jade_button)
                    }

                    else -> {
                        it.title.setText(R.string.id_connect_using_usb_or_bluetooth)
                        it.subtitle.setText(R.string.id_choose_a_usb_or_bluetooth)
                        it.rive.setRiveResource(R.raw.jade_scroll)
                    }
                }
            }
        }.also {
            it.lifecycleOwner = viewLifecycleOwner
            binding = it
        }.root
    }

    override fun onResume() {
        super.onResume()
        binding?.rive?.play()
    }

    override fun onPause() {
        super.onPause()
        binding?.rive?.stop()
    }

    companion object : Loggable() {
        private const val PAGE = "PAGE"

        fun newInstance(page: Int): JadePageFragment {
            return JadePageFragment().also {
                it.arguments = Bundle().also { bundle ->
                    bundle.putInt(PAGE, page)
                }
            }
        }
    }
}

class HWPageFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return HwConnectStepBinding.inflate(layoutInflater).also {
            it.lifecycleOwner = viewLifecycleOwner
        }.root
    }
}

class PagerAdapter constructor(val isJade: Boolean, fragment: Fragment) : FragmentStateAdapter(fragment) {
    override fun getItemCount(): Int = if (isJade) 3 else 1
    override fun createFragment(position: Int): Fragment =
        if (isJade) JadePageFragment.newInstance(position) else HWPageFragment()
}