package com.blockstream.compose.sheets

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import cafe.adriel.voyager.koin.koinScreenModel
import com.blockstream.common.Parcelable
import com.blockstream.common.Parcelize
import com.blockstream.common.data.GreenWallet
import com.blockstream.common.models.GreenViewModel
import com.blockstream.common.models.SimpleGreenViewModel
import com.blockstream.compose.components.GreenBottomSheet
import com.blockstream.compose.components.GreenQR
import org.koin.core.parameter.parametersOf

@Parcelize
data class QrBottomSheet(
    val greenWallet: GreenWallet,
    val title: String? = null,
    val subtitle: String? = null,
    val data: String
) : BottomScreen(), Parcelable {
    @Composable
    override fun Content() {
        val viewModel = koinScreenModel<SimpleGreenViewModel> {
            parametersOf(greenWallet)
        }

        QrBottomSheet(
            viewModel = viewModel,
            title = title,
            subtitle = subtitle,
            data = data,
            onDismissRequest = onDismissRequest()
        )
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QrBottomSheet(
    viewModel: GreenViewModel,
    title: String? = null,
    subtitle: String? = null,
    data: String,
    onDismissRequest: () -> Unit,
) {
    GreenBottomSheet(
        title = title,
        subtitle = subtitle,
        viewModel = viewModel,
        sheetState = rememberModalBottomSheetState(
            skipPartiallyExpanded = true
        ),
        onDismissRequest = onDismissRequest
    ) {
        GreenQR(data = data, modifier = Modifier.align(Alignment.CenterHorizontally))
    }
}