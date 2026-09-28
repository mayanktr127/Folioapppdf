package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Compare
import androidx.compose.material.icons.filled.Compress
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.MergeType
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Numbers
import androidx.compose.material.icons.filled.Reorder
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.PdfDocumentItem
import com.example.ui.theme.CanvasBackground
import com.example.ui.theme.CranberryPale
import com.example.ui.theme.CranberryPrimary
import com.example.ui.theme.InkPrimary
import com.example.ui.theme.InkSecondary
import com.example.ui.theme.SurfaceWhite
import com.example.ui.viewmodel.AppScreen
import com.example.ui.viewmodel.EditorTool
import com.example.ui.viewmodel.PdfViewModel
import kotlinx.coroutines.launch

@Composable
fun ToolsScreen(
    viewModel: PdfViewModel,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    var selectedCategoryIndex by remember { mutableStateOf(0) }
    val categories = listOf("Organize", "... to PDF", "PDF to ...", "Security")
    val allDocs by viewModel.allDocuments.collectAsState()
    val recentDocs by viewModel.recentDocuments.collectAsState()

    // Dialog trigger states
    var showMergeDialog by remember { mutableStateOf(false) }
    var docForSplit by remember { mutableStateOf<PdfDocumentItem?>(null) }
    var docForRotate by remember { mutableStateOf<PdfDocumentItem?>(null) }
    var docForExtractText by remember { mutableStateOf<Pair<PdfDocumentItem, String>?>(null) }
    var docForProtect by remember { mutableStateOf<PdfDocumentItem?>(null) }
    var docForUnlock by remember { mutableStateOf<PdfDocumentItem?>(null) }
    var docForRepair by remember { mutableStateOf<PdfDocumentItem?>(null) }
    var showCompareDialog by remember { mutableStateOf(false) }
    var docForPageNumbers by remember { mutableStateOf<PdfDocumentItem?>(null) }
    var docForCompress by remember { mutableStateOf<PdfDocumentItem?>(null) }
    var comparisonResult by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CanvasBackground)
    ) {
        // Red Top Header matching Image 1
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = CranberryPrimary,
            shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "PDF TOOLS",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    Row {
                        IconButton(
                            onClick = {
                                viewModel.setStatusMessage("All Folio tools run 100% on-device. No files uploaded to servers.")
                            },
                            modifier = Modifier
                                .size(38.dp)
                                .background(Color.White.copy(alpha = 0.2f), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = "Notifications",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        IconButton(
                            onClick = onOpenSettings,
                            modifier = Modifier
                                .size(38.dp)
                                .background(Color.White.copy(alpha = 0.2f), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Settings",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // White Category Pill Tabs matching Image 1
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    color = SurfaceWhite
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        categories.forEachIndexed { index, cat ->
                            val isSelected = selectedCategoryIndex == index
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(if (isSelected) CranberryPale else Color.Transparent)
                                    .clickable { selectedCategoryIndex = index }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = cat,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) CranberryPrimary else InkSecondary
                                )
                            }
                        }
                    }
                }
            }
        }

        // Section Title
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = categories[selectedCategoryIndex] + " PDF",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = InkPrimary,
            modifier = Modifier.padding(horizontal = 20.dp)
        )
        Spacer(modifier = Modifier.height(12.dp))

        // Tool Grid Cards matching Image 1
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(bottom = 100.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            when (selectedCategoryIndex) {
                0 -> { // Organize
                    item {
                        ToolActionCard(
                            title = "MERGE PDF...",
                            icon = Icons.Default.MergeType,
                            onClick = {
                                if (allDocs.size >= 2) {
                                    showMergeDialog = true
                                } else {
                                    viewModel.setStatusMessage("Need at least 2 documents in library to merge.")
                                }
                            },
                            testTag = "tool_merge_card"
                        )
                    }
                    item {
                        ToolActionCard(
                            title = "SPLIT PDF...",
                            icon = Icons.Default.Description,
                            onClick = {
                                val target = recentDocs.firstOrNull() ?: allDocs.firstOrNull()
                                if (target != null) {
                                    docForSplit = target
                                } else {
                                    viewModel.setStatusMessage("No document available to split.")
                                }
                            },
                            testTag = "tool_split_card"
                        )
                    }
                    item {
                        ToolActionCard(
                            title = "REARRANGE...",
                            icon = Icons.Default.Reorder,
                            onClick = {
                                val target = recentDocs.firstOrNull() ?: allDocs.firstOrNull()
                                if (target != null) {
                                    viewModel.openDocument(target)
                                    viewModel.navigateTo(AppScreen.PAGE_ORGANIZER)
                                } else {
                                    viewModel.setStatusMessage("Open a document first to rearrange.")
                                }
                            },
                            testTag = "tool_rearrange_card"
                        )
                    }
                    item {
                        ToolActionCard(
                            title = "ROTATE PDF...",
                            icon = Icons.Default.RotateRight,
                            onClick = {
                                val target = recentDocs.firstOrNull() ?: allDocs.firstOrNull()
                                if (target != null) {
                                    docForRotate = target
                                } else {
                                    viewModel.setStatusMessage("No document available to rotate.")
                                }
                            },
                            testTag = "tool_rotate_card"
                        )
                    }
                }
                1 -> { // ... to PDF
                    item {
                        ToolActionCard(
                            title = "ANY IMAGE TO PDF...",
                            icon = Icons.Default.Image,
                            onClick = { viewModel.navigateTo(AppScreen.CONVERT) },
                            testTag = "tool_img_to_pdf_card"
                        )
                    }
                    item {
                        ToolActionCard(
                            title = "SCAN TO PDF...",
                            icon = Icons.Default.CameraAlt,
                            onClick = { viewModel.navigateTo(AppScreen.SCANNER) },
                            testTag = "tool_scan_to_pdf_card"
                        )
                    }
                    item {
                        ToolActionCard(
                            title = "TEXT / WORD TO PDF...",
                            icon = Icons.Default.TextFields,
                            onClick = { viewModel.navigateTo(AppScreen.CONVERT) },
                            testTag = "tool_text_to_pdf_card"
                        )
                    }
                }
                2 -> { // PDF to ...
                    item {
                        ToolActionCard(
                            title = "PDF TO IMAGES...",
                            icon = Icons.Default.Image,
                            onClick = {
                                val target = recentDocs.firstOrNull() ?: allDocs.firstOrNull()
                                if (target != null) {
                                    viewModel.exportDocumentAsImages(target)
                                } else {
                                    viewModel.setStatusMessage("No document available to export.")
                                }
                            },
                            testTag = "tool_pdf_to_images_card"
                        )
                    }
                    item {
                        ToolActionCard(
                            title = "EXTRACT TEXT (OCR)...",
                            icon = Icons.Default.Description,
                            onClick = {
                                val target = recentDocs.firstOrNull() ?: allDocs.firstOrNull()
                                if (target != null) {
                                    coroutineScope.launch {
                                        val text = viewModel.extractAllDocumentText(target)
                                        docForExtractText = target to text
                                    }
                                } else {
                                    viewModel.setStatusMessage("No document available to extract.")
                                }
                            },
                            testTag = "tool_extract_text_card"
                        )
                    }
                }
                3 -> { // Security & Tools
                    item {
                        ToolActionCard(
                            title = "COMPRESS...",
                            icon = Icons.Default.Compress,
                            onClick = {
                                val target = recentDocs.firstOrNull() ?: allDocs.firstOrNull()
                                if (target != null) {
                                    docForCompress = target
                                } else {
                                    viewModel.setStatusMessage("No document available to compress.")
                                }
                            },
                            testTag = "tool_compress_card"
                        )
                    }
                    item {
                        ToolActionCard(
                            title = "PROTECT PDF...",
                            icon = Icons.Default.Lock,
                            onClick = {
                                val target = recentDocs.firstOrNull() ?: allDocs.firstOrNull()
                                if (target != null) {
                                    docForProtect = target
                                } else {
                                    viewModel.setStatusMessage("No document available to protect.")
                                }
                            },
                            testTag = "tool_protect_card"
                        )
                    }
                    item {
                        ToolActionCard(
                            title = "UNLOCK PDF...",
                            icon = Icons.Default.LockOpen,
                            onClick = {
                                val target = allDocs.firstOrNull { it.isLocked } ?: recentDocs.firstOrNull()
                                if (target != null) {
                                    docForUnlock = target
                                } else {
                                    viewModel.setStatusMessage("No locked document found.")
                                }
                            },
                            testTag = "tool_unlock_card"
                        )
                    }
                    item {
                        ToolActionCard(
                            title = "REDACT INFO...",
                            icon = Icons.Default.VisibilityOff,
                            onClick = {
                                val target = recentDocs.firstOrNull() ?: allDocs.firstOrNull()
                                if (target != null) {
                                    viewModel.openDocument(target)
                                    viewModel.setEditorTool(EditorTool.REDACT)
                                } else {
                                    viewModel.setStatusMessage("Open a document to redact sensitive sections.")
                                }
                            },
                            testTag = "tool_redact_card"
                        )
                    }
                    item {
                        ToolActionCard(
                            title = "WATERMARK...",
                            icon = Icons.Default.WaterDrop,
                            onClick = {
                                val target = recentDocs.firstOrNull() ?: allDocs.firstOrNull()
                                if (target != null) {
                                    viewModel.openDocument(target)
                                    viewModel.setEditorTool(EditorTool.WATERMARK)
                                } else {
                                    viewModel.setStatusMessage("Open a document to apply watermark.")
                                }
                            },
                            testTag = "tool_watermark_card"
                        )
                    }
                    item {
                        ToolActionCard(
                            title = "PAGE NUMBERS...",
                            icon = Icons.Default.Numbers,
                            onClick = {
                                val target = recentDocs.firstOrNull() ?: allDocs.firstOrNull()
                                if (target != null) {
                                    docForPageNumbers = target
                                } else {
                                    viewModel.setStatusMessage("Open a document to stamp page numbers.")
                                }
                            },
                            testTag = "tool_page_numbers_card"
                        )
                    }
                    item {
                        ToolActionCard(
                            title = "REPAIR PDF...",
                            icon = Icons.Default.Build,
                            onClick = {
                                val target = recentDocs.firstOrNull() ?: allDocs.firstOrNull()
                                if (target != null) {
                                    docForRepair = target
                                } else {
                                    viewModel.setStatusMessage("No document available to repair.")
                                }
                            },
                            testTag = "tool_repair_card"
                        )
                    }
                    item {
                        ToolActionCard(
                            title = "COMPARE PDFS...",
                            icon = Icons.Default.Compare,
                            onClick = {
                                if (allDocs.size >= 2) {
                                    showCompareDialog = true
                                } else {
                                    viewModel.setStatusMessage("Need at least 2 documents in library to compare.")
                                }
                            },
                            testTag = "tool_compare_card"
                        )
                    }
                }
            }
        }
    }

    // Merge PDF Dialog
    if (showMergeDialog) {
        MergePdfDialog(
            allDocs = allDocs,
            onDismiss = { showMergeDialog = false },
            onMerge = { docs, title ->
                viewModel.mergeDocuments(docs, title)
            }
        )
    }

    // Split PDF Dialog
    docForSplit?.let { doc ->
        SplitPdfDialog(
            doc = doc,
            onDismiss = { docForSplit = null },
            onSplitRange = { start, end, title ->
                viewModel.splitPdfRange(doc, start, end, title)
                docForSplit = null
            },
            onSplitAll = {
                viewModel.splitPdfAll(doc)
                docForSplit = null
            }
        )
    }

    // Rotate PDF Dialog
    docForRotate?.let { doc ->
        RotatePdfDialog(
            doc = doc,
            onDismiss = { docForRotate = null },
            onRotate = { degrees ->
                viewModel.rotateDocumentPages(doc, degrees)
                docForRotate = null
            }
        )
    }

    // Extract Text (OCR) Dialog
    docForExtractText?.let { (doc, text) ->
        ExtractTextDialog(
            doc = doc,
            extractedText = text,
            onDismiss = { docForExtractText = null },
            onSaveAsTxt = { content ->
                viewModel.convertTextToPdf("${doc.title.removeSuffix(".pdf")}_notes", content)
                docForExtractText = null
            }
        )
    }

    // Compress Dialog
    docForCompress?.let { doc ->
        val isProc by viewModel.isProcessing.collectAsState()
        val compResult by viewModel.compressionResult.collectAsState()
        CompressDialog(
            document = doc,
            isProcessing = isProc,
            lastResult = compResult,
            onDismiss = { docForCompress = null },
            onCompress = { preset ->
                viewModel.compressDocument(doc, preset)
                docForCompress = null
            }
        )
    }

    // Protect PDF Dialog
    docForProtect?.let { doc ->
        ProtectPdfDialog(
            doc = doc,
            onDismiss = { docForProtect = null },
            onProtect = { pwd ->
                viewModel.protectDocument(doc, pwd)
                docForProtect = null
            }
        )
    }

    // Unlock PDF Dialog
    docForUnlock?.let { doc ->
        UnlockPdfDialog(
            doc = doc,
            onDismiss = { docForUnlock = null },
            onUnlock = { pwd ->
                viewModel.unlockDocument(doc, pwd)
                docForUnlock = null
            }
        )
    }

    // Repair PDF Dialog
    docForRepair?.let { doc ->
        RepairPdfDialog(
            doc = doc,
            onDismiss = { docForRepair = null },
            onRepair = {
                viewModel.repairDocument(doc)
                docForRepair = null
            }
        )
    }

    // Compare PDF Dialog
    if (showCompareDialog) {
        ComparePdfDialog(
            allDocs = allDocs,
            onDismiss = { showCompareDialog = false },
            onCompare = { d1, d2 ->
                coroutineScope.launch {
                    val result = viewModel.compareDocuments(d1, d2)
                    comparisonResult = result
                }
            }
        )
    }

    // Comparison Result Dialog
    comparisonResult?.let { result ->
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { comparisonResult = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Compare, contentDescription = null, tint = CranberryPrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("PDF Comparison Result", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                }
            },
            text = {
                Column {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = CanvasBackground,
                        modifier = Modifier.fillMaxWidth().heightIn(max = 280.dp)
                    ) {
                        androidx.compose.foundation.lazy.LazyColumn(modifier = Modifier.padding(12.dp)) {
                            item {
                                Text(result, fontSize = 13.sp, color = InkPrimary, lineHeight = 18.sp)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                androidx.compose.material3.Button(
                    onClick = { comparisonResult = null },
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = CranberryPrimary)
                ) {
                    Text("Close")
                }
            }
        )
    }

    // Page Numbers Dialog
    docForPageNumbers?.let { doc ->
        PageNumbersDialog(
            doc = doc,
            onDismiss = { docForPageNumbers = null },
            onApply = { format ->
                viewModel.openDocument(doc)
                viewModel.applyPageNumbers(format)
                docForPageNumbers = null
            }
        )
    }
}

@Composable
private fun ToolActionCard(
    title: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    testTag: String
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag(testTag),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Document graphic preview box
            Box(
                modifier = Modifier
                    .size(74.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(CanvasBackground),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = CranberryPrimary,
                    modifier = Modifier.size(38.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Red pill action button matching Image 1
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = CranberryPale,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(vertical = 8.dp, horizontal = 8.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = title,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = CranberryPrimary,
                        maxLines = 1,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        tint = CranberryPrimary,
                        modifier = Modifier.size(13.dp)
                    )
                }
            }
        }
    }
}
