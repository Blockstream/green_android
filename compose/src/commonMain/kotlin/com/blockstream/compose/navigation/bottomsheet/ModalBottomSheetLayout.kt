package com.blockstream.compose.navigation.bottomsheet

import androidx.compose.runtime.Composable

@Composable
fun ModalBottomSheetLayout(
    bottomSheetNavigator: BottomSheetNavigator,
) {
    bottomSheetNavigator.sheetInitializer()

    if (bottomSheetNavigator.sheetEnabled) {
        bottomSheetNavigator.sheetContent()
    }
}