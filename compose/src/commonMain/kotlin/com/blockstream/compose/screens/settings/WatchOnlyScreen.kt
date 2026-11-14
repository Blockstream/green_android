package com.blockstream.compose.screens.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.id_enabled_1s
import blockstream_green.common.generated.resources.id_extended_public_key
import blockstream_green.common.generated.resources.id_extended_public_keys
import blockstream_green.common.generated.resources.id_multisig
import blockstream_green.common.generated.resources.id_output_descriptors
import blockstream_green.common.generated.resources.id_set_up_watchonly_credentials
import blockstream_green.common.generated.resources.id_singlesig
import blockstream_green.common.generated.resources.id_tip_you_can_use_the
import blockstream_green.common.generated.resources.key_multisig
import com.blockstream.compose.GreenPreview
import com.blockstream.compose.components.GreenRow
import com.blockstream.compose.extensions.icon
import com.blockstream.compose.managers.LocalPlatformManager
import com.blockstream.compose.models.settings.WatchOnlyViewModelAbstract
import com.blockstream.compose.models.settings.WatchOnlyViewModelPreview
import com.blockstream.compose.navigation.LocalInnerPadding
import com.blockstream.compose.navigation.NavigateDestinations
import com.blockstream.compose.screens.assetaccounts.Descriptor
import com.blockstream.compose.theme.bodyMedium
import com.blockstream.compose.theme.titleMedium
import com.blockstream.compose.theme.titleSmall
import com.blockstream.compose.theme.whiteMedium
import com.blockstream.compose.utils.SetupScreen
import com.blockstream.compose.utils.bottom
import com.blockstream.compose.utils.plus
import com.blockstream.data.extensions.isNotBlank
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun WatchOnlyScreen(
    viewModel: WatchOnlyViewModelAbstract
) {
    val scope = rememberCoroutineScope()
    val platformManager = LocalPlatformManager.current

    val multisigWatchOnly by viewModel.multisigWatchOnly.collectAsStateWithLifecycle()
    val extendedPublicKeysAccounts by viewModel.extendedPublicKeysAccounts.collectAsStateWithLifecycle()
    val outputDescriptorsAccounts by viewModel.outputDescriptorsAccounts.collectAsStateWithLifecycle()
    val innerPadding = LocalInnerPadding.current

    SetupScreen(viewModel = viewModel, withPadding = false, withBottomInsets = false) {

        LazyColumn(
            contentPadding = PaddingValues(16.dp) + innerPadding.bottom(),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {

            if (multisigWatchOnly.isNotEmpty()) {
                item {
                    GreenRow(
                        padding = 0,
                        space = 4,
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        Icon(
                            painter = painterResource(Res.drawable.key_multisig),
                            contentDescription = null
                        )

                        Text(
                            text = stringResource(Res.string.id_multisig),
                            style = titleMedium,
                        )
                    }

                }

                items(multisigWatchOnly) { look ->
                    Setting(
                        title = look.network?.canonicalName ?: "Network",
                        subtitle = look.username?.takeIf { it.isNotBlank() }
                            ?.let { stringResource(Res.string.id_enabled_1s, it) }
                            ?: stringResource(Res.string.id_set_up_watchonly_credentials),
                        modifier = Modifier.clickable {
                            look.network?.also { network ->
                                viewModel.postEvent(
                                    NavigateDestinations.WatchOnlyCredentialsSettings(
                                        greenWallet = viewModel.greenWallet,
                                        network = network
                                    )
                                )
                            }
                        }
                    )
                }
            }

            if (extendedPublicKeysAccounts.isNotEmpty() || outputDescriptorsAccounts.isNotEmpty()) {
                item {
                    GreenRow(
                        padding = 0,
                        space = 4,
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        Icon(
                            painter = painterResource(Res.drawable.key_multisig),
                            contentDescription = null
                        )

                        Text(
                            text = stringResource(Res.string.id_singlesig),
                            style = titleMedium
                        )
                    }
                }
            }

            if (extendedPublicKeysAccounts.isNotEmpty()) {
                item {
                    Column {
                        Text(
                            text = stringResource(Res.string.id_extended_public_keys),
                            style = titleSmall,
                            modifier = Modifier.padding(top = 8.dp)
                        )

                        Text(
                            text = stringResource(Res.string.id_tip_you_can_use_the),
                            style = bodyMedium,
                            color = whiteMedium
                        )
                    }
                }

                items(extendedPublicKeysAccounts) {
                    Descriptor(
                        title = it.account?.name ?: "-",
                        icon = painterResource(it.account!!.network.icon()),
                        descriptor = it.extendedPubkey ?: "-",
                        onCopy = {
                            platformManager.copyToClipboard(content = it.extendedPubkey ?: "-")
                        },
                        onQr = {
                            scope.launch {
                                viewModel.postEvent(
                                    NavigateDestinations.Qr(
                                        greenWallet = viewModel.greenWallet,
                                        title = getString(Res.string.id_extended_public_key),
                                        subtitle = it.account?.name,
                                        data = it.extendedPubkey ?: ""
                                    )
                                )
                            }
                        }
                    )
                }
            }

            if (outputDescriptorsAccounts.isNotEmpty()) {
                item {
                    Text(
                        text = stringResource(Res.string.id_output_descriptors),
                        style = titleSmall,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                items(outputDescriptorsAccounts) {
                    Descriptor(
                        title = it.account?.name ?: "-",
                        icon = painterResource(it.account!!.network.icon()),
                        descriptor = it.outputDescriptors ?: "-",
                        onCopy = {
                            platformManager.copyToClipboard(content = it.outputDescriptors ?: "-")
                        },
                        onQr = {
                            scope.launch {
                                viewModel.postEvent(
                                    NavigateDestinations.Qr(
                                        greenWallet = viewModel.greenWallet,
                                        title = getString(Res.string.id_output_descriptors),
                                        subtitle = it.account?.name,
                                        data = it.outputDescriptors ?: ""
                                    )
                                )
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
@Preview
fun WatchOnlyScreenPreview() {
    GreenPreview {
        WatchOnlyScreen(viewModel = WatchOnlyViewModelPreview.preview())
    }
}
