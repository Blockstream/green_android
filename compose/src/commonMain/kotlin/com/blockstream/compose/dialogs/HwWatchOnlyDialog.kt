package com.blockstream.compose.dialogs

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.id_enable_biometrics
import blockstream_green.common.generated.resources.id_require_jade_connection
import blockstream_green.common.generated.resources.id_use_biometrics_for_quick_access
import blockstream_green.common.generated.resources.id_use_biometrics_to_quickly_check
import blockstream_green.common.generated.resources.id_your_keys_stay_on_jade
import com.adamglin.PhosphorIcons
import com.adamglin.phosphoricons.Regular
import com.adamglin.phosphoricons.regular.Fingerprint
import com.blockstream.compose.components.GreenButton
import com.blockstream.compose.components.GreenButtonSize
import com.blockstream.compose.components.GreenButtonType
import com.blockstream.compose.components.GreenCard
import com.blockstream.compose.components.GreenColumn
import com.blockstream.compose.navigation.NavigateDestinations
import com.blockstream.compose.navigation.setResult
import com.blockstream.compose.theme.bodyMedium
import com.blockstream.compose.theme.textMedium
import com.blockstream.compose.theme.titleLarge
import org.jetbrains.compose.resources.stringResource

@Composable
fun HwWatchOnlyDialog(
    onDismissRequest: () -> Unit,
) {
    Dialog(onDismissRequest = {
        // Empty onDismissRequest: dialog is non-dismissable, user must choose an option
    }) {
        GreenCard(modifier = Modifier.fillMaxWidth()) {
            GreenColumn(
                padding = 0, space = 16, horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(vertical = 24.dp)
            ) {
                Icon(
                    imageVector = PhosphorIcons.Regular.Fingerprint, contentDescription = null, modifier = Modifier.size(64.dp)
                )

                Text(
                    text = stringResource(Res.string.id_use_biometrics_for_quick_access),
                    style = titleLarge,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = stringResource(Res.string.id_use_biometrics_to_quickly_check),
                    style = bodyMedium,
                    color = textMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = stringResource(Res.string.id_your_keys_stay_on_jade),
                    style = bodyMedium,
                    color = textMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                GreenButton(
                    text = stringResource(Res.string.id_enable_biometrics),
                    size = GreenButtonSize.BIG,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                ) {
                    NavigateDestinations.HwWatchOnlyChoice.setResult(true)
                    onDismissRequest()
                }

                GreenButton(
                    text = stringResource(Res.string.id_require_jade_connection),
                    type = GreenButtonType.OUTLINE,
                    size = GreenButtonSize.BIG,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    NavigateDestinations.HwWatchOnlyChoice.setResult(false)
                    onDismissRequest()
                }
            }
        }
    }
}
