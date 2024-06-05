package com.blockstream.compose.sheets

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import cafe.adriel.voyager.core.lifecycle.JavaSerializable
import cafe.adriel.voyager.core.screen.Screen

@OptIn(ExperimentalMaterial3Api::class)
abstract class BottomScreen : Screen, JavaSerializable {

    var sheetState: SheetState? = null
        private set

    @Composable
    fun sheetState(skipPartiallyExpanded: Boolean = false): SheetState {
        return rememberModalBottomSheetState(skipPartiallyExpanded = skipPartiallyExpanded).also {
            sheetState = it
        }
    }

    @Composable
    fun onDismissRequest(onDismiss: () -> Unit = {}): () -> Unit {
        val bottomSheetNavigator = LocalBottomSheetNavigatorM3.current
        return {
            bottomSheetNavigator?.hide()
            onDismiss.invoke()
        }
    }
}