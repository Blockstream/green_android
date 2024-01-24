package com.blockstream.green.extensions

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.annotation.IdRes
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.blockstream.green.R


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

    val currentDestinationId = navController.currentDestination?.id
    // Don't animate on overview change
    val animate =
        !(currentDestinationId == R.id.walletOverviewFragment && resId == R.id.action_global_walletOverviewFragment)
                && !(currentDestinationId == R.id.loginFragment && resId == R.id.action_global_loginFragment)
                && !(currentDestinationId == R.id.homeFragment && resId == R.id.action_global_onboardingTermsFragment)

    if (animate) {
        navOptionsBuilder.setEnterAnim(R.anim.nav_enter_anim)
            .setExitAnim(R.anim.nav_exit_anim)
            .setPopEnterAnim(R.anim.nav_pop_enter_anim)
            .setPopExitAnim(R.anim.nav_pop_exit_anim)
    }

    if (isLogout || resId == R.id.action_global_walletOverviewFragment) {
        navController.currentBackStack.value.firstOrNull()?.let {
            navOptionsBuilder.setPopUpTo(it.destination.id, true)
        }
        navOptionsBuilder.setLaunchSingleTop(true) // this is only needed on lateral movements
    } else if (resId == R.id.action_global_loginFragment) {
        // Allow only one Login screen
        navOptionsBuilder.setPopUpTo(R.id.loginFragment, true)
        navOptionsBuilder.setLaunchSingleTop(true)
    } else if (resId == R.id.action_global_deviceScanFragment) {
        // Allow only one Login screen
        navOptionsBuilder.setPopUpTo(R.id.deviceScanFragment, true)
        navOptionsBuilder.setLaunchSingleTop(true)
    } else if (resId == R.id.action_global_onboardingTermsFragment) {
        // Allow only one Login screen
        navOptionsBuilder.setPopUpTo(R.id.homeFragment, true)
        navOptionsBuilder.setLaunchSingleTop(true)
    } else if (resId == R.id.action_global_setupNewWalletFragment){
        // Allow a single onboarding path
        navOptionsBuilder.setPopUpTo(R.id.setupNewWalletFragment, true)
    }

    try{
        // Simple fix for https://issuetracker.google.com/issues/118975714
        navController.navigate(resId, args, navOptionsBuilder.build())
    }catch (e: Exception){
        e.printStackTrace()
    }
}