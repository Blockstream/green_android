package com.blockstream.compose.extensions

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.TextFieldValue
import kotlinx.coroutines.flow.MutableStateFlow

fun MutableState<Boolean>.toggle() {
    this.value = !this.value
}

//fun MutableStateFlow<String>.onValueChange(): (String) -> Unit = { newValue ->
//    this.value = newValue
//}

fun <T> MutableStateFlow<T>.onValueChange(): (T) -> Unit = { newValue ->
    this.value = newValue
}

@Composable
fun MutableStateFlow<String>.onTextFieldValueChange(updateTextFieldValue: (textFieldValue: TextFieldValue) -> Unit): (TextFieldValue) -> Unit =
    { newValue ->
        updateTextFieldValue.invoke(newValue)
        this.value = newValue.text
    }


fun Color.Companion.random() = Color(
    kotlin.random.Random.nextInt(256),
    kotlin.random.Random.nextInt(256),
    kotlin.random.Random.nextInt(256)
)