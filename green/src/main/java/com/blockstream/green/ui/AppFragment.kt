package com.blockstream.green.ui


import android.annotation.SuppressLint
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
import androidx.navigation.NavDirections
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.blockstream.green.R
import com.blockstream.green.database.Wallet
import com.blockstream.green.gdk.SessionManager
import com.blockstream.green.gdk.getIcon
import com.blockstream.green.utils.isDevelopmentFlavor
import com.blockstream.green.utils.toast
import com.greenaddress.Bridge
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
    internal lateinit var binding: T

    @Inject
    internal lateinit var sessionManager: SessionManager

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

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        if (menuRes > 0) {
            inflater.inflate(menuRes, menu)
        }
    }

    protected fun closeDrawer() {
        (requireActivity() as IActivity).closeDrawer()
    }

    protected fun isDrawerOpen() = (requireActivity() as IActivity).isDrawerOpen()


    protected fun setSecureScreen(isSecure : Boolean){
        // In development flavor allow screen capturing
        if(isSecure && requireContext().isDevelopmentFlavor()){
            toast("Development Flavor: FLAG_SECURE is disabled!")
            return
        }

        if(isSecure){
            activity?.window?.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
        }else{
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
    }

    fun setToolbar(wallet: Wallet) {
        val icon = ContextCompat.getDrawable(requireContext(), wallet.getIcon())
        setToolbar(wallet.name, subtitle = null, drawable = icon)
    }

    fun setToolbar(title: String? = null, subtitle: String? = null, drawable: Drawable? = null, button: CharSequence? = null,
                   buttonListener: View.OnClickListener? = null){
        (requireActivity() as IActivity).setToolbar(title, subtitle, drawable, button, buttonListener)
    }

    fun setToolbarVisibility(isVisible: Boolean){
        (requireActivity() as IActivity).setToolbarVisibility(isVisible)
    }

    fun navigate(directions: NavDirections) {
        navigate(directions.actionId, directions.arguments)
    }

    fun navigate(@IdRes resId: Int) {
        navigate(resId, null)
    }

    @SuppressLint("RestrictedApi")
    fun navigate(@IdRes resId: Int, args: Bundle?, isLogout: Boolean = false) {

        val navOptionsBuilder = NavOptions.Builder()

        val animate = true

        if (animate) {
            navOptionsBuilder.setEnterAnim(R.anim.nav_enter_anim)
                .setExitAnim(R.anim.nav_exit_anim)
                .setPopEnterAnim(R.anim.nav_pop_enter_anim)
                .setPopExitAnim(R.anim.nav_pop_exit_anim)
        }

        if (isLogout) {
            navOptionsBuilder.setPopUpTo(findNavController().backStack.first.destination.id, true)
            navOptionsBuilder.setLaunchSingleTop(true) // this is only needed on lateral movements
        } else if (resId == R.id.action_global_loginFragment) {
            // Allow only one Login screen
            navOptionsBuilder.setLaunchSingleTop(true)
        }else if (resId == R.id.action_global_addWalletFragment){
            // Allow a single onboarding path
            navOptionsBuilder.setPopUpTo(R.id.addWalletFragment, true)
        }


        findNavController().navigate(resId, args, navOptionsBuilder.build())
    }
}