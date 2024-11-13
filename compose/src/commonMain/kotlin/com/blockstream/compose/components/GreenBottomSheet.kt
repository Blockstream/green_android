@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.blockstream.compose.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.blockstream.common.extensions.isNotBlank
import com.blockstream.common.models.GreenViewModel
import com.blockstream.common.sideeffects.SideEffect
import com.blockstream.common.sideeffects.SideEffects
import com.blockstream.compose.theme.bodyLarge
import com.blockstream.compose.theme.titleSmall
import com.blockstream.compose.theme.whiteHigh
import com.blockstream.compose.theme.whiteMedium
import com.blockstream.compose.utils.HandleSideEffect
import com.blockstream.compose.utils.ifTrue
import kotlinx.coroutines.CoroutineScope


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GreenBottomSheet(
    title: String? = null,
    subtitle: String? = null,
    viewModel: GreenViewModel? = null,
    withHorizontalPadding: Boolean = true,
    withBottomPadding: Boolean = true,
    sheetState: SheetState = rememberModalBottomSheetState(),
    sideEffectHandler: CoroutineScope.(sideEffect: SideEffect) -> Unit = {},
    onDismissRequest: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {

    if(viewModel != null) {
        HandleSideEffect(viewModel) {
            if (it is SideEffects.Dismiss) {
                onDismissRequest()
            }
            sideEffectHandler.invoke(this, it)
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
                .ifTrue(withBottomPadding){
                    padding(bottom = 48.dp)
                }
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
                            color = whiteHigh,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .padding(horizontal = 16.dp)
                                .align(Alignment.CenterHorizontally)
                        )
                    }

                    if (subtitle?.isNotBlank() == true) {
                        Text(
                            text = subtitle,
                            style = bodyLarge,
                            color = whiteMedium,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .padding(top = 4.dp)
                                .padding(horizontal = 16.dp)
                                .align(Alignment.CenterHorizontally)
                        )
                    }
                }
            }

            content()
        }
    }
}