package com.blockstream.green.ui.onboarding

import androidx.lifecycle.Observer
import com.blockstream.green.TestViewModel
import com.nhaarman.mockitokotlin2.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
class AddWalletViewModelUnitTests : TestViewModel<AddWalletViewModel>(){

    @Mock
    private lateinit var termsObserver : Observer<Boolean>


    @Before
    fun setup(){
        viewModel = AddWalletViewModel(mock(), mock(), 0)
        viewModel.termsChecked.observeForever(termsObserver)
    }

    @Test
    fun test_initial_value(){
        verify(termsObserver).onChanged(eq(false))
        verify(termsObserver, never()).onChanged(eq(true))
    }

    @Test
    fun test_when_checked(){
        viewModel.termsChecked.value = true
        verify(termsObserver).onChanged(eq(true))
    }

    @Test
    fun test_when_toggled(){
        viewModel.termsChecked.value = true
        viewModel.termsChecked.value = false
        verify(termsObserver).onChanged(eq(true))
        verify(termsObserver, times(2)).onChanged(eq(false))
    }
}