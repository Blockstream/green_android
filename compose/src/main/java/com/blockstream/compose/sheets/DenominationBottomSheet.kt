package com.blockstream.compose.sheets

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.koin.getScreenModel
import com.arkivanov.essenty.parcelable.Parcelable
import com.arkivanov.essenty.parcelable.Parcelize
import com.blockstream.common.data.DenominatedValue
import com.blockstream.common.data.Denomination
import com.blockstream.common.data.GreenWallet
import com.blockstream.common.models.send.DenominationViewModel
import com.blockstream.common.models.send.DenominationViewModelAbstract
import com.blockstream.common.models.send.DenominationViewModelPreview
import com.blockstream.compose.GreenPreview
import com.blockstream.compose.R
import com.blockstream.compose.components.GreenBottomSheet
import com.blockstream.compose.components.GreenRow
import com.blockstream.compose.navigation.resultKey
import com.blockstream.compose.navigation.setNavigationResult
import com.blockstream.compose.theme.GreenThemePreview
import com.blockstream.compose.theme.bodyLarge
import com.blockstream.compose.theme.green
import com.blockstream.compose.theme.titleSmall
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@Parcelize
data class DenominationBottomSheet(
    val greenWallet: GreenWallet,
    val denominatedValue: DenominatedValue
) : BottomScreen(), Parcelable {
    @Composable
    override fun Content() {

        val viewModel = koinViewModel<DenominationViewModel> {
            parametersOf(greenWallet, denominatedValue)
        }

        DenominationBottomSheet(
            viewModel = viewModel,
            onDismissRequest = onDismissRequest()
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DenominationBottomSheet(
    viewModel: DenominationViewModelAbstract,
    onDismissRequest: () -> Unit,
) {
    GreenBottomSheet(
        title = stringResource(id = R.string.id_enter_amount_in),
        viewModel = viewModel,
        withHorizontalPadding = false,
        sheetState = rememberModalBottomSheetState(
            skipPartiallyExpanded = true,
        ),
        onDismissRequest = onDismissRequest
    ) {

        val denominations by viewModel.denominations.collectAsStateWithLifecycle()

        Column {
            denominations.forEach { denominatedValue ->
                DenominatedValueItem(
                    denominatedValue = denominatedValue,
                    isChecked = denominatedValue.denomination == viewModel.denomination.value,
                    onClick = {
                        setNavigationResult(
                            DenominationBottomSheet::class.resultKey,
                            denominatedValue
                        )
                        onDismissRequest()
                    })

                HorizontalDivider()
            }
        }

    }
}

@Composable
fun DenominatedValueItem(
    denominatedValue: DenominatedValue,
    isChecked: Boolean = false,
    onClick: () -> Unit = {}
) {
    Box(modifier = Modifier
        .fillMaxWidth()
        .clickable {
            onClick()
        }
    ) {
        GreenRow(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = denominatedValue.denomination.denomination, style = titleSmall)
                denominatedValue.asLook?.also {
                    Text(text = it, style = bodyLarge)
                }
            }

            if (isChecked) {
                Icon(
                    painter = painterResource(id = R.drawable.check_circle),
                    contentDescription = null,
                    tint = green
                )
            }
        }
    }
}

@Composable
@Preview
fun DenominatedValueItemPreview() {
    GreenThemePreview {
        Column {
            DenominatedValueItem(DenominatedValue(Denomination.BTC))
            DenominatedValueItem(DenominatedValue(Denomination.SATOSHI), isChecked = true)
        }
    }
}

@Composable
@Preview
fun DenominationBottomSheetPreview() {
    GreenPreview {
        DenominationBottomSheet(
            viewModel = DenominationViewModelPreview.preview(),
            onDismissRequest = { }
        )
    }
}