package com.moneytracker.app.ui.components

import android.content.Intent
import android.net.Uri
import android.widget.ImageView
import android.widget.Toast
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.moneytracker.app.data.local.database.entities.TransactionType
import com.moneytracker.app.domain.model.Transaction
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun TransactionItem(
    transaction: Transaction,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var previewImageUri by remember(transaction.id) { mutableStateOf<Uri?>(null) }

    fun firstAttachmentUri(serialized: String?): Uri? {
        val first = serialized
            ?.split("\n")
            ?.map { it.trim() }
            ?.firstOrNull { it.isNotBlank() }
            ?: return null
        return runCatching { Uri.parse(first) }.getOrNull()
    }

    fun openAttachment() {
        val uri = firstAttachmentUri(transaction.receiptUri) ?: return
        val mimeType = context.contentResolver.getType(uri).orEmpty()
        val isImageByMime = mimeType.startsWith("image/")
        val isImageByPath = uri.toString().lowercase(Locale.ROOT).let {
            it.endsWith(".jpg") || it.endsWith(".jpeg") || it.endsWith(".png") || it.endsWith(".webp") || it.endsWith(".heic") || it.endsWith(".heif")
        }

        if (isImageByMime || isImageByPath) {
            previewImageUri = uri
            return
        }

        val openIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, mimeType.ifBlank { "*/*" })
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        runCatching {
            context.startActivity(Intent.createChooser(openIntent, "Open attachment"))
        }.onFailure {
            Toast.makeText(context, "No app found to open this attachment", Toast.LENGTH_SHORT).show()
        }
    }

    previewImageUri?.let { imageUri ->
        Dialog(onDismissRequest = { previewImageUri = null }) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 240.dp, max = 520.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.End
                    ) {
                        IconButton(onClick = { previewImageUri = null }) {
                            Icon(Icons.Filled.Close, contentDescription = "Close")
                        }
                    }

                    AndroidView(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false)
                            .heightIn(min = 220.dp, max = 460.dp),
                        factory = { viewContext ->
                            ImageView(viewContext).apply {
                                adjustViewBounds = true
                                scaleType = ImageView.ScaleType.FIT_CENTER
                            }
                        },
                        update = { imageView -> imageView.setImageURI(imageUri) }
                    )
                }
            }
        }
    }

    val currency = LocalCurrencySymbol.current
    val primarySplit = transaction.splits.firstOrNull()
    val categoryColor = primarySplit?.categoryColor?.let { parseHexColor(it) } ?: MaterialTheme.colorScheme.onSurfaceVariant
    val displayAmount = when (transaction.type) {
        TransactionType.EXPENSE -> "-$currency${formatAmount(transaction.totalAmount)}"
        TransactionType.INCOME -> "+$currency${formatAmount(transaction.totalAmount)}"
        TransactionType.TRANSFER -> "$currency${formatAmount(transaction.totalAmount)}"
    }
    val amountColor = when (transaction.type) {
        TransactionType.EXPENSE -> MaterialTheme.colorScheme.error
        TransactionType.INCOME -> MaterialTheme.colorScheme.tertiary
        TransactionType.TRANSFER -> MaterialTheme.colorScheme.secondary
    }

    val categoryName = if (transaction.type == TransactionType.TRANSFER) {
        "Transfer"
    } else if (transaction.splits.size > 1) {
        transaction.splits.joinToString(", ") { it.categoryName }
    } else {
        primarySplit?.categoryName ?: "Unknown"
    }

    val accountDisplay = if (transaction.type == TransactionType.TRANSFER && transaction.toAccountName != null) {
        "${transaction.accountName} → ${transaction.toAccountName}"
    } else {
        transaction.accountName
    }

    val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
    val timeString = timeFormat.format(Date(transaction.date))

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Category icon
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(
                    if (transaction.type == TransactionType.TRANSFER)
                        MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)
                    else categoryColor.copy(alpha = 0.15f)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = CategoryIcons.getIcon(
                    if (transaction.type == TransactionType.TRANSFER) "swap_horiz"
                    else primarySplit?.categoryIcon ?: "more_horiz"
                ),
                contentDescription = null,
                tint = if (transaction.type == TransactionType.TRANSFER) MaterialTheme.colorScheme.secondary else categoryColor,
                modifier = Modifier.size(22.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Details
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = categoryName,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Row {
                Text(
                    text = accountDisplay,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (transaction.note?.isNotBlank() == true && !transaction.note.equals(categoryName, ignoreCase = true)) {
                    Text(" • ", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = transaction.note,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            }
        }

        // Amount + time
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = displayAmount,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = amountColor
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.End) {
                Text(
                    text = timeString,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (transaction.isRecurring || transaction.parentRecurringId != null) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = CategoryIcons.getIcon("autorenew"),
                        contentDescription = "Recurring",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp)
                    )
                }
                if (!transaction.receiptUri.isNullOrBlank()) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.Filled.AttachFile,
                        contentDescription = "Attachment",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .size(14.dp)
                            .clickable { openAttachment() }
                    )
                }
            }
        }
    }
}

@Composable
fun TransactionDateHeader(
    dateLabel: String,
    dayExpense: Double = 0.0,
    dayIncome: Double = 0.0,
    dayTransfer: Double = 0.0,
    modifier: Modifier = Modifier
) {
    val currency = LocalCurrencySymbol.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .padding(top = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = dateLabel,
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (dayIncome > 0) {
                Text(
                    text = "+$currency${formatAmount(dayIncome)}",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.tertiary
                )
            }
            if (dayExpense > 0) {
                Text(
                    text = "-$currency${formatAmount(dayExpense)}",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.error
                )
            }
            if (dayTransfer > 0) {
                Text(
                    text = "⇄$currency${formatAmount(dayTransfer)}",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }
    }
}

fun parseHexColor(hex: String): Color {
    return try {
        val colorInt = android.graphics.Color.parseColor(hex)
        Color(colorInt)
    } catch (e: Exception) {
        Color.Gray
    }
}
