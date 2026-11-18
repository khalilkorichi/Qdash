package com.qdash.data.repository

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.qdash.domain.model.ExportReportRequest
import com.qdash.domain.model.ExportResult
import com.qdash.domain.repository.ExportRepository
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ExportRepositoryImpl(
    private val context: Context,
    private val accountDao: com.qdash.data.local.dao.AccountDao,
    private val transactionDao: com.qdash.data.local.dao.TransactionDao,
    private val database: com.qdash.data.local.AppDatabase? = null
) : ExportRepository {

    override suspend fun exportPdfReport(request: ExportReportRequest): ExportResult = withContext(Dispatchers.IO) {
        try {
            val reportsDir = File(context.cacheDir, "reports").apply { mkdirs() }

            // Cleanup only reports produced by this repository.
            try {
                reportsDir.listFiles()?.forEach { f ->
                    if (f.isFile && f.name.startsWith("kdach_report_") && f.name.endsWith(".pdf", ignoreCase = true)) {
                        f.delete()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            val db = database ?: (context.applicationContext as? com.qdash.FinTrackApp)?.container?.database

            // 1. Fetch and filter accounts
            val allAccounts = accountDao.getAllAccounts().first()
            val accounts = if (request.selectedAccounts.isNotEmpty()) {
                allAccounts.filter { it.id in request.selectedAccounts }
            } else {
                allAccounts
            }
            val accountsMap = allAccounts.associateBy { it.id }
            val totalBalance = accounts.filter { it.isActive }.sumOf { it.balance }

            // 2. Resolve date range & query real transactions
            val now = System.currentTimeMillis()
            val oneDayMs = 86_400_000L
            val (startDate, endDate) = request.dateRange ?: when (request.reportType.uppercase(Locale.ROOT)) {
                "ANNUAL" -> (now - 365L * oneDayMs) to now
                "MONTHLY" -> (now - 30L * oneDayMs) to now
                else -> (now - 30L * oneDayMs) to now
            }

            val rawTransactions = transactionDao.getTransactionsByDateRangeList(startDate, endDate)
            val filteredTransactions = rawTransactions.filter { tx ->
                val accountMatch = request.selectedAccounts.isEmpty() || tx.accountId in request.selectedAccounts
                val categoryMatch = request.selectedCategories.isEmpty() ||
                    (tx.categoryId != null && tx.categoryId in request.selectedCategories)
                accountMatch && categoryMatch
            }

            val totalIncome = filteredTransactions
                .filter { it.type == "INCOME" && it.kind != "SAVINGS_CONTRIBUTION" && it.kind != "TRANSFER" }
                .sumOf { it.amount }
            val totalExpenses = filteredTransactions
                .filter { it.type == "EXPENSE" && it.kind != "TRANSFER" }
                .sumOf { it.amount }
            val netCashflow = totalIncome - totalExpenses

            // 3. Query categories, active savings goals, and active debts
            val categoriesMap = db?.categoryDao()?.getAllCategories()?.first()?.associateBy { it.id } ?: emptyMap()
            val activeSavingGoals = if (request.includeSavingsSection && db != null) {
                db.savingGoalDao().getAllSavingGoals().first().filter { !it.isArchived }
            } else {
                emptyList()
            }
            val activeDebts = if (request.includeDebtSection && db != null) {
                db.debtDao().getActiveDebts().first().filter { !it.debt.isClosed }
            } else {
                emptyList()
            }

            val localizedReportTitle = localizeReportType(request.reportType)

            // Generate write output as file
            val baseName = sanitizeReportFileName(request.fileName ?: "kdach_report_${System.currentTimeMillis()}")
            val cleanName = if (baseName.endsWith(".pdf", ignoreCase = true)) baseName else "$baseName.pdf"
            val file = File(reportsDir, cleanName)

            val pdfDocument = PdfDocument()
            try {
                // Standard A4 size is 595 x 842 points
                var pageNumber = 1
                var page = pdfDocument.startPage(PdfDocument.PageInfo.Builder(595, 842, pageNumber).create())
                var canvas: Canvas = page.canvas

                val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.parseColor("#1E293B")
                    textSize = 20f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    textAlign = Paint.Align.CENTER
                }
                val subtitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.parseColor("#475569")
                    textSize = 13f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
                    textAlign = Paint.Align.CENTER
                }
                val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.parseColor("#334155")
                    textSize = 11.5f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                    textAlign = Paint.Align.RIGHT
                }
                val boldBodyPaint = Paint(bodyPaint).apply {
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                }
                val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.parseColor("#4F46E5")
                    textSize = 14f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    textAlign = Paint.Align.RIGHT
                }
                val linePaint = Paint().apply {
                    color = Color.parseColor("#E2E8F0")
                    strokeWidth = 1f
                }
                val footerPaint = Paint(subtitlePaint).apply {
                    textSize = 9f
                    typeface = Typeface.DEFAULT
                }

                fun drawFooter(targetCanvas: Canvas, pageNum: Int) {
                    targetCanvas.drawRect(0f, 792f, 595f, 842f, Paint().apply { color = Color.parseColor("#F1F5F9") })
                    targetCanvas.drawText(
                        "تم توليد هذا التقرير بأمان وسرية تامة محلياً في هاتفك بواسطة قداشّ • صفحة $pageNum",
                        297f,
                        820f,
                        footerPaint
                    )
                }

                var yPos = 190f

                fun ensurePageCapacity(requiredSpace: Float = 22f) {
                    if (yPos > 760f || yPos + requiredSpace > 760f) {
                        drawFooter(canvas, pageNumber)
                        pdfDocument.finishPage(page)
                        pageNumber++
                        page = pdfDocument.startPage(PdfDocument.PageInfo.Builder(595, 842, pageNumber).create())
                        canvas = page.canvas
                        canvas.drawRect(0f, 0f, 595f, 56f, Paint().apply { color = Color.parseColor("#F8FAFC") })
                        canvas.drawText("قداشّ — $localizedReportTitle (صفحة $pageNumber)", 550f, 35f, headerPaint)
                        canvas.drawLine(0f, 56f, 595f, 56f, Paint().apply { color = Color.parseColor("#CBD5E1"); strokeWidth = 1.5f })
                        yPos = 86f
                    }
                }

                val sdf = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault())
                val shortDateSdf = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault())

                // Page 1 Header
                canvas.drawRect(0f, 0f, 595f, 155f, Paint().apply { color = Color.parseColor("#F8FAFC") })
                canvas.drawText("قداشّ — $localizedReportTitle", 297f, 60f, titlePaint)
                canvas.drawText(
                    "الفترة: من ${shortDateSdf.format(Date(startDate))} إلى ${shortDateSdf.format(Date(endDate))}",
                    297f,
                    90f,
                    subtitlePaint
                )
                canvas.drawText("تاريخ التصدير: ${sdf.format(Date())}", 550f, 132f, bodyPaint)
                canvas.drawText("نوع التقرير: $localizedReportTitle", 50f, 132f, Paint(bodyPaint).apply { textAlign = Paint.Align.LEFT })
                canvas.drawLine(0f, 155f, 595f, 155f, Paint().apply { color = Color.parseColor("#CBD5E1"); strokeWidth = 2f })

                var sectionIndex = 1

                // Section 1: Financial Summary & Accounts
                ensurePageCapacity(60f)
                canvas.drawText("$sectionIndex. ملخص التدفقات النقدية والأرصدة", 550f, yPos, headerPaint)
                sectionIndex++
                yPos += 14f
                canvas.drawLine(50f, yPos, 550f, yPos, linePaint)
                yPos += 24f

                val totalBalanceFormatted = com.qdash.core.utils.FormatterUtils.formatCurrency(totalBalance)
                val totalIncomeFormatted = com.qdash.core.utils.FormatterUtils.formatCurrency(totalIncome)
                val totalExpensesFormatted = com.qdash.core.utils.FormatterUtils.formatCurrency(totalExpenses)
                val netCashflowFormatted = com.qdash.core.utils.FormatterUtils.formatCurrency(netCashflow)

                ensurePageCapacity()
                canvas.drawText("• إجمالي الرصيد النشط: $totalBalanceFormatted", 540f, yPos, boldBodyPaint)
                yPos += 20f
                ensurePageCapacity()
                canvas.drawText("• إجمالي المداخيل خلال الفترة: $totalIncomeFormatted", 540f, yPos, bodyPaint)
                yPos += 20f
                ensurePageCapacity()
                canvas.drawText("• إجمالي المصاريف خلال الفترة: $totalExpensesFormatted", 540f, yPos, bodyPaint)
                yPos += 20f
                ensurePageCapacity()
                canvas.drawText("• صافي التدفق النقدي: $netCashflowFormatted (${filteredTransactions.size} معاملة)", 540f, yPos, boldBodyPaint)
                yPos += 24f

                ensurePageCapacity()
                canvas.drawText("توزيع الحسابات المشمولة في التقرير:", 540f, yPos, boldBodyPaint)
                yPos += 20f

                if (accounts.isEmpty()) {
                    ensurePageCapacity()
                    canvas.drawText("  - لا توجد حسابات مطابقة مسجلة حالياً.", 530f, yPos, bodyPaint)
                    yPos += 20f
                } else {
                    accounts.forEach { acc ->
                        ensurePageCapacity()
                        val accTypeLabel = localizeAccountType(acc.type)
                        val balanceFormatted = com.qdash.core.utils.FormatterUtils.formatCurrency(acc.balance)
                        val statusSuffix = if (!acc.isActive) " (غير نشط)" else ""
                        canvas.drawText("  - ${acc.name} ($accTypeLabel)$statusSuffix: $balanceFormatted", 530f, yPos, bodyPaint)
                        yPos += 19f
                    }
                }
                yPos += 18f

                // Section 2: Real Active Savings Goals
                if (request.includeSavingsSection) {
                    ensurePageCapacity(60f)
                    canvas.drawText("$sectionIndex. الأهداف الادخارية النشطة", 550f, yPos, headerPaint)
                    sectionIndex++
                    yPos += 14f
                    canvas.drawLine(50f, yPos, 550f, yPos, linePaint)
                    yPos += 24f

                    if (activeSavingGoals.isEmpty()) {
                        ensurePageCapacity()
                        canvas.drawText("• لا توجد أهداف ادخارية مسجلة حالياً.", 540f, yPos, bodyPaint)
                        yPos += 22f
                    } else {
                        val totalSaved = activeSavingGoals.sumOf { it.currentAmount }
                        val totalTarget = activeSavingGoals.sumOf { it.targetAmount }
                        val overallPercent = if (totalTarget > 0.0) ((totalSaved / totalTarget) * 100.0).toInt().coerceIn(0, 100) else 0
                        ensurePageCapacity()
                        canvas.drawText(
                            "• إجمالي المدخرات: ${com.qdash.core.utils.FormatterUtils.formatCurrency(totalSaved)} من أصل ${com.qdash.core.utils.FormatterUtils.formatCurrency(totalTarget)} ($overallPercent%)",
                            540f,
                            yPos,
                            boldBodyPaint
                        )
                        yPos += 22f

                        activeSavingGoals.forEach { goal ->
                            ensurePageCapacity()
                            val pct = if (goal.targetAmount > 0.0) ((goal.currentAmount / goal.targetAmount) * 100.0).toInt().coerceIn(0, 100) else 0
                            val stateTag = when {
                                goal.isCompleted -> " [مكتمل]"
                                goal.isPaused -> " [متوقف مؤقتاً]"
                                else -> ""
                            }
                            val currentStr = com.qdash.core.utils.FormatterUtils.formatCurrency(goal.currentAmount)
                            val targetStr = com.qdash.core.utils.FormatterUtils.formatCurrency(goal.targetAmount)
                            canvas.drawText("  - ${goal.name}$stateTag: $currentStr / $targetStr ($pct%)", 530f, yPos, bodyPaint)
                            yPos += 19f
                        }
                    }
                    yPos += 18f
                }

                // Section 3: Real Active Debts
                if (request.includeDebtSection) {
                    ensurePageCapacity(60f)
                    canvas.drawText("$sectionIndex. الديون والالتزامات القائمة", 550f, yPos, headerPaint)
                    sectionIndex++
                    yPos += 14f
                    canvas.drawLine(50f, yPos, 550f, yPos, linePaint)
                    yPos += 24f

                    if (activeDebts.isEmpty()) {
                        ensurePageCapacity()
                        canvas.drawText("• لا توجد ديون أو التزامات نشطة حالياً.", 540f, yPos, bodyPaint)
                        yPos += 22f
                    } else {
                        val owedByMe = activeDebts.filter { it.debt.direction != "OWED_TO_ME" }.sumOf { it.debt.remainingAmount }
                        val owedToMe = activeDebts.filter { it.debt.direction == "OWED_TO_ME" }.sumOf { it.debt.remainingAmount }
                        ensurePageCapacity()
                        canvas.drawText(
                            "• إجمالي المتبقي عليّ سداده: ${com.qdash.core.utils.FormatterUtils.formatCurrency(owedByMe)} | المستحق لي: ${com.qdash.core.utils.FormatterUtils.formatCurrency(owedToMe)}",
                            540f,
                            yPos,
                            boldBodyPaint
                        )
                        yPos += 22f

                        activeDebts.forEach { item ->
                            ensurePageCapacity()
                            val d = item.debt
                            val dirLabel = if (d.direction == "OWED_TO_ME") "لي عند" else "عليّ لـ"
                            val remStr = com.qdash.core.utils.FormatterUtils.formatCurrency(d.remainingAmount)
                            val dueStr = d.dueDate?.let { " • الاستحقاق: ${shortDateSdf.format(Date(it))}" } ?: ""
                            canvas.drawText("  - ${d.title} ($dirLabel ${d.creditorName}): متبقي $remStr$dueStr", 530f, yPos, bodyPaint)
                            yPos += 19f
                        }
                    }
                    yPos += 18f
                }

                // Section 4: Detailed Transactions Table/List (paginated across multiple pages)
                if (request.includeTransactions) {
                    ensurePageCapacity(60f)
                    canvas.drawText("$sectionIndex. سجل المعاملات المالية (${filteredTransactions.size})", 550f, yPos, headerPaint)
                    yPos += 14f
                    canvas.drawLine(50f, yPos, 550f, yPos, linePaint)
                    yPos += 24f

                    if (filteredTransactions.isEmpty()) {
                        ensurePageCapacity()
                        canvas.drawText("• لا توجد معاملات مسجلة ضمن الفترة المحددة.", 540f, yPos, bodyPaint)
                        yPos += 20f
                    } else {
                        filteredTransactions.forEach { tx ->
                            ensurePageCapacity(22f)
                            val dateStr = shortDateSdf.format(Date(tx.occurredAt ?: tx.date))
                            val typeLabel = when (tx.type) {
                                "INCOME" -> "دخل"
                                "EXPENSE" -> "مصروف"
                                "TRANSFER" -> if (tx.isDebit) "تحويل صادر" else "تحويل وارد"
                                else -> "معاملة"
                            }
                            val catName = tx.categoryId?.let { categoriesMap[it]?.name } ?: "بدون فئة"
                            val accName = accountsMap[tx.accountId]?.name ?: "حساب #${tx.accountId}"
                            val amountStr = com.qdash.core.utils.FormatterUtils.formatCurrency(tx.amount)
                            val noteSnippet = tx.note?.trim()?.takeIf { it.isNotEmpty() }?.let {
                                val truncated = if (it.length > 28) it.take(28) + "…" else it
                                " — $truncated"
                            } ?: ""

                            canvas.drawText(
                                "• [$dateStr] $typeLabel | $catName ($accName): $amountStr$noteSnippet",
                                545f,
                                yPos,
                                bodyPaint
                            )
                            yPos += 19f
                        }
                    }
                }

                drawFooter(canvas, pageNumber)
                pdfDocument.finishPage(page)

                FileOutputStream(file).use { outputStream ->
                    pdfDocument.writeTo(outputStream)
                }
            } finally {
                pdfDocument.close()
            }

            ExportResult(
                success = true,
                fileName = cleanName,
                fileUri = file.absolutePath,
                message = "تم حفظ التقرير بنجاح بصيغة PDF."
            )
        } catch (e: Exception) {
            e.printStackTrace()
            ExportResult(
                success = false,
                fileName = "",
                fileUri = null,
                message = "حدث خطأ أثناء تصدير التقرير بصيغة PDF: ${e.localizedMessage}"
            )
        }
    }

    private fun localizeReportType(reportType: String): String {
        return when (reportType.trim().uppercase(Locale.ROOT)) {
            "MONTHLY" -> "التقرير المالي الشهري"
            "ANNUAL" -> "التقرير المالي السنوي"
            "SAVINGS" -> "تقرير الأهداف الادخارية"
            "DEBTS" -> "تقرير الديون والالتزامات"
            "ANALYTICS" -> "تقرير التحليلات المالية"
            "ACCOUNT_STATEMENT" -> "كشف الحساب التفصيلي"
            else -> "التقرير المالي الشامل"
        }
    }

    private fun localizeAccountType(type: String): String {
        return when (type.uppercase(Locale.ROOT)) {
            "BARIDIMOB" -> "بريدي موب"
            "CCP" -> "حساب CCP"
            "CASH" -> "نقدي/كاش"
            "SAVINGS" -> "حصالة الادخار"
            "BANK" -> "بنك"
            "WALLET" -> "محفظة"
            else -> "حساب آخر"
        }
    }

    private fun sanitizeReportFileName(fileName: String): String {
        val baseName = File(fileName).name
            .replace(Regex("[^A-Za-z0-9._-]"), "_")
            .trim('_')
        return baseName.ifBlank { "kdach_report_${System.currentTimeMillis()}" }
    }
}
