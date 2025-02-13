package com.blockstream.ui.navigation.bottomsheet


import androidx.compose.material3.SheetState
import androidx.compose.material3.SheetValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigation.FloatingWindow
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.Navigator
import androidx.navigation.get


/**
 * The state of a [ModalBottomSheetLayout] that the [BottomSheetNavigator] drives
 *
 * @param sheetState The sheet state that is driven by the [BottomSheetNavigator]
 */
public class BottomSheetNavigatorSheetState(private val sheetState: SheetState) {
    /**
     * @see SheetState.isVisible
     */
    public val isVisible: Boolean
        get() = sheetState.isVisible

    /**
     * @see SheetState.currentValue
     */
    public val currentValue: SheetValue
        get() = sheetState.currentValue

    /**
     * @see SheetState.targetValue
     */
    public val targetValue: SheetValue
        get() = sheetState.targetValue
}

/**
 * Create and remember a [BottomSheetNavigator]
 */
@Composable
public fun rememberBottomSheetNavigator(): BottomSheetNavigator {
    return remember { BottomSheetNavigator() }
}

/**
 * [NavDestination] specific to [BottomSheetNavigator]
 */
public class Destination(
    navigator: BottomSheetNavigator,
    internal val content: @Composable (NavBackStackEntry) -> Unit
) : NavDestination(navigator), FloatingWindow

@Composable
fun NavController.onDismissRequest(onDismiss: () -> Unit = {}): () -> Unit {
    val bottomSheetNavigator = navigatorProvider[BottomSheetNavigator::class]
    return {
        onDismiss.invoke()
        bottomSheetNavigator.onDismissRequest()
    }
}

expect class BottomSheetNavigator(): Navigator<Destination> {
    internal var sheetEnabled:  Boolean
        private set
    internal val sheetInitializer: @Composable () -> Unit
    internal var sheetContent: @Composable () -> Unit
    internal var onDismissRequest: () -> Unit

    fun setSheetState(sheetState: SheetState)
}