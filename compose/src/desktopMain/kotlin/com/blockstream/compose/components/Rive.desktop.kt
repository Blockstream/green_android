package com.blockstream.compose.components

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalInspectionMode
import blockstream_green.common.generated.resources.Res
import com.blockstream.compose.LocalPreview
import dev.muazkadan.rivecmp.CustomRiveAnimation
import dev.muazkadan.rivecmp.RiveCompositionSpec
import dev.muazkadan.rivecmp.RiveDesktop
import dev.muazkadan.rivecmp.rememberRiveComposition
import dev.muazkadan.rivecmp.utils.ExperimentalRiveCmpApi

// rive-cmp ships its desktop runtime for macOS arm64 only; elsewhere loading it fails
private val isRiveSupported: Boolean by lazy {
    runCatching { RiveDesktop.init() }.isSuccess
}

@OptIn(ExperimentalRiveCmpApi::class)
@Composable
actual fun Rive(riveAnimation: RiveAnimation, previewContent: @Composable (() -> Unit)?) {
    val path = riveAnimation.filePath

    val isPreview = LocalInspectionMode.current || LocalPreview.current

    if (isPreview || !isRiveSupported) {
        previewContent?.invoke() ?: Text("Rive")
    } else {
        val composition by rememberRiveComposition(path) {
            RiveCompositionSpec.byteArray(Res.readBytes(path))
        }

        CustomRiveAnimation(composition = composition)
    }
}
