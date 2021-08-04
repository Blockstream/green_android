package com.blockstream.green.ui

import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import com.blockstream.green.R
import com.blockstream.green.utils.isDevelopmentFlavor
import com.blockstream.green.utils.notifyDevelopmentFeature

abstract class AppActivity : AppCompatActivity() {

    abstract fun isDrawerOpen(): Boolean
    abstract fun closeDrawer()
    abstract fun lockDrawer(isLocked: Boolean)
    abstract fun setToolbar(
        title: String?,
        subtitle: String? = null,
        drawable: Drawable? = null,
        button: CharSequence? = null,
        buttonListener: View.OnClickListener? = null
    )

    abstract fun setToolbarVisibility(isVisible: Boolean)

    internal lateinit var navController: NavController

    private var isWindowSecure: Boolean = false

    internal fun setupSecureScreenListener() {
        navController.addOnDestinationChangedListener { _, destination, _ ->
            if (destination.id == R.id.recoveryIntroFragment ||
                destination.id == R.id.recoveryCheckFragment ||
                destination.id == R.id.recoveryWordsFragment ||
                destination.id == R.id.recoveryPhraseFragment
            ) {
                setSecureScreen(true)
            } else {
                setSecureScreen(false)
            }
        }
    }

    private fun setSecureScreen(isSecure: Boolean) {
        if (isSecure == isWindowSecure) return

        isWindowSecure = isSecure

        // In development flavor allow screen capturing
        if (isDevelopmentFlavor()) {
            notifyDevelopmentFeature("FLAG_SECURE = $isSecure")
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