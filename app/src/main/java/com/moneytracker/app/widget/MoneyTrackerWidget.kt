package com.moneytracker.app.widget

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.LocalContext
import androidx.glance.action.ActionParameters
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.*
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.moneytracker.app.MainActivity

class MoneyTrackerWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            WidgetContent()
        }
    }

    @Composable
    private fun WidgetContent() {
        val context = LocalContext.current

        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(ColorProvider(DarkSurface))
                .cornerRadius(16.dp)
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // App title row
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Finance Tracker",
                    style = TextStyle(
                        color = ColorProvider(TextWhite),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
            }

            Spacer(modifier = GlanceModifier.height(10.dp))

            // Action buttons row
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Expense Button
                WidgetActionButton(
                    context = context,
                    label = "−",
                    bgColor = ExpenseRedWidget,
                    type = "EXPENSE",
                    modifier = GlanceModifier.defaultWeight()
                )

                Spacer(modifier = GlanceModifier.width(8.dp))

                // Income Button
                WidgetActionButton(
                    context = context,
                    label = "+",
                    bgColor = IncomeGreenWidget,
                    type = "INCOME",
                    modifier = GlanceModifier.defaultWeight()
                )

                Spacer(modifier = GlanceModifier.width(8.dp))

                // Transfer Button
                WidgetActionButton(
                    context = context,
                    label = "⇄",
                    bgColor = TransferBlueWidget,
                    type = "TRANSFER",
                    modifier = GlanceModifier.defaultWeight()
                )
            }
        }
    }

    @Composable
    private fun WidgetActionButton(
        context: Context,
        label: String,
        bgColor: Color,
        type: String,
        modifier: GlanceModifier = GlanceModifier
    ) {
        val intent = Intent(context, MainActivity::class.java).apply {
            putExtra(EXTRA_TRANSACTION_TYPE, type)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        Box(
            modifier = modifier
                .height(48.dp)
                .background(ColorProvider(bgColor))
                .cornerRadius(12.dp)
                .clickable(actionStartActivity(intent)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                style = TextStyle(
                    color = ColorProvider(TextWhite),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            )
        }
    }

    companion object {
        const val EXTRA_TRANSACTION_TYPE = "transaction_type"

        // Widget colors (androidx.compose.ui.graphics.Color)
        private val DarkSurface = Color(0xFF2D2D2D)
        private val ExpenseRedWidget = Color(0xFFF44336)
        private val IncomeGreenWidget = Color(0xFF4CAF50)
        private val TransferBlueWidget = Color(0xFF2196F3)
        private val TextWhite = Color(0xFFFFFFFF)
    }
}
