package com.blockstream.compose.screens.lightning

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.id_enable_lightning
import blockstream_green.common.generated.resources.id_get_more_out_of_jade
import blockstream_green.common.generated.resources.id_global_permissionless_payments
import blockstream_green.common.generated.resources.id_instant_transactions
import blockstream_green.common.generated.resources.id_lightning_is_in_beta
import blockstream_green.common.generated.resources.id_lightning_network
import blockstream_green.common.generated.resources.id_low_fees
import blockstream_green.common.generated.resources.id_scaling_solution_for_faster
import blockstream_green.common.generated.resources.id_unlock_lightning
import blockstream_green.common.generated.resources.id_you_may_experience_bugs
import com.adamglin.PhosphorIcons
import com.adamglin.phosphoricons.Regular
import com.adamglin.phosphoricons.regular.Coins
import com.adamglin.phosphoricons.regular.Globe
import com.adamglin.phosphoricons.regular.Info
import com.adamglin.phosphoricons.regular.Lightning
import com.blockstream.compose.GreenPreview
import com.blockstream.compose.components.GreenAlert
import com.blockstream.compose.components.GreenButton
import com.blockstream.compose.components.GreenButtonSize
import com.blockstream.compose.components.GreenRow
import com.blockstream.compose.components.LearnMoreButton
import com.blockstream.compose.components.Rive
import com.blockstream.compose.components.RiveAnimation
import com.blockstream.compose.events.Events
import com.blockstream.compose.events.Events.OpenBrowser
import com.blockstream.compose.extensions.previewWallet
import com.blockstream.compose.models.lightning.LightningOnboardingViewModel
import com.blockstream.compose.models.lightning.LightningOnboardingViewModelAbstract
import com.blockstream.compose.navigation.NavigateDestinations
import com.blockstream.compose.navigation.getResult
import com.blockstream.compose.navigation.setResult
import com.blockstream.compose.screens.jade.JadeQRResult
import com.blockstream.compose.sideeffects.SideEffects
import com.blockstream.compose.theme.GreenColors
import com.blockstream.compose.theme.bodyMedium
import com.blockstream.compose.theme.headlineSmall
import com.blockstream.compose.theme.whiteMedium
import com.blockstream.compose.utils.SetupScreen
import com.blockstream.compose.utils.StringHolder
import com.blockstream.data.Urls
import com.blockstream.data.data.GreenWallet
import kotlinx.serialization.Serializable
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

@Serializable
sealed class LightningOnboardingResult {
    @Serializable
    data object HotWallet : LightningOnboardingResult()

    @Serializable
    data class Jade(val mnemonic: String) : LightningOnboardingResult()
}

@Composable
fun LightningOnboardingScreen(viewModel: LightningOnboardingViewModelAbstract) {
    val title = viewModel.title.string()
    val subtitle = viewModel.subtitle.string()
    val scrollState = rememberScrollState()

    NavigateDestinations.JadeQR.getResult<JadeQRResult> { result ->
        if (result.lightningMnemonic != null) {
            NavigateDestinations.LightningOnboarding.setResult(
                LightningOnboardingResult.Jade(result.lightningMnemonic)
            )
            viewModel.postEvent(Events.NavigateBack)
        }
    }

    NavigateDestinations.Login.getResult<GreenWallet> {
        viewModel.postEvent(LightningOnboardingViewModel.LocalEvents.EnableLightning)
    }

    SetupScreen(
        viewModel = viewModel,
        withPadding = false,
        sideEffectsHandler = {
            when (it) {
                is SideEffects.Success -> {
                    NavigateDestinations.LightningOnboarding.setResult(
                        LightningOnboardingResult.HotWallet
                    )
                    viewModel.postEvent(Events.NavigateBack)
                }
            }
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .padding(bottom = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = title,
                        style = headlineSmall,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text(
                        text = subtitle,
                        style = bodyMedium,
                        textAlign = TextAlign.Center,
                        color = GreenColors.onSurfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(230.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            // Scaling manually to match design
                            .graphicsLayer(
                                scaleX = 1.8f,
                                scaleY = 1.8f,
                            )
                    ) {
                        Rive(riveAnimation = RiveAnimation.LIGHTNING_SUCCESS)
                    }
                }

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.Start
                ) {
                    Box(modifier = Modifier.align(Alignment.CenterHorizontally)) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            OnboardingItem(
                                icon = PhosphorIcons.Regular.Coins,
                                text = stringResource(Res.string.id_low_fees)
                            )
                            OnboardingItem(
                                icon = PhosphorIcons.Regular.Lightning,
                                text = stringResource(Res.string.id_instant_transactions)
                            )
                            OnboardingItem(
                                icon = PhosphorIcons.Regular.Globe,
                                text = stringResource(Res.string.id_global_permissionless_payments)
                            )
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                GreenAlert(
                    modifier = Modifier.fillMaxWidth(),
                    title = stringResource(Res.string.id_lightning_is_in_beta),
                    message = stringResource(Res.string.id_you_may_experience_bugs),
                    isBlue = true,
                    icon = PhosphorIcons.Regular.Info
                )

                GreenButton(
                    size = GreenButtonSize.BIG,
                    modifier = Modifier.fillMaxWidth(),
                    text = stringResource(Res.string.id_enable_lightning),
                ) {
                    viewModel.postEvent(LightningOnboardingViewModel.LocalEvents.EnableLightning)
                }

                LearnMoreButton(
                    onClick = {
                        viewModel.postEvent(OpenBrowser(Urls.HELP_LIGHTNING_BETA))
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun OnboardingItem(icon: ImageVector, text: String) {
    GreenRow(space = 12, padding = 0) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = whiteMedium,
            modifier = Modifier.size(24.dp)
        )
        Text(
            text = text,
            style = bodyMedium,
            color = whiteMedium
        )
    }
}

private class LightningOnboardingViewModelPreview(isHardware: Boolean = false) :
    LightningOnboardingViewModelAbstract(previewWallet(isHardware = isHardware)) {

    override fun screenName(): String = "LightningOnboardingPreview"

    override val title = if (greenWallet.isHardware) {
        StringHolder.create(Res.string.id_get_more_out_of_jade)
    } else {
        StringHolder.create(Res.string.id_lightning_network)
    }

    override val subtitle = if (greenWallet.isHardware) {
        StringHolder.create(Res.string.id_unlock_lightning)
    } else {
        StringHolder.create(Res.string.id_scaling_solution_for_faster)
    }
}

@Preview(name = "Hot Wallet Mode")
@Composable
fun LightningOnboardingScreenHotPreview() {
    GreenPreview {
        LightningOnboardingScreen(viewModel = LightningOnboardingViewModelPreview(isHardware = false))
    }
}

@Preview(name = "Jade Mode")
@Composable
fun LightningOnboardingScreenJadePreview() {
    GreenPreview {
        LightningOnboardingScreen(viewModel = LightningOnboardingViewModelPreview(isHardware = true))
    }
}