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
        FeeViewModel(get(), getOrNull(), get())
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
