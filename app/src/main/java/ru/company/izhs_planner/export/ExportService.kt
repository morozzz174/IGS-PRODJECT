package ru.company.izhs_planner.export

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.graphics.Typeface
import ru.company.izhs_planner.domain.model.Project
import java.io.File
import java.io.FileOutputStream

class ExportService(private val context: Context) {
    private val pageWidth = 595
    private val pageHeight = 842
    private val margin = 50f
    
    fun exportToPdf(project: Project, includeWatermark: Boolean = true): File? {
        val document = PdfDocument()
        
        val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
        val page = document.startPage(pageInfo)
        
        val canvas = page.canvas
        drawProjectPages(canvas, project, includeWatermark)
        
        document.finishPage(page)
        
        val file = File(context.getExternalFilesDir(null), "project_${project.id}.pdf")
        try {
            FileOutputStream(file).use { fos ->
                document.writeTo(fos)
            }
            document.close()
            return file
        } catch (e: Exception) {
            document.close()
            return null
        }
    }
    
    private fun drawProjectPages(canvas: Canvas, project: Project, includeWatermark: Boolean) {
        val titlePaint = Paint().apply {
            color = Color.parseColor("#1B5E42")
            textSize = 24f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        
        val headerPaint = Paint().apply {
            color = Color.BLACK
            textSize = 16f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        
        val textPaint = Paint().apply {
            color = Color.DKGRAY
            textSize = 12f
            isAntiAlias = true
        }
        
        val linePaint = Paint().apply {
            color = Color.parseColor("#1B5E42")
            strokeWidth = 2f
        }
        
        var y = margin
        
        canvas.drawText("ИЖС-Проектировщик", margin, y + 24, titlePaint)
        y += 40f
        
        canvas.drawLine(margin, y, pageWidth - margin, y, linePaint)
        y += 30f
        
        canvas.drawText("Проект: ${project.name.ifEmpty { "Без названия" }}", margin, y, headerPaint)
        y += 25f
        
        if (project.description.isNotEmpty()) {
            canvas.drawText(project.description, margin, y, textPaint)
            y += 30f
        }
        
        canvas.drawText("Дата: ${java.text.SimpleDateFormat("dd.MM.yyyy", java.util.Locale.getDefault()).format(java.util.Date(project.updatedAt))}", margin, y, textPaint)
        y += 40f
        
        canvas.drawText("Параметры участка", margin, y, headerPaint)
        y += 20f
        canvas.drawText("• Размеры: ${project.plot.width} × ${project.plot.length} м (${project.plot.area} сот.)", margin, y, textPaint)
        y += 18f
        canvas.drawText("• Уклон: ${project.plot.slope}%", margin, y, textPaint)
        y += 35f
        
        canvas.drawText("Параметры дома", margin, y, headerPaint)
        y += 20f
        canvas.drawText("• Размеры: ${project.house.width} × ${project.house.length} м", margin, y, textPaint)
        y += 18f
        canvas.drawText("• Этажность: ${project.house.floors}", margin, y, textPaint)
        y += 18f
        canvas.drawText("• Высота этажа: ${project.house.floorHeight} м", margin, y, textPaint)
        y += 18f
        canvas.drawText("• Тип кровли: ${project.house.roofType.name.lowercase().replaceFirstChar { it.uppercase() }}", margin, y, textPaint)
        y += 18f
        canvas.drawText("• Фундамент: ${project.house.foundationType.name.lowercase().replaceFirstChar { it.uppercase() }}", margin, y, textPaint)
        y += 18f
        canvas.drawText("• Материал стен: ${project.house.wallMaterial.displayName}", margin, y, textPaint)
        y += 35f
        
        canvas.drawText("Площади", margin, y, headerPaint)
        y += 20f
        canvas.drawText("• Общая площадь: ${project.totalArea} м²", margin, y, textPaint)
        y += 18f
        canvas.drawText("• Жилая площадь: ${project.livingArea} м²", margin, y, textPaint)
        
        if (includeWatermark) {
            val watermarkPaint = Paint().apply {
                color = Color.parseColor("#AA1B5E42")
                textSize = 14f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText(
                "Создано в приложении «ИЖС-Проектировщик»",
                pageWidth / 2f,
                pageHeight - margin,
                watermarkPaint
            )
        }
    }
    
    fun exportToObj(project: Project): File? {
        val file = File(context.getExternalFilesDir(null), "house_${project.id}.obj")
        
        try {
            file.bufferedWriter().use { writer ->
                writer.write("# ИЖС-Проектировщик - Экспорт OBJ\n")
                writer.write("# Проект: ${project.name.ifEmpty { "Без названия" }}\n\n")
                
                val house = project.house
                writer.write("# Дом: ${house.width}x${house.length}, ${house.floors} эт.\n\n")
                
                writer.write("g house\n")
                
                writer.write("# Фундамент\n")
                writer.write("v ${-house.width/2} 0 ${-house.length/2}\n")
                writer.write("v ${house.width/2} 0 ${-house.length/2}\n")
                writer.write("v ${house.width/2} 0 ${house.length/2}\n")
                writer.write("v ${-house.width/2} 0 ${house.length/2}\n")
                
                writer.write("\n# Стены\n")
                val wallHeight = house.floorHeight * house.floors
                writer.write("v ${-house.width/2} $wallHeight ${-house.length/2}\n")
                writer.write("v ${house.width/2} $wallHeight ${-house.length/2}\n")
                writer.write("v ${house.width/2} $wallHeight ${house.length/2}\n")
                writer.write("v ${-house.width/2} $wallHeight ${house.length/2}\n")
            }
            return file
        } catch (e: Exception) {
            return null
        }
    }
    
    fun exportToGlb(project: Project): File? {
        val file = File(context.getExternalFilesDir(null), "house_${project.id}.glb")
        
        return file
    }
}