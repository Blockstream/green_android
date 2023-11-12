@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.blockstream.compose.views

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.blockstream.common.extensions.isNotBlank
import com.blockstream.common.models.GreenViewModel
import com.blockstream.common.sideeffects.SideEffects
import com.blockstream.compose.components.GreenColumn
import com.blockstream.compose.theme.GreenTheme
import com.blockstream.compose.theme.bodySmall
import com.blockstream.compose.theme.titleSmall
import com.blockstream.compose.utils.HandleSideEffect
import com.blockstream.compose.utils.ifTrue


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GreenBottomSheet(
    title: String? = null,
    subtitle: String? = null,
    viewModel: GreenViewModel? = null,
    withHorizontalPadding: Boolean = true,
    sheetState: SheetState = rememberModalBottomSheetState(),
    onDismissRequest: () -> Unit,
    content: @Composable () -> Unit
) {

    if(viewModel != null) {
        HandleSideEffect(viewModel) {
            if (it is SideEffects.Dismiss) {
                onDismissRequest()
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        containerColor = MaterialTheme.colorScheme.background,
        sheetState = sheetState
    ) {
        GreenColumn(
            padding = 0,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 48.dp)
                .ifTrue(withHorizontalPadding){
                    padding(horizontal = 16.dp)
                }

        ) {

            if (title?.isNotBlank() == true || subtitle?.isNotBlank() == true) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                ) {
                    if (title?.isNotBlank() == true) {
                        Text(
                            text = title,
                            style = titleSmall,
                            modifier = Modifier
                                .align(Alignment.CenterHorizontally)
                        )
                    }

                    if (subtitle?.isNotBlank() == true) {
                        Text(
                            text = subtitle,
                            style = bodySmall,
                            modifier = Modifier
                                .align(Alignment.CenterHorizontally)
                        )
                    }
                }
            }

            content()
        }
    }
}

@ExperimentalMaterial3Api
@Composable
@Preview(showSystemUi = true, showBackground = true)
fun GreenBottomSheetPreview() {
    GreenTheme {
        Text("WalletRenameBottomSheet")

        var showBottomSheet by remember { mutableStateOf(true) }
        GreenBottomSheet(title = "Wallet Name", subtitle = "Change your wallet name", onDismissRequest = {
            showBottomSheet = false
        }) {
            Text(text = "OK")
        }
    }
}