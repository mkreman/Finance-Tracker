package com.moneytracker.app.ui.screens.dashboard

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlin.math.max
import kotlin.math.ceil
import com.moneytracker.app.domain.model.TransactionListItem
import com.moneytracker.app.ui.components.LocalCurrencySymbol
import com.moneytracker.app.ui.components.TransactionDateHeader
import com.moneytracker.app.ui.components.TransactionItem
import com.moneytracker.app.ui.components.formatAmount
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryTransactionsScreen(
    onNavigateBack: () -> Unit,
    onEditTransaction: (String) -> Unit = {},
    viewModel: CategoryTransactionsViewModel = hiltViewModel()
) {
    val transactions by viewModel.transactions.collectAsState()
    val summary by viewModel.summary.collectAsState()
    val percentage by viewModel.percentage.collectAsState()
    val trendPoints by viewModel.trendPoints.collectAsState()
    val currentMonth by viewModel.currentMonth.collectAsState()
    val currency = LocalCurrencySymbol.current

    val typeColor = when (viewModel.type.uppercase()) {
        "INCOME" -> MaterialTheme.colorScheme.tertiary
        "TRANSFER" -> MaterialTheme.colorScheme.secondary
        else -> MaterialTheme.colorScheme.error
    }

    Column(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
    ) {
        TopAppBar(
            title = { Text("${viewModel.categoryName} (${viewModel.type.lowercase().replaceFirstChar { it.uppercase() }})", color = MaterialTheme.colorScheme.onSurface) },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.Filled.ArrowBack, "Back", tint = MaterialTheme.colorScheme.onSurface)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            windowInsets = WindowInsets(0.dp)
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Month Selector
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            CategoryMonthSelector(
                currentMonth = currentMonth,
                onPreviousMonth = viewModel::previousMonth,
                onNextMonth = viewModel::nextMonth,
                onSelectAll = viewModel::selectAll
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))

        if (transactions.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("No transactions for this period", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                // Summary Card
                item(key = "summary") {
                    CategorySummaryCard(
                        totalAmount = summary.totalAmount,
                        transactionCount = summary.transactionCount,
                        highestAmount = summary.highestAmount,
                        averageAmount = summary.averageAmount,
                        currency = currency,
                        typeColor = typeColor,
                        typeName = viewModel.type.lowercase().replaceFirstChar { it.uppercase() },
                        percentage = percentage
                    )
                }

                item(key = "trend") {
                    CategoryTrendSection(
                        points = trendPoints,
                        isAllTime = currentMonth == null,
                        typeColor = typeColor
                    )
                }

                items(
                    items = transactions,
                    key = { item ->
                        when (item) {
                            is TransactionListItem.Header -> "header_${item.dateMillis}"
                            is TransactionListItem.Entry -> "txn_${item.transaction.id}"
                        }
                    }
                ) { item ->
                    when (item) {
                        is TransactionListItem.Header -> {
                            TransactionDateHeader(dateLabel = item.dateLabel, dayExpense = item.dayExpense, dayIncome = item.dayIncome, dayTransfer = item.dayTransfer)
                        }
                        is TransactionListItem.Entry -> {
                            TransactionItem(
                                transaction = item.transaction,
                                onClick = { onEditTransaction(item.transaction.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryTrendSection(
    points: List<CategoryTrendPoint>,
    isAllTime: Boolean,
    typeColor: androidx.compose.ui.graphics.Color
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp)
    ) {
        Text(
            text = if (isAllTime) "All Time Trend" else "Daily Trend",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(12.dp))

        if (points.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No trend data",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            return
        }

        val rawMax = max(1.0, points.maxOfOrNull { it.value } ?: 1.0)
        val maxValue = if (rawMax < 10) ceil(rawMax) else rawMax
        
        val numSteps = if (maxValue >= 4) 4 else maxValue.toInt()
        val yAxisLabels = (numSteps downTo 0).map { step ->
            formatAxisAmount((maxValue * step) / numSteps)
        }.distinct()

        Row(modifier = Modifier.fillMaxWidth()) {
            // Y-Axis (Fixed on the left)
            Column(
                modifier = Modifier
                    .height(160.dp)
                    .padding(end = 8.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.End
            ) {
                yAxisLabels.forEach { label ->
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Canvas and X-Axis
            BoxWithConstraints(modifier = Modifier.weight(1f)) {
                val visiblePoints = 8
                val isScrollable = isAllTime && points.size > visiblePoints
                val chartWidth = if (isScrollable) {
                    (maxWidth / visiblePoints) * points.size
                } else {
                    maxWidth
                }
                
                val scrollState = rememberScrollState()

                LaunchedEffect(points.size, isAllTime) {
                    if (isScrollable) {
                        scrollState.scrollTo(scrollState.maxValue)
                    }
                }

                Column(
                    modifier = Modifier
                        .then(if (isScrollable) Modifier.horizontalScroll(scrollState) else Modifier)
                        .width(chartWidth)
                ) {
                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                    ) {
                        val xStep = if (points.size > 1) size.width / (points.size - 1) else size.width
                        val path = Path()

                        points.forEachIndexed { index, point ->
                            val x = if (points.size == 1) size.width / 2f else index * xStep
                            val y = ((maxValue - point.value) / maxValue * size.height).toFloat()
                            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
                            drawCircle(color = typeColor, radius = 2.5.dp.toPx(), center = Offset(x, y))
                        }

                        drawPath(
                            path = path,
                            color = typeColor,
                            style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    val targetMonthlyIndices = if (!isAllTime && points.size > 5) {
                        listOf(
                            0,
                            points.lastIndex / 4,
                            points.lastIndex / 2,
                            (points.lastIndex * 3) / 4,
                            points.lastIndex
                        )
                    } else {
                        emptyList()
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        points.forEachIndexed { index, point ->
                            val showLabel = if (isAllTime) {
                                true
                            } else {
                                points.size <= 5 || index in targetMonthlyIndices
                            }

                            Text(
                                text = if (showLabel) point.label else "",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = if (isAllTime && showLabel) Modifier.rotate(-45f) else Modifier
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CategoryMonthSelector(
    currentMonth: Calendar?,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onSelectAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    val monthText = if (currentMonth == null) {
        "All Time"
    } else {
        val sdf = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
        sdf.format(currentMonth.time)
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = modifier) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(24.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            IconButton(onClick = onPreviousMonth, modifier = Modifier.size(36.dp)) {
                Icon(
                    imageVector = Icons.Filled.ChevronLeft,
                    contentDescription = "Previous Month",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }

            Text(
                text = monthText,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            IconButton(onClick = onNextMonth, modifier = Modifier.size(36.dp)) {
                Icon(
                    imageVector = Icons.Filled.ChevronRight,
                    contentDescription = "Next Month",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        }
        
        if (currentMonth != null) {
            TextButton(
                onClick = onSelectAll,
                contentPadding = PaddingValues(0.dp),
                modifier = Modifier.height(32.dp)
            ) {
                Text("Show All Time", style = MaterialTheme.typography.labelMedium)
            }
        } else {
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun CategorySummaryCard(
    totalAmount: Double,
    transactionCount: Int,
    highestAmount: Double,
    averageAmount: Double,
    currency: String,
    typeColor: androidx.compose.ui.graphics.Color,
    typeName: String,
    percentage: Float = 0f
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(20.dp)
    ) {
        Column {
            Text(
                text = "Total $typeName",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    text = "$currency${formatAmount(totalAmount)}",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 28.sp
                    ),
                    color = typeColor
                )

                Text(
                    text = "${String.format("%.1f", percentage)}%",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 28.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.5.dp)
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                SummaryStatItem(
                    label = "Transactions",
                    value = transactionCount.toString(),
                    modifier = Modifier.weight(1f)
                )
                SummaryStatItem(
                    label = "Average",
                    value = "$currency${formatAmount(averageAmount)}",
                    modifier = Modifier.weight(1f)
                )
                SummaryStatItem(
                    label = "Highest",
                    value = "$currency${formatAmount(highestAmount)}",
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun SummaryStatItem(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun formatAxisAmount(amount: Double): String {
    return when {
        amount >= 100_000 -> String.format(Locale.getDefault(), "%.1fL", amount / 100_000).replace(".0L", "L")
        amount >= 1_000 -> String.format(Locale.getDefault(), "%.1fK", amount / 1_000).replace(".0K", "K")
        else -> amount.toInt().toString()
    }
}