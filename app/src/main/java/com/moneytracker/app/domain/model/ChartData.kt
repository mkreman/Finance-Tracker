package com.moneytracker.app.domain.model

import androidx.compose.ui.graphics.Color

data class ChartData(
    val categoryId: String = "",
    val categoryName: String,
    val amount: Double,
    val color: Color,
    val iconKey: String,
    val percentage: Float
)
