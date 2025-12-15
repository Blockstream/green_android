package com.blockstream.compose.extensions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.launchIn

fun <T> Flow<T>.launchIn(viewModel: ViewModel) = launchIn(viewModel.viewModelScope)
