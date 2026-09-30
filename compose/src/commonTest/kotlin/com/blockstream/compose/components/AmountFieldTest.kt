package com.blockstream.compose.components

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import com.blockstream.compose.utils.StringHolder
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AmountFieldTest {
    private fun state(feedback: AmountFieldFeedback?) = AmountFieldState(
        value = "1",
        denominationLabel = "BTC",
        feedback = feedback,
    )

    @Test
    fun externalValueSyncMovesSelectionWhenTextLengthChanges() {
        val current = TextFieldValue(text = "12", selection = TextRange(1))

        val synced = syncAmountTextFieldValue(current, externalValue = "1234")

        assertEquals("1234", synced.text)
        assertEquals(TextRange(4), synced.selection)
    }

    @Test
    fun externalValueSyncPreservesSelectionWhenTextLengthIsStable() {
        val current = TextFieldValue(text = "12", selection = TextRange(1))

        val synced = syncAmountTextFieldValue(current, externalValue = "34")

        assertEquals(TextRange(1), synced.selection)
    }

    @Test
    fun onlyEditableModeAllowsInputAndActions() {
        assertTrue(AmountFieldInteraction.Editable.allowsTextInput())
        assertTrue(AmountFieldInteraction.Editable.allowsFieldAction())
        assertFalse(AmountFieldInteraction.Locked.allowsTextInput())
        assertFalse(AmountFieldInteraction.Disabled.allowsFieldAction())
        assertFalse(AmountFieldInteraction.ReadOnly.allowsTextInput())
    }

    @Test
    fun feedbackWithoutTextOrContentIsNotVisible() {
        val feedback = AmountFieldFeedback(severity = AmountFieldFeedbackSeverity.Info)

        assertNull(state(feedback).visibleFeedback(hasFeedbackContent = false))
    }

    @Test
    fun feedbackIsVisibleWithEitherTextOrCustomContent() {
        val withText = AmountFieldFeedback(
            severity = AmountFieldFeedbackSeverity.Error,
            text = StringHolder.create("too low"),
        )
        val withoutText = AmountFieldFeedback(severity = AmountFieldFeedbackSeverity.Info)

        assertNotNull(state(withText).visibleFeedback(hasFeedbackContent = false))
        assertNotNull(state(withoutText).visibleFeedback(hasFeedbackContent = true))
    }
}
