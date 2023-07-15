package com.blockstream.green.ui.onboarding

import androidx.annotation.LayoutRes
import androidx.annotation.MenuRes
import androidx.databinding.ViewDataBinding
import com.blockstream.common.data.SetupArgs
import com.blockstream.green.ui.AppFragment

abstract class AbstractOnboardingFragment<T : ViewDataBinding>(
    @LayoutRes layout: Int,
    @MenuRes menuRes: Int,
) : AppFragment<T>(layout, menuRes) {

    var setupArgs: SetupArgs? = null

    override val segmentation get() = setupArgs?.let { countly.onBoardingSegmentation(it) }
}