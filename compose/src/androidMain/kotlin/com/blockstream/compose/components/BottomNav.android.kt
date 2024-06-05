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