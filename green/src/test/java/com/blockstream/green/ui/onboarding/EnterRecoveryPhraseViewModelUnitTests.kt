package com.blockstream.green.ui.onboarding

import androidx.lifecycle.Observer
import com.blockstream.common.gdk.Wally
import com.blockstream.green.TestData
import com.blockstream.green.TestViewModel
import com.blockstream.green.views.RecoveryPhraseKeyboardView
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.*

@RunWith(MockitoJUnitRunner::class)
class EnterRecoveryPhraseViewModelUnitTests : TestViewModel<EnterRecoveryPhraseViewModel>(){

    @Mock
    private lateinit var showPasteButtonObserver : Observer<Boolean>
    @Mock
    private lateinit var showHelpButtonObserver: Observer<Boolean>
    @Mock
    private lateinit var isValidObserver: Observer<Boolean>
    @Mock
    private lateinit var wally: Wally

    @Before
    override fun setup(){
        super.setup()

        TestData.recoveryPhrases.forEach {
            whenever(wally.bip39MnemonicValidate(it)).thenReturn(true)
        }

        viewModel = EnterRecoveryPhraseViewModel(mock(), wally, null)

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
        verify(wally, times(TestData.recoveryPhrases.size)).bip39MnemonicValidate(any())

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