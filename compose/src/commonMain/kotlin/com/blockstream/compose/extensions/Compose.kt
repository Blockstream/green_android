package com.blockstream.compose.extensions

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.IntSize
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


fun Offset.toMenuDpOffset(container: IntSize = IntSize.Zero, density: Density): DpOffset =
    DpOffset(x = x.pxToDp(density), y = -(container.height - y).pxToDp(density))

@Composable
fun Dp.dpToPx() = with(LocalDensity.current) { this@dpToPx.toPx() }

@Composable
fun Int.pxToDp() = with(LocalDensity.current) { this@pxToDp.toDp() }

@Composable
fun Float.pxToDp() = with(LocalDensity.current) { this@pxToDp.toDp() }

fun Float.pxToDp(density: Density) = with(density) { this@pxToDp.toDp() }