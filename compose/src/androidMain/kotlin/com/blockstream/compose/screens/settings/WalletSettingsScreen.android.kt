package com.blockstream.compose.screens.settings

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.blockstream.common.models.settings.WalletSettingsViewModelPreview
import com.blockstream.compose.GreenAndroidPreview
import com.blockstream.compose.R
import com.blockstream.compose.components.GreenColumn
import com.blockstream.compose.theme.GreenThemePreview
import com.blockstream.compose.theme.titleMedium

@Composable
@Preview
fun SettingPreview() {
    GreenThemePreview {
        GreenColumn {
            Text(
                text = "General",
                style = titleMedium,
                modifier = Modifier.padding(top = 8.dp)
            )
            Setting(
                title = "Logout",
                subtitle = "Wallet",
                painter = painterResource(id = R.drawable.sign_out)
            )
            Setting(title = "Watch-only")
            Setting(title = "Change PIN")
            Setting(
                title = "Login with Biometrics",
                subtitle = "Biometrics Login is Enabled",
                checked = true
            )
            Setting(
                title = "Login with Biometrics",
                checked = true
            )
        }
    }
}


@Composable
@Preview
fun WalletSettingsScreenPreview() {
    GreenAndroidPreview {
        WalletSettingsScreen(viewModel = WalletSettingsViewModelPreview.preview())
    }
}

@Composable
@Preview
fun RecoveryTransactionsScreenPreview() {
    GreenAndroidPreview {
        WalletSettingsScreen(viewModel = WalletSettingsViewModelPreview.previewRecovery())
    }
}
