@file:OptIn(ExperimentalComposeUiApi::class)

package com.blockstream.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.currentBackStackEntryAsState
import com.rickclephas.kmp.nativecoroutines.NativeCoroutinesState
import kotlinx.coroutines.flow.StateFlow
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource


data class NavData(
    val title: String? = null,
    val titleRes: StringResource? = null,

    val subtitle: String? = null,

    val walletName: String? = null,

    val isVisible: Boolean = true,
    val backHandlerEnabled: Boolean = false,

    val showBadge: Boolean = false,
    val showBottomNavigation: Boolean = false,

    val showNavigationIcon: Boolean = true,
    val actions: List<NavAction> = listOf(),
    // val actionsMenu: List<RegularActionItem> = emptyList(),
)

data class NavAction(
    val title: String? = null,
    val titleRes: StringResource? = null,
    val icon: DrawableResource? = null,
    val imageVector: ImageVector? = null,
    val isMenuEntry: Boolean = false,
    val onClick: () -> Unit = { }
)

@Stable
class NavDataState(navData: NavData = NavData()) {
    var data = mutableStateOf(navData)
        private set

    fun update(data: NavData) {
        this.data.value = data
    }
}

val LocalNavData = compositionLocalOf { NavDataState() }

interface INavData {
    @NativeCoroutinesState
    val navData: StateFlow<NavData>

    fun navigateBack()
}

@Composable
fun AppBarState(viewModel: INavData) {
    val navigator = LocalNavigator.current
    val navDataState = LocalNavData.current
    val selfBackStackEntry = LocalNavBackStackEntry.current

    val navData by viewModel.navData.collectAsStateWithLifecycle()
    val currentBackStackEntry by navigator.currentBackStackEntryAsState()

    val key = navData.hashCode() + (currentBackStackEntry?.id?.hashCode() ?: 0)

    LaunchedEffect(key) {
        if(currentBackStackEntry?.id == selfBackStackEntry?.id){
            navDataState.update(navData)
        }
    }

    BackHandler(enabled = !navData.isVisible || navData.backHandlerEnabled) {
        viewModel.navigateBack()
    }
}