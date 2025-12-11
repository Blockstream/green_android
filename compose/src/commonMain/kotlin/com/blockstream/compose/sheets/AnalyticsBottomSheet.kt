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
import androidx.compose.ui.unit.dp
import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.id_allow_data_collection
import blockstream_green.common.generated.resources.id_dont_collect_data
import blockstream_green.common.generated.resources.id_help_us_improve
import blockstream_green.common.generated.resources.id_hide_details
import blockstream_green.common.generated.resources.id_if_you_agree_blockstream_will
import blockstream_green.common.generated.resources.id_learn_more
import blockstream_green.common.generated.resources.id_os__app_version_loading_times
import blockstream_green.common.generated.resources.id_page_visits_button_presses
import blockstream_green.common.generated.resources.id_recovery_phrases_key_material
import blockstream_green.common.generated.resources.id_show_details
import blockstream_green.common.generated.resources.id_user_contact_info_ip_address
import blockstream_green.common.generated.resources.id_whats_collected
import blockstream_green.common.generated.resources.id_whats_not_collected
import com.blockstream.common.models.sheets.AnalyticsViewModel
import com.blockstream.common.models.sheets.AnalyticsViewModelAbstract
import com.blockstream.common.sideeffects.SideEffects
import com.blockstream.compose.components.GreenBottomSheet
import com.blockstream.compose.components.GreenButton
import com.blockstream.compose.components.GreenButtonSize
import com.blockstream.compose.components.GreenButtonType
import com.blockstream.compose.theme.whiteHigh
import com.blockstream.compose.utils.HandleSideEffect
import com.blockstream.compose.components.GreenCircle
import com.blockstream.compose.components.GreenColumn
import com.blockstream.compose.components.GreenRow
import com.blockstream.compose.components.GreenSpacer
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AnalyticsBottomSheet(
    viewModel: AnalyticsViewModelAbstract,
    onDismissRequest: () -> Unit,
) {
    GreenBottomSheet(
        title = stringResource(Res.string.id_help_us_improve),
        viewModel = viewModel,
        sheetState = rememberModalBottomSheetState(
            skipPartiallyExpanded = true,
            confirmValueChange = {
                !viewModel.isActionRequired
            }
        ),
        onDismissRequest = onDismissRequest
    ) {

        var isExpanded by remember {
            mutableStateOf(false)
        }

        HandleSideEffect(viewModel = viewModel) {
            if (it is SideEffects.Dismiss) {
                onDismissRequest()
            }
        }

        Text(text = stringResource(Res.string.id_if_you_agree_blockstream_will))

        Card(
            colors = elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.inverseSurface,
            )
        ) {

            Column {
                Row(
                    modifier = Modifier
                        .clickable {
                            isExpanded = !isExpanded
                        }
                        .padding(16.dp), verticalAlignment = Alignment.CenterVertically) {

                    AnimatedContent(
                        modifier = Modifier.weight(1f),
                        targetState = stringResource(if (isExpanded) Res.string.id_hide_details else Res.string.id_show_details),
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
                        Text(stringResource(Res.string.id_whats_collected))
                        GreenRow(
                            space = 6,
                            padding = 0,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            GreenCircle(size = 4, color = whiteHigh)
                            Text(stringResource(Res.string.id_page_visits_button_presses))
                        }
                        GreenRow(
                            space = 6,
                            padding = 0,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            GreenCircle(size = 4, color = whiteHigh)
                            Text(stringResource(Res.string.id_os__app_version_loading_times))
                        }

                        GreenSpacer(16)

                        Text(stringResource(Res.string.id_whats_not_collected))
                        GreenRow(
                            space = 6,
                            padding = 0,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            GreenCircle(size = 4, color = whiteHigh)
                            Text(stringResource(Res.string.id_recovery_phrases_key_material))
                        }
                        GreenRow(
                            space = 6,
                            padding = 0,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            GreenCircle(size = 4, color = whiteHigh)
                            Text(stringResource(Res.string.id_user_contact_info_ip_address))
                        }

                        GreenButton(
                            text = stringResource(Res.string.id_learn_more),
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
                    text = stringResource(Res.string.id_allow_data_collection),
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "allow_analytics"
                ) {
                    viewModel.postEvent(AnalyticsViewModel.LocalEvents.ClickDataCollection(true))
                }

                GreenButton(
                    text = stringResource(Res.string.id_dont_collect_data),
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "deny_analytics",
                    type = GreenButtonType.TEXT
                ) {
                    viewModel.postEvent(AnalyticsViewModel.LocalEvents.ClickDataCollection(false))
                }
            }
        }
    }
}
