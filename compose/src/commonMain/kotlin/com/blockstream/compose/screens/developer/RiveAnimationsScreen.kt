// Developer screen that plays every Rive animation the app ships, each under its name and file,
// to check how they render on a device. Opened from App Settings → Developer.
package com.blockstream.compose.screens.developer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.blockstream.compose.components.Rive
import com.blockstream.compose.components.RiveAnimation
import com.blockstream.compose.components.filePath
import com.blockstream.compose.models.developer.RiveAnimationsViewModel
import com.blockstream.compose.utils.SetupScreen
import com.blockstream.designsystem.AppTheme
import com.blockstream.designsystem.components.AppText

internal val shippedRiveAnimations: List<RiveAnimation> = RiveAnimation.entries

@Composable
fun RiveAnimationsScreen(viewModel: RiveAnimationsViewModel) {
    SetupScreen(viewModel = viewModel, withPadding = false) {
        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
            items(shippedRiveAnimations, key = { it.name }) { animation ->
                Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    AppText(
                        text = "${animation.name} · ${animation.filePath.substringAfterLast('/')}",
                        style = AppTheme.typography.label.mMedium,
                        color = AppTheme.colors.text.subtle
                    )
                    Box(Modifier.fillMaxWidth().height(200.dp)) { Rive(animation) }
                }
            }
        }
    }
}
