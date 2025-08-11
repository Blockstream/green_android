package com.blockstream.compose.screens.archived

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.id_no_archived_accounts
import blockstream_green.common.generated.resources.id_unarchive
import com.blockstream.common.models.archived.ArchivedAccountsViewModelAbstract
import com.blockstream.compose.components.GreenAccountAsset
import com.blockstream.compose.components.GreenButton
import com.blockstream.compose.components.GreenButtonSize
import com.blockstream.compose.theme.bodyMedium
import com.blockstream.compose.utils.SetupScreen
import org.jetbrains.compose.resources.stringResource

@Composable
fun ArchivedAccountsScreen(
    viewModel: ArchivedAccountsViewModelAbstract
) {
    val archivedAccounts by viewModel.archivedAccounts.collectAsStateWithLifecycle()
    val selectedAccounts by viewModel.selectedAccounts.collectAsStateWithLifecycle()

    SetupScreen(viewModel = viewModel, withPadding = false) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            if (archivedAccounts.isEmpty()) {
                Text(
                    text = stringResource(Res.string.id_no_archived_accounts),
                    style = bodyMedium,
                    textAlign = TextAlign.Center,
                    fontStyle = FontStyle.Italic,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 32.dp)
                        .padding(horizontal = 16.dp)
                )
            } else if (archivedAccounts.isSuccess()) {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(all = 16.dp)
                ) {
                    items(archivedAccounts.data() ?: listOf()) { account ->
                        GreenAccountAsset(
                            modifier = Modifier.fillMaxWidth(),
                            accountAssetBalance = account,
                            withAsset = false,
                            session = viewModel.sessionOrNull,
                            selectable = true,
                            isSelected = selectedAccounts.contains(account.account),
                            onClick = {
                                viewModel.toggleAccountSelection(account.account)
                            }
                        )
                    }
                }
                
                GreenButton(
                    text = if (selectedAccounts.isEmpty()) {
                        stringResource(Res.string.id_unarchive)
                    } else {
                        "${stringResource(Res.string.id_unarchive)} (${selectedAccounts.size})"
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    size = GreenButtonSize.BIG,
                    enabled = selectedAccounts.isNotEmpty()
                ) {
                    viewModel.unarchiveSelected()
                }
            }
        }
    }
}