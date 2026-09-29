package com.example.ui.screens

import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FindReplace
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.Gesture
import androidx.compose.material.icons.filled.Highlight
import androidx.compose.material.icons.filled.NavigateBefore
import androidx.compose.material.icons.filled.NavigateNext
import androidx.compose.material.icons.filled.Numbers
import androidx.compose.material.icons.filled.Reorder
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.AnnotationData
import com.example.engine.AnnotationType
import com.example.engine.PdfTextBlock
import com.example.engine.RectFData
import com.example.ui.theme.BorderLight
import com.example.ui.theme.CanvasBackground
import com.example.ui.theme.CranberryPale
import com.example.ui.theme.CranberryPrimary
import com.example.ui.theme.InkPrimary
import com.example.ui.theme.InkSecondary
import com.example.ui.theme.SurfaceWhite
import com.example.ui.viewmodel.AppScreen
import com.example.ui.viewmodel.EditorTool
import com.example.ui.viewmodel.PdfViewModel

@Composable
fun EditorScreen(
    viewModel: PdfViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }

    val context = LocalContext.current
    val activeDoc by viewModel.activeDocument.collectAsState()
    val docState by viewModel.docState.collectAsState()
    val hasUnsavedChanges by viewModel.hasUnsavedChanges.collectAsState()
    val activePageIndex by viewModel.activePageIndex.collectAsState()
    val pageBitmap by viewModel.activePageBitmap.collectAsState()
    val textBlocks by viewModel.currentTextBlocks.collectAsState()
    val activeTool by viewModel.editorTool.collectAsState()
    val activeColor by viewModel.activeColor.collectAsState()
    val strokeWidth by viewModel.activeStrokeWidth.collectAsState()
    val annotations by viewModel.annotations.collectAsState()
    val isProcessing by viewModel.isProcessing.collectAsState()
    val savedSignatures by viewModel.savedSignatures.collectAsState()

    // Dialog states
    var showSignatureDialog by remember { mutableStateOf(false) }
    var showWatermarkDialog by remember { mutableStateOf(false) }
    var watermarkText by remember { mutableStateOf("CONFIDENTIAL") }
    var showTextDialog by remember { mutableStateOf(false) }
    var textInput by remember { mutableStateOf("") }
    var showSaveDialog by remember { mutableStateOf(false) }
    var saveCopyNameSuffix by remember { mutableStateOf("_edited") }
    var showEditToolMenu by remember { mutableStateOf(false) }
    var showAssistantSheet by remember { mutableStateOf(false) }
    var isOverlayEditMode by remember { mutableStateOf(false) }

    // Text Editing states
    var selectedBlockToEdit by remember { mutableStateOf<PdfTextBlock?>(null) }
    var showFindReplaceDialog by remember { mutableStateOf(false) }
    var showTextBlocksSheet by remember { mutableStateOf(false) }

    val savedResultDoc by viewModel.savedResultDoc.collectAsState()

    // Canvas Zoom & Pan
    var scale by remember { mutableFloatStateOf(1.0f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    // Realtime drawing state
    val livePoints = remember { mutableStateListOf<Offset>() }

    val doc = activeDoc

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFE5E7EB))
    ) {
        // Top Bar
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = SurfaceWhite,
            shadowElevation = 2.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Close / Back button ("X")
                IconButton(onClick = onBack, modifier = Modifier.testTag("editor_back_btn")) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = InkPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Title & Page indicator
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f).padding(horizontal = 6.dp)
                ) {
                    Text(
                        text = docState?.docItem?.title ?: doc?.title ?: "Document.pdf",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = InkPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    val totalPages = docState?.pageCount ?: doc?.pageCount ?: 1
                    Text(
                        text = "Page ${activePageIndex + 1} of $totalPages",
                        fontSize = 11.sp,
                        color = InkSecondary
                    )
                }

                // Text Lines Sheet, Find & Replace, Undo, Redo & Save Action
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { showFindReplaceDialog = true },
                        modifier = Modifier.size(34.dp).testTag("editor_find_replace_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.FindReplace,
                            contentDescription = "Find and Replace",
                            tint = CranberryPrimary,
                            modifier = Modifier.size(19.dp)
                        )
                    }

                    IconButton(
                        onClick = { showTextBlocksSheet = true },
                        modifier = Modifier.size(34.dp).testTag("editor_text_lines_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.FormatListBulleted,
                            contentDescription = "Detected Text Lines",
                            tint = InkPrimary,
                            modifier = Modifier.size(19.dp)
                        )
                    }

                    IconButton(
                        onClick = { viewModel.undo() },
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Undo,
                            contentDescription = "Undo",
                            tint = InkPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    IconButton(
                        onClick = { viewModel.redo() },
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Redo,
                            contentDescription = "Redo",
                            tint = InkPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    // Red "Save a copy" button with persistent unsaved changes indicator dot
                    Button(
                        onClick = { viewModel.saveAnnotatedCopy() },
                        modifier = Modifier.height(34.dp).testTag("editor_save_btn"),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = CranberryPrimary),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 0.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (hasUnsavedChanges) {
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFFBBF24))
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                            }
                            Text(
                                text = if (hasUnsavedChanges) "Save copy *" else "Save copy",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // Active Tool Sub-Bar (When an annotation tool is active)
        if (activeTool != EditorTool.NONE) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = SurfaceWhite,
                shadowElevation = 1.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = CranberryPale,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (activeTool == EditorTool.EDIT_PDF_TEXT) Icons.Default.Edit else Icons.Default.Draw,
                                    contentDescription = null,
                                    tint = CranberryPrimary,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (activeTool == EditorTool.EDIT_PDF_TEXT) "Tap any text box to replace visible text on this page" else "Active: ${activeTool.name}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = CranberryPrimary
                        )
                    }

                    // Color palette chips
                    if (activeTool != EditorTool.EDIT_PDF_TEXT) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            val colors = listOf(
                                android.graphics.Color.parseColor("#D52B49"), // Red
                                android.graphics.Color.parseColor("#2563EB"), // Blue
                                android.graphics.Color.parseColor("#182230"), // Black
                                android.graphics.Color.parseColor("#F59E0B"), // Amber Highlight
                                android.graphics.Color.parseColor("#16A34A")  // Green
                            )
                            colors.forEach { c ->
                                Box(
                                    modifier = Modifier
                                        .size(22.dp)
                                        .clip(CircleShape)
                                        .background(Color(c))
                                        .clickable { viewModel.setActiveColor(c) }
                                        .border(
                                            width = if (activeColor == c) 2.dp else 0.dp,
                                            color = if (activeColor == c) Color.White else Color.Transparent,
                                            shape = CircleShape
                                        )
                                )
                            }
                        }
                    }

                    TextButton(onClick = { viewModel.setEditorTool(EditorTool.NONE) }) {
                        Text("Done", fontSize = 12.sp, color = CranberryPrimary)
                    }
                }
            }
        }

        // Central Document Canvas with Pan & Zoom & Interactive Drawing & Text Editing
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(0.dp))
                .pointerInput(activeTool) {
                    if (activeTool == EditorTool.NONE) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            scale = (scale * zoom).coerceIn(1.0f, 3.5f)
                            if (scale > 1.0f) {
                                offsetX += pan.x
                                offsetY += pan.y
                            } else {
                                offsetX = 0f
                                offsetY = 0f
                            }
                        }
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            if (pageBitmap != null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp)
                        .graphicsLayer(
                            scaleX = scale,
                            scaleY = scale,
                            translationX = offsetX,
                            translationY = offsetY
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    BoxWithConstraints(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color.White)
                            .border(1.dp, BorderLight)
                    ) {
                        val boxW = maxWidth
                        val boxH = maxHeight

                        // 1. Rendered underlying PDF page bitmap
                        Image(
                            bitmap = pageBitmap!!.asImageBitmap(),
                            contentDescription = "PDF Page",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize()
                        )

                        // 2. Annotation & Replacement Text Overlay Canvas
                        Canvas(
                            modifier = Modifier
                                .fillMaxSize()
                                .pointerInput(activeTool, activeColor, strokeWidth) {
                                    if (activeTool != EditorTool.NONE && activeTool != EditorTool.EDIT_PDF_TEXT) {
                                        detectDragGestures(
                                            onDragStart = { offset ->
                                                livePoints.add(offset)
                                            },
                                            onDrag = { change, _ ->
                                                change.consume()
                                                livePoints.add(change.position)
                                            },
                                            onDragEnd = {
                                                if (livePoints.isNotEmpty()) {
                                                    val normPoints = livePoints.map {
                                                        (it.x / size.width) to (it.y / size.height)
                                                    }
                                                    val minX = normPoints.minOf { it.first }
                                                    val maxX = normPoints.maxOf { it.first }
                                                    val minY = normPoints.minOf { it.second }
                                                    val maxY = normPoints.maxOf { it.second }

                                                    when (activeTool) {
                                                        EditorTool.INK -> {
                                                            viewModel.addAnnotation(
                                                                AnnotationData(
                                                                    pageIndex = activePageIndex,
                                                                    type = AnnotationType.INK,
                                                                    points = normPoints,
                                                                    color = activeColor,
                                                                    strokeWidth = strokeWidth
                                                                )
                                                            )
                                                        }
                                                        EditorTool.HIGHLIGHT -> {
                                                            viewModel.addAnnotation(
                                                                AnnotationData(
                                                                    pageIndex = activePageIndex,
                                                                    type = AnnotationType.HIGHLIGHT,
                                                                    rect = RectFData(minX, minY, maxX, maxY),
                                                                    color = activeColor,
                                                                    opacity = 0.45f
                                                                )
                                                            )
                                                        }
                                                        EditorTool.UNDERLINE -> {
                                                            viewModel.addAnnotation(
                                                                AnnotationData(
                                                                    pageIndex = activePageIndex,
                                                                    type = AnnotationType.UNDERLINE,
                                                                    rect = RectFData(minX, minY, maxX, maxY),
                                                                    color = activeColor
                                                                )
                                                            )
                                                        }
                                                        EditorTool.STRIKETHROUGH -> {
                                                            viewModel.addAnnotation(
                                                                AnnotationData(
                                                                    pageIndex = activePageIndex,
                                                                    type = AnnotationType.STRIKETHROUGH,
                                                                    rect = RectFData(minX, minY, maxX, maxY),
                                                                    color = activeColor
                                                                )
                                                            )
                                                        }
                                                        EditorTool.RECTANGLE -> {
                                                            viewModel.addAnnotation(
                                                                AnnotationData(
                                                                    pageIndex = activePageIndex,
                                                                    type = AnnotationType.RECTANGLE,
                                                                    rect = RectFData(minX, minY, maxX, maxY),
                                                                    color = activeColor,
                                                                    strokeWidth = strokeWidth
                                                                )
                                                            )
                                                        }
                                                        EditorTool.CIRCLE -> {
                                                            viewModel.addAnnotation(
                                                                AnnotationData(
                                                                    pageIndex = activePageIndex,
                                                                    type = AnnotationType.CIRCLE,
                                                                    rect = RectFData(minX, minY, maxX, maxY),
                                                                    color = activeColor,
                                                                    strokeWidth = strokeWidth
                                                                )
                                                            )
                                                        }
                                                        EditorTool.REDACT -> {
                                                            viewModel.addAnnotation(
                                                                AnnotationData(
                                                                    pageIndex = activePageIndex,
                                                                    type = AnnotationType.REDACTION,
                                                                    rect = RectFData(minX, minY, maxX, maxY),
                                                                    color = android.graphics.Color.BLACK
                                                                )
                                                            )
                                                        }
                                                        else -> {}
                                                    }
                                                    livePoints.clear()
                                                }
                                            },
                                            onDragCancel = {
                                                livePoints.clear()
                                            }
                                        )
                                    }
                                }
                        ) {
                            val w = size.width
                            val h = size.height

                            // Draw saved annotations for active page
                            for (annot in annotations.filter { it.pageIndex == activePageIndex }) {
                                when (annot.type) {
                                    AnnotationType.INK -> {
                                        if (annot.points.size > 1) {
                                            val path = Path()
                                            path.moveTo(annot.points[0].first * w, annot.points[0].second * h)
                                            for (i in 1 until annot.points.size) {
                                                path.lineTo(annot.points[i].first * w, annot.points[i].second * h)
                                            }
                                            drawPath(
                                                path = path,
                                                color = Color(annot.color),
                                                style = Stroke(
                                                    width = annot.strokeWidth,
                                                    cap = StrokeCap.Round,
                                                    join = StrokeJoin.Round
                                                )
                                            )
                                        }
                                    }
                                    AnnotationType.HIGHLIGHT -> {
                                        annot.rect?.let { r ->
                                            drawRect(
                                                color = Color(annot.color).copy(alpha = 0.4f),
                                                topLeft = Offset(r.left * w, r.top * h),
                                                size = androidx.compose.ui.geometry.Size(r.width * w, r.height * h)
                                            )
                                        }
                                    }
                                    AnnotationType.UNDERLINE -> {
                                        annot.rect?.let { r ->
                                            drawLine(
                                                color = Color(annot.color),
                                                start = Offset(r.left * w, r.bottom * h),
                                                end = Offset(r.right * w, r.bottom * h),
                                                strokeWidth = 3f
                                            )
                                        }
                                    }
                                    AnnotationType.STRIKETHROUGH -> {
                                        annot.rect?.let { r ->
                                            val midY = (r.top + r.bottom) / 2f * h
                                            drawLine(
                                                color = Color(annot.color),
                                                start = Offset(r.left * w, midY),
                                                end = Offset(r.right * w, midY),
                                                strokeWidth = 3f
                                            )
                                        }
                                    }
                                    AnnotationType.RECTANGLE -> {
                                        annot.rect?.let { r ->
                                            drawRect(
                                                color = Color(annot.color),
                                                topLeft = Offset(r.left * w, r.top * h),
                                                size = androidx.compose.ui.geometry.Size(r.width * w, r.height * h),
                                                style = Stroke(width = annot.strokeWidth)
                                            )
                                        }
                                    }
                                    AnnotationType.CIRCLE -> {
                                        annot.rect?.let { r ->
                                            drawOval(
                                                color = Color(annot.color),
                                                topLeft = Offset(r.left * w, r.top * h),
                                                size = androidx.compose.ui.geometry.Size(r.width * w, r.height * h),
                                                style = Stroke(width = annot.strokeWidth)
                                            )
                                        }
                                    }
                                    AnnotationType.REDACTION -> {
                                        annot.rect?.let { r ->
                                            drawRect(
                                                color = Color.Black,
                                                topLeft = Offset(r.left * w, r.top * h),
                                                size = androidx.compose.ui.geometry.Size(r.width * w, r.height * h)
                                            )
                                        }
                                    }
                                    AnnotationType.TEXT_REPLACE, AnnotationType.OVERLAY_EDIT -> {
                                        annot.rect?.let { r ->
                                            // 1. Cover original text with solid white
                                            drawRect(
                                                color = Color.White,
                                                topLeft = Offset(r.left * w, r.top * h),
                                                size = androidx.compose.ui.geometry.Size(r.width * w, r.height * h)
                                            )
                                            // 2. Draw replacement text in place
                                            if (!annot.text.isNullOrBlank()) {
                                                val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                                                    color = annot.color
                                                    textSize = kotlin.math.max(16f, r.height * h * 0.72f)
                                                    isFakeBoldText = annot.strokeWidth > 1f
                                                }
                                                val baseline = (r.bottom * h) - (r.height * h * 0.18f)
                                                drawContext.canvas.nativeCanvas.drawText(
                                                    annot.text,
                                                    r.left * w,
                                                    baseline,
                                                    paint
                                                )
                                            }
                                        }
                                    }
                                    AnnotationType.TEXT -> {
                                        annot.rect?.let { r ->
                                            if (!annot.text.isNullOrBlank()) {
                                                val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                                                    color = annot.color
                                                    textSize = kotlin.math.max(16f, r.height * h * 0.72f)
                                                    isFakeBoldText = annot.strokeWidth > 1f
                                                }
                                                val baseline = (r.bottom * h) - (r.height * h * 0.18f)
                                                drawContext.canvas.nativeCanvas.drawText(
                                                    annot.text,
                                                    r.left * w,
                                                    baseline,
                                                    paint
                                                )
                                            }
                                        }
                                    }
                                    AnnotationType.SIGNATURE -> {
                                        annot.rect?.let { r ->
                                            annot.signatureBitmap?.let { sigBmp ->
                                                drawImage(
                                                    image = sigBmp.asImageBitmap(),
                                                    dstOffset = androidx.compose.ui.unit.IntOffset((r.left * w).toInt(), (r.top * h).toInt()),
                                                    dstSize = androidx.compose.ui.unit.IntSize((r.width * w).toInt(), (r.height * h).toInt())
                                                )
                                            }
                                        }
                                    }
                                    AnnotationType.WATERMARK -> {
                                        annot.text?.let { text ->
                                            val wmPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                                                color = android.graphics.Color.parseColor("#44D52B49")
                                                textSize = w * 0.08f
                                                isFakeBoldText = true
                                                textAlign = android.graphics.Paint.Align.CENTER
                                            }
                                            drawContext.canvas.nativeCanvas.save()
                                            drawContext.canvas.nativeCanvas.rotate(-45f, w / 2f, h / 2f)
                                            drawContext.canvas.nativeCanvas.drawText(text, w / 2f, h / 2f, wmPaint)
                                            drawContext.canvas.nativeCanvas.restore()
                                        }
                                    }
                                    AnnotationType.PAGE_NUMBER -> {
                                        annot.text?.let { numStr ->
                                            val numPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                                                color = android.graphics.Color.DKGRAY
                                                textSize = 22f
                                                textAlign = android.graphics.Paint.Align.CENTER
                                            }
                                            drawContext.canvas.nativeCanvas.drawText(numStr, w / 2f, h - 28f, numPaint)
                                        }
                                    }
                                }
                            }

                            // Draw currently active drag stroke
                            if (livePoints.size > 1) {
                                val path = Path()
                                path.moveTo(livePoints[0].x, livePoints[0].y)
                                for (i in 1 until livePoints.size) {
                                    path.lineTo(livePoints[i].x, livePoints[i].y)
                                }
                                drawPath(
                                    path = path,
                                    color = Color(activeColor),
                                    style = Stroke(
                                        width = strokeWidth,
                                        cap = StrokeCap.Round,
                                        join = StrokeJoin.Round
                                    )
                                )
                            }
                        }

                        // 3. Interactive Text Blocks Overlay (When EDIT_PDF_TEXT mode is active)
                        if (activeTool == EditorTool.EDIT_PDF_TEXT) {
                            textBlocks.forEach { block ->
                                val leftOffset = boxW * block.rect.left
                                val topOffset = boxH * block.rect.top
                                val blockW = (boxW * block.rect.width).coerceAtLeast(40.dp)
                                val blockH = (boxH * block.rect.height).coerceAtLeast(24.dp)

                                Box(
                                    modifier = Modifier
                                        .offset(x = leftOffset, y = topOffset)
                                        .size(width = blockW, height = blockH)
                                        .border(
                                            width = 1.5.dp,
                                            color = if (block.isModified) CranberryPrimary else Color(0xFF2563EB).copy(alpha = 0.75f),
                                            shape = RoundedCornerShape(2.dp)
                                        )
                                        .background(
                                            if (block.isModified) CranberryPale.copy(alpha = 0.35f) else Color(0xFF2563EB).copy(alpha = 0.12f)
                                        )
                                        .clickable {
                                            selectedBlockToEdit = block
                                        }
                                        .testTag("text_block_${block.id}")
                                )
                            }
                        }
                    }
                }
            } else {
                CircularProgressIndicator(color = CranberryPrimary)
            }

            // Quick Page Navigation Floating Buttons (Left and Right)
            if (activePageIndex > 0) {
                IconButton(
                    onClick = { viewModel.setPageIndex(activePageIndex - 1) },
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = 8.dp)
                        .background(SurfaceWhite.copy(alpha = 0.85f), CircleShape)
                ) {
                    Icon(Icons.Default.NavigateBefore, contentDescription = "Previous Page", tint = InkPrimary)
                }
            }

            val totalPages = docState?.pageCount ?: doc?.pageCount ?: 1
            if (activePageIndex < totalPages - 1) {
                IconButton(
                    onClick = { viewModel.setPageIndex(activePageIndex + 1) },
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 8.dp)
                        .background(SurfaceWhite.copy(alpha = 0.85f), CircleShape)
                ) {
                    Icon(Icons.Default.NavigateNext, contentDescription = "Next Page", tint = InkPrimary)
                }
            }
        }

        // Bottom Toolbar:
        // "Arrange" | "Edit Text" | "Annotate" | "Add Sign" | "Watermark" | "Folio AI"
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding(),
            color = SurfaceWhite,
            shadowElevation = 8.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp, horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Edit Text in Document (MANDATORY REQUEST)
                EditorBottomItem(
                    icon = Icons.Default.TextFields,
                    label = "Edit Text",
                    selected = activeTool == EditorTool.EDIT_PDF_TEXT,
                    onClick = {
                        if (activeTool == EditorTool.EDIT_PDF_TEXT) {
                            viewModel.setEditorTool(EditorTool.NONE)
                        } else {
                            viewModel.setEditorTool(EditorTool.EDIT_PDF_TEXT)
                            viewModel.setStatusMessage("Text editing active. Tap any text on page to edit.")
                        }
                    },
                    testTag = "editor_tool_edit_text"
                )

                // Arrange / Rearrange
                EditorBottomItem(
                    icon = Icons.Default.Reorder,
                    label = "Arrange",
                    selected = false,
                    onClick = { viewModel.navigateTo(AppScreen.PAGE_ORGANIZER) },
                    testTag = "editor_tool_arrange"
                )

                // Annotate (Ink, Highlight, Shapes, Redact)
                Box {
                    EditorBottomItem(
                        icon = Icons.Default.Edit,
                        label = "Annotate",
                        selected = activeTool != EditorTool.NONE && activeTool != EditorTool.EDIT_PDF_TEXT,
                        onClick = { showEditToolMenu = true },
                        testTag = "editor_tool_annotate"
                    )

                    DropdownMenu(
                        expanded = showEditToolMenu,
                        onDismissRequest = { showEditToolMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Edit Text in Document") },
                            leadingIcon = { Icon(Icons.Default.TextFields, contentDescription = null, tint = CranberryPrimary) },
                            onClick = {
                                viewModel.setEditorTool(EditorTool.EDIT_PDF_TEXT)
                                showEditToolMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Find & Replace Text") },
                            leadingIcon = { Icon(Icons.Default.FindReplace, contentDescription = null, tint = CranberryPrimary) },
                            onClick = {
                                showFindReplaceDialog = true
                                showEditToolMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Add Custom Text Box") },
                            leadingIcon = { Icon(Icons.Default.TextFields, contentDescription = null, tint = CranberryPrimary) },
                            onClick = {
                                isOverlayEditMode = false
                                showTextDialog = true
                                showEditToolMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Pen / Freehand Drawing") },
                            leadingIcon = { Icon(Icons.Default.Draw, contentDescription = null, tint = CranberryPrimary) },
                            onClick = {
                                viewModel.setEditorTool(EditorTool.INK)
                                showEditToolMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Highlighter") },
                            leadingIcon = { Icon(Icons.Default.Highlight, contentDescription = null, tint = CranberryPrimary) },
                            onClick = {
                                viewModel.setEditorTool(EditorTool.HIGHLIGHT)
                                showEditToolMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Underline Text") },
                            leadingIcon = { Icon(Icons.Default.TextFields, contentDescription = null, tint = CranberryPrimary) },
                            onClick = {
                                viewModel.setEditorTool(EditorTool.UNDERLINE)
                                showEditToolMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Redact Sensitive Info") },
                            leadingIcon = { Icon(Icons.Default.VisibilityOff, contentDescription = null, tint = Color.Black) },
                            onClick = {
                                viewModel.setEditorTool(EditorTool.REDACT)
                                showEditToolMenu = false
                            }
                        )
                    }
                }

                // Add Sign
                EditorBottomItem(
                    icon = Icons.Default.Gesture,
                    label = "Add Sign",
                    selected = showSignatureDialog,
                    onClick = { showSignatureDialog = true },
                    testTag = "editor_tool_sign"
                )

                // Watermark & Numbers
                EditorBottomItem(
                    icon = Icons.Default.WaterDrop,
                    label = "Watermark",
                    selected = false,
                    onClick = { showWatermarkDialog = true },
                    testTag = "editor_tool_watermark"
                )

                // AI Assistant
                EditorBottomItem(
                    icon = Icons.Default.AutoAwesome,
                    label = "Folio AI",
                    selected = showAssistantSheet,
                    onClick = { showAssistantSheet = true },
                    testTag = "editor_tool_assistant"
                )
            }
        }
    }

    // Edit Document Text Dialog
    selectedBlockToEdit?.let { block ->
        EditDocumentTextDialog(
            block = block,
            onDismiss = { selectedBlockToEdit = null },
            onApply = { newText, color, isBold ->
                viewModel.editTextInPdf(block, newText, color, isBold)
                selectedBlockToEdit = null
            },
            onDelete = {
                viewModel.deleteTextInPdf(block)
                selectedBlockToEdit = null
            },
            onFindAndReplaceAll = { find, replace ->
                viewModel.executeFindAndReplace(find, replace, isEntireDoc = true)
                selectedBlockToEdit = null
            }
        )
    }

    // Find and Replace Dialog
    if (showFindReplaceDialog) {
        FindAndReplaceDialog(
            onDismiss = { showFindReplaceDialog = false },
            onExecute = { find, replace, isEntireDoc ->
                viewModel.executeFindAndReplace(find, replace, isEntireDoc)
            }
        )
    }

    // Page Text Blocks Sheet (List view of all text lines on page)
    if (showTextBlocksSheet) {
        PageTextBlocksSheet(
            pageIndex = activePageIndex,
            textBlocks = textBlocks,
            onDismiss = { showTextBlocksSheet = false },
            onSelectBlock = { block ->
                selectedBlockToEdit = block
            },
            onAddNewText = {
                isOverlayEditMode = false
                showTextDialog = true
            }
        )
    }

    // Signature Dialog
    if (showSignatureDialog) {
        SignatureDialog(
            savedSignatures = savedSignatures,
            onDismiss = { showSignatureDialog = false },
            onSignatureCreated = { sigBmp ->
                showSignatureDialog = false
                viewModel.addAnnotation(
                    AnnotationData(
                        pageIndex = activePageIndex,
                        type = AnnotationType.SIGNATURE,
                        rect = RectFData(0.25f, 0.70f, 0.75f, 0.85f),
                        signatureBitmap = sigBmp
                    )
                )
                viewModel.setStatusMessage("Placed signature on page ${activePageIndex + 1}")
            },
            onSaveToLibrary = { sigBmp ->
                viewModel.saveSignatureToLibrary("My Signature", sigBmp)
            }
        )
    }

    // Watermark Dialog
    if (showWatermarkDialog) {
        AlertDialog(
            onDismissRequest = { showWatermarkDialog = false },
            title = { Text("Add Watermark", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Enter watermark text to stamp across all pages:", fontSize = 13.sp, color = InkSecondary)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = watermarkText,
                        onValueChange = { watermarkText = it },
                        singleLine = true,
                        label = { Text("Watermark text") }
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (doc != null) {
                            for (p in 0 until doc.pageCount) {
                                viewModel.addAnnotation(
                                    AnnotationData(
                                        pageIndex = p,
                                        type = AnnotationType.WATERMARK,
                                        text = watermarkText
                                    )
                                )
                            }
                            viewModel.setStatusMessage("Applied watermark '$watermarkText'")
                        }
                        showWatermarkDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CranberryPrimary)
                ) {
                    Text("Apply Watermark")
                }
            },
            dismissButton = {
                TextButton(onClick = { showWatermarkDialog = false }) { Text("Cancel") }
            }
        )
    }

    // Text / Overlay Edit Dialog
    if (showTextDialog) {
        AlertDialog(
            onDismissRequest = { showTextDialog = false },
            title = {
                Text(
                    text = if (isOverlayEditMode) "Overlay Text Edit" else "Add Text Box",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    if (isOverlayEditMode) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFFFF3CD),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 10.dp)
                        ) {
                            Text(
                                text = "Overlay edit: Covers underlying text with solid white and types new text.",
                                fontSize = 11.sp,
                                color = Color(0xFF856404),
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    } else {
                        Text(
                            text = "Add a clean transparent text note onto the current page:",
                            fontSize = 12.sp,
                            color = InkSecondary,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }
                    OutlinedTextField(
                        value = textInput,
                        onValueChange = { textInput = it },
                        singleLine = true,
                        label = { Text("Text content") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("text_input_field")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (textInput.isNotBlank()) {
                            viewModel.addAnnotation(
                                AnnotationData(
                                    pageIndex = activePageIndex,
                                    type = if (isOverlayEditMode) AnnotationType.OVERLAY_EDIT else AnnotationType.TEXT,
                                    rect = RectFData(0.15f, 0.40f, 0.85f, 0.46f),
                                    text = textInput,
                                    color = activeColor
                                )
                            )
                            val modeMsg = if (isOverlayEditMode) "Placed overlay edit" else "Added text note"
                            viewModel.setStatusMessage(modeMsg)
                            textInput = ""
                            showTextDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CranberryPrimary)
                ) {
                    Text("Apply")
                }
            },
            dismissButton = {
                TextButton(onClick = { showTextDialog = false }) { Text("Cancel") }
            }
        )
    }

    // Save Result & Download Dialog
    savedResultDoc?.let { (newDoc, origDoc) ->
        SaveResultDialog(
            savedDoc = newDoc,
            originalDoc = origDoc,
            onDismiss = { viewModel.closeSaveResultDialog() },
            onDownloadToDevice = { fileName ->
                viewModel.downloadToDevice(fileName, newDoc)
            },
            onOpenInViewer = {
                viewModel.closeSaveResultDialog()
                viewModel.openDocument(newDoc)
            },
            onShare = {
                viewModel.shareDocument(context, newDoc)
            },
            onPrint = {
                viewModel.printDocument(context, newDoc)
            },
            onContinueEditing = {
                viewModel.closeSaveResultDialog()
            }
        )
    }

    // AI Assistant Sheet
    if (showAssistantSheet) {
        AssistantSheet(
            viewModel = viewModel,
            onDismiss = { showAssistantSheet = false }
        )
    }
}

@Composable
private fun EditorBottomItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 2.dp)
            .testTag(testTag)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (selected) CranberryPrimary else InkPrimary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) CranberryPrimary else InkSecondary
        )
    }
}
