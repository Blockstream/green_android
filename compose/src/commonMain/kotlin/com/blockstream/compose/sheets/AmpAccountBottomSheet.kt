package com.blockstream.compose.sheets

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.id_amp
import blockstream_green.common.generated.resources.id_amp_account
import blockstream_green.common.generated.resources.id_amp_accounts_allow_you_to_send
import blockstream_green.common.generated.resources.id_amp_legacy
import blockstream_green.common.generated.resources.id_cancel
import blockstream_green.common.generated.resources.id_continue
import blockstream_green.common.generated.resources.id_create
import blockstream_green.common.generated.resources.id_create_amp_account
import blockstream_green.common.generated.resources.id_create_an_amp_account
import blockstream_green.common.generated.resources.id_creating_amp_account
import blockstream_green.common.generated.resources.id_learn_more
import blockstream_green.common.generated.resources.id_multisig_accounts_require_your_jade
import blockstream_green.common.generated.resources.id_share_your_amp_id
import blockstream_green.common.generated.resources.id_watchonly
import com.adamglin.PhosphorIcons
import com.adamglin.phosphoricons.Regular
import com.adamglin.phosphoricons.regular.Copy
import com.blockstream.compose.LocalDialog
import com.blockstream.compose.components.GreenBottomSheet
import com.blockstream.compose.components.GreenButton
import com.blockstream.compose.components.GreenButtonSize
import com.blockstream.compose.events.Events
import com.blockstream.compose.models.settings.AmpAccountViewModel
import com.blockstream.compose.models.settings.AmpAccountViewModelAbstract
import com.blockstream.compose.sideeffects.OpenDialogData
import com.blockstream.compose.theme.bodySmall
import com.blockstream.compose.theme.labelMedium
import com.blockstream.compose.theme.whiteMedium
import com.blockstream.compose.utils.StringHolder
import com.blockstream.data.Urls
import com.blockstream.data.extensions.middleTruncate
import com.blockstream.data.gdk.data.Account
import com.blockstream.data.gdk.data.AccountType
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource

@Composable
fun AmpAccountBottomSheet(
    viewModel: AmpAccountViewModelAbstract,
    onDismissRequest: () -> Unit,
) {
    val accounts by viewModel.session.accounts.collectAsStateWithLifecycle()
    val creatingAccountTypes by viewModel.creatingAccountTypes.collectAsStateWithLifecycle()
    val canCreateAccounts = viewModel.canCreateAccounts
    val canCreateAmp = viewModel.canCreateAmp
    val canCreateLegacy = viewModel.canCreateLegacy

    val ampAccount = accounts.firstOrNull { it.type.isAmp() }.takeIf { canCreateAmp }
    val ampLegacyAccounts = accounts.filter { it.type.isAmpLegacy() }
    val hasAmpAccounts = ampAccount != null || ampLegacyAccounts.isNotEmpty()
    val isCreating = creatingAccountTypes.isNotEmpty()

    val dialog = LocalDialog.current

    GreenBottomSheet(
        title = if (hasAmpAccounts) stringResource(Res.string.id_amp_account) else stringResource(Res.string.id_create_an_amp_account),
        subtitle = if (hasAmpAccounts) {
            stringResource(Res.string.id_share_your_amp_id)
        } else {
            stringResource(Res.string.id_amp_accounts_allow_you_to_send)
        },
        titleTextAlign = TextAlign.Start,
        bottomPadding = 64.dp,
        viewModel = viewModel,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false),
        onDismissRequest = onDismissRequest,
        sideEffectHandler = {
            when (it) {
                is AmpAccountViewModel.LocalSideEffects.JadeWoDisableDialog -> {
                    launch {
                        dialog.openDialog(
                            OpenDialogData(
                                title = StringHolder.create(Res.string.id_watchonly),
                                message = StringHolder.create(Res.string.id_multisig_accounts_require_your_jade),
                                primaryText = getString(Res.string.id_continue),
                                onPrimary = {
                                    viewModel.createAmpAccounts(accountTypes = it.accountTypes, actionConfirmed = true)
                                },
                                secondaryText = getString(Res.string.id_cancel)
                            )
                        )
                    }
                }
            }
        }
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        ) {
            if (!hasAmpAccounts && canCreateAccounts) {
                GreenButton(
                    text = if (isCreating) stringResource(Res.string.id_creating_amp_account) else stringResource(Res.string.id_create_amp_account),
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isCreating,
                    onProgress = isCreating,
                    size = GreenButtonSize.BIG,
                ) {
                    viewModel.createAmpAccounts()
                }
            } else if (hasAmpAccounts) {
                if (ampAccount != null || canCreateAmp) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(text = stringResource(Res.string.id_amp), style = labelMedium, color = whiteMedium)
                        AmpAccountRow(
                            account = ampAccount,
                            accountName = ampAccount?.name ?: "AMP Liquid",
                            canCreate = canCreateAmp,
                            isCreating = AccountType.AMP2_ACCOUNT in creatingAccountTypes,
                            onCreate = {
                                viewModel.createAmpAccounts(listOf(AccountType.AMP2_ACCOUNT))
                            },
                            onCopy = {
                                viewModel.copyAmpAccountId(it)
                            }
                        )
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = stringResource(if (canCreateAmp) Res.string.id_amp_legacy else Res.string.id_amp),
                        style = labelMedium,
                        color = whiteMedium
                    )
                    val ampLegacyRows: List<Account?> = ampLegacyAccounts.ifEmpty { listOf(null) }
                    ampLegacyRows.forEach { account ->
                        AmpAccountRow(
                            account = account,
                            accountName = account?.name ?: "AMP Liquid",
                            canCreate = canCreateLegacy,
                            isCreating = account == null && AccountType.AMP_LEGACY_ACCOUNT in creatingAccountTypes,
                            onCreate = {
                                viewModel.createAmpAccounts(listOf(AccountType.AMP_LEGACY_ACCOUNT))
                            },
                            onCopy = {
                                viewModel.copyAmpAccountId(it)
                            }
                        )
                    }
                }
            }

            TextButton(
                modifier = Modifier.align(Alignment.CenterHorizontally),
                onClick = {
                    viewModel.postEvent(Events.OpenBrowser(Urls.HELP_AMP_ASSETS))
                }
            ) {
                Text(text = stringResource(Res.string.id_learn_more))
            }
        }
    }
}

@Composable
private fun AmpAccountRow(
    account: Account?,
    accountName: String,
    canCreate: Boolean,
    isCreating: Boolean,
    onCreate: () -> Unit,
    onCopy: (Account) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, MaterialTheme.shapes.small)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = accountName, style = labelMedium)
            val ampId = account?.receivingId
            if (!ampId.isNullOrBlank()) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(text = "ID: ", style = bodySmall, color = whiteMedium)
                    Text(
                        text = ampId.middleTruncate(),
                        style = bodySmall,
                        color = whiteMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Clip,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        if (account == null && canCreate) {
            GreenButton(
                text = stringResource(Res.string.id_create),
                enabled = !isCreating,
                onProgress = isCreating,
                onClick = onCreate
            )
        } else if (account != null) {
            Icon(
                imageVector = PhosphorIcons.Regular.Copy,
                contentDescription = null,
                modifier = Modifier.clickable { onCopy(account) }
            )
        }
    }
}
