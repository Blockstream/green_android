package com.blockstream.green.di

import com.blockstream.common.models.MainViewModel
import com.blockstream.common.models.SimpleGreenViewModel
import com.blockstream.common.models.about.AboutViewModel
import com.blockstream.common.models.add.Account2of3ViewModel
import com.blockstream.common.models.add.ChooseAccountTypeViewModel
import com.blockstream.common.models.add.ReviewAddAccountViewModel
import com.blockstream.common.models.add.XpubViewModel
import com.blockstream.common.models.addresses.AddressesViewModel
import com.blockstream.common.models.addresses.SignMessageViewModel
import com.blockstream.common.models.archived.ArchivedAccountsViewModel
import com.blockstream.common.models.camera.CameraViewModel
import com.blockstream.common.models.demo.DemoViewModel
import com.blockstream.common.models.devices.DeviceInfoViewModel
import com.blockstream.common.models.devices.DeviceListViewModel
import com.blockstream.common.models.devices.DeviceScanViewModel
import com.blockstream.common.models.devices.ImportPubKeyViewModel
import com.blockstream.common.models.devices.JadeGuideViewModel
import com.blockstream.common.models.drawer.DrawerViewModel
import com.blockstream.common.models.exchange.AccountExchangeViewModel
import com.blockstream.common.models.exchange.OnOffRampsViewModel
import com.blockstream.common.models.home.HomeViewModel
import com.blockstream.common.models.jade.JadeQRViewModel
import com.blockstream.common.models.lightning.LnUrlAuthViewModel
import com.blockstream.common.models.lightning.LnUrlWithdrawViewModel
import com.blockstream.common.models.lightning.RecoverFundsViewModel
import com.blockstream.common.models.login.Bip39PassphraseViewModel
import com.blockstream.common.models.login.LoginViewModel
import com.blockstream.common.models.onboarding.SetupNewWalletViewModel
import com.blockstream.common.models.onboarding.hardware.UseHardwareDeviceViewModel
import com.blockstream.common.models.onboarding.phone.AddWalletViewModel
import com.blockstream.common.models.onboarding.phone.EnterRecoveryPhraseViewModel
import com.blockstream.common.models.onboarding.phone.PinViewModel
import com.blockstream.common.models.onboarding.watchonly.WatchOnlyCredentialsViewModel
import com.blockstream.common.models.onboarding.watchonly.WatchOnlyNetworkViewModel
import com.blockstream.common.models.onboarding.watchonly.WatchOnlyPolicyViewModel
import com.blockstream.common.models.overview.AccountOverviewViewModel
import com.blockstream.common.models.overview.WalletAssetsViewModel
import com.blockstream.common.models.overview.WalletOverviewViewModel
import com.blockstream.common.models.promo.PromoViewModel
import com.blockstream.common.models.receive.ReceiveViewModel
import com.blockstream.common.models.receive.RequestAmountViewModel
import com.blockstream.common.models.recovery.RecoveryCheckViewModel
import com.blockstream.common.models.recovery.RecoveryIntroViewModel
import com.blockstream.common.models.recovery.RecoveryPhraseViewModel
import com.blockstream.common.models.recovery.RecoveryWordsViewModel
import com.blockstream.common.models.send.BumpViewModel
import com.blockstream.common.models.send.DenominationViewModel
import com.blockstream.common.models.send.FeeViewModel
import com.blockstream.common.models.send.RedepositViewModel
import com.blockstream.common.models.send.SendConfirmViewModel
import com.blockstream.common.models.send.SendViewModel
import com.blockstream.common.models.send.SweepViewModel
import com.blockstream.common.models.settings.AppSettingsViewModel
import com.blockstream.common.models.settings.DenominationExchangeRateViewModel
import com.blockstream.common.models.settings.TwoFactorAuthenticationViewModel
import com.blockstream.common.models.settings.TwoFactorSetupViewModel
import com.blockstream.common.models.settings.WalletSettingsViewModel
import com.blockstream.common.models.settings.WatchOnlyCredentialsSettingsViewModel
import com.blockstream.common.models.settings.WatchOnlyViewModel
import com.blockstream.common.models.sheets.AnalyticsViewModel
import com.blockstream.common.models.sheets.AssetDetailsViewModel
import com.blockstream.common.models.sheets.JadeFirmwareUpdateViewModel
import com.blockstream.common.models.sheets.LightningNodeViewModel
import com.blockstream.common.models.sheets.NoteViewModel
import com.blockstream.common.models.sheets.RecoveryHelpViewModel
import com.blockstream.common.models.sheets.TransactionDetailsViewModel
import com.blockstream.common.models.transaction.TransactionViewModel
import com.blockstream.common.models.twofactor.ReEnable2FAViewModel
import com.blockstream.common.models.wallet.WalletDeleteViewModel
import com.blockstream.common.models.wallet.WalletNameViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val viewModels = module {
    viewModelOf(::MainViewModel)
    viewModelOf(::AboutViewModel)
    viewModelOf(::PromoViewModel)
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
    viewModelOf(::RecoveryWordsViewModel)
    viewModelOf(::RecoveryCheckViewModel)
    viewModelOf(::LnUrlAuthViewModel)
    viewModelOf(::LnUrlWithdrawViewModel)
    viewModelOf(::WatchOnlyViewModel)
    viewModelOf(::XpubViewModel)
    viewModelOf(::TransactionViewModel)
    viewModelOf(::TransactionDetailsViewModel)
    viewModelOf(::NoteViewModel)
    viewModelOf(::ReviewAddAccountViewModel)
    viewModelOf(::Account2of3ViewModel)
    viewModelOf(::TwoFactorAuthenticationViewModel)
    viewModelOf(::WalletAssetsViewModel)
    viewModelOf(::ArchivedAccountsViewModel)
    viewModelOf(::UseHardwareDeviceViewModel)
    viewModelOf(::JadeGuideViewModel)
    viewModelOf(::WatchOnlyNetworkViewModel)
    viewModelOf(::AddressesViewModel)
    viewModelOf(::SignMessageViewModel)
    viewModelOf(::WalletNameViewModel)
    viewModelOf(::WalletDeleteViewModel)
    viewModelOf(::AnalyticsViewModel)
    viewModelOf(::Bip39PassphraseViewModel)
    viewModelOf(::ReceiveViewModel)
    viewModelOf(::RecoveryHelpViewModel)
    viewModelOf(::AccountOverviewViewModel)
    viewModelOf(::DenominationExchangeRateViewModel)
    viewModelOf(::RequestAmountViewModel)
    viewModelOf(::TwoFactorSetupViewModel)
    viewModelOf(::DenominationViewModel)
    viewModelOf(::BumpViewModel)
    viewModelOf(::LightningNodeViewModel)
    viewModelOf(::RedepositViewModel)
    viewModelOf(::ReEnable2FAViewModel)
    viewModelOf(::WatchOnlyCredentialsSettingsViewModel)
    viewModelOf(::OnOffRampsViewModel)
    viewModelOf(::DeviceListViewModel)
    viewModelOf(::DeviceInfoViewModel)
    viewModelOf(::DeviceScanViewModel)
    viewModelOf(::JadeFirmwareUpdateViewModel)
    viewModelOf(::ImportPubKeyViewModel)
    viewModel {
        AssetDetailsViewModel(get(), get(), getOrNull())
    }
    viewModel {
        FeeViewModel(get(), getOrNull(), get())
    }
    viewModel {
        SendConfirmViewModel(get(), get(), getOrNull())
    }
    viewModel {
        AccountExchangeViewModel(get(), getOrNull())
    }
    viewModel {
        SendViewModel(get(), getOrNull(), getOrNull())
    }
    viewModel {
        SimpleGreenViewModel(getOrNull(), getOrNull(), getOrNull())
    }
    viewModel {
        SweepViewModel(get(), getOrNull(), getOrNull())
    }
    viewModel {
        WalletSettingsViewModel(get(), get(), getOrNull())
    }
    viewModel {
        JadeQRViewModel(getOrNull(), get(), get())
    }
    viewModel {
        CameraViewModel(get(), getOrNull(), getOrNull())
    }
    viewModel {
        // https://github.com/InsertKoinIO/koin/issues/1352
        RecoverFundsViewModel(get(), get(), getOrNull(), get())
    }
    viewModel {
        // https://github.com/InsertKoinIO/koin/issues/1352
        ChooseAccountTypeViewModel(get(), getOrNull(), getOrNull())
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