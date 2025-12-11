package com.blockstream.compose.screens.recovery

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.house
import blockstream_green.common.generated.resources.id_make_sure_to_be_in_a_private
import blockstream_green.common.generated.resources.id_recovery_phrase_check
import blockstream_green.common.generated.resources.id_what_is_word_number_s
import com.blockstream.common.models.recovery.RecoveryCheckViewModel
import com.blockstream.common.models.recovery.RecoveryCheckViewModelAbstract
import com.blockstream.compose.components.GreenButton
import com.blockstream.compose.components.GreenButtonColor
import com.blockstream.compose.components.GreenButtonSize
import com.blockstream.compose.components.GreenButtonType
import com.blockstream.compose.extensions.colorText
import com.blockstream.compose.theme.bodyLarge
import com.blockstream.compose.theme.bodySmall
import com.blockstream.compose.theme.displaySmall
import com.blockstream.compose.theme.green
import com.blockstream.compose.theme.titleSmall
import com.blockstream.compose.theme.whiteHigh
import com.blockstream.compose.theme.whiteMedium
import com.blockstream.compose.utils.SetupScreen
import com.blockstream.compose.components.GreenColumn
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun RecoveryCheckScreen(
    viewModel: RecoveryCheckViewModelAbstract
) {
    SetupScreen(viewModel = viewModel, withPadding = false) {

        GreenColumn(
            padding = 0,
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .padding(bottom = 16.dp)
                .fillMaxHeight(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {

            GreenColumn(
                padding = 0,
                modifier = Modifier.padding(horizontal = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                LinearProgressIndicator(
                    progress = {
                        viewModel.progress / 100f
                    },
                    modifier = Modifier
                        .widthIn(max = 250.dp)
                        .height(12.dp)
                        .clip(RoundedCornerShape(6.dp)),
                )

                Text(
                    stringResource(Res.string.id_recovery_phrase_check),
                    style = displaySmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    stringResource(Res.string.id_what_is_word_number_s, viewModel.checkWordIndex),
                    style = bodyLarge
                )
            }


            ConstraintLayout(modifier = Modifier.fillMaxWidth()) {

                val (line, checkIndex, leftWord, rightWord) = createRefs()

                HorizontalDivider(
                    color = green,
                    thickness = 2.dp,
                    modifier = Modifier
                        .width(84.dp)
                        .constrainAs(line) {
                            start.linkTo(parent.start)
                            end.linkTo(parent.end)
                            bottom.linkTo(parent.bottom)
                        })

                Text(
                    "${viewModel.checkWordIndex}.", style = titleSmall, color = green,
                    modifier = Modifier.constrainAs(checkIndex) {
                        start.linkTo(line.start)
                        bottom.linkTo(line.top)
                    }
                )

                "${viewModel.checkWordIndex - 1}.".let {
                    colorText("$it ${viewModel.wordLeft}", listOf(it))
                }.also {
                    Text(
                        it, style = titleSmall, color = whiteHigh,
                        modifier = Modifier.constrainAs(leftWord) {
                            end.linkTo(line.start, margin = 16.dp)
                            baseline.linkTo(checkIndex.baseline)
                        }
                    )
                }

                "${viewModel.checkWordIndex + 1}.".let {
                    colorText("$it ${viewModel.wordRight}", listOf(it))
                }.also {
                    Text(
                        it, style = titleSmall, color = whiteHigh,
                        modifier = Modifier.constrainAs(rightWord) {
                            start.linkTo(line.end, margin = 16.dp)
                            baseline.linkTo(checkIndex.baseline)
                        }
                    )
                }
            }

            GreenColumn {
                viewModel.words.forEachIndexed { index, word ->
                    val highlight =
                        viewModel.hightlightCorrectWord && index == viewModel.correctWordIndex
                    GreenButton(
                        text = word,
                        size = GreenButtonSize.BIG,
                        type = GreenButtonType.OUTLINE,
                        color = if (highlight) GreenButtonColor.GREENER else GreenButtonColor.WHITE,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        viewModel.postEvent(RecoveryCheckViewModel.LocalEvents.SelectWord(word))
                    }
                }
            }

            GreenColumn(
                space = 4,
                padding = 0,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    painter = painterResource(Res.drawable.house),
                    contentDescription = null,
                    tint = whiteMedium
                )
                Text(
                    stringResource(Res.string.id_make_sure_to_be_in_a_private),
                    style = bodySmall,
                    color = whiteMedium
                )
            }
        }
    }
}