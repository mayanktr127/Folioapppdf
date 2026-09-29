package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FindReplace
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.PdfTextBlock
import com.example.ui.theme.CanvasBackground
import com.example.ui.theme.CranberryPale
import com.example.ui.theme.CranberryPrimary
import com.example.ui.theme.InkPrimary
import com.example.ui.theme.InkSecondary
import com.example.ui.theme.SurfaceWhite

@Composable
fun EditDocumentTextDialog(
    block: PdfTextBlock,
    onDismiss: () -> Unit,
    onApply: (newText: String, color: Int, isBold: Boolean) -> Unit,
    onDelete: () -> Unit,
    onFindAndReplaceAll: (findText: String, replaceText: String) -> Unit
) {
    var editedText by remember { mutableStateOf(block.text) }
    var selectedColor by remember { mutableIntStateOf(block.textColor) }
    var isBold by remember { mutableStateOf(block.isBold) }
    var showReplacePrompt by remember { mutableStateOf(false) }

    val presetColors = listOf(
        android.graphics.Color.parseColor("#182230"), // Dark Black/Slate
        android.graphics.Color.parseColor("#D52B49"), // Cranberry Primary
        android.graphics.Color.parseColor("#2563EB"), // Blue
        android.graphics.Color.parseColor("#16A34A"), // Green
        android.graphics.Color.parseColor("#475467"), // Slate Gray
        android.graphics.Color.parseColor("#FFFFFF")  // White
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = null,
                        tint = CranberryPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Edit PDF Text",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = InkPrimary
                    )
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = InkSecondary)
                }
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = CanvasBackground,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "Original Text in PDF:",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = InkSecondary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = block.originalText,
                            fontSize = 13.sp,
                            color = InkPrimary,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Text(
                    text = "Replaces the visible text on this page",
                    fontSize = 11.sp,
                    color = InkSecondary,
                    modifier = Modifier.padding(top = 6.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Replacement Text:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = InkSecondary
                )
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = editedText,
                    onValueChange = { editedText = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("edit_text_input"),
                    shape = RoundedCornerShape(10.dp),
                    placeholder = { Text("Enter modified text...") }
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Styling options (Color & Bold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Color chips
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        presetColors.forEach { colorInt ->
                            val isSelected = selectedColor == colorInt
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(Color(colorInt))
                                    .border(
                                        width = if (isSelected) 2.5.dp else 1.dp,
                                        color = if (isSelected) CranberryPrimary else Color.LightGray,
                                        shape = CircleShape
                                    )
                                    .clickable { selectedColor = colorInt }
                            )
                        }
                    }

                    // Bold toggle
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isBold) CranberryPale else CanvasBackground,
                        modifier = Modifier.clickable { isBold = !isBold }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.FormatBold,
                                contentDescription = "Bold",
                                tint = if (isBold) CranberryPrimary else InkSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Bold",
                                fontSize = 12.sp,
                                fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal,
                                color = if (isBold) CranberryPrimary else InkSecondary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Find & Replace suggestion
                if (block.originalText != editedText && editedText.isNotBlank()) {
                    OutlinedButton(
                        onClick = {
                            onFindAndReplaceAll(block.originalText, editedText)
                            onDismiss()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FindReplace,
                            contentDescription = null,
                            tint = CranberryPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Replace all occurrences in PDF",
                            fontSize = 12.sp,
                            color = CranberryPrimary
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onApply(editedText, selectedColor, isBold)
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = CranberryPrimary),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("apply_text_edit_btn")
            ) {
                Text("Apply to PDF", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    onDelete()
                    onDismiss()
                }
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = Color.Red,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Clear Text", color = Color.Red)
                }
            }
        }
    )
}

