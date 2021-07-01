package com.blockstream.green.ui.onboarding

import com.blockstream.green.TestViewModel
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.*
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
class WalletNameViewModelUnitTests : TestViewModel<WalletNameViewModel>(){

    @Before
    fun setup(){
        viewModel = WalletNameViewModel(mock(), mock(), mock(), mock())
    }

    @Test
    fun test_initial_values(){
        val name = viewModel.getName()
        assertNull(name)
    }

    @Test
    fun test_wallet_name(){
        val name = "wallet_name"
        viewModel.walletName.value = name
        assertEquals(name, viewModel.getName())
    }
}