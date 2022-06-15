@file:OptIn(ExperimentalCoroutinesApi::class)

package com.blockstream.green

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.ViewModel
import com.blockstream.green.data.Countly
import com.blockstream.green.gdk.GreenSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.mockito.Mock

open class TestViewModel<VM : ViewModel> {
    internal lateinit var viewModel : VM


    @get:Rule
    val taskExecutorRule = InstantTaskExecutorRule()

    @get:Rule
    val schedulersRule = ImmediateSchedulersRule()

    @Mock
    protected lateinit var greenSession: GreenSession

    @Mock
    protected lateinit var countly: Countly

    private val testDispatcher = UnconfinedTestDispatcher()


    @Before
    open fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }
}