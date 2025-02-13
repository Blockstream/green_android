package com.blockstream.ui.navigation.bottomsheet


import androidx.compose.runtime.Composable
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDeepLink
import androidx.navigation.NavDestinationBuilder
import androidx.navigation.NavDestinationDsl
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.get
import kotlin.jvm.JvmSuppressWildcards
import kotlin.reflect.KClass
import kotlin.reflect.KType

inline fun <reified T : Any> NavGraphBuilder.bottomSheet(
    deepLinks: List<NavDeepLink> = emptyList(),
    typeMap: Map<KType, @JvmSuppressWildcards NavType<*>> = emptyMap(),
    noinline content: @Composable (backstackEntry: NavBackStackEntry) -> Unit
) {
    destination(
        BottomSheetNavigatorDestinationBuilder(
            provider[BottomSheetNavigator::class],
            T::class,
            typeMap,
            content
        )
            .apply {
                deepLinks.forEach { deepLink -> deepLink(deepLink) }
            }
    )
}

/** DSL for constructing a new [ComposeNavigator.Destination] */
@NavDestinationDsl
class BottomSheetNavigatorDestinationBuilder :
    NavDestinationBuilder<Destination> {

    private val composeNavigator: BottomSheetNavigator
    private val content: @Composable (NavBackStackEntry) -> Unit

    constructor(
        navigator: BottomSheetNavigator,
        route: String,
        content: @Composable (NavBackStackEntry) -> Unit
    ) : super(navigator, route) {
        this.composeNavigator = navigator
        this.content = content
    }

    constructor(
        navigator: BottomSheetNavigator,
        route: KClass<*>,
        typeMap: Map<KType, @JvmSuppressWildcards NavType<*>>,
        content: @Composable (NavBackStackEntry) -> Unit
    ) : super(navigator, route, typeMap) {
        this.composeNavigator = navigator
        this.content = content
    }

    override fun instantiateDestination(): Destination {
        return Destination(composeNavigator, content)
    }

    override fun build(): Destination {
        return super.build()
    }
}