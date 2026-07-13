package com.example.utils

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Environment
import com.example.data.model.AttendanceRecord
import com.example.data.model.User
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

object ExportHelper {

    fun exportToCsv(
        context: Context,
        records: List<AttendanceRecord>,
        users: List<User>
    ): File? {
        val fileName = "Laporan_Absensi_${System.currentTimeMillis()}.csv"
        val userMap = users.associateBy { it.id }

        try {
            val file = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), fileName)
            val writer = FileOutputStream(file).bufferedWriter()

            // Header (UTF-8 BOM to prevent character issue in Excel)
            writer.write('\ufeff'.code)
            writer.write("No,Nama,ID Unik,Jabatan/Kelas,Role,Tanggal,Waktu Masuk,Waktu Pulang,Status,Keterangan\n")

            records.forEachIndexed { index, record ->
                val user = userMap[record.userId]
                val name = user?.name ?: "Unknown"
                val uniqueCode = user?.uniqueCode ?: ""
                val jabatan = user?.jabatan ?: ""
                val role = user?.role ?: ""
                
                val clockIn = record.clockInTime ?: "-"
                val clockOut = record.clockOutTime ?: "-"
                val notes = record.notes ?: "-"

                writer.write(
                    "${index + 1},\"$name\",\"$uniqueCode\",\"$jabatan\",\"$role\"," +
                            "\"${record.date}\",\"$clockIn\",\"$clockOut\",\"${record.status}\",\"$notes\"\n"
                )
            }
            writer.close()
            return file
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    fun exportToPdf(
        context: Context,
        records: List<AttendanceRecord>,
        users: List<User>
    ): File? {
        val fileName = "Laporan_Absensi_${System.currentTimeMillis()}.pdf"
        val userMap = users.associateBy { it.id }

        val pdfDocument = PdfDocument()
        val paint = Paint()
        val titlePaint = Paint().apply {
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = 18f
            color = Color.BLACK
        }
        val subPaint = Paint().apply {
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            textSize = 10f
            color = Color.DKGRAY
        }
        val headerPaint = Paint().apply {
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = 10f
            color = Color.WHITE
        }
        val bodyPaint = Paint().apply {
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            textSize = 9f
            color = Color.BLACK
        }
        val borderPaint = Paint().apply {
            style = Paint.Style.STROKE
            strokeWidth = 0.5f
            color = Color.LTGRAY
        }

        // A4 page dimensions are 595 x 842 points
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        var page = pdfDocument.startPage(pageInfo)
        var canvas = page.canvas

        // Header
        canvas.drawText("REKAPITULASI LAPORAN ABSENSI", 30f, 50f, titlePaint)
        val dateGenerated = SimpleDateFormat("dd MMMM yyyy HH:mm:ss", Locale.getDefault()).format(Date())
        canvas.drawText("Dicetak pada: $dateGenerated", 30f, 70f, subPaint)
        canvas.drawText("Aplikasi AbsenQR - Absensi Mandiri QR Code", 30f, 85f, subPaint)

        // Draw Table Header
        val startY = 110f
        val rowHeight = 25f

        // Draw header background
        paint.color = Color.parseColor("#1A73E8") // Google Blue
        paint.style = Paint.Style.FILL
        canvas.drawRect(30f, startY, 565f, startY + rowHeight, paint)

        // Draw header text
        canvas.drawText("No", 35f, startY + 16f, headerPaint)
        canvas.drawText("Nama", 60f, startY + 16f, headerPaint)
        canvas.drawText("Tanggal", 200f, startY + 16f, headerPaint)
        canvas.drawText("Masuk", 280f, startY + 16f, headerPaint)
        canvas.drawText("Pulang", 340f, startY + 16f, headerPaint)
        canvas.drawText("Status", 410f, startY + 16f, headerPaint)
        canvas.drawText("Jabatan", 480f, startY + 16f, headerPaint)

        var currentY = startY + rowHeight
        var recordCounter = 1

        records.forEach { record ->
            // If we run out of page vertical space, start a new page
            if (currentY > 780f) {
                pdfDocument.finishPage(page)
                val newPageInfo = PdfDocument.PageInfo.Builder(595, 842, pdfDocument.pages.size + 1).create()
                page = pdfDocument.startPage(newPageInfo)
                canvas = page.canvas
                currentY = 50f

                // Re-draw small header on subsequent pages
                paint.color = Color.parseColor("#1A73E8")
                paint.style = Paint.Style.FILL
                canvas.drawRect(30f, currentY, 565f, currentY + rowHeight, paint)
                
                canvas.drawText("No", 35f, currentY + 16f, headerPaint)
                canvas.drawText("Nama", 60f, currentY + 16f, headerPaint)
                canvas.drawText("Tanggal", 200f, currentY + 16f, headerPaint)
                canvas.drawText("Masuk", 280f, currentY + 16f, headerPaint)
                canvas.drawText("Pulang", 340f, currentY + 16f, headerPaint)
                canvas.drawText("Status", 410f, currentY + 16f, headerPaint)
                canvas.drawText("Jabatan", 480f, currentY + 16f, headerPaint)
                
                currentY += rowHeight
            }

            val user = userMap[record.userId]
            val name = user?.name ?: "Unknown"
            val clockIn = record.clockInTime ?: "-"
            val clockOut = record.clockOutTime ?: "-"
            val jabatan = user?.jabatan ?: ""

            // Draw zebra striping
            if (recordCounter % 2 == 0) {
                paint.color = Color.parseColor("#F1F3F4")
                paint.style = Paint.Style.FILL
                canvas.drawRect(30f, currentY, 565f, currentY + rowHeight, paint)
            }

            // Draw borders
            canvas.drawRect(30f, currentY, 565f, currentY + rowHeight, borderPaint)

            // Draw texts
            canvas.drawText(recordCounter.toString(), 35f, currentY + 16f, bodyPaint)
            
            // Truncate name if too long
            val displayName = if (name.length > 22) name.substring(0, 20) + ".." else name
            canvas.drawText(displayName, 60f, currentY + 16f, bodyPaint)
            
            canvas.drawText(record.date, 200f, currentY + 16f, bodyPaint)
            canvas.drawText(clockIn, 280f, currentY + 16f, bodyPaint)
            canvas.drawText(clockOut, 340f, currentY + 16f, bodyPaint)

            // Status color highlight
            val statusColor = when (record.status) {
                "Hadir" -> Color.parseColor("#137333") // Green
                "Terlambat" -> Color.parseColor("#B06000") // Orange
                "Izin", "Cuti" -> Color.parseColor("#1A73E8") // Blue
                else -> Color.parseColor("#C5221F") // Red
            }
            val statusPaint = Paint().apply {
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textSize = 9f
                color = statusColor
            }
            canvas.drawText(record.status, 410f, currentY + 16f, statusPaint)

            val displayJabatan = if (jabatan.length > 13) jabatan.substring(0, 11) + ".." else jabatan
            canvas.drawText(displayJabatan, 480f, currentY + 16f, bodyPaint)

            currentY += rowHeight
            recordCounter++
        }

        pdfDocument.finishPage(page)

        try {
            val file = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), fileName)
            val outputStream = FileOutputStream(file)
            pdfDocument.writeTo(outputStream)
            outputStream.close()
            pdfDocument.close()
            return file
        } catch (e: Exception) {
            e.printStackTrace()
            pdfDocument.close()
            return null
        }
    }
}
