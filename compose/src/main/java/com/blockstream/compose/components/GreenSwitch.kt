package com.blockstream.compose.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.blockstream.compose.R
import com.blockstream.compose.theme.GreenTheme
import com.blockstream.compose.theme.bodyMedium
import com.blockstream.compose.theme.labelLarge

@Composable
fun GreenSwitch(
    modifier: Modifier = Modifier,
    title: String,
    caption: String? = null,
    checked: Boolean,
    painter: Painter,
    onCheckedChange: ((Boolean) -> Unit) = {},
) {
    GreenRow(
        space = 16,
        padding = 0,
        modifier = Modifier
            .clickable {
                onCheckedChange.invoke(!checked)
            }
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .then(modifier),
        verticalAlignment = Alignment.CenterVertically

    ) {
        Icon(
            painter = painter,
            contentDescription = null,
        )

        Column(
            modifier = Modifier
                .weight(1f)
        ) {
            Text(
                text = title,
                style = labelLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth()
            )
            if(caption != null) {
                Text(
                    text = caption,
                    style = bodyMedium,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}


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
                })

            HorizontalDivider()

            var checked2 by remember { mutableStateOf(true) }
            GreenSwitch(
                title = "Enhanced Privacy $checked2",
                caption = "Use secure display and Screen Lock",
                checked = checked2,
                painter = painterResource(id = R.drawable.globe),
                onCheckedChange = {
                    checked2 = it
                })
//            GreenSwitch()
//            GreenSwitch()
        }
    }
}