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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.window.Dialog
import com.blockstream.compose.R
import com.blockstream.compose.components.GreenButton
import com.blockstream.compose.components.GreenButtonType
import com.blockstream.compose.components.GreenCard
import com.blockstream.compose.components.GreenColumn
import com.blockstream.compose.theme.GreenThemePreview
import com.blockstream.compose.theme.bodyLarge
import com.blockstream.compose.theme.titleSmall
import com.blockstream.compose.utils.OpenKeyboard
import com.blockstream.compose.utils.getClipboard
import com.blockstream.compose.utils.stringResourceId


@Composable
fun TextDialog(
    title: String,
    message: String? = null,
    initialText: String? = null,
    placeholder: String? = null,
    label: String,
    supportingText: String? = null,
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
                    text = stringResourceId(id = title),
                    style = titleSmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                if (!message.isNullOrBlank()) {
                    Text(
                        text = stringResourceId(id = message),
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

                val context = LocalContext.current
                var passwordVisibility: Boolean by remember { mutableStateOf(!isPassword) }
                val focusManager = LocalFocusManager.current
                OutlinedTextField(
                    value = textFieldValueState,
                    onValueChange = {
                        textFieldValueState = it
                    },
                    visualTransformation = if (passwordVisibility) VisualTransformation.None else PasswordVisualTransformation(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester),
                    singleLine = true,
                    keyboardOptions = keyboardOptions ?: KeyboardOptions.Default.let {
                        if (isPassword) it.copy(
                            autoCorrect = false,
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
                        //.takeIf { placeholder != null },
//                    placeholder = placeholder?.also { Text(it) },
                    label = { Text(label) },
                    supportingText = {
                        Text(text = supportingText ?: "")
                    },
                    trailingIcon = {
                        if (isPassword) {
                            IconButton(onClick = {
                                passwordVisibility = !passwordVisibility
                            }) {
                                Icon(
                                    painter = painterResource(id = if (passwordVisibility) R.drawable.eye_slash else R.drawable.eye),
                                    contentDescription = "password visibility",
                                )
                            }
                        } else {
                            if (textFieldValueState.text.isEmpty()) {
                                Icon(
                                    painterResource(id = R.drawable.clipboard),
                                    contentDescription = "clear text",
                                    modifier = Modifier
                                        .clickable {
                                            textFieldValueState = textFieldValueState.copy(
                                                text = getClipboard(context) ?: ""
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
                        text = stringResource(id = R.string.id_cancel),
                        type = GreenButtonType.TEXT
                    ) {
                        onDismissRequest(null)
                    }

                    GreenButton(
                        text = stringResource(id = android.R.string.ok),
                        type = GreenButtonType.TEXT,
                    ) {
                        onDismissRequest(textFieldValueState.text)
                    }
                }
            }
        }
    }
}

@Composable
@Preview
fun TextDialogPreview() {
    GreenThemePreview {
        TextDialog(
            title = "Title",
            message = "Message",
            label = "Label",
            supportingText = "Supporting Text"
        ) {

        }
    }
}