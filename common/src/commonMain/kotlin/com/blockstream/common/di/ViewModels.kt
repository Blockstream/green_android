package com.blockstream.common.di

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
import com.blockstream.common.models.devices.*
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
import com.blockstream.common.models.onboarding.phone.EnterRecoveryPhraseViewModel
import com.blockstream.common.models.onboarding.phone.PinViewModel
import com.blockstream.common.models.onboarding.watchonly.WatchOnlyMultisigViewModel
import com.blockstream.common.models.onboarding.watchonly.WatchOnlySinglesigViewModel
import com.blockstream.common.models.overview.*
import com.blockstream.common.models.promo.PromoViewModel
import com.blockstream.common.models.receive.ReceiveViewModel
import com.blockstream.common.models.receive.RequestAmountViewModel
import com.blockstream.common.models.recovery.RecoveryCheckViewModel
import com.blockstream.common.models.recovery.RecoveryIntroViewModel
import com.blockstream.common.models.recovery.RecoveryPhraseViewModel
import com.blockstream.common.models.recovery.RecoveryWordsViewModel
import com.blockstream.common.models.send.*
import com.blockstream.common.models.settings.*
import com.blockstream.common.models.sheets.*
import com.blockstream.common.models.support.SupportViewModel
import com.blockstream.common.models.transaction.TransactionViewModel
import com.blockstream.common.models.twofactor.ReEnable2FAViewModel
import com.blockstream.common.models.wallet.WalletDeleteViewModel
import com.blockstream.common.models.wallet.WalletNameViewModel
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module

val factoryViewModels = module {
    factoryOf(::MainViewModel)
    factoryOf(::AboutViewModel)
    factoryOf(::DemoViewModel)
    factoryOf(::SetupNewWalletViewModel)
    factoryOf(::AppSettingsViewModel)
    factoryOf(::RecoveryIntroViewModel)
    factoryOf(::WalletOverviewViewModel)
    factoryOf(::WatchOnlySinglesigViewModel)
    factoryOf(::WatchOnlyMultisigViewModel)
    factoryOf(::HomeViewModel)
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
    factoryOf(::JadeGuideViewModel)
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
    factoryOf(::OnOffRampsViewModel)
    factoryOf(::ImportPubKeyViewModel)
    factoryOf(::DeviceListViewModel)
    factoryOf(::DeviceInfoViewModel)
    factoryOf(::DeviceScanViewModel)
    factoryOf(::JadeFirmwareUpdateViewModel)
    factoryOf(::TransactViewModel)
    factoryOf(::SecurityViewModel)
    factory {
        SupportViewModel(get(), get(), getOrNull())
    }
    factory {
        JadeGenuineCheckViewModel(getOrNull(), getOrNull())
    }
    factory {
        PromoViewModel(get(), getOrNull())
    }
    factory {
        AssetDetailsViewModel(get(), get(), getOrNull())
    }
    factory {
        FeeViewModel(get(), getOrNull(), get())
    }
    factory {
        SendConfirmViewModel(get(), get(), getOrNull())
    }
    factory {
        AccountExchangeViewModel(get(), getOrNull())
    }
    factory {
        SendViewModel(get(), get(), get(), get())
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
        JadeQRViewModel(getOrNull(), get(), get())
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
        ChooseAccountTypeViewModel(get(), getOrNull(), getOrNull(), get())
    }
    factory {
        // https://github.com/InsertKoinIO/koin/issues/1352
        LoginViewModel(get(), getOrNull(), get(), get())
    }
    factory {
        // https://github.com/InsertKoinIO/koin/issues/1352
        RecoveryPhraseViewModel(get(), getOrNull(), getOrNull())
    }
}
