package com.blockstream.compose

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.ui.Modifier
import androidx.compose.runtime.CompositionLocalProvider
import androidx.core.view.WindowCompat
import androidx.fragment.app.FragmentActivity
import com.blockstream.compose.theme.GreenTheme
import com.blockstream.compose.utils.compatTestTagsAsResourceId

class GreenActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            GreenTheme {
                CompositionLocalProvider(LocalActivity provides this) {
                    GreenApp(modifier = Modifier.compatTestTagsAsResourceId())
                }
            }
        }
    }
}