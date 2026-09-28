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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Done
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
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
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
import coil.compose.AsyncImage
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

@Composable
fun PageOrganizerScreen(
    viewModel: PdfViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }

    val activeDoc by viewModel.activeDocument.collectAsState()
    val isProcessing by viewModel.isProcessing.collectAsState()

    // Working page order (indices)
    val pageOrder = remember { mutableStateListOf<Int>() }
    var selectedIndex by remember { mutableStateOf(0) }
    val pageThumbnails = remember { mutableStateMapOf<Int, Bitmap>() }

    LaunchedEffect(activeDoc) {
        val doc = activeDoc ?: return@LaunchedEffect
        pageOrder.clear()
        for (i in 0 until doc.pageCount) {
            pageOrder.add(i)
        }
        val file = File(doc.filePath)
        if (file.exists()) {
            for (i in 0 until doc.pageCount) {
                val bmp = PdfEngine.renderPageToBitmap(file, i, targetWidth = 360)
                if (bmp != null) pageThumbnails[i] = bmp
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CanvasBackground)
    ) {
        // Header matching Image 3
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
                            text = "Just drag or step them into place",
                            fontSize = 12.sp,
                            color = InkSecondary
                        )
                    }
                }

                Button(
                    onClick = {
                        activeDoc?.let { doc ->
                            viewModel.rearrangeDocument(doc, pageOrder.toList())
                        }
                    },
                    enabled = !isProcessing && pageOrder.isNotEmpty(),
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
                    text = "Page ${selectedIndex + 1} of ${pageOrder.size}",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = InkPrimary
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Move Left / Up
                    IconButton(
                        onClick = {
                            if (selectedIndex > 0) {
                                val item = pageOrder.removeAt(selectedIndex)
                                pageOrder.add(selectedIndex - 1, item)
                                selectedIndex--
                            }
                        },
                        enabled = selectedIndex > 0,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Default.ArrowUpward, contentDescription = "Move Earlier", tint = CranberryPrimary)
                    }

                    // Move Right / Down
                    IconButton(
                        onClick = {
                            if (selectedIndex < pageOrder.size - 1) {
                                val item = pageOrder.removeAt(selectedIndex)
                                pageOrder.add(selectedIndex + 1, item)
                                selectedIndex++
                            }
                        },
                        enabled = selectedIndex < pageOrder.size - 1,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Default.ArrowDownward, contentDescription = "Move Later", tint = CranberryPrimary)
                    }

                    // Duplicate
                    IconButton(
                        onClick = {
                            if (pageOrder.isNotEmpty()) {
                                val current = pageOrder[selectedIndex]
                                pageOrder.add(selectedIndex + 1, current)
                            }
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Duplicate", tint = InkSecondary)
                    }

                    // Delete
                    IconButton(
                        onClick = {
                            if (pageOrder.size > 1) {
                                pageOrder.removeAt(selectedIndex)
                                if (selectedIndex >= pageOrder.size) selectedIndex = pageOrder.size - 1
                            } else {
                                viewModel.setStatusMessage("A document must have at least 1 page.")
                            }
                        },
                        enabled = pageOrder.size > 1,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete Page", tint = Color.Red)
                    }
                }
            }
        }

        // Thumbnails Grid matching Image 3
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(bottom = 32.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            itemsIndexed(pageOrder) { index, originalPageIndex ->
                val isSelected = selectedIndex == index
                val bmp = pageThumbnails[originalPageIndex]

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { selectedIndex = index }
                        .border(
                            width = if (isSelected) 2.5.dp else 1.dp,
                            color = if (isSelected) CranberryPrimary else BorderLight,
                            shape = RoundedCornerShape(14.dp)
                        ),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceWhite)
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
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

                            // Selection checkmark matching Image 3
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

                        // Page pill badge matching Image 3 (e.g. "1")
                        Surface(
                            shape = CircleShape,
                            color = if (isSelected) CranberryPrimary else BorderLight,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "${index + 1}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color.White else InkPrimary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
