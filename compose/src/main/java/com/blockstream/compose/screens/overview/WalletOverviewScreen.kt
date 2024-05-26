package com.blockstream.compose.screens.overview

import android.Manifest
import android.os.Build
import android.view.LayoutInflater
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshContainer
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import androidx.core.util.TypedValueCompat.pxToDp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.rive.runtime.kotlin.RiveAnimationView
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.uniqueScreenKey
import cafe.adriel.voyager.koin.koinScreenModel
import com.arkivanov.essenty.parcelable.Parcelable
import com.arkivanov.essenty.parcelable.Parcelize
import com.blockstream.common.data.GreenWallet
import com.blockstream.common.data.ScanResult
import com.blockstream.common.events.Events
import com.blockstream.common.extensions.isNotBlank
import com.blockstream.common.models.SimpleGreenViewModel
import com.blockstream.common.models.archived.ArchivedAccountsViewModel
import com.blockstream.common.models.overview.WalletOverviewViewModel
import com.blockstream.common.models.overview.WalletOverviewViewModelAbstract
import com.blockstream.common.models.overview.WalletOverviewViewModelPreview
import com.blockstream.common.models.settings.DenominationExchangeRateViewModel
import com.blockstream.common.navigation.NavigateDestinations
import com.blockstream.common.sideeffects.SideEffects
import com.blockstream.compose.GreenPreview
import com.blockstream.compose.LocalRootNavigator
import com.blockstream.compose.R
import com.blockstream.compose.components.GreenAccountCard
import com.blockstream.compose.components.GreenAlert
import com.blockstream.compose.components.GreenButton
import com.blockstream.compose.components.GreenColumn
import com.blockstream.compose.components.GreenGradient
import com.blockstream.compose.components.GreenRow
import com.blockstream.compose.components.GreenSpacer
import com.blockstream.compose.components.GreenTransaction
import com.blockstream.compose.components.MenuEntry
import com.blockstream.compose.components.PopupMenu
import com.blockstream.compose.components.PopupState
import com.blockstream.compose.dialogs.AppRateDialog
import com.blockstream.compose.dialogs.ArchivedAccountsDialog
import com.blockstream.compose.dialogs.DenominationExchangeDialog
import com.blockstream.compose.extensions.assetIcon
import com.blockstream.compose.navigation.getNavigationResult
import com.blockstream.compose.navigation.resultKey
import com.blockstream.compose.sheets.CameraBottomSheet
import com.blockstream.compose.sheets.LocalBottomSheetNavigatorM3
import com.blockstream.compose.sheets.MainMenuBottomSheet
import com.blockstream.compose.sheets.MainMenuEntry
import com.blockstream.compose.theme.GreenSmallEnd
import com.blockstream.compose.theme.GreenSmallStart
import com.blockstream.compose.theme.GreenThemePreview
import com.blockstream.compose.theme.bodyLarge
import com.blockstream.compose.theme.bodyMedium
import com.blockstream.compose.theme.bottom_nav_bg
import com.blockstream.compose.theme.green
import com.blockstream.compose.theme.labelLarge
import com.blockstream.compose.theme.md_theme_background
import com.blockstream.compose.theme.titleLarge
import com.blockstream.compose.theme.titleSmall
import com.blockstream.compose.theme.whiteHigh
import com.blockstream.compose.theme.whiteLow
import com.blockstream.compose.theme.whiteMedium
import com.blockstream.compose.utils.AppBar
import com.blockstream.compose.utils.HandleSideEffect
import com.blockstream.compose.utils.noRippleClickable
import com.blockstream.compose.views.LightningInfo
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import kotlinx.coroutines.delay
import kotlinx.parcelize.IgnoredOnParcel
import org.koin.core.parameter.parametersOf


