@file:OptIn(ExperimentalComposeUiApi::class)

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Menu
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.currentBackStackEntryAsState
import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.dots_three_vertical_bold
import com.blockstream.common.data.LocalNavData
import com.blockstream.common.data.NavData
import com.blockstream.common.events.Events
import com.blockstream.common.models.GreenViewModel
import com.blockstream.compose.navigation.LocalNavBackStackEntry
import com.blockstream.compose.navigation.LocalNavigator
import com.blockstream.compose.theme.bodySmall
import com.blockstream.compose.theme.labelMedium
import com.blockstream.compose.theme.titleSmall
import org.jetbrains.compose.resources.painterResource

@Composable
fun AppBar(viewModel: GreenViewModel) {
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
        viewModel.postEvent(Events.NavigateBackUserAction)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GreenTopAppBar(
    showDrawerNavigationIcon: Boolean = true,
    navData: NavData,
    modifier: Modifier = Modifier,
    goBack: () -> Unit = { },
    openDrawer: () -> Unit = { },
) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())

    // CenterAlignedTopAppBar if you want center aligned
    TopAppBar(
        modifier = modifier,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Transparent,
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
                visible = navData.isVisible && navData.showNavigationIcon,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                AnimatedContent(targetState = showDrawerNavigationIcon, transitionSpec = {
                    slideIntoContainer(
                        AnimatedContentTransitionScope.SlideDirection.End,
                        animationSpec = tween(400)
                    ).togetherWith(
                        slideOutOfContainer(
                            AnimatedContentTransitionScope.SlideDirection.Start,
                            animationSpec = tween(200)
                        )
                    )
                }, label = "NavigationIcon") { showDrawerNavigationIcon ->
                    if (showDrawerNavigationIcon) {
                        IconButton(onClick = { openDrawer() }) {
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = "Drawer Menu"
                            )
                        }
                    } else {
                        IconButton(onClick = {
                            goBack()
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
//            ActionMenu(
//                items = navData.actionsMenu,
//                colors = ActionMenuDefaults.colors(contentColor = MaterialTheme.colorScheme.onPrimary)
//            )

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