package com.blockstream.compose.extensions

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.id_contact_support
import com.blockstream.common.SupportType
import com.blockstream.common.data.SupportData
import com.blockstream.common.models.GreenViewModel
import com.blockstream.common.navigation.NavigateDestinations
import com.blockstream.common.utils.getStringFromIdOrNull
import com.blockstream.compose.managers.PlatformManager
import com.blockstream.compose.sideeffects.DialogState
import org.jetbrains.compose.resources.getString

suspend fun SnackbarHostState.showErrorSnackbar(
    platformManager: PlatformManager,
    dialogState: DialogState,
    viewModel: GreenViewModel,
    error: Throwable,
    supportData: SupportData? = null
) {
    val result = showSnackbar(
        message = getStringFromIdOrNull(error.message) ?: "",
        actionLabel = if (supportData != null) getString(Res.string.id_contact_support) else null,
        duration = if (supportData != null) SnackbarDuration.Long else SnackbarDuration.Short
    )

    if (result == SnackbarResult.ActionPerformed && supportData != null) {
        viewModel.postEvent(
            NavigateDestinations.Support(
                type = SupportType.INCIDENT,
                supportData = supportData,
                greenWalletOrNull = viewModel.greenWalletOrNull
            )
        )
    }
}