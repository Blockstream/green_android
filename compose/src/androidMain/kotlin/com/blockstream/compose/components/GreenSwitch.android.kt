package com.blockstream.compose.components

import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import com.blockstream.compose.R
import com.blockstream.compose.theme.GreenTheme

@Composable
@Preview
fun GreenSwitchPreview() {
    GreenTheme {
        GreenColumn(padding = 0, space = 0) {
            var checked by remember { mutableStateOf(false) }
            GreenSwitch(
                title = "Title $checked is very large title with all the info athanks sdfa asdf",
                caption = "Caption Caption afad asd asdf asdf asdf asd ads fads asf asd fasd asdf as adf",
                checked = checked,
                painter = painterResource(id = R.drawable.globe),
                onCheckedChange = {
                    checked = it
                }
            )

            HorizontalDivider()

            var checked2 by remember { mutableStateOf(true) }
            GreenSwitch(
                title = "Enhanced Privacy $checked2",
                caption = "Use secure display and Screen Lock",
                checked = checked2,
                painter = painterResource(id = R.drawable.globe),
                onCheckedChange = {
                    checked2 = it
                }
            )
        }
    }
}