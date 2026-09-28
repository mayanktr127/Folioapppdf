package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Compress
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.MergeType
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.PdfDocumentItem
import com.example.ui.components.DocumentListItem
import com.example.ui.components.FolioTopHeader
import com.example.ui.theme.AccentAmber
import com.example.ui.theme.AccentBlue
import com.example.ui.theme.AccentPurple
import com.example.ui.theme.CanvasBackground
import com.example.ui.theme.CranberryPale
import com.example.ui.theme.CranberryPrimary
import com.example.ui.theme.InkPrimary
import com.example.ui.theme.InkSecondary
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.SurfaceWhite
import com.example.ui.viewmodel.AppScreen
import com.example.ui.viewmodel.PdfViewModel

@Composable
fun HomeScreen(
    viewModel: PdfViewModel,
    onOpenAddMenu: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val searchQuery by viewModel.searchQuery.collectAsState()
    val recentDocs by viewModel.recentDocuments.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CanvasBackground)
    ) {
        // Red Top Header
        FolioTopHeader(
            searchQuery = searchQuery,
            onSearchChange = { viewModel.setSearchQuery(it) },
            onSettingsClick = onOpenSettings,
            onNotificationClick = {
                viewModel.setStatusMessage("Folio PDF is running locally. All documents are private and on-device.")
            }
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            // Quick Tools Title
            item {
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = "PDF Tools",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = InkPrimary
                )
                Spacer(modifier = Modifier.height(14.dp))

                // Quick Action Cards matching Image 3 & 4
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    QuickToolCard(
                        title = "Convert",
                        icon = Icons.Default.SwapHoriz,
                        iconTint = Color.White,
                        iconBg = AccentBlue,
                        onClick = { viewModel.navigateTo(AppScreen.CONVERT) },
                        modifier = Modifier.weight(1f),
                        testTag = "quick_tool_convert"
                    )

                    QuickToolCard(
                        title = "Compress",
                        icon = Icons.Default.Compress,
                        iconTint = Color.White,
                        iconBg = SuccessGreen,
                        onClick = {
                            recentDocs.firstOrNull()?.let {
                                viewModel.compressDocument(it, com.example.engine.CompressionPreset.STANDARD)
                            } ?: viewModel.navigateTo(AppScreen.TOOLS)
                        },
                        modifier = Modifier.weight(1f),
                        testTag = "quick_tool_compress"
                    )

                    QuickToolCard(
                        title = "Scan",
                        icon = Icons.Default.CameraAlt,
                        iconTint = Color.White,
                        iconBg = AccentAmber,
                        onClick = { viewModel.navigateTo(AppScreen.SCANNER) },
                        modifier = Modifier.weight(1f),
                        testTag = "quick_tool_scan"
                    )

                    QuickToolCard(
                        title = "Merge",
                        icon = Icons.Default.MergeType,
                        iconTint = Color.White,
                        iconBg = AccentPurple,
                        onClick = { viewModel.navigateTo(AppScreen.TOOLS) },
                        modifier = Modifier.weight(1f),
                        testTag = "quick_tool_merge"
                    )
                }
            }

            // Recent Files Section Header
            item {
                Spacer(modifier = Modifier.height(24.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Recent Files",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = InkPrimary
                    )

                    Text(
                        text = "View all",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = CranberryPrimary,
                        modifier = Modifier
                            .clickable { viewModel.navigateTo(AppScreen.FILES) }
                            .padding(4.dp)
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Empty state matching Image 3
            if (recentDocs.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 20.dp),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = SurfaceWhite)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 36.dp, horizontal = 20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .background(CranberryPale, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Description,
                                    contentDescription = null,
                                    tint = CranberryPrimary,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Upload your first file",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = InkPrimary
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Your PDF documents will appear here",
                                fontSize = 14.sp,
                                color = InkSecondary
                            )
                            Spacer(modifier = Modifier.height(20.dp))
                            Button(
                                onClick = onOpenAddMenu,
                                shape = RoundedCornerShape(24.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = CranberryPrimary),
                                modifier = Modifier.testTag("add_first_document_btn")
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Add document", fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            } else {
                items(recentDocs, key = { it.id }) { doc ->
                    DocumentListItem(
                        document = doc,
                        onClick = { viewModel.openDocument(doc) },
                        onToggleFavorite = { viewModel.toggleFavorite(doc) },
                        onDelete = { viewModel.deleteDocument(doc) },
                        onCompress = {
                            viewModel.compressDocument(doc, com.example.engine.CompressionPreset.STANDARD)
                        },
                        onRearrange = {
                            viewModel.openDocument(doc)
                            viewModel.navigateTo(AppScreen.PAGE_ORGANIZER)
                        },
                        onShare = { viewModel.shareDocument(context, doc) },
                        onPrint = { viewModel.printDocument(context, doc) },
                        modifier = Modifier.padding(vertical = 5.dp)
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = onOpenAddMenu,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("home_add_doc_banner_btn"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = CranberryPrimary)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Add document",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(100.dp)) // padding for bottom bar
                }
            }
        }
    }
}

@Composable
private fun QuickToolCard(
    title: String,
    icon: ImageVector,
    iconTint: Color,
    iconBg: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    testTag: String
) {
    Card(
        modifier = modifier
            .clickable(onClick = onClick)
            .testTag(testTag),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .background(iconBg, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = iconTint,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = InkPrimary
            )
        }
    }
}
