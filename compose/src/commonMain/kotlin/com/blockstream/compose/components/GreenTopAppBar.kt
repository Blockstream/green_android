package com.blockstream.compose.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.constraintlayout.compose.ConstraintLayout
import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.arrow_left
import blockstream_green.common.generated.resources.dots_three_vertical_bold
import blockstream_green.common.generated.resources.list
import cafe.adriel.voyager.core.screen.Screen
import com.blockstream.compose.LocalAppBarState
import com.blockstream.compose.LocalRootNavigator
import com.blockstream.compose.theme.bodySmall
import com.blockstream.compose.theme.labelMedium
import com.blockstream.compose.theme.titleSmall
import org.jetbrains.compose.resources.painterResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GreenTopAppBar(
    openDrawer: () -> Unit = { },
    showDrawer: (Screen) -> Boolean = { false }
) {
    val navigator = LocalRootNavigator.current
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())

    val showDrawerNavigationIcon =
        navigator?.lastItemOrNull?.let { showDrawer(it) }
            ?: false

    val navData by LocalAppBarState.current.data

    // CenterAlignedTopAppBar if you want center aligned
    TopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background,
            titleContentColor = MaterialTheme.colorScheme.onPrimary,
        ),
        title = {
            AnimatedVisibility(
                visible = navData.isVisible,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                ConstraintLayout {
                    val (title, subtitle) = createRefs()

                    Crossfade(targetState = navData.title ?: "", modifier = Modifier.constrainAs(title) {
                        start.linkTo(parent.start)
                        // end.linkTo(parent.end)
                        top.linkTo(parent.top)
                        bottom.linkTo(parent.bottom)
                    }) {
                        Text(
                            text = it,
                            maxLines = 1,
                            style = titleSmall,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Crossfade(targetState = navData.subtitle ?: "", modifier = Modifier.constrainAs(subtitle) {
                        start.linkTo(parent.start)
                        // end.linkTo(parent.end)
                        top.linkTo(title.bottom)
                    }) {
                        Text(
                            text = it,
                            maxLines = 1,
                            style = bodySmall,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        },
        navigationIcon = {
            AnimatedVisibility(
                visible = navData.isVisible,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                AnimatedContent(targetState = showDrawerNavigationIcon, transitionSpec = {
                    slideIntoContainer(
                        AnimatedContentTransitionScope.SlideDirection.Right,
                        animationSpec = tween(400)
                    ).togetherWith(
                        slideOutOfContainer(
                            AnimatedContentTransitionScope.SlideDirection.Left,
                            animationSpec = tween(200)
                        )
                    )
                }, label = "NavigationIcon") { showDrawerNavigationIcon ->
                    if (showDrawerNavigationIcon) {
                        IconButton(onClick = {
                            openDrawer()
                        }) {
                            Icon(
                                painter = painterResource(Res.drawable.list),
                                contentDescription = "Drawer Menu"
                            )
                        }
                    } else {
                        IconButton(enabled = navData.isVisible, onClick = {
                            if (navData.isVisible && navData.onBackPressed()) {
                                navigator?.pop()
                            }
                        }) {
                            Icon(
                                painter = painterResource(Res.drawable.arrow_left),
                                contentDescription = "Back"
                            )
                        }
                    }
                }
            }
        },
        actions = {
            val popupState = remember { PopupState() }

            AnimatedVisibility(
                visible = navData.isVisible,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Row {
                    navData.actions.filter { !it.isMenuEntry }.forEach {
                        if (it.icon == null) {
                            TextButton(
                                onClick = it.onClick,
                                modifier = Modifier.align(Alignment.CenterVertically)
                            ) {
                                Text(text = it.title, style = labelMedium)
                            }
                        } else {
                            IconButton(onClick = {
                                it.onClick()
                            }) {
                                it.icon?.also {
                                    Image(
                                        painter = painterResource(it),
                                        contentDescription = null
                                    )
                                }
                            }
                        }
                    }

                    navData.actions.filter { it.isMenuEntry }.takeIf { it.isNotEmpty() }?.map {
                        MenuEntry.from(it)
                    }?.also {
                        IconButton(onClick = {
                            popupState.isContextMenuVisible.value = true
                        }) {
                            Icon(
                                painter = painterResource(Res.drawable.dots_three_vertical_bold),
                                contentDescription = "More Menu"
                            )
                        }

                        PopupMenu(state = popupState, entries = it)
                    }
                }
            }
        },
        scrollBehavior = scrollBehavior,
    )
}