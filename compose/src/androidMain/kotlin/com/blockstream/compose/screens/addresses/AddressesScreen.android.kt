package com.blockstream.compose.screens.addresses

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.blockstream.common.looks.account.AddressLook
import com.blockstream.common.models.addresses.AddressesViewModelPreview
import com.blockstream.compose.GreenAndroidPreview
import com.blockstream.compose.theme.GreenChromePreview
import com.blockstream.ui.components.GreenColumn

@Composable
@Preview
fun AddressListItemPreview() {
    GreenChromePreview {
        GreenColumn {
            AddressListItem(
                AddressLook(
                    address = "bc1qaqtq80759n35gk6ftc57vh7du83nwvt5lgkznu",
                    index = 1,
                    txCount = "99",
                    canSign = true
                )
            )
        }
    }
}

@Composable
@Preview
fun AddressesScreenPreview() {
    GreenAndroidPreview {
        AddressesScreen(viewModel = AddressesViewModelPreview.preview())
    }
}