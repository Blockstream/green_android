package com.blockstream.compose.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.blockstream.ui.components.GreenColumn
import com.blockstream.compose.theme.GreenChromePreview


@Composable
@Preview
fun BottomNavPreview() {
    GreenChromePreview {
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
                canSend = true,
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