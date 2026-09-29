package com.example.engine

import android.graphics.Bitmap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb

enum class AnnotationType {
    HIGHLIGHT,
    UNDERLINE,
    STRIKETHROUGH,
    INK,
    TEXT,
    OVERLAY_EDIT,
    TEXT_REPLACE,
    RECTANGLE,
    CIRCLE,
    SIGNATURE,
    WATERMARK,
    REDACTION,
    PAGE_NUMBER
}

sealed class PdfValidationResult {
    object Valid : PdfValidationResult()
    object PasswordProtected : PdfValidationResult()
    object CorruptedOrNotPdf : PdfValidationResult()
    object EmptyOrZeroPages : PdfValidationResult()
}

data class RectFData(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float
) {
    val width: Float get() = kotlin.math.abs(right - left)
    val height: Float get() = kotlin.math.abs(bottom - top)
}

data class PdfTextBlock(
    val id: String = java.util.UUID.randomUUID().toString(),
    val pageIndex: Int,
    val text: String,
    val originalText: String = text,
    val rect: RectFData, // Normalized coordinates (0f..1f)
    val fontSize: Float = 14f,
    val isBold: Boolean = false,
    val textColor: Int = android.graphics.Color.parseColor("#182230"),
    val backgroundColor: Int = android.graphics.Color.WHITE,
    val isModified: Boolean = false
)

data class AnnotationData(
    val id: String = java.util.UUID.randomUUID().toString(),
    val pageIndex: Int,
    val type: AnnotationType,
    val points: List<Pair<Float, Float>> = emptyList(),
    val rect: RectFData? = null,
    val text: String? = null,
    val originalText: String? = null,
    val color: Int = android.graphics.Color.RED,
    val strokeWidth: Float = 4f,
    val opacity: Float = 1.0f,
    val signatureBitmap: Bitmap? = null
)

enum class CompressionPreset(val title: String, val description: String, val scale: Float, val jpegQuality: Int) {
    MINIMAL("Minimal Compression", "Maximum quality, larger file size", 1.2f, 90),
    STANDARD("Standard Compression", "Balanced quality and file size", 1.0f, 75),
    MAXIMUM("Maximum Compression", "Smallest file size, good for email sharing", 0.75f, 50)
}

data class CompressionResult(
    val originalBytes: Long,
    val compressedBytes: Long,
    val savedPercentage: Int,
    val outputFile: java.io.File
)

data class FolioPageState(
    val pageId: String = java.util.UUID.randomUUID().toString(),
    val originalPageIndex: Int, // 0-based page index in the original source PDF
    val rotationDegrees: Float = 0f, // 0f, 90f, 180f, 270f
    val annotations: List<AnnotationData> = emptyList(),
    val textBlocks: List<PdfTextBlock> = emptyList()
)

data class FolioDocumentState(
    val docItem: com.example.data.model.PdfDocumentItem,
    val sourceFile: java.io.File,
    val pages: List<FolioPageState> = emptyList(),
    val hasUnsavedChanges: Boolean = false
) {
    val pageCount: Int get() = pages.size
}
