package com.blockstream.green.di

import com.blockstream.compose.models.MainViewModel
import com.blockstream.compose.models.SimpleGreenViewModel
import com.blockstream.compose.models.about.AboutViewModel
import com.blockstream.compose.models.add.Account2of3ViewModel
import com.blockstream.compose.models.add.ChooseAccountTypeViewModel
import com.blockstream.compose.models.add.ReviewAddAccountViewModel
import com.blockstream.compose.models.add.XpubViewModel
import com.blockstream.compose.models.addresses.AddressesViewModel
import com.blockstream.compose.models.addresses.SignMessageViewModel
import com.blockstream.compose.models.archived.ArchivedAccountsViewModel
import com.blockstream.compose.models.camera.CameraViewModel
import com.blockstream.compose.models.demo.DemoViewModel
import com.blockstream.compose.models.devices.DeviceInfoViewModel
import com.blockstream.compose.models.devices.DeviceListViewModel
import com.blockstream.compose.models.devices.DeviceScanViewModel
import com.blockstream.compose.models.devices.ImportPubKeyViewModel
import com.blockstream.compose.models.devices.JadeGenuineCheckViewModel
import com.blockstream.compose.models.devices.JadeGuideViewModel
import com.blockstream.compose.models.exchange.AccountExchangeViewModel
import com.blockstream.compose.models.exchange.OnOffRampsViewModel
import com.blockstream.compose.models.home.HomeViewModel
import com.blockstream.compose.models.jade.JadeQRViewModel
import com.blockstream.compose.models.lightning.LnUrlAuthViewModel
import com.blockstream.compose.models.lightning.LnUrlWithdrawViewModel
import com.blockstream.compose.models.lightning.RecoverFundsViewModel
import com.blockstream.compose.models.login.Bip39PassphraseViewModel
import com.blockstream.compose.models.login.LoginViewModel
import com.blockstream.compose.models.onboarding.SetupNewWalletViewModel
import com.blockstream.compose.models.onboarding.phone.EnterRecoveryPhraseViewModel
import com.blockstream.compose.models.onboarding.phone.PinViewModel
import com.blockstream.compose.models.onboarding.watchonly.WatchOnlyMultisigViewModel
import com.blockstream.compose.models.onboarding.watchonly.WatchOnlySinglesigViewModel
import com.blockstream.compose.models.overview.AccountOverviewViewModel
import com.blockstream.compose.models.overview.SecurityViewModel
import com.blockstream.compose.models.overview.TransactViewModel
import com.blockstream.compose.models.overview.WalletAssetsViewModel
import com.blockstream.compose.models.overview.WalletOverviewViewModel
import com.blockstream.compose.models.promo.PromoViewModel
import com.blockstream.compose.models.receive.ReceiveViewModel
import com.blockstream.compose.models.receive.RequestAmountViewModel
import com.blockstream.compose.models.recovery.RecoveryCheckViewModel
import com.blockstream.compose.models.recovery.RecoveryIntroViewModel
import com.blockstream.compose.models.recovery.RecoveryPhraseViewModel
import com.blockstream.compose.models.recovery.RecoveryWordsViewModel
import com.blockstream.compose.models.send.BumpViewModel
import com.blockstream.compose.models.send.DenominationViewModel
import com.blockstream.compose.models.send.FeeViewModel
import com.blockstream.compose.models.send.RedepositViewModel
import com.blockstream.compose.models.send.SendConfirmViewModel
import com.blockstream.compose.models.send.SendViewModel
import com.blockstream.compose.models.send.SweepViewModel
import com.blockstream.compose.models.settings.AppSettingsViewModel
import com.blockstream.compose.models.settings.DenominationExchangeRateViewModel
import com.blockstream.compose.models.settings.TwoFactorAuthenticationViewModel
import com.blockstream.compose.models.settings.TwoFactorSetupViewModel
import com.blockstream.compose.models.settings.WalletSettingsViewModel
import com.blockstream.compose.models.settings.WatchOnlyCredentialsSettingsViewModel
import com.blockstream.compose.models.settings.WatchOnlyViewModel
import com.blockstream.compose.models.sheets.AnalyticsViewModel
import com.blockstream.compose.models.sheets.AssetDetailsViewModel
import com.blockstream.compose.models.sheets.JadeFirmwareUpdateViewModel
import com.blockstream.compose.models.sheets.LightningNodeViewModel
import com.blockstream.compose.models.sheets.MeldCountriesViewModel
import com.blockstream.compose.models.sheets.NoteViewModel
import com.blockstream.compose.models.sheets.RecoveryHelpViewModel
import com.blockstream.compose.models.sheets.TransactionDetailsViewModel
import com.blockstream.compose.models.support.SupportViewModel
import com.blockstream.compose.models.transaction.TransactionViewModel
import com.blockstream.compose.models.twofactor.ReEnable2FAViewModel
import com.blockstream.compose.models.wallet.WalletDeleteViewModel
import com.blockstream.compose.models.wallet.WalletNameViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val viewModels = module {
    viewModelOf(::MainViewModel)
    viewModelOf(::AboutViewModel)
    viewModelOf(::DemoViewModel)
    viewModelOf(::SetupNewWalletViewModel)
    viewModelOf(::AppSettingsViewModel)
    viewModelOf(::RecoveryIntroViewModel)
    viewModelOf(::WalletOverviewViewModel)
    viewModelOf(::WatchOnlySinglesigViewModel)
    viewModelOf(::WatchOnlyMultisigViewModel)
    viewModelOf(::HomeViewModel)
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
    viewModelOf(::JadeGuideViewModel)
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
    viewModelOf(::MeldCountriesViewModel)
    viewModelOf(::RedepositViewModel)
    viewModelOf(::ReEnable2FAViewModel)
    viewModelOf(::WatchOnlyCredentialsSettingsViewModel)
    viewModelOf(::OnOffRampsViewModel)
    viewModelOf(::DeviceListViewModel)
    viewModelOf(::DeviceInfoViewModel)
    viewModelOf(::DeviceScanViewModel)
    viewModelOf(::JadeFirmwareUpdateViewModel)
    viewModelOf(::ImportPubKeyViewModel)
    viewModelOf(::TransactViewModel)
    viewModelOf(::SecurityViewModel)
    viewModelOf(::FeeViewModel)
    viewModel {
        SupportViewModel(get(), get(), getOrNull())
    }
    viewModel {
        JadeGenuineCheckViewModel(getOrNull(), getOrNull())
    }
    viewModel {
        PromoViewModel(get(), getOrNull())
    }
    viewModel {
        AssetDetailsViewModel(get(), get(), getOrNull())
    }
    viewModel {
        SendConfirmViewModel(get(), get(), getOrNull())
    }
    viewModel {
        AccountExchangeViewModel(get(), getOrNull())
    }
    viewModel {
        SendViewModel(get(), get(), get(), get())
    }
    viewModel {
        SimpleGreenViewModel(getOrNull(), getOrNull(), getOrNull(), getOrNull())
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
        ChooseAccountTypeViewModel(get(), getOrNull(), getOrNull(), get())
    }
    viewModel {
        // https://github.com/InsertKoinIO/koin/issues/1352
        LoginViewModel(get(), getOrNull(), get(), get())
    }
    viewModel {
        // https://github.com/InsertKoinIO/koin/issues/1352
        RecoveryPhraseViewModel(get(), getOrNull(), getOrNull())
    }
}
