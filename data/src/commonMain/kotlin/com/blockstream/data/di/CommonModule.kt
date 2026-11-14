package com.blockstream.data.di

import com.blockstream.data.dataModule
import com.blockstream.data.usecases.CheckRecoveryPhraseUseCase
import com.blockstream.data.usecases.SetBiometricsUseCase
import com.blockstream.data.usecases.SetPinUseCase
import com.blockstream.data.utils.WatchOnlyDetector
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

// At some point we'll move this to domain module.
val commonModule = module {
    includes(dataModule)
    singleOf(::WatchOnlyDetector)
    singleOf(::CheckRecoveryPhraseUseCase)
    singleOf(::SetBiometricsUseCase)
    singleOf(::SetPinUseCase)
}
