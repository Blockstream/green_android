package com.blockstream.compose.screens.recovery

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.koin.getScreenModel
import com.arkivanov.essenty.parcelable.Parcelable
import com.arkivanov.essenty.parcelable.Parcelize
import com.blockstream.common.data.SetupArgs
import com.blockstream.common.events.Events
import com.blockstream.common.models.recovery.RecoveryWordsViewModel
import com.blockstream.common.models.recovery.RecoveryWordsViewModelAbstract
import com.blockstream.common.models.recovery.RecoveryWordsViewModelPreview
import com.blockstream.compose.GreenPreview
import com.blockstream.compose.R
import com.blockstream.compose.components.GreenButton
import com.blockstream.compose.components.GreenButtonSize
import com.blockstream.compose.components.GreenColumn
import com.blockstream.compose.extensions.colorText
import com.blockstream.compose.sheets.BottomSheetNavigatorM3
import com.blockstream.compose.theme.GreenTheme
import com.blockstream.compose.theme.GreenThemePreview
import com.blockstream.compose.theme.bodyLarge
import com.blockstream.compose.theme.bodySmall
import com.blockstream.compose.theme.displaySmall
import com.blockstream.compose.theme.headlineSmall
import com.blockstream.compose.theme.titleSmall
import com.blockstream.compose.theme.whiteMedium
import com.blockstream.compose.utils.AppBar
import com.blockstream.compose.utils.HandleSideEffect
import org.koin.core.parameter.parametersOf

@Parcelize
data class RecoveryWordsScreen(val setupArgs: SetupArgs) : Screen, Parcelable {

    override val key: ScreenKey
        get() = "${super.key}:${setupArgs.hashCode()}}"

    @Composable
    override fun Content() {
        val viewModel = getScreenModel<RecoveryWordsViewModel>() {
            parametersOf(setupArgs)
        }

        val navData by viewModel.navData.collectAsStateWithLifecycle()

        AppBar(navData)

        RecoveryWordsScreen(viewModel = viewModel)
    }
}

@Composable
fun RecoveryWordsScreen(
    viewModel: RecoveryWordsViewModelAbstract
) {
    HandleSideEffect(viewModel)

    GreenColumn(
        padding = 0,
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .padding(bottom = 16.dp)
            .fillMaxHeight(),
        horizontalAlignment = Alignment.CenterHorizontally
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

            val greenString = colorText(
                stringResource(R.string.id_write_down_your_recovery_phrase),
                listOf(
                    R.string.id_recovery_phrase,
                    R.string.id_correct_order
                ).map { stringResource(it) }
            )

            Text(
                greenString,
                style = displaySmall,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Text(stringResource(R.string.id_store_it_somewhere_safe), style = bodyLarge)
        }


        GreenColumn(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.SpaceAround
        ) {
            viewModel.words.forEachIndexed { index, word ->
                Item(index = viewModel.startIndex + index, word = word)
            }
        }

        GreenButton(
            stringResource(R.string.id_continue),
            size = GreenButtonSize.BIG,
            modifier = Modifier.fillMaxWidth()
        ) {
            viewModel.postEvent(Events.Continue)
        }

        GreenColumn(space = 4, padding = 0, horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                painter = painterResource(id = R.drawable.house),
                contentDescription = null,
                tint = whiteMedium
            )
            Text(
                stringResource(R.string.id_make_sure_to_be_in_a_private),
                style = bodySmall,
                color = whiteMedium
            )
        }
    }

}

@Composable
private fun Item(index: Int, word: String) {
    ConstraintLayout(modifier = Modifier.fillMaxWidth()) {

        val (wordRef, indexRef) = createRefs()

        Text(word, style = headlineSmall,
            modifier = Modifier.constrainAs(wordRef) {
                start.linkTo(parent.start)
                end.linkTo(parent.end)
            }
        )

        Text(
            "$index",
            color = MaterialTheme.colorScheme.primary,
            style = titleSmall,
            modifier = Modifier.constrainAs(indexRef) {
                end.linkTo(wordRef.start, margin = 8.dp)
                baseline.linkTo(wordRef.baseline)

            }
        )

    }
}

@Composable
@Preview
private fun ItemPreview() {
    GreenThemePreview {
        GreenColumn {
            Item(1, "word")
        }
    }
}

@Composable
@Preview
fun RecoveryWordsScreenPreview() {
    GreenPreview {
        RecoveryWordsScreen(viewModel = RecoveryWordsViewModelPreview.preview())
    }
}