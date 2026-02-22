package com.moneytracker.app.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ColorFilter
import androidx.glance.ImageProvider
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
import androidx.glance.unit.ColorProvider // 🌟 The correct import for the TYPE
import com.moneytracker.app.MainActivity
import com.moneytracker.app.R
import com.moneytracker.app.data.local.UserPreferences
import com.moneytracker.app.ui.theme.*
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface WidgetEntryPoint {
    fun userPreferences(): UserPreferences
}

class MoneyTrackerWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val entryPoint = EntryPointAccessors.fromApplication(
            context.applicationContext,
            WidgetEntryPoint::class.java
        )
        val userPreferences = entryPoint.userPreferences()
        
        provideContent {
            val themeMode by userPreferences.themeMode.collectAsState(initial = 0)
            WidgetContent(themeMode = themeMode)
        }
    }

    @Composable
    private fun WidgetContent(themeMode: Int) {
        val context = LocalContext.current
        
        // 🌟 Native OS Theming Delegation using fully qualified factory calls to avoid import clashes
        val surfaceColor = when (themeMode) {
            1 -> androidx.glance.color.ColorProvider(day = md_theme_light_surface, night = md_theme_light_surface)
            2 -> androidx.glance.color.ColorProvider(day = md_theme_dark_surface, night = md_theme_dark_surface)
            else -> androidx.glance.color.ColorProvider(day = md_theme_light_surface, night = md_theme_dark_surface)
        }
        
        val expenseBg = when (themeMode) {
            1 -> androidx.glance.color.ColorProvider(day = md_theme_light_errorContainer, night = md_theme_light_errorContainer)
            2 -> androidx.glance.color.ColorProvider(day = md_theme_dark_errorContainer, night = md_theme_dark_errorContainer)
            else -> androidx.glance.color.ColorProvider(day = md_theme_light_errorContainer, night = md_theme_dark_errorContainer)
        }
        val expenseIcon = when (themeMode) {
            1 -> androidx.glance.color.ColorProvider(day = md_theme_light_onErrorContainer, night = md_theme_light_onErrorContainer)
            2 -> androidx.glance.color.ColorProvider(day = md_theme_dark_onErrorContainer, night = md_theme_dark_onErrorContainer)
            else -> androidx.glance.color.ColorProvider(day = md_theme_light_onErrorContainer, night = md_theme_dark_onErrorContainer)
        }
        
        val incomeBg = when (themeMode) {
            1 -> androidx.glance.color.ColorProvider(day = md_theme_light_tertiaryContainer, night = md_theme_light_tertiaryContainer)
            2 -> androidx.glance.color.ColorProvider(day = md_theme_dark_tertiaryContainer, night = md_theme_dark_tertiaryContainer)
            else -> androidx.glance.color.ColorProvider(day = md_theme_light_tertiaryContainer, night = md_theme_dark_tertiaryContainer)
        }
        val incomeIcon = when (themeMode) {
            1 -> androidx.glance.color.ColorProvider(day = md_theme_light_onTertiaryContainer, night = md_theme_light_onTertiaryContainer)
            2 -> androidx.glance.color.ColorProvider(day = md_theme_dark_onTertiaryContainer, night = md_theme_dark_onTertiaryContainer)
            else -> androidx.glance.color.ColorProvider(day = md_theme_light_onTertiaryContainer, night = md_theme_dark_onTertiaryContainer)
        }
        
        val transferBg = when (themeMode) {
            1 -> androidx.glance.color.ColorProvider(day = md_theme_light_secondaryContainer, night = md_theme_light_secondaryContainer)
            2 -> androidx.glance.color.ColorProvider(day = md_theme_dark_secondaryContainer, night = md_theme_dark_secondaryContainer)
            else -> androidx.glance.color.ColorProvider(day = md_theme_light_secondaryContainer, night = md_theme_dark_secondaryContainer)
        }
        val transferIcon = when (themeMode) {
            1 -> androidx.glance.color.ColorProvider(day = md_theme_light_onSecondaryContainer, night = md_theme_light_onSecondaryContainer)
            2 -> androidx.glance.color.ColorProvider(day = md_theme_dark_onSecondaryContainer, night = md_theme_dark_onSecondaryContainer)
            else -> androidx.glance.color.ColorProvider(day = md_theme_light_onSecondaryContainer, night = md_theme_dark_onSecondaryContainer)
        }

        val appIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        Row(
            modifier = GlanceModifier
                .fillMaxWidth()
                .height(80.dp)
                .background(surfaceColor)
                .cornerRadius(24.dp)
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = GlanceModifier
                    .size(56.dp)
                    .clickable(actionStartActivity(appIntent)),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    provider = ImageProvider(R.mipmap.ic_launcher),
                    contentDescription = "Open App",
                    modifier = GlanceModifier.fillMaxSize()
                )
            }

            Spacer(modifier = GlanceModifier.width(12.dp))

            // ... (inside WidgetContent Row) ...

            // Expense (Red)
            WidgetActionButton(
                context = context,
                iconRes = R.drawable.ic_arrow_upward, // 🌟 Updated to use drawable
                bgColor = expenseBg,
                iconColor = expenseIcon,
                type = "EXPENSE",
                modifier = GlanceModifier.defaultWeight()
            )

            Spacer(modifier = GlanceModifier.width(8.dp))

            // Income (Green)
            WidgetActionButton(
                context = context,
                iconRes = R.drawable.ic_arrow_downward, // 🌟 Updated to use drawable
                bgColor = incomeBg,
                iconColor = incomeIcon,
                type = "INCOME",
                modifier = GlanceModifier.defaultWeight()
            )

            Spacer(modifier = GlanceModifier.width(8.dp))

            // Transfer (Blue)
            WidgetActionButton(
                context = context,
                iconRes = R.drawable.ic_swap_horiz, // 🌟 Updated to use drawable
                bgColor = transferBg,
                iconColor = transferIcon,
                type = "TRANSFER",
                modifier = GlanceModifier.defaultWeight()
            )
        }
    }

    // 🌟 Updated Composable
    @Composable
    private fun WidgetActionButton(
        context: Context,
        iconRes: Int, // <-- Changed from symbol: String
        bgColor: ColorProvider,
        iconColor: ColorProvider,
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
                .background(bgColor)
                .cornerRadius(16.dp)
                .clickable(actionStartActivity(intent)),
            contentAlignment = Alignment.Center
        ) {
            // Replaced Text() with Image()
            Image(
                provider = ImageProvider(iconRes),
                contentDescription = type,
                modifier = GlanceModifier.size(32.dp),
                colorFilter = ColorFilter.tint(iconColor) // Applies your dynamic Day/Night colors to the icon!
            )
        }
    }

    companion object {
        const val EXTRA_TRANSACTION_TYPE = "transaction_type"
    }
}
