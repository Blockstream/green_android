package com.blockstream.compose.views

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.arrows_counter_clockwise
import blockstream_green.common.generated.resources.backspace
import blockstream_green.common.generated.resources.clipboard
import blockstream_green.common.generated.resources.id_invalid_clipboard_contents
import com.blockstream.common.extensions.isDigitsOnly
import com.blockstream.common.utils.stringResourceFromId
import com.blockstream.compose.managers.LocalPlatformManager
import com.blockstream.compose.theme.MonospaceFont
import com.blockstream.compose.theme.displaySmall
import com.blockstream.compose.theme.green
import com.blockstream.compose.theme.headlineLarge
import com.blockstream.compose.theme.headlineSmall
import com.blockstream.compose.theme.labelMedium
import com.blockstream.compose.theme.md_theme_outline
import com.blockstream.compose.theme.whiteHigh
import com.blockstream.compose.theme.whiteLow
import com.blockstream.compose.utils.AnimatedNullableVisibility
import com.blockstream.compose.utils.ifTrue
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.painterResource

@Composable
fun PinButton(
    key: Int,
    digits: List<String>? = null,
    enabled: Boolean = true,
    isSmall: Boolean = false,
    isShuffle: Boolean = false,
    rowScope: RowScope,
    onClick: (key: Int) -> Unit
) {
    rowScope.apply {
        Box(
            modifier = Modifier
                .weight(1f)
                .aspectRatio(1f, true)
                .align(Alignment.CenterVertically)
        ) {

            TextButton(
                enabled = enabled,
                onClick = { onClick(key) }, modifier = Modifier
                    .aspectRatio(1f, true)
                    .align(Alignment.Center)
            ) {
                when (key) {
                    -1 -> {
                        Icon(
                            painter = painterResource(Res.drawable.backspace),
                            contentDescription = "Backspace",
                            tint = if (enabled) whiteHigh else whiteLow
                        )
                    }

                    -2 -> {
                        if(isShuffle) {
                            Icon(
                                painter = painterResource(Res.drawable.arrows_counter_clockwise),
                                contentDescription = "Shuffle",
                                tint = if (enabled) whiteHigh else whiteLow
                            )
                        }else{
                            Icon(
                                painter = painterResource(Res.drawable.clipboard),
                                contentDescription = "Paste",
                                tint = if (enabled) whiteHigh else whiteLow
                            )
                        }
                    }

                    else -> {
                        Text(
                            digits?.get(key) ?: "-",
                            color = if (enabled) whiteHigh else whiteLow,
                            style = if(isSmall) headlineSmall else headlineLarge
                        )
                    }
                }
            }
        }
    }
}


