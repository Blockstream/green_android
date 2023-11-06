package com.blockstream.compose.extensions

import androidx.compose.runtime.MutableState

fun MutableState<Boolean>.toggle(){
    this.value = !this.value
}