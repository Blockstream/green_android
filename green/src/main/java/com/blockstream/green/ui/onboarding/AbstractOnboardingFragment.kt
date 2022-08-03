package com.blockstream.green.ui.onboarding

import androidx.annotation.LayoutRes
import androidx.annotation.MenuRes
import androidx.databinding.ViewDataBinding
import com.blockstream.green.data.OnboardingOptions
import com.blockstream.green.ui.AppFragment

abstract class AbstractOnboardingFragment<T : ViewDataBinding>(
    @LayoutRes layout: Int,
    @MenuRes menuRes: Int,
) : AppFragment<T>(layout, menuRes) {

    var options: OnboardingOptions? = null

    override val segmentation get() = options?.let { countly.onBoardingSegmentation(it) }
}