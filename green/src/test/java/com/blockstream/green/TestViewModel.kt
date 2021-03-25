package com.blockstream.green

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.ViewModel
import org.junit.Rule

open class TestViewModel<VM : ViewModel> {
    internal lateinit var viewModel : VM

    @get:Rule
    val taskExecutorRule = InstantTaskExecutorRule()

    @get:Rule
    val schedulersRule = ImmediateSchedulersRule()
}