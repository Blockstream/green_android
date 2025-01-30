package com.blockstream.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.adamglin.PhosphorIcons
import com.adamglin.phosphoricons.Regular
import com.adamglin.phosphoricons.regular.ArrowRight


@Composable
fun GreenArrow(modifier: Modifier = Modifier, enabled: Boolean = true) {

    Card(
        Modifier
            .then(modifier)
            .size(40.dp),
        colors = CardDefaults.cardColors(
            contentColor = MaterialTheme.colorScheme.onPrimary,
            containerColor = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
        ) {
            Icon(
                modifier = Modifier.align(Alignment.Center),
                imageVector = PhosphorIcons.Regular.ArrowRight,
                contentDescription = "Arrow"
            )
        }
    }
}
