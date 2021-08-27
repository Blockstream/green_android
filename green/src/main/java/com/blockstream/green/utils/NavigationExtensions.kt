package com.blockstream.green.utils

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.annotation.IdRes
import androidx.annotation.MenuRes
import androidx.appcompat.widget.PopupMenu
import androidx.biometric.BiometricPrompt
import androidx.core.app.ShareCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.blockstream.green.BuildConfig
import com.blockstream.green.R
import com.blockstream.green.gdk.isConnectionError
import com.blockstream.green.gdk.isNotAuthorized
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import java.util.*


fun <T> Fragment.getNavigationResult(key: String = "result") =
    findNavController().currentBackStackEntry?.savedStateHandle?.getLiveData<T>(
        key
    )

fun Fragment.clearNavigationResult(key: String = "result") =
    findNavController().currentBackStackEntry?.savedStateHandle?.set(
        key,
        null
    )

fun <T> Fragment.setNavigationResult(
    result: T,
    key: String = "result",
    @IdRes destinationId: Int? = null
) {
    findNavController().apply {
        (if (destinationId != null) getBackStackEntry(destinationId) else previousBackStackEntry)
            ?.savedStateHandle
            ?.set(key, result)
    }
}

@SuppressLint("RestrictedApi")
fun navigate(navController: NavController, @IdRes resId: Int, args: Bundle?, isLogout: Boolean = false, optionsBuilder: NavOptions.Builder? = null) {
    val navOptionsBuilder = optionsBuilder ?: NavOptions.Builder()

    // Don't animate on overview change
    val animate = !(navController.currentDestination?.id == R.id.overviewFragment && resId == R.id.action_global_overviewFragment)

    if (animate) {
        navOptionsBuilder.setEnterAnim(R.anim.nav_enter_anim)
            .setExitAnim(R.anim.nav_exit_anim)
            .setPopEnterAnim(R.anim.nav_pop_enter_anim)
            .setPopExitAnim(R.anim.nav_pop_exit_anim)
    }

    if (isLogout || resId == R.id.action_global_overviewFragment) {
        if(navController.backStack.isNotEmpty()) {
            navOptionsBuilder.setPopUpTo(navController.backStack.first.destination.id, true)
        }
        navOptionsBuilder.setLaunchSingleTop(true) // this is only needed on lateral movements
    } else if (resId == R.id.action_global_loginFragment) {
        // Allow only one Login screen
        navOptionsBuilder.setLaunchSingleTop(true)
    }else if (resId == R.id.action_global_addWalletFragment){
        // Allow a single onboarding path
        navOptionsBuilder.setPopUpTo(R.id.addWalletFragment, true)
    }

    try{
        // Simple fix for https://issuetracker.google.com/issues/118975714
        navController.navigate(resId, args, navOptionsBuilder.build())
    }catch (e: Exception){
        e.printStackTrace()
    }
}