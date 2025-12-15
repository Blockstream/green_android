package com.blockstream.compose.extensions

import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.id_lock_after_1_minute
import blockstream_green.common.generated.resources.id_lock_immediately
import com.blockstream.data.data.ScreenLockSetting
import org.jetbrains.compose.resources.StringResource

fun ScreenLockSetting.Companion.getStringList(): List<StringResource> {
    return listOf(Res.string.id_lock_immediately, Res.string.id_lock_after_1_minute)
}