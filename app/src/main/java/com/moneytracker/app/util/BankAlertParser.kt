package com.moneytracker.app.util

import com.moneytracker.app.data.local.database.entities.TransactionType
import java.util.UUID

data class ParsedBankAlert(
    val suggestionId: String,
    val type: TransactionType,
    val amount: Double,
    val payee: String,
    val note: String,
    val rawText: String,
    val timestamp: Long
)

object BankAlertParser {

    private val amountRegex = Regex("""(?i)(?:rs\.?|inr)\s*([\d,]+(?:\.\d{1,2})?)""")
    private val toPayeeRegex = Regex("""(?i)\bto\s+([A-Z0-9@._\- ]{2,40})""")
    private val atMerchantRegex = Regex("""(?i)\bat\s+([A-Z0-9._\- ]{2,40})""")
    private val fromRegex = Regex("""(?i)\bfrom\s+([A-Z0-9@._\- ]{2,40})""")

    fun parse(rawMessage: String): ParsedBankAlert? {
        val cleaned = rawMessage.trim().replace("\n", " ").replace(Regex("\\s+"), " ")
        if (cleaned.isBlank()) return null

        val normalized = cleaned.lowercase()
        val type = when {
            normalized.contains("debit") || normalized.contains("spent") || normalized.contains("sent rs") -> TransactionType.EXPENSE
            normalized.contains("credit alert") || normalized.contains("credited") -> TransactionType.INCOME
            else -> return null
        }

        val amount = amountRegex.find(cleaned)
            ?.groupValues
            ?.getOrNull(1)
            ?.replace(",", "")
            ?.toDoubleOrNull()
            ?: return null

        val payee = extractPayee(cleaned, type)
        val note = buildString {
            append(if (type == TransactionType.EXPENSE) "Auto-detected debit alert" else "Auto-detected credit alert")
            val refMatch = Regex("""(?i)\bref\s+([A-Z0-9]+)""").find(cleaned)?.groupValues?.getOrNull(1)
            if (!refMatch.isNullOrBlank()) {
                append(" • Ref ")
                append(refMatch)
            }
        }

        return ParsedBankAlert(
            suggestionId = UUID.randomUUID().toString(),
            type = type,
            amount = amount,
            payee = payee,
            note = note,
            rawText = rawMessage,
            timestamp = System.currentTimeMillis()
        )
    }

    private fun extractPayee(message: String, type: TransactionType): String {
        val match = when (type) {
            TransactionType.EXPENSE -> toPayeeRegex.find(message) ?: atMerchantRegex.find(message)
            TransactionType.INCOME -> fromRegex.find(message)
            else -> null
        }

        return match?.groupValues
            ?.getOrNull(1)
            ?.trim()
            ?.takeIf { it.isNotBlank() }
            ?: if (type == TransactionType.EXPENSE) "Expense" else "Income"
    }
}
