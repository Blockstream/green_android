package com.blockstream.green.di

import com.blockstream.common.models.about.AboutViewModel
import com.blockstream.common.models.demo.DemoViewModel
import com.blockstream.common.models.drawer.DrawerViewModel
import com.blockstream.common.models.home.HomeViewModel
import com.blockstream.common.models.login.LoginViewModel
import com.blockstream.common.models.onboarding.AddWalletViewModel
import com.blockstream.common.models.onboarding.EnterRecoveryPhraseViewModel
import com.blockstream.common.models.onboarding.PinViewModel
import com.blockstream.common.models.onboarding.SetupNewWalletViewModel
import com.blockstream.common.models.onboarding.WatchOnlyCredentialsViewModel
import com.blockstream.common.models.onboarding.WatchOnlyPolicyViewModel
import com.blockstream.common.models.overview.WalletOverviewViewModel
import com.blockstream.common.models.recovery.RecoveryCheckViewModel
import com.blockstream.common.models.recovery.RecoveryIntroViewModel
import com.blockstream.common.models.recovery.RecoveryPhraseViewModel
import com.blockstream.common.models.recovery.RecoveryWordsViewModel
import com.blockstream.common.models.settings.AppSettingsViewModel
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
    viewModel {
        // https://github.com/InsertKoinIO/koin/issues/1352
        LoginViewModel(get(), get(), get(), getOrNull())
    }
    viewModel {
        // https://github.com/InsertKoinIO/koin/issues/1352
        RecoveryPhraseViewModel(get(), getOrNull(), getOrNull())
    }
}