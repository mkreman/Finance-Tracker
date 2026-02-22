package com.moneytracker.app.widget

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.LocalContext
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.*
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.moneytracker.app.MainActivity
import com.moneytracker.app.R
import com.moneytracker.app.ui.theme.*

class MoneyTrackerWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            WidgetContent()
        }
    }

    @Composable
    private fun WidgetContent() {
        // 1. Get the UI Context which has the correct Configuration (Day/Night)
        val context = LocalContext.current
        
        // 2. Check the System Theme directly from the View Context
        val isDark = (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        
        // 3. Select colors based on 'isDark'
        val surfaceColor = if (isDark) md_theme_dark_surface else md_theme_light_surface
        
        val expenseBg = if (isDark) md_theme_dark_errorContainer else md_theme_light_errorContainer
        val expenseIcon = if (isDark) md_theme_dark_onErrorContainer else md_theme_light_onErrorContainer
        
        val incomeBg = if (isDark) md_theme_dark_tertiaryContainer else md_theme_light_tertiaryContainer
        val incomeIcon = if (isDark) md_theme_dark_onTertiaryContainer else md_theme_light_onTertiaryContainer
        
        val transferBg = if (isDark) md_theme_dark_secondaryContainer else md_theme_light_secondaryContainer
        val transferIcon = if (isDark) md_theme_dark_onSecondaryContainer else md_theme_light_onSecondaryContainer

        val appIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        Row(
            modifier = GlanceModifier
                .fillMaxWidth()
                .height(80.dp)
                // Use ColorProvider(Color) which is supported in your version
                .background(ColorProvider(surfaceColor))
                .cornerRadius(24.dp)
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // App Icon
            Box(
                modifier = GlanceModifier
                    .size(56.dp)
                    .clickable(actionStartActivity(appIntent)),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    provider = androidx.glance.ImageProvider(R.mipmap.ic_launcher),
                    contentDescription = "Open App",
                    modifier = GlanceModifier.fillMaxSize()
                )
            }

            Spacer(modifier = GlanceModifier.width(12.dp))

            // Expense (Red)
            WidgetActionButton(
                context = context,
                symbol = "↓",
                bgColor = expenseBg,
                iconColor = expenseIcon,
                type = "EXPENSE",
                modifier = GlanceModifier.defaultWeight()
            )

            Spacer(modifier = GlanceModifier.width(8.dp))

            // Income (Green)
            WidgetActionButton(
                context = context,
                symbol = "↑",
                bgColor = incomeBg,
                iconColor = incomeIcon,
                type = "INCOME",
                modifier = GlanceModifier.defaultWeight()
            )

            Spacer(modifier = GlanceModifier.width(8.dp))

            // Transfer (Blue)
            WidgetActionButton(
                context = context,
                symbol = "⇄",
                bgColor = transferBg,
                iconColor = transferIcon,
                type = "TRANSFER",
                modifier = GlanceModifier.defaultWeight()
            )
        }
    }

    @Composable
    private fun WidgetActionButton(
        context: Context,
        symbol: String,
        bgColor: Color,
        iconColor: Color,
        type: String,
        modifier: GlanceModifier = GlanceModifier
    ) {
        val intent = Intent(context, MainActivity::class.java).apply {
            putExtra(EXTRA_TRANSACTION_TYPE, type)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        Box(
            modifier = modifier
                .fillMaxHeight()
                .background(ColorProvider(bgColor))
                .cornerRadius(16.dp)
                .clickable(actionStartActivity(intent)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = symbol,
                style = TextStyle(
                    color = ColorProvider(iconColor),
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            )
        }
    }

    companion object {
        const val EXTRA_TRANSACTION_TYPE = "transaction_type"
    }
}
