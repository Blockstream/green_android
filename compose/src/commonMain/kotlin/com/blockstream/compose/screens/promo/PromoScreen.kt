package com.blockstream.compose.screens.promo

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import coil3.compose.AsyncImagePainter
import coil3.compose.rememberAsyncImagePainter
import com.blockstream.common.Parcelable
import com.blockstream.common.Parcelize
import com.blockstream.common.data.GreenWallet
import com.blockstream.common.data.Promo
import com.blockstream.common.events.Events
import com.blockstream.common.extensions.isNotBlank
import com.blockstream.common.models.promo.PromoViewModel
import com.blockstream.common.models.promo.PromoViewModelAbstract
import com.blockstream.compose.components.GreenButton
import com.blockstream.compose.components.GreenButtonSize
import com.blockstream.compose.components.GreenColumn
import com.blockstream.compose.theme.bodyMedium
import com.blockstream.compose.theme.titleMedium
import com.blockstream.compose.utils.AppBar
import com.blockstream.compose.utils.HandleSideEffect
import org.koin.core.parameter.parametersOf


@Parcelize
data class PromoScreen(val greenWallet: GreenWallet, val promo: Promo) : Screen, Parcelable {
    @Composable
    override fun Content() {
        val viewModel = koinScreenModel<PromoViewModel> {
            parametersOf(greenWallet, promo)
        }

        val navData by viewModel.navData.collectAsStateWithLifecycle()
        AppBar(navData)

        PromoScreen(viewModel = viewModel)
    }
}

@Composable
fun PromoScreen(
    viewModel: PromoViewModelAbstract
) {

    HandleSideEffect(viewModel)

    val promoOrNull by viewModel.promo.collectAsStateWithLifecycle()

    promoOrNull?.also { promo ->

        val showImage = remember { !viewModel.settingsManager.appSettings.tor && promo.imageLarge.isNotBlank() }

        val painter = rememberAsyncImagePainter(promo.imageLarge.takeIf { showImage })

        val state by painter.state.collectAsStateWithLifecycle()

        if (state is AsyncImagePainter.State.Success || state is AsyncImagePainter.State.Error) {
            GreenColumn {

                GreenColumn(
                    padding = 0,
                    modifier = Modifier.weight(1f)
                        .verticalScroll(
                            rememberScrollState()
                        )
                ) {
                    if (showImage && state is AsyncImagePainter.State.Success) {
                        Image(
                            painter = painter,
                            contentDescription = null,
                            modifier = Modifier.heightIn(max = 350.dp)
                                .align(Alignment.CenterHorizontally)
                        )
                    }

                    promo.titleLarge?.also {
                        Text(
                            text = it,
                            style = titleMedium,
                        )
                    }

                    promo.textLarge?.also { Text(it, style = bodyMedium) }
                }

                promo.ctaLarge?.also { ctaSmall ->
                    GreenButton(
                        text = ctaSmall,
                        size = GreenButtonSize.BIG,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        viewModel.postEvent(Events.PromoAction)
                    }
                }
            }
        } else {
            Box(modifier = Modifier.fillMaxSize()) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(120.dp),
                    color = MaterialTheme.colorScheme.secondary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                )
            }
        }
    }
}

