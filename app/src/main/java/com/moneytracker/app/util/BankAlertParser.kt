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
    val timestamp: Long,
    var suggestedCategoryId: String? = null,
    var suggestedCategoryName: String? = null,
    var suggestedAccountId: String? = null
)

object BankAlertParser {

    private val amountRegex = Regex("""(?i)(?:rs\.?|inr)\s*([\d,]+(?:\.\d{1,2})?)""")
    
    // Regexes expanded with + and * to handle more complex merchant strings
    private val toPayeeRegex = Regex("""(?i)\bto\s+([A-Z0-9@._\-+* ]{2,40})""")
    private val atMerchantRegex = Regex("""(?i)\bat\s+([A-Z0-9._\-+* ]{2,40})""")
    private val fromRegex = Regex("""(?i)\bfrom\s+([A-Z0-9@._\-+* ]{2,40})""")
    private val byRegex = Regex("""(?i)\bby\s+(?:a/c linked to vpa\s+)?([A-Z0-9@._\-+* ]{2,40})""")
    
    private val upiSlashRegex = Regex("""(?i)upi/([^/]+)""")
    private val upiVpaRegex = Regex("""(?i)(?:vpa|upi)[-/\s]+([A-Z0-9@._-]+)""")

    fun parse(rawMessage: String): ParsedBankAlert? {
        val cleaned = rawMessage.trim().replace("\n", " ").replace(Regex("\\s+"), " ")
        if (cleaned.isBlank()) return null

        val normalized = cleaned.lowercase()
        val type = when {
            normalized.contains("debit") || normalized.contains("spent") || normalized.contains("sent rs") || normalized.contains("withdrawn") || normalized.contains("withdrawal") -> TransactionType.EXPENSE
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
        
        // Build the note string
        val noteBuilder = buildString {
            if (type == TransactionType.EXPENSE) {
                if (normalized.contains("withdrawn") || normalized.contains("withdrawal")) {
                    append("ATM Withdrawal: $payee (Auto-detected)")
                } else {
                    append("Paid to: $payee (Auto-detected)")
                }
            } else {
                append("Received from: $payee (Auto-detected)")
            }
            
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
            note = noteBuilder, // FIX: Restored the actual note string here so it saves to the DB!
            rawText = rawMessage,
            timestamp = System.currentTimeMillis()
        )
    }

    private fun extractPayee(message: String, type: TransactionType): String {
        val candidates = mutableListOf<String>()

        // 1. UPI identifiers (highest accuracy, checked first)
        upiSlashRegex.findAll(message).forEach { candidates.add(it.groupValues[1]) }
        upiVpaRegex.findAll(message).forEach { candidates.add(it.groupValues[1]) }

        // 2. Directional keywords
        if (type == TransactionType.EXPENSE) {
            // Prioritize 'At' over 'To' for better Merchant/ATM detection
            atMerchantRegex.findAll(message).forEach { candidates.add(it.groupValues[1]) }
            toPayeeRegex.findAll(message).forEach { candidates.add(it.groupValues[1]) }
        } else {
            fromRegex.findAll(message).forEach { candidates.add(it.groupValues[1]) }
            byRegex.findAll(message).forEach { candidates.add(it.groupValues[1]) }
        }

        // 3. Process and return the first valid, clean candidate
        for (candidate in candidates) {
            val valid = validateAndCleanPayee(candidate)
            if (valid != null) return valid
        }

        // 4. Absolute Fallbacks if all extraction fails
        return if (type == TransactionType.EXPENSE) {
            if (message.lowercase().contains("withdrawn") || message.lowercase().contains("withdrawal")) "ATM" else "Expense"
        } else "Income"
    }

    /**
     * Highly aggressive cleaner to ensure we never return "To", pure numbers, or bank spam text.
     */
    private fun validateAndCleanPayee(raw: String): String? {
        var cleaned = raw
        
        // Words that signify the end of a merchant/person name in bank SMS
        val stopWords = listOf(" on ", " ref ", " via ", " txn ", " date ", " available ", " avail ", " avl ", " bal ", " a/c ", " account ", " not you", " call ", " sms ", " block ", " card ", " branch ", " limit ")
        
        for (word in stopWords) {
            val idx = cleaned.indexOf(word, ignoreCase = true)
            if (idx > 0) {
                cleaned = cleaned.substring(0, idx)
            }
        }
        
        cleaned = cleaned
            .replace(Regex("(?i)^vpa[- ]*"), "")
            .replace(Regex("(?i)^mr\\.?\\s+"), "")
            .replace(Regex("(?i)^mrs\\.?\\s+"), "")
            .replace(Regex("(?i)^to\\s+"), "") // Erase leading 'to '
            .replace(Regex("(?i)^from\\s+"), "") // Erase leading 'from '
            .replace(Regex("(?i)^at\\s+"), "") // Erase leading 'at '
            .replace(Regex("(?i)^by\\s+"), "") // Erase leading 'by '
            .trim()

        // Discard strings that are too short (1 character) or purely numeric/symbols (like phone numbers)
        if (cleaned.length < 2) return null
        if (cleaned.matches(Regex("""^[\d\W]+$"""))) return null
        
        // Blacklist exact matches of common meaningless structural words
        val blacklisted = setOf("to", "at", "from", "by", "on", "in", "the", "an", "is", "a/c", "account", "card", "bank", "rs", "inr", "pos", "ecom", "atm", "upi", "imps", "neft", "rtgs", "info")
        if (cleaned.lowercase() in blacklisted) return null

        return cleaned
    }
}