@Parcelize
data class WalletOverviewScreen(
    val greenWallet: GreenWallet,
) : Screen, Parcelable {
    @Composable
    override fun Content() {
        val viewModel = koinScreenModel<WalletOverviewViewModel>() {
            parametersOf(greenWallet)
        }

        val navData by viewModel.navData.collectAsStateWithLifecycle()
        AppBar(navData)

        WalletOverviewScreen(viewModel = viewModel)
    }

    @IgnoredOnParcel
    override val key = uniqueScreenKey
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun WalletOverviewScreen(
    viewModel: WalletOverviewViewModelAbstract
) {
    val bottomSheetNavigator = LocalBottomSheetNavigatorM3.current
    val navigator = LocalRootNavigator.current

    var denominationExchangeRateViewModel by remember {
        mutableStateOf<DenominationExchangeRateViewModel?>(null)
    }
    var appRateViewModel by remember {
        mutableStateOf<SimpleGreenViewModel?>(null)
    }
    var archivedAccountsViewModel by remember {
        mutableStateOf<ArchivedAccountsViewModel?>(null)
    }

    getNavigationResult<MainMenuEntry>(MainMenuBottomSheet.resultKey).value?.also {
        when (it) {
            MainMenuEntry.SCAN -> {
                viewModel.postEvent(
                    NavigateDestinations.Camera(
                        isDecodeContinuous = true,
                        parentScreenName = viewModel.screenName()
                    )
                )
            }

            MainMenuEntry.ACCOUNT_TRANSFER -> {
                viewModel.postEvent(NavigateDestinations.AccountExchange)
            }
        }
    }

    getNavigationResult<ScanResult>(CameraBottomSheet::class.resultKey).value?.also {
        viewModel.postEvent(Events.HandleUserInput(it.result, isQr = true))
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val requestPermissionLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            if (isGranted) {
                viewModel.postEvent(Events.NotificationPermissionGiven)
            } else {
                // Handle permission denial
            }
        }

        val notificatioPermissionState = rememberPermissionState(Manifest.permission.POST_NOTIFICATIONS)
        LaunchedEffect(notificatioPermissionState) {
            if (!notificatioPermissionState.status.isGranted && notificatioPermissionState.status.shouldShowRationale) {
                // Show rationale if needed
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    HandleSideEffect(viewModel = viewModel) {
        if (it is SideEffects.OpenDenominationExchangeRate) {
            denominationExchangeRateViewModel =
                DenominationExchangeRateViewModel(viewModel.greenWallet)
        } else if (it is SideEffects.AppReview) {
            appRateViewModel = SimpleGreenViewModel(viewModel.greenWallet)
        } else if(it is WalletOverviewViewModel.LocalSideEffects.AccountArchivedDialog) {
            archivedAccountsViewModel =
                ArchivedAccountsViewModel(viewModel.greenWallet).also {
                    if(navigator == null) {
                        it.parentViewModel = viewModel
                    }
                }
        }
    }

    denominationExchangeRateViewModel?.also {
        DenominationExchangeDialog(viewModel = it) {
            denominationExchangeRateViewModel = null
        }
    }

    appRateViewModel?.also {
        AppRateDialog(viewModel = it) {
            appRateViewModel = null
        }
    }

    archivedAccountsViewModel?.also {
        ArchivedAccountsDialog(viewModel = it) {
            archivedAccountsViewModel = null
        }
    }

    val state = rememberPullToRefreshState()
    if (state.isRefreshing) {
        LaunchedEffect(true) {
            viewModel.postEvent(WalletOverviewViewModel.LocalEvents.Refresh)
            delay(1500)
            state.endRefresh()
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .nestedScroll(state.nestedScrollConnection)
    ) {

        val isWalletOnboarding by viewModel.isWalletOnboarding.collectAsStateWithLifecycle()
        val accounts by viewModel.accounts.collectAsStateWithLifecycle()
        val alerts by viewModel.alerts.collectAsStateWithLifecycle()
        val lightningInfo by viewModel.lightningInfo.collectAsStateWithLifecycle()
        val transactions by viewModel.transactions.collectAsStateWithLifecycle()
        val displayMetrics = LocalContext.current.resources.displayMetrics

        LazyColumn(contentPadding = PaddingValues(bottom = 96.dp)) {
            if (!viewModel.isLightningShortcut) {
                item {
                    WalletBalance(viewModel)
                }
            }

            item {
                WalletAssets(viewModel = viewModel) {
                    viewModel.postEvent(NavigateDestinations.Assets)
                }
            }

            item {
                GreenSpacer()
            }

            if (!isWalletOnboarding) {

                items(alerts) {
                    GreenAlert(
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .padding(bottom = 6.dp), alertType = it, viewModel = viewModel
                    )
                }

                items(accounts) {
                    val popupState = remember {
                        PopupState()
                    }
                    val expandedAccount by viewModel.activeAccount.collectAsStateWithLifecycle()
                    var cardSize by remember {
                        mutableStateOf(IntSize.Zero)
                    }
                    val hasContextMenu = !viewModel.isLightningShortcut

                    Box {
                        GreenAccountCard(
                            modifier = Modifier
                                .padding(bottom = 1.dp)
                                .onSizeChanged {
                                    cardSize = it
                                },
                            account = it,
                            isExpanded = it.account.id == expandedAccount?.id,
                            session = viewModel.sessionOrNull,
                            onArrowClick = if (!viewModel.isLightningShortcut) {
                                {
                                    viewModel.postEvent(
                                        NavigateDestinations.AccountOverview(
                                            accountAsset = it.accountAsset
                                        )
                                    )
                                }
                            } else null,
                            onWarningClick = if (it.warningTwoFactor) {
                                {
                                    if (it.hasExpiredUtxos){
                                        viewModel.postEvent(NavigateDestinations.ReEnable2FA)
                                    }else{
                                        viewModel.postEvent(NavigateDestinations.EnableTwoFactor(it.account.network))
                                    }
                                }
                            } else null,
                            onClick = {
                                viewModel.postEvent(
                                    Events.SetAccountAsset(
                                        accountAsset = it.account.accountAsset,
                                        setAsActive = true
                                    )
                                )
                            }, onLongClick = {
                                if (hasContextMenu) {
                                    popupState.offset.value = DpOffset(
                                        pxToDp(it.x, displayMetrics).dp,
                                        -pxToDp(cardSize.height - it.y, displayMetrics).dp
                                    )
                                    popupState.isContextMenuVisible.value = true
                                }
                            }
                        )

                        if (hasContextMenu) {
                            PopupMenu(
                                state = popupState,
                                entries = if (it.account.isLightning) listOf(
                                    MenuEntry(
                                        title = stringResource(id = R.string.id_remove),
                                        iconRes = R.drawable.trash,
                                        onClick = {
                                            viewModel.postEvent(Events.RemoveAccount(it.account))
                                        }
                                    )
                                ) else listOfNotNull(
                                    MenuEntry(
                                        title = stringResource(id = R.string.id_rename_account),
                                        iconRes = R.drawable.text_aa,
                                        onClick = {
                                            viewModel.postEvent(NavigateDestinations.RenameAccount(it.account))
                                        }
                                    ),
                                    if (viewModel.accounts.value.size > 1) {
                                        MenuEntry(
                                            title = stringResource(id = R.string.id_archive_account),
                                            iconRes = R.drawable.box_arrow_down,
                                            onClick = {
                                                viewModel.postEvent(Events.ArchiveAccount(it.account))
                                            }
                                        )
                                    } else null
                                )
                            )
                        }
                    }
                }

//            item {
//                val expandedAccount by viewModel.session.activeAccount.collectAsStateWithLifecycle()
//                AnimatedVisibility(visible = accounts.isNotEmpty()) {
//                    GreenColumn(
//                        padding = 0,
//                        space = 1,
//                        modifier = Modifier.padding(vertical = 8.dp)
//                    ) {
//                        accounts.forEach {
//                            GreenAccountCard(
//                                modifier = Modifier.padding(bottom = 1.dp),
//                                accountBalance = it,
//                                isExpanded = it.account.id == expandedAccount?.id,
//                                session = viewModel.sessionOrNull,
//                                onArrowClick = {
//
//                                }
//                            ) {
//                                viewModel.postEvent(
//                                    Events.SetAccountAsset(
//                                        accountAsset = it.account.accountAsset,
//                                        setAsActive = true
//                                    )
//                                )
//                            }
//                        }
//                    }
//                }
//            }

                lightningInfo?.also { lightningInfo ->
                    item {
                        LightningInfo(lightningInfoLook = lightningInfo, onLearnMore = {
                            viewModel.postEvent(WalletOverviewViewModel.LocalEvents.ClickLightningLearnMore)
                        })
                    }
                }

                item {
                    Text(
                        text = stringResource(R.string.id_latest_transactions),
                        style = titleSmall,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .padding(top = 16.dp)
                            .padding(bottom = 8.dp)
                    )

                    if(transactions.isLoading()) {
                        LinearProgressIndicator(
                            modifier = Modifier
                                .padding(all = 32.dp)
                                .height(1.dp)
                                .fillMaxWidth()
                        )
                    } else if (transactions.isEmpty()) {
                        Text(
                            text = stringResource(R.string.id_your_transactions_will_be_shown),
                            style = bodyMedium,
                            textAlign = TextAlign.Center,
                            fontStyle = FontStyle.Italic,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 24.dp)
                                .padding(horizontal = 16.dp)
                        )
                    }
                }

                transactions.data()?.also {
                    items(it) { item ->
                        GreenTransaction(transactionLook = item) {
                            viewModel.postEvent(Events.Transaction(transaction = item.transaction))
                        }
                    }
                }
            }
        }

        PullToRefreshContainer(
            modifier = Modifier.align(Alignment.TopCenter),
            state = state,
        )

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
        ) {
            GreenGradient(
                modifier = Modifier
                    .align(Alignment.BottomCenter),
                size = 76
            )

            BottomNav(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 16.dp),
                isWatchOnly = viewModel.session.isWatchOnly,
                isSweepEnabled = viewModel.session.defaultNetworkOrNull?.isBitcoin == true,
                onSendClick = {
                    viewModel.postEvent(WalletOverviewViewModel.LocalEvents.Send)
                }, onReceiveClick = {
                    viewModel.postEvent(WalletOverviewViewModel.LocalEvents.Receive)
                }, onCircleClick = {
                    bottomSheetNavigator.show(MainMenuBottomSheet)
                }
            )
        }

        if (isWalletOnboarding) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .noRippleClickable {
                        // catch all clicks
                    }
                    .background(md_theme_background.copy(alpha = 0.9f))
            ) {

                GreenColumn(
                    padding = 32,
                    space = 16,
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {

                    if (!LocalInspectionMode.current) {
                        AndroidView({
                            LayoutInflater.from(it)
                                .inflate(R.layout.rive, null)
                                .apply {
                                    val animationView: RiveAnimationView = findViewById(R.id.rive)
                                    animationView.setRiveResource(
                                        R.raw.wallet,
                                        autoplay = true
                                    )
                                }
                        })
                    }

                    Text(
                        text = stringResource(id = R.string.id_welcome_to_your_wallet),
                        style = titleLarge,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )

                    Text(
                        text = stringResource(id = R.string.id_create_your_first_account_to),
                        style = bodyLarge,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )

                    GreenButton(text = stringResource(R.string.id_create_account)) {
                        viewModel.postEvent(Events.ChooseAccountType(isFirstAccount = true))
                    }
                }
            }
        }
    }
}

