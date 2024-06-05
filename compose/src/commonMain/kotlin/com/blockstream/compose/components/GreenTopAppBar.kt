package com.blockstream.compose.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.constraintlayout.compose.ConstraintLayout
import cafe.adriel.voyager.core.screen.Screen
import com.blockstream.compose.LocalAppBarState
import com.blockstream.compose.LocalRootNavigator
import com.blockstream.compose.theme.bodySmall
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


    CenterAlignedTopAppBar(
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

                    Text(
                        text = navData.title ?: "",
                        maxLines = 1,
                        style = titleSmall,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.constrainAs(title) {
                            start.linkTo(parent.start)
                            end.linkTo(parent.end)
                            top.linkTo(parent.top)
                            bottom.linkTo(parent.bottom)
                        }
                    )

                    navData.subtitle?.also {
                        Text(
                            text = it,
                            maxLines = 1,
                            style = bodySmall,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.constrainAs(subtitle) {
                                start.linkTo(parent.start)
                                end.linkTo(parent.end)
                                top.linkTo(title.bottom)
                            }
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
                                imageVector = Icons.Filled.Menu,
                                contentDescription = "Drawer Menu"
                            )
                        }
                    } else {
                        IconButton(enabled = navData.isVisible, onClick = {
                            if(navData.isVisible){
                                navigator?.pop()
                            }
                        }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
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

                    navData.actions.filter { it.isMenuEntry }.takeIf { it.isNotEmpty() }?.map {
                        MenuEntry.from(it)
                    }?.also {
                        IconButton(onClick = {
                            popupState.isContextMenuVisible.value = true
                        }) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
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