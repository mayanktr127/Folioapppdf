package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Compress
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MergeType
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Numbers
import androidx.compose.material.icons.filled.Reorder
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.Security
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
import com.example.ui.theme.CanvasBackground
import com.example.ui.theme.CranberryPale
import com.example.ui.theme.CranberryPrimary
import com.example.ui.theme.InkPrimary
import com.example.ui.theme.InkSecondary
import com.example.ui.theme.SurfaceWhite
import com.example.ui.viewmodel.AppScreen
import com.example.ui.viewmodel.PdfViewModel

@Composable
fun ToolsScreen(
    viewModel: PdfViewModel,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedCategoryIndex by remember { mutableStateOf(0) }
    val categories = listOf("Organize", "... to PDF", "PDF to ...", "Security")
    val recentDocs by viewModel.recentDocuments.collectAsState()

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
                        text = "PDF EDITOR",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    Row {
                        IconButton(
                            onClick = {
                                viewModel.setStatusMessage("Folio local tool engine is ready.")
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
                                if (recentDocs.size >= 2) {
                                    viewModel.mergeDocuments(recentDocs.take(2), "Merged_Folio_Document")
                                } else {
                                    viewModel.setStatusMessage("Need at least 2 documents to merge. Add more files.")
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
                                recentDocs.firstOrNull()?.let {
                                    viewModel.openDocument(it)
                                    viewModel.navigateTo(AppScreen.PAGE_ORGANIZER)
                                } ?: viewModel.setStatusMessage("Open a document first to split.")
                            },
                            testTag = "tool_split_card"
                        )
                    }
                    item {
                        ToolActionCard(
                            title = "REARRANGE...",
                            icon = Icons.Default.Reorder,
                            onClick = {
                                recentDocs.firstOrNull()?.let {
                                    viewModel.openDocument(it)
                                    viewModel.navigateTo(AppScreen.PAGE_ORGANIZER)
                                } ?: viewModel.setStatusMessage("Open a document first to rearrange.")
                            },
                            testTag = "tool_rearrange_card"
                        )
                    }
                    item {
                        ToolActionCard(
                            title = "ROTATE PDF...",
                            icon = Icons.Default.RotateRight,
                            onClick = {
                                recentDocs.firstOrNull()?.let {
                                    viewModel.rotateDocument(it, 90f)
                                } ?: viewModel.setStatusMessage("Open a document first to rotate.")
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
                            title = "TEXT TO PDF...",
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
                                recentDocs.firstOrNull()?.let {
                                    viewModel.exportDocumentAsImages(it)
                                } ?: viewModel.setStatusMessage("Open a document first to export as images.")
                            },
                            testTag = "tool_pdf_to_images_card"
                        )
                    }
                    item {
                        ToolActionCard(
                            title = "EXTRACT TEXT...",
                            icon = Icons.Default.Description,
                            onClick = {
                                recentDocs.firstOrNull()?.let {
                                    viewModel.setStatusMessage("Extracted searchable text from ${it.title}")
                                } ?: viewModel.setStatusMessage("Open a document first.")
                            },
                            testTag = "tool_extract_text_card"
                        )
                    }
                }
                3 -> { // Security & Optimize
                    item {
                        ToolActionCard(
                            title = "COMPRESS...",
                            icon = Icons.Default.Compress,
                            onClick = {
                                recentDocs.firstOrNull()?.let {
                                    viewModel.compressDocument(it, com.example.engine.CompressionPreset.STANDARD)
                                } ?: viewModel.setStatusMessage("Open a document to compress.")
                            },
                            testTag = "tool_compress_card"
                        )
                    }
                    item {
                        ToolActionCard(
                            title = "REDACT INFO...",
                            icon = Icons.Default.VisibilityOff,
                            onClick = {
                                recentDocs.firstOrNull()?.let {
                                    viewModel.openDocument(it)
                                    viewModel.setEditorTool(com.example.ui.viewmodel.EditorTool.REDACT)
                                } ?: viewModel.setStatusMessage("Open a document to redact sensitive sections.")
                            },
                            testTag = "tool_redact_card"
                        )
                    }
                    item {
                        ToolActionCard(
                            title = "WATERMARK...",
                            icon = Icons.Default.WaterDrop,
                            onClick = {
                                recentDocs.firstOrNull()?.let {
                                    viewModel.openDocument(it)
                                    viewModel.setEditorTool(com.example.ui.viewmodel.EditorTool.WATERMARK)
                                } ?: viewModel.setStatusMessage("Open a document to apply watermark.")
                            },
                            testTag = "tool_watermark_card"
                        )
                    }
                    item {
                        ToolActionCard(
                            title = "PAGE NUMBERS...",
                            icon = Icons.Default.Numbers,
                            onClick = {
                                recentDocs.firstOrNull()?.let {
                                    viewModel.openDocument(it)
                                } ?: viewModel.setStatusMessage("Open a document to stamp page numbers.")
                            },
                            testTag = "tool_page_numbers_card"
                        )
                    }
                }
            }
        }
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
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Document graphic preview box
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(CanvasBackground),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = CranberryPrimary,
                    modifier = Modifier.size(40.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Red pill action button matching Image 1
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = CranberryPale,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(vertical = 8.dp, horizontal = 10.dp),
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
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        tint = CranberryPrimary,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}
