package com.blockstream.compose.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.arrow_right
import blockstream_green.common.generated.resources.id_copy_id
import blockstream_green.common.generated.resources.id_experimental
import blockstream_green.common.generated.resources.shield_warning
import com.blockstream.common.gdk.GdkSession
import com.blockstream.common.gdk.data.AccountBalance
import com.blockstream.compose.extensions.assetIcon
import com.blockstream.compose.extensions.getAccountColor
import com.blockstream.compose.extensions.policyAndType
import com.blockstream.compose.extensions.policyIcon
import com.blockstream.compose.theme.bodyMedium
import com.blockstream.compose.theme.md_theme_surfaceCircle
import com.blockstream.compose.theme.titleMedium
import com.blockstream.compose.theme.whiteHigh
import com.blockstream.compose.theme.whiteMedium
import com.blockstream.compose.utils.ifTrue
import com.blockstream.compose.utils.noRippleClickable
import com.blockstream.compose.utils.roundBackground
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun GreenAccountCard(
    modifier: Modifier = Modifier,
    account: AccountBalance,
    isExpanded: Boolean,
    session: GdkSession? = null,
    onCopyClick: ((AccountBalance) -> Unit)? = null,
    onArrowClick: ((AccountBalance) -> Unit)? = null,
    onWarningClick: ((AccountBalance) -> Unit)? = null,
    onClick: (AccountBalance) -> Unit = {},
    onLongClick: (AccountBalance, offset: Offset) -> Unit = { _ , _ -> },
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .then(modifier)
    ) {

        Card(
            colors = CardDefaults.cardColors(
                containerColor = account.account.getAccountColor(),
                contentColor = whiteHigh
            ),
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth()
                .pointerInput(Unit){
                    detectTapGestures(
                        onTap = {
                            onClick(account)
                        },
                        onLongPress = {
                            onLongClick(account, it)
                        }
                    )
                }
        ) {

            Row {
                GreenColumn(space = 0, modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(account.account.policyIcon()),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )

                        Text(
                            text = account.account.type.policyAndType().uppercase(),
                            style = bodyMedium,
                            color = whiteMedium
                        )

                        if (account.account.isLightning) {
                            Text(
                                stringResource(Res.string.id_experimental),
                                style = bodyMedium,
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
                                        style = bodyMedium,
                                        color = whiteMedium
                                    )
                                    Text(account.balance ?: "", style = titleMedium)
                                }
                            }
                        }

                        if (onCopyClick != null) {
                            GreenButton(
                                modifier = Modifier.align(Alignment.Bottom),
                                text = stringResource(Res.string.id_copy_id),
                                type = GreenButtonType.OUTLINE,
                                color = GreenButtonColor.WHITE,
                                size = GreenButtonSize.SMALL,
                                onClick = {
                                    onCopyClick(account)
                                }
                            )
                        } else if (onArrowClick != null) {
                            Card(
                                onClick = {
                                    onArrowClick(account)
                                },
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
                                        painter = painterResource(Res.drawable.arrow_right),
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
                    painter = painterResource(Res.drawable.shield_warning),
                    contentDescription = null,
                    modifier = Modifier
                        .noRippleClickable {
                            onWarningClick(account)
                        }
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