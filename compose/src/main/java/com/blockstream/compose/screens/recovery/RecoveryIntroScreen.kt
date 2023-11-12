package com.blockstream.compose.screens.recovery

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.androidx.AndroidScreenLifecycleOwner
import cafe.adriel.voyager.core.lifecycle.ScreenLifecycleProvider
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.getScreenModel
import com.arkivanov.essenty.parcelable.Parcelable
import com.arkivanov.essenty.parcelable.Parcelize
import com.arkivanov.essenty.statekeeper.StateKeeperDispatcher
import com.arkivanov.essenty.statekeeper.stateKeeper
import com.blockstream.common.data.SetupArgs
import com.blockstream.common.events.Events
import com.blockstream.common.models.recovery.RecoveryIntroViewModel
import com.blockstream.common.models.recovery.RecoveryIntroViewModelAbstract
import com.blockstream.common.models.recovery.RecoveryIntroViewModelPreview
import com.blockstream.common.utils.AndroidKeystore
import com.blockstream.compose.LocalSnackbar
import com.blockstream.compose.R
import com.blockstream.compose.components.GreenButton
import com.blockstream.compose.components.GreenButtonSize
import com.blockstream.compose.components.GreenCard
import com.blockstream.compose.components.GreenColumn
import com.blockstream.compose.components.GreenRow
import com.blockstream.compose.sheets.BottomSheetNavigatorM3
import com.blockstream.compose.sideeffects.BiometricsState
import com.blockstream.compose.sideeffects.DialogHost
import com.blockstream.compose.sideeffects.DialogState
import com.blockstream.compose.theme.GreenTheme
import com.blockstream.compose.theme.bodyLarge
import com.blockstream.compose.theme.bodyMedium
import com.blockstream.compose.theme.bodySmall
import com.blockstream.compose.theme.labelMedium
import com.blockstream.compose.theme.whiteMedium
import com.blockstream.compose.utils.AppBar
import com.blockstream.compose.utils.AppBarData
import com.blockstream.compose.utils.HandleSideEffect
import org.koin.compose.koinInject
import org.koin.core.parameter.parametersOf

@Parcelize
data class RecoveryIntroScreen(val setupArgs: SetupArgs) : Screen, Parcelable {
    @Composable
    override fun Content() {
        val viewModel = getScreenModel<RecoveryIntroViewModel>() {
            parametersOf(setupArgs, StateKeeperDispatcher())
        }

        AppBar {
            AppBarData(title = stringResource(R.string.id_before_you_backup))
        }

        RecoveryIntroScreen(viewModel = viewModel)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecoveryIntroScreen(
    viewModel: RecoveryIntroViewModelAbstract
) {
    val context = LocalContext.current

    val snackbar = LocalSnackbar.current
    val scope = rememberCoroutineScope()
    // LocalInspectionMode is true in preview
    val androidKeystore: AndroidKeystore =
        if (LocalInspectionMode.current) AndroidKeystore(context) else koinInject()

    val dialogState = remember { DialogState(context) }
    DialogHost(state = dialogState)

    val biometricsState = remember {
        BiometricsState(
            context = context,
            coroutineScope = scope,
            snackbarHostState = snackbar,
            dialogState = dialogState,
            androidKeystore = androidKeystore
        )
    }

    HandleSideEffect(viewModel) { sideEffect ->
        if (sideEffect is RecoveryIntroViewModel.LocalSideEffects.LaunchUserPresence) {
            biometricsState.launchUserPresencePrompt {
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
                stringResource(id = R.string.id_safe_environment),
                stringResource(id = R.string.id_make_sure_you_are_alone_and_no),
                R.drawable.house
            )

            Item(
                stringResource(id = R.string.id_sensitive_information),
                stringResource(id = R.string.id_whomever_can_access_your),
                R.drawable.warning
            )

            Item(
                stringResource(id = R.string.id_safely_stored),
                stringResource(id = R.string.id_if_you_forget_it_or_lose_it),
                R.drawable.shield_check
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
                        stringResource(R.string.id_recovery_phrase_length),
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
            stringResource(R.string.id_show_recovery_phrase),
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
private fun Item(title: String, message: String, icon: Int) {
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
                Icon(painter = painterResource(id = icon), contentDescription = null)
                Text(title, style = labelMedium)
            }
            Text(message, textAlign = TextAlign.Center, style = bodyLarge)
        }
    }
}

@Composable
@Preview
private fun ItemPreview() {
    GreenTheme {
        GreenColumn {
            Item(
                stringResource(id = R.string.id_safe_environment),
                stringResource(id = R.string.id_make_sure_you_are_alone_and_no),
                R.drawable.house
            )
        }
    }
}

@Composable
@Preview
fun RecoveryIntroScreenPreview() {
    GreenTheme {
        BottomSheetNavigatorM3 {
            RecoveryIntroScreen(viewModel = RecoveryIntroViewModelPreview.preview())
        }
    }
}