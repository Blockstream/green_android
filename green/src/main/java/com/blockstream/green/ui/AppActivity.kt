package com.blockstream.green.ui

import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import com.blockstream.common.managers.SettingsManager
import com.blockstream.green.R
import com.blockstream.green.devices.DeviceManagerAndroid
import com.blockstream.green.utils.isDevelopmentFlavor
import com.blockstream.green.views.GreenToolbar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.koin.android.ext.android.inject

abstract class AppActivity : AppCompatActivity() {

    protected val settingsManager: SettingsManager by inject()

    private val deviceManager: DeviceManagerAndroid by inject()

    abstract fun isDrawerOpen(): Boolean
    abstract fun closeDrawer()
    abstract fun lockDrawer(isLocked: Boolean)

    abstract fun setToolbarVisibility(isVisible: Boolean)

    abstract val toolbar: GreenToolbar

    internal lateinit var navController: NavController

    private var isWindowSecure: Boolean = false

    private val secureFragments = listOf(
        R.id.recoveryIntroFragment,
        R.id.recoveryCheckFragment,
        R.id.recoveryWordsFragment,
        R.id.recoveryPhraseFragment,
        R.id.loginFragment,
        R.id.pinFragment,
        R.id.changePinFragment
    )

    internal fun setupSecureScreenListener() {
        navController.addOnDestinationChangedListener { _, destination, _ ->
            // If enhancedPrivacy is turned off, secure only specific screens
            if(!settingsManager.getApplicationSettings().enhancedPrivacy) {
                if(secureFragments.contains(destination.id)) {
                    setSecureScreen(true)
                } else {
                    setSecureScreen(false)
                }
            }

            if(destination.id == R.id.deviceListFragment || destination.id == R.id.deviceScanFragment){
                deviceManager.startBluetoothScanning()
            }else{
                deviceManager.stopBluetoothScanning()
            }
        }
        settingsManager.appSettingsStateFlow.onEach {
            // Skip changing secure screen if we are on a secure fragment
            if(it.enhancedPrivacy || !secureFragments.contains(navController.currentDestination?.id)){
                setSecureScreen(it.enhancedPrivacy)
            }
        }.launchIn(CoroutineScope(context = Dispatchers.Main))
    }

    private fun setSecureScreen(isSecure: Boolean) {
        if (isSecure == isWindowSecure) return

        isWindowSecure = isSecure

        // In development flavor allow screen capturing
        if (isDevelopmentFlavor) {
            // notifyDevelopmentFeature("FLAG_SECURE = $isSecure")
            return
        }

        if (isWindowSecure) {
            window?.setFlags(
                WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE
            )
        } else {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
    }
}