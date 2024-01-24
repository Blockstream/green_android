package com.blockstream.compose.extensions

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.Dp
import kotlinx.coroutines.flow.MutableStateFlow

fun MutableState<Boolean>.toggle() {
    this.value = !this.value
}

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

@Composable
fun Dp.dpToPx() = with(LocalDensity.current) { this@dpToPx.toPx() }


@Composable
fun Int.pxToDp() = with(LocalDensity.current) { this@pxToDp.toDp() }