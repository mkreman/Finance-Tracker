package com.moneytracker.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isUnspecified

@Composable
fun SummaryCard(
    title: String,
    amount: Double,
    type: SummaryType,
    isSelected: Boolean = false,
    currency: String = LocalCurrencySymbol.current,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val color = when (type) {
        SummaryType.EXPENSE -> MaterialTheme.colorScheme.error
        SummaryType.INCOME -> MaterialTheme.colorScheme.tertiary
        SummaryType.TRANSFER -> MaterialTheme.colorScheme.secondary
    }

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .then(
                if (isSelected) Modifier.border(1.5.dp, color, RoundedCornerShape(16.dp))
                else Modifier
            )
            .clickable(onClick = onClick)
            .padding(16.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(color.copy(alpha = if (isSelected) 0.25f else 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = CategoryIcons.getIcon(
                        when (type) {
                            SummaryType.EXPENSE -> "trending_down"
                            SummaryType.INCOME -> "trending_up"
                            SummaryType.TRANSFER -> "swap_horiz"
                        }
                    ),
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        AutoResizeText(
            text = "$currency${formatAmount(kotlin.math.abs(amount))}",
            color = color,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold
            ),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

enum class SummaryType {
    EXPENSE, INCOME, TRANSFER
}

@Composable
fun AutoResizeText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    style: TextStyle = MaterialTheme.typography.titleMedium
) {
    var scaledTextStyle by remember { mutableStateOf(style) }
    var readyToDraw by remember { mutableStateOf(false) }

    Text(
        text = text,
        modifier = modifier,
        color = color,
        style = scaledTextStyle,
        softWrap = false,
        maxLines = 1,
        onTextLayout = { textLayoutResult ->
            if (textLayoutResult.didOverflowWidth) {
                if (scaledTextStyle.fontSize.isUnspecified) {
                    scaledTextStyle = scaledTextStyle.copy(fontSize = style.fontSize)
                }
                scaledTextStyle = scaledTextStyle.copy(
                    fontSize = scaledTextStyle.fontSize * 0.9f
                )
            } else {
                readyToDraw = true
            }
        },
        overflow = TextOverflow.Clip
    )
}