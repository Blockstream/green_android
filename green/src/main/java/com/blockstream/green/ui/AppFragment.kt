package com.blockstream.green.ui


import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.*
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.IdRes
import androidx.annotation.LayoutRes
import androidx.annotation.MenuRes
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.fragment.app.Fragment
import androidx.lifecycle.MutableLiveData
import androidx.navigation.NavDirections
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.blockstream.gdk.data.Device
import com.blockstream.green.data.AppEvent
import com.blockstream.green.database.Wallet
import com.blockstream.green.gdk.SessionManager
import com.blockstream.green.gdk.getIcon
import com.blockstream.green.settings.SettingsManager
import com.blockstream.green.ui.devices.DeviceInteractionRequestBottomSheetDialogFragment
import com.blockstream.green.utils.ConsumableEvent
import com.blockstream.green.utils.navigate
import com.greenaddress.greenbits.ui.GaActivity
import com.greenaddress.greenbits.ui.authentication.TrezorPassphraseActivity
import com.greenaddress.greenbits.ui.authentication.TrezorPinActivity
import io.reactivex.rxjava3.core.Completable
import mu.KLogging
import javax.inject.Inject


/**
 * AppFragment
 *
 * This class is a useful abstract base class. Extend all other Fragments if possible.
 * Some of the features can be turned on/off in the constructor.
 *
 * It's crucial every AppFragment implementation to call @AndroidEntryPoint
 *
 * @property layout the layout id of the fragment
 * is called when the fragment is not actually visible
 *
 */

abstract class AppFragment<T : ViewDataBinding>(
    @LayoutRes val layout: Int,
    @MenuRes val menuRes: Int
) : Fragment() {

    sealed class DeviceRequestEvent : AppEvent {
        object RequestPinMatrix: DeviceRequestEvent()
        object RequestPassphrase: DeviceRequestEvent()
    }

    open val isAdjustResize = false

    internal lateinit var binding: T

    @Inject
    internal lateinit var sessionManager: SessionManager

    @Inject
    lateinit var settingsManager: SettingsManager

    open fun getAppViewModel(): AppViewModel? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DataBindingUtil.inflate(layoutInflater, layout, container, false)
        binding.lifecycleOwner = viewLifecycleOwner

        if (menuRes > 0) {
            setHasOptionsMenu(true)
        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        getAppViewModel()?.let { viewModel ->
            viewModel.onEvent.observe(viewLifecycleOwner) { onEvent ->
                onEvent.getContentIfNotHandledForType<DeviceRequestEvent>()?.let {
                    when(it){
                        is DeviceRequestEvent.RequestPinMatrix -> {
                            requestPinMatrix()
                        }
                        is DeviceRequestEvent.RequestPassphrase -> {
                            requestPassphrase()
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()

        // Prevent DrawerFragment from corrupting the main fragment
        if(this !is DrawerFragment) {
            requireActivity().window.setSoftInputMode(if (isAdjustResize) WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE else WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        if (menuRes > 0) {
            inflater.inflate(menuRes, menu)
        }
    }

    protected fun closeDrawer() {
        (requireActivity() as AppActivity).closeDrawer()
    }

    protected fun isDrawerOpen() = (requireActivity() as AppActivity).isDrawerOpen()

    fun setToolbar(wallet: Wallet) {
        val icon = ContextCompat.getDrawable(requireContext(), wallet.getIcon())
        setToolbar(wallet.name, subtitle = null, drawable = icon)
    }

    fun setToolbar(title: String? = null, subtitle: String? = null, drawable: Drawable? = null, button: CharSequence? = null,
                   buttonListener: View.OnClickListener? = null){
        (requireActivity() as AppActivity).setToolbar(title, subtitle, drawable, button, buttonListener)
    }

    fun setToolbarVisibility(isVisible: Boolean){
        (requireActivity() as AppActivity).setToolbarVisibility(isVisible)
    }

    fun navigate(directions: NavDirections, navOptionsBuilder: NavOptions.Builder? = null) {
        navigate(directions.actionId, directions.arguments, false, navOptionsBuilder)
    }

    fun navigate(@IdRes resId: Int, navOptionsBuilder: NavOptions.Builder? = null) {
        navigate(resId, null, false, navOptionsBuilder)
    }

    @SuppressLint("RestrictedApi")
    fun navigate(@IdRes resId: Int, args: Bundle?, isLogout: Boolean = false, optionsBuilder: NavOptions.Builder? = null) {
        navigate(findNavController(), resId, args, isLogout, optionsBuilder)
    }

    internal open fun popBackStack(){
        findNavController().popBackStack()
    }

    internal fun setupDeviceInteractionEvent(onDeviceInteractionEvent: MutableLiveData<ConsumableEvent<Triple<Device, Completable?, String?>>>) {
        onDeviceInteractionEvent.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandledOrReturnNull()?.let {
                DeviceInteractionRequestBottomSheetDialogFragment(device = it.first, completable = it.second, text = it.third).also {
                    it.show(childFragmentManager, it.toString())
                }
            }
        }
    }

    private val startForResultPinMatrix = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
        if (result.resultCode == Activity.RESULT_OK) {
            val intent = result.data
            getAppViewModel()?.requestPinMatrixEmitter?.onSuccess(intent?.getStringExtra(GaActivity.HARDWARE_PIN_REQUEST.toString()) ?: "")
        }else{
            getAppViewModel()?.requestPinMatrixEmitter?.onError(Exception("id_action_canceled"))
        }
    }

    private val startForResultPassphrase = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
        if (result.resultCode == Activity.RESULT_OK) {
            val intent = result.data
            getAppViewModel()?.requestPinPassphraseEmitter?.onSuccess(intent?.getStringExtra(GaActivity.HARDWARE_PASSPHRASE_REQUEST.toString()) ?: "")
        }else{
            getAppViewModel()?.requestPinPassphraseEmitter?.onError(Exception("id_action_canceled"))
        }
    }

    private fun requestPinMatrix() {
        startForResultPinMatrix.launch(Intent(requireContext(), TrezorPinActivity::class.java))
    }

    private fun requestPassphrase() {
        startForResultPassphrase.launch(Intent(requireContext(), TrezorPassphraseActivity::class.java))
    }

    companion object: KLogging()
}