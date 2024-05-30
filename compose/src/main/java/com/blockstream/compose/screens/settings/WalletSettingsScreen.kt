package com.blockstream.compose.screens.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import com.arkivanov.essenty.parcelable.Parcelable
import com.arkivanov.essenty.parcelable.Parcelize
import com.blockstream.common.data.GreenWallet
import com.blockstream.common.data.LogoutReason
import com.blockstream.common.data.WalletSetting
import com.blockstream.common.events.Events
import com.blockstream.common.gdk.data.Network
import com.blockstream.common.models.settings.DenominationExchangeRateViewModel
import com.blockstream.common.models.settings.WalletSettingsSection
import com.blockstream.common.models.settings.WalletSettingsViewModel
import com.blockstream.common.models.settings.WalletSettingsViewModelAbstract
import com.blockstream.common.models.settings.WalletSettingsViewModelPreview
import com.blockstream.common.navigation.NavigateDestinations
import com.blockstream.common.sideeffects.SideEffects
import com.blockstream.common.utils.AndroidKeystore
import com.blockstream.compose.GreenPreview
import com.blockstream.compose.LocalDialog
import com.blockstream.compose.LocalSnackbar
import com.blockstream.compose.R
import com.blockstream.compose.components.GreenColumn
import com.blockstream.compose.components.GreenRow
import com.blockstream.compose.components.LearnMoreButton
import com.blockstream.compose.dialogs.DenominationExchangeDialog
import com.blockstream.compose.dialogs.SingleChoiceDialog
import com.blockstream.compose.dialogs.TextDialog
import com.blockstream.compose.extensions.colorText
import com.blockstream.compose.sideeffects.BiometricsState
import com.blockstream.compose.theme.GreenThemePreview
import com.blockstream.compose.theme.bodyLarge
import com.blockstream.compose.theme.red
import com.blockstream.compose.theme.titleMedium
import com.blockstream.compose.theme.titleSmall
import com.blockstream.compose.theme.whiteHigh
import com.blockstream.compose.theme.whiteLow
import com.blockstream.compose.theme.whiteMedium
import com.blockstream.compose.utils.AppBar
import com.blockstream.compose.utils.HandleSideEffect
import com.blockstream.compose.utils.stringResourceId
import cafe.adriel.voyager.koin.koinScreenModel
import org.koin.compose.koinInject
import org.koin.core.parameter.parametersOf

@Parcelize
data class WalletSettingsScreen(
    val greenWallet: GreenWallet,
    val section: WalletSettingsSection = WalletSettingsSection.General,
    val network: Network? = null
) : Screen, Parcelable {
    @Composable
    override fun Content() {
        val viewModel = koinScreenModel<WalletSettingsViewModel> {
            parametersOf(greenWallet, section, network)
        }

        val navData by viewModel.navData.collectAsStateWithLifecycle()

        AppBar(navData)

        WalletSettingsScreen(viewModel = viewModel)
    }
}

