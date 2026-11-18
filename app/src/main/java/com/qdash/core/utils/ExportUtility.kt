package com.qdash.core.utils

import android.content.Context
import android.net.Uri
import com.qdash.domain.model.Transaction
import com.qdash.domain.model.Category
import com.qdash.domain.model.Account
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedWriter
import java.io.OutputStreamWriter

object ExportUtility {

    private val FORMULA_PREFIXES = charArrayOf('=', '+', '-', '@', '\t', '\r')

    /**
     * Formats a CSV cell per RFC-4180:
     * - Prefixes formula-triggering leading characters (=, +, -, @) with a single quote when [sanitizeFormula] is true.
     * - Doubles embedded double-quotes and wraps the field in double-quotes.
     */
    private fun escapeCsvField(rawValue: String?, sanitizeFormula: Boolean = false): String {
        var value = rawValue ?: ""
        if (sanitizeFormula && value.isNotEmpty()) {
            val firstNonSpace = value.trimStart().firstOrNull()
            if (firstNonSpace != null && firstNonSpace in FORMULA_PREFIXES) {
                value = "'$value"
            }
        }
        val escaped = value.replace("\"", "\"\"")
        return "\"$escaped\""
    }

    // Export list of transactions to CSV format for external reports (e.g. Excel)
    fun exportTransactionsToCsv(
        context: Context,
        uri: Uri,
        transactions: List<Transaction>,
        categories: List<Category>,
        accounts: List<Account>
    ): Boolean {
        return try {
            val stream = context.contentResolver.openOutputStream(uri) ?: return false
            stream.use { outputStream ->
                BufferedWriter(OutputStreamWriter(outputStream, Charsets.UTF_8)).use { writer ->
                    // Write UTF-8 BOM to ensure Arabic text renders correctly in Microsoft Excel
                    writer.write("\uFEFF")
                    val headers = listOf(
                        "المعرف",
                        "المبلغ (دج)",
                        "النوع",
                        "القسم",
                        "الحساب",
                        "التاريخ",
                        "الملاحظات"
                    ).joinToString(",") { escapeCsvField(it) }
                    writer.write("$headers\r\n")

                    transactions.forEach { tx ->
                        val catName = categories.firstOrNull { it.id == tx.categoryId }?.name ?: "غير محدد"
                        val accName = accounts.firstOrNull { it.id == tx.accountId }?.name ?: "غير محدد"
                        val typeLabel = when (tx.type.name) {
                            "EXPENSE" -> "مصروف"
                            "INCOME" -> "دخل"
                            "TRANSFER" -> "تحويل"
                            else -> tx.type.name
                        }
                        val formattedDate = FormatterUtils.formatDate(tx.date)
                        val row = listOf(
                            tx.id.toString(),
                            tx.amount.toString(),
                            escapeCsvField(typeLabel),
                            escapeCsvField(catName, sanitizeFormula = true),
                            escapeCsvField(accName, sanitizeFormula = true),
                            escapeCsvField(formattedDate),
                            escapeCsvField(tx.note ?: "", sanitizeFormula = true)
                        ).joinToString(",")

                        writer.write("$row\r\n")
                    }
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    // Export list of categories to CSV
    fun exportCategoriesToCsv(
        context: Context,
        uri: Uri,
        categories: List<Category>
    ): Boolean {
        return try {
            val stream = context.contentResolver.openOutputStream(uri) ?: return false
            stream.use { outputStream ->
                BufferedWriter(OutputStreamWriter(outputStream, Charsets.UTF_8)).use { writer ->
                    writer.write("\uFEFF")
                    val headers = listOf(
                        "المعرف",
                        "الاسم",
                        "النوع",
                        "اللون",
                        "الحد الأقصى للميزانية"
                    ).joinToString(",") { escapeCsvField(it) }
                    writer.write("$headers\r\n")

                    categories.forEach { cat ->
                        val typeLabel = when (cat.type.name) {
                            "EXPENSE" -> "مصروف"
                            "INCOME" -> "دخل"
                            else -> cat.type.name
                        }
                        val row = listOf(
                            cat.id.toString(),
                            escapeCsvField(cat.name, sanitizeFormula = true),
                            escapeCsvField(typeLabel),
                            escapeCsvField(cat.color),
                            (cat.budgetLimit ?: 0.0).toString()
                        ).joinToString(",")

                        writer.write("$row\r\n")
                    }
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    // Export transactions to JSON for light data-sharing/syncing
    fun exportDataToJson(
        context: Context,
        uri: Uri,
        transactions: List<Transaction>
    ): Boolean {
        return try {
            val stream = context.contentResolver.openOutputStream(uri) ?: return false
            stream.use { outputStream ->
                val jsonArray = JSONArray()
                transactions.forEach { tx ->
                    val obj = JSONObject().apply {
                        put("id", tx.id)
                        put("amount", tx.amount)
                        put("type", tx.type.name)
                        put("date", tx.date)
                        put("note", tx.note ?: JSONObject.NULL)
                        put("categoryId", tx.categoryId ?: JSONObject.NULL)
                        put("accountId", tx.accountId)
                    }
                    jsonArray.put(obj)
                }
                outputStream.write(jsonArray.toString(4).toByteArray(Charsets.UTF_8))
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
