package com.blockstream.compose.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.interop.LocalUIViewController

@Composable
actual fun GreenChrome(isLight: Boolean) {
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

}