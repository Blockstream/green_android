package com.blockstream.compose.screens.overview.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.bitcoin
import blockstream_green.common.generated.resources.id_buy_now
import blockstream_green.common.generated.resources.id_data_not_available
import blockstream_green.common.generated.resources.id_retry
import com.adamglin.PhosphorIcons
import com.adamglin.phosphoricons.Regular
import com.adamglin.phosphoricons.regular.TrendDown
import com.adamglin.phosphoricons.regular.TrendUp
import com.blockstream.common.btcpricehistory.model.BitcoinChartData
import com.blockstream.common.btcpricehistory.model.BitcoinChartPeriod
import com.blockstream.common.data.DataState
import com.blockstream.common.utils.DecimalFormat
import com.blockstream.compose.components.GreenButton
import com.blockstream.compose.components.GreenButtonSize
import com.blockstream.compose.theme.bodyMedium
import com.blockstream.compose.theme.green20
import com.blockstream.compose.theme.labelMedium
import com.blockstream.compose.theme.red
import com.blockstream.compose.theme.titleSmall
import com.blockstream.compose.theme.whiteLow
import com.blockstream.compose.utils.appTestTag
import com.blockstream.compose.utils.composeIf
import io.github.koalaplot.core.ChartLayout
import io.github.koalaplot.core.legend.LegendLocation
import io.github.koalaplot.core.line.AreaBaseline
import io.github.koalaplot.core.line.AreaPlot
import io.github.koalaplot.core.style.AreaStyle
import io.github.koalaplot.core.style.LineStyle
import io.github.koalaplot.core.util.ExperimentalKoalaPlotApi
import io.github.koalaplot.core.xygraph.AxisStyle
import io.github.koalaplot.core.xygraph.FloatLinearAxisModel
import io.github.koalaplot.core.xygraph.Point
import io.github.koalaplot.core.xygraph.XYGraph
import kotlinx.coroutines.flow.StateFlow
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import kotlin.math.absoluteValue

@Composable
fun BitcoinPriceChart(
    pricesState: StateFlow<DataState<BitcoinChartData>?>,
    onClickRetry: () -> Unit,
    onClickBuyNow: () -> Unit,
) {
    val chartPrices by pricesState.collectAsStateWithLifecycle()
    var chartPeriod by remember { mutableStateOf(BitcoinChartPeriod.ONE_DAY) }

    val currentPrice = chartPrices?.data()?.currentPrice
    val currentPeriodPrices = chartPrices?.data()?.prices?.get(chartPeriod) ?: emptyList()
    val chartDataStartPrice = currentPeriodPrices.firstOrNull()?.second ?: 0f
    val currency = chartPrices?.data()?.currency ?: ""


    Column(
        modifier = Modifier.background(
            color = MaterialTheme.colorScheme.surface,
        ).border(1.dp, SolidColor(MaterialTheme.colorScheme.outline), RoundedCornerShape(6.dp))
            .padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {

        BitcoinPrice(
            startPrice = chartDataStartPrice,
            currentPrice = currentPrice ?: -1f,
            currency = currency
        )

        when (chartPrices) {
            is DataState.Error -> {
                RetryButton(onClick = onClickRetry)
            }

            else -> {
                XYChartLayout(currentPeriodPrices)
            }
        }

        ChartPeriodSelector(chartPeriod, {
            chartPeriod = it
        })

        GreenButton(
            text = stringResource(Res.string.id_buy_now),
            onClick = onClickBuyNow,
            modifier = Modifier.fillMaxWidth()
                .appTestTag("buy_now_button"),
            size = GreenButtonSize.BIG,
        )
    }
}

@Composable
private fun BitcoinPrice(startPrice: Float, currentPrice: Float, currency: String) {
    val changePercentageInPrice = (currentPrice - startPrice) / startPrice * 100
    val isTrendUp = changePercentageInPrice >= 0

    val trendIcon = if (isTrendUp) {
        PhosphorIcons.Regular.TrendUp
    } else {
        PhosphorIcons.Regular.TrendDown
    }
    val trendColor = if (isTrendUp) {
        green20.copy(alpha = 1f)
    } else {
        red
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                modifier = Modifier.size(32.dp).padding(2.dp),
                painter = painterResource(Res.drawable.bitcoin),
                contentDescription = "bitcoin-icon"
            )
            Text(text = "Bitcoin", style = titleSmall)
        }

        composeIf(currentPrice >= 0f) { // only render if current price is available, setting -1f in case its null (loading state)
            Column(horizontalAlignment = Alignment.End) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = changePercentageInPrice.formatPercentage(),
                        style = labelMedium.copy(color = trendColor, lineHeight = 16.sp),
                        modifier = Modifier.appTestTag("percentage_change_text")

                    )
                    Icon(
                        modifier = Modifier.size(16.dp),
                        imageVector = trendIcon,
                        contentDescription = "trend-icon",
                        tint = trendColor
                    )
                }

                Text(text = currentPrice.formatFiatPrice(currency),
                    style = bodyMedium,
                    modifier = Modifier.appTestTag("price_value_text")
                )
            }
        }
    }
}

