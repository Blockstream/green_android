package com.blockstream.compose.screens.add

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.box_arrow_down
import blockstream_green.common.generated.resources.flask_fill
import blockstream_green.common.generated.resources.id_added_already
import blockstream_green.common.generated.resources.id_archived_account
import blockstream_green.common.generated.resources.id_archived_accounts
import blockstream_green.common.generated.resources.id_asset
import blockstream_green.common.generated.resources.id_choose_security_policy
import blockstream_green.common.generated.resources.id_continue
import blockstream_green.common.generated.resources.id_experimental_feature
import blockstream_green.common.generated.resources.id_experimental_features_might
import blockstream_green.common.generated.resources.id_hide_advanced_options
import blockstream_green.common.generated.resources.id_show_advanced_options
import blockstream_green.common.generated.resources.id_there_is_already_an_archived
import blockstream_green.common.generated.resources.id_you_cannot_add_more_than_one
import com.blockstream.common.extensions.toggle
import com.blockstream.common.gdk.data.AssetBalance
import com.blockstream.common.looks.AccountTypeLook
import com.blockstream.common.models.add.ChooseAccountTypeViewModel
import com.blockstream.common.models.add.ChooseAccountTypeViewModelAbstract
import com.blockstream.common.navigation.NavigateDestinations
import com.blockstream.common.sideeffects.SideEffects
import com.blockstream.common.utils.StringHolder
import com.blockstream.compose.LocalDialog
import com.blockstream.compose.components.GreenAsset
import com.blockstream.compose.components.GreenButton
import com.blockstream.compose.components.GreenButtonType
import com.blockstream.compose.components.GreenCard
import com.blockstream.compose.components.OnProgressStyle
import com.blockstream.compose.extensions.drawDiagonalLabel
import com.blockstream.compose.screens.jade.JadeQRResult
import com.blockstream.compose.sideeffects.OpenDialogData
import com.blockstream.compose.theme.bodyMedium
import com.blockstream.compose.theme.labelLarge
import com.blockstream.compose.theme.labelMedium
import com.blockstream.compose.theme.lightning
import com.blockstream.compose.theme.md_theme_surfaceCircle
import com.blockstream.compose.theme.titleLarge
import com.blockstream.compose.theme.whiteHigh
import com.blockstream.compose.theme.whiteMedium
import com.blockstream.compose.utils.SetupScreen
import com.blockstream.compose.utils.roundBackground
import com.blockstream.ui.components.GreenArrow
import com.blockstream.ui.components.GreenColumn
import com.blockstream.ui.components.GreenRow
import com.blockstream.ui.navigation.getResult
import com.blockstream.ui.navigation.setResult
import com.blockstream.ui.utils.ifTrue
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun ChooseAccountTypeScreen(
    viewModel: ChooseAccountTypeViewModelAbstract
) {
    val dialog = LocalDialog.current
    val scope = rememberCoroutineScope()

    NavigateDestinations.Assets.getResult<AssetBalance> {
        viewModel.asset.value = it
    }

    NavigateDestinations.JadeQR.getResult<JadeQRResult> {
        viewModel.postEvent(ChooseAccountTypeViewModel.LocalEvents.CreateLightningAccount(it.result))
    }

    SetupScreen(viewModel = viewModel, sideEffectsHandler = {
        when (it) {
            is SideEffects.AccountCreated -> {
                NavigateDestinations.ReviewAddAccount.setResult(it.accountAsset)
            }

            is ChooseAccountTypeViewModel.LocalSideEffects.ExperimentalFeaturesDialog -> {
                launch {
                    dialog.openDialog(
                        OpenDialogData(
                            title = StringHolder.create(Res.string.id_experimental_feature),
                            message = StringHolder.create(Res.string.id_experimental_features_might),
                            icon = Res.drawable.flask_fill,
                            onPrimary = {
                                viewModel.postEvent(it.event)
                            }
                        )
                    )
                }
            }

            is ChooseAccountTypeViewModel.LocalSideEffects.ArchivedAccountDialog -> {
                launch {
                    dialog.openDialog(
                        OpenDialogData(
                            title = StringHolder.create(Res.string.id_archived_account),
                            message = StringHolder.create(Res.string.id_there_is_already_an_archived),
                            icon = Res.drawable.box_arrow_down,
                            primaryText = getString(Res.string.id_continue),
                            onPrimary = {
                                viewModel.postEvent(it.event)
                            },
                            secondaryText = getString(Res.string.id_archived_accounts),
                            onSecondary = {
                                viewModel.postEvent(
                                    NavigateDestinations.ArchivedAccounts(
                                        greenWallet = viewModel.greenWallet,
                                        navigateToRoot = true
                                    )
                                )
                            }
                        )
                    )
                }
            }
        }
    }, onProgressStyle = OnProgressStyle.Full(bluBackground = true)) {

        val asset by viewModel.asset.collectAsStateWithLifecycle()
        GreenAsset(
            modifier = Modifier.padding(bottom = 16.dp),
            assetBalance = asset, session = viewModel.sessionOrNull, title = stringResource(
                resource = Res.string.id_asset
            ), withEditIcon = viewModel.allowAssetSelection
        ) {
            if (viewModel.allowAssetSelection) {
                scope.launch {
                    viewModel.postEvent(
                        NavigateDestinations.Assets.create(
                            viewModel = viewModel
                        )
                    )
                }
            }
        }

        val accountTypes by viewModel.accountTypes.collectAsStateWithLifecycle()
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(Res.string.id_choose_security_policy),
                style = labelMedium,
                modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)
            )

            GreenColumn(
                padding = 0, space = 8,
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            ) {
                accountTypes.forEach {
                    AccountType(accountType = it, onClick = {
                        viewModel.postEvent(
                            ChooseAccountTypeViewModel.LocalEvents.ChooseAccountType(
                                it.accountType
                            )
                        )
                    })
                }
            }
        }

        val isShowingAdvancedOptions by viewModel.isShowingAdvancedOptions.collectAsStateWithLifecycle()
        val hasAdvancedOptions by viewModel.hasAdvancedOptions.collectAsStateWithLifecycle()

        if (hasAdvancedOptions) {
            GreenButton(
                text = stringResource(if (isShowingAdvancedOptions) Res.string.id_hide_advanced_options else Res.string.id_show_advanced_options),
                type = GreenButtonType.TEXT,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            ) {
                viewModel.isShowingAdvancedOptions.toggle()
            }
        }
    }
}

