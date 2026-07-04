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

    // Export list of transactions to CSV format for external reports (e.g. Excel)
    fun exportTransactionsToCsv(
        context: Context,
        uri: Uri,
        transactions: List<Transaction>,
        categories: List<Category>,
        accounts: List<Account>
    ): Boolean {
        return try {
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                BufferedWriter(OutputStreamWriter(outputStream, "UTF-8")).use { writer ->
                    // Write UTF-8 BOM to ensure Arabic text renders correctly in Microsoft Excel
                    writer.write('\ufeff'.code)
                    writer.write("المعرف,المبلغ (دج),النوع,القسم,الحساب,التاريخ,الملاحظات\n")

                    transactions.forEach { tx ->
                        val catName = categories.firstOrNull { it.id == tx.categoryId }?.name ?: "غير محدد"
                        val accName = accounts.firstOrNull { it.id == tx.accountId }?.name ?: "غير محدد"
                        val formattedDate = FormatterUtils.formatDate(tx.date)
                        val noteText = tx.note?.replace(",", " ") ?: ""

                        writer.write("${tx.id},${tx.amount},${tx.type},$catName,$accName,$formattedDate,$noteText\n")
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
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                BufferedWriter(OutputStreamWriter(outputStream, "UTF-8")).use { writer ->
                    writer.write('\ufeff'.code)
                    writer.write("المعرف,الاسم,النوع,اللون,الحد الأقصى للميزانية\n")

                    categories.forEach { cat ->
                        writer.write("${cat.id},${cat.name},${cat.type},${cat.color},${cat.budgetLimit ?: 0.0}\n")
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
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                val jsonArray = JSONArray()
                transactions.forEach { tx ->
                    val obj = JSONObject().apply {
                        put("amount", tx.amount)
                        put("type", tx.type)
                        put("date", tx.date)
                        put("note", tx.note)
                        put("categoryId", tx.categoryId)
                        put("accountId", tx.accountId)
                    }
                    jsonArray.put(obj)
                }
                outputStream.write(jsonArray.toString(4).toByteArray())
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
