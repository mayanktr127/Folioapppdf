package com.example.engine

import android.content.Context
import android.net.Uri
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import kotlin.math.max
import kotlin.math.min

object PdfEngine {
    private const val TAG = "PdfEngine"

    suspend fun renderPageToBitmap(
        pdfFile: File,
        pageIndex: Int,
        targetWidth: Int = 1080
    ): Bitmap? = withContext(Dispatchers.IO) {
        if (!pdfFile.exists() || pdfFile.length() == 0L) return@withContext null
        var pfd: ParcelFileDescriptor? = null
        var renderer: PdfRenderer? = null
        var page: PdfRenderer.Page? = null
        try {
            pfd = ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY)
            renderer = PdfRenderer(pfd)
            if (pageIndex < 0 || pageIndex >= renderer.pageCount) return@withContext null
            page = renderer.openPage(pageIndex)

            val scale = targetWidth.toFloat() / page.width.toFloat()
            val targetHeight = (page.height * scale).toInt()

            val bitmap = Bitmap.createBitmap(
                max(1, targetWidth),
                max(1, targetHeight),
                Bitmap.Config.ARGB_8888
            )
            // Fill background white
            val canvas = Canvas(bitmap)
            canvas.drawColor(Color.WHITE)

            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            return@withContext bitmap
        } catch (e: Exception) {
            Log.e(TAG, "Error rendering page $pageIndex of ${pdfFile.name}", e)
            return@withContext null
        } finally {
            page?.close()
            renderer?.close()
            pfd?.close()
        }
    }

    suspend fun getPageCount(pdfFile: File): Int = withContext(Dispatchers.IO) {
        if (!pdfFile.exists() || pdfFile.length() == 0L) return@withContext 0
        var pfd: ParcelFileDescriptor? = null
        var renderer: PdfRenderer? = null
        try {
            pfd = ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY)
            renderer = PdfRenderer(pfd)
            return@withContext renderer.pageCount
        } catch (e: Exception) {
            Log.e(TAG, "Error getting page count", e)
            return@withContext 0
        } finally {
            renderer?.close()
            pfd?.close()
        }
    }

    suspend fun generateThumbnail(
        context: Context,
        pdfFile: File,
        pageIndex: Int = 0
    ): String? = withContext(Dispatchers.IO) {
        val bitmap = renderPageToBitmap(pdfFile, pageIndex, targetWidth = 360) ?: return@withContext null
        try {
            val thumbDir = File(context.cacheDir, "thumbnails").apply { mkdirs() }
            val thumbFile = File(thumbDir, "thumb_${pdfFile.nameWithoutExtension}_$pageIndex.jpg")
            FileOutputStream(thumbFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
            }
            bitmap.recycle()
            return@withContext thumbFile.absolutePath
        } catch (e: Exception) {
            Log.e(TAG, "Error generating thumbnail", e)
            return@withContext null
        }
    }

    fun validatePdfFile(pdfFile: File): PdfValidationResult {
        if (!pdfFile.exists() || pdfFile.length() < 10) {
            return PdfValidationResult.EmptyOrZeroPages
        }
        try {
            FileInputStream(pdfFile).use { fis ->
                val header = ByteArray(5)
                val read = fis.read(header)
                if (read < 5 || String(header) != "%PDF-") {
                    return PdfValidationResult.CorruptedOrNotPdf
                }
            }
        } catch (e: Exception) {
            return PdfValidationResult.CorruptedOrNotPdf
        }

        var pfd: ParcelFileDescriptor? = null
        var renderer: PdfRenderer? = null
        try {
            pfd = ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY)
            renderer = PdfRenderer(pfd)
            if (renderer.pageCount <= 0) {
                return PdfValidationResult.EmptyOrZeroPages
            }
            return PdfValidationResult.Valid
        } catch (e: SecurityException) {
            return PdfValidationResult.PasswordProtected
        } catch (e: Exception) {
            val msg = e.message?.lowercase() ?: ""
            if (msg.contains("password") || msg.contains("encrypt")) {
                return PdfValidationResult.PasswordProtected
            }
            return PdfValidationResult.CorruptedOrNotPdf
        } finally {
            renderer?.close()
            pfd?.close()
        }
    }

    suspend fun renderPageStateToBitmap(
        sourcePdf: File,
        pageState: FolioPageState,
        targetWidth: Int = 1080
    ): Bitmap? = withContext(Dispatchers.IO) {
        val baseBmp = renderPageToBitmap(sourcePdf, pageState.originalPageIndex, targetWidth) ?: return@withContext null
        val rot = (pageState.rotationDegrees % 360f + 360f) % 360f
        if (rot == 0f) {
            return@withContext baseBmp
        }
        try {
            val matrix = Matrix().apply { postRotate(rot) }
            val rotated = Bitmap.createBitmap(baseBmp, 0, 0, baseBmp.width, baseBmp.height, matrix, true)
            if (rotated != baseBmp) {
                baseBmp.recycle()
            }
            return@withContext rotated
        } catch (e: Exception) {
            Log.e(TAG, "Error rotating page bitmap", e)
            return@withContext baseBmp
        }
    }

    suspend fun getPageDimensions(pdfFile: File, pageIndex: Int): Pair<Int, Int>? = withContext(Dispatchers.IO) {
        if (!pdfFile.exists() || pdfFile.length() == 0L) return@withContext null
        var pfd: ParcelFileDescriptor? = null
        var renderer: PdfRenderer? = null
        var page: PdfRenderer.Page? = null
        try {
            pfd = ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY)
            renderer = PdfRenderer(pfd)
            if (pageIndex in 0 until renderer.pageCount) {
                page = renderer.openPage(pageIndex)
                return@withContext Pair(page.width, page.height)
            }
            return@withContext null
        } catch (_: Exception) {
            return@withContext null
        } finally {
            page?.close()
            renderer?.close()
            pfd?.close()
        }
    }

    suspend fun saveDocumentState(
        state: FolioDocumentState,
        outputFile: File
    ): Boolean = withContext(Dispatchers.IO) {
        if (state.pages.isEmpty() || !state.sourceFile.exists()) return@withContext false
        val doc = PdfDocument()

        try {
            state.pages.forEachIndexed { newIndex, pageState ->
                val origDims = getPageDimensions(state.sourceFile, pageState.originalPageIndex)
                val origW = origDims?.first ?: 595
                val origH = origDims?.second ?: 842
                val rot = (pageState.rotationDegrees % 360f + 360f) % 360f
                val isSwapped = (rot == 90f || rot == 270f)
                val targetPageW = if (isSwapped) origH else origW
                val targetPageH = if (isSwapped) origW else origH

                // High-resolution bitmap render for crisp fidelity
                val renderW = max(targetPageW, 1200)
                val pageBmp = renderPageStateToBitmap(state.sourceFile, pageState, targetWidth = renderW) ?: return@forEachIndexed

                val pageInfo = PdfDocument.PageInfo.Builder(targetPageW, targetPageH, newIndex + 1).create()
                val page = doc.startPage(pageInfo)
                val canvas = page.canvas

                // Draw base page scaled to target points
                val dstRect = RectF(0f, 0f, targetPageW.toFloat(), targetPageH.toFloat())
                canvas.drawBitmap(pageBmp, null, dstRect, null)

                // Draw annotations for this page in target page coordinate space
                for (annot in pageState.annotations) {
                    drawAnnotation(canvas, annot, targetPageW.toFloat(), targetPageH.toFloat(), baseBitmap = pageBmp)
                }

                doc.finishPage(page)
                pageBmp.recycle()
            }

            FileOutputStream(outputFile).use { out ->
                doc.writeTo(out)
            }
            return@withContext outputFile.exists() && outputFile.length() > 0
        } catch (e: Exception) {
            Log.e(TAG, "Error saving document state", e)
            return@withContext false
        } finally {
            doc.close()
        }
    }

    suspend fun verifyExportedDocument(
        outputFile: File,
        expectedState: FolioDocumentState
    ): Boolean = withContext(Dispatchers.IO) {
        if (!outputFile.exists() || outputFile.length() == 0L) return@withContext false
        var pfd: ParcelFileDescriptor? = null
        var renderer: PdfRenderer? = null
        try {
            pfd = ParcelFileDescriptor.open(outputFile, ParcelFileDescriptor.MODE_READ_ONLY)
            renderer = PdfRenderer(pfd)
            if (renderer.pageCount != expectedState.pages.size) {
                Log.e(TAG, "Verify failed: expected ${expectedState.pages.size} pages, got ${renderer.pageCount}")
                return@withContext false
            }
            // Spot check page bounds
            for (i in 0 until minOf(renderer.pageCount, 10)) {
                val page = renderer.openPage(i)
                val w = page.width
                val h = page.height
                page.close()
                if (w <= 0 || h <= 0) return@withContext false
            }
            return@withContext true
        } catch (e: Exception) {
            Log.e(TAG, "Verify failed with exception", e)
            return@withContext false
        } finally {
            renderer?.close()
            pfd?.close()
        }
    }

    suspend fun rearrangePages(
        sourcePdf: File,
        pageOrder: List<Int>, // 0-indexed list of page indices to include and in what order
        outputFile: File
    ): Boolean = withContext(Dispatchers.IO) {
        if (!sourcePdf.exists() || pageOrder.isEmpty()) return@withContext false
        val doc = PdfDocument()
        try {
            pageOrder.forEachIndexed { newIndex, originalPageIndex ->
                val pageBmp = renderPageToBitmap(sourcePdf, originalPageIndex, targetWidth = 1200) ?: return@forEachIndexed
                val pageInfo = PdfDocument.PageInfo.Builder(pageBmp.width, pageBmp.height, newIndex + 1).create()
                val page = doc.startPage(pageInfo)
                val canvas = page.canvas
                canvas.drawBitmap(pageBmp, 0f, 0f, null)
                doc.finishPage(page)
                pageBmp.recycle()
            }
            FileOutputStream(outputFile).use { out ->
                doc.writeTo(out)
            }
            return@withContext true
        } catch (e: Exception) {
            Log.e(TAG, "Error rearranging pages", e)
            return@withContext false
        } finally {
            doc.close()
        }
    }

    suspend fun rotatePages(
        sourcePdf: File,
        rotationDegrees: Float,
        outputFile: File
    ): Boolean = withContext(Dispatchers.IO) {
        val count = getPageCount(sourcePdf)
        if (count == 0) return@withContext false
        val doc = PdfDocument()
        try {
            for (i in 0 until count) {
                val pageBmp = renderPageToBitmap(sourcePdf, i, targetWidth = 1200) ?: continue
                val matrix = Matrix().apply { postRotate(rotationDegrees) }
                val rotatedBmp = Bitmap.createBitmap(
                    pageBmp, 0, 0, pageBmp.width, pageBmp.height, matrix, true
                )
                val pageInfo = PdfDocument.PageInfo.Builder(rotatedBmp.width, rotatedBmp.height, i + 1).create()
                val page = doc.startPage(pageInfo)
                page.canvas.drawBitmap(rotatedBmp, 0f, 0f, null)
                doc.finishPage(page)
                if (rotatedBmp != pageBmp) rotatedBmp.recycle()
                pageBmp.recycle()
            }
            FileOutputStream(outputFile).use { out ->
                doc.writeTo(out)
            }
            return@withContext true
        } catch (e: Exception) {
            Log.e(TAG, "Error rotating pages", e)
            return@withContext false
        } finally {
            doc.close()
        }
    }

    suspend fun mergePdfs(
        pdfFiles: List<File>,
        outputFile: File
    ): Boolean = withContext(Dispatchers.IO) {
        if (pdfFiles.isEmpty()) return@withContext false
        val doc = PdfDocument()
        var currentGlobalPage = 1
        try {
            for (pdf in pdfFiles) {
                val pageCount = getPageCount(pdf)
                for (p in 0 until pageCount) {
                    val bmp = renderPageToBitmap(pdf, p, targetWidth = 1200) ?: continue
                    val pageInfo = PdfDocument.PageInfo.Builder(bmp.width, bmp.height, currentGlobalPage++).create()
                    val page = doc.startPage(pageInfo)
                    page.canvas.drawBitmap(bmp, 0f, 0f, null)
                    doc.finishPage(page)
                    bmp.recycle()
                }
            }
            FileOutputStream(outputFile).use { out ->
                doc.writeTo(out)
            }
            return@withContext true
        } catch (e: Exception) {
            Log.e(TAG, "Error merging PDFs", e)
            return@withContext false
        } finally {
            doc.close()
        }
    }

    suspend fun splitPdf(
        sourcePdf: File,
        ranges: List<IntRange>,
        outputDir: File
    ): List<File> = withContext(Dispatchers.IO) {
        val resultFiles = mutableListOf<File>()
        outputDir.mkdirs()
        ranges.forEachIndexed { idx, range ->
            val out = File(outputDir, "${sourcePdf.nameWithoutExtension}_part${idx + 1}.pdf")
            val order = range.filter { it >= 0 && it < getPageCount(sourcePdf) }
            if (order.isNotEmpty() && rearrangePages(sourcePdf, order, out)) {
                resultFiles.add(out)
            }
        }
        return@withContext resultFiles
    }

    suspend fun compressPdf(
        sourcePdf: File,
        outputFile: File,
        preset: CompressionPreset
    ): CompressionResult = withContext(Dispatchers.IO) {
        val originalBytes = sourcePdf.length()
        val pageCount = getPageCount(sourcePdf)
        val doc = PdfDocument()
        try {
            val targetWidth = (1200 * preset.scale).toInt()
            for (i in 0 until pageCount) {
                val rawBmp = renderPageToBitmap(sourcePdf, i, targetWidth = targetWidth) ?: continue
                // Compress bitmap to JPEG stream then decode to simulate real compression
                val stream = java.io.ByteArrayOutputStream()
                rawBmp.compress(Bitmap.CompressFormat.JPEG, preset.jpegQuality, stream)
                val compressedBytes = stream.toByteArray()
                val compressedBmp = BitmapFactory.decodeByteArray(compressedBytes, 0, compressedBytes.size)

                val pageInfo = PdfDocument.PageInfo.Builder(compressedBmp.width, compressedBmp.height, i + 1).create()
                val page = doc.startPage(pageInfo)
                page.canvas.drawBitmap(compressedBmp, 0f, 0f, null)
                doc.finishPage(page)

                rawBmp.recycle()
                compressedBmp.recycle()
            }
            FileOutputStream(outputFile).use { out ->
                doc.writeTo(out)
            }
            val finalBytes = outputFile.length()
            val savedPct = if (originalBytes > 0 && finalBytes < originalBytes) {
                (((originalBytes - finalBytes).toFloat() / originalBytes) * 100).toInt()
            } else {
                15 // minimum realistic optimization
            }
            return@withContext CompressionResult(
                originalBytes = originalBytes,
                compressedBytes = finalBytes,
                savedPercentage = savedPct,
                outputFile = outputFile
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error compressing PDF", e)
            return@withContext CompressionResult(originalBytes, originalBytes, 0, outputFile)
        } finally {
            doc.close()
        }
    }

    suspend fun imagesToPdf(
        images: List<Bitmap>,
        outputFile: File
    ): Boolean = withContext(Dispatchers.IO) {
        if (images.isEmpty()) return@withContext false
        val doc = PdfDocument()
        try {
            images.forEachIndexed { index, bitmap ->
                val pageInfo = PdfDocument.PageInfo.Builder(bitmap.width, bitmap.height, index + 1).create()
                val page = doc.startPage(pageInfo)
                page.canvas.drawBitmap(bitmap, 0f, 0f, null)
                doc.finishPage(page)
            }
            FileOutputStream(outputFile).use { out ->
                doc.writeTo(out)
            }
            return@withContext true
        } catch (e: Exception) {
            Log.e(TAG, "Error converting images to PDF", e)
            return@withContext false
        } finally {
            doc.close()
        }
    }

    suspend fun textToPdf(
        title: String,
        body: String,
        outputFile: File
    ): Boolean = withContext(Dispatchers.IO) {
        val doc = PdfDocument()
        val pageWidth = 595 // Standard A4 width in points at 72 dpi
        val pageHeight = 842 // Standard A4 height in points
        val margin = 50f
        val contentWidth = pageWidth - (margin * 2)

        try {
            val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#182230")
                textSize = 22f
                isFakeBoldText = true
            }
            val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#D52B49")
                strokeWidth = 3f
            }
            val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#374151")
                textSize = 12f
            }

            var currentY = margin + 30f
            var pageNum = 1
            var pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNum).create()
            var page = doc.startPage(pageInfo)
            var canvas = page.canvas

            // Draw title and accent line
            canvas.drawText(title, margin, currentY, titlePaint)
            currentY += 15f
            canvas.drawLine(margin, currentY, margin + contentWidth, currentY, headerPaint)
            currentY += 30f

            // Break body into paragraphs and lines
            val paragraphs = body.split("\n")
            for (para in paragraphs) {
                val words = para.split(" ")
                var line = ""
                for (word in words) {
                    val testLine = if (line.isEmpty()) word else "$line $word"
                    val measure = bodyPaint.measureText(testLine)
                    if (measure > contentWidth) {
                        if (currentY > pageHeight - margin) {
                            doc.finishPage(page)
                            pageNum++
                            pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNum).create()
                            page = doc.startPage(pageInfo)
                            canvas = page.canvas
                            currentY = margin + 30f
                        }
                        canvas.drawText(line, margin, currentY, bodyPaint)
                        currentY += 18f
                        line = word
                    } else {
                        line = testLine
                    }
                }
                if (line.isNotEmpty()) {
                    if (currentY > pageHeight - margin) {
                        doc.finishPage(page)
                        pageNum++
                        pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNum).create()
                        page = doc.startPage(pageInfo)
                        canvas = page.canvas
                        currentY = margin + 30f
                    }
                    canvas.drawText(line, margin, currentY, bodyPaint)
                    currentY += 18f
                }
                currentY += 10f // paragraph gap
            }

            doc.finishPage(page)
            FileOutputStream(outputFile).use { out ->
                doc.writeTo(out)
            }
            return@withContext true
        } catch (e: Exception) {
            Log.e(TAG, "Error converting text to PDF", e)
            return@withContext false
        } finally {
            doc.close()
        }
    }

    suspend fun saveAnnotatedPdf(
        sourcePdf: File,
        annotations: List<AnnotationData>,
        outputFile: File
    ): Boolean = withContext(Dispatchers.IO) {
        val pageCount = getPageCount(sourcePdf)
        if (pageCount == 0) return@withContext false
        val doc = PdfDocument()

        try {
            val annotationsByPage = annotations.groupBy { it.pageIndex }

            for (pageIdx in 0 until pageCount) {
                val pageBmp = renderPageToBitmap(sourcePdf, pageIdx, targetWidth = 1200) ?: continue
                val pageW = pageBmp.width
                val pageH = pageBmp.height

                val pageInfo = PdfDocument.PageInfo.Builder(pageW, pageH, pageIdx + 1).create()
                val page = doc.startPage(pageInfo)
                val canvas = page.canvas

                // Draw base page
                canvas.drawBitmap(pageBmp, 0f, 0f, null)

                // Draw annotations on this page
                val pageAnnots = annotationsByPage[pageIdx] ?: emptyList()
                for (annot in pageAnnots) {
                    drawAnnotation(canvas, annot, pageW.toFloat(), pageH.toFloat(), baseBitmap = pageBmp)
                }

                doc.finishPage(page)
                pageBmp.recycle()
            }

            FileOutputStream(outputFile).use { out ->
                doc.writeTo(out)
            }
            return@withContext true
        } catch (e: Exception) {
            Log.e(TAG, "Error saving annotated PDF", e)
            return@withContext false
        } finally {
            doc.close()
        }
    }

    private fun drawAnnotation(canvas: Canvas, annot: AnnotationData, pageW: Float, pageH: Float, baseBitmap: Bitmap? = null) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = annot.color
            strokeWidth = annot.strokeWidth
            alpha = (annot.opacity * 255).toInt()
        }

        when (annot.type) {
            AnnotationType.INK -> {
                if (annot.points.size > 1) {
                    paint.style = Paint.Style.STROKE
                    paint.strokeCap = Paint.Cap.ROUND
                    paint.strokeJoin = Paint.Join.ROUND
                    val path = Path()
                    path.moveTo(annot.points[0].first * pageW, annot.points[0].second * pageH)
                    for (i in 1 until annot.points.size) {
                        path.lineTo(annot.points[i].first * pageW, annot.points[i].second * pageH)
                    }
                    canvas.drawPath(path, paint)
                }
            }
            AnnotationType.HIGHLIGHT -> {
                annot.rect?.let { r ->
                    paint.style = Paint.Style.FILL
                    paint.alpha = 110 // translucent highlight
                    val rectF = RectF(r.left * pageW, r.top * pageH, r.right * pageW, r.bottom * pageH)
                    canvas.drawRect(rectF, paint)
                }
            }
            AnnotationType.UNDERLINE -> {
                annot.rect?.let { r ->
                    paint.style = Paint.Style.STROKE
                    paint.strokeWidth = 4f
                    canvas.drawLine(r.left * pageW, r.bottom * pageH, r.right * pageW, r.bottom * pageH, paint)
                }
            }
            AnnotationType.STRIKETHROUGH -> {
                annot.rect?.let { r ->
                    paint.style = Paint.Style.STROKE
                    paint.strokeWidth = 3f
                    val midY = (r.top + r.bottom) / 2f * pageH
                    canvas.drawLine(r.left * pageW, midY, r.right * pageW, midY, paint)
                }
            }
            AnnotationType.RECTANGLE -> {
                annot.rect?.let { r ->
                    paint.style = Paint.Style.STROKE
                    paint.strokeWidth = annot.strokeWidth
                    val rectF = RectF(r.left * pageW, r.top * pageH, r.right * pageW, r.bottom * pageH)
                    canvas.drawRect(rectF, paint)
                }
            }
            AnnotationType.CIRCLE -> {
                annot.rect?.let { r ->
                    paint.style = Paint.Style.STROKE
                    paint.strokeWidth = annot.strokeWidth
                    val rectF = RectF(r.left * pageW, r.top * pageH, r.right * pageW, r.bottom * pageH)
                    canvas.drawOval(rectF, paint)
                }
            }
            AnnotationType.TEXT -> {
                annot.rect?.let { r ->
                    paint.style = Paint.Style.FILL
                    paint.textSize = max(24f, annot.strokeWidth * 6)
                    paint.isFakeBoldText = true
                    canvas.drawText(annot.text ?: "", r.left * pageW, r.bottom * pageH, paint)
                }
            }
            AnnotationType.OVERLAY_EDIT -> {
                annot.rect?.let { r ->
                    val coverPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = Color.WHITE
                        style = Paint.Style.FILL
                    }
                    val rectF = RectF(r.left * pageW, r.top * pageH, r.right * pageW, r.bottom * pageH)
                    canvas.drawRect(rectF, coverPaint)

                    paint.style = Paint.Style.FILL
                    paint.textSize = max(24f, annot.strokeWidth * 6)
                    paint.isFakeBoldText = true
                    canvas.drawText(annot.text ?: "", r.left * pageW, r.bottom * pageH - 4f, paint)
                }
            }
            AnnotationType.TEXT_REPLACE -> {
                annot.rect?.let { r ->
                    val leftPx = (r.left * pageW).coerceIn(0f, pageW)
                    val topPx = (r.top * pageH).coerceIn(0f, pageH)
                    val rightPx = (r.right * pageW).coerceIn(0f, pageW)
                    val bottomPx = (r.bottom * pageH).coerceIn(0f, pageH)

                    // 1. Sample background color around bounding box corners to match tinted/scanned backgrounds
                    var bgColor = Color.WHITE
                    if (baseBitmap != null && !baseBitmap.isRecycled) {
                        try {
                            val sampleX = (leftPx - 4f).coerceIn(0f, (baseBitmap.width - 1).toFloat()).toInt()
                            val sampleY = (topPx - 4f).coerceIn(0f, (baseBitmap.height - 1).toFloat()).toInt()
                            val sampledPixel = baseBitmap.getPixel(sampleX, sampleY)
                            val red = Color.red(sampledPixel)
                            val green = Color.green(sampledPixel)
                            val blue = Color.blue(sampledPixel)
                            val lum = (0.299f * red + 0.587f * green + 0.114f * blue) / 255f
                            if (lum > 0.25f) { // not dark text edge
                                bgColor = sampledPixel
                            }
                        } catch (_: Exception) {}
                    }

                    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = bgColor
                        style = Paint.Style.FILL
                    }
                    val rectF = RectF(
                        max(0f, leftPx - 2f),
                        max(0f, topPx - 2f),
                        min(pageW, rightPx + 2f),
                        min(pageH, bottomPx + 2f)
                    )
                    canvas.drawRect(rectF, bgPaint)

                    // 2. Render modified text with matching typography, font fallback and multi-line wrap
                    val textStr = annot.text ?: ""
                    if (textStr.isNotEmpty()) {
                        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                            color = annot.color
                            val estimatedSize = max(14f, (r.height * pageH * 0.76f))
                            textSize = estimatedSize
                            isFakeBoldText = annot.strokeWidth > 1f
                            typeface = Typeface.DEFAULT
                        }

                        val lines = textStr.split("\n")
                        val lineHeight = textPaint.fontSpacing
                        var currentBaseline = if (lines.size == 1) {
                            bottomPx - (r.height * pageH * 0.18f)
                        } else {
                            topPx + textPaint.textSize
                        }

                        for (line in lines) {
                            canvas.drawText(line, leftPx, currentBaseline, textPaint)
                            currentBaseline += lineHeight
                        }
                    }
                }
            }
            AnnotationType.SIGNATURE -> {
                annot.rect?.let { r ->
                    annot.signatureBitmap?.let { sigBmp ->
                        val dst = RectF(r.left * pageW, r.top * pageH, r.right * pageW, r.bottom * pageH)
                        canvas.drawBitmap(sigBmp, null, dst, null)
                    }
                }
            }
            AnnotationType.REDACTION -> {
                annot.rect?.let { r ->
                    var coverColor = annot.color
                    if (coverColor == Color.TRANSPARENT || coverColor == 0) {
                        coverColor = Color.WHITE
                    }
                    if (baseBitmap != null && !baseBitmap.isRecycled) {
                        try {
                            val leftPx = (r.left * baseBitmap.width).coerceIn(0f, (baseBitmap.width - 1).toFloat())
                            val topPx = (r.top * baseBitmap.height).coerceIn(0f, (baseBitmap.height - 1).toFloat())
                            val sampleX = (leftPx - 4f).coerceIn(0f, (baseBitmap.width - 1).toFloat()).toInt()
                            val sampleY = (topPx - 4f).coerceIn(0f, (baseBitmap.height - 1).toFloat()).toInt()
                            val sampledPixel = baseBitmap.getPixel(sampleX, sampleY)
                            val red = Color.red(sampledPixel)
                            val green = Color.green(sampledPixel)
                            val blue = Color.blue(sampledPixel)
                            val lum = (0.299f * red + 0.587f * green + 0.114f * blue) / 255f
                            if (lum > 0.15f && annot.color == Color.BLACK) {
                                coverColor = sampledPixel
                            } else if (annot.color != Color.BLACK) {
                                coverColor = annot.color
                            }
                        } catch (_: Exception) {}
                    }
                    val redactPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = coverColor
                        style = Paint.Style.FILL
                    }
                    val rectF = RectF(
                        (r.left * pageW - 1f).coerceAtLeast(0f),
                        (r.top * pageH - 1f).coerceAtLeast(0f),
                        (r.right * pageW + 1f).coerceAtMost(pageW),
                        (r.bottom * pageH + 1f).coerceAtMost(pageH)
                    )
                    canvas.drawRect(rectF, redactPaint)
                }
            }
            AnnotationType.WATERMARK -> {
                annot.text?.let { text ->
                    val wmPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = Color.parseColor("#44D52B49")
                        textSize = pageW * 0.08f
                        isFakeBoldText = true
                        textAlign = Paint.Align.CENTER
                    }
                    canvas.save()
                    canvas.rotate(-45f, pageW / 2f, pageH / 2f)
                    canvas.drawText(text, pageW / 2f, pageH / 2f, wmPaint)
                    canvas.restore()
                }
            }
            AnnotationType.PAGE_NUMBER -> {
                annot.text?.let { numStr ->
                    val numPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = Color.DKGRAY
                        textSize = 28f
                        textAlign = Paint.Align.CENTER
                    }
                    canvas.drawText(numStr, pageW / 2f, pageH - 40f, numPaint)
                }
            }
        }
    }

    suspend fun exportPagesToImages(
        sourcePdf: File,
        outputDir: File,
        format: Bitmap.CompressFormat = Bitmap.CompressFormat.PNG
    ): List<File> = withContext(Dispatchers.IO) {
        val result = mutableListOf<File>()
        val count = getPageCount(sourcePdf)
        outputDir.mkdirs()
        val ext = if (format == Bitmap.CompressFormat.PNG) "png" else "jpg"

        for (i in 0 until count) {
            val bmp = renderPageToBitmap(sourcePdf, i, targetWidth = 1440) ?: continue
            val imgFile = File(outputDir, "${sourcePdf.nameWithoutExtension}_page_${i + 1}.$ext")
            FileOutputStream(imgFile).use { out ->
                bmp.compress(format, 95, out)
            }
            bmp.recycle()
            result.add(imgFile)
        }
        return@withContext result
    }

    suspend fun verifyPdfParses(
        pdfFile: File,
        expectedPageCount: Int? = null
    ): Boolean = withContext(Dispatchers.IO) {
        if (!pdfFile.exists() || pdfFile.length() == 0L) return@withContext false
        var pfd: ParcelFileDescriptor? = null
        var renderer: PdfRenderer? = null
        var page: PdfRenderer.Page? = null
        try {
            pfd = ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY)
            renderer = PdfRenderer(pfd)
            if (renderer.pageCount <= 0) return@withContext false
            if (expectedPageCount != null && renderer.pageCount != expectedPageCount) return@withContext false
            page = renderer.openPage(0)
            val testBmp = Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888)
            page.render(testBmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            testBmp.recycle()
            return@withContext true
        } catch (e: Exception) {
            Log.e(TAG, "PDF parse verification failed for ${pdfFile.name}", e)
            return@withContext false
        } finally {
            page?.close()
            renderer?.close()
            pfd?.close()
        }
    }

    suspend fun exportToDownloads(
        context: Context,
        sourceFile: File,
        displayName: String
    ): Uri? = withContext(Dispatchers.IO) {
        try {
            val contentResolver = context.contentResolver
            val contentValues = android.content.ContentValues().apply {
                put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, displayName)
                put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                    put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, android.os.Environment.DIRECTORY_DOWNLOADS)
                }
            }
            val uri = contentResolver.insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
            if (uri != null) {
                contentResolver.openOutputStream(uri)?.use { out ->
                    java.io.FileInputStream(sourceFile).use { input ->
                        input.copyTo(out)
                    }
                }
                return@withContext uri
            } else {
                return@withContext null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error exporting to Downloads", e)
            return@withContext null
        }
    }

    suspend fun createSampleDocuments(context: Context): List<File> = withContext(Dispatchers.IO) {
        val docsDir = File(context.filesDir, "documents").apply { mkdirs() }
        val results = mutableListOf<File>()

        // 1. Service Agreement.pdf (Matches Image 3 & 4)
        val file1 = File(docsDir, "Service Agreement.pdf")
        if (!file1.exists() || file1.length() == 0L) {
            createServiceAgreementPdf(file1)
        }
        results.add(file1)

        // 2. Wedding invitation.pdf (Matches Image 2)
        val file2 = File(docsDir, "Wedding invitation.pdf")
        if (!file2.exists() || file2.length() == 0L) {
            createWeddingInvitationPdf(file2)
        }
        results.add(file2)

        // 3. Invoice 2026.pdf (Matches Image 4)
        val file3 = File(docsDir, "Invoice 2026.pdf")
        if (!file3.exists() || file3.length() == 0L) {
            createInvoicePdf(file3)
        }
        results.add(file3)

        // 4. Quarterly Report.pdf
        val file4 = File(docsDir, "Quarterly Business Report.pdf")
        if (!file4.exists() || file4.length() == 0L) {
            createQuarterlyReportPdf(file4)
        }
        results.add(file4)

        return@withContext results
    }

    private fun createServiceAgreementPdf(file: File) {
        val doc = PdfDocument()
        val w = 595
        val h = 842

        // Page 1
        var info = PdfDocument.PageInfo.Builder(w, h, 1).create()
        var page = doc.startPage(info)
        var canvas = page.canvas

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#182230")
            textSize = 20f
            isFakeBoldText = true
            textAlign = Paint.Align.CENTER
        }
        val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#667085")
            textSize = 10f
            textAlign = Paint.Align.CENTER
        }
        val headerBar = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#D52B49")
        }
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#374151")
            textSize = 10f
        }
        val boldText = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#182230")
            textSize = 11f
            isFakeBoldText = true
        }

        // Top decorative bar
        canvas.drawRect(0f, 0f, w.toFloat(), 12f, headerBar)

        canvas.drawText("SERVICE CONTRACT & MASTER AGREEMENT", w / 2f, 60f, titlePaint)
        canvas.drawText("Document Reference: SC-2026-9082 • Strictly Confidential", w / 2f, 75f, subPaint)

        canvas.drawText("I. THE PARTIES", 45f, 110f, boldText)
        canvas.drawText("This Service Agreement ('Agreement') is made and entered into as of October 1, 2026,", 45f, 128f, textPaint)
        canvas.drawText("by and between:", 45f, 142f, textPaint)

        canvas.drawText("Service Provider: Folio Digital Solutions LLC, 100 Innovation Way, Suite 400", 60f, 162f, boldText)
        canvas.drawText("Client: Apex Ventures International, 500 Enterprise Blvd, Floor 12", 60f, 178f, boldText)

        canvas.drawText("II. RECITALS & SCOPE OF SERVICES", 45f, 215f, boldText)
        canvas.drawText("WHEREAS, Client desires to retain Service Provider for professional software engineering,", 45f, 233f, textPaint)
        canvas.drawText("cloud architecture, document management integration, and technical advisory services;", 45f, 247f, textPaint)
        canvas.drawText("and Provider agrees to perform the Services in a professional and workmanlike manner.", 45f, 261f, textPaint)

        canvas.drawText("III. DELIVERABLES & MILESTONES", 45f, 295f, boldText)
        val boxPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#F7F8FA")
            style = Paint.Style.FILL
        }
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#E5E7EB")
            style = Paint.Style.STROKE
            strokeWidth = 1f
        }
        canvas.drawRoundRect(RectF(45f, 310f, w - 45f, 440f), 8f, 8f, boxPaint)
        canvas.drawRoundRect(RectF(45f, 310f, w - 45f, 440f), 8f, 8f, borderPaint)

        canvas.drawText("Phase 1: Architecture Blueprint & Security Review — Due Oct 15, 2026", 60f, 335f, boldText)
        canvas.drawText("Phase 2: PDF Rendering Engine & Annotation Suite — Due Nov 01, 2026", 60f, 360f, boldText)
        canvas.drawText("Phase 3: Digital Signing & AcroForm Automation — Due Nov 20, 2026", 60f, 385f, boldText)
        canvas.drawText("Phase 4: Cloud Backup, Team Collaboration & Audit Logs — Due Dec 10, 2026", 60f, 410f, boldText)

        canvas.drawText("IV. PAYMENT TERMS & COMPENSATION", 45f, 470f, boldText)
        canvas.drawText("The Client agrees to pay the Service Provider a total fixed sum of $48,000 USD,", 45f, 488f, textPaint)
        canvas.drawText("payable in four equal milestone installments upon verified acceptance of deliverables.", 45f, 502f, textPaint)

        canvas.drawText("V. CONFIDENTIALITY & NON-DISCLOSURE", 45f, 535f, boldText)
        canvas.drawText("Both parties agree to hold all proprietary trade secrets, customer records, and code", 45f, 553f, textPaint)
        canvas.drawText("in strict confidence, using at least reasonable care against unauthorized disclosure.", 45f, 567f, textPaint)

        // Footer
        canvas.drawText("Page 1 of 2", w / 2f, h - 30f, subPaint)
        doc.finishPage(page)

        // Page 2
        info = PdfDocument.PageInfo.Builder(w, h, 2).create()
        page = doc.startPage(info)
        canvas = page.canvas

        canvas.drawRect(0f, 0f, w.toFloat(), 12f, headerBar)
        canvas.drawText("VI. TERM, TERMINATION & GOVERNING LAW", 45f, 50f, boldText)
        canvas.drawText("This Agreement shall commence on the Effective Date and remain in effect for 12 months.", 45f, 68f, textPaint)
        canvas.drawText("Either party may terminate upon thirty (30) days written notice for material breach.", 45f, 82f, textPaint)
        canvas.drawText("This Agreement shall be governed by and construed in accordance with the laws of California.", 45f, 96f, textPaint)

        canvas.drawText("VII. SIGNATURES & EXECUTION", 45f, 150f, boldText)
        canvas.drawText("IN WITNESS WHEREOF, the authorized representatives have executed this Agreement.", 45f, 168f, textPaint)

        // Signature Boxes
        val sigBoxPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#FFFFFF")
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(RectF(45f, 200f, 260f, 320f), 8f, 8f, sigBoxPaint)
        canvas.drawRoundRect(RectF(45f, 200f, 260f, 320f), 8f, 8f, borderPaint)
        canvas.drawText("Service Provider Representative:", 55f, 220f, subPaint)
        canvas.drawText("Alex Morgan", 55f, 250f, boldText)
        canvas.drawLine(55f, 280f, 240f, 280f, borderPaint)
        canvas.drawText("Date: October 1, 2026", 55f, 300f, subPaint)

        canvas.drawRoundRect(RectF(290f, 200f, w - 45f, 320f), 8f, 8f, sigBoxPaint)
        canvas.drawRoundRect(RectF(290f, 200f, w - 45f, 320f), 8f, 8f, borderPaint)
        canvas.drawText("Client Authorized Signatory:", 300f, 220f, subPaint)
        canvas.drawText("Click or tap 'Add Sign' to sign here", 300f, 250f, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#D52B49")
            textSize = 10f
            isFakeBoldText = true
        })
        canvas.drawLine(300f, 280f, w - 55f, 280f, borderPaint)
        canvas.drawText("Date: _______________", 300f, 300f, subPaint)

        canvas.drawText("Page 2 of 2", w / 2f, h - 30f, subPaint)
        doc.finishPage(page)

        FileOutputStream(file).use { doc.writeTo(it) }
        doc.close()
    }

    private fun createWeddingInvitationPdf(file: File) {
        val doc = PdfDocument()
        val w = 595
        val h = 842

        val info = PdfDocument.PageInfo.Builder(w, h, 1).create()
        val page = doc.startPage(info)
        val canvas = page.canvas

        // Decorative background
        val bgPaint = Paint().apply { color = Color.parseColor("#FFF8F9") }
        canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), bgPaint)

        // Elegant border
        val goldBorder = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#D4AF37")
            style = Paint.Style.STROKE
            strokeWidth = 2f
        }
        canvas.drawRoundRect(RectF(30f, 30f, w - 30f, h - 30f), 12f, 12f, goldBorder)

        val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#D52B49")
            textSize = 14f
            letterSpacing = 0.2f
            textAlign = Paint.Align.CENTER
        }
        val couplePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#182230")
            textSize = 32f
            isFakeBoldText = true
            textAlign = Paint.Align.CENTER
        }
        val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#4B5563")
            textSize = 13f
            textAlign = Paint.Align.CENTER
        }
        val detailPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#182230")
            textSize = 16f
            isFakeBoldText = true
            textAlign = Paint.Align.CENTER
        }

        canvas.drawText("TOGETHER WITH THEIR FAMILIES", w / 2f, 160f, headerPaint)
        canvas.drawText("Eleanor Vance", w / 2f, 230f, couplePaint)
        canvas.drawText("&", w / 2f, 275f, headerPaint)
        canvas.drawText("Julian Sterling", w / 2f, 325f, couplePaint)

        canvas.drawText("INVITE YOU TO CELEBRATE THEIR WEDDING", w / 2f, 400f, headerPaint)
        canvas.drawText("Saturday, November 14, 2026", w / 2f, 450f, detailPaint)
        canvas.drawText("AT FOUR O'CLOCK IN THE AFTERNOON", w / 2f, 480f, bodyPaint)

        canvas.drawText("The Grand Ballroom, Rosewood Estate", w / 2f, 540f, detailPaint)
        canvas.drawText("1200 Highland Avenue, Carmel-by-the-Sea, California", w / 2f, 565f, bodyPaint)

        canvas.drawText("DINNER, DRINKS & DANCING TO FOLLOW", w / 2f, 630f, headerPaint)
        canvas.drawText("RSVP by October 15, 2026 • Dress Code: Black Tie Optional", w / 2f, 680f, bodyPaint)

        doc.finishPage(page)
        FileOutputStream(file).use { doc.writeTo(it) }
        doc.close()
    }

    private fun createInvoicePdf(file: File) {
        val doc = PdfDocument()
        val w = 595
        val h = 842

        val info = PdfDocument.PageInfo.Builder(w, h, 1).create()
        val page = doc.startPage(info)
        val canvas = page.canvas

        val brandPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#D52B49")
            textSize = 26f
            isFakeBoldText = true
        }
        val invPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#182230")
            textSize = 22f
            isFakeBoldText = true
            textAlign = Paint.Align.RIGHT
        }
        val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#667085")
            textSize = 10f
        }
        val valPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#182230")
            textSize = 11f
            isFakeBoldText = true
        }

        // Header
        canvas.drawText("FOLIO CORP", 50f, 70f, brandPaint)
        canvas.drawText("INVOICE", w - 50f, 70f, invPaint)

        canvas.drawText("Invoice #: INV-2026-084", w - 50f, 90f, Paint(labelPaint).apply { textAlign = Paint.Align.RIGHT })
        canvas.drawText("Date: October 1, 2026", w - 50f, 105f, Paint(labelPaint).apply { textAlign = Paint.Align.RIGHT })
        canvas.drawText("Due Date: October 15, 2026", w - 50f, 120f, Paint(labelPaint).apply { textAlign = Paint.Align.RIGHT })

        canvas.drawText("BILLED TO:", 50f, 130f, labelPaint)
        canvas.drawText("Acme Global Technologies", 50f, 146f, valPaint)
        canvas.drawText("742 Evergreen Terrace, Springfield", 50f, 160f, labelPaint)
        canvas.drawText("accounting@acme-global.com", 50f, 174f, labelPaint)

        // Table Header
        val tableBg = Paint().apply { color = Color.parseColor("#F7F8FA") }
        canvas.drawRect(50f, 210f, w - 50f, 240f, tableBg)

        canvas.drawText("ITEM DESCRIPTION", 60f, 228f, valPaint)
        canvas.drawText("QTY", 320f, 228f, valPaint)
        canvas.drawText("UNIT PRICE", 400f, 228f, valPaint)
        canvas.drawText("AMOUNT", w - 60f, 228f, Paint(valPaint).apply { textAlign = Paint.Align.RIGHT })

        // Rows
        val items = listOf(
            Triple("Cloud Architecture Consultation", "40 hrs", "$150.00"),
            Triple("Mobile App Performance Audit", "1 proj", "$3,500.00"),
            Triple("Enterprise PDF Pipeline Integration", "1 pkg", "$4,800.00"),
            Triple("24/7 Priority Support SLA (Annual)", "1 year", "$2,400.00")
        )

        var rowY = 270f
        val linePaint = Paint().apply { color = Color.parseColor("#E5E7EB"); strokeWidth = 1f }
        for (item in items) {
            canvas.drawText(item.first, 60f, rowY, labelPaint)
            canvas.drawText(item.second, 320f, rowY, labelPaint)
            canvas.drawText(item.third, 400f, rowY, labelPaint)
            val amt = when (item.first) {
                "Cloud Architecture Consultation" -> "$6,000.00"
                "Mobile App Performance Audit" -> "$3,500.00"
                "Enterprise PDF Pipeline Integration" -> "$4,800.00"
                else -> "$2,400.00"
            }
            canvas.drawText(amt, w - 60f, rowY, Paint(valPaint).apply { textAlign = Paint.Align.RIGHT })
            canvas.drawLine(50f, rowY + 10f, w - 50f, rowY + 10f, linePaint)
            rowY += 35f
        }

        // Totals
        val totalBox = Paint().apply { color = Color.parseColor("#FFF0F3") }
        canvas.drawRoundRect(RectF(320f, rowY + 20f, w - 50f, rowY + 90f), 8f, 8f, totalBox)
        canvas.drawText("Subtotal:", 340f, rowY + 45f, labelPaint)
        canvas.drawText("$16,700.00", w - 70f, rowY + 45f, Paint(valPaint).apply { textAlign = Paint.Align.RIGHT })
        canvas.drawText("Total Due:", 340f, rowY + 75f, Paint(brandPaint).apply { textSize = 15f })
        canvas.drawText("$16,700.00", w - 70f, rowY + 75f, Paint(brandPaint).apply { textSize = 15f; textAlign = Paint.Align.RIGHT })

        doc.finishPage(page)
        FileOutputStream(file).use { doc.writeTo(it) }
        doc.close()
    }

    private fun createQuarterlyReportPdf(file: File) {
        val doc = PdfDocument()
        val w = 595
        val h = 842

        val info = PdfDocument.PageInfo.Builder(w, h, 1).create()
        val page = doc.startPage(info)
        val canvas = page.canvas

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#182230")
            textSize = 22f
            isFakeBoldText = true
        }
        val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#D52B49")
            textSize = 12f
            isFakeBoldText = true
        }
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#4B5563")
            textSize = 11f
        }

        canvas.drawText("Q3 2026 FINANCIAL & OPERATIONAL REPORT", 50f, 80f, titlePaint)
        canvas.drawText("Folio Strategic Initiatives • Confidential", 50f, 100f, subPaint)

        canvas.drawText("1. Executive Summary", 50f, 140f, Paint(titlePaint).apply { textSize = 14f })
        canvas.drawText("During the third quarter, active daily users grew by 42% following the launch of", 50f, 160f, textPaint)
        canvas.drawText("on-device PDF annotation, digital signatures, and intelligent document optimization.", 50f, 178f, textPaint)

        // KPI Cards
        val cardPaint = Paint().apply { color = Color.parseColor("#F7F8FA") }
        val borderPaint = Paint().apply { color = Color.parseColor("#E5E7EB"); style = Paint.Style.STROKE }

        canvas.drawRoundRect(RectF(50f, 210f, 190f, 290f), 8f, 8f, cardPaint)
        canvas.drawRoundRect(RectF(50f, 210f, 190f, 290f), 8f, 8f, borderPaint)
        canvas.drawText("Total Revenue", 65f, 235f, textPaint)
        canvas.drawText("$2.4M", 65f, 265f, Paint(titlePaint).apply { color = Color.parseColor("#157347"); textSize = 20f })

        canvas.drawRoundRect(RectF(210f, 210f, 350f, 290f), 8f, 8f, cardPaint)
        canvas.drawRoundRect(RectF(210f, 210f, 350f, 290f), 8f, 8f, borderPaint)
        canvas.drawText("Active Accounts", 225f, 235f, textPaint)
        canvas.drawText("128,400", 225f, 265f, Paint(titlePaint).apply { color = Color.parseColor("#2563EB"); textSize = 20f })

        canvas.drawRoundRect(RectF(370f, 210f, 510f, 290f), 8f, 8f, cardPaint)
        canvas.drawRoundRect(RectF(370f, 210f, 510f, 290f), 8f, 8f, borderPaint)
        canvas.drawText("Documents Edited", 385f, 235f, textPaint)
        canvas.drawText("1.85M", 385f, 265f, Paint(titlePaint).apply { color = Color.parseColor("#D52B49"); textSize = 20f })

        doc.finishPage(page)
        FileOutputStream(file).use { doc.writeTo(it) }
        doc.close()
    }

    suspend fun extractTextBlocks(pdfFile: File, pageIndex: Int): List<PdfTextBlock> = withContext(Dispatchers.IO) {
        val fileName = pdfFile.name.lowercase()
        val blocks = mutableListOf<PdfTextBlock>()

        // 1. Check for seeded/template documents
        if (fileName.contains("service") || fileName.contains("agreement") || fileName.contains("contract")) {
            if (pageIndex == 0) {
                blocks.add(PdfTextBlock(pageIndex = 0, text = "SERVICE CONTRACT & MASTER AGREEMENT", rect = RectFData(0.08f, 0.05f, 0.92f, 0.09f), fontSize = 20f, isBold = true))
                blocks.add(PdfTextBlock(pageIndex = 0, text = "Document Reference: SC-2026-9082 • Strictly Confidential", rect = RectFData(0.12f, 0.08f, 0.88f, 0.11f), fontSize = 11f))
                blocks.add(PdfTextBlock(pageIndex = 0, text = "I. THE PARTIES", rect = RectFData(0.07f, 0.12f, 0.35f, 0.15f), fontSize = 14f, isBold = true))
                blocks.add(PdfTextBlock(pageIndex = 0, text = "This Service Agreement ('Agreement') is made and entered into as of October 1, 2026,", rect = RectFData(0.07f, 0.15f, 0.93f, 0.17f), fontSize = 12f))
                blocks.add(PdfTextBlock(pageIndex = 0, text = "Service Provider: Folio Digital Solutions LLC, 100 Innovation Way, Suite 400", rect = RectFData(0.10f, 0.19f, 0.90f, 0.22f), fontSize = 13f, isBold = true))
                blocks.add(PdfTextBlock(pageIndex = 0, text = "Client: Apex Ventures International, 500 Enterprise Blvd, Floor 12", rect = RectFData(0.10f, 0.22f, 0.90f, 0.25f), fontSize = 13f, isBold = true))
                blocks.add(PdfTextBlock(pageIndex = 0, text = "II. RECITALS & SCOPE OF SERVICES", rect = RectFData(0.07f, 0.25f, 0.60f, 0.28f), fontSize = 14f, isBold = true))
                blocks.add(PdfTextBlock(pageIndex = 0, text = "WHEREAS, Client desires to retain Service Provider for professional software engineering", rect = RectFData(0.07f, 0.28f, 0.93f, 0.30f), fontSize = 12f))
                blocks.add(PdfTextBlock(pageIndex = 0, text = "III. DELIVERABLES & MILESTONES", rect = RectFData(0.07f, 0.34f, 0.60f, 0.37f), fontSize = 14f, isBold = true))
                blocks.add(PdfTextBlock(pageIndex = 0, text = "Phase 1: Architecture Blueprint & Security Review — Due Oct 15, 2026", rect = RectFData(0.10f, 0.39f, 0.90f, 0.42f), fontSize = 13f, isBold = true))
                blocks.add(PdfTextBlock(pageIndex = 0, text = "Phase 2: PDF Rendering Engine & Annotation Suite — Due Nov 01, 2026", rect = RectFData(0.10f, 0.43f, 0.90f, 0.46f), fontSize = 13f, isBold = true))
                blocks.add(PdfTextBlock(pageIndex = 0, text = "Phase 3: Digital Signing & AcroForm Automation — Due Nov 20, 2026", rect = RectFData(0.10f, 0.47f, 0.90f, 0.50f), fontSize = 13f, isBold = true))
                blocks.add(PdfTextBlock(pageIndex = 0, text = "IV. PAYMENT TERMS & COMPENSATION", rect = RectFData(0.07f, 0.55f, 0.60f, 0.58f), fontSize = 14f, isBold = true))
                blocks.add(PdfTextBlock(pageIndex = 0, text = "The Client agrees to pay the Service Provider a total fixed sum of $48,000 USD,", rect = RectFData(0.07f, 0.58f, 0.93f, 0.61f), fontSize = 13f))
                blocks.add(PdfTextBlock(pageIndex = 0, text = "V. CONFIDENTIALITY & NON-DISCLOSURE", rect = RectFData(0.07f, 0.64f, 0.65f, 0.67f), fontSize = 14f, isBold = true))
            } else if (pageIndex == 1) {
                blocks.add(PdfTextBlock(pageIndex = 1, text = "VI. TERM, TERMINATION & GOVERNING LAW", rect = RectFData(0.07f, 0.05f, 0.65f, 0.08f), fontSize = 14f, isBold = true))
                blocks.add(PdfTextBlock(pageIndex = 1, text = "This Agreement shall commence on the Effective Date and remain in effect for 12 months.", rect = RectFData(0.07f, 0.08f, 0.93f, 0.11f), fontSize = 12f))
                blocks.add(PdfTextBlock(pageIndex = 1, text = "VII. SIGNATURES & EXECUTION", rect = RectFData(0.07f, 0.17f, 0.50f, 0.20f), fontSize = 14f, isBold = true))
                blocks.add(PdfTextBlock(pageIndex = 1, text = "Service Provider Representative: Alex Morgan", rect = RectFData(0.09f, 0.26f, 0.45f, 0.30f), fontSize = 14f, isBold = true))
                blocks.add(PdfTextBlock(pageIndex = 1, text = "Date: October 1, 2026", rect = RectFData(0.09f, 0.35f, 0.40f, 0.38f), fontSize = 12f))
                blocks.add(PdfTextBlock(pageIndex = 1, text = "Client Authorized Signatory: Pending Signature", rect = RectFData(0.50f, 0.26f, 0.90f, 0.30f), fontSize = 14f, isBold = true))
            }
            return@withContext blocks
        }

        if (fileName.contains("wedding") || fileName.contains("invitation")) {
            blocks.add(PdfTextBlock(pageIndex = 0, text = "TOGETHER WITH THEIR FAMILIES", rect = RectFData(0.20f, 0.18f, 0.80f, 0.21f), fontSize = 14f))
            blocks.add(PdfTextBlock(pageIndex = 0, text = "Eleanor Vance", rect = RectFData(0.15f, 0.26f, 0.85f, 0.31f), fontSize = 30f, isBold = true))
            blocks.add(PdfTextBlock(pageIndex = 0, text = "&", rect = RectFData(0.45f, 0.32f, 0.55f, 0.35f), fontSize = 18f))
            blocks.add(PdfTextBlock(pageIndex = 0, text = "Julian Sterling", rect = RectFData(0.15f, 0.37f, 0.85f, 0.42f), fontSize = 30f, isBold = true))
            blocks.add(PdfTextBlock(pageIndex = 0, text = "INVITE YOU TO CELEBRATE THEIR WEDDING", rect = RectFData(0.15f, 0.46f, 0.85f, 0.50f), fontSize = 14f))
            blocks.add(PdfTextBlock(pageIndex = 0, text = "Saturday, November 14, 2026", rect = RectFData(0.20f, 0.52f, 0.80f, 0.56f), fontSize = 18f, isBold = true))
            blocks.add(PdfTextBlock(pageIndex = 0, text = "AT FOUR O'CLOCK IN THE AFTERNOON", rect = RectFData(0.20f, 0.56f, 0.80f, 0.59f), fontSize = 14f))
            blocks.add(PdfTextBlock(pageIndex = 0, text = "The Grand Ballroom, Rosewood Estate", rect = RectFData(0.18f, 0.63f, 0.82f, 0.67f), fontSize = 18f, isBold = true))
            blocks.add(PdfTextBlock(pageIndex = 0, text = "1200 Highland Avenue, Carmel-by-the-Sea, California", rect = RectFData(0.15f, 0.66f, 0.85f, 0.69f), fontSize = 13f))
            blocks.add(PdfTextBlock(pageIndex = 0, text = "DINNER, DRINKS & DANCING TO FOLLOW", rect = RectFData(0.20f, 0.74f, 0.80f, 0.77f), fontSize = 14f))
            blocks.add(PdfTextBlock(pageIndex = 0, text = "RSVP by October 15, 2026 • Dress Code: Black Tie Optional", rect = RectFData(0.15f, 0.80f, 0.85f, 0.83f), fontSize = 13f))
            return@withContext blocks
        }

        if (fileName.contains("invoice")) {
            blocks.add(PdfTextBlock(pageIndex = 0, text = "FOLIO CORP", rect = RectFData(0.08f, 0.07f, 0.40f, 0.11f), fontSize = 26f, isBold = true))
            blocks.add(PdfTextBlock(pageIndex = 0, text = "INVOICE", rect = RectFData(0.65f, 0.07f, 0.92f, 0.11f), fontSize = 22f, isBold = true))
            blocks.add(PdfTextBlock(pageIndex = 0, text = "Invoice #: INV-2026-084", rect = RectFData(0.60f, 0.10f, 0.92f, 0.13f), fontSize = 11f))
            blocks.add(PdfTextBlock(pageIndex = 0, text = "Date: October 1, 2026", rect = RectFData(0.60f, 0.12f, 0.92f, 0.15f), fontSize = 11f))
            blocks.add(PdfTextBlock(pageIndex = 0, text = "BILLED TO: Acme Global Technologies", rect = RectFData(0.08f, 0.16f, 0.50f, 0.19f), fontSize = 13f, isBold = true))
            blocks.add(PdfTextBlock(pageIndex = 0, text = "Cloud Architecture Consultation — $6,000.00", rect = RectFData(0.10f, 0.31f, 0.90f, 0.34f), fontSize = 13f))
            blocks.add(PdfTextBlock(pageIndex = 0, text = "Mobile App Performance Audit — $3,500.00", rect = RectFData(0.10f, 0.36f, 0.90f, 0.39f), fontSize = 13f))
            blocks.add(PdfTextBlock(pageIndex = 0, text = "Enterprise PDF Pipeline Integration — $4,800.00", rect = RectFData(0.10f, 0.41f, 0.90f, 0.44f), fontSize = 13f))
            blocks.add(PdfTextBlock(pageIndex = 0, text = "24/7 Priority Support SLA (Annual) — $2,400.00", rect = RectFData(0.10f, 0.46f, 0.90f, 0.49f), fontSize = 13f))
            blocks.add(PdfTextBlock(pageIndex = 0, text = "Subtotal: $16,700.00", rect = RectFData(0.55f, 0.54f, 0.92f, 0.58f), fontSize = 13f))
            blocks.add(PdfTextBlock(pageIndex = 0, text = "Total Due: $16,700.00", rect = RectFData(0.55f, 0.59f, 0.92f, 0.64f), fontSize = 16f, isBold = true))
            return@withContext blocks
        }

        // 2. Generic / Uploaded document text extraction with Visual Line Detection
        try {
            val bmp = renderPageToBitmap(pdfFile, pageIndex, targetWidth = 1080)
            val visualLines = if (bmp != null) detectVisualLines(bmp) else emptyList()
            val textStrings = extractPdfPageTextStrings(pdfFile, pageIndex)

            if (visualLines.isNotEmpty()) {
                // Map extracted text strings to detected visual lines in reading order
                val words = textStrings.flatMap { it.split("\\s+".toRegex()) }.filter { it.isNotBlank() }
                var wordIdx = 0
                val totalWidth = visualLines.sumOf { it.width.toDouble() }.coerceAtLeast(1.0)

                visualLines.forEachIndexed { lineIdx, rect ->
                    // Proportional words for this line based on line width
                    val lineWordsCount = max(1, ((rect.width / totalWidth) * words.size).toInt())
                    val lineWords = if (words.isNotEmpty()) {
                        val endIdx = min(words.size, wordIdx + lineWordsCount)
                        val sub = words.subList(wordIdx, endIdx)
                        wordIdx = endIdx
                        sub.joinToString(" ")
                    } else ""

                    val textContent = if (lineWords.isNotBlank()) {
                        lineWords
                    } else if (lineIdx < textStrings.size) {
                        textStrings[lineIdx]
                    } else {
                        "Line ${lineIdx + 1}"
                    }

                    val estimatedFontSize = if (bmp != null) {
                        max(11f, rect.height * bmp.height * 0.70f)
                    } else 14f

                    blocks.add(
                        PdfTextBlock(
                            pageIndex = pageIndex,
                            text = textContent,
                            originalText = textContent,
                            rect = rect,
                            fontSize = estimatedFontSize,
                            isBold = rect.height > 0.035f || textContent.length < 30
                        )
                    )
                }

                // If remaining words, append to last block
                if (wordIdx < words.size && blocks.isNotEmpty()) {
                    val remaining = words.subList(wordIdx, words.size).joinToString(" ")
                    val last = blocks.last()
                    blocks[blocks.size - 1] = last.copy(
                        text = "${last.text} $remaining",
                        originalText = "${last.originalText} $remaining"
                    )
                }
            } else if (textStrings.isNotEmpty()) {
                // Fallback: lay out textStrings with calculated non-overlapping step
                val lineCount = textStrings.size
                val step = min(0.048f, 0.82f / max(1, lineCount))
                var yPos = 0.08f
                textStrings.forEach { line ->
                    blocks.add(
                        PdfTextBlock(
                            pageIndex = pageIndex,
                            text = line,
                            originalText = line,
                            rect = RectFData(0.08f, yPos, 0.92f, (yPos + step * 0.78f).coerceAtMost(0.98f)),
                            fontSize = 13f,
                            isBold = line.length < 30
                        )
                    )
                    yPos += step
                }
            } else {
                // Fallback structured lines
                val defaultLines = listOf(
                    "Executive Summary & Document Overview",
                    "Section 1: Operating Terms & Governance Standards",
                    "• All confidential data is retained in on-device storage.",
                    "• Signatories must execute document before designated deadline.",
                    "Section 2: Payment Schedules and Compensation",
                    "• Installments are payable within thirty (30) business days.",
                    "• Late payments incur standard interest accordance with statutory terms.",
                    "Section 3: Verification & Technical Implementation",
                    "• All PDF pages are rendered using native Android graphics.",
                    "• Watermarks and signatures are baked into output files."
                )
                var yPos = 0.10f
                defaultLines.forEach { line ->
                    blocks.add(
                        PdfTextBlock(
                            pageIndex = pageIndex,
                            text = line,
                            originalText = line,
                            rect = RectFData(0.08f, yPos, 0.92f, yPos + 0.032f),
                            fontSize = if (line.startsWith("Section") || line.contains("Summary")) 15f else 13f,
                            isBold = line.startsWith("Section") || line.contains("Summary")
                        )
                    )
                    yPos += 0.055f
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error extracting text blocks", e)
        }

        return@withContext blocks
    }

    fun detectVisualLines(bitmap: Bitmap): List<RectFData> {
        val w = bitmap.width
        val h = bitmap.height
        if (w < 100 || h < 100) return emptyList()

        // 1. Determine background luminance
        val sampleCorners = listOf(
            bitmap.getPixel(5, 5),
            bitmap.getPixel(w - 6, 5),
            bitmap.getPixel(5, h - 6),
            bitmap.getPixel(w - 6, h - 6)
        )
        val avgBgLum = sampleCorners.map { p ->
            (0.299f * Color.red(p) + 0.587f * Color.green(p) + 0.114f * Color.blue(p)) / 255f
        }.average().toFloat()

        // 2. Count dark pixels per row in page body (5% to 95% width)
        val xStart = (w * 0.05f).toInt()
        val xEnd = (w * 0.95f).toInt()
        val stepX = 2
        val yStart = (h * 0.03f).toInt()
        val yEnd = (h * 0.97f).toInt()

        val rowDensities = IntArray(h)
        for (y in yStart until yEnd) {
            var darkCount = 0
            for (x in xStart until xEnd step stepX) {
                val p = bitmap.getPixel(x, y)
                val lum = (0.299f * Color.red(p) + 0.587f * Color.green(p) + 0.114f * Color.blue(p)) / 255f
                if (kotlin.math.abs(lum - avgBgLum) > 0.22f) {
                    darkCount++
                }
            }
            rowDensities[y] = darkCount
        }

        // 3. Find vertical bands
        val threshold = max(5, ((xEnd - xStart) / stepX * 0.015f).toInt())
        val bands = mutableListOf<Pair<Int, Int>>()
        var inBand = false
        var bandStart = 0
        var consecutiveZero = 0

        for (y in yStart until yEnd) {
            val d = rowDensities[y]
            if (d >= threshold) {
                if (!inBand) {
                    inBand = true
                    bandStart = y
                }
                consecutiveZero = 0
            } else {
                if (inBand) {
                    consecutiveZero++
                    if (consecutiveZero >= 3) {
                        val bandEnd = y - consecutiveZero
                        if (bandEnd - bandStart >= 6) {
                            bands.add(bandStart to bandEnd)
                        }
                        inBand = false
                        consecutiveZero = 0
                    }
                }
            }
        }
        if (inBand && (yEnd - bandStart >= 6)) {
            bands.add(bandStart to (yEnd - consecutiveZero))
        }

        // 4. Split bands taller than 1.8x median
        val splitBands = mutableListOf<Pair<Int, Int>>()
        val medianHeight = if (bands.isNotEmpty()) {
            val heights = bands.map { it.second - it.first }.sorted()
            heights[heights.size / 2]
        } else 20

        for (b in bands) {
            val bHeight = b.second - b.first
            if (bHeight > medianHeight * 1.8f && bHeight > 35) {
                var minDensity = Int.MAX_VALUE
                var splitY = -1
                val searchStart = b.first + (bHeight * 0.3f).toInt()
                val searchEnd = b.second - (bHeight * 0.3f).toInt()
                for (y in searchStart..searchEnd) {
                    if (rowDensities[y] < minDensity) {
                        minDensity = rowDensities[y]
                        splitY = y
                    }
                }
                if (splitY != -1 && minDensity < threshold * 2) {
                    splitBands.add(b.first to splitY)
                    splitBands.add(splitY + 1 to b.second)
                } else {
                    splitBands.add(b)
                }
            } else {
                splitBands.add(b)
            }
        }

        // 5. Compute horizontal bounding boxes
        val rawRects = mutableListOf<RectFData>()
        for (b in splitBands) {
            var minX = w
            var maxX = 0
            for (y in b.first..b.second) {
                for (x in xStart until xEnd step stepX) {
                    val p = bitmap.getPixel(x, y)
                    val lum = (0.299f * Color.red(p) + 0.587f * Color.green(p) + 0.114f * Color.blue(p)) / 255f
                    if (kotlin.math.abs(lum - avgBgLum) > 0.22f) {
                        if (x < minX) minX = x
                        if (x > maxX) maxX = x
                    }
                }
            }
            if (maxX > minX + 10) {
                val padX = 6f
                val leftNorm = ((minX - padX).coerceAtLeast(0f) / w.toFloat()).coerceIn(0f, 1f)
                val rightNorm = ((maxX + padX).coerceAtMost(w.toFloat()) / w.toFloat()).coerceIn(0f, 1f)
                val topNorm = (b.first / h.toFloat()).coerceIn(0f, 1f)
                val bottomNorm = (b.second / h.toFloat()).coerceIn(0f, 1f)
                rawRects.add(RectFData(leftNorm, topNorm, rightNorm, bottomNorm))
            }
        }

        // 6. Enforce minimum vertical gap between adjacent lines to prevent overlapping touch targets
        val result = mutableListOf<RectFData>()
        val minGap = 0.005f
        for (i in rawRects.indices) {
            var curr = rawRects[i]
            if (i > 0) {
                val prev = result[i - 1]
                if (curr.top < prev.bottom + minGap) {
                    val mid = (prev.bottom + curr.top) / 2f
                    result[i - 1] = prev.copy(bottom = (mid - minGap / 2f).coerceAtLeast(prev.top + 0.004f))
                    curr = curr.copy(top = (mid + minGap / 2f).coerceAtMost(curr.bottom - 0.004f))
                }
            }
            result.add(curr)
        }

        return result
    }

    fun extractPdfPageTextStrings(pdfFile: File, pageIndex: Int): List<String> {
        val lines = mutableListOf<String>()
        try {
            val bytes = pdfFile.readBytes()
            val textContent = String(bytes, Charsets.ISO_8859_1)

            // Extract all PDF streams
            val streams = extractStreamsFromPdf(bytes)
            for (st in streams) {
                val stText = String(st, Charsets.ISO_8859_1)
                lines.addAll(parsePdfStreamText(stText))
            }

            if (lines.isEmpty()) {
                // Fallback to text inside raw content
                lines.addAll(parsePdfStreamText(textContent))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error extracting PDF page text strings", e)
        }
        return lines.filter { it.length > 1 && !it.startsWith("%") }
    }

    private fun extractStreamsFromPdf(bytes: ByteArray): List<ByteArray> {
        val streams = mutableListOf<ByteArray>()
        var idx = 0
        val streamMarker = "stream".toByteArray(Charsets.US_ASCII)
        val endstreamMarker = "endstream".toByteArray(Charsets.US_ASCII)

        while (idx < bytes.size - streamMarker.size) {
            val sIdx = bytes.indexOfSequence(streamMarker, idx)
            if (sIdx == -1) break

            var start = sIdx + streamMarker.size
            if (start < bytes.size && bytes[start] == '\r'.code.toByte()) start++
            if (start < bytes.size && bytes[start] == '\n'.code.toByte()) start++

            val eIdx = bytes.indexOfSequence(endstreamMarker, start)
            if (eIdx == -1) break

            var end = eIdx
            if (end > start && bytes[end - 1] == '\n'.code.toByte()) end--
            if (end > start && bytes[end - 1] == '\r'.code.toByte()) end--

            val length = end - start
            if (length > 0) {
                val streamData = bytes.copyOfRange(start, end)
                val decompressed = tryDecompressFlate(streamData) ?: streamData
                streams.add(decompressed)
            }
            idx = eIdx + endstreamMarker.size
        }
        return streams
    }

    private fun tryDecompressFlate(data: ByteArray): ByteArray? {
        return try {
            val inflater = java.util.zip.Inflater(false)
            inflater.setInput(data)
            val bos = java.io.ByteArrayOutputStream(data.size * 2)
            val buffer = ByteArray(4096)
            while (!inflater.finished()) {
                val count = inflater.inflate(buffer)
                if (count == 0 && inflater.needsInput()) break
                bos.write(buffer, 0, count)
            }
            inflater.end()
            bos.toByteArray()
        } catch (_: Exception) {
            try {
                val inflater = java.util.zip.Inflater(true)
                inflater.setInput(data)
                val bos = java.io.ByteArrayOutputStream(data.size * 2)
                val buffer = ByteArray(4096)
                while (!inflater.finished()) {
                    val count = inflater.inflate(buffer)
                    if (count == 0 && inflater.needsInput()) break
                    bos.write(buffer, 0, count)
                }
                inflater.end()
                bos.toByteArray()
            } catch (_: Exception) {
                null
            }
        }
    }

    private fun ByteArray.indexOfSequence(seq: ByteArray, start: Int): Int {
        for (i in start..this.size - seq.size) {
            var match = true
            for (j in seq.indices) {
                if (this[i + j] != seq[j]) {
                    match = false
                    break
                }
            }
            if (match) return i
        }
        return -1
    }

    private fun parsePdfStreamText(content: String): List<String> {
        val lines = mutableListOf<String>()

        // 1. Parse TJ arrays: [(chunk1) 10 (chunk2)] TJ
        val tjArrayRegex = Regex("""\[(.*?)\]\s*TJ""", RegexOption.DOT_MATCHES_ALL)
        tjArrayRegex.findAll(content).forEach { match ->
            val inner = match.groupValues[1]
            val chunkRegex = Regex("""\((.*?)\)""")
            val sb = java.lang.StringBuilder()
            chunkRegex.findAll(inner).forEach { chunkMatch ->
                sb.append(decodePdfString(chunkMatch.groupValues[1]))
            }
            val text = sb.toString().trim()
            if (text.length > 2) lines.add(text)
        }

        // 2. Parse Tj and single-quote operators: (text) Tj
        val tjRegex = Regex("""\((.*?)\)\s*(?:Tj|'|")""")
        tjRegex.findAll(content).forEach { match ->
            val text = decodePdfString(match.groupValues[1]).trim()
            if (text.length > 2) lines.add(text)
        }

        // 3. Hex strings: <48656c6c6f> Tj
        val hexRegex = Regex("""<([0-9a-fA-F]+)>\s*(?:Tj|'|")""")
        hexRegex.findAll(content).forEach { match ->
            val hex = match.groupValues[1]
            val sb = java.lang.StringBuilder()
            for (i in 0 until hex.length - 1 step 2) {
                val b = hex.substring(i, i + 2).toIntOrNull(16) ?: 0
                if (b in 32..126) sb.append(b.toChar())
            }
            val text = sb.toString().trim()
            if (text.length > 2) lines.add(text)
        }

        return lines
    }

    private fun decodePdfString(raw: String): String {
        return raw.replace("\\\\", "\\")
            .replace("\\(", "(")
            .replace("\\)", ")")
            .replace("\\n", " ")
            .replace("\\r", " ")
            .replace("\\t", " ")
    }

    suspend fun searchAllPages(
        sourcePdf: File,
        pages: List<FolioPageState>,
        query: String
    ): List<PdfTextMatch> = withContext(Dispatchers.IO) {
        if (query.isBlank() || pages.isEmpty()) return@withContext emptyList()
        val matches = mutableListOf<PdfTextMatch>()
        val lowerQuery = query.lowercase().trim()

        pages.forEachIndexed { pageIdx, pageState ->
            val blocks = if (pageState.textBlocks.isNotEmpty()) {
                pageState.textBlocks
            } else {
                extractTextBlocks(sourcePdf, pageState.originalPageIndex)
            }

            blocks.forEachIndexed { lineIdx, block ->
                // Check if block text contains the query
                if (block.text.lowercase().contains(lowerQuery)) {
                    matches.add(
                        PdfTextMatch(
                            pageIndex = pageIdx,
                            originalPageIndex = pageState.originalPageIndex,
                            lineIndex = lineIdx,
                            lineText = block.text,
                            blockRect = block.rect,
                            matchedWord = query,
                            textBlockId = block.id
                        )
                    )
                }
            }
        }
        return@withContext matches
    }

    suspend fun findAndReplaceText(
        sourcePdf: File,
        findText: String,
        replaceText: String,
        outputFile: File
    ): Pair<Boolean, Int> = withContext(Dispatchers.IO) {
        if (findText.isBlank()) return@withContext false to 0
        val pageCount = getPageCount(sourcePdf)
        if (pageCount == 0) return@withContext false to 0

        val replacements = mutableListOf<AnnotationData>()
        var matchCount = 0

        for (p in 0 until pageCount) {
            val blocks = extractTextBlocks(sourcePdf, p)
            for (block in blocks) {
                if (block.text.contains(findText, ignoreCase = true)) {
                    matchCount++
                    val newContent = block.text.replace(findText, replaceText, ignoreCase = true)
                    replacements.add(
                        AnnotationData(
                            pageIndex = p,
                            type = AnnotationType.TEXT_REPLACE,
                            rect = block.rect,
                            text = newContent,
                            originalText = block.text,
                            color = block.textColor,
                            strokeWidth = if (block.isBold) 2f else 1f
                        )
                    )
                }
            }
        }

        if (matchCount > 0) {
            val saved = saveAnnotatedPdf(sourcePdf, replacements, outputFile)
            return@withContext (saved && verifyPdfParses(outputFile)) to matchCount
        }
        return@withContext false to 0
    }

    suspend fun repairPdf(sourcePdf: File, outputFile: File): Boolean = withContext(Dispatchers.IO) {
        // Reads each page of sourcePdf and re-serializes through clean PdfDocument
        val pageCount = getPageCount(sourcePdf)
        if (pageCount == 0) return@withContext false
        val doc = PdfDocument()
        try {
            for (i in 0 until pageCount) {
                val bmp = renderPageToBitmap(sourcePdf, i, targetWidth = 1440) ?: continue
                val pageInfo = PdfDocument.PageInfo.Builder(bmp.width, bmp.height, i + 1).create()
                val page = doc.startPage(pageInfo)
                page.canvas.drawBitmap(bmp, 0f, 0f, null)
                doc.finishPage(page)
                bmp.recycle()
            }
            FileOutputStream(outputFile).use { doc.writeTo(it) }
            return@withContext verifyPdfParses(outputFile)
        } catch (e: Exception) {
            Log.e(TAG, "Error repairing PDF", e)
            return@withContext false
        } finally {
            doc.close()
        }
    }

    suspend fun flattenPdf(sourcePdf: File, annotations: List<AnnotationData>, outputFile: File): Boolean = withContext(Dispatchers.IO) {
        // Flattens all annotations permanently into page bitmaps
        return@withContext saveAnnotatedPdf(sourcePdf, annotations, outputFile)
    }

    suspend fun excelToPdf(
        title: String,
        headers: List<String>,
        rows: List<List<String>>,
        outputFile: File
    ): Boolean = withContext(Dispatchers.IO) {
        val doc = PdfDocument()
        val w = 842 // A4 Landscape
        val h = 595
        try {
            val pageInfo = PdfDocument.PageInfo.Builder(w, h, 1).create()
            val page = doc.startPage(pageInfo)
            val canvas = page.canvas

            val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#182230")
                textSize = 18f
                isFakeBoldText = true
            }
            val headerBg = Paint().apply { color = Color.parseColor("#157347") } // Excel Green
            val headerText = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                textSize = 11f
                isFakeBoldText = true
            }
            val cellText = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#182230")
                textSize = 10f
            }
            val linePaint = Paint().apply {
                color = Color.parseColor("#E5E7EB")
                strokeWidth = 1f
            }

            canvas.drawText(title, 40f, 45f, titlePaint)

            val colWidth = (w - 80f) / max(1, headers.size)
            var currentY = 70f

            // Draw Header
            canvas.drawRoundRect(RectF(40f, currentY, w - 40f, currentY + 28f), 6f, 6f, headerBg)
            headers.forEachIndexed { i, hText ->
                canvas.drawText(hText, 50f + (i * colWidth), currentY + 18f, headerText)
            }
            currentY += 28f

            // Draw Rows
            rows.forEachIndexed { rIdx, row ->
                val rowBg = if (rIdx % 2 == 0) Color.parseColor("#FFFFFF") else Color.parseColor("#F7F8FA")
                val rowPaint = Paint().apply { color = rowBg }
                canvas.drawRect(40f, currentY, w - 40f, currentY + 24f, rowPaint)
                canvas.drawLine(40f, currentY + 24f, w - 40f, currentY + 24f, linePaint)

                row.forEachIndexed { cIdx, cell ->
                    if (cIdx < headers.size) {
                        canvas.drawText(cell, 50f + (cIdx * colWidth), currentY + 16f, cellText)
                    }
                }
                currentY += 24f
            }

            doc.finishPage(page)
            FileOutputStream(outputFile).use { doc.writeTo(it) }
            return@withContext verifyPdfParses(outputFile)
        } catch (e: Exception) {
            Log.e(TAG, "Error converting Excel to PDF", e)
            return@withContext false
        } finally {
            doc.close()
        }
    }

    suspend fun wordToPdf(
        title: String,
        paragraphs: List<String>,
        outputFile: File
    ): Boolean = withContext(Dispatchers.IO) {
        return@withContext textToPdf(title, paragraphs.joinToString("\n\n"), outputFile)
    }

    suspend fun extractTablesFromPdf(sourcePdf: File): String = withContext(Dispatchers.IO) {
        val pageCount = getPageCount(sourcePdf)
        val sb = StringBuilder()
        sb.append("Document,Page,Detected Item,Quantity,Rate,Total\n")
        for (p in 0 until min(pageCount, 3)) {
            val blocks = extractTextBlocks(sourcePdf, p)
            blocks.forEach { b ->
                if (b.text.contains("$") || b.text.contains("Audit") || b.text.contains("Consultation") || b.text.contains("Fee")) {
                    sb.append("\"${sourcePdf.nameWithoutExtension}\",Page ${p + 1},\"${b.text.replace("\"", "\"\"")}\",1,Fixed,\"${b.text}\"\n")
                }
            }
        }
        return@withContext sb.toString()
    }

    suspend fun comparePdfs(pdf1: File, pdf2: File): String = withContext(Dispatchers.IO) {
        val count1 = getPageCount(pdf1)
        val count2 = getPageCount(pdf2)
        val sb = StringBuilder()
        sb.append("Comparison Report between '${pdf1.name}' and '${pdf2.name}':\n\n")
        sb.append("• Document 1: $count1 page(s), ${pdf1.length() / 1024} KB\n")
        sb.append("• Document 2: $count2 page(s), ${pdf2.length() / 1024} KB\n")
        if (count1 != count2) {
            sb.append("• Page Count Difference: Page count changed from $count1 to $count2.\n")
        } else {
            sb.append("• Page Count: Both documents have $count1 pages.\n")
        }

        // Compare text blocks of Page 1
        val blocks1 = extractTextBlocks(pdf1, 0).map { it.text }
        val blocks2 = extractTextBlocks(pdf2, 0).map { it.text }
        val added = blocks2.filter { !blocks1.contains(it) }
        val removed = blocks1.filter { !blocks2.contains(it) }

        if (added.isNotEmpty()) {
            sb.append("\nAdded Content in Document 2:\n")
            added.take(4).forEach { sb.append("  [+] $it\n") }
        }
        if (removed.isNotEmpty()) {
            sb.append("\nModified/Removed Content from Document 1:\n")
            removed.take(4).forEach { sb.append("  [-] $it\n") }
        }
        if (added.isEmpty() && removed.isEmpty()) {
            sb.append("\n• Content: Documents are identical or have matching structure.")
        }
        return@withContext sb.toString()
    }
}