@Composable
fun WalletBalance(viewModel: WalletOverviewViewModelAbstract) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = stringResource(id = R.string.id_total_balance), color = whiteMedium)
                val hideAmounts by viewModel.hideAmounts.collectAsStateWithLifecycle()
                Icon(
                    painter = painterResource(id = if (hideAmounts) R.drawable.eye_slash else R.drawable.eye),
                    contentDescription = null,
                    modifier = Modifier
                        .noRippleClickable {
                            viewModel.postEvent(WalletOverviewViewModel.LocalEvents.ToggleHideAmounts)
                        }
                        .padding(horizontal = 8.dp)
                        .align(Alignment.CenterVertically)
                )
            }

            Box {
                val balancePrimary by viewModel.balancePrimary.collectAsStateWithLifecycle()
                Column(modifier = Modifier.noRippleClickable {
                    viewModel.postEvent(WalletOverviewViewModel.LocalEvents.ToggleBalance)
                }) {

                    Text(
                        text = balancePrimary.takeIf { it.isNotBlank() } ?: " ",
                        color = whiteHigh,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold
                    )
                    val balanceSecondary by viewModel.balanceSecondary.collectAsStateWithLifecycle()
                    Text(text = balanceSecondary.takeIf { it.isNotBlank() } ?: " ",
                        color = whiteMedium,
                        style = bodyLarge)
                }

                androidx.compose.animation.AnimatedVisibility(
                    visible = balancePrimary == null,
                    modifier = Modifier.align(Alignment.Center)
                ) {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .height(1.dp)
                            .padding(horizontal = 32.dp)
                            .fillMaxWidth()
                    )
                }
            }
        }

        Icon(
            painter = painterResource(id = R.drawable.coins),
            contentDescription = null,
            tint = whiteLow,
            modifier = Modifier
                .clickable {
                    viewModel.postEvent(WalletOverviewViewModel.LocalEvents.DenominationExchangeRate)
                }
                .padding(8.dp)
                .align(Alignment.CenterVertically)
        )
    }
}

