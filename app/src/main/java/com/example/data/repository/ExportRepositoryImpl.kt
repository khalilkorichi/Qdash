package com.example.data.repository

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.example.domain.model.ExportReportRequest
import com.example.domain.model.ExportResult
import com.example.domain.repository.ExportRepository
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
    private val accountDao: com.example.data.local.dao.AccountDao,
    private val transactionDao: com.example.data.local.dao.TransactionDao
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

            // Fetch real database records for report dynamic data
            val accounts = accountDao.getAllAccounts().first()
            val totalBalance = accounts.sumOf { it.balance }

            // Generate write output as file
            val baseName = sanitizeReportFileName(request.fileName ?: "kdach_report_${System.currentTimeMillis()}")
            val cleanName = if (baseName.endsWith(".pdf", ignoreCase = true)) baseName else "$baseName.pdf"
            val file = File(reportsDir, cleanName)

            val pdfDocument = PdfDocument()
            try {
                // Standard A4 size is 595 x 842 points
                val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
                val page = pdfDocument.startPage(pageInfo)
                val canvas: Canvas = page.canvas
                val titlePaint = Paint().apply {
                    color = Color.parseColor("#1E293B")
                    textSize = 20f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    textAlign = Paint.Align.CENTER
                }
                val subtitlePaint = Paint().apply {
                    color = Color.parseColor("#475569")
                    textSize = 13f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
                    textAlign = Paint.Align.CENTER
                }
                val bodyPaint = Paint().apply {
                    color = Color.parseColor("#334155")
                    textSize = 12f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                    textAlign = Paint.Align.RIGHT
                }
                val headerPaint = Paint().apply {
                    color = Color.parseColor("#4F46E5")
                    textSize = 14f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    textAlign = Paint.Align.RIGHT
                }
                val linePaint = Paint().apply {
                    color = Color.parseColor("#E2E8F0")
                    strokeWidth = 1f
                }

                canvas.drawRect(0f, 0f, 595f, 150f, Paint().apply { color = Color.parseColor("#F8FAFC") })
                canvas.drawText("فنتراك الجزائر - تقرير مالي شامل", 297f, 65f, titlePaint)
                canvas.drawText("قداشّ - تقرير مالي شامل", 297f, 95f, subtitlePaint)

                val sdf = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault())
                canvas.drawText("تاريخ التصدير: ${sdf.format(Date())}", 550f, 130f, bodyPaint)
                canvas.drawText("تقرير: ${request.reportType}", 50f, 130f, Paint(bodyPaint).apply { textAlign = Paint.Align.LEFT })
                canvas.drawLine(0f, 150f, 595f, 150f, Paint().apply { color = Color.parseColor("#CBD5E1"); strokeWidth = 2f })

                var yPos = 190f
                canvas.drawText("1. ملخص القياسات والأداء المالي", 550f, yPos, headerPaint)
                yPos += 15f
                canvas.drawLine(50f, yPos, 550f, yPos, linePaint)
                yPos += 25f

                val totalBalanceFormatted = com.example.core.utils.FormatterUtils.formatCurrency(totalBalance)
                canvas.drawText("• إجمالي الرصيد المتوفر: $totalBalanceFormatted", 540f, yPos, bodyPaint)
                yPos += 22f
                canvas.drawText("توزيع الحسابات المالية النشطة:", 540f, yPos, Paint(bodyPaint).apply { typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD) })
                yPos += 20f

                if (accounts.isEmpty()) {
                    canvas.drawText("  - لا توجد حسابات مسجلة حالياً.", 530f, yPos, bodyPaint)
                    yPos += 20f
                } else {
                    accounts.forEach { acc ->
                        val accTypeLabel = when (acc.type) {
                            "BARIDIMOB" -> "بريدي موب"
                            "CCP" -> "حساب CCP"
                            "CASH" -> "نقدي/كاش"
                            "SAVINGS" -> "حصالة الادخار"
                            "BANK" -> "بنك"
                            "WALLET" -> "محفظة"
                            else -> "حساب آخر"
                        }
                        val balanceFormatted = com.example.core.utils.FormatterUtils.formatCurrency(acc.balance)
                        canvas.drawText("  - ${acc.name} ($accTypeLabel): $balanceFormatted", 530f, yPos, bodyPaint)
                        yPos += 20f
                    }
                }
                yPos += 25f

                if (request.includeSavingsSection) {
                    canvas.drawText("2. تقرير الأهداف الادخارية والمجموعات الاستثمارية", 550f, yPos, headerPaint)
                    yPos += 15f
                    canvas.drawLine(50f, yPos, 550f, yPos, linePaint)
                    yPos += 30f
                    canvas.drawText("• حالة الادخار الحالية تظهر تحسناً ملحوظاً بنسبة تزيد عن الربع مقارنة بالشهور الماضية.", 560f, yPos, bodyPaint)
                    yPos += 25f
                    canvas.drawText("• الالتزام بالمساهمات التلقائية الموصى بها يساعد على تقليص فترات بلوغ الأهداف.", 560f, yPos, bodyPaint)
                    yPos += 45f
                }

                if (request.includeDebtSection) {
                    canvas.drawText("3. تقرير تسوية الديون وتقليل الأعباء", 550f, yPos, headerPaint)
                    yPos += 15f
                    canvas.drawLine(50f, yPos, 550f, yPos, linePaint)
                    yPos += 30f
                    canvas.drawText("• يتبع التطبيق استراتيجية كرة الثلج (Snowball Method) لتسريع سداد الديون الأصغر كخطوة أولى.", 560f, yPos, bodyPaint)
                    yPos += 25f
                    canvas.drawText("• تفعيل دفعات شهرية إضافية يعجل بتحقيق الاستقلال المالي الكامل.", 560f, yPos, bodyPaint)
                }

                canvas.drawRect(0f, 792f, 595f, 842f, Paint().apply { color = Color.parseColor("#F1F5F9") })
                canvas.drawText("تم توليد هذا التقرير بأمان وسرية تامة محلياً في هاتفك بواسطة قداشّ.", 297f, 820f, Paint(subtitlePaint).apply { textSize = 9f; typeface = Typeface.DEFAULT })
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

    private fun sanitizeReportFileName(fileName: String): String {
        val baseName = File(fileName).name
            .replace(Regex("[^A-Za-z0-9._-]"), "_")
            .trim('_')
        return baseName.ifBlank { "kdach_report_${System.currentTimeMillis()}" }
    }
}
