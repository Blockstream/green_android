package com.blockstream.compose.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.blockstream.compose.theme.GreenThemePreview


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