@OptIn(ExperimentalKoalaPlotApi::class)
@Composable
private fun XYChartLayout(chartPrices: List<Pair<Long, Float>>) {

    if (chartPrices.isEmpty()) {
        Spacer(modifier = Modifier.height(100.dp)) // Placeholder for empty chart
        return
    }

    val prices = remember(chartPrices) { chartPrices.map { it.second } }

    val minPrice = prices.min()
    val maxPrice = prices.max()

    val horizontalPadding = prices.size * 0.005f
    val minX = -horizontalPadding
    val maxX = prices.size.toFloat() + horizontalPadding

    val data = prices.mapIndexed { index, price ->
        Point(
            x = index.toFloat(),
            y = price,
        )
    }
    ChartLayout(
        modifier = Modifier.height(100.dp), legendLocation = LegendLocation.NONE
    ) {
        XYGraph(
            xAxisModel = FloatLinearAxisModel(minX..maxX),
            yAxisModel = FloatLinearAxisModel(minPrice..maxPrice),

            xAxisLabels = { "" },
            yAxisLabels = { "" },
            xAxisTitle = null,
            yAxisTitle = null,

            xAxisStyle = AxisStyle(
                color = Color.Transparent, minorTickSize = 0.dp, lineWidth = 0.dp
            ),
            yAxisStyle = AxisStyle(
                color = Color.Transparent, minorTickSize = 0.dp, lineWidth = 0.dp
            ),

            horizontalMajorGridLineStyle = null,
            verticalMajorGridLineStyle = null,
            horizontalMinorGridLineStyle = null,
            verticalMinorGridLineStyle = null
        ) {
            AreaPlot(
                modifier = Modifier, data = data, lineStyle = LineStyle(
                    brush = SolidColor(MaterialTheme.colorScheme.primary),
                    strokeWidth = 1.dp,
                    pathEffect = PathEffect.cornerPathEffect(20f)
                ), areaStyle = AreaStyle(
                    brush = Brush.linearGradient(
                        colors = listOf(0f, 0.05f, 0.075f, 0.1f, 0.2f, 0.3f).map {
                            MaterialTheme.colorScheme.primary.copy(alpha = it)
                        },
                        start = Offset(0f, Float.POSITIVE_INFINITY),
                        end = Offset(0f, 0f),
                        tileMode = TileMode.Clamp
                    )
                ), areaBaseline = AreaBaseline.ConstantLine(0f)

            )

        }
    }
}

@Composable
private fun ChartPeriodSelector(
    selectedPeriod: BitcoinChartPeriod, onChangePeriod: (BitcoinChartPeriod) -> Unit
) {
    val periods = BitcoinChartPeriod.entries.toTypedArray()

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        periods.forEach { period ->
            ChartPeriodSelectorItem(
                period = period,
                isSelected = selectedPeriod == period,
                onChangePeriod = onChangePeriod
            )
        }
    }
}

@Composable
private fun ChartPeriodSelectorItem(
    period: BitcoinChartPeriod, isSelected: Boolean, onChangePeriod: (BitcoinChartPeriod) -> Unit
) {
    val containerColor = if (isSelected) {
        MaterialTheme.colorScheme.outline
    } else {
        Color.Transparent
    }

    val contentColor = if (isSelected) {
        MaterialTheme.colorScheme.onSecondaryContainer
    } else {
        whiteLow
    }

    TextButton(
        modifier = Modifier.appTestTag(period.label),
        onClick = { onChangePeriod(period) },
        colors = ButtonDefaults.textButtonColors()
            .copy(containerColor = containerColor, contentColor = contentColor)
    ) {
        Text(
            modifier = Modifier.padding(horizontal = 8.dp),
            text = period.label,
            style = MaterialTheme.typography.bodyMedium
        )
    }

}

@Composable
private fun RetryButton(onClick: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().height(100.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {

        Text(
            text = stringResource(Res.string.id_data_not_available),
            style = MaterialTheme.typography.bodyMedium,
        )

        TextButton(
            onClick = onClick,
        ) {
            Text(
                text = stringResource(Res.string.id_retry),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

private fun Float.formatPercentage(): String {
    val formatter = DecimalFormat(locale = Locale.current.toLanguageTag()).apply {
        minimumFractionDigits = 2
        maximumFractionDigits = 2
        isGroupingUsed = false
        isDecimalSeparatorAlwaysShown = true
    }

    val formatted = formatter.format(this.absoluteValue.toDouble()) ?: "0.00"

    return when {
        this > 0f -> "+$formatted%"
        this < 0f -> "-$formatted%"
        else -> "0.00%"
    }
}

private fun Float.formatFiatPrice(currency: String): String {

    val formatter = DecimalFormat(locale = Locale.current.toLanguageTag()).apply {
        isGroupingUsed = true
    }

    val formatted = formatter.format(this.absoluteValue.toDouble()) ?: "0"

    return "$formatted ${currency.uppercase()}"
}

