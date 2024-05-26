package com.blockstream.compose.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.blockstream.common.BTC_POLICY_ASSET
import com.blockstream.common.LBTC_POLICY_ASSET
import com.blockstream.common.extensions.previewAccountBalance
import com.blockstream.common.extensions.policyAndType
import com.blockstream.common.gdk.GdkSession
import com.blockstream.common.gdk.data.AccountBalance
import com.blockstream.compose.R
import com.blockstream.compose.extensions.assetIcon
import com.blockstream.compose.extensions.getAccountColor
import com.blockstream.compose.extensions.policyIcon
import com.blockstream.compose.theme.GreenThemePreview
import com.blockstream.compose.theme.bodySmall
import com.blockstream.compose.theme.md_theme_surfaceCircle
import com.blockstream.compose.theme.titleMedium
import com.blockstream.compose.theme.whiteHigh
import com.blockstream.compose.theme.whiteMedium
import com.blockstream.compose.utils.ifTrue
import com.blockstream.compose.utils.noRippleClickable
import com.blockstream.compose.utils.roundBackground
import com.blockstream.compose.utils.stringResourceId

@OptIn(ExperimentalFoundationApi::class, ExperimentalComposeUiApi::class)
@Composable
fun GreenAccountCard(
    modifier: Modifier = Modifier,
    account: AccountBalance,
    isExpanded: Boolean,
    session: GdkSession? = null,
    onCopyClick: (() -> Unit)? = null,
    onArrowClick: (() -> Unit)? = null,
    onWarningClick: (() -> Unit)? = null,
    onClick: () -> Unit = {},
    onLongClick: (offset: Offset) -> Unit = {},
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .then(modifier)
    ) {

        var offset = Offset.Zero

        Card(
//            onClick = onClick,
            colors = CardDefaults.cardColors(
                containerColor = account.account.getAccountColor(),
                contentColor = whiteHigh
            ),
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth()
                .pointerInteropFilter {
                    offset = Offset(it.x, it.y)
                    false
                }
                .combinedClickable(
                    onClick = {
                        onClick()
                    },
                    onLongClick = {
                        onLongClick(offset)
                    }
                )
        ) {

            Row {
                GreenColumn(space = 0, modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(id = account.account.policyIcon()),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )

                        Text(
                            text = stringResourceId(account.account.type.policyAndType()).uppercase(),
                            style = bodySmall,
                            color = whiteMedium
                        )

                        if (account.account.isLightning) {
                            Text(
                                stringResource(id = R.string.id_experimental),
                                style = bodySmall,
                                color = whiteMedium,
                                modifier = Modifier
                                    .padding(start = 4.dp)
                                    .roundBackground(
                                        horizontal = 6.dp,
                                        vertical = 2.dp,
                                        size = 4.dp,
                                        color = md_theme_surfaceCircle.copy(alpha = 0.2f)
                                    )
                            )
                        }
                    }

                    Text(
                        text = account.account.name,
                        style = titleMedium,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                AccountAssets(
                    accountBalance = account,
                    session = session,
                    modifier = Modifier
                        .padding(end = 16.dp)
                        .align(Alignment.CenterVertically)
                )
            }

            androidx.compose.animation.AnimatedVisibility(
                visible = isExpanded,
                enter = fadeIn() + expandVertically(
                    animationSpec = tween(
                        durationMillis = 150,
                        easing = FastOutSlowInEasing
                    )
                ),
                exit = fadeOut() + shrinkVertically(
                    animationSpec = tween(
                        durationMillis = 150,
                        easing = FastOutSlowInEasing
                    )
                ),
            ) {
                Box(
                    modifier = Modifier
                        .heightIn(min = 75.dp)
                ) {
                    GreenRow(
                        space = 8,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                    ) {

                        Box(modifier = Modifier
                            .weight(1f)
                            .ifTrue(
                                account.balance == null
                            ) {
                                align(Alignment.Bottom)
                            }) {
                            if (account.balance == null) {
                                CircularProgressIndicator(
                                    strokeWidth = 1.dp,
                                    modifier = Modifier
                                        .size(24.dp),
                                    color = whiteHigh
                                )
                            } else {
                                Column {
                                    Text(
                                        account.balanceExchange ?: "",
                                        style = bodySmall,
                                        color = whiteMedium
                                    )
                                    Text(account.balance ?: "", style = titleMedium)
                                }
                            }
                        }

                        if (onCopyClick != null) {
                            GreenButton(
                                modifier = Modifier.align(Alignment.Bottom),
                                text = stringResource(id = R.string.id_copy_id),
                                type = GreenButtonType.OUTLINE,
                                color = GreenButtonColor.WHITE,
                                size = GreenButtonSize.SMALL,
                                onClick = onCopyClick
                            )
                        } else if (onArrowClick != null) {
                            Card(
                                onClick = onArrowClick,
                                modifier = Modifier
                                    .align(Alignment.Bottom)
                                    .size(42.dp),
                                colors = CardDefaults.cardColors(
                                    contentColor = whiteHigh,
                                    containerColor = Color.Transparent
                                ),
                                border = BorderStroke(1.dp, whiteHigh)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .fillMaxHeight()
                                ) {
                                    Image(
                                        modifier = Modifier.align(Alignment.Center),
                                        painter = painterResource(id = R.drawable.arrow_right),
                                        contentDescription = ""
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        if(onWarningClick != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(top = 16.dp, start = 1.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.shield_warning),
                    contentDescription = null,
                    modifier = Modifier
                        .noRippleClickable(onWarningClick)
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(account.account.getAccountColor())
                        .border(1.dp, Color.Black, CircleShape)
                        .padding(6.dp)
                )
            }
        }
    }
}

@Composable
fun AccountAssets(modifier: Modifier = Modifier, accountBalance: AccountBalance, session: GdkSession? = null) {
    AnimatedVisibility(visible = !accountBalance.assets.isNullOrEmpty(), modifier = modifier) {
        Box(modifier = Modifier.height(36.dp)) {
            accountBalance.assets?.forEachIndexed { index, asset ->
                val size = 26
                val padding = (size / (1.5 + (0.1 * index)) * index)

                Image(
                    painter = asset.assetIcon(session = session, isLightning = accountBalance.account.isLightning),
                    contentDescription = null,
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = padding.dp)
                        .size(size.dp)
                        .clip(CircleShape)
                        .align(Alignment.BottomEnd)
                        .border(1.dp, Color.Black, CircleShape)
                        .zIndex(-index.toFloat())
                )
            }
        }
    }
}

@Composable
@Preview
fun AccountAssetsPreview() {
    GreenThemePreview {
        AccountAssets(accountBalance = previewAccountBalance().let {
            it.copy(
                assets = listOf(
                    BTC_POLICY_ASSET,
                    BTC_POLICY_ASSET,
                    LBTC_POLICY_ASSET,
                    LBTC_POLICY_ASSET,
                    LBTC_POLICY_ASSET,
                    LBTC_POLICY_ASSET,
                    LBTC_POLICY_ASSET
                )
            )
        })
    }
}


@Composable
@Preview
fun GreenAccountCardPreview() {
    GreenThemePreview {
        GreenColumn(horizontalAlignment = Alignment.CenterHorizontally, space = 1, padding = 0) {
            var expanded by remember {
                mutableStateOf(0)
            }
            GreenAccountCard(
                account = previewAccountBalance().let {
                    it.copy(
                        balance =  null,
                        assets = listOf(
                            BTC_POLICY_ASSET,
                            LBTC_POLICY_ASSET,
                            LBTC_POLICY_ASSET,
                            LBTC_POLICY_ASSET,
                            LBTC_POLICY_ASSET,
                            LBTC_POLICY_ASSET,
                            LBTC_POLICY_ASSET,
                            LBTC_POLICY_ASSET
                        )
                    )
                },
                isExpanded = expanded == 0,
                onClick = {
                    expanded = 0
                },
                onArrowClick = {

                })
            GreenAccountCard(
                account = previewAccountBalance(),
                isExpanded = expanded == 0,
                onClick = {
                    expanded = 0
                },
                onCopyClick = {

                })
            GreenAccountCard(
                account = previewAccountBalance(),
                isExpanded = expanded == 2,
                onClick = {
                    expanded = 2
                })
            GreenAccountCard(
                account = previewAccountBalance(),
                isExpanded = expanded == 3,
                onClick = {
                    expanded = 3
                })
            GreenAccountCard(
                account = previewAccountBalance(),
                isExpanded = expanded == 4,
                onWarningClick = {

                },
                onClick = {
                    expanded = 4
                })
        }
    }
}