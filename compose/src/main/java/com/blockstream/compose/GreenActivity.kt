package com.blockstream.compose

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.fragment.app.FragmentActivity
import com.blockstream.compose.theme.GreenTheme
import org.koin.core.annotation.KoinExperimentalAPI

class GreenActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            GreenTheme {
                GreenApp()
            }
        }
    }
}