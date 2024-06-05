package com.blockstream.compose.theme

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.interop.LocalUIViewController

@Composable
actual fun GreenTheme(content: @Composable () -> Unit) {
    val controller = LocalUIViewController.current


    SideEffect {
//        GreenColors.background.toArgb().also {
//            navigationController?.navigationBar?.barTintColor = UIColor.greenColor
//            navigationController?.navigationBar?.backgroundColor = UIColor.redColor
//        }
    }

//    SideEffect {
//        val window = (view.context as Activity).window
//
//        GreenColors.background.toArgb().also {
//            window.navigationBarColor = it
//            window.statusBarColor = it
//        }
//
//        WindowCompat.getInsetsController(window, view).also {
//            it.isAppearanceLightStatusBars = false
//            it.isAppearanceLightNavigationBars = false
//        }
//    }


    MaterialTheme(
        colorScheme = GreenColors,
        shapes = GreenShapes,
        typography = GreenTypography()
    ){
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
            content = content
        )
    }
}