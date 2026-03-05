package com.moneytracker.app.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import androidx.core.app.NotificationCompat
import com.moneytracker.app.MainActivity
import com.moneytracker.app.R
import com.moneytracker.app.data.local.UserPreferences
import com.moneytracker.app.data.local.database.dao.BudgetAlertDao
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BudgetAlertManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val budgetAlertDao: BudgetAlertDao,
    private val userPreferences: UserPreferences // Injected to check settings
) {
    suspend fun checkBudgets(dateInMillis: Long, categoryData: Map<String, String>) {
        // Check if the user has disabled this notification in settings
        val alertsEnabled = userPreferences.budgetAlertsEnabled.first()
        if (!alertsEnabled) return

        val calendar = Calendar.getInstance().apply { timeInMillis = dateInMillis }
        val month = calendar.get(Calendar.MONTH) + 1
        val year = calendar.get(Calendar.YEAR)

        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        val startOfMonth = calendar.timeInMillis

        calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        val endOfMonth = calendar.timeInMillis

        categoryData.forEach { (categoryId, categoryName) ->
            val budget = budgetAlertDao.getBudgetForCategory(categoryId, month, year) ?: return@forEach
            val spent = budgetAlertDao.getSpentAmountForCategory(categoryId, startOfMonth, endOfMonth) ?: 0.0

            if (spent > budget.limitAmount) {
                showNotification(categoryId, categoryName, spent - budget.limitAmount, budget.limitAmount)
            }
        }
    }

    private fun showNotification(categoryId: String, categoryName: String, exceededBy: Double, limit: Double) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "budget_alerts_v2"
        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val openBudgetIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("open_budget_category_id", categoryId)
            putExtra("open_budget_category_name", categoryName)
        }
        val openBudgetPendingIntent = PendingIntent.getActivity(
            context,
            categoryId.hashCode(),
            openBudgetIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Budget Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alerts when a category exceeds its monthly budget"
                setSound(
                    defaultSoundUri,
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                        .build()
                )
            }
            manager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Budget Exceeded: $categoryName")
            .setContentText("You've exceeded your ₹${"%.0f".format(limit)} budget by ₹${"%.2f".format(exceededBy)}.")
            .setColor(Color.parseColor("#D32F2F"))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setSound(defaultSoundUri)
            .setContentIntent(openBudgetPendingIntent)
            .setAutoCancel(true)
            .build()

        manager.notify("budget_$categoryId".hashCode(), notification)
    }
}