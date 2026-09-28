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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.DocumentGridItem
import com.example.ui.components.DocumentListItem
import com.example.ui.components.FolioTopHeader
import com.example.ui.theme.CanvasBackground
import com.example.ui.theme.CranberryPale
import com.example.ui.theme.CranberryPrimary
import com.example.ui.theme.InkPrimary
import com.example.ui.theme.InkSecondary
import com.example.ui.viewmodel.AppScreen
import com.example.ui.viewmodel.PdfViewModel

@Composable
fun FilesScreen(
    viewModel: PdfViewModel,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedFilter by viewModel.selectedFilter.collectAsState()
    val isGridView by viewModel.isGridView.collectAsState()
    val documents by viewModel.filteredDocuments.collectAsState()

    val filterOptions = listOf("All", "Recent", "Like", "Contracts", "Invoices", "Reports")

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
                viewModel.setStatusMessage("Folio local documents are active.")
            }
        )

        // Filter Chips Row
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp, bottom = 4.dp),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(filterOptions) { filter ->
                val selected = selectedFilter == filter
                FilterChip(
                    selected = selected,
                    onClick = { viewModel.setSelectedFilter(filter) },
                    label = {
                        Text(
                            text = if (filter == "Like") "Favorites" else filter,
                            fontSize = 13.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    shape = RoundedCornerShape(20.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = CranberryPrimary,
                        selectedLabelColor = Color.White,
                        containerColor = Color.White,
                        labelColor = InkPrimary
                    ),
                    border = null,
                    modifier = Modifier.testTag("filter_chip_$filter")
                )
            }
        }

        // Sub-header with document count and view switcher toggle
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${documents.size} Document${if (documents.size != 1) "s" else ""}",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = InkSecondary
            )

            IconButton(
                onClick = { viewModel.toggleGridView() },
                modifier = Modifier.size(36.dp).testTag("view_toggle_btn")
            ) {
                Icon(
                    imageVector = if (isGridView) Icons.Default.ViewList else Icons.Default.GridView,
                    contentDescription = "Toggle Grid/List View",
                    tint = CranberryPrimary,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        // List or Grid presentation matching Image 2
        if (documents.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "No documents found",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = InkPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Try another search or import a PDF file",
                        fontSize = 13.sp,
                        color = InkSecondary
                    )
                }
            }
        } else if (isGridView) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp),
                contentPadding = PaddingValues(bottom = 100.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(documents, key = { it.id }) { doc ->
                    DocumentGridItem(
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
                        onPrint = { viewModel.printDocument(context, doc) }
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(bottom = 100.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(documents, key = { it.id }) { doc ->
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
                        onPrint = { viewModel.printDocument(context, doc) }
                    )
                }
            }
        }
    }
}
