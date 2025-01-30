package com.blockstream.compose.screens.recovery

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.house
import blockstream_green.common.generated.resources.id_authenticate_to_view_the
import blockstream_green.common.generated.resources.id_if_you_forget_it_or_lose_it
import blockstream_green.common.generated.resources.id_make_sure_to_be_in_a_private
import blockstream_green.common.generated.resources.id_make_sure_you_are_alone_and_no
import blockstream_green.common.generated.resources.id_recovery_phrase_length
import blockstream_green.common.generated.resources.id_safe_environment
import blockstream_green.common.generated.resources.id_safely_stored
import blockstream_green.common.generated.resources.id_sensitive_information
import blockstream_green.common.generated.resources.id_show_recovery_phrase
import blockstream_green.common.generated.resources.id_whomever_can_access_your
import blockstream_green.common.generated.resources.shield_check
import blockstream_green.common.generated.resources.warning
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import com.blockstream.common.Parcelable
import com.blockstream.common.Parcelize
import com.blockstream.common.data.SetupArgs
import com.blockstream.common.events.Events
import com.blockstream.common.models.recovery.RecoveryIntroViewModel
import com.blockstream.common.models.recovery.RecoveryIntroViewModelAbstract
import com.blockstream.compose.LocalBiometricState
import com.blockstream.compose.components.GreenButton
import com.blockstream.compose.components.GreenButtonSize
import com.blockstream.compose.components.GreenCard
import com.blockstream.ui.components.GreenColumn
import com.blockstream.ui.components.GreenRow
import com.blockstream.compose.managers.rememberStateKeeperFactory
import com.blockstream.compose.theme.bodyLarge
import com.blockstream.compose.theme.bodySmall
import com.blockstream.compose.theme.labelMedium
import com.blockstream.compose.theme.whiteMedium
import com.blockstream.compose.utils.AppBar
import com.blockstream.compose.utils.HandleSideEffect
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.core.parameter.parametersOf


@Parcelize
data class RecoveryIntroScreen(val setupArgs: SetupArgs) : Screen, Parcelable {
    @Composable
    override fun Content() {
        val stateKeeper = rememberStateKeeperFactory()

        val viewModel = koinScreenModel<RecoveryIntroViewModel>() {
            parametersOf(setupArgs, stateKeeper.stateKeeper())
        }

        val navData by viewModel.navData.collectAsStateWithLifecycle()

        AppBar(navData)

        RecoveryIntroScreen(viewModel = viewModel)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecoveryIntroScreen(
    viewModel: RecoveryIntroViewModelAbstract
) {

    val biometricsState = LocalBiometricState.current

    HandleSideEffect(viewModel) { sideEffect ->
        if (sideEffect is RecoveryIntroViewModel.LocalSideEffects.LaunchUserPresence) {
            biometricsState?.launchUserPresencePrompt(getString(Res.string.id_authenticate_to_view_the)) {
                viewModel.postEvent(RecoveryIntroViewModel.LocalEvents.Authenticated(it))
            }
        }
    }

    GreenColumn(horizontalAlignment = Alignment.CenterHorizontally) {

        GreenColumn(
            padding = 0,
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
        ) {
            Item(
                stringResource(Res.string.id_safe_environment),
                stringResource(Res.string.id_make_sure_you_are_alone_and_no),
                Res.drawable.house
            )

            Item(
                stringResource(Res.string.id_sensitive_information),
                stringResource(Res.string.id_whomever_can_access_your),
                Res.drawable.warning
            )

            Item(
                stringResource(Res.string.id_safely_stored),
                stringResource(Res.string.id_if_you_forget_it_or_lose_it),
                Res.drawable.shield_check
            )
        }

        if (viewModel.setupArgs.isGenerateMnemonic) {
            GreenCard(
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.background),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                GreenRow(
                    padding = 0,
                ) {
                    Text(
                        stringResource(Res.string.id_recovery_phrase_length),
                        modifier = Modifier.weight(1f)
                    )

                    val options = listOf(12, 24)
                    val mnemonicSize by viewModel.mnemonicSize.collectAsStateWithLifecycle()
                    SingleChoiceSegmentedButtonRow {
                        options.forEachIndexed { index, label ->
                            SegmentedButton(
                                shape = SegmentedButtonDefaults.itemShape(
                                    index = index, count = options.size
                                ),
                                onClick = { viewModel.mnemonicSize.value = options[index] },
                                selected = mnemonicSize == options[index]
                            ) {
                                Text(label.toString())
                            }
                        }
                    }
                }
            }
        }

        GreenButton(
            stringResource(Res.string.id_show_recovery_phrase),
            size = GreenButtonSize.BIG,
            modifier = Modifier.fillMaxWidth()
        ) {
            viewModel.postEvent(Events.Continue)
        }

        GreenColumn(space = 4, padding = 0, horizontalAlignment = Alignment.CenterHorizontally) {
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

@Composable
internal fun Item(title: String, message: String, icon: DrawableResource) {
    GreenCard {
        GreenColumn(
            padding = 0,
            space = 8,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            GreenColumn(
                space = 4,
                padding = 0,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(painter = painterResource(icon), contentDescription = null)
                Text(title, style = labelMedium)
            }
            Text(message, textAlign = TextAlign.Center, style = bodyLarge)
        }
    }
}