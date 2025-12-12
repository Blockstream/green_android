package com.blockstream.compose.screens.assetaccounts

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.id_output_descriptors
import blockstream_green.common.generated.resources.id_you_can_use_your_descriptor_to
import blockstream_green.common.generated.resources.qr_code
import com.adamglin.PhosphorIcons
import com.adamglin.phosphoricons.Regular
import com.adamglin.phosphoricons.regular.Copy
import com.adamglin.phosphoricons.regular.Info
import com.blockstream.compose.GreenPreview
import com.blockstream.compose.components.GreenAlert
import com.blockstream.compose.components.GreenColumn
import com.blockstream.compose.components.GreenRow
import com.blockstream.compose.extensions.icon
import com.blockstream.compose.managers.LocalPlatformManager
import com.blockstream.compose.models.assetaccounts.AccountDescriptorViewModelAbstract
import com.blockstream.compose.models.assetaccounts.AccountDescriptorViewModelPreview
import com.blockstream.compose.navigation.NavigateDestinations
import com.blockstream.compose.theme.MonospaceFont
import com.blockstream.compose.theme.bodyMedium
import com.blockstream.compose.theme.titleSmall
import com.blockstream.compose.theme.whiteMedium
import com.blockstream.compose.utils.SetupScreen
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun AccountDescriptorScreen(
    viewModel: AccountDescriptorViewModelAbstract
) {
    val scope = rememberCoroutineScope()
    val platformManager = LocalPlatformManager.current

    val descriptor by viewModel.descriptor.collectAsStateWithLifecycle()

    SetupScreen(viewModel = viewModel, withPadding = false) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(PaddingValues(16.dp) ),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            GreenAlert(
                title = null,
                message = stringResource(Res.string.id_you_can_use_your_descriptor_to),
                isBlue = true,
                icon = PhosphorIcons.Regular.Info,
                modifier = Modifier.fillMaxWidth()
            )

            Text(
                text = stringResource(Res.string.id_output_descriptors),
                style = titleSmall
            )

            Descriptor(
                title = viewModel.account.name,
                icon = painterResource(viewModel.account.network.icon()),
                descriptor = descriptor,
                onCopy = {
                    platformManager.copyToClipboard(content = descriptor)
                },
                onQr = {
                    scope.launch {
                        viewModel.postEvent(
                            NavigateDestinations.Qr(
                                greenWallet = viewModel.greenWallet,
                                title = getString(Res.string.id_output_descriptors),
                                subtitle = viewModel.account.name,
                                data = descriptor
                            )
                        )
                    }
                }
            )
        }
    }
}

@Composable
fun Descriptor(
    modifier: Modifier = Modifier,
    title: String,
    icon: Painter,
    descriptor: String,
    onCopy: () -> Unit = {},
    onQr: () -> Unit = {}
) {
    Card(modifier = Modifier.then(modifier)) {
        GreenColumn(
            space = 0,
            padding = 0,
            modifier = Modifier.padding(vertical = 16.dp).padding(start = 16.dp, end = 8.dp)
        ) {

            GreenRow(
                space = 8,
                padding = 0,
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                Image(
                    painter = icon,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )

                Text(
                    text = title,
                    style = titleSmall,
                    maxLines = 1,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Row(verticalAlignment = Alignment.Top, modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = descriptor,
                    style = bodyMedium,
                    color = whiteMedium,
                    fontFamily = MonospaceFont(),
                    modifier = Modifier.weight(1f)
                )

                Row {
                    IconButton(onCopy) {
                        Icon(
                            imageVector = PhosphorIcons.Regular.Copy,
                            contentDescription = null,
                            modifier = Modifier.minimumInteractiveComponentSize()
                        )
                    }
                    IconButton(onQr) {
                        Icon(
                            painter = painterResource(Res.drawable.qr_code),
                            contentDescription = null,
                            modifier = Modifier.minimumInteractiveComponentSize()
                        )
                    }
                }
            }
        }

    }
}

@Composable
@Preview
fun AccountDescriptorScreenPreview() {
    GreenPreview {
        AccountDescriptorScreen(
            viewModel = AccountDescriptorViewModelPreview.preview()
        )
    }
}