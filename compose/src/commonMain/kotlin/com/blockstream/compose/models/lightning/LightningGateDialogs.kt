// Maps the Lightning capacity gate failures to the dialogs the user sees instead of a generic error.
package com.blockstream.compose.models.lightning

import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.id_couldnt_enable_lightning
import blockstream_green.common.generated.resources.id_lightning_is_at_capacity
import blockstream_green.common.generated.resources.id_we_are_rolling_out_lightning_access
import blockstream_green.common.generated.resources.id_we_are_unable_to_enable_lightning
import com.blockstream.compose.sideeffects.SideEffects
import com.blockstream.compose.utils.StringHolder
import com.blockstream.domain.lightning.LightningAtCapacityException
import com.blockstream.domain.lightning.LightningAvailabilityUnknownException
import com.blockstream.domain.lightning.LightningGateException

fun Throwable.lightningGateDialog(): SideEffects.Dialog? = (this as? LightningGateException)?.let {
    when (it) {
        is LightningAtCapacityException -> SideEffects.Dialog(
            title = StringHolder.create(Res.string.id_lightning_is_at_capacity),
            message = StringHolder.create(Res.string.id_we_are_rolling_out_lightning_access)
        )

        is LightningAvailabilityUnknownException -> SideEffects.Dialog(
            title = StringHolder.create(Res.string.id_couldnt_enable_lightning),
            message = StringHolder.create(Res.string.id_we_are_unable_to_enable_lightning)
        )
    }
}
