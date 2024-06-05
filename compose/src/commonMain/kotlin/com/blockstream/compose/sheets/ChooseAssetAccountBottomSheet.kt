package com.blockstream.compose.sheets

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.koin.koinScreenModel
import com.arkivanov.essenty.parcelable.IgnoredOnParcel
import com.blockstream.common.BTC_POLICY_ASSET
import com.blockstream.common.Parcelable
import com.blockstream.common.Parcelize
import com.blockstream.common.data.EnrichedAsset
import com.blockstream.common.data.GreenWallet
import com.blockstream.common.extensions.isPolicyAsset
import com.blockstream.common.gdk.data.Account
import com.blockstream.common.gdk.data.AccountAsset
import com.blockstream.common.gdk.data.AccountAssetBalance
import com.blockstream.common.gdk.data.AccountType
import com.blockstream.common.gdk.data.AssetBalance
import com.blockstream.common.models.GreenViewModel
import com.blockstream.common.models.SimpleGreenViewModel
import com.blockstream.common.navigation.NavigateDestinations
import com.blockstream.compose.LocalRootNavigator
import com.blockstream.compose.components.GreenAccountAsset
import com.blockstream.compose.components.GreenAssetAccounts
import com.blockstream.compose.components.GreenBottomSheet
import com.blockstream.compose.components.GreenColumn
import com.blockstream.compose.navigation.getNavigationResult
import com.blockstream.compose.navigation.setNavigationResult
import org.koin.core.parameter.parametersOf
import kotlin.jvm.Transient

@Parcelize
data class ChooseAssetAccountBottomSheet(
    val greenWallet: GreenWallet
) : BottomScreen(), Parcelable {

    // Temp fix until fully migration to Compose
    @Transient
    @IgnoredOnParcel
    var parentViewModel: GreenViewModel? = null

    @Composable
    override fun Content() {
        val viewModel = koinScreenModel<SimpleGreenViewModel> {
            parametersOf(greenWallet, null, "ChooseAssetAndAccount")
        }.also {
            val navigator = LocalRootNavigator.current
            if(navigator == null) {
                it.parentViewModel = parentViewModel
            }
        }

        ChooseAssetAccountBottomSheet(
            viewModel = viewModel,
            onDismissRequest = onDismissRequest()
        )
    }

    companion object {
        @Composable
        fun getResult(fn: (AccountAsset) -> Unit) =
            getNavigationResult(this::class, fn)

        internal fun setResult(result: AccountAsset) =
            setNavigationResult(this::class, result)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChooseAssetAccountBottomSheet(
    viewModel: GreenViewModel,
    onDismissRequest: () -> Unit,
) {
    var enrichedAssets by remember {
        mutableStateOf(listOf<Pair<EnrichedAsset, List<Account>>>())
    }

    var expanded by remember {
        mutableStateOf<EnrichedAsset?>(null)
    }

    LaunchedEffect(Unit) {
        enrichedAssets = (setOfNotNull(
            EnrichedAsset.createOrNull(
                session = viewModel.session,
                viewModel.session.bitcoin?.policyAsset
            ),
            EnrichedAsset.createOrNull(
                session = viewModel.session,
                viewModel.session.liquid?.policyAsset
            ),
        ) + viewModel.session.walletAssets.value.assets.keys.map {
            EnrichedAsset.create(session = viewModel.session, assetId = it)
        }
            .toSet() + (viewModel.session.enrichedAssets.value.takeIf { viewModel.session.liquid != null }
            ?.map {
                EnrichedAsset.create(session = viewModel.session, assetId = it.assetId)
            } ?: setOf()) + setOfNotNull(
            EnrichedAsset.createAnyAsset(session = viewModel.session, isAmp = false),
            EnrichedAsset.createAnyAsset(session = viewModel.session, isAmp = true)
        )).sortedWith(viewModel.session::sortEnrichedAssets).let {
            it.map {
                it to viewModel.session.accounts.value.filter { account ->
                    if (it.isAnyAsset && it.isAmp) {
                        account.type == AccountType.AMP_ACCOUNT
                    } else if (it.assetId.isPolicyAsset(viewModel.session)) {
                        account.network.policyAsset == it.assetId
                    } else {
                        account.isLiquid && (it.isAmp == account.isAmp || it.isAnyAsset)
                    }
                }
            }
        }

        // Expand Bitcoin Asset if wallet is BTC only
        if(!viewModel.session.hasLiquidAccount){
            expanded = enrichedAssets.firstOrNull()?.first
        }
    }

    GreenBottomSheet(
        sheetState = rememberModalBottomSheetState(
            skipPartiallyExpanded = false
        ),
        viewModel = viewModel,
        onDismissRequest = onDismissRequest
    ) {
        GreenColumn(
            padding = 0, space = 4, modifier = Modifier
                .padding(top = 16.dp)
                .verticalScroll(
                    rememberScrollState()
                )
        ) {
            enrichedAssets.forEach {
                GreenAssetAccounts(
                    asset = it.first,
                    accounts = it.second,
                    session = viewModel.sessionOrNull,
                    isExpanded = expanded == it.first,
                    onAccountClick = {
                        ChooseAssetAccountBottomSheet.setResult(it)
                        onDismissRequest()
                    },
                    onExpandClick = {
                        expanded = it
                    },
                    onCreateNewAccount = {
                        viewModel.postEvent(
                            NavigateDestinations.ChooseAccountType(
                                assetBalance = AssetBalance.create(it),
                                isReceive = true
                            )
                        )
                        onDismissRequest()
                    })
            }
        }

    }
}
