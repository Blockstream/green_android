package com.blockstream.ui.navigation.bottomsheet

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