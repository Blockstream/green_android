package com.blockstream.designsystem.catalog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.adamglin.PhosphorIcons
import com.adamglin.phosphoricons.Regular
import com.adamglin.phosphoricons.regular.Clipboard
import com.adamglin.phosphoricons.regular.Eye
import com.adamglin.phosphoricons.regular.EyeSlash
import com.blockstream.designsystem.AppTheme
import com.blockstream.designsystem.components.AppIcon
import com.blockstream.designsystem.components.AppText
import com.blockstream.designsystem.components.AppTextField
import com.blockstream.designsystem.components.AppTextFieldDefaults
import com.blockstream.designsystem.components.AppTextFieldMessage
import com.blockstream.designsystem.components.AppTextFieldMessageType
import com.blockstream.designsystem.components.HexagonIcon

@Composable
fun TextFieldCatalog() {
    var plain by rememberSaveable { mutableStateOf("") }
    var filled by rememberSaveable { mutableStateOf("Filled") }
    var error by rememberSaveable { mutableStateOf("Filled") }
    var warning by rememberSaveable { mutableStateOf("0.05 BTC") }
    var info by rememberSaveable { mutableStateOf("0.01 BTC") }
    var labelled by rememberSaveable { mutableStateOf("") }
    var withLeading by rememberSaveable { mutableStateOf("") }
    var withBoth by rememberSaveable { mutableStateOf("") }
    var withAffixes by rememberSaveable { mutableStateOf("0.001") }
    var password by rememberSaveable { mutableStateOf("hunter2") }
    var passwordVisible by rememberSaveable { mutableStateOf(false) }
    var multiline by rememberSaveable { mutableStateOf("") }
    var note by rememberSaveable { mutableStateOf("") }

    Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            CatalogLabel("Default")
            AppTextField(value = plain, onValueChange = { plain = it }, placeholder = "Placeholder")
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            CatalogLabel("Filled, clear button")
            AppTextField(value = filled, onValueChange = { filled = it }, placeholder = "Placeholder")
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            CatalogLabel("Error")
            AppTextField(
                value = error,
                onValueChange = { error = it },
                placeholder = "Placeholder",
                message = AppTextFieldMessage(
                    AppTextFieldMessageType.Error,
                    "This address cannot be used for the selected network. Check the address and try again."
                )
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            CatalogLabel("Warning")
            AppTextField(
                value = warning,
                onValueChange = { warning = it },
                placeholder = "Amount",
                message = AppTextFieldMessage(
                    AppTextFieldMessageType.Warning,
                    "Amount is above the recommended limit for this channel."
                )
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            CatalogLabel("Info")
            AppTextField(
                value = info,
                onValueChange = { info = it },
                placeholder = "Amount",
                message = AppTextFieldMessage(
                    AppTextFieldMessageType.Info,
                    "Funds arrive after one confirmation."
                )
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            CatalogLabel("Disabled")
            AppTextField(value = "", onValueChange = {}, placeholder = "Placeholder", enabled = false)
            AppTextField(value = "Filled", onValueChange = {}, enabled = false)
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            CatalogLabel("Label and supporting text")
            AppTextField(
                value = labelled,
                onValueChange = { labelled = it },
                placeholder = "Wallet name",
                label = "Name",
                supporting = "Shown on the wallet list"
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            CatalogLabel("Leading content")
            AppTextField(
                value = withLeading,
                onValueChange = { withLeading = it },
                placeholder = "Search",
                leading = { AppIcon(HexagonIcon, contentDescription = null, modifier = Modifier.size(20.dp)) }
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            CatalogLabel("Leading and trailing content")
            AppTextField(
                value = withBoth,
                onValueChange = { withBoth = it },
                placeholder = "Address",
                leading = { AppIcon(HexagonIcon, contentDescription = null, modifier = Modifier.size(20.dp)) },
                trailing = {
                    AppTextFieldDefaults.IconButton(
                        imageVector = PhosphorIcons.Regular.Clipboard,
                        contentDescription = "Paste",
                        onClick = { withBoth = "bc1q" }
                    )
                }
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            CatalogLabel("Prefix and suffix")
            AppTextField(
                value = withAffixes,
                onValueChange = { withAffixes = it },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                clearable = false,
                prefix = { AppText("≈") },
                suffix = { AppText("BTC") }
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            CatalogLabel("Custom trailing content")
            AppTextField(
                value = password,
                onValueChange = { password = it },
                placeholder = "Password",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailing = {
                    AppTextFieldDefaults.IconButton(
                        imageVector = if (passwordVisible) PhosphorIcons.Regular.EyeSlash else PhosphorIcons.Regular.Eye,
                        contentDescription = if (passwordVisible) "Hide password" else "Show password",
                        onClick = { passwordVisible = !passwordVisible }
                    )
                }
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            CatalogLabel("Multiline, grows from one line")
            AppTextField(
                value = note,
                onValueChange = { note = it },
                placeholder = "Memo",
                singleLine = false
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            CatalogLabel("Multiline, text area")
            AppTextField(
                value = multiline,
                onValueChange = { multiline = it },
                placeholder = "Note",
                singleLine = false,
                minLines = 3
            )
        }
    }
}

