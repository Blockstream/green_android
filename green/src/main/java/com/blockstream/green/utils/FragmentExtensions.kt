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
import com.blockstream.green.R
import com.blockstream.green.gdk.getGDKErrorCode
import com.blockstream.libgreenaddress.KotlinGDK
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

fun Fragment.errorFromGDK(throwable: Throwable): String {
    throwable.message?.let{
        when(it){
            "id_invalid_pgp_key" -> R.string.id_invalid_pgp_key
            "id_invalid_twofactor_code" -> R.string.id_invalid_twofactor_code
            "id_action_canceled" -> R.string.generic_error_user_canceled
            else -> null
        }?.let{ strRes ->
            return getString(strRes)
        }

    }

    return getString(if (throwable.getGDKErrorCode() == KotlinGDK.GA_ERROR) R.string.id_login_failed else R.string.id_connection_failed)
}

fun Fragment.errorDialogFromGDK(throwable: Throwable, listener: (() -> Unit)? = null) {
    throwable.printStackTrace()
    errorDialog(error = errorFromGDK(throwable), listener = listener)
}

fun Fragment.errorDialog(throwable: Throwable, listener: (() -> Unit)? = null) {
    throwable.printStackTrace()
    errorDialog(
        error = throwable.cause?.message ?: throwable.message ?: "An exception occurred",
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

fun Fragment.toast(text: String, duration: Int = Toast.LENGTH_SHORT) {
    Toast.makeText(requireContext(), text, duration).show()
}

fun Fragment.errorSnackbar(throwable: Throwable, duration: Int = Snackbar.LENGTH_SHORT) {
    throwable.printStackTrace()
    snackbar(throwable.cause?.message ?: throwable.message ?: "An exception occurred", duration)
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
