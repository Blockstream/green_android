package com.blockstream.green

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.blockstream.green.data.Countly
import com.blockstream.green.gdk.GdkSession
import com.blockstream.green.ui.AppViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.mockito.Mock

open class TestViewModel<VM : AppViewModel> {
    internal lateinit var viewModel : VM

    @get:Rule
    val taskExecutorRule = InstantTaskExecutorRule()

    @get:Rule
    val schedulersRule = ImmediateSchedulersRule()

    @Mock
    protected lateinit var gdkSession: GdkSession

    @Mock
    protected lateinit var countly: Countly

    protected val testDispatcher = UnconfinedTestDispatcher()
    @Before
    open fun setup() {
        Dispatchers.setMain(testDispatcher)
        AppViewModel.ioDispatcher = testDispatcher
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }
}