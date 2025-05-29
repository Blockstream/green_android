@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.blockstream.compose.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalBottomSheetDefaults
import androidx.compose.material3.ModalBottomSheetProperties
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.get
import com.blockstream.common.extensions.isNotBlank
import com.blockstream.common.models.GreenViewModel
import com.blockstream.common.sideeffects.SideEffects
import com.blockstream.compose.theme.bodyLarge
import com.blockstream.compose.theme.titleMedium
import com.blockstream.compose.theme.whiteHigh
import com.blockstream.compose.theme.whiteMedium
import com.blockstream.compose.utils.HandleSideEffect
import com.blockstream.ui.components.GreenColumn
import com.blockstream.ui.navigation.LocalNavigator
import com.blockstream.ui.navigation.bottomsheet.BottomSheetNavigator
import com.blockstream.ui.sideeffects.SideEffect
import com.blockstream.ui.utils.ifTrue
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
    properties: ModalBottomSheetProperties = ModalBottomSheetDefaults.properties,
    sideEffectHandler: CoroutineScope.(sideEffect: SideEffect) -> Unit = {},
    onDismissRequest: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    val bottomSheetNavigator = LocalNavigator.current.navigatorProvider[BottomSheetNavigator::class]
    LaunchedEffect(sheetState) {
        bottomSheetNavigator.setSheetState(sheetState)
    }

    if (viewModel != null) {
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
        sheetState = sheetState,
        properties = properties
    ) {
        GreenColumn(
            padding = 0,
            modifier = Modifier
                .fillMaxWidth()
                .ifTrue(withBottomPadding) {
                    it.padding(bottom = 48.dp)
                }
                .ifTrue(withHorizontalPadding) {
                    it.padding(horizontal = 16.dp)
                }

        ) {

            if (title?.isNotBlank() == true || subtitle?.isNotBlank() == true) {
                Column(
                    modifier = Modifier
                        .ifTrue(!withHorizontalPadding) {
                            it.padding(horizontal = 16.dp)
                        }
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    if (title?.isNotBlank() == true) {
                        Text(
                            text = title,
                            style = titleMedium,
                            textAlign = TextAlign.Center,
                            color = whiteHigh,
                            modifier = Modifier
                                .fillMaxWidth()
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
                                .align(Alignment.CenterHorizontally)
                        )
                    }
                }
            }

            content()
        }
    }
}