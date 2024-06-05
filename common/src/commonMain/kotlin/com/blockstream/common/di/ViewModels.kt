package com.blockstream.common.di

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
import com.blockstream.common.models.devices.JadeGuideViewModel
import com.blockstream.common.models.drawer.DrawerViewModel
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
import com.blockstream.common.models.receive.ReceiveViewModel
import com.blockstream.common.models.receive.RequestAmountViewModel
import com.blockstream.common.models.recovery.RecoveryCheckViewModel
import com.blockstream.common.models.recovery.RecoveryIntroViewModel
import com.blockstream.common.models.recovery.RecoveryPhraseViewModel
import com.blockstream.common.models.recovery.RecoveryWordsViewModel
import com.blockstream.common.models.send.AccountExchangeViewModel
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
import com.blockstream.common.models.sheets.LightningNodeViewModel
import com.blockstream.common.models.sheets.NoteViewModel
import com.blockstream.common.models.sheets.RecoveryHelpViewModel
import com.blockstream.common.models.sheets.TransactionDetailsViewModel
import com.blockstream.common.models.transaction.TransactionViewModel
import com.blockstream.common.models.twofactor.ReEnable2FAViewModel
import com.blockstream.common.models.wallet.WalletDeleteViewModel
import com.blockstream.common.models.wallet.WalletNameViewModel
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module

val factoryViewModels = module {
    factoryOf(::AboutViewModel)
    factoryOf(::DemoViewModel)
    factoryOf(::SetupNewWalletViewModel)
    factoryOf(::AddWalletViewModel)
    factoryOf(::AppSettingsViewModel)
    factoryOf(::RecoveryIntroViewModel)
    factoryOf(::WalletOverviewViewModel)
    factoryOf(::WatchOnlyPolicyViewModel)
    factoryOf(::WatchOnlyCredentialsViewModel)
    factoryOf(::HomeViewModel)
    factoryOf(::DrawerViewModel)
    factoryOf(::EnterRecoveryPhraseViewModel)
    factoryOf(::PinViewModel)
    factoryOf(::RecoveryWordsViewModel)
    factoryOf(::RecoveryCheckViewModel)
    factoryOf(::LnUrlAuthViewModel)
    factoryOf(::LnUrlWithdrawViewModel)
    factoryOf(::WatchOnlyViewModel)
    factoryOf(::XpubViewModel)
    factoryOf(::TransactionViewModel)
    factoryOf(::TransactionDetailsViewModel)
    factoryOf(::NoteViewModel)
    factoryOf(::ReviewAddAccountViewModel)
    factoryOf(::Account2of3ViewModel)
    factoryOf(::TwoFactorAuthenticationViewModel)
    factoryOf(::WalletAssetsViewModel)
    factoryOf(::ArchivedAccountsViewModel)
    factoryOf(::UseHardwareDeviceViewModel)
    factoryOf(::JadeGuideViewModel)
    factoryOf(::WatchOnlyNetworkViewModel)
    factoryOf(::AddressesViewModel)
    factoryOf(::SignMessageViewModel)
    factoryOf(::WalletNameViewModel)
    factoryOf(::WalletDeleteViewModel)
    factoryOf(::AnalyticsViewModel)
    factoryOf(::Bip39PassphraseViewModel)
    factoryOf(::ReceiveViewModel)
    factoryOf(::RecoveryHelpViewModel)
    factoryOf(::AccountOverviewViewModel)
    factoryOf(::DenominationExchangeRateViewModel)
    factoryOf(::RequestAmountViewModel)
    factoryOf(::TwoFactorSetupViewModel)
    factoryOf(::DenominationViewModel)
    factoryOf(::BumpViewModel)
    factoryOf(::LightningNodeViewModel)
    factoryOf(::RedepositViewModel)
    factoryOf(::ReEnable2FAViewModel)
    factoryOf(::WatchOnlyCredentialsSettingsViewModel)
    factory {
        AssetDetailsViewModel(get(), get(), getOrNull())
    }
    factory {
        FeeViewModel(get(), getOrNull(), getOrNull(), get())
    }
    factory {
        SendConfirmViewModel(get(), get(), getOrNull())
    }
    factory {
        AccountExchangeViewModel(get(), getOrNull())
    }
    factory {
        SendViewModel(get(), getOrNull(), getOrNull())
    }
    factory {
        SimpleGreenViewModel(getOrNull(), getOrNull(), getOrNull())
    }
    factory {
        SweepViewModel(get(), getOrNull(), getOrNull())
    }
    factory {
        WalletSettingsViewModel(get(), get(), getOrNull())
    }
    factory {
        JadeQRViewModel(get(), getOrNull())
    }
    factory {
        CameraViewModel(get(), getOrNull(), getOrNull())
    }
    factory {
        // https://github.com/InsertKoinIO/koin/issues/1352
        RecoverFundsViewModel(get(), get(), getOrNull(), get())
    }
    factory {
        // https://github.com/InsertKoinIO/koin/issues/1352
        ChooseAccountTypeViewModel(get(), getOrNull(), get())
    }
    factory {
        // https://github.com/InsertKoinIO/koin/issues/1352
        LoginViewModel(get(), get(), getOrNull(), get())
    }
    factory {
        // https://github.com/InsertKoinIO/koin/issues/1352
        RecoveryPhraseViewModel(get(), getOrNull(), getOrNull())
    }
}