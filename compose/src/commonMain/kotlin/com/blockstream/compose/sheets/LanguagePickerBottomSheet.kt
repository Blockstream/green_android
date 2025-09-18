package com.blockstream.compose.sheets

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.id_language
import blockstream_green.common.generated.resources.id_system_default
import com.blockstream.common.models.settings.AppSettingsViewModel
import com.blockstream.common.models.settings.AppSettingsViewModelAbstract
import com.blockstream.compose.components.GreenBottomSheet
import com.blockstream.ui.components.GreenRow
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanguagePickerBottomSheet(
    viewModel: AppSettingsViewModelAbstract,
    onDismissRequest: () -> Unit
) {
    val locales by viewModel.locales.collectAsStateWithLifecycle()
    val currentLocale by viewModel.locale.collectAsStateWithLifecycle()
    
    GreenBottomSheet(
        title = stringResource(Res.string.id_language),
        onDismissRequest = onDismissRequest,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    ) {
        LazyColumn(
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {
            items(locales.toList()) { (key, value) ->
                val isSelected = key == currentLocale
                
                GreenRow(
                    padding = 0,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            viewModel.locale.value = key
                            viewModel.postEvent(AppSettingsViewModel.LocalEvents.AutoSave)
                            onDismissRequest()
                        }
                ) {
                    Text(
                        text = value ?: stringResource(Res.string.id_system_default),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .weight(1f)
                            .padding(vertical = 16.dp)
                    )
                }
                
                if (locales.toList().indexOf(key to value) < locales.size - 1) {
                    HorizontalDivider(
                        thickness = 0.5.dp,
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                    )
                }
            }
        }
    }
}