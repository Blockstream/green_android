package com.blockstream.compose.screens.add


import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.blockstream.common.looks.AccountTypeLook
import com.blockstream.common.models.add.ChooseAccountTypeViewModelPreview
import com.blockstream.compose.GreenAndroidPreview
import com.blockstream.compose.components.GreenColumn
import com.blockstream.compose.theme.GreenThemePreview

@Composable
@Preview
fun AccountTypePreview() {
    GreenThemePreview {
        GreenColumn {
            AccountType(
                AccountTypeLook(
                    accountType = com.blockstream.common.gdk.data.AccountType.BIP44_LEGACY,
                    canBeAdded = true
                )
            )
            AccountType(
                AccountTypeLook(
                    accountType = com.blockstream.common.gdk.data.AccountType.LIGHTNING,
                    canBeAdded = true
                )
            )
            AccountType(
                AccountTypeLook(
                    accountType = com.blockstream.common.gdk.data.AccountType.LIGHTNING,
                    canBeAdded = false
                )
            )
        }
    }
}


@Composable
@Preview
fun ChooseAccountTypeScreenPreview() {
    GreenAndroidPreview {
        ChooseAccountTypeScreen(viewModel = ChooseAccountTypeViewModelPreview.preview())
    }
}
