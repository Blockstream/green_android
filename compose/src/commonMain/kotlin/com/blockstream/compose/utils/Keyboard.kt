package com.blockstream.compose.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import kotlinx.coroutines.delay

@Composable
fun OpenKeyboard(focusRequester: FocusRequester) {
    val showKeyboard by remember { mutableStateOf(true) }
    val keyboard = LocalSoftwareKeyboardController.current

    // LaunchedEffect prevents endless focus request
    LaunchedEffect(focusRequester) {
        if (showKeyboard) {
            focusRequester.requestFocus()
            delay(200) // Make sure you have delay here
            keyboard?.show()
        }
    }
}