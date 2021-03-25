package com.blockstream.green.ui.onboarding

import androidx.lifecycle.Observer
import com.blockstream.green.TestData
import com.blockstream.green.TestViewModel
import com.blockstream.gdk.GreenWallet
import com.blockstream.green.views.RecoveryPhraseKeyboardView
import com.nhaarman.mockitokotlin2.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
class EnterRecoveryPhraseViewModelUnitTests : TestViewModel<EnterRecoveryPhraseViewModel>(){

    @Mock
    private lateinit var showPasteButtonObserver : Observer<Boolean>
    @Mock
    private lateinit var showHelpButtonObserver: Observer<Boolean>
    @Mock
    private lateinit var isValidObserver: Observer<Boolean>
    @Mock
    private lateinit var greenWallet: GreenWallet

    @Before
    fun setup(){

        TestData.recoveryPhrases.forEach {
            whenever(greenWallet.isMnemonicValid(it)).thenReturn(true)
        }

        viewModel = EnterRecoveryPhraseViewModel(greenWallet, null,false)

        viewModel.showPasteButton.observeForever(showPasteButtonObserver)
        viewModel.showHelpButton.observeForever(showHelpButtonObserver)
        viewModel.isValid.observeForever(isValidObserver)

    }

    @Test
    fun test_initial_value(){
        verify(showPasteButtonObserver).onChanged(eq(true))
        verify(showHelpButtonObserver, never()).onChanged(eq(true))
        verify(isValidObserver, never()).onChanged(eq(true))
    }

    @Test
    fun test_valid_recovery_phrases(){
        verify(isValidObserver).onChanged(eq(false))


        for(phrase in TestData.recoveryPhrases){
            viewModel.updateRecoveryPhrase(
                RecoveryPhraseKeyboardView.RecoveryPhraseState.fromString(
                    phrase
                )
            )
        }

        // isBip39MnemonicValid called
        verify(greenWallet, times(TestData.recoveryPhrases.size)).isMnemonicValid(any())

        verify(isValidObserver, times(TestData.recoveryPhrases.size)).onChanged(eq(true))
    }

    @Test
    fun test_invalid_recovery_phrases(){
        for(phrase in TestData.recoveryPhrasesInvalid){
            viewModel.updateRecoveryPhrase(
                RecoveryPhraseKeyboardView.RecoveryPhraseState.fromString(
                    phrase
                )
            )
        }

        verify(isValidObserver, atLeast(TestData.recoveryPhrases.size)).onChanged(eq(false))
    }

    @Test
    fun pasteButton_shouldBe_visible(){
        verify(showPasteButtonObserver).onChanged(eq(true))
    }

    @Test
    fun pasteButton_shouldBe_invisible(){
        viewModel.updateRecoveryPhrase(
            RecoveryPhraseKeyboardView.RecoveryPhraseState.fromString("random input")
        )
        verify(showPasteButtonObserver).onChanged(eq(false))
    }

    @Test
    fun helpButton_shouldBe_invisible(){
        verify(showHelpButtonObserver).onChanged(eq(false))
    }

    @Test
    fun helpButton_shouldBe_visible(){
        viewModel.updateRecoveryPhrase(
            RecoveryPhraseKeyboardView.RecoveryPhraseState.fromString(TestData.RECOVERY_PHRASE_24.split(" ").joinToString(" "))
        )
        verify(showPasteButtonObserver).onChanged(eq(true))
    }

}