@Composable
fun PinView(
    modifier: Modifier = Modifier,
    error: String? = null,
    withShuffle: Boolean = true,
    showDigits: Boolean = false,
    isSmall: Boolean = false,
    isVerifyMode: Boolean = false,
    snackbarHostState: SnackbarHostState? = null,
    onPinNotVerified: () -> Unit = {},
    onModeChange: (isVerify: Boolean) -> Unit = {},
    onPin: (pin: String) -> Unit = {}
) {
    val platformManager = LocalPlatformManager.current
    val scope = rememberCoroutineScope()
    var digits by remember { mutableStateOf("1 2 3 4 5 6 7 8 9 0".split(" ")) }
    var pin by remember { mutableStateOf("") }
    var pinToBeVerified by remember { mutableStateOf("") }


    fun validatePin() {
        if (pin.length == 6) {
            if (isVerifyMode) {
                when {
                    pinToBeVerified.isEmpty() -> {
                        pinToBeVerified = pin
                        pin = ""
                        onModeChange(true)
                        onPin("")
                    }

                    pin == pinToBeVerified -> {
                        onPin(pin)
                    }

                    else -> {
                        pin = ""
                        pinToBeVerified = ""
                        onPinNotVerified()
                        onModeChange(false)
                        onPin("")
                        // shakeIndicator()
                    }
                }
            } else {
                onPin(pin)
            }
        } else {
            onPin("")
        }
    }

    fun deletePinDigit(deleteAllDigis: Boolean) {
        if (pin.isNotEmpty()) {
            if (deleteAllDigis) {
                pin = ""
                deletePinDigit(false) // call recursive to clear the verify mode
            } else {
                pin = pin.substring(0 until pin.length - 1)
            }
        } else if (isVerifyMode) {
            pinToBeVerified = ""
            onModeChange(false)
        }
    }

    val onClick: (key: Int) -> Unit = { key: Int ->
        if (key >= 0) {
            pin += digits[key]
        } else if (key == -1 && (pin.isNotEmpty() || pinToBeVerified.isNotEmpty())) {
            deletePinDigit(false)
        } else if (key == -2) {
            if (withShuffle) {
                digits = digits.shuffled()
            }else{
                platformManager.getClipboard()?.also {
                    if (it.length == 6 && it.isDigitsOnly()) {
                        pin = it
                    } else {
                        scope.launch {
                            snackbarHostState?.showSnackbar(
                                message = getString(Res.string.id_invalid_clipboard_contents)
                            )
                        }
                    }
                }
            }
        }

        validatePin()
    }

    ConstraintLayout(
        modifier = Modifier
            .fillMaxWidth()
            .then(modifier)
    ) {
        val (dots, keyboard) = createRefs()

        Column(
            modifier = Modifier
                .defaultMinSize(minHeight = 40.dp)
                .constrainAs(dots) {
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                    top.linkTo(parent.top)
                    bottom.linkTo(keyboard.top)
                },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically)
        ) {

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .ifTrue(showDigits) {
                        padding(vertical = 16.dp)
                    }
                ,
                horizontalArrangement = Arrangement.spacedBy(
                    if (showDigits) 6.dp else if(isSmall) 12.dp else 24.dp,
                    Alignment.CenterHorizontally
                ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                (1..6).forEach {
                    if(showDigits){
                        Text(
                            text = pin.getOrNull(it - 1)?.toString() ?: " ",
                            fontFamily = MonospaceFont(),
                            style = displaySmall,
                            color = green,
                            modifier = Modifier
                                .background(
                                    color = md_theme_outline,
                                    shape = RoundedCornerShape(8.dp),
                                )
                                .padding(horizontal = 6.dp)
                                .padding(vertical = 0.dp)
                        )
                    }else {
                        Box(
                            modifier = Modifier
                                .size(18.dp)
                                .clip(CircleShape)
                                .background(if (it <= pin.length) MaterialTheme.colorScheme.primary else Color.Black)
                        )
                    }
                }
            }

            AnimatedNullableVisibility(value = error) { error ->
                Text(
                    text = stringResourceFromId(error),
                    style = labelMedium,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center
                )
            }
        }


        Column(modifier = Modifier
            .heightIn(min = 100.dp, max = if (isSmall) 220.dp else 400.dp)
            .constrainAs(keyboard) {
                start.linkTo(parent.start)
                end.linkTo(parent.end)
                bottom.linkTo(parent.bottom)
            }) {

            Row(modifier = Modifier.weight(1f)) {
                (0..2).forEach {
                    PinButton(
                        key = it,
                        enabled = pin.length < 6,
                        isSmall = isSmall,
                        digits = digits,
                        rowScope = this,
                        onClick = onClick
                    )
                }
            }
            Row(modifier = Modifier.weight(1f)) {
                (3..5).forEach {
                    PinButton(
                        key = it,
                        enabled = pin.length < 6,
                        isSmall = isSmall,
                        digits = digits,
                        rowScope = this,
                        onClick = onClick
                    )
                }
            }
            Row(modifier = Modifier.weight(1f)) {
                (6..8).forEach {
                    PinButton(
                        key = it,
                        enabled = pin.length < 6,
                        isSmall = isSmall,
                        digits = digits,
                        rowScope = this,
                        onClick = onClick
                    )
                }
            }
            Row(modifier = Modifier.weight(1f)) {
                PinButton(
                    key = -2,
                    rowScope = this,
                    isShuffle = withShuffle,
                    onClick = onClick
                )
                PinButton(
                    key = 9,
                    enabled = pin.length < 6,
                    isSmall = isSmall,
                    digits = digits,
                    rowScope = this,
                    onClick = onClick
                )
                PinButton(
                    key = -1,
                    enabled = pin.isNotEmpty() || pinToBeVerified.isNotEmpty(),
                    isSmall = isSmall,
                    rowScope = this,
                    onClick = onClick
                )
            }
        }

    }
}