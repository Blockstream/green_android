package com.blockstream.compose.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.blockstream.common.data.TwoFactorMethod
import com.blockstream.common.extensions.twoFactorMethodsLocalized
import com.blockstream.common.gdk.data.AuthHandlerStatus
import com.blockstream.compose.R
import com.blockstream.compose.components.GreenButton
import com.blockstream.compose.components.GreenButtonType
import com.blockstream.compose.components.GreenColumn
import com.blockstream.compose.extensions.colorText
import com.blockstream.compose.extensions.getIcon
import com.blockstream.compose.theme.GreenThemePreview
import com.blockstream.compose.theme.bodyMedium
import com.blockstream.compose.theme.titleMedium
import com.blockstream.compose.theme.whiteMedium
import com.blockstream.compose.utils.stringResourceId
import com.blockstream.compose.views.PinView


@Composable
fun TwoFactorCodeDialog(
    authHandlerStatus: AuthHandlerStatus,
    onDismissRequest: (code: String?, helpClicked: Boolean?) -> Unit
) {
    Dialog(
        onDismissRequest = {
            onDismissRequest(null, null)
        },
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Card(
            modifier = Modifier
                .widthIn(max = 400.dp)
                .padding(24.dp)
        ) {
            GreenColumn(padding = 24, space = 16) {
                val method by remember {
                    mutableStateOf(
                        authHandlerStatus.method?.let {
                            TwoFactorMethod.from(it)
                        }
                    )
                }

                GreenColumn(padding = 0, space = 8) {
                    method?.also {
                        Icon(
                            painter = painterResource(it.getIcon()),
                            contentDescription = null,
                            modifier = Modifier
                                .align(Alignment.CenterHorizontally)
                                .size(50.dp)
                        )
                    }

                    authHandlerStatus.method?.twoFactorMethodsLocalized()
                        ?.let { stringResourceId(it) }?.let {
                            colorText(
                                stringResourceId("id_please_provide_your_1s_code|${it}"),
                                listOf(it)
                            )
                        }?.also {
                            Text(
                                text = it,
                                style = titleMedium,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                    authHandlerStatus.attemptsRemaining?.also {
                        Text(
                            text = stringResource(id = R.string.id_attempts_remaining_d, it),
                            color = whiteMedium,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center,
                            style = bodyMedium
                        )
                    }
                }

                PinView(
                    modifier = Modifier,
                    showDigits = true,
                    withShuffle = false,
                    isSmall = true,
                    isVerifyMode = false,
                    onPin = {
                        if (it.length == 6) {
                            onDismissRequest(it, null)
                        }
                    }
                )


                val isSms = authHandlerStatus.isSms()
                Row(
                    horizontalArrangement = if (isSms) Arrangement.SpaceBetween else Arrangement.End,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isSms) {
                        GreenButton(
                            text = stringResource(id = R.string.id_help),
                            type = GreenButtonType.TEXT
                        ) {
                            onDismissRequest(null, true)
                        }
                    }

                    GreenButton(
                        text = stringResource(id = R.string.id_cancel),
                        type = GreenButtonType.TEXT
                    ) {
                        onDismissRequest(null, null)
                    }
                }
            }
        }
    }
}

@Composable
@Preview
fun TwoFactorCodeDialoggPreview() {
    GreenThemePreview {
        TwoFactorCodeDialog(
            AuthHandlerStatus(action = "tes", method = "sms", status = "", attemptsRemaining = 2)
        ) { _, _ ->

        }
    }
}