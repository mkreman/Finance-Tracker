package com.moneytracker.app.workers

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Build
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.moneytracker.app.MainActivity
import com.moneytracker.app.R
import com.moneytracker.app.data.local.database.entities.RecurringUnit
import com.moneytracker.app.data.local.database.entities.SyncStatus
import com.moneytracker.app.data.local.database.entities.TransactionType
import com.moneytracker.app.data.local.database.entities.TransactionEntity
import com.moneytracker.app.data.local.database.entities.TransactionSplitEntity
import com.moneytracker.app.data.local.repository.TransactionRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.*

@HiltWorker
class RecurringTransactionWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted params: WorkerParameters,
    private val transactionRepository: TransactionRepository
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            processRecurringTransactions()
            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }

    private suspend fun processRecurringTransactions() {
        val allTransactions = transactionRepository.getAllTransactionsRawIncludingDeleted()
        val recurringTransactions = allTransactions.filter {
            it.transaction.isRecurring && !it.transaction.isDeleted
        }
        val occurrencesByParentId = allTransactions
            .filter { it.transaction.parentRecurringId != null }
            .groupBy { it.transaction.parentRecurringId }
        val now = System.currentTimeMillis()

        recurringTransactions.forEach { transactionWithSplits ->
            val transaction = transactionWithSplits.transaction
            val occurrencesForParent = occurrencesByParentId[transaction.id].orEmpty()
            val latestOccurrence = occurrencesForParent.maxByOrNull { it.transaction.date }

            val templateSource = buildList {
                add(transactionWithSplits)
                addAll(occurrencesForParent.filter { !it.transaction.isDeleted })
            }.maxWithOrNull(
                compareBy<com.moneytracker.app.data.local.database.entities.TransactionWithSplits> {
                    it.transaction.modifiedAt
                }.thenBy { it.transaction.date }
            ) ?: transactionWithSplits

            val templateTransaction = templateSource.transaction
            val templateSplits = templateSource.splits

            if (!shouldProcessRecurringTransaction(transaction, now)) {
                return@forEach
            }

            // 1. Get the base date to calculate from
            val baseDate = latestOccurrence?.transaction?.date ?: transaction.date

            // 2. Calculate the FIRST needed occurrence by adding the interval
            var nextDate = calculateNextDate(
                fromDate = baseDate,
                interval = transaction.recurringInterval ?: 1,
                recurringUnit = transaction.recurringUnit ?: return@forEach
            )

            var createdCount = 0
            var latestCreatedTransaction: TransactionEntity? = null

            // 3. Keep generating transactions as long as the nextDate is in the past/present
            while (isOccurrenceDue(nextDate, transaction.recurringEndDate, now)) {
                val currentTime = System.currentTimeMillis()
                val newTransaction = templateTransaction.copy(
                    id = UUID.randomUUID().toString(),
                    date = nextDate, // Assign the exact historical/current due date
                    createdAt = currentTime,
                    modifiedAt = currentTime,
                    parentRecurringId = transaction.id,
                    isRecurring = false,
                    recurringInterval = null,
                    recurringUnit = null,
                    recurringEndDate = null,
                    notifyForRecurringEntries = false, // Set false here, handled below
                    syncStatus = SyncStatus.PENDING
                )

                val newSplits = templateSplits.map { split ->
                    split.copy(
                        id = UUID.randomUUID().toString(),
                        transactionId = newTransaction.id
                    )
                }

                // Save this specific occurrence
                transactionRepository.saveTransaction(newTransaction, newSplits)
                
                latestCreatedTransaction = newTransaction
                createdCount++

                // Calculate the date for the NEXT loop iteration
                nextDate = calculateNextDate(
                    fromDate = nextDate,
                    interval = transaction.recurringInterval ?: 1,
                    recurringUnit = transaction.recurringUnit ?: break
                )
            }

            // 4. Fire notification only ONCE per series if at least one entry was created
            if (createdCount > 0 && transaction.notifyForRecurringEntries && latestCreatedTransaction != null) {
                showNotification(latestCreatedTransaction)
            }
        }
    }

    private fun shouldProcessRecurringTransaction(transaction: TransactionEntity, now: Long): Boolean {
        if (transaction.recurringInterval == null || transaction.recurringUnit == null) {
            return false
        }
        return transaction.recurringEndDate?.let { now <= it } ?: true
    }

    private fun isOccurrenceDue(nextDate: Long, endDate: Long?, now: Long): Boolean {
        if (nextDate > now) return false
        if (endDate != null && nextDate > endDate) return false
        return true
    }

    private fun calculateNextDate(fromDate: Long, interval: Int, recurringUnit: RecurringUnit): Long {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = fromDate
        }

        when (recurringUnit) {
            RecurringUnit.DAY -> calendar.add(Calendar.DAY_OF_YEAR, interval)
            RecurringUnit.WEEK -> calendar.add(Calendar.WEEK_OF_YEAR, interval)
            RecurringUnit.MONTH -> calendar.add(Calendar.MONTH, interval)
            RecurringUnit.YEAR -> calendar.add(Calendar.YEAR, interval)
        }

        return calendar.timeInMillis
    }

    private fun showNotification(transaction: TransactionEntity) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "recurring_transactions"
        val channelName = "Recurring Transactions"

        // Create notification channel for Android O and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                channelName,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications for recurring transactions"
            }
            notificationManager.createNotificationChannel(channel)
        }

        // Create intent to open app
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val amountText = String.format(Locale.getDefault(), "%.2f", transaction.totalAmount)
        val contentText = SpannableString("${transaction.payee}: $amountText")
        val amountStart = contentText.length - amountText.length
        val amountColor = when (transaction.type) {
            TransactionType.EXPENSE -> Color.parseColor("#D32F2F")
            TransactionType.INCOME -> Color.parseColor("#2E7D32")
            TransactionType.TRANSFER -> Color.parseColor("#2196F3")
        }
        contentText.setSpan(
            ForegroundColorSpan(amountColor),
            amountStart,
            contentText.length,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        val expandedText = buildString {
            append("${transaction.payee}: $amountText")
            if (!transaction.note.isNullOrBlank()) {
                append("\n")
                append("Note: ${transaction.note}")
            }
        }
        val expandedSpannable = SpannableStringBuilder(expandedText).apply {
            val amountStartInExpanded = indexOf(amountText)
            if (amountStartInExpanded >= 0) {
                setSpan(
                    ForegroundColorSpan(amountColor),
                    amountStartInExpanded,
                    amountStartInExpanded + amountText.length,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
        }

        // Build notification
        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setLargeIcon(BitmapFactory.decodeResource(context.resources, R.mipmap.ic_launcher))
            .setContentTitle("Recurring Transaction Added")
            .setContentText(contentText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(expandedSpannable))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(transaction.id.hashCode(), notification)
    }

    companion object {
        const val WORK_NAME = "recurring_transaction_worker"
    }
}
