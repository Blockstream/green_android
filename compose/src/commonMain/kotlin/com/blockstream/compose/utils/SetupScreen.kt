package com.blockstream.compose.utils

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.blockstream.common.models.GreenViewModel
import com.blockstream.common.sideeffects.SideEffect
import com.blockstream.compose.components.AppBar
import com.blockstream.compose.components.OnProgressStyle
import com.blockstream.compose.components.ScreenContainer
import kotlinx.coroutines.CoroutineScope

@Composable
fun SetupScreen(
    viewModel: GreenViewModel,
    modifier: Modifier = Modifier,
    scrollable: Boolean = false,
    withPadding: Boolean = true,
    withInsets: Boolean = true,
    withBottomInsets: Boolean = true,
    withBottomNavBarPadding: Boolean = false,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    onProgressStyle: OnProgressStyle = OnProgressStyle.Top,
    sideEffectsHandler: suspend CoroutineScope.(sideEffect: SideEffect) -> Unit = {},
    content: @Composable (ColumnScope.(innerPadding: PaddingValues) -> Unit)? = null
) {

    AppBar(viewModel)

    HandleSideEffect(viewModel, handler = sideEffectsHandler)

    content?.also {
        ScreenContainer(
            viewModel = viewModel,
            modifier = modifier,
            scrollable = scrollable,
            withPadding = withPadding,
            withInsets = withInsets,
            withBottomInsets = withBottomInsets,
            withBottomNavBarPadding = withBottomNavBarPadding,
            onProgressStyle = onProgressStyle,
            verticalArrangement = verticalArrangement,
            horizontalAlignment = horizontalAlignment,
            content = it
        )
    }
}