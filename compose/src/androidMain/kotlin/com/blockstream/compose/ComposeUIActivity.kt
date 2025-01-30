package com.blockstream.compose

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.blockstream.common.looks.wallet.WalletListLook
import com.blockstream.compose.components.GreenButton
import com.blockstream.compose.components.GreenButtonColor
import com.blockstream.compose.components.GreenButtonType
import com.blockstream.ui.components.GreenColumn
import com.blockstream.compose.theme.GreenChrome
import com.blockstream.compose.theme.GreenTheme
import com.blockstream.compose.views.WalletListItem

class ComposeUIActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            GreenChrome()
            GreenTheme {
                ComposeUI()
            }
        }
    }
}

@Composable
fun ComposeUI() {
    GreenColumn(space = 8, modifier = Modifier.verticalScroll(rememberScrollState())) {
        Text("Typography", style = MaterialTheme.typography.titleSmall)

        HorizontalDivider()

        GreenColumn(space = 0, padding = 0) {

            GreenColumn(space = 0, padding = 0) {
                Text(text = "Display Large", style = MaterialTheme.typography.displayLarge)
                Text(text = "Display Medium", style = MaterialTheme.typography.displayMedium)
                Text(text = "Display Small", style = MaterialTheme.typography.displaySmall)
            }

            GreenColumn(space = 0, padding = 0) {
                Text(text = "Headline Large", style = MaterialTheme.typography.headlineLarge)
                Text(text = "Headline Medium", style = MaterialTheme.typography.headlineMedium)
                Text(text = "Headline Small", style = MaterialTheme.typography.headlineSmall)
            }

            GreenColumn(space = 0, padding = 0) {
                Text(text = "Title Large", style = MaterialTheme.typography.titleLarge)
                Text(text = "Title Medium", style = MaterialTheme.typography.titleMedium)
                Text(text = "Title Small", style = MaterialTheme.typography.titleSmall)
            }

            GreenColumn(space = 0, padding = 0) {
                Text(text = "Body Large", style = MaterialTheme.typography.bodyLarge)
                Text(text = "Body Medium", style = MaterialTheme.typography.bodyMedium)
                Text(text = "Body Small", style = MaterialTheme.typography.bodySmall)
            }

            GreenColumn(space = 0, padding = 0) {
                Text(text = "Label Large", style = MaterialTheme.typography.labelLarge)
                Text(text = "Label Medium", style = MaterialTheme.typography.labelMedium)
                Text(text = "Label Small", style = MaterialTheme.typography.labelSmall)
            }

        }

        Text("WalletListItem", style = MaterialTheme.typography.titleSmall)

        HorizontalDivider()

        GreenColumn(padding = 0, space = 4) {
            WalletListItem(look = WalletListLook.preview(hasLightningShortcut = true))

            WalletListItem(look = WalletListLook.preview(false))
            WalletListItem(look = WalletListLook.preview(true, false))

            WalletListItem(look = WalletListLook.preview(true, true))
            WalletListItem(look = WalletListLook.preview(false, true))
        }

        HorizontalDivider()

        GreenColumn {
            GreenButton("Button") { }
            GreenButton("Button", type = GreenButtonType.OUTLINE) { }
            GreenButton("Button", type = GreenButtonType.OUTLINE, color = GreenButtonColor.GREENER) { }
            GreenButton("Button", type = GreenButtonType.TEXT) { }
        }

        CircularProgressIndicator()
    }
}

@Preview(showBackground = true)
@Composable
fun ComposeUIPreview() {
    GreenTheme {
        ComposeUI()
    }
}