package com.blockstream.compose.screens.transaction

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.arrow_u_left_down
import blockstream_green.common.generated.resources.export
import blockstream_green.common.generated.resources.magnifying_glass
import blockstream_green.common.generated.resources.pencil_simple_line
import com.blockstream.compose.models.transaction.TransactionViewModelPreview
import com.blockstream.compose.GreenAndroidPreview
import com.blockstream.compose.theme.GreenChromePreview
import org.jetbrains.compose.resources.painterResource

object TransactionViewModelPreviewProvider : PreviewParameterProvider<TransactionViewModelPreview> {
    override val values = sequenceOf(
        TransactionViewModelPreview.previewUnconfirmed(),
        TransactionViewModelPreview.previewConfirmed(),
        TransactionViewModelPreview.previewCompleted(),
        TransactionViewModelPreview.previewFailed()
    )
}

@Composable
@Preview
fun TransactionScreenPreview(
    //@PreviewParameter(TransactionViewModelPreviewProvider::class)
//    viewModel: TransactionViewModelPreview
) {
    GreenAndroidPreview {
        TransactionScreen(viewModel = TransactionViewModelPreview.previewCompleted())
    }
}

@Composable
@Preview
fun TransactionScreenPreviewConfirmed() {
    GreenAndroidPreview {
        TransactionScreen(viewModel = TransactionViewModelPreview.previewConfirmed())
    }
}

@Composable
@Preview
fun TransactionScreenPreviewFailed() {
    GreenAndroidPreview {
        TransactionScreen(viewModel = TransactionViewModelPreview.previewFailed())
    }
}

@Composable
@Preview
fun MenuPreview() {
    GreenChromePreview {
        Column {
            HorizontalDivider()
            MenuListItem("Add Note", painterResource(Res.drawable.pencil_simple_line))
            HorizontalDivider()
            MenuListItem("Share Transaction", painterResource(Res.drawable.export))
            HorizontalDivider()
            MenuListItem("Initiate Refund", painterResource(Res.drawable.arrow_u_left_down))
            HorizontalDivider()
            MenuListItem("More Details", painterResource(Res.drawable.magnifying_glass))
        }
    }
}