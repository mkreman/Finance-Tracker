package com.moneytracker.app.util

import com.moneytracker.app.data.local.database.entities.TransactionType
import java.util.UUID

object SplitwiseParser {
    private val youOweRegex = Regex("""(?i)(?:you owe)\s+(?:rs\.?|inr|\$|₹)\s*([\d,]+(?:\.\d{1,2})?)""")
    private val youPaidRegex = Regex("""(?i)(?:you paid)\s+([A-Za-z0-9 ]+?)\s+(?:rs\.?|inr|\$|₹)\s*([\d,]+(?:\.\d{1,2})?)""")
    private val paidYouRegex = Regex("""(?i)([A-Za-z0-9 ]+?)\s+(?:paid you)\s+(?:rs\.?|inr|\$|₹)\s*([\d,]+(?:\.\d{1,2})?)""")
    private val addedByRegex = Regex("""(?i)Added by\s+([A-Za-z0-9 ]+?)(?:\s+in\s+|$)""")
    
    // NEW: Captures the amount and name directly from the summary line: "Cats ($60.00) • Added by Kaku in..."
    private val groupedAddedByRegex = Regex("""(?i).*?\((?:rs\.?|inr|\$|₹)\s*([\d,]+(?:\.\d{1,2})?)\)\s*•\s*Added by\s+([A-Za-z0-9 ]+?)(?:\s+in|$)""")

    fun parse(title: String, text: String): ParsedBankAlert? {
        val fullText = "$title $text".replace("\n", " ")
        
        var type: TransactionType = TransactionType.TRANSFER
        var amountStr: String? = null
        var person: String? = null

        val oweMatch = youOweRegex.find(fullText)
        val youPaidMatch = youPaidRegex.find(fullText)
        val paidYouMatch = paidYouRegex.find(fullText)
        val addedByMatch = addedByRegex.find(title)
        val groupedMatch = groupedAddedByRegex.find(text)

        if (oweMatch != null) {
            type = TransactionType.EXPENSE 
            person = addedByMatch?.groupValues?.getOrNull(1)?.trim()
            amountStr = oweMatch.groupValues[1]
        } else if (groupedMatch != null) {
            type = TransactionType.EXPENSE
            amountStr = groupedMatch.groupValues[1]
            person = groupedMatch.groupValues[2].trim()
        } else if (youPaidMatch != null) {
            type = TransactionType.TRANSFER
            person = youPaidMatch.groupValues[1].trim()
            amountStr = youPaidMatch.groupValues[2]
        } else if (paidYouMatch != null) {
            type = TransactionType.INCOME 
            person = paidYouMatch.groupValues[1].trim()
            amountStr = paidYouMatch.groupValues[2]
        }

        if (amountStr == null) return null

        val amount = amountStr.replace(",", "").toDoubleOrNull() ?: return null
        val finalPerson = person ?: "Splitwise Person"

        return ParsedBankAlert(
            suggestionId = UUID.randomUUID().toString(),
            type = type,
            amount = amount,
            payee = finalPerson,
            note = "Splitwise: $text",
            rawText = fullText,
            timestamp = System.currentTimeMillis()
        )
    }
}