@Composable
fun FindAndReplaceDialog(
    initialFindText: String = "",
    searchMatches: List<com.example.engine.PdfTextMatch> = emptyList(),
    isSearching: Boolean = false,
    onSearch: (String) -> Unit = {},
    onReplaceSingle: (com.example.engine.PdfTextMatch, String) -> Unit = { _, _ -> },
    onReplacePage: (pageIndex: Int, query: String, replacement: String) -> Unit = { _, _, _ -> },
    onReplaceAll: (query: String, replacement: String) -> Unit = { _, _ -> },
    onDismiss: () -> Unit
) {
    var findText by remember { mutableStateOf(initialFindText) }
    var replaceText by remember { mutableStateOf("") }
    var lastReplacedMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(findText) {
        if (findText.isNotBlank()) {
            kotlinx.coroutines.delay(200)
            onSearch(findText)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.FindReplace,
                    contentDescription = null,
                    tint = CranberryPrimary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Find & Replace Text",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = InkPrimary
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 480.dp)
            ) {
                Text(
                    text = "Search across all pages in the PDF and replace occurrences:",
                    fontSize = 12.sp,
                    color = InkSecondary
                )
                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = findText,
                    onValueChange = {
                        findText = it
                        lastReplacedMessage = null
                    },
                    label = { Text("Find text...") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    trailingIcon = {
                        if (findText.isNotEmpty()) {
                            IconButton(onClick = { findText = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear", modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = replaceText,
                    onValueChange = { replaceText = it },
                    label = { Text("Replace with...") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Search Results / Status Banner
                if (isSearching) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    ) {
                        androidx.compose.material3.CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = CranberryPrimary,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Scanning document pages...",
                            fontSize = 12.sp,
                            color = InkSecondary
                        )
                    }
                } else if (lastReplacedMessage != null) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = Color(0xFFECFDF5),
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF10B981))
                    ) {
                        Text(
                            text = lastReplacedMessage ?: "",
                            fontSize = 12.sp,
                            color = Color(0xFF047857),
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                } else if (findText.isNotBlank()) {
                    if (searchMatches.isEmpty()) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = CranberryPale,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "No matches found for '$findText'",
                                fontSize = 12.sp,
                                color = CranberryPrimary,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    } else {
                        val pagesList = searchMatches.map { it.pageIndex + 1 }
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = Color(0xFFEFF6FF),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = "${searchMatches.size} match(es) found on page(s): ${pagesList.joinToString(", ")}",
                                    fontSize = 12.sp,
                                    color = Color(0xFF1D4ED8),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Per-page replace chips
                        val distinctPages = searchMatches.map { it.pageIndex }.distinct()
                        if (distinctPages.size > 1) {
                            Text(
                                text = "Replace on specific page:",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = InkSecondary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            androidx.compose.foundation.lazy.LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(distinctPages) { pIdx ->
                                    val countOnPage = searchMatches.count { it.pageIndex == pIdx }
                                    FilterChip(
                                        selected = false,
                                        onClick = {
                                            if (replaceText.isNotBlank()) {
                                                onReplacePage(pIdx, findText, replaceText)
                                                lastReplacedMessage = "Replaced $countOnPage match(es) on Page ${pIdx + 1}"
                                            }
                                        },
                                        enabled = replaceText.isNotBlank(),
                                        label = { Text("Page ${pIdx + 1} ($countOnPage)", fontSize = 11.sp) }
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        // Matches list for single-occurrence replacement
                        Text(
                            text = "Occurrences (${searchMatches.size}):",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = InkSecondary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 160.dp)
                        ) {
                            items(searchMatches) { match ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = Color(0xFFF9FAFB)),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE5E7EB)),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                            Surface(
                                                color = CranberryPrimary.copy(alpha = 0.1f),
                                                shape = RoundedCornerShape(4.dp)
                                            ) {
                                                Text(
                                                    text = "Page ${match.pageIndex + 1}",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = CranberryPrimary,
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = match.lineText,
                                                fontSize = 11.sp,
                                                maxLines = 2,
                                                color = InkPrimary
                                            )
                                        }

                                        Button(
                                            onClick = {
                                                if (replaceText.isNotBlank()) {
                                                    onReplaceSingle(match, replaceText)
                                                    lastReplacedMessage = "Replaced occurrence on Page ${match.pageIndex + 1}"
                                                }
                                            },
                                            enabled = replaceText.isNotBlank(),
                                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                            shape = RoundedCornerShape(6.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = CranberryPrimary)
                                        ) {
                                            Text("Replace", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (findText.isNotBlank() && searchMatches.isNotEmpty()) {
                        val count = searchMatches.size
                        val pages = searchMatches.map { it.pageIndex + 1 }.distinct().joinToString(", ")
                        onReplaceAll(findText, replaceText)
                        lastReplacedMessage = "Replaced $count match(es) across pages: $pages"
                        onDismiss()
                    }
                },
                enabled = findText.isNotBlank() && searchMatches.isNotEmpty() && replaceText.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = CranberryPrimary),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("execute_find_replace_btn")
            ) {
                Text(
                    text = if (searchMatches.isNotEmpty()) "Replace All in PDF (${searchMatches.size})" else "Replace All Matches",
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun PageTextBlocksSheet(
    pageIndex: Int,
    textBlocks: List<PdfTextBlock>,
    onDismiss: () -> Unit,
    onSelectBlock: (PdfTextBlock) -> Unit,
    onAddNewText: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = SurfaceWhite
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Page ${pageIndex + 1} Text Lines",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = InkPrimary
                    )
                    Text(
                        text = "${textBlocks.size} editable text segments detected",
                        fontSize = 12.sp,
                        color = InkSecondary
                    )
                }

                Button(
                    onClick = {
                        onDismiss()
                        onAddNewText()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CranberryPrimary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Text", fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = CanvasBackground)
            Spacer(modifier = Modifier.height(10.dp))

            if (textBlocks.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No selectable text lines detected on this page. Tap 'Add Text' to insert new text blocks anywhere.",
                        fontSize = 13.sp,
                        color = InkSecondary,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(textBlocks) { block ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (block.isModified) CranberryPale else CanvasBackground,
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (block.isModified) CranberryPrimary else Color(0xFFE2E8F0)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onDismiss()
                                    onSelectBlock(block)
                                }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.TextFields,
                                        contentDescription = null,
                                        tint = if (block.isModified) CranberryPrimary else InkSecondary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = block.text,
                                            fontSize = 13.sp,
                                            fontWeight = if (block.isBold) FontWeight.Bold else FontWeight.Normal,
                                            color = InkPrimary,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        if (block.isModified) {
                                            Text(
                                                text = "Modified • Original: ${block.originalText}",
                                                fontSize = 10.sp,
                                                color = CranberryPrimary
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                Surface(
                                    shape = CircleShape,
                                    color = CranberryPale,
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = "Edit text",
                                            tint = CranberryPrimary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
