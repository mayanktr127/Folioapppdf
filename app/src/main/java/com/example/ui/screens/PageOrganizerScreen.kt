package com.example.ui.screens

import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.FolioPageState
import com.example.engine.PdfEngine
import com.example.ui.theme.BorderLight
import com.example.ui.theme.CanvasBackground
import com.example.ui.theme.CranberryPale
import com.example.ui.theme.CranberryPrimary
import com.example.ui.theme.InkPrimary
import com.example.ui.theme.InkSecondary
import com.example.ui.theme.SurfaceWhite
import com.example.ui.viewmodel.PdfViewModel
import java.io.File
import java.util.UUID

@Composable
fun PageOrganizerScreen(
    viewModel: PdfViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }

    val docState by viewModel.docState.collectAsState()
    val isProcessing by viewModel.isProcessing.collectAsState()

    // Working page states list (shared document state representation)
    val workingPages = remember { mutableStateListOf<FolioPageState>() }
    var selectedIndex by remember { mutableIntStateOf(0) }
    val pageThumbnails = remember { mutableStateMapOf<String, Bitmap>() }

    LaunchedEffect(docState) {
        val state = docState ?: return@LaunchedEffect
        workingPages.clear()
        workingPages.addAll(state.pages)
        if (selectedIndex >= workingPages.size) {
            selectedIndex = (workingPages.size - 1).coerceAtLeast(0)
        }

        val file = state.sourceFile
        if (file.exists()) {
            workingPages.forEach { page ->
                val bmp = PdfEngine.renderPageStateToBitmap(file, page, targetWidth = 360)
                if (bmp != null) pageThumbnails[page.pageId] = bmp
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CanvasBackground)
    ) {
        // Header
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = SurfaceWhite,
            shadowElevation = 2.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = InkPrimary
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Column {
                        Text(
                            text = "Rearrange Pages",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = InkPrimary
                        )
                        Text(
                            text = "Reorder, rotate, duplicate, or delete pages",
                            fontSize = 12.sp,
                            color = InkSecondary
                        )
                    }
                }

                Button(
                    onClick = {
                        if (workingPages.isNotEmpty()) {
                            viewModel.updateDocumentPages(workingPages.toList())
                            onBack()
                        }
                    },
                    enabled = !isProcessing && workingPages.isNotEmpty(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = CranberryPrimary),
                    modifier = Modifier.testTag("apply_rearrange_btn")
                ) {
                    if (isProcessing) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Apply", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Live preview of resulting page order
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = CranberryPale.copy(alpha = 0.5f)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Result Order: ${workingPages.mapIndexed { idx, p -> "${idx + 1}(P${p.originalPageIndex + 1}${if (p.rotationDegrees != 0f) " ${p.rotationDegrees.toInt()}°" else ""})" }.joinToString(" → ")}",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = CranberryPrimary,
                    maxLines = 1
                )
                Text(
                    text = "${workingPages.size} pages",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = CranberryPrimary
                )
            }
        }

        // Action controls for selected page
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            shape = RoundedCornerShape(14.dp),
            color = SurfaceWhite,
            shadowElevation = 1.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (workingPages.isNotEmpty()) "Selected: Page ${selectedIndex + 1} of ${workingPages.size}" else "0 Pages",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = InkPrimary
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Move Earlier / Up
                    IconButton(
                        onClick = {
                            if (selectedIndex > 0) {
                                val item = workingPages.removeAt(selectedIndex)
                                workingPages.add(selectedIndex - 1, item)
                                selectedIndex--
                            }
                        },
                        enabled = selectedIndex > 0,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Default.ArrowUpward, contentDescription = "Move Earlier", tint = CranberryPrimary)
                    }

                    // Move Later / Down
                    IconButton(
                        onClick = {
                            if (selectedIndex < workingPages.size - 1) {
                                val item = workingPages.removeAt(selectedIndex)
                                workingPages.add(selectedIndex + 1, item)
                                selectedIndex++
                            }
                        },
                        enabled = selectedIndex < workingPages.size - 1,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Default.ArrowDownward, contentDescription = "Move Later", tint = CranberryPrimary)
                    }

                    // Rotate 90°
                    IconButton(
                        onClick = {
                            if (workingPages.isNotEmpty() && selectedIndex in 0 until workingPages.size) {
                                val current = workingPages[selectedIndex]
                                val newRot = (current.rotationDegrees + 90f) % 360f
                                val updated = current.copy(rotationDegrees = newRot)
                                workingPages[selectedIndex] = updated

                                // Update thumbnail with rotation
                                docState?.sourceFile?.let { f ->
                                    val bmp = kotlinx.coroutines.runBlocking {
                                        PdfEngine.renderPageStateToBitmap(f, updated, targetWidth = 360)
                                    }
                                    if (bmp != null) pageThumbnails[updated.pageId] = bmp
                                }
                            }
                        },
                        enabled = workingPages.isNotEmpty(),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Default.RotateRight, contentDescription = "Rotate 90°", tint = CranberryPrimary)
                    }

                    // Duplicate
                    IconButton(
                        onClick = {
                            if (workingPages.isNotEmpty() && selectedIndex in 0 until workingPages.size) {
                                val current = workingPages[selectedIndex]
                                val dup = current.copy(pageId = UUID.randomUUID().toString())
                                workingPages.add(selectedIndex + 1, dup)
                                pageThumbnails[current.pageId]?.let {
                                    pageThumbnails[dup.pageId] = it
                                }
                            }
                        },
                        enabled = workingPages.isNotEmpty(),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Duplicate", tint = InkSecondary)
                    }

                    // Delete
                    IconButton(
                        onClick = {
                            if (workingPages.size > 1) {
                                workingPages.removeAt(selectedIndex)
                                if (selectedIndex >= workingPages.size) {
                                    selectedIndex = workingPages.size - 1
                                }
                            } else {
                                viewModel.setStatusMessage("A document must have at least 1 page.")
                            }
                        },
                        enabled = workingPages.size > 1,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete Page", tint = Color.Red)
                    }
                }
            }
        }

        // Page Grid
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(bottom = 32.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            itemsIndexed(workingPages) { index, pageState ->
                val isSelected = selectedIndex == index
                val bmp = pageThumbnails[pageState.pageId]

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { selectedIndex = index }
                        .border(
                            width = if (isSelected) 2.5.dp else 1.dp,
                            color = if (isSelected) CranberryPrimary else BorderLight,
                            shape = RoundedCornerShape(12.dp)
                        ),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceWhite)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(0.75f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFFAFAFA)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (bmp != null) {
                                Image(
                                    bitmap = bmp.asImageBitmap(),
                                    contentDescription = "Page ${index + 1}",
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = CranberryPrimary
                                )
                            }

                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopStart)
                                        .padding(6.dp)
                                        .size(24.dp)
                                        .background(CranberryPrimary, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Selected",
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = if (isSelected) CranberryPrimary else BorderLight,
                                modifier = Modifier.size(26.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = "${index + 1}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) Color.White else InkPrimary
                                    )
                                }
                            }
                            if (pageState.rotationDegrees != 0f) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "${pageState.rotationDegrees.toInt()}°",
                                    fontSize = 10.sp,
                                    color = CranberryPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
