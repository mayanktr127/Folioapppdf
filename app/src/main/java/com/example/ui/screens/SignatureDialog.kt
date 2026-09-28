package com.example.ui.screens

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Gesture
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.data.model.SignatureItem
import com.example.ui.theme.BorderLight
import com.example.ui.theme.CranberryPale
import com.example.ui.theme.CranberryPrimary
import com.example.ui.theme.InkPrimary
import com.example.ui.theme.InkSecondary
import com.example.ui.theme.SurfaceWhite
import java.io.File

@Composable
fun SignatureDialog(
    savedSignatures: List<SignatureItem>,
    onDismiss: () -> Unit,
    onSignatureCreated: (Bitmap) -> Unit,
    onSaveToLibrary: (Bitmap) -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) } // 0: Draw, 1: Saved
    var strokeColor by remember { mutableStateOf(Color.Black) }
    var strokeWidth by remember { mutableStateOf(6f) }
    var saveToLibraryChecked by remember { mutableStateOf(true) }

    val currentStroke = remember { mutableStateListOf<Offset>() }
    val completedStrokes = remember { mutableStateListOf<Pair<List<Offset>, Color>>() }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(vertical = 24.dp)
                .testTag("signature_dialog_card"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceWhite)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Signature",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = InkPrimary
                        )
                        Text(
                            text = "Draw your signature • Tools to help you sign",
                            fontSize = 12.sp,
                            color = InkSecondary
                        )
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = InkSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Tabs: Draw vs Saved
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(CranberryPale, RoundedCornerShape(12.dp))
                        .padding(3.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (selectedTab == 0) SurfaceWhite else Color.Transparent)
                            .clickable { selectedTab = 0 }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Draw Signature",
                            fontSize = 13.sp,
                            fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Medium,
                            color = if (selectedTab == 0) CranberryPrimary else InkSecondary
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (selectedTab == 1) SurfaceWhite else Color.Transparent)
                            .clickable { selectedTab = 1 }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Saved (${savedSignatures.size})",
                            fontSize = 13.sp,
                            fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Medium,
                            color = if (selectedTab == 1) CranberryPrimary else InkSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                if (selectedTab == 0) {
                    // Tool controls matching Image 3: color chips, clear
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            // Black
                            ColorChip(
                                color = Color.Black,
                                selected = strokeColor == Color.Black,
                                onClick = { strokeColor = Color.Black }
                            )
                            // Blue
                            ColorChip(
                                color = Color(0xFF2563EB),
                                selected = strokeColor == Color(0xFF2563EB),
                                onClick = { strokeColor = Color(0xFF2563EB) }
                            )
                            // Red
                            ColorChip(
                                color = CranberryPrimary,
                                selected = strokeColor == CranberryPrimary,
                                onClick = { strokeColor = CranberryPrimary }
                            )
                        }

                        IconButton(
                            onClick = {
                                currentStroke.clear()
                                completedStrokes.clear()
                            },
                            modifier = Modifier
                                .size(36.dp)
                                .background(BorderLight, CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear Canvas",
                                tint = InkPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Drawing Canvas matching Image 3
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFFFAFAFA))
                            .border(1.dp, BorderLight, RoundedCornerShape(14.dp))
                            .pointerInput(Unit) {
                                detectDragGestures(
                                    onDragStart = { offset ->
                                        currentStroke.add(offset)
                                    },
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        currentStroke.add(change.position)
                                    },
                                    onDragEnd = {
                                        if (currentStroke.isNotEmpty()) {
                                            completedStrokes.add(currentStroke.toList() to strokeColor)
                                            currentStroke.clear()
                                        }
                                    },
                                    onDragCancel = {
                                        currentStroke.clear()
                                    }
                                )
                            }
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            // Draw completed strokes
                            for ((points, col) in completedStrokes) {
                                if (points.size > 1) {
                                    val path = Path()
                                    path.moveTo(points[0].x, points[0].y)
                                    for (i in 1 until points.size) {
                                        path.lineTo(points[i].x, points[i].y)
                                    }
                                    drawPath(
                                        path = path,
                                        color = col,
                                        style = Stroke(
                                            width = strokeWidth,
                                            cap = StrokeCap.Round,
                                            join = StrokeJoin.Round
                                        )
                                    )
                                }
                            }
                            // Draw current active stroke
                            if (currentStroke.size > 1) {
                                val path = Path()
                                path.moveTo(currentStroke[0].x, currentStroke[0].y)
                                for (i in 1 until currentStroke.size) {
                                    path.lineTo(currentStroke[i].x, currentStroke[i].y)
                                }
                                drawPath(
                                    path = path,
                                    color = strokeColor,
                                    style = Stroke(
                                        width = strokeWidth,
                                        cap = StrokeCap.Round,
                                        join = StrokeJoin.Round
                                    )
                                )
                            }
                        }

                        if (completedStrokes.isEmpty() && currentStroke.isEmpty()) {
                            Text(
                                text = "Sign here with your finger",
                                fontSize = 13.sp,
                                color = InkSecondary.copy(alpha = 0.5f),
                                modifier = Modifier.align(Alignment.Center)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Save to reusable library checkbox
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = saveToLibraryChecked,
                            onCheckedChange = { saveToLibraryChecked = it }
                        )
                        Text(
                            text = "Save signature for future documents",
                            fontSize = 13.sp,
                            color = InkPrimary
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = {
                            val bmp = createSignatureBitmap(completedStrokes, 600, 300)
                            if (bmp != null) {
                                if (saveToLibraryChecked) {
                                    onSaveToLibrary(bmp)
                                }
                                onSignatureCreated(bmp)
                            }
                        },
                        enabled = completedStrokes.isNotEmpty(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("apply_signature_btn"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = CranberryPrimary)
                    ) {
                        Text("Apply Signature to Document", fontWeight = FontWeight.Bold)
                    }
                } else {
                    // Saved signatures list
                    if (savedSignatures.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No saved signatures yet. Draw one in the 'Draw Signature' tab!",
                                fontSize = 13.sp,
                                color = InkSecondary
                            )
                        }
                    } else {
                        LazyRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(savedSignatures) { sig ->
                                val file = File(sig.imagePath)
                                Card(
                                    modifier = Modifier
                                        .size(160.dp, 100.dp)
                                        .clickable {
                                            if (file.exists()) {
                                                val bmp = android.graphics.BitmapFactory.decodeFile(file.absolutePath)
                                                if (bmp != null) onSignatureCreated(bmp)
                                            }
                                        },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFAFAFA)),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderLight)
                                ) {
                                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        AsyncImage(
                                            model = file,
                                            contentDescription = "Signature",
                                            modifier = Modifier.padding(10.dp)
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

@Composable
private fun ColorChip(
    color: Color,
    selected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(color)
            .clickable(onClick = onClick)
            .border(
                width = if (selected) 3.dp else 1.dp,
                color = if (selected) CranberryPale else BorderLight,
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        if (selected) {
            Icon(
                imageVector = Icons.Default.Done,
                contentDescription = null,
                tint = if (color == Color.White) Color.Black else Color.White,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

private fun createSignatureBitmap(
    strokes: List<Pair<List<Offset>, Color>>,
    width: Int,
    height: Int
): Bitmap? {
    if (strokes.isEmpty()) return null
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        strokeWidth = 8f
    }

    for ((points, col) in strokes) {
        if (points.size > 1) {
            paint.color = col.toArgb()
            val path = android.graphics.Path()
            // Scale points to bitmap dimensions (assuming ~300x180 preview canvas)
            val scaleX = width / 320f
            val scaleY = height / 180f
            path.moveTo(points[0].x * scaleX, points[0].y * scaleY)
            for (i in 1 until points.size) {
                path.lineTo(points[i].x * scaleX, points[i].y * scaleY)
            }
            canvas.drawPath(path, paint)
        }
    }
    return bitmap
}
