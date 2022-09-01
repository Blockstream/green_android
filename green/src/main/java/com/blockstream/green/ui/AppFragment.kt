package com.blockstream.green.ui


import android.annotation.SuppressLint
import android.os.Bundle
import android.view.*
import androidx.annotation.IdRes
import androidx.annotation.LayoutRes
import androidx.annotation.MenuRes
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.fragment.app.Fragment
import androidx.lifecycle.MutableLiveData
import androidx.navigation.NavDirections
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.blockstream.gdk.data.Device
import com.blockstream.green.data.AppEvent
import com.blockstream.green.data.BannerView
import com.blockstream.green.data.Countly
import com.blockstream.green.data.ScreenView
import com.blockstream.green.gdk.SessionManager
import com.blockstream.green.settings.SettingsManager
import com.blockstream.green.ui.bottomsheets.PassphraseBottomSheetDialogFragment
import com.blockstream.green.ui.bottomsheets.PinMatrixBottomSheetDialogFragment
import com.blockstream.green.ui.devices.DeviceInteractionRequestBottomSheetDialogFragment
import com.blockstream.green.utils.*
import com.blockstream.green.views.GreenAlertView
import com.blockstream.green.views.GreenToolbar
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
) : Fragment(), ScreenView, BannerView {

    sealed class DeviceRequestEvent : AppEvent {
        object RequestPinMatrix: DeviceRequestEvent()
        object RequestPassphrase: DeviceRequestEvent()
    }

    open val isAdjustResize = false

    internal lateinit var binding: T

    @Inject
    internal lateinit var countly: Countly

    @Inject
    internal lateinit var sessionManager: SessionManager

    @Inject
    lateinit var settingsManager: SettingsManager

    open fun getAppViewModel(): AppViewModel? = null

    open val title : String? = null

    override var screenIsRecorded = false
    override val segmentation: HashMap<String, Any>? = null

    protected val toolbar: GreenToolbar
        get() = (requireActivity() as AppActivity).toolbar

    open fun updateToolbar() {
        title?.let {
            toolbar.title = it
        }
        toolbar.subtitle = null
        toolbar.logo = null
        toolbar.setBubble(null)
        toolbar.setButton(null)
    }

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

            // Register listener for Passphrase result
            getNavigationResult<String>(PassphraseBottomSheetDialogFragment.PASSPHRASE_RESULT)?.observe(viewLifecycleOwner) { result ->
                result?.let {
                    clearNavigationResult(PassphraseBottomSheetDialogFragment.PASSPHRASE_RESULT)
                    getAppViewModel()?.requestPinPassphraseEmitter?.onSuccess(result)
                }
            }

            // Register listener for Passphrase cancel result as empty string is a valid passphrase
            getNavigationResult<Boolean>(PassphraseBottomSheetDialogFragment.PASSPHRASE_CANCEL_RESULT)?.observe(viewLifecycleOwner) { result ->
                result?.let {
                    clearNavigationResult(PassphraseBottomSheetDialogFragment.PASSPHRASE_CANCEL_RESULT)
                    getAppViewModel()?.requestPinPassphraseEmitter?.onError(Exception("id_action_canceled"))
                }
            }

            // Register listener for Pin result
            getNavigationResult<String>(PinMatrixBottomSheetDialogFragment.PIN_RESULT)?.observe(viewLifecycleOwner) { result ->
                result?.let {
                    clearNavigationResult(PinMatrixBottomSheetDialogFragment.PIN_RESULT)
                    if(result.isBlank()){
                        getAppViewModel()?.requestPinMatrixEmitter?.onError(Exception("id_action_canceled"))
                    }else{
                        getAppViewModel()?.requestPinMatrixEmitter?.onSuccess(result)
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()

        // Prevent DrawerFragment from corrupting the main fragment
        if (this !is DrawerFragment) {
            requireActivity().window.setSoftInputMode(if (isAdjustResize) WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE else WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN)
            updateToolbar()
            countly.screenView(this)
        }

        BannersHelper.handle(this, if(this is WalletFragment<*>) this.walletOrNull else null)
    }

    @Deprecated("Deprecated in Java")
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        if (menuRes > 0) {
            inflater.inflate(menuRes, menu)
        }
    }

    protected fun closeDrawer() {
        (requireActivity() as AppActivity).closeDrawer()
    }

    protected fun isDrawerOpen() = (requireActivity() as AppActivity).isDrawerOpen()

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

    private fun requestPinMatrix() {
        PinMatrixBottomSheetDialogFragment.show(childFragmentManager)
    }

    private fun requestPassphrase() {
        PassphraseBottomSheetDialogFragment.show(childFragmentManager)
    }

    override fun getBannerAlertView(): GreenAlertView? = null

    companion object: KLogging()
}