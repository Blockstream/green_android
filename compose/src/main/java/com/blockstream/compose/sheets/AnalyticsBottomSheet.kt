package com.blockstream.compose.sheets

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults.elevatedCardColors
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.koin.koinScreenModel
import com.blockstream.common.models.sheets.AnalyticsViewModel
import com.blockstream.common.models.sheets.AnalyticsViewModelAbstract
import com.blockstream.common.models.sheets.AnalyticsViewModelPreview
import com.blockstream.compose.GreenPreview
import com.blockstream.compose.R
import com.blockstream.compose.components.GreenButton
import com.blockstream.compose.components.GreenButtonSize
import com.blockstream.compose.components.GreenButtonType
import com.blockstream.compose.components.GreenCircle
import com.blockstream.compose.components.GreenColumn
import com.blockstream.compose.components.GreenRow
import com.blockstream.compose.components.GreenSpacer
import com.blockstream.compose.navigation.resultKey
import com.blockstream.compose.navigation.setNavigationResult
import com.blockstream.compose.theme.whiteHigh
import com.blockstream.compose.utils.HandleSideEffect
import com.blockstream.compose.components.GreenBottomSheet


object AnalyticsBottomSheet : BottomScreen() {
    @Composable
    override fun Content() {
        val viewModel = koinScreenModel<AnalyticsViewModel>()

        AnalyticsBottomSheet(
            viewModel = viewModel,
            onDismissRequest = onDismissRequest {
                setNavigationResult(resultKey, true)
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AnalyticsBottomSheet(
    viewModel: AnalyticsViewModelAbstract,
    onDismissRequest: () -> Unit,
) {
    GreenBottomSheet(
        title = stringResource(id = R.string.id_help_green_improve),
        viewModel = viewModel,
        sheetState = rememberModalBottomSheetState(
            skipPartiallyExpanded = true,
            confirmValueChange = {
                !viewModel.showActionButtons
            }
        ),
        onDismissRequest = onDismissRequest
    ) {

        var isExpanded by remember {
            mutableStateOf(false)
        }

        HandleSideEffect(viewModel = viewModel)

        Text(text = stringResource(R.string.id_if_you_agree_green_will_collect))

        Card(
            colors = elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.inverseSurface,
            )
        ) {

            Column {
                Row(modifier = Modifier
                    .clickable {
                        isExpanded = !isExpanded
                    }
                    .padding(16.dp), verticalAlignment = Alignment.CenterVertically) {

                    AnimatedContent(
                        modifier = Modifier.weight(1f),
                        targetState = stringResource(if (isExpanded) R.string.id_hide_details else R.string.id_show_details),
                        transitionSpec = {
                            fadeIn().togetherWith(fadeOut())
                        }, label = "Show/Hide Label"
                    ) {
                        Text(
                            text = it,
                        )
                    }

                    val rotation: Float by animateFloatAsState(
                        if (isExpanded) 180f else 0f,
                        label = "ArrowDropDown"
                    )
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = "Expand",
                        modifier = Modifier.rotate(rotation)
                    )
                }

                AnimatedVisibility(visible = isExpanded) {

                    HorizontalDivider()

                    GreenColumn(space = 8) {
                        Text(stringResource(R.string.id_whats_collected))
                        GreenRow(
                            space = 6,
                            padding = 0,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            GreenCircle(size = 4, color = whiteHigh)
                            Text(stringResource(R.string.id_page_visits_button_presses))
                        }
                        GreenRow(
                            space = 6,
                            padding = 0,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            GreenCircle(size = 4, color = whiteHigh)
                            Text(stringResource(R.string.id_os__app_version_loading_times))
                        }

                        GreenSpacer(16)

                        Text(stringResource(R.string.id_whats_not_collected))
                        GreenRow(
                            space = 6,
                            padding = 0,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            GreenCircle(size = 4, color = whiteHigh)
                            Text(stringResource(R.string.id_recovery_phrases_key_material))
                        }
                        GreenRow(
                            space = 6,
                            padding = 0,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            GreenCircle(size = 4, color = whiteHigh)
                            Text(stringResource(R.string.id_user_contact_info_ip_address))
                        }

                        GreenButton(
                            text = stringResource(R.string.id_learn_more),
                            type = GreenButtonType.TEXT,
                            size = GreenButtonSize.SMALL
                        ) {
                            viewModel.postEvent(AnalyticsViewModel.LocalEvents.ClickLearnMore)
                        }
                    }
                }
            }
        }

        if (viewModel.showActionButtons) {
            GreenColumn(padding = 0, space = 8) {
                GreenButton(
                    text = stringResource(R.string.id_allow_data_collection),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    viewModel.postEvent(AnalyticsViewModel.LocalEvents.ClickDataCollection(true))
                }

                GreenButton(
                    text = stringResource(R.string.id_dont_collect_data),
                    modifier = Modifier.fillMaxWidth(),
                    type = GreenButtonType.TEXT
                ) {
                    viewModel.postEvent(AnalyticsViewModel.LocalEvents.ClickDataCollection(false))
                }
            }
        }
    }
}

@Composable
@Preview
fun AnalyticsBottomSheetPreview() {
    GreenPreview {
        AnalyticsBottomSheet(
            viewModel = AnalyticsViewModelPreview.preview(),
            onDismissRequest = { }
        )
    }
}