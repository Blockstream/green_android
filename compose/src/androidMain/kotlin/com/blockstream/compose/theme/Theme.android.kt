package com.blockstream.compose.theme

import android.app.Activity
import android.content.res.Configuration
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.view.WindowCompat
import com.blockstream.compose.GreenPreview

@Composable
actual fun GreenChrome(isLight: Boolean) {
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window

            (if(isLight) GreenColorsLight else GreenColors).background.toArgb().also {
                window.navigationBarColor = it
                window.statusBarColor = it
            }

//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
//                window.navigationBarDividerColor = 0
//            }

            WindowCompat.getInsetsController(window, view).also {
                it.isAppearanceLightStatusBars = false
                it.isAppearanceLightNavigationBars = false
            }
        }
    }
}


@Composable
@Preview(showSystemUi = true, showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_NO)
fun GreenThemePreview() {
    GreenPreview {
        // A surface container using the 'background' color from the theme
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Card {
                Text("Android")
            }
        }
    }
}
