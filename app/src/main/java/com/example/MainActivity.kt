package com.example

import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.components.FolioBottomBar
import com.example.ui.screens.ConvertScreen
import com.example.ui.screens.EditorScreen
import com.example.ui.screens.FilesScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.PageOrganizerScreen
import com.example.ui.screens.ProfileDrawer
import com.example.ui.screens.ScannerScreen
import com.example.ui.screens.ToolsScreen
import com.example.ui.theme.BorderLight
import com.example.ui.theme.CranberryPale
import com.example.ui.theme.CranberryPrimary
import com.example.ui.theme.InkPrimary
import com.example.ui.theme.InkSecondary
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.SurfaceWhite
import com.example.ui.viewmodel.AppScreen
import com.example.ui.viewmodel.PdfViewModel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                FolioPdfApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FolioPdfApp() {
    val context = LocalContext.current
    val viewModel: PdfViewModel = viewModel()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val currentScreen by viewModel.currentScreen.collectAsState()
    val statusMessage by viewModel.statusMessage.collectAsState()

    var showAddBottomSheet by remember { mutableStateOf(false) }
    var showProfileDrawer by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // System file picker for importing PDFs
    val openPdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            var fileName = "Imported_Document.pdf"
            try {
                val cursor: Cursor? = context.contentResolver.query(uri, null, null, null, null)
                cursor?.use {
                    if (it.moveToFirst()) {
                        val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (nameIndex != -1) {
                            fileName = it.getString(nameIndex) ?: fileName
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            viewModel.importPdfFromUri(uri, fileName)
        }
    }

    // React to status messages with Snackbar
    LaunchedEffect(statusMessage) {
        statusMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearStatusMessage()
        }
    }

    // System BackHandler
    BackHandler(enabled = currentScreen != AppScreen.HOME || showProfileDrawer || showAddBottomSheet) {
        when {
            showProfileDrawer -> showProfileDrawer = false
            showAddBottomSheet -> showAddBottomSheet = false
            else -> {
                if (!viewModel.navigateBack()) {
                    viewModel.navigateTo(AppScreen.HOME)
                }
            }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            // Only show global bottom navigation on main tab screens
            if (currentScreen in listOf(AppScreen.HOME, AppScreen.FILES, AppScreen.TOOLS, AppScreen.PROFILE)) {
                Box {
                    FolioBottomBar(
                        currentScreen = currentScreen,
                        onNavigate = { screen ->
                            if (screen == AppScreen.PROFILE) {
                                showProfileDrawer = true
                            } else {
                                viewModel.navigateTo(screen)
                            }
                        },
                        onAddClick = { showAddBottomSheet = true }
                    )

                    // Floating Action Button (+) matching Image 2
                    FloatingActionButton(
                        onClick = { showAddBottomSheet = true },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(end = 20.dp)
                            .size(54.dp)
                            .testTag("global_fab_add"),
                        shape = CircleShape,
                        containerColor = CranberryPrimary,
                        contentColor = Color.White,
                        elevation = FloatingActionButtonDefaults.elevation(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add Document",
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentScreen) {
                AppScreen.HOME -> {
                    HomeScreen(
                        viewModel = viewModel,
                        onOpenAddMenu = { showAddBottomSheet = true },
                        onOpenSettings = { showProfileDrawer = true }
                    )
                }
                AppScreen.FILES -> {
                    FilesScreen(
                        viewModel = viewModel,
                        onOpenSettings = { showProfileDrawer = true }
                    )
                }
                AppScreen.TOOLS -> {
                    ToolsScreen(
                        viewModel = viewModel,
                        onOpenSettings = { showProfileDrawer = true }
                    )
                }
                AppScreen.PROFILE -> {
                    ProfileDrawer(
                        viewModel = viewModel,
                        onClose = { viewModel.navigateTo(AppScreen.HOME) }
                    )
                }
                AppScreen.EDITOR -> {
                    EditorScreen(
                        viewModel = viewModel,
                        onBack = {
                            if (!viewModel.navigateBack()) {
                                viewModel.navigateTo(AppScreen.HOME)
                            }
                        }
                    )
                }
                AppScreen.PAGE_ORGANIZER -> {
                    PageOrganizerScreen(
                        viewModel = viewModel,
                        onBack = {
                            if (!viewModel.navigateBack()) {
                                viewModel.navigateTo(AppScreen.EDITOR)
                            }
                        }
                    )
                }
                AppScreen.CONVERT -> {
                    ConvertScreen(
                        viewModel = viewModel,
                        onBack = {
                            if (!viewModel.navigateBack()) {
                                viewModel.navigateTo(AppScreen.TOOLS)
                            }
                        }
                    )
                }
                AppScreen.SCANNER -> {
                    ScannerScreen(
                        viewModel = viewModel,
                        onBack = {
                            if (!viewModel.navigateBack()) {
                                viewModel.navigateTo(AppScreen.HOME)
                            }
                        }
                    )
                }
                else -> {
                    HomeScreen(
                        viewModel = viewModel,
                        onOpenAddMenu = { showAddBottomSheet = true },
                        onOpenSettings = { showProfileDrawer = true }
                    )
                }
            }

            // Side Drawer / Profile overlay matching Image 1
            AnimatedVisibility(
                visible = showProfileDrawer,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                ProfileDrawer(
                    viewModel = viewModel,
                    onClose = { showProfileDrawer = false }
                )
            }
        }

        // Add Document Bottom Sheet
        if (showAddBottomSheet) {
            ModalBottomSheet(
                onDismissRequest = { showAddBottomSheet = false },
                sheetState = sheetState,
                containerColor = SurfaceWhite
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 12.dp)
                ) {
                    Text(
                        text = "Add Document",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = InkPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Choose an option to create or import a PDF",
                        fontSize = 13.sp,
                        color = InkSecondary
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    AddActionItem(
                        icon = Icons.Default.FolderOpen,
                        title = "Import PDF from Device",
                        subtitle = "Select an existing PDF file from storage",
                        onClick = {
                            showAddBottomSheet = false
                            openPdfLauncher.launch(arrayOf("application/pdf"))
                        },
                        testTag = "add_action_import"
                    )

                    AddActionItem(
                        icon = Icons.Default.CameraAlt,
                        title = "Scan with Camera",
                        subtitle = "Scan physical paper documents and receipts",
                        onClick = {
                            showAddBottomSheet = false
                            viewModel.navigateTo(AppScreen.SCANNER)
                        },
                        testTag = "add_action_scan"
                    )

                    AddActionItem(
                        icon = Icons.Default.Image,
                        title = "Create from Images",
                        subtitle = "Convert gallery photos into a single PDF",
                        onClick = {
                            showAddBottomSheet = false
                            viewModel.navigateTo(AppScreen.CONVERT)
                        },
                        testTag = "add_action_images"
                    )

                    AddActionItem(
                        icon = Icons.Default.TextFields,
                        title = "New Formatted Note",
                        subtitle = "Write formatted text notes into a clean PDF",
                        onClick = {
                            showAddBottomSheet = false
                            viewModel.navigateTo(AppScreen.CONVERT)
                        },
                        testTag = "add_action_note"
                    )

                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}

@Composable
private fun AddActionItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    testTag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(CranberryPale),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = CranberryPrimary,
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column {
            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = InkPrimary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = InkSecondary
            )
        }
    }
}
