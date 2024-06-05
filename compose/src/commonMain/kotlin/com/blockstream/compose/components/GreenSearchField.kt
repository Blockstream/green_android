package com.blockstream.compose.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.clipboard
import blockstream_green.common.generated.resources.id_search
import blockstream_green.common.generated.resources.magnifying_glass
import blockstream_green.common.generated.resources.x_circle
import com.blockstream.compose.managers.LocalPlatformManager
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun GreenSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val platformManager = LocalPlatformManager.current
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = {
            Text(stringResource(Res.string.id_search))
        },
        singleLine = true,
        modifier = Modifier
            .fillMaxWidth()
            .then(modifier),
        leadingIcon = {
            Icon(
                painter = painterResource(Res.drawable.magnifying_glass),
                contentDescription = "Search"
            )
        },
        trailingIcon = {
            if (value.isEmpty()) {
                IconButton(
                    onClick = { onValueChange(platformManager.getClipboard() ?: "") },
                    enabled = enabled
                ) {
                    Icon(
                        painter = painterResource(Res.drawable.clipboard),
                        contentDescription = "Edit"
                    )
                }
            } else {
                IconButton(onClick = { onValueChange("") }, enabled = enabled) {
                    Icon(
                        painter = painterResource(Res.drawable.x_circle),
                        contentDescription = "Clear"
                    )
                }
            }
        }
    )

}