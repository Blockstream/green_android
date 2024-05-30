package com.blockstream.compose.screens.recovery

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import com.arkivanov.essenty.parcelable.Parcelable
import com.blockstream.common.data.GreenWallet
import com.blockstream.common.extensions.isNotBlank
import com.blockstream.common.gdk.data.Credentials
import com.blockstream.common.models.recovery.RecoveryPhraseViewModel
import com.blockstream.common.models.recovery.RecoveryPhraseViewModelAbstract
import com.blockstream.common.models.recovery.RecoveryPhraseViewModelPreview
import com.blockstream.compose.GreenPreview
import com.blockstream.compose.R
import com.blockstream.compose.components.GreenButton
import com.blockstream.compose.components.GreenButtonSize
import com.blockstream.compose.components.GreenButtonType
import com.blockstream.compose.components.GreenCard
import com.blockstream.compose.components.GreenColumn
import com.blockstream.compose.components.GreenQR
import com.blockstream.compose.components.GreenRow
import com.blockstream.compose.components.LearnMoreButton
import com.blockstream.compose.extensions.random
import com.blockstream.compose.theme.GreenTheme
import com.blockstream.compose.theme.GreenThemePreview
import com.blockstream.compose.theme.bodyMedium
import com.blockstream.compose.theme.green
import com.blockstream.compose.theme.green20
import com.blockstream.compose.theme.labelLarge
import com.blockstream.compose.theme.labelMedium
import com.blockstream.compose.theme.labelSmall
import com.blockstream.compose.theme.titleMedium
import com.blockstream.compose.theme.titleSmall
import com.blockstream.compose.theme.whiteMedium
import com.blockstream.compose.utils.AlphaPulse
import com.blockstream.compose.utils.AppBar
import com.blockstream.compose.utils.HandleSideEffect
import com.blockstream.compose.utils.Pulsating
import kotlinx.parcelize.Parcelize
import org.koin.core.parameter.parametersOf
import kotlin.math.ceil


@Parcelize
data class RecoveryPhraseScreen(
    val isLightning: Boolean,
    val providedCredentials: Credentials?,
    val greenWallet: GreenWallet?
) : Screen, Parcelable {
    @Composable
    override fun Content() {
        val viewModel = koinScreenModel<RecoveryPhraseViewModel>() {
            parametersOf(isLightning, providedCredentials, greenWallet)
        }

        val navData by viewModel.navData.collectAsStateWithLifecycle()
        AppBar(navData)

        RecoveryPhraseScreen(viewModel = viewModel)
    }
}

@Composable
fun RecoveryPhraseScreen(
    viewModel: RecoveryPhraseViewModelAbstract
) {
    HandleSideEffect(viewModel)

    Column {

        Column {
            Text(
                text = stringResource(id = R.string.id_the_recovery_phrase_can_be_used),
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 16.dp)
            )
        }

        val mnemonicWords by viewModel.mnemonicWords.collectAsStateWithLifecycle()
        LazyVerticalGrid(
            columns = GridCells.Fixed(3)
        ) {
            itemsIndexed(mnemonicWords) { index, word ->
                WordItem(index = index + 1, word = word)
            }
        }


        if (viewModel.isLightning) {
            GreenColumn(modifier = Modifier.fillMaxWidth()) {
                AlphaPulse {
                    Card(
                        colors = CardDefaults.elevatedCardColors(
                            containerColor = green20,
                        )
                    ) {
                        Text(
                            text = stringResource(id = R.string.id_use_this_recovery_phrase_to),
                            style = labelMedium,
                            modifier = Modifier.padding(vertical = 10.dp, horizontal = 12.dp)
                        )
                    }
                }
            }
        }

        val passphrase by viewModel.passphrase.collectAsStateWithLifecycle()

        if (passphrase.isNotBlank()) {
            GreenColumn(space = 0, modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = stringResource(id = R.string.id_bip39_passphrase),
                    style = labelMedium,
                    color = whiteMedium
                )

                GreenRow(padding = 0, space = 8, modifier = Modifier.padding(top = 8.dp)) {
                    Image(
                        painter = painterResource(id = R.drawable.bip39_passphrase),
                        contentDescription = "Lock",
                        colorFilter = ColorFilter.tint(green)
                    )
                    SelectionContainer {
                        Text(text = passphrase ?: "", style = titleSmall)
                    }
                }

                LearnMoreButton {
                    viewModel.postEvent(RecoveryPhraseViewModel.LocalEvents.ClickLearnMore)
                }
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .padding(bottom = 16.dp)
                .weight(1f)
        ) {
            val mnemonic = viewModel.mnemonic.value
            val showQR by viewModel.showQR.collectAsStateWithLifecycle()
            GreenQR(
                data = mnemonic, modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(), isVisible = showQR,
                visibilityClick = {
                    viewModel.postEvent(RecoveryPhraseViewModel.LocalEvents.ShowQR)
                }
            )

            if (passphrase.isNotBlank()) {
                AlphaPulse {
                    Text(
                        text = stringResource(id = R.string.id_the_qr_code_does_not_include),
                        textAlign = TextAlign.Center,
                        style = labelMedium,
                    )
                }
            }
        }
    }
}

@Composable
private fun WordItem(index: Int, word: String) {

    ConstraintLayout(
        modifier = Modifier
            .widthIn(min = 100.dp)
            .background(
                if (ceil(index / 3f).toInt() % 2 == 0) Color(0x1500b45a) else Color.Transparent
            )
            .padding(vertical = 6.dp)
    ) {
        val (indexRef, wordRef) = createRefs()
        Text(
            "$index",
            color = MaterialTheme.colorScheme.primary,
            style = labelLarge,
            modifier = Modifier
                .constrainAs(indexRef) {
                    start.linkTo(parent.start, margin = 16.dp)
                    baseline.linkTo(wordRef.baseline)
                }
        )

        Text(word, style = titleMedium,
            overflow = TextOverflow.Clip,
            maxLines = 1,
            textAlign = TextAlign.Start,
            modifier = Modifier
                .constrainAs(wordRef) {
                    start.linkTo(indexRef.end, margin = 8.dp)
                    end.linkTo(parent.end)
                    width = Dimension.fillToConstraints
                }
        )
    }
}

@Composable
@Preview
fun WordItemPreview() {
    GreenThemePreview {
        GreenColumn {
            WordItem(1, "chalk")
            WordItem(2, "patch")
            WordItem(3, "eye")
            WordItem(4, "speak")
        }
    }
}

@Composable
@Preview
fun RecoveryPhraseScreenPreview() {
    GreenPreview {
        RecoveryPhraseScreen(viewModel = RecoveryPhraseViewModelPreview.previewBip39())
    }
}