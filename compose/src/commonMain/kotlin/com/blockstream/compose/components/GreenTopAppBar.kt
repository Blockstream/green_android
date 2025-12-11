package com.blockstream.compose.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import com.adamglin.PhosphorIcons
import com.adamglin.phosphoricons.Regular
import com.adamglin.phosphoricons.regular.Wallet
import com.blockstream.common.extensions.isNotBlank
import com.blockstream.common.navigation.NavigateDestinations
import com.blockstream.compose.theme.bodyLarge
import com.blockstream.compose.theme.bodySmall
import com.blockstream.compose.theme.titleMedium
import com.blockstream.compose.navigation.LocalNavigator
import com.blockstream.ui.navigation.NavData
import com.blockstream.compose.utils.ifTrue
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GreenTopAppBar(
    hasBackStack: Boolean = false,
    scrollBehavior: TopAppBarScrollBehavior? = null,
    navData: NavData,
    modifier: Modifier = Modifier,
    goBack: () -> Unit = { }
) {
    val navigator = LocalNavigator.current

    // CenterAlignedTopAppBar if you want center aligned
    TopAppBar(
        modifier = modifier,
        scrollBehavior = scrollBehavior,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Transparent,
            scrolledContainerColor = Color.Transparent,
            titleContentColor = Color.White,
        ),
        navigationIcon = {
            AnimatedVisibility(
                visible = navData.isVisible && navData.showNavigationIcon,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                val targetState = when {
                    navData.showNavigationIcon == false -> null
                    hasBackStack && navData.walletName == null -> true
                    else -> null
                }
                AnimatedContent(
                    targetState = targetState,
                    transitionSpec = {
                        slideIntoContainer(
                            AnimatedContentTransitionScope.SlideDirection.End,
                            animationSpec = tween(400)
                        ).togetherWith(
                            slideOutOfContainer(
                                AnimatedContentTransitionScope.SlideDirection.Start,
                                animationSpec = tween(200)
                            )
                        )
                    }, label = "NavigationIcon"
                ) { targetState ->
                    when (targetState) {
                        true -> IconButton(onClick = {
                            navData.onBackClicked?.also {
                                it.invoke()
                            } ?: run {
                                goBack()
                            }
                        }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back"
                            )
                        }

                        else -> {

                        }
                    }
                }
            }
        },
        title = {
            AnimatedVisibility(
                visible = navData.isVisible,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                ConstraintLayout {
                    val (title, subtitle) = createRefs()

                    Crossfade(
                        targetState = navData.title ?: navData.titleRes?.let { stringResource(it) }
                        ?: "",
                        modifier = Modifier.constrainAs(title) {
                            start.linkTo(parent.start)
                            // end.linkTo(parent.end)
                            top.linkTo(parent.top)
                            bottom.linkTo(parent.bottom)
                        }) {
                        Text(
                            text = it,
                            maxLines = 1,
                            style = titleMedium,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // Looking good
//                    Text(
//                        text = navData.title ?: navData.titleRes?.let { stringResource(it) } ?: "",
//                        maxLines = 1,
//                        style = titleMedium,
//                        overflow = TextOverflow.Ellipsis,
//                        modifier = Modifier.fillMaxWidth().constrainAs(title) {
//                            start.linkTo(parent.start)
//                            // end.linkTo(parent.end)
//                            top.linkTo(parent.top)
//                            bottom.linkTo(parent.bottom)
//                        }
//                    )

                    Crossfade(
                        targetState = navData.subtitle ?: "",
                        modifier = Modifier.constrainAs(subtitle) {
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
        actions = {
            if (navData.isVisible && navData.showNavigationIcon && navData.walletName != null) {

                Row(modifier = Modifier.padding(end = 8.dp)) {
                    TextButton(
                        onClick = {
                            navigator.navigate(route = NavigateDestinations.Home)
                        },
                        contentPadding = PaddingValues(),
                    ) {
                        Text(
                            text = navData.walletName ?: "",
                            style = bodyLarge,
                            modifier = Modifier.ifTrue(navData.title.isNotBlank() || navData.titleRes != null) {
                                it.widthIn(max = 180.dp)
                            },
                            overflow = TextOverflow.Ellipsis,
                            maxLines = 1
                        )
                    }

                    IconButton(onClick = {
                        navigator.navigate(route = NavigateDestinations.Home)
                    }) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .background(
                                    color = MaterialTheme.colorScheme.primary,
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = PhosphorIcons.Regular.Wallet,
                                contentDescription = "Drawer Menu",
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            ActionMenu(navData = navData)
        }
    )
}