@Composable
fun WalletAssets(viewModel: WalletOverviewViewModelAbstract, onClick: () -> Unit = {}) {
    val totalAssets by viewModel.totalAssets.collectAsStateWithLifecycle()
    val assetIcons by viewModel.assetIcons.collectAsStateWithLifecycle()
    val assetsVisibility by viewModel.assetsVisibility.collectAsStateWithLifecycle()

    AnimatedVisibility(visible = assetsVisibility != false) {
        GreenRow(
            padding = 0,
            space = 8,
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .padding(top = 4.dp)
        ) {
            Box(modifier = Modifier
                .height(24.dp)
                .clickable {
                    onClick.invoke()
                }) {
                assetIcons.forEachIndexed { index, asset ->
                    val size = 24
                    val padding = (size / (1.5 + (0.1 * index)) * index)

                    Image(
                        painter = asset.assetIcon(session = viewModel.sessionOrNull),
                        contentDescription = null,
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(end = padding.dp)
                            .size(size.dp)
                            .clip(CircleShape)
                            .align(Alignment.BottomEnd)
                            .border(1.dp, Color.Black, CircleShape)
                            .zIndex(-index.toFloat())
                    )
                }
            }

            AnimatedVisibility(visible = assetsVisibility == true) {
                Text(
                    text = stringResource(R.string.id_d_assets_in_total, totalAssets),
                    style = labelLarge.copy(
                        textDecoration = TextDecoration.Underline
                    ),
                    color = green,
                    modifier = Modifier.clickable {
                        onClick.invoke()
                    }
                )
            }
        }
    }
}

