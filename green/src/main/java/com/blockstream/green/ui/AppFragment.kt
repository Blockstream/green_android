package com.blockstream.green.ui


import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.*
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
import com.blockstream.DeviceBrand
import com.blockstream.gdk.data.Device
import com.blockstream.green.R
import com.blockstream.green.database.Wallet
import com.blockstream.green.gdk.SessionManager
import com.blockstream.green.gdk.getIcon
import com.blockstream.green.settings.SettingsManager
import com.blockstream.green.utils.*
import com.google.android.material.snackbar.Snackbar
import com.greenaddress.greenapi.HWWallet
import com.greenaddress.greenapi.HWWalletBridge
import com.greenaddress.greenbits.ui.TabbedMainActivity
import io.reactivex.Single
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
    open val isAdjustResize = false

    internal lateinit var binding: T

    @Inject
    internal lateinit var sessionManager: SessionManager

    @Inject
    lateinit var settingsManager: SettingsManager

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DataBindingUtil.inflate(layoutInflater, layout, container, false)
        binding.lifecycleOwner = this

        if (menuRes > 0) {
            setHasOptionsMenu(true)
        }

        return binding.root
    }

    override fun onResume() {
        super.onResume()
        requireActivity().window.setSoftInputMode(if (isAdjustResize) WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE else WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN)
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

    fun openOverview(){
        val intent = Intent(requireContext(), TabbedMainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
    }

    internal open fun popBackStack(){
        findNavController().popBackStack()
    }

    internal fun setupDeviceInteractionEvent(onDeviceInteractionEvent: MutableLiveData<ConsumableEvent<Device>>) {
        onDeviceInteractionEvent.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandledOrReturnNull()?.let {
                // KISS: It's not what v3 had done in the past, but it's simpler
                snackbar(R.string.id_please_follow_the_instructions, Snackbar.LENGTH_LONG)
            }
        }
    }

    companion object: KLogging()
}