@Composable
fun WalletSettingsScreen(
    viewModel: WalletSettingsViewModelAbstract
) {
    var denominationExchangeRateViewModel by remember {
        mutableStateOf<DenominationExchangeRateViewModel?>(null)
    }
    var showPgpDialog by remember { mutableStateOf<String?>(null) }
    var showAutologoutTimeoutDialog by remember { mutableStateOf<Int?>(null) }

    val context = LocalContext.current
    val snackbar = LocalSnackbar.current
    val scope = rememberCoroutineScope()
    val dialog = LocalDialog.current
    // LocalInspectionMode is true in preview
    val androidKeystore: AndroidKeystore =
        if (LocalInspectionMode.current) AndroidKeystore(context) else koinInject()

    val biometricsState = remember {
        BiometricsState(
            context = context,
            coroutineScope = scope,
            snackbarHostState = snackbar,
            dialogState = dialog,
            androidKeystore = androidKeystore
        )
    }

    HandleSideEffect(viewModel) {
        if (it is WalletSettingsViewModel.LocalSideEffects.OpenAutologoutTimeout) {
            showAutologoutTimeoutDialog = it.minutes
        } else if (it is WalletSettingsViewModel.LocalSideEffects.OpenPgpKey) {
            showPgpDialog = it.pgp
        } else if (it is WalletSettingsViewModel.LocalSideEffects.LaunchBiometrics) {
            biometricsState.getBiometricsCipher(viewModel)
        } else if (it is SideEffects.OpenDenominationExchangeRate) {
            denominationExchangeRateViewModel =
                DenominationExchangeRateViewModel(viewModel.greenWallet)
        }
    }

    denominationExchangeRateViewModel?.also {
        DenominationExchangeDialog(viewModel = it) {
            denominationExchangeRateViewModel = null
        }
    }

    if (showPgpDialog != null) {
        TextDialog(
            title = stringResource(R.string.id_pgp_key),
            message = stringResource(R.string.id_add_a_pgp_public_key_to_receive),
            label = stringResource(id = R.string.id_pgp_key)
        ) { key ->
            showPgpDialog = null

            if (key != null) {
                viewModel.postEvent(WalletSettingsViewModel.LocalEvents.SetPgp(key))
            }
        }
    }

    if (showAutologoutTimeoutDialog != null) {
        val values = listOf(1, 2, 5, 10, 60)
        val entries = values.map {
            if (it == 1) "1 ${stringResource(R.string.id_minute)}" else stringResource(
                R.string.id_1d_minutes,
                it
            )
        }

        SingleChoiceDialog(
            title = stringResource(id = R.string.id_auto_logout_timeout),
            items = entries,
            checkedItem = values.indexOf(showAutologoutTimeoutDialog).takeIf { it >= 0 }
        ) { position ->
            showAutologoutTimeoutDialog = null

            if (position != null) {
                viewModel.postEvent(WalletSettingsViewModel.LocalEvents.SetAutologoutTimeout(values[position]))
            }
        }
    }

    val items by viewModel.items.collectAsStateWithLifecycle()

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {

        items(items) { item ->
            when (item) {
                is WalletSetting.Text -> {
                    item.title?.also {
                        Text(
                            text = stringResourceId(it),
                            style = titleMedium,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                    item.message?.also {
                        Text(
                            text = stringResourceId(it),
                            style = bodyLarge,
                            color = whiteMedium
                        )
                    }
                }

                is WalletSetting.LearnMore -> {
                    LearnMoreButton {
                        viewModel.postEvent(item.event)
                    }
                }

                WalletSetting.Logout -> {
                    Setting(
                        title = "id_logout",
                        subtitle = viewModel.greenWallet.name,
                        subtitleColor = red,
                        painter = painterResource(id = R.drawable.sign_out),
                        modifier = Modifier.clickable {
                            viewModel.postEvent(Events.Logout(LogoutReason.USER_ACTION))
                        }
                    )
                }

                is WalletSetting.DenominationExchangeRate -> {
                    val list = listOf(
                        item.unit,
                        item.currency,
                        item.exchange
                    )
                    Setting(
                        title = "id_denomination__exchange_rate",
                        subtitleAnnotated = colorText(
                            text = stringResource(
                                R.string.id_display_values_in_s_and, *list.toTypedArray()
                            ),
                            coloredTexts = list,
                            baseColor = whiteMedium,
                            color = whiteHigh
                        ),
                        modifier = Modifier.clickable {
                            viewModel.postEvent(WalletSettingsViewModel.LocalEvents.DenominationExchangeRate)
                        })

                }

                is WalletSetting.ArchivedAccounts -> {
                    Setting(
                        title = "id_archived_accounts",
                        subtitle = "(${item.size})".takeIf { item.size > 0 },
                        painter = painterResource(id = R.drawable.caret_right),
                        modifier = Modifier.clickable {
                            viewModel.postEvent(NavigateDestinations.ArchivedAccounts())
                        })
                }

                WalletSetting.WatchOnly -> {
                    Setting(
                        title = "id_watchonly",
                        painter = painterResource(id = R.drawable.caret_right),
                        modifier = Modifier.clickable {
                            viewModel.postEvent(WalletSettingsViewModel.LocalEvents.WatchOnly)
                        })
                }

                is WalletSetting.AutologoutTimeout -> {
                    Setting(
                        title = "id_auto_logout_timeout",
                        subtitle = if (item.timeout == 1) "${item.timeout} ${stringResource(id = R.string.id_minute)}" else stringResource(
                            R.string.id_1d_minutes,
                            item.timeout
                        ),
                        modifier = Modifier.clickable {
                            viewModel.postEvent(WalletSettingsViewModel.LocalEvents.AutologoutTimeout)
                        })
                }

                is WalletSetting.RecoveryTransactionEmails -> {
                    Setting(
                        title = "id_recovery_transaction_emails",
                        checked = item.enabled,
                        onCheckedChange = {
                            viewModel.postEvent(WalletSettingsViewModel.LocalEvents.RecoveryTransactionEmails)
                        },
                        modifier = Modifier.clickable {
                            viewModel.postEvent(WalletSettingsViewModel.LocalEvents.RecoveryTransactionEmails)
                        }
                    )
                }

                WalletSetting.RequestRecoveryTransactions -> {
                    Setting(
                        title = "id_request_recovery_transactions",
                        modifier = Modifier.clickable {
                            viewModel.postEvent(WalletSettingsViewModel.LocalEvents.RequestRecoveryTransactions)
                        })
                }

                WalletSetting.SetupEmailRecovery -> {
                    Setting(
                        title = "id_set_an_email_for_recovery",
                        painter = painterResource(id = R.drawable.caret_right),
                        modifier = Modifier.clickable {
                            viewModel.postEvent(WalletSettingsViewModel.LocalEvents.SetupEmailRecovery)
                        })
                }

                WalletSetting.ChangePin -> {
                    Setting(
                        title = "id_change_pin",
                        painter = painterResource(id = R.drawable.caret_right),
                        modifier = Modifier.clickable {
                            viewModel.postEvent(WalletSettingsViewModel.LocalEvents.ChangePin)
                        })
                }

                is WalletSetting.LoginWithBiometrics -> {
                    Setting(
                        title = "id_login_with_biometrics",
                        subtitle = stringResource(
                            if (item.canEnable) {
                                if (item.enabled) {
                                    R.string.id_biometric_login_is_enabled
                                } else {
                                    R.string.id_biometric_login_is_disabled
                                }
                            } else {
                                R.string.id_a_screen_lock_must_be_enabled
                            }
                        ),
                        checked = item.enabled,
                        onCheckedChange = {
                            viewModel.postEvent(WalletSettingsViewModel.LocalEvents.LoginWithBiometrics)
                        },
                        modifier = Modifier.clickable {
                            viewModel.postEvent(WalletSettingsViewModel.LocalEvents.LoginWithBiometrics)
                        }
                    )
                }

                is WalletSetting.PgpKey -> {
                    Setting(
                        title = "id_pgp_key",
                        subtitle = if (item.enabled) null else "id_add_a_pgp_public_key_to_receive",
                        modifier = Modifier.clickable {
                            viewModel.postEvent(WalletSettingsViewModel.LocalEvents.PgpKey)
                        }
                    )
                }

                WalletSetting.TwoFactorAuthentication -> {
                    Setting(
                        title = "id_twofactor_authentication",
                        painter = painterResource(id = R.drawable.caret_right),
                        modifier = Modifier.clickable {
                            viewModel.postEvent(WalletSettingsViewModel.LocalEvents.TwoFactorAuthentication)
                        }
                    )
                }

                WalletSetting.RecoveryPhrase -> {
                    Setting(
                        title = "id_backup_recovery_phrase",
                        subtitle = "id_touch_to_display",
                        painter = painterResource(id = R.drawable.caret_right),
                        modifier = Modifier.clickable {
                            viewModel.postEvent(WalletSettingsViewModel.LocalEvents.RecoveryPhrase)
                        })
                }

                is WalletSetting.Version -> {
                    Setting(
                        title = "id_version",
                        subtitle = item.version,
                        modifier = Modifier.clickable {
                            viewModel.postEvent(NavigateDestinations.About)
                        }
                    )
                }

                WalletSetting.Support -> {
                    Setting(
                        title = "id_support",
                        subtitle = "id_copy_support_id",
                        painter = painterResource(id = R.drawable.copy),
                        modifier = Modifier.clickable {
                            viewModel.postEvent(WalletSettingsViewModel.LocalEvents.SupportId)
                        })
                }
            }
        }
    }


    Box {
        GreenColumn(
            padding = 0,
            space = 0,
            modifier = Modifier
                .fillMaxHeight()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 100.dp),
        ) {


        }
    }
}


@Composable
fun Setting(
    modifier: Modifier = Modifier,
    title: String,
    subtitle: String? = null,
    subtitleAnnotated: AnnotatedString? = null,
    subtitleColor: Color = whiteMedium,
    painter: Painter? = null,
    checked: Boolean? = null,
    onCheckedChange: ((Boolean) -> Unit) = {},
) {
    Card(modifier = Modifier.then(modifier)) {
        GreenRow(space = 8, padding = 0, verticalAlignment = Alignment.Top) {
            GreenColumn(
                padding = 0, space = 4,
                modifier = Modifier
                    .padding(vertical = 16.dp)
                    .padding(start = 16.dp)
                    .weight(1f)
            ) {
                Text(
                    text = stringResourceId(title),
                    style = titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth()
                )
                if (subtitle != null) {
                    Text(
                        text = stringResourceId(subtitle),
                        style = bodyLarge,
                        color = subtitleColor
                    )
                }
                if (subtitleAnnotated != null) {
                    Text(
                        text = subtitleAnnotated,
                        style = bodyLarge,
                        color = subtitleColor
                    )
                }
            }

            if (checked != null) {
                Switch(
                    checked = checked,
                    onCheckedChange = onCheckedChange,
                    modifier = Modifier
                        .align(Alignment.CenterVertically)
                        .padding(end = 16.dp)
                )
            }

            if (painter != null) {
                Icon(
                    painter = painter,
                    contentDescription = null,
                    tint = whiteLow,
                    modifier = Modifier
                        .align(Alignment.CenterVertically)
                        .padding(end = 16.dp)
                )
            }
        }
    }
}

@Composable
@Preview
fun SettingPreview() {
    GreenThemePreview {
        GreenColumn {
            Text(
                text = stringResourceId("General"),
                style = titleMedium,
                modifier = Modifier.padding(top = 8.dp)
            )
            Setting(
                title = "Logout",
                subtitle = "Wallet",
                painter = painterResource(id = R.drawable.sign_out)
            )
            Setting(title = "Watch-only")
            Setting(title = "Change PIN")
            Setting(
                title = "Login with Biometrics",
                subtitle = "Biometrics Login is Enabled",
                checked = true
            )
            Setting(
                title = "Login with Biometrics",
                checked = true
            )
        }
    }
}


@Composable
@Preview
fun WalletSettingsScreenPreview() {
    GreenPreview {
        WalletSettingsScreen(viewModel = WalletSettingsViewModelPreview.preview())
    }
}

@Composable
@Preview
fun RecoveryTransactionsScreenPreview() {
    GreenPreview {
        WalletSettingsScreen(viewModel = WalletSettingsViewModelPreview.previewRecovery())
    }
}
