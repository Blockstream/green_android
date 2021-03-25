package com.blockstream.green.ui.onboarding

import androidx.annotation.LayoutRes
import androidx.annotation.MenuRes
import androidx.databinding.ViewDataBinding
import com.blockstream.green.ui.AppFragment
import com.blockstream.green.data.OnboardingOptions

abstract class AbstractOnboardingFragment<T : ViewDataBinding>(
    @LayoutRes layout: Int,
    @MenuRes menuRes: Int,
) : AppFragment<T>(layout, menuRes) {

    var options: OnboardingOptions? = null

}