@Composable
fun BottomNav(
    modifier: Modifier = Modifier,
    isWatchOnly: Boolean = false,
    isSweepEnabled: Boolean = false,
    onSendClick: () -> Unit,
    onReceiveClick: () -> Unit,
    onCircleClick: () -> Unit
) {

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .then(modifier)
    ) {
        Row(
            modifier = Modifier.align(Alignment.Center),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {

            Card(
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp),
                shape = GreenSmallEnd,
                colors = CardDefaults.cardColors(containerColor = bottom_nav_bg),
                onClick = onSendClick,
                enabled = !isWatchOnly || isSweepEnabled
            ) {
                GreenRow(
                    padding = 0,
                    space = 8,
                    modifier = Modifier
                        .weight(1f)
                        .align(Alignment.CenterHorizontally)
                        .padding(end = 30.dp)
                ) {
                    Icon(
                        painterResource(if (isWatchOnly && isSweepEnabled) R.drawable.broom else R.drawable.arrow_line_up),
                        contentDescription = null,
                        tint = green,
                    )
                    Text(text = stringResource(if (isWatchOnly && isSweepEnabled) R.string.id_sweep else R.string.id_send))
                }
            }

            Card(
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp),
                shape = GreenSmallStart,
                colors = CardDefaults.cardColors(containerColor = bottom_nav_bg),
                onClick = onReceiveClick
            ) {
                GreenRow(
                    padding = 0,
                    space = 8,
                    modifier = Modifier
                        .weight(1f)
                        .align(Alignment.CenterHorizontally)
                        .padding(start = 30.dp)
                ) {
                    Icon(
                        painterResource(id = R.drawable.arrow_line_down),
                        contentDescription = null,
                        tint = green
                    )
                    Text(text = stringResource(id = R.string.id_receive))
                }
            }
        }

        FloatingActionButton(
            modifier = Modifier
                .align(Alignment.Center)
                .size(60.dp),
            shape = CircleShape,
            onClick = onCircleClick,
        ) {
            Icon(Icons.Filled.Add, "Floating action button.")
        }
    }
}

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

            BottomNav(modifier = Modifier, isSweepEnabled = false, isWatchOnly = true, onSendClick = {

            }, onReceiveClick = {

            }, onCircleClick = {

            })
        }
    }
}

@Composable
@Preview
fun WalletBalancePreview() {
    GreenThemePreview {
        WalletBalance(viewModel = WalletOverviewViewModelPreview.create())
    }
}

@Composable
@Preview
fun WalletAssetsPreview() {
    GreenThemePreview {
        WalletAssets(viewModel = WalletOverviewViewModelPreview.create())
    }
}

@Composable
@Preview
fun WalletOverviewPreview() {
    GreenPreview {
        WalletOverviewScreen(viewModel = WalletOverviewViewModelPreview.create())
    }
}

@Composable
@Preview
fun WalletOverviewEmptyPreview() {
    GreenPreview {
        WalletOverviewScreen(viewModel = WalletOverviewViewModelPreview.create(true))
    }
}