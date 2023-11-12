package com.blockstream.green.di

import com.blockstream.common.models.about.AboutViewModel
import com.blockstream.common.models.add.Account2of3ViewModel
import com.blockstream.common.models.add.ChooseAccountTypeViewModel
import com.blockstream.common.models.add.ExportLightningKeyViewModel
import com.blockstream.common.models.add.ReviewAddAccountViewModel
import com.blockstream.common.models.add.XpubViewModel
import com.blockstream.common.models.addresses.AddressesViewModel
import com.blockstream.common.models.addresses.SignMessageViewModel
import com.blockstream.common.models.archived.ArchivedAccountsViewModel
import com.blockstream.common.models.camera.CameraViewModel
import com.blockstream.common.models.demo.DemoViewModel
import com.blockstream.common.models.devices.JadeGuideViewModel
import com.blockstream.common.models.drawer.DrawerViewModel
import com.blockstream.common.models.home.HomeViewModel
import com.blockstream.common.models.lightning.LnUrlAuthViewModel
import com.blockstream.common.models.lightning.LnUrlWithdrawViewModel
import com.blockstream.common.models.lightning.RecoverFundsViewModel
import com.blockstream.common.models.login.Bip39PassphraseViewModel
import com.blockstream.common.models.login.LoginViewModel
import com.blockstream.common.models.onboarding.AddWalletViewModel
import com.blockstream.common.models.onboarding.ChooseNetworkViewModel
import com.blockstream.common.models.onboarding.EnterRecoveryPhraseViewModel
import com.blockstream.common.models.onboarding.PinViewModel
import com.blockstream.common.models.onboarding.SetupNewWalletViewModel
import com.blockstream.common.models.onboarding.UseHardwareDeviceViewModel
import com.blockstream.common.models.onboarding.WatchOnlyCredentialsViewModel
import com.blockstream.common.models.onboarding.WatchOnlyPolicyViewModel
import com.blockstream.common.models.overview.AssetsViewModel
import com.blockstream.common.models.overview.WalletOverviewViewModel
import com.blockstream.common.models.recovery.RecoveryCheckViewModel
import com.blockstream.common.models.recovery.RecoveryIntroViewModel
import com.blockstream.common.models.recovery.RecoveryPhraseViewModel
import com.blockstream.common.models.recovery.RecoveryWordsViewModel
import com.blockstream.common.models.settings.AppSettingsViewModel
import com.blockstream.common.models.settings.TwoFactorAuthenticationViewModel
import com.blockstream.common.models.settings.WatchOnlyViewModel
import com.blockstream.common.models.sheets.AnalyticsViewModel
import com.blockstream.common.models.transaction.TransactionDetailsViewModel
import com.blockstream.common.models.wallet.WalletDeleteViewModel
import com.blockstream.common.models.wallet.WalletNameViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.androidx.viewmodel.dsl.viewModelOf
import org.koin.dsl.module

val viewModels = module {
    viewModelOf(::AboutViewModel)
    viewModelOf(::DemoViewModel)
    viewModelOf(::SetupNewWalletViewModel)
    viewModelOf(::AddWalletViewModel)
    viewModelOf(::AppSettingsViewModel)
    viewModelOf(::RecoveryIntroViewModel)
    viewModelOf(::WalletOverviewViewModel)
    viewModelOf(::WatchOnlyPolicyViewModel)
    viewModelOf(::WatchOnlyCredentialsViewModel)
    viewModelOf(::HomeViewModel)
    viewModelOf(::DrawerViewModel)
    viewModelOf(::EnterRecoveryPhraseViewModel)
    viewModelOf(::PinViewModel)
    viewModelOf(::RecoveryIntroViewModel)
    viewModelOf(::RecoveryWordsViewModel)
    viewModelOf(::RecoveryCheckViewModel)
    viewModelOf(::LnUrlAuthViewModel)
    viewModelOf(::LnUrlWithdrawViewModel)
    viewModelOf(::WatchOnlyViewModel)
    viewModelOf(::ExportLightningKeyViewModel)
    viewModelOf(::XpubViewModel)
    viewModelOf(::TransactionDetailsViewModel)
    viewModelOf(::ReviewAddAccountViewModel)
    viewModelOf(::Account2of3ViewModel)
    viewModelOf(::TwoFactorAuthenticationViewModel)
    viewModelOf(::AssetsViewModel)
    viewModelOf(::ArchivedAccountsViewModel)
    viewModelOf(::UseHardwareDeviceViewModel)
    viewModelOf(::JadeGuideViewModel)
    viewModelOf(::ChooseNetworkViewModel)
    viewModelOf(::AddressesViewModel)
    viewModelOf(::SignMessageViewModel)
    viewModelOf(::WalletNameViewModel)
    viewModelOf(::WalletDeleteViewModel)
    viewModelOf(::AnalyticsViewModel)
    viewModelOf(::CameraViewModel)
    viewModelOf(::Bip39PassphraseViewModel)
    viewModel {
        // https://github.com/InsertKoinIO/koin/issues/1352
        RecoverFundsViewModel(get(), get(), getOrNull(), get())
    }
    viewModel {
        // https://github.com/InsertKoinIO/koin/issues/1352
        ChooseAccountTypeViewModel(get(), getOrNull())
    }
    viewModel {
        // https://github.com/InsertKoinIO/koin/issues/1352
        LoginViewModel(get(), get(), getOrNull(), get())
    }
    viewModel {
        // https://github.com/InsertKoinIO/koin/issues/1352
        RecoveryPhraseViewModel(get(), getOrNull(), getOrNull())
    }
}