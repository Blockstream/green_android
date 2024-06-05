package com.blockstream.compose.dialogs

import androidx.compose.runtime.Composable
import com.blockstream.common.models.GreenViewModel

@Composable
expect fun AppRateDialog(viewModel: GreenViewModel, onDismissRequest: () -> Unit)