@Composable
fun AccountType(accountType: AccountTypeLook, onClick: () -> Unit = {}) {
    GreenCard(
        onClick = if (accountType.canBeAdded) onClick else null,
        padding = 0
    ) {

        Box(
            modifier = Modifier
                .ifTrue(!accountType.canBeAdded) {
                    it.alpha(0.2f).blur(4.dp)
                }
                .ifTrue(accountType.isLightning) {
                    it.drawDiagonalLabel(
                        text = "BETA", color = whiteHigh, style = TextStyle(
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        ),
                        labelTextRatio = 3f
                    )
                }) {

            Row {
                GreenColumn(modifier = Modifier.weight(1f), space = 8) {
                    GreenRow(padding = 0, space = 6) {
                        listOf(
                            stringResource(accountType.security).uppercase(),
                            stringResource(accountType.policy).uppercase()
                        ).forEachIndexed { index, s ->
                            val isLightning = (accountType.isLightning && index != 0)
                            Text(
                                text = s,
                                style = bodyMedium,
                                color = if (isLightning) whiteHigh else whiteMedium,
                                modifier = Modifier.roundBackground(
                                    horizontal = 6.dp,
                                    vertical = 2.dp,
                                    color = if (isLightning) lightning else md_theme_surfaceCircle
                                )
                            )
                        }
                    }

                    GreenColumn(padding = 0, space = 4) {
                        GreenRow(
                            padding = 0,
                            space = 4,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                stringResource(accountType.title),
                                style = titleLarge
                            )
                            Image(
                                painter = painterResource(accountType.icon()),
                                contentDescription = null,
                                modifier = Modifier
                                    .size(30.dp)
                            )
                        }
                        Text(
                            stringResource(accountType.description),
                            style = bodyMedium,
                            color = whiteMedium
                        )
                    }
                }

                GreenRow(modifier = Modifier.align(Alignment.CenterVertically)) {
                    GreenArrow()
                }
            }
        }

        if (!accountType.canBeAdded) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
            ) {
                GreenColumn(space = 6, horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = stringResource(Res.string.id_added_already), style = labelLarge)
                    Text(
                        text = stringResource(Res.string.id_you_cannot_add_more_than_one),
                        style = bodyMedium
                    )
                }
            }
        }
    }
}