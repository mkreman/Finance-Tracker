package com.moneytracker.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.moneytracker.app.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun MonthSelector(
    currentMonth: Calendar,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sdf = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
    val monthText = sdf.format(currentMonth.time)

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(CardBackground)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        IconButton(onClick = onPreviousMonth, modifier = Modifier.size(36.dp)) {
            Icon(
                imageVector = Icons.Filled.ChevronLeft,
                contentDescription = "Previous Month",
                tint = TextPrimary
            )
        }

        Text(
            text = monthText,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            color = TextPrimary,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        IconButton(onClick = onNextMonth, modifier = Modifier.size(36.dp)) {
            Icon(
                imageVector = Icons.Filled.ChevronRight,
                contentDescription = "Next Month",
                tint = TextPrimary
            )
        }
    }
}
