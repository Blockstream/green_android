package com.blockstream.green.ui.onboarding

import com.blockstream.green.TestViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.mock

@RunWith(MockitoJUnitRunner::class)
class WalletNameViewModelUnitTests : TestViewModel<WalletNameViewModel>(){

    @Before
    override fun setup(){
        super.setup()

        viewModel = WalletNameViewModel(mock(), mock(), mock(), mock(), mock())
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