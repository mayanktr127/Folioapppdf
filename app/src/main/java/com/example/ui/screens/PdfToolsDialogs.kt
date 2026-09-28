package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
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
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Compare
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.MergeType
import androidx.compose.material.icons.filled.Numbers
import androidx.compose.material.icons.filled.RotateLeft
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.PdfDocumentItem
import com.example.ui.theme.AccentBlue
import com.example.ui.theme.AccentPurple
import com.example.ui.theme.CanvasBackground
import com.example.ui.theme.CranberryPale
import com.example.ui.theme.CranberryPrimary
import com.example.ui.theme.InkPrimary
import com.example.ui.theme.InkSecondary
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.SurfaceWhite

@Composable
fun MergePdfDialog(
    allDocs: List<PdfDocumentItem>,
    onDismiss: () -> Unit,
    onMerge: (selectedDocs: List<PdfDocumentItem>, title: String) -> Unit
) {
    val selectedDocs = remember { mutableStateListOf<PdfDocumentItem>() }
    var mergedTitle by remember { mutableStateOf("Merged_Folio_Doc") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.MergeType, contentDescription = null, tint = CranberryPrimary, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Merge PDFs", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = InkPrimary)
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Select 2 or more documents to merge in order:",
                    fontSize = 12.sp,
                    color = InkSecondary
                )
                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = mergedTitle,
                    onValueChange = { mergedTitle = it },
                    label = { Text("Output PDF Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Documents (${selectedDocs.size} selected):",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = InkPrimary
                )
                Spacer(modifier = Modifier.height(6.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 240.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(allDocs) { doc ->
                        val isSelected = selectedDocs.contains(doc)
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) CranberryPale else CanvasBackground,
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isSelected) CranberryPrimary else Color(0xFFE2E8F0)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (isSelected) selectedDocs.remove(doc) else selectedDocs.add(doc)
                                }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = isSelected,
                                    onCheckedChange = { checked ->
                                        if (checked) selectedDocs.add(doc) else selectedDocs.remove(doc)
                                    },
                                    colors = CheckboxDefaults.colors(checkedColor = CranberryPrimary)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = doc.title,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = InkPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${doc.pageCount} pages • ${doc.fileSize / 1024} KB",
                                        fontSize = 11.sp,
                                        color = InkSecondary
                                    )
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
                    if (selectedDocs.size >= 2) {
                        onMerge(selectedDocs.toList(), mergedTitle)
                        onDismiss()
                    }
                },
                enabled = selectedDocs.size >= 2,
                colors = ButtonDefaults.buttonColors(containerColor = CranberryPrimary),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("confirm_merge_btn")
            ) {
                Text("Merge (${selectedDocs.size}) Files", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun SplitPdfDialog(
    doc: PdfDocumentItem,
    onDismiss: () -> Unit,
    onSplitRange: (startPage: Int, endPage: Int, title: String) -> Unit,
    onSplitAll: () -> Unit
) {
    var splitMode by remember { mutableStateOf("range") } // "range" or "all"
    var startPageStr by remember { mutableStateOf("1") }
    var endPageStr by remember { mutableStateOf(doc.pageCount.toString()) }
    var splitTitle by remember { mutableStateOf("${doc.title.removeSuffix(".pdf")}_split") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Description, contentDescription = null, tint = CranberryPrimary, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Split PDF", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = InkPrimary)
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Source: ${doc.title} (${doc.pageCount} pages)",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = InkPrimary
                )
                Spacer(modifier = Modifier.height(14.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        selected = splitMode == "range",
                        onClick = { splitMode = "range" }
                    )
                    Text("Extract Page Range", fontSize = 13.sp, color = InkPrimary)
                }

                if (splitMode == "range") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 32.dp, top = 4.dp, bottom = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = startPageStr,
                            onValueChange = { startPageStr = it },
                            label = { Text("From") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        )
                        OutlinedTextField(
                            value = endPageStr,
                            onValueChange = { endPageStr = it },
                            label = { Text("To") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        )
                    }

                    OutlinedTextField(
                        value = splitTitle,
                        onValueChange = { splitTitle = it },
                        label = { Text("Result File Name") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 32.dp),
                        shape = RoundedCornerShape(8.dp)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        selected = splitMode == "all",
                        onClick = { splitMode = "all" }
                    )
                    Text("Extract All Pages into Separate PDFs", fontSize = 13.sp, color = InkPrimary)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (splitMode == "range") {
                        val s = (startPageStr.toIntOrNull() ?: 1).coerceIn(1, doc.pageCount)
                        val e = (endPageStr.toIntOrNull() ?: doc.pageCount).coerceIn(s, doc.pageCount)
                        onSplitRange(s, e, splitTitle)
                    } else {
                        onSplitAll()
                    }
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = CranberryPrimary),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(if (splitMode == "range") "Extract Range" else "Split All Pages", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun ExtractTextDialog(
    doc: PdfDocumentItem,
    extractedText: String,
    onDismiss: () -> Unit,
    onSaveAsTxt: (text: String) -> Unit
) {
    val context = LocalContext.current
    var copied by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Description, contentDescription = null, tint = CranberryPrimary, modifier = Modifier.size(22.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Extracted Text (OCR)", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = InkPrimary)
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = InkSecondary)
                }
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Text content extracted from '${doc.title}':",
                    fontSize = 12.sp,
                    color = InkSecondary
                )
                Spacer(modifier = Modifier.height(10.dp))

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = CanvasBackground,
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp)
                ) {
                    SelectionContainer {
                        LazyColumn(modifier = Modifier.padding(12.dp)) {
                            item {
                                Text(
                                    text = extractedText.ifBlank { "No readable text content extracted from this document." },
                                    fontSize = 13.sp,
                                    lineHeight = 19.sp,
                                    color = InkPrimary
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("PDF Text", extractedText))
                            copied = true
                        },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = if (copied) Icons.Default.Check else Icons.Default.ContentCopy,
                            contentDescription = null,
                            tint = if (copied) SuccessGreen else CranberryPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (copied) "Copied!" else "Copy Text", fontSize = 12.sp)
                    }

                    OutlinedButton(
                        onClick = {
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, extractedText)
                                putExtra(Intent.EXTRA_SUBJECT, "Extracted text from ${doc.title}")
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Share Extracted Text"))
                        },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, tint = CranberryPrimary, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Share", fontSize = 12.sp)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSaveAsTxt(extractedText)
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = CranberryPrimary),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Save as Text Note", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}

@Composable
fun RotatePdfDialog(
    doc: PdfDocumentItem,
    onDismiss: () -> Unit,
    onRotate: (angle: Float) -> Unit
) {
    var selectedAngle by remember { mutableStateOf(90f) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.RotateRight, contentDescription = null, tint = CranberryPrimary, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Rotate PDF", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = InkPrimary)
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text("Rotate all pages of '${doc.title}' permanently:", fontSize = 12.sp, color = InkSecondary)
                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = { selectedAngle = 270f },
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (selectedAngle == 270f) CranberryPale else Color.Transparent
                        ),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.RotateLeft, contentDescription = null, tint = CranberryPrimary)
                            Text("Left 90°", fontSize = 11.sp)
                        }
                    }

                    OutlinedButton(
                        onClick = { selectedAngle = 90f },
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (selectedAngle == 90f) CranberryPale else Color.Transparent
                        ),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.RotateRight, contentDescription = null, tint = CranberryPrimary)
                            Text("Right 90°", fontSize = 11.sp)
                        }
                    }

                    OutlinedButton(
                        onClick = { selectedAngle = 180f },
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (selectedAngle == 180f) CranberryPale else Color.Transparent
                        ),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.RotateRight, contentDescription = null, tint = CranberryPrimary)
                            Text("180°", fontSize = 11.sp)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onRotate(selectedAngle)
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = CranberryPrimary),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Apply Rotation", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun ProtectPdfDialog(
    doc: PdfDocumentItem,
    onDismiss: () -> Unit,
    onProtect: (password: String) -> Unit
) {
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Lock, contentDescription = null, tint = CranberryPrimary, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Protect PDF with Password", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = InkPrimary)
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text("Encrypt and password-protect '${doc.title}':", fontSize = 12.sp, color = InkSecondary)
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it; errorMsg = null },
                    label = { Text("Password") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it; errorMsg = null },
                    label = { Text("Confirm Password") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                errorMsg?.let {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = it, fontSize = 12.sp, color = Color.Red)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (password.isBlank()) {
                        errorMsg = "Password cannot be empty"
                    } else if (password != confirmPassword) {
                        errorMsg = "Passwords do not match"
                    } else {
                        onProtect(password)
                        onDismiss()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = CranberryPrimary),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Protect PDF", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun UnlockPdfDialog(
    doc: PdfDocumentItem,
    onDismiss: () -> Unit,
    onUnlock: (password: String) -> Unit
) {
    var password by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.LockOpen, contentDescription = null, tint = CranberryPrimary, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Unlock PDF", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = InkPrimary)
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text("Enter the password to remove security encryption from '${doc.title}':", fontSize = 12.sp, color = InkSecondary)
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onUnlock(password)
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = CranberryPrimary),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Unlock Document", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun RepairPdfDialog(
    doc: PdfDocumentItem,
    onDismiss: () -> Unit,
    onRepair: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Build, contentDescription = null, tint = CranberryPrimary, modifier = Modifier.size(22.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Repair PDF Structure", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = InkPrimary)
            }
        },
        text = {
            Column {
                Text(
                    text = "Folio will re-parse, fix corrupted cross-reference tables, and re-serialize '${doc.title}' into a 100% compliant PDF file.",
                    fontSize = 13.sp,
                    color = InkSecondary,
                    lineHeight = 18.sp
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onRepair()
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = CranberryPrimary),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Repair Now", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun ComparePdfDialog(
    allDocs: List<PdfDocumentItem>,
    onDismiss: () -> Unit,
    onCompare: (doc1: PdfDocumentItem, doc2: PdfDocumentItem) -> Unit
) {
    var doc1 by remember { mutableStateOf(allDocs.firstOrNull()) }
    var doc2 by remember { mutableStateOf(allDocs.getOrNull(1) ?: allDocs.firstOrNull()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Compare, contentDescription = null, tint = CranberryPrimary, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Compare PDFs", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = InkPrimary)
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text("Select two documents to compare differences, page counts, and metadata:", fontSize = 12.sp, color = InkSecondary)
                Spacer(modifier = Modifier.height(14.dp))

                Text("Document 1:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = InkPrimary)
                Spacer(modifier = Modifier.height(4.dp))
                LazyColumn(modifier = Modifier.heightIn(max = 120.dp)) {
                    items(allDocs) { d ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { doc1 = d }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = doc1 == d, onClick = { doc1 = d })
                            Text(d.title, fontSize = 12.sp, color = InkPrimary, maxLines = 1)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = CanvasBackground)
                Spacer(modifier = Modifier.height(10.dp))

                Text("Document 2:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = InkPrimary)
                Spacer(modifier = Modifier.height(4.dp))
                LazyColumn(modifier = Modifier.heightIn(max = 120.dp)) {
                    items(allDocs) { d ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { doc2 = d }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = doc2 == d, onClick = { doc2 = d })
                            Text(d.title, fontSize = 12.sp, color = InkPrimary, maxLines = 1)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (doc1 != null && doc2 != null) {
                        onCompare(doc1!!, doc2!!)
                        onDismiss()
                    }
                },
                enabled = doc1 != null && doc2 != null && doc1 != doc2,
                colors = ButtonDefaults.buttonColors(containerColor = CranberryPrimary),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Compare Now", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun PageNumbersDialog(
    doc: PdfDocumentItem,
    onDismiss: () -> Unit,
    onApply: (format: String) -> Unit
) {
    var selectedFormat by remember { mutableStateOf("Page X of Y") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Numbers, contentDescription = null, tint = CranberryPrimary, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Add Page Numbers", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = InkPrimary)
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text("Choose numbering format to stamp on bottom of each page:", fontSize = 12.sp, color = InkSecondary)
                Spacer(modifier = Modifier.height(14.dp))

                val formats = listOf("Page X of Y", "Page X", "X / Y", "X")
                formats.forEach { fmt ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedFormat = fmt }
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = selectedFormat == fmt, onClick = { selectedFormat = fmt })
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(fmt, fontSize = 13.sp, color = InkPrimary)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onApply(selectedFormat)
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = CranberryPrimary),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Apply Page Numbers", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
