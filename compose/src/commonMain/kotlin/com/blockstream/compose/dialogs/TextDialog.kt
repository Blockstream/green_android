package com.blockstream.compose.dialogs

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.window.Dialog
import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.clipboard
import blockstream_green.common.generated.resources.eye
import blockstream_green.common.generated.resources.eye_slash
import blockstream_green.common.generated.resources.id_cancel
import blockstream_green.common.generated.resources.id_ok
import com.blockstream.common.utils.DecimalFormat
import com.blockstream.compose.components.GreenButton
import com.blockstream.compose.components.GreenButtonType
import com.blockstream.compose.components.GreenCard
import com.blockstream.compose.components.GreenColumn
import com.blockstream.compose.managers.LocalPlatformManager
import com.blockstream.compose.theme.bodyLarge
import com.blockstream.compose.theme.titleSmall
import com.blockstream.compose.utils.DecimalFormatter
import com.blockstream.compose.utils.OpenKeyboard
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource


@Composable
fun TextDialog(
    title: String,
    message: String? = null,
    initialText: String? = null,
    placeholder: String? = null,
    label: String,
    supportingText: String? = null,
    suffixText: String? = null,
    isPassword: Boolean = false,
    keyboardOptions: KeyboardOptions? = null,
    onDismissRequest: (text: String?) -> Unit
) {
    Dialog(
        onDismissRequest = {
            onDismissRequest(null)
        }
    ) {
        GreenCard(modifier = Modifier.fillMaxWidth()) {
            GreenColumn(padding = 8, space = 16) {
                Text(
                    text = title,
                    style = titleSmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                if (!message.isNullOrBlank()) {
                    Text(
                        text = message,
                        style = bodyLarge,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                val focusRequester = remember { FocusRequester() }
                OpenKeyboard(focusRequester)

                var textFieldValueState by remember {
                    mutableStateOf(
                        TextFieldValue(
                            text = initialText ?: ""
                        )
                    )
                }

                val formatter = remember {
                    DecimalFormatter(
                        decimalSeparator = DecimalFormat.DecimalSeparator.first(),
                        groupingSeparator = DecimalFormat.GroupingSeparator.first()
                    )
                }

                val platformManager = LocalPlatformManager.current
                var passwordVisibility: Boolean by remember { mutableStateOf(!isPassword) }
                val focusManager = LocalFocusManager.current
                OutlinedTextField(
                    value = textFieldValueState,
                    onValueChange = {
                        textFieldValueState = if(keyboardOptions?.keyboardType == KeyboardType.Decimal){
                            it.copy(text = formatter.cleanup(it.text))
                        }else{
                            it
                        }
                    },
                    visualTransformation = if (passwordVisibility) VisualTransformation.None else PasswordVisualTransformation(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester),
                    singleLine = true,
                    keyboardOptions = keyboardOptions ?: KeyboardOptions.Default.let {
                        if (isPassword) it.copy(
                            autoCorrectEnabled = false,
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done
                        ) else it
                    },
                    keyboardActions = KeyboardActions(
                        onDone = {
                            focusManager.clearFocus()
                        }
                    ),
                    placeholder = {
                         Text(placeholder ?: "")
                    },
                    label = { Text(label) },
                    suffix = {
                        suffixText?.also {
                            Text(it)
                        }
                    },
                    supportingText = {
                        Text(text = supportingText ?: "")
                    },
                    trailingIcon = {
                        if (isPassword) {
                            IconButton(onClick = {
                                passwordVisibility = !passwordVisibility
                            }) {
                                Icon(
                                    painter = painterResource(if (passwordVisibility) Res.drawable.eye_slash else Res.drawable.eye),
                                    contentDescription = "password visibility",
                                )
                            }
                        } else {
                            if (textFieldValueState.text.isEmpty()) {
                                Icon(
                                    painterResource(Res.drawable.clipboard),
                                    contentDescription = "clear text",
                                    modifier = Modifier
                                        .clickable {
                                            textFieldValueState = textFieldValueState.copy(
                                                text = platformManager.getClipboard() ?: ""
                                            )
                                        }
                                )
                            } else {
                                Icon(
                                    Icons.Default.Clear,
                                    contentDescription = "clear text",
                                    modifier = Modifier
                                        .clickable {
                                            textFieldValueState =
                                                textFieldValueState.copy(text = "")
                                        }
                                )
                            }
                        }
                    }
                )


                Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                    GreenButton(
                        text = stringResource(Res.string.id_cancel),
                        type = GreenButtonType.TEXT
                    ) {
                        onDismissRequest(null)
                    }

                    GreenButton(
                        text = stringResource(Res.string.id_ok),
                        type = GreenButtonType.TEXT,
                    ) {
                        onDismissRequest(textFieldValueState.text)
                    }
                }
            }
        }
    }
}