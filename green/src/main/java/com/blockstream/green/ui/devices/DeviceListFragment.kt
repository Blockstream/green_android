package com.blockstream.green.ui.devices

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback
import com.blockstream.base.Urls
import com.blockstream.green.R
import com.blockstream.green.data.NavigateEvent
import com.blockstream.green.databinding.DeviceListFragmentBinding
import com.blockstream.green.databinding.HwConnectStepBinding
import com.blockstream.green.databinding.JadeConnectStepBinding
import com.blockstream.green.devices.Device
import com.blockstream.green.devices.DeviceManager
import com.blockstream.green.ui.items.DeviceListItem
import com.blockstream.green.utils.observeList
import com.blockstream.green.utils.openBrowser
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.adapters.ModelAdapter
import com.mikepenz.itemanimators.SlideDownAlphaAnimator
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import mu.KLogging
import javax.inject.Inject
import kotlin.time.DurationUnit
import kotlin.time.toDuration


@AndroidEntryPoint
class DeviceListFragment : AbstractDeviceFragment<DeviceListFragmentBinding>(
    layout = R.layout.device_list_fragment,
    menuRes = 0
) {
    private val args: DeviceListFragmentArgs by navArgs()

    override val screenName = "DeviceList"

    @Inject
    lateinit var viewModelFactory: DeviceListViewModel.AssistedFactory

    override val viewModel: DeviceListViewModel by viewModels {
        DeviceListViewModel.provideFactory(
            viewModelFactory,
            args.isJade
        )
    }

    @Inject
    lateinit var deviceManager: DeviceManager

    private var changeStepJob: Job? = null

    override val title: String
        get() = if (args.isJade) "Blockstream Jade" else ""

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.vm = viewModel

        requestPermission =
            registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { _ ->
                // Nothing to do here, it's already handled by DeviceManager
            }

        val devicesAdapter = ModelAdapter<Device, DeviceListItem>() {
            DeviceListItem(it)
        }.observeList(viewLifecycleOwner, viewModel.devices)

        val fastAdapter = FastAdapter.with(devicesAdapter)

        fastAdapter.onClickListener = { _, _, item, _ ->

            // Handle Jade as an already Bonded device
            if (item.device.hasPermissionsOrIsBonded() || item.device.handleBondingByHwwImplementation()) {
                selectDevice(item.device)
            } else {
                viewModel.askForPermissionOrBond(item.device)
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
            requestLocationPermission()
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

        viewModel.onEvent.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandledForType<NavigateEvent.NavigateWithData>()?.let { navigate ->
                selectDevice(navigate.data as Device)
            }
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
    }

    private fun changeStep() {
        changeStepJob?.cancel()

        val itemCount = binding.viewPager.adapter?.itemCount ?: 1

        if (itemCount < 2) {
            return
        }

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

    private fun selectDevice(device: Device) {
        navigate(DeviceListFragmentDirections.actionDeviceListFragmentToDeviceInfoFragment(deviceId = device.id))
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
                it.step.text = "${getString(R.string.id_step)} ${page + 1}".uppercase()

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

    companion object : KLogging() {
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