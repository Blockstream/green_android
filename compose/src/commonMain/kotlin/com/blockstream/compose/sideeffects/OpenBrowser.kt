package com.blockstream.compose.sideeffects

import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.id_cancel
import blockstream_green.common.generated.resources.id_open
import blockstream_green.common.generated.resources.id_tor
import blockstream_green.common.generated.resources.id_you_have_tor_enabled_are_you
import com.blockstream.common.utils.StringHolder
import com.blockstream.compose.managers.PlatformManager
import org.jetbrains.compose.resources.getString

suspend fun openBrowser(
    platformManager: PlatformManager,
    dialogState: DialogState,
    isTor: Boolean,
    url: String,
    type: OpenBrowserType = OpenBrowserType.IN_APP
) {
    if (isTor) {
        dialogState.openDialog(
            OpenDialogData(
                title = StringHolder.create(Res.string.id_tor),
                message = StringHolder.create(Res.string.id_you_have_tor_enabled_are_you),
                primaryText = getString(Res.string.id_open),
                onPrimary = {
                    platformManager.openBrowser(url = url, type = type)
                },
                secondaryText = getString(Res.string.id_cancel),
            )
        )

    } else {
        platformManager.openBrowser(url = url, type = type)
    }
}