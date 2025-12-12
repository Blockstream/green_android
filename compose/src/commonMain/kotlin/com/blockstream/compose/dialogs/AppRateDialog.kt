package com.blockstream.compose.dialogs

import androidx.compose.runtime.Composable
import com.blockstream.compose.models.GreenViewModel

@Composable
expect fun AppRateDialog(viewModel: GreenViewModel, onDismissRequest: () -> Unit)
