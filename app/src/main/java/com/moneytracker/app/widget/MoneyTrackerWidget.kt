package com.moneytracker.app.widget

import android.content.ComponentName
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
import androidx.glance.text.TextAlign
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.moneytracker.app.MainActivity
import com.moneytracker.app.R
import com.moneytracker.app.ui.theme.md_theme_dark_onPrimaryContainer
import com.moneytracker.app.ui.theme.md_theme_dark_onSurface
import com.moneytracker.app.ui.theme.md_theme_dark_onSurfaceVariant
import com.moneytracker.app.ui.theme.md_theme_dark_errorContainer
import com.moneytracker.app.ui.theme.md_theme_dark_primary
import com.moneytracker.app.ui.theme.md_theme_dark_primaryContainer
import com.moneytracker.app.ui.theme.md_theme_dark_secondary
import com.moneytracker.app.ui.theme.md_theme_dark_secondaryContainer
import com.moneytracker.app.ui.theme.md_theme_dark_surface
import com.moneytracker.app.ui.theme.md_theme_dark_tertiary
import com.moneytracker.app.ui.theme.md_theme_dark_tertiaryContainer
import com.moneytracker.app.ui.theme.md_theme_light_onPrimaryContainer
import com.moneytracker.app.ui.theme.md_theme_light_onSurface
import com.moneytracker.app.ui.theme.md_theme_light_onSurfaceVariant
import com.moneytracker.app.ui.theme.md_theme_light_errorContainer
import com.moneytracker.app.ui.theme.md_theme_light_primary
import com.moneytracker.app.ui.theme.md_theme_light_primaryContainer
import com.moneytracker.app.ui.theme.md_theme_light_secondary
import com.moneytracker.app.ui.theme.md_theme_light_secondaryContainer
import com.moneytracker.app.ui.theme.md_theme_light_surface
import com.moneytracker.app.ui.theme.md_theme_light_tertiary
import com.moneytracker.app.ui.theme.md_theme_light_tertiaryContainer

class MoneyTrackerWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            WidgetContent()
        }
    }

    @Composable
    private fun WidgetContent() {
        val context = LocalContext.current
        val colors = widgetColors(context)
        
        val appIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        Row(
            modifier = GlanceModifier
                .fillMaxWidth()
                .height(80.dp)
                .background(ColorProvider(colors.surface))
                .cornerRadius(16.dp)
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // App Icon Button (no background, larger icon)
            Box(
                modifier = GlanceModifier
                    .defaultWeight()
                    .fillMaxHeight()
                    .clickable(actionStartActivity(appIntent)),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    provider = androidx.glance.ImageProvider(R.mipmap.ic_launcher),
                    contentDescription = "Open Money Tracker",
                    modifier = GlanceModifier.size(70.dp)
                )
            }

            Spacer(modifier = GlanceModifier.width(8.dp))

            // Expense Button
            WidgetActionButton(
                context = context,
                label = "Expense",
                symbol = "↓",
                bgColor = colors.errorContainer,
                textColor = colors.error,
                type = "EXPENSE",
                modifier = GlanceModifier
                    .defaultWeight()
                    .fillMaxHeight()
            )

            Spacer(modifier = GlanceModifier.width(8.dp))

            // Income Button
            WidgetActionButton(
                context = context,
                label = "Income",
                symbol = "↑",
                bgColor = colors.tertiaryContainer,
                textColor = colors.tertiary,
                type = "INCOME",
                modifier = GlanceModifier
                    .defaultWeight()
                    .fillMaxHeight()
            )

            Spacer(modifier = GlanceModifier.width(8.dp))

            // Transfer Button
            WidgetActionButton(
                context = context,
                label = "Transfer",
                symbol = "⇄",
                bgColor = colors.secondaryContainer,
                textColor = colors.secondary,
                type = "TRANSFER",
                modifier = GlanceModifier
                    .defaultWeight()
                    .fillMaxHeight()
            )
        }
    }

    @Composable
    private fun WidgetActionButton(
        context: Context,
        label: String,
        symbol: String,
        bgColor: Color,
        textColor: Color,
        type: String,
        modifier: GlanceModifier = GlanceModifier
    ) {
        val intent = Intent(context, MainActivity::class.java).apply {
            putExtra(EXTRA_TRANSACTION_TYPE, type)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        Box(
            modifier = modifier
                .background(ColorProvider(bgColor))
                .cornerRadius(12.dp)
                .clickable(actionStartActivity(intent)),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = symbol,
                    style = TextStyle(
                        color = ColorProvider(textColor),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                )
                Text(
                    text = label,
                    style = TextStyle(
                        color = ColorProvider(textColor),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center
                    )
                )
            }
        }
    }

    companion object {
        const val EXTRA_TRANSACTION_TYPE = "transaction_type"
    }

    private data class WidgetColors(
        val surface: Color,
        val primaryContainer: Color,
        val errorContainer: Color,
        val error: Color,
        val tertiaryContainer: Color,
        val tertiary: Color,
        val secondaryContainer: Color,
        val secondary: Color,
        val onPrimaryContainer: Color,
        val onSurface: Color,
        val onSurfaceVariant: Color
    )

    private fun widgetColors(context: Context): WidgetColors {
        val isDark = (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        return if (isDark) {
            WidgetColors(
                surface = md_theme_dark_surface,
                primaryContainer = md_theme_dark_primaryContainer,
                errorContainer = md_theme_dark_errorContainer,
                error = md_theme_dark_primary,
                tertiaryContainer = md_theme_dark_tertiaryContainer,
                tertiary = md_theme_dark_tertiary,
                secondaryContainer = md_theme_dark_secondaryContainer,
                secondary = md_theme_dark_secondary,
                onPrimaryContainer = md_theme_dark_onPrimaryContainer,
                onSurface = md_theme_dark_onSurface,
                onSurfaceVariant = md_theme_dark_onSurfaceVariant
            )
        } else {
            WidgetColors(
                surface = md_theme_light_surface,
                primaryContainer = md_theme_light_primaryContainer,
                errorContainer = md_theme_light_errorContainer,
                error = md_theme_light_primary,
                tertiaryContainer = md_theme_light_tertiaryContainer,
                tertiary = md_theme_light_tertiary,
                secondaryContainer = md_theme_light_secondaryContainer,
                secondary = md_theme_light_secondary,
                onPrimaryContainer = md_theme_light_onPrimaryContainer,
                onSurface = md_theme_light_onSurface,
                onSurfaceVariant = md_theme_light_onSurfaceVariant
            )
        }
    }
}
