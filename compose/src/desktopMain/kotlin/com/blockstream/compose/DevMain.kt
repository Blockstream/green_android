package com.blockstream.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.singleWindowApplication
import com.blockstream.common.models.MainViewModel
import com.blockstream.compose.theme.GreenChrome
import com.blockstream.compose.theme.GreenTheme
import com.blockstream.compose.utils.compatTestTagsAsResourceId

fun main() {
    singleWindowApplication(
        title = "Compose",
        state = WindowState(width = 800.dp, height = 800.dp),
        alwaysOnTop = true
    ) {
//        DevelopmentEntryPoint {
//            MainPage()
//        }
    }
}

@Composable
fun MainPage() {
    val mainViewModel = remember { MainViewModel() }

    GreenChrome()
    GreenTheme {
        GreenApp(mainViewModel = mainViewModel, modifier = Modifier.compatTestTagsAsResourceId())
    }
}