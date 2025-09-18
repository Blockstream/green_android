package com.blockstream.compose.screens.settings.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.blockstream.ui.components.GreenRow

@Composable
fun SettingsItem(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
    content: (@Composable () -> Unit)? = null,
    rightContent: (@Composable () -> Unit)? = null,
    leadingContent: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    titleMaxLines: Int = 1,
    subtitleMaxLines: Int = 2,
    contentPadding: PaddingValues = PaddingValues(16.dp),
) {
    val bodyColor = MaterialTheme.colorScheme.onSurfaceVariant

    @Composable
    fun TitleSubtitle() {
        if (title.isNotBlank()) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                maxLines = titleMaxLines,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { heading() }
            )
        }
        subtitle?.let {
            if (it.isNotBlank()) {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyLarge,
                    color = bodyColor,
                    maxLines = subtitleMaxLines,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }

    @Composable
    fun ItemContent() {
        if (content != null) {
            Column(Modifier.fillMaxWidth()) {
                val hasTitleOrSubtitle = title.isNotBlank() || (subtitle?.isNotBlank() == true)
                if (hasTitleOrSubtitle) {
                    TitleSubtitle()
                    Spacer(Modifier.height(8.dp))
                }
                content()
            }
        } else {
            GreenRow(
                space = 16,
                padding = 0,
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (leadingContent != null) {
                    Box(Modifier.padding(end = 12.dp)) { leadingContent() }
                }
                Column(modifier = Modifier.weight(1f)) {
                    TitleSubtitle()
                }
                rightContent?.invoke()
            }
        }
    }

    val baseContent = @Composable {
        Box(
            modifier = Modifier
                .padding(contentPadding)
                .alpha(if (enabled) 1f else 0.6f)
        ) {
            ItemContent()
        }
    }

    if (onClick != null) {
        OutlinedCard(
            onClick = onClick,
            enabled = enabled,
            modifier = modifier
        ) { baseContent() }
    } else {
        OutlinedCard(modifier = modifier) { baseContent() }
    }

}