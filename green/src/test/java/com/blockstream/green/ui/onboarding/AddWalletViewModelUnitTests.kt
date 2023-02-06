package com.blockstream.green.ui.onboarding

import androidx.lifecycle.Observer
import com.blockstream.green.TestViewModel
import com.blockstream.green.database.WalletRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.*

@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class AddWalletViewModelUnitTests : TestViewModel<AddWalletViewModel>(){

    private lateinit var termsObserver : Observer<Boolean>

    private fun init(walletExists: Boolean = false) = runBlockingTest {
        termsObserver = mock()

        val walletRepository : WalletRepository = mock()
        whenever(walletRepository.walletsExists()).thenReturn(walletExists)

        // whenever(walletRepository.getWalletsSuspend()).thenReturn(if(walletExists) listOf(mock()) else emptyList())

        viewModel = AddWalletViewModel(mock(), mock(), walletRepository, mock(),null)
        viewModel.termsChecked.observeForever(termsObserver)
    }

    @Test
    fun test_initial_value() {
        init()

        verify(termsObserver, atLeastOnce()).onChanged(eq(false))
        verify(termsObserver, never()).onChanged(eq(true))
    }

    @Test
    fun test_when_checked() {
        init()

        viewModel.termsChecked.value = true
        verify(termsObserver).onChanged(eq(true))
    }

    @Test
    fun test_when_toggled() {
        init()
        // One extra termsChecked event comes from VM init where we set the value to true/false
        // if you have already accepted the terms
        viewModel.termsChecked.value = true
        viewModel.termsChecked.value = false
        verify(termsObserver).onChanged(eq(true))
        verify(termsObserver, times(2)).onChanged(eq(false))
    }

    @Test
    fun whenWalletExists_termsShouldBeChecked() = runTest {
        init(true)

        verify(termsObserver).onChanged(eq(true))
    }
}