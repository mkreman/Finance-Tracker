package com.moneytracker.app.notifications

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.moneytracker.app.data.local.repository.CategoryRecommendationRepository
import com.moneytracker.app.data.local.repository.CategoryRepository
import com.moneytracker.app.util.SplitwiseParser
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class AppNotificationListenerService : NotificationListenerService() {

    @Inject
    lateinit var recommendationRepository: CategoryRecommendationRepository

    @Inject
    lateinit var categoryRepository: CategoryRepository

    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + job)

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (sbn.packageName == "com.Splitwise.SplitwiseMobile") {
            val extras = sbn.notification.extras
            val title = extras.getString(Notification.EXTRA_TITLE) ?: ""
            var text = extras.getString(Notification.EXTRA_TEXT) ?: ""

            // Extract the newest line if this is a grouped "InboxStyle" notification
            val lines = extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES)
            if (!lines.isNullOrEmpty()) {
                text = lines.first().toString()
            }

            val parsedAlert = SplitwiseParser.parse(title, text) ?: return

            scope.launch {
                // Fetch the recommended IDs
                val recommendedCatId = recommendationRepository.getRecommendation(parsedAlert.payee, parsedAlert.type)
                
                parsedAlert.suggestedCategoryId = recommendedCatId
                parsedAlert.suggestedAccountId = recommendationRepository.getRecommendedAccountId(parsedAlert.payee, parsedAlert.type)

                // Fetch the actual category name so the notification displays it correctly instead of "AutoDetected"
                if (recommendedCatId != null) {
                    val category = categoryRepository.getCategoryById(recommendedCatId)
                    if (category != null) {
                        parsedAlert.suggestedCategoryName = category.name
                    }
                }

                BankAlertSuggestionNotifier.show(applicationContext, parsedAlert)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
    }
}