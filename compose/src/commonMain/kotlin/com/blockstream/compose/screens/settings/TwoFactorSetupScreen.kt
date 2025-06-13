package com.blockstream.compose.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.caret_down
import blockstream_green.common.generated.resources.id_by_continuing_you_agree_to
import blockstream_green.common.generated.resources.id_country
import blockstream_green.common.generated.resources.id_email
import blockstream_green.common.generated.resources.id_for_help_visit
import blockstream_green.common.generated.resources.id_helpblockstreamcom
import blockstream_green.common.generated.resources.id_message_frequency_varies
import blockstream_green.common.generated.resources.id_phone_number
import blockstream_green.common.generated.resources.id_privacy_policy
import blockstream_green.common.generated.resources.id_scan_the_qr_code_with_an
import blockstream_green.common.generated.resources.id_terms_of_service
import blockstream_green.common.generated.resources.id_the_recovery_key_below_will_not
import com.adamglin.PhosphorIcons
import com.adamglin.phosphoricons.Regular
import com.adamglin.phosphoricons.regular.Copy
import com.blockstream.common.data.TwoFactorMethod
import com.blockstream.common.data.TwoFactorSetupAction
import com.blockstream.common.events.Events
import com.blockstream.common.models.settings.TwoFactorSetupViewModel
import com.blockstream.common.models.settings.TwoFactorSetupViewModelAbstract
import com.blockstream.common.navigation.NavigateDestinations
import com.blockstream.compose.components.GreenButton
import com.blockstream.compose.components.GreenQR
import com.blockstream.compose.extensions.colorText
import com.blockstream.compose.extensions.onValueChange
import com.blockstream.compose.theme.bodyLarge
import com.blockstream.compose.theme.bodyMedium
import com.blockstream.compose.theme.labelMedium
import com.blockstream.compose.theme.titleSmall
import com.blockstream.compose.theme.whiteHigh
import com.blockstream.compose.theme.whiteMedium
import com.blockstream.compose.utils.CopyContainer
import com.blockstream.compose.utils.SetupScreen
import com.blockstream.green.data.countries.Country
import com.blockstream.ui.components.GreenColumn
import com.blockstream.ui.components.GreenRow
import com.blockstream.ui.navigation.getResult
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun TwoFactorSetupScreen(
    viewModel: TwoFactorSetupViewModelAbstract,
) {
    val focusRequester = remember { FocusRequester() }

    NavigateDestinations.Countries.getResult<Country> {
        viewModel.country.value = it.dialCodeString
        focusRequester.requestFocus()
    }

    val method = viewModel.method

    SetupScreen(viewModel = viewModel) {

        GreenColumn(verticalArrangement = Arrangement.SpaceBetween) {
            GreenColumn(
                padding = 0,
                space = 24,
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {

                val messageText by viewModel.messageText.collectAsStateWithLifecycle()
                val actionText by viewModel.actionText.collectAsStateWithLifecycle()

                messageText?.also {
                    Text(
                        text = it,
                        style = bodyLarge,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (viewModel.action != TwoFactorSetupAction.CANCEL) {
                    if (method == TwoFactorMethod.EMAIL) {

                        val email by viewModel.email.collectAsStateWithLifecycle()

                        OutlinedTextField(
                            label = { Text(stringResource(Res.string.id_email)) },
                            value = email,
                            onValueChange = viewModel.email.onValueChange(),
                            keyboardOptions = KeyboardOptions.Default.copy(
                                keyboardType = KeyboardType.Email,
                                imeAction = ImeAction.Done
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                    } else if (method == TwoFactorMethod.SMS || method == TwoFactorMethod.PHONE || method == TwoFactorMethod.TELEGRAM) {

                        val country by viewModel.country.collectAsStateWithLifecycle()
                        val number by viewModel.number.collectAsStateWithLifecycle()

                        GreenRow(padding = 0, space = 8) {
                            OutlinedTextField(
                                label = { Text(stringResource(Res.string.id_country)) },
                                value = country,
                                onValueChange = {

                                },
                                readOnly = true,
                                trailingIcon = {
                                    Icon(
                                        painter = painterResource(Res.drawable.caret_down),
                                        contentDescription = null
                                    )
                                },
                                modifier = Modifier
                                    .width(140.dp).onFocusChanged {
                                        if (it.isFocused) {
                                            viewModel.postEvent(NavigateDestinations.Countries(greenWallet = viewModel.greenWallet))
                                        }
                                    }
                            )

                            OutlinedTextField(
                                label = { Text(stringResource(Res.string.id_phone_number)) },
                                value = number,
                                onValueChange = viewModel.number.onValueChange(),
                                keyboardOptions = KeyboardOptions.Default.copy(
                                    keyboardType = KeyboardType.Phone,
                                    imeAction = ImeAction.Done
                                ),
                                modifier = Modifier.weight(1f).focusRequester(focusRequester)
                            )
                        }

                    } else if (method == TwoFactorMethod.AUTHENTICATOR) {

                        Text(
                            stringResource(Res.string.id_scan_the_qr_code_with_an),
                            color = whiteHigh,
                            style = titleSmall,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )

                        val qr by viewModel.qr.collectAsStateWithLifecycle()
                        qr?.also {
                            GreenQR(data = it)
                        }

                        Text(
                            stringResource(Res.string.id_the_recovery_key_below_will_not),
                            color = whiteHigh,
                            style = labelMedium,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )

                        val authenticatorCode by viewModel.authenticatorCode.collectAsStateWithLifecycle()
                        authenticatorCode?.also {
                            CopyContainer(value = it, withSelection = true) {
                                GreenRow(
                                    padding = 0,
                                    space = 8,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(it)
                                    Icon(
                                        imageVector = PhosphorIcons.Regular.Copy,
                                        contentDescription = "Copy"
                                    )
                                }
                            }
                        }
                    }

                    actionText?.also {
                        val buttonEnabled by viewModel.buttonEnabled.collectAsStateWithLifecycle()

                        GreenButton(
                            text = it,
                            enabled = buttonEnabled,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            viewModel.postEvent(Events.Continue)
                        }
                    }
                }
            }

            if (method == TwoFactorMethod.SMS) {
                GreenColumn(
                    padding = 0,
                    space = 16,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {

                    val annotatedText1 = colorText(
                        text = stringResource(Res.string.id_by_continuing_you_agree_to),
                        coloredTexts = listOf(
                            stringResource(Res.string.id_terms_of_service),
                            stringResource(Res.string.id_privacy_policy)
                        ),
                        baseColor = whiteMedium,
                    )

                    ClickableText(
                        text = annotatedText1,
                        style = bodyMedium.copy(textAlign = TextAlign.Center),
                        onClick = { offset ->
                            annotatedText1.getStringAnnotations(
                                tag = "Index", start = offset, end = offset
                            ).firstOrNull()?.item?.toIntOrNull()?.let { index ->
                                if (index == 0) {
                                    viewModel.postEvent(TwoFactorSetupViewModel.LocalEvents.ClickTermsOfService())
                                } else {
                                    viewModel.postEvent(TwoFactorSetupViewModel.LocalEvents.ClickPrivacyPolicy())
                                }
                            }
                        }
                    )

                    Text(
                        text = stringResource(Res.string.id_message_frequency_varies),
                        style = bodyMedium,
                        color = whiteMedium,
                        textAlign = TextAlign.Center
                    )

                    val annotatedText2 = colorText(
                        text = stringResource(Res.string.id_for_help_visit),
                        coloredTexts = listOf(
                            stringResource(Res.string.id_helpblockstreamcom),
                        ),
                        baseColor = whiteMedium,
                    )

                    ClickableText(
                        text = annotatedText2,
                        style = bodyMedium.copy(textAlign = TextAlign.Center),
                        onClick = { offset ->
                            annotatedText2.getStringAnnotations(
                                tag = "Index", start = offset, end = offset
                            ).firstOrNull()?.let { index ->
                                viewModel.postEvent(TwoFactorSetupViewModel.LocalEvents.ClickHelp())
                            }
                        }
                    )
                }
            }
        }
    }
}
