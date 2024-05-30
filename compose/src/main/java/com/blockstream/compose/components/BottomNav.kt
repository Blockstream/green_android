package com.blockstream.compose.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.blockstream.compose.R
import com.blockstream.compose.theme.GreenSmallEnd
import com.blockstream.compose.theme.GreenSmallStart
import com.blockstream.compose.theme.GreenThemePreview
import com.blockstream.compose.theme.bottom_nav_bg
import com.blockstream.compose.theme.green

@Composable
fun BottomNav(
    modifier: Modifier = Modifier,
    isWatchOnly: Boolean = false,
    isSweepEnabled: Boolean = false,
    showMenu: Boolean = false,
    onSendClick: () -> Unit,
    onReceiveClick: () -> Unit,
    onCircleClick: () -> Unit
) {

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .then(modifier)
    ) {
        Row(
            modifier = Modifier.align(Alignment.Center),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {

            Card(
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp),
                shape = GreenSmallEnd,
                colors = CardDefaults.cardColors(containerColor = bottom_nav_bg),
                onClick = onSendClick,
                enabled = !isWatchOnly || isSweepEnabled
            ) {
                GreenRow(
                    padding = 0,
                    space = 8,
                    modifier = Modifier
                        .weight(1f)
                        .align(Alignment.CenterHorizontally)
                        .padding(end = 30.dp)
                ) {
                    Icon(
                        painterResource(if (isWatchOnly && isSweepEnabled) R.drawable.broom else R.drawable.arrow_line_up),
                        contentDescription = null,
                        tint = green,
                    )
                    Text(text = stringResource(if (isWatchOnly && isSweepEnabled) R.string.id_sweep else R.string.id_send))
                }
            }

            Card(
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp),
                shape = GreenSmallStart,
                colors = CardDefaults.cardColors(containerColor = bottom_nav_bg),
                onClick = onReceiveClick
            ) {
                GreenRow(
                    padding = 0,
                    space = 8,
                    modifier = Modifier
                        .weight(1f)
                        .align(Alignment.CenterHorizontally)
                        .padding(start = 30.dp)
                ) {
                    Icon(
                        painterResource(id = R.drawable.arrow_line_down),
                        contentDescription = null,
                        tint = green
                    )
                    Text(text = stringResource(id = R.string.id_receive))
                }
            }
        }

        FloatingActionButton(
            modifier = Modifier
                .align(Alignment.Center)
                .size(60.dp),
            shape = CircleShape,
            onClick = {
                onCircleClick.invoke()
            },
        ) {
            if (showMenu) {
                Icon(Icons.Filled.Add, "Floating action button.")
            } else {
                Icon(painterResource(R.drawable.qr_code), "Scan")
            }
        }
    }
}

@Composable
@Preview
fun BottomNavPreview() {
    GreenThemePreview {
        GreenColumn {
            BottomNav(modifier = Modifier, onSendClick = {

            }, onReceiveClick = {

            }, onCircleClick = {

            })

            BottomNav(modifier = Modifier, isSweepEnabled = true, onSendClick = {

            }, onReceiveClick = {

            }, onCircleClick = {

            })

            BottomNav(
                modifier = Modifier,
                isSweepEnabled = false,
                isWatchOnly = true,
                onSendClick = {

                },
                onReceiveClick = {

                },
                onCircleClick = {

                }
            )
        }
    }
}