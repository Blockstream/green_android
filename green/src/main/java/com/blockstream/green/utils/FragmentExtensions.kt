package com.blockstream.green.utils

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.Gravity
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.annotation.IdRes
import androidx.annotation.MenuRes
import androidx.appcompat.widget.PopupMenu
import androidx.biometric.BiometricPrompt
import androidx.core.app.ShareCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.blockstream.green.BuildConfig
import com.blockstream.green.R
import com.blockstream.green.gdk.isConnectionError
import com.blockstream.green.gdk.isNotAuthorized
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar

fun <T>Fragment.getNavigationResult(key: String = "result") = findNavController().currentBackStackEntry?.savedStateHandle?.getLiveData<T>(
    key
)
fun Fragment.clearNavigationResult(key: String = "result") = findNavController().currentBackStackEntry?.savedStateHandle?.set(
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

fun Fragment.hideKeyboard() {
    view?.let { context?.hideKeyboard(it) }
}

fun Context.hideKeyboard(view: View) {
    val inputMethodManager = getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
    inputMethodManager.hideSoftInputFromWindow(view.windowToken, 0)
}

fun Fragment.errorFromResourcesAndGDK(throwable: Throwable): String {
    throwable.message?.let {
        val intRes = resources.getIdentifier(it, "string", BuildConfig.APPLICATION_ID)
        if(intRes > 0){
            return getString(intRes)
        }
    }

    if(throwable.isConnectionError()){
        return getString(R.string.id_connection_failed)
    }else if(throwable.isNotAuthorized()){
        return getString(R.string.id_login_failed)
    }

    return throwable.cause?.message ?: throwable.message ?: "An error occurred"
}

fun Fragment.errorDialog(throwable: Throwable, print: Boolean = false, listener: (() -> Unit)? = null) {
    if(print) {
        throwable.printStackTrace()
    }

    errorDialog(
        error = errorFromResourcesAndGDK(throwable),
        listener = listener
    )
}

fun Fragment.errorDialog(error: String, listener: (() -> Unit)? = null) {
    MaterialAlertDialogBuilder(requireContext(), R.style.ThemeOverlay_Green_MaterialAlertDialog)
        .setTitle(R.string.id_error)
        .setMessage(error)
        .setPositiveButton(android.R.string.ok, null)
        .setOnDismissListener {
            listener?.invoke()
        }
        .show()
}

fun Fragment.dialog(title: Int, message: Int, listener: (() -> Unit)? = null) {
    dialog(getString(title), getString(message), listener)
}

fun Fragment.dialog(title: String, message: String, listener: (() -> Unit)? = null) {
    MaterialAlertDialogBuilder(requireContext(), R.style.ThemeOverlay_Green_MaterialAlertDialog)
        .setTitle(title)
        .setMessage(message)
        .setPositiveButton(android.R.string.ok, null)
        .setOnDismissListener {
            listener?.invoke()
        }
        .show()
}

fun Fragment.toast(resId: Int, duration: Int = Toast.LENGTH_SHORT) {
    Toast.makeText(requireContext(), getString(resId), duration).show()
}

fun Fragment.toast(text: String, duration: Int = Toast.LENGTH_SHORT) {
    Toast.makeText(requireContext(), text, duration).show()
}

fun Fragment.errorSnackbar(throwable: Throwable, duration: Int = Snackbar.LENGTH_SHORT, print: Boolean = false) {
    if(print) {
        throwable.printStackTrace()
    }
    snackbar(errorFromResourcesAndGDK(throwable), duration)
}

fun Fragment.snackbar(resId: Int, duration: Int = Snackbar.LENGTH_SHORT) {
    snackbar(getString(resId), duration)
}

fun Fragment.snackbar(text: String, duration: Int = Snackbar.LENGTH_SHORT) {
    view?.let{
        Snackbar.make(it, text, duration).show()
    }
}

fun Fragment.share(text: String) {
    val builder = ShareCompat.IntentBuilder.from(requireActivity())
        .setType("text/plain")
        .setText(text)

    requireActivity().startActivity(
        Intent.createChooser(
            builder.intent,
            getString(R.string.id_share)
        )
    )
}

fun Fragment.shareJPEG(uri: Uri) {
    val builder = ShareCompat.IntentBuilder.from(requireActivity())
        .setType("image/jpg")
        .setStream(uri)

    requireActivity().startActivity(
        Intent.createChooser(
            builder.intent,
            getString(R.string.id_share)
        )
    )
}

fun Fragment.showPopupMenu(
    view: View,
    @MenuRes menuRes: Int,
    listener: PopupMenu.OnMenuItemClickListener
) {
    val popup = PopupMenu(requireContext(), view, Gravity.END)
    popup.menuInflater.inflate(menuRes, popup.menu)
    popup.setOnMenuItemClickListener(listener)
    popup.show()
}

fun Fragment.handleBiometricsError(errorCode: Int, errString: CharSequence){
    if (errorCode == BiometricPrompt.ERROR_USER_CANCELED || errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON || errorCode == BiometricPrompt.ERROR_CANCELED) {
        // This is OK
    } else {
        // TODO INVALIDATE ALL BIOMETRIC LOGIN CREDENTIALS
        Toast.makeText(
            context,
            "Authentication error: $errorCode $errString",
            Toast.LENGTH_SHORT
        ).show()
    }
}
