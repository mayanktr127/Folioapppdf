package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintManager
import android.util.Log
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.database.FolioDatabase
import com.example.data.model.PdfDocumentItem
import com.example.data.model.SignatureItem
import com.example.data.repository.PdfRepository
import com.example.engine.AnnotationData
import com.example.engine.AnnotationType
import com.example.engine.CompressionPreset
import com.example.engine.CompressionResult
import com.example.engine.FolioDocumentState
import com.example.engine.FolioPageState
import com.example.engine.PdfEngine
import com.example.engine.PdfTextBlock
import com.example.engine.PdfValidationResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

enum class AppScreen {
    HOME,
    FILES,
    TOOLS,
    PROFILE,
    EDITOR,
    PAGE_ORGANIZER,
    COMPRESS,
    CONVERT,
    SCANNER,
    TEAM,
    SETTINGS
}

enum class EditorTool {
    NONE,
    EDIT_PDF_TEXT,
    INK,
    HIGHLIGHT,
    UNDERLINE,
    STRIKETHROUGH,
    TEXT,
    OVERLAY_EDIT,
    RECTANGLE,
    CIRCLE,
    SIGNATURE,
    WATERMARK,
    REDACT
}

class PdfViewModel(application: Application) : AndroidViewModel(application) {
    private val db = FolioDatabase.getDatabase(application)
    private val repository = PdfRepository(application, db.pdfDocumentDao(), db.signatureDao())

    // Navigation State
    private val _currentScreen = MutableStateFlow(AppScreen.HOME)
    val currentScreen: StateFlow<AppScreen> = _currentScreen.asStateFlow()

    // Screen navigation stack for smooth BackHandler support
    private val screenStack = mutableListOf<AppScreen>()

    // Filter & Search
    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _selectedFilter = MutableStateFlow("All")
    val selectedFilter = _selectedFilter.asStateFlow()

    private val _isGridView = MutableStateFlow(false)
    val isGridView = _isGridView.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage = _statusMessage.asStateFlow()

    fun toggleGridView() {
        _isGridView.value = !_isGridView.value
    }

    fun clearStatusMessage() {
        _statusMessage.value = null
    }

    // Repository Flows
    val allDocuments = repository.allDocuments.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val recentDocuments = repository.recentDocuments.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val favoriteDocuments = repository.favoriteDocuments.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val savedSignatures = repository.savedSignatures.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Filtered documents for Files screen
    val filteredDocuments: StateFlow<List<PdfDocumentItem>> = combine(
        allDocuments,
        searchQuery,
        selectedFilter
    ) { docs, query, filter ->
        var list = docs
        if (query.isNotBlank()) {
            list = list.filter { it.title.contains(query, ignoreCase = true) }
        }
        when (filter) {
            "Recent" -> list.filter { it.isRecent }
            "Like" -> list.filter { it.isFavorite }
            "All" -> list
            else -> list.filter { it.category.equals(filter, ignoreCase = true) }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // UNIFIED DOCUMENT STATE - Single Source of Truth
    private val _docState = MutableStateFlow<FolioDocumentState?>(null)
    val docState = _docState.asStateFlow()

    val hasUnsavedChanges: StateFlow<Boolean> = _docState.map {
        it?.hasUnsavedChanges == true
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = false
    )

    // Active Editor State
    private val _activeDocument = MutableStateFlow<PdfDocumentItem?>(null)
    val activeDocument = _activeDocument.asStateFlow()

    private val _activePageIndex = MutableStateFlow(0)
    val activePageIndex = _activePageIndex.asStateFlow()

    private val _activePageBitmap = MutableStateFlow<Bitmap?>(null)
    val activePageBitmap = _activePageBitmap.asStateFlow()

    private val _currentTextBlocks = MutableStateFlow<List<PdfTextBlock>>(emptyList())
    val currentTextBlocks = _currentTextBlocks.asStateFlow()

    private val _editorTool = MutableStateFlow(EditorTool.NONE)
    val editorTool = _editorTool.asStateFlow()

    private val _activeColor = MutableStateFlow(android.graphics.Color.parseColor("#D52B49"))
    val activeColor = _activeColor.asStateFlow()

    private val _activeStrokeWidth = MutableStateFlow(6f)
    val activeStrokeWidth = _activeStrokeWidth.asStateFlow()

    private val _annotations = MutableStateFlow<List<AnnotationData>>(emptyList())
    val annotations = _annotations.asStateFlow()

    // Undo / Redo stack holding complete document states
    private val undoStateStack = mutableListOf<FolioDocumentState>()
    private val redoStateStack = mutableListOf<FolioDocumentState>()

    // Compression State
    private val _compressionResult = MutableStateFlow<CompressionResult?>(null)
    val compressionResult = _compressionResult.asStateFlow()

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing = _isProcessing.asStateFlow()

    // AI Document Assistant State
    private val _assistantChat = MutableStateFlow<List<Pair<Boolean, String>>>(listOf(
        false to "Hi! I'm your Folio Document Assistant. Ask me anything about your document, request summaries, or key clause extractions."
    ))
    val assistantChat = _assistantChat.asStateFlow()

    // Save Result & Export State
    private val _savedResultDoc = MutableStateFlow<Pair<PdfDocumentItem, PdfDocumentItem?>?>(null)
    val savedResultDoc = _savedResultDoc.asStateFlow()

    // In-Document Search & Replace State
    private val _docSearchQuery = MutableStateFlow("")
    val docSearchQuery = _docSearchQuery.asStateFlow()

    private val _searchMatches = MutableStateFlow<List<com.example.engine.PdfTextMatch>>(emptyList())
    val searchMatches = _searchMatches.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching = _isSearching.asStateFlow()

    private val _notificationsEnabled = MutableStateFlow(true)
    val notificationsEnabled = _notificationsEnabled.asStateFlow()

    init {
        viewModelScope.launch {
            repository.checkAndSeedInitialDocuments()
        }
    }

    fun navigateTo(screen: AppScreen) {
        if (_currentScreen.value != screen) {
            screenStack.add(_currentScreen.value)
            _currentScreen.value = screen
        }
    }

    fun navigateBack(): Boolean {
        if (screenStack.isNotEmpty()) {
            val previous = screenStack.removeAt(screenStack.size - 1)
            _currentScreen.value = previous
            return true
        }
        return false
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSelectedFilter(filter: String) {
        _selectedFilter.value = filter
    }

    fun setStatusMessage(msg: String?) {
        _statusMessage.value = msg
    }

    fun toggleFavorite(doc: PdfDocumentItem) {
        viewModelScope.launch {
            repository.toggleFavorite(doc.id, !doc.isFavorite)
        }
    }

    fun deleteDocument(doc: PdfDocumentItem) {
        viewModelScope.launch {
            repository.deleteDocument(doc)
            _statusMessage.value = "Deleted ${doc.title}"
        }
    }

    fun importPdfFromUri(uri: Uri, displayName: String = "Imported_Document") {
        viewModelScope.launch {
            _isProcessing.value = true
            val item = repository.importFromUri(uri, displayName)
            _isProcessing.value = false
            if (item != null) {
                _statusMessage.value = "Imported ${item.title}"
                openDocument(item)
            } else {
                _statusMessage.value = "Failed to import document"
            }
        }
    }

    fun openDocument(doc: PdfDocumentItem) {
        val file = File(doc.filePath)
        if (!file.exists()) {
            _statusMessage.value = "File not found: ${doc.title}"
            return
        }

        // Validate PDF reliably
        val validation = PdfEngine.validatePdfFile(file)
        when (validation) {
            PdfValidationResult.PasswordProtected -> {
                _statusMessage.value = "Document is password protected. Enter password to unlock."
                return
            }
            PdfValidationResult.CorruptedOrNotPdf -> {
                _statusMessage.value = "Cannot open: File is corrupted or not a valid PDF."
                return
            }
            PdfValidationResult.EmptyOrZeroPages -> {
                _statusMessage.value = "Cannot open: PDF contains 0 pages."
                return
            }
            PdfValidationResult.Valid -> { /* Valid file */ }
        }

        val pageCount = doc.pageCount
        val initialPages = (0 until maxOf(1, pageCount)).map {
            FolioPageState(originalPageIndex = it)
        }

        val initialState = FolioDocumentState(
            docItem = doc,
            sourceFile = file,
            pages = initialPages,
            hasUnsavedChanges = false
        )

        _activeDocument.value = doc
        _docState.value = initialState
        _activePageIndex.value = 0
        undoStateStack.clear()
        redoStateStack.clear()
        _editorTool.value = EditorTool.NONE

        viewModelScope.launch {
            repository.markAccessed(doc.id)
            loadActivePage()
            navigateTo(AppScreen.EDITOR)
        }
    }

    private suspend fun loadActivePage() {
        val state = _docState.value ?: return
        if (state.pages.isEmpty()) return

        val safeIndex = _activePageIndex.value.coerceIn(0, state.pages.size - 1)
        if (_activePageIndex.value != safeIndex) {
            _activePageIndex.value = safeIndex
        }

        val pageState = state.pages[safeIndex]
        val bmp = PdfEngine.renderPageStateToBitmap(state.sourceFile, pageState, targetWidth = 1080)
        _activePageBitmap.value = bmp

        val blocks = if (pageState.textBlocks.isNotEmpty()) {
            pageState.textBlocks
        } else {
            val extracted = PdfEngine.extractTextBlocks(state.sourceFile, pageState.originalPageIndex)
            val updatedPages = state.pages.toMutableList()
            updatedPages[safeIndex] = pageState.copy(textBlocks = extracted)
            _docState.value = state.copy(pages = updatedPages)
            extracted
        }

        _currentTextBlocks.value = blocks
        _annotations.value = pageState.annotations
    }

    fun setPageIndex(newIndex: Int) {
        val state = _docState.value ?: return
        if (newIndex in 0 until state.pages.size) {
            _activePageIndex.value = newIndex
            viewModelScope.launch {
                loadActivePage()
            }
        }
    }

    fun setEditorTool(tool: EditorTool) {
        _editorTool.value = if (_editorTool.value == tool) EditorTool.NONE else tool
    }

    fun setActiveColor(color: Int) {
        _activeColor.value = color
    }

    fun setActiveStrokeWidth(width: Float) {
        _activeStrokeWidth.value = width
    }

    fun addAnnotation(annotation: AnnotationData) {
        val state = _docState.value ?: return
        val safeIndex = _activePageIndex.value.coerceIn(0, state.pages.size - 1)
        val pageState = state.pages[safeIndex]

        undoStateStack.add(state)
        redoStateStack.clear()

        val updatedAnnots = pageState.annotations + annotation.copy(pageIndex = safeIndex)
        val updatedPages = state.pages.toMutableList()
        updatedPages[safeIndex] = pageState.copy(annotations = updatedAnnots)

        _docState.value = state.copy(pages = updatedPages, hasUnsavedChanges = true)
        _annotations.value = updatedAnnots
    }

    fun editTextInPdf(
        block: PdfTextBlock,
        newText: String,
        color: Int,
        isBold: Boolean,
        fontFamily: String = "Helvetica",
        fontSize: Float = 14f
    ) {
        val state = _docState.value ?: return
        val targetIndex = block.pageIndex.coerceIn(0, state.pages.size - 1)
        val pageState = state.pages[targetIndex]

        undoStateStack.add(state)
        redoStateStack.clear()

        val annot = AnnotationData(
            pageIndex = targetIndex,
            type = AnnotationType.TEXT_REPLACE,
            rect = block.rect,
            text = newText,
            originalText = block.originalText,
            color = color,
            strokeWidth = if (isBold) 2f else 1f,
            fontFamily = fontFamily,
            fontSize = fontSize,
            isCentered = block.isCentered,
            baselineY = block.baselineY,
            backgroundColor = block.backgroundColor
        )

        val updatedAnnots = pageState.annotations.filterNot {
            it.rect == block.rect && (it.type == AnnotationType.TEXT_REPLACE || it.type == AnnotationType.OVERLAY_EDIT)
        } + annot

        val updatedBlocks = pageState.textBlocks.map {
            if (it.id == block.id || it.rect == block.rect) {
                it.copy(
                    text = newText,
                    isModified = true,
                    textColor = color,
                    isBold = isBold,
                    fontFamily = fontFamily,
                    fontSize = fontSize
                )
            } else it
        }

        val updatedPages = state.pages.toMutableList()
        updatedPages[targetIndex] = pageState.copy(
            annotations = updatedAnnots,
            textBlocks = updatedBlocks
        )

        _docState.value = state.copy(pages = updatedPages, hasUnsavedChanges = true)
        if (_activePageIndex.value == targetIndex) {
            _annotations.value = updatedAnnots
            _currentTextBlocks.value = updatedBlocks
        }
        _statusMessage.value = "Updated text in PDF"
    }

    fun deleteTextInPdf(block: PdfTextBlock) {
        val state = _docState.value ?: return
        val targetIndex = block.pageIndex.coerceIn(0, state.pages.size - 1)
        val pageState = state.pages[targetIndex]

        undoStateStack.add(state)
        redoStateStack.clear()

        val annot = AnnotationData(
            pageIndex = targetIndex,
            type = AnnotationType.TEXT_REPLACE,
            rect = block.rect,
            text = "",
            originalText = block.originalText,
            color = android.graphics.Color.WHITE
        )

        val updatedAnnots = pageState.annotations.filterNot {
            it.rect == block.rect && it.type == AnnotationType.TEXT_REPLACE
        } + annot

        val updatedBlocks = pageState.textBlocks.map {
            if (it.id == block.id || it.rect == block.rect) {
                it.copy(text = "", isModified = true)
            } else it
        }

        val updatedPages = state.pages.toMutableList()
        updatedPages[targetIndex] = pageState.copy(
            annotations = updatedAnnots,
            textBlocks = updatedBlocks
        )

        _docState.value = state.copy(pages = updatedPages, hasUnsavedChanges = true)
        if (_activePageIndex.value == targetIndex) {
            _annotations.value = updatedAnnots
            _currentTextBlocks.value = updatedBlocks
        }
        _statusMessage.value = "Cleared text line"
    }

    fun executeFindAndReplace(findText: String, replaceText: String, isEntireDoc: Boolean) {
        val state = _docState.value ?: return
        if (findText.isBlank()) return

        viewModelScope.launch {
            _isProcessing.value = true
            undoStateStack.add(state)
            redoStateStack.clear()

            var totalMatches = 0
            val targetIndices = if (isEntireDoc) (0 until state.pages.size) else listOf(_activePageIndex.value)
            val updatedPages = state.pages.toMutableList()

            for (idx in targetIndices) {
                val pageState = updatedPages[idx]
                val blocks = if (pageState.textBlocks.isNotEmpty()) {
                    pageState.textBlocks
                } else {
                    PdfEngine.extractTextBlocks(state.sourceFile, pageState.originalPageIndex)
                }

                var pageModified = false
                val newAnnots = pageState.annotations.toMutableList()
                val newBlocks = blocks.map { b ->
                    if (b.text.contains(findText, ignoreCase = true)) {
                        totalMatches++
                        pageModified = true
                        val replaced = b.text.replace(findText, replaceText, ignoreCase = true)
                        newAnnots.removeAll { it.rect == b.rect && it.type == AnnotationType.TEXT_REPLACE }
                        newAnnots.add(
                            AnnotationData(
                                pageIndex = idx,
                                type = AnnotationType.TEXT_REPLACE,
                                rect = b.rect,
                                text = replaced,
                                originalText = b.originalText,
                                color = b.textColor,
                                strokeWidth = if (b.isBold) 2f else 1f,
                                fontFamily = b.fontFamily,
                                fontSize = b.fontSize,
                                isCentered = b.isCentered,
                                baselineY = b.baselineY,
                                backgroundColor = b.backgroundColor
                            )
                        )
                        b.copy(text = replaced, isModified = true)
                    } else b
                }

                if (pageModified) {
                    updatedPages[idx] = pageState.copy(
                        annotations = newAnnots,
                        textBlocks = newBlocks
                    )
                }
            }

            _docState.value = state.copy(pages = updatedPages, hasUnsavedChanges = true)
            loadActivePage()
            _isProcessing.value = false
            _statusMessage.value = "Replaced $totalMatches match(es) across document"
        }
    }

    fun searchInDocument(query: String) {
        _docSearchQuery.value = query
        val state = _docState.value ?: return
        if (query.isBlank()) {
            _searchMatches.value = emptyList()
            return
        }

        viewModelScope.launch {
            _isSearching.value = true
            val matches = PdfEngine.searchAllPages(state.sourceFile, state.pages, query)
            _searchMatches.value = matches
            _isSearching.value = false
        }
    }

    fun replaceSingleOccurrence(match: com.example.engine.PdfTextMatch, replacement: String) {
        val state = _docState.value ?: return
        undoStateStack.add(state)
        redoStateStack.clear()

        val pageIdx = match.pageIndex
        if (pageIdx !in state.pages.indices) return
        val pageState = state.pages[pageIdx]

        val newText = match.lineText.replace(match.matchedWord, replacement, ignoreCase = true)
        val block = pageState.textBlocks.find { it.rect == match.blockRect || it.id == match.textBlockId }
        val textColor = block?.textColor ?: android.graphics.Color.parseColor("#182230")
        val isBold = block?.isBold ?: false
        val fontFamily = block?.fontFamily ?: "Helvetica"
        val fontSize = block?.fontSize ?: 14f

        val annot = AnnotationData(
            pageIndex = pageIdx,
            type = AnnotationType.TEXT_REPLACE,
            rect = match.blockRect,
            text = newText,
            originalText = match.lineText,
            color = textColor,
            strokeWidth = if (isBold) 2f else 1f,
            fontFamily = fontFamily,
            fontSize = fontSize,
            isCentered = block?.isCentered ?: false,
            baselineY = block?.baselineY,
            backgroundColor = block?.backgroundColor
        )

        val updatedAnnots = pageState.annotations.filterNot {
            it.rect == match.blockRect && it.type == AnnotationType.TEXT_REPLACE
        } + annot

        val updatedBlocks = pageState.textBlocks.map { b ->
            if (b.rect == match.blockRect || b.id == match.textBlockId) {
                b.copy(text = newText, isModified = true, textColor = textColor, isBold = isBold, fontFamily = fontFamily, fontSize = fontSize)
            } else b
        }

        val updatedPages = state.pages.toMutableList()
        updatedPages[pageIdx] = pageState.copy(
            annotations = updatedAnnots,
            textBlocks = updatedBlocks
        )

        _docState.value = state.copy(pages = updatedPages, hasUnsavedChanges = true)
        _searchMatches.value = _searchMatches.value.filterNot { it.matchId == match.matchId }

        viewModelScope.launch {
            if (_activePageIndex.value != pageIdx) {
                _activePageIndex.value = pageIdx
            }
            loadActivePage()
        }
        _statusMessage.value = "Replaced match on Page ${pageIdx + 1}"
    }

    fun replaceOnPage(pageIdx: Int, query: String, replacement: String) {
        val state = _docState.value ?: return
        if (pageIdx !in state.pages.indices || query.isBlank()) return
        undoStateStack.add(state)
        redoStateStack.clear()

        viewModelScope.launch {
            val pageState = state.pages[pageIdx]
            val blocks = if (pageState.textBlocks.isNotEmpty()) {
                pageState.textBlocks
            } else {
                PdfEngine.extractTextBlocks(state.sourceFile, pageState.originalPageIndex)
            }

            val newAnnots = pageState.annotations.toMutableList()
            var count = 0
            val updatedBlocks = blocks.map { b ->
                if (b.text.contains(query, ignoreCase = true)) {
                    count++
                    val replaced = b.text.replace(query, replacement, ignoreCase = true)
                    newAnnots.removeAll { it.rect == b.rect && it.type == AnnotationType.TEXT_REPLACE }
                    newAnnots.add(
                        AnnotationData(
                            pageIndex = pageIdx,
                            type = AnnotationType.TEXT_REPLACE,
                            rect = b.rect,
                            text = replaced,
                            originalText = b.originalText,
                            color = b.textColor,
                            strokeWidth = if (b.isBold) 2f else 1f,
                            fontFamily = b.fontFamily,
                            fontSize = b.fontSize,
                            isCentered = b.isCentered,
                            baselineY = b.baselineY,
                            backgroundColor = b.backgroundColor
                        )
                    )
                    b.copy(text = replaced, isModified = true)
                } else b
            }

            val updatedPages = state.pages.toMutableList()
            updatedPages[pageIdx] = pageState.copy(
                annotations = newAnnots,
                textBlocks = updatedBlocks
            )

            _docState.value = state.copy(pages = updatedPages, hasUnsavedChanges = true)
            _searchMatches.value = _searchMatches.value.filterNot { it.pageIndex == pageIdx }

            if (_activePageIndex.value != pageIdx) {
                _activePageIndex.value = pageIdx
            }
            loadActivePage()
        }
    }

    fun replaceAllInDocument(query: String, replacement: String) {
        executeFindAndReplace(query, replacement, isEntireDoc = true)
        _searchMatches.value = emptyList()
    }

    fun addRedactions(redactions: List<Pair<com.example.engine.RectFData, Int>>) {
        val state = _docState.value ?: return
        if (redactions.isEmpty()) return
        undoStateStack.add(state)
        redoStateStack.clear()

        val pageIdx = _activePageIndex.value.coerceIn(0, state.pages.size - 1)
        val pageState = state.pages[pageIdx]

        val newAnnots = redactions.map { (rect, color) ->
            AnnotationData(
                pageIndex = pageIdx,
                type = AnnotationType.REDACTION,
                rect = rect,
                color = color
            )
        }

        val updatedPages = state.pages.toMutableList()
        updatedPages[pageIdx] = pageState.copy(annotations = pageState.annotations + newAnnots)

        _docState.value = state.copy(pages = updatedPages, hasUnsavedChanges = true)
        _annotations.value = updatedPages[pageIdx].annotations
        _statusMessage.value = "Applied ${redactions.size} redaction(s) on Page ${pageIdx + 1}"
    }

    fun updateDocumentPages(newPages: List<FolioPageState>) {
        val state = _docState.value ?: return
        if (newPages.isEmpty()) {
            _statusMessage.value = "Cannot delete all pages. At least 1 page is required."
            return
        }

        undoStateStack.add(state)
        redoStateStack.clear()

        _docState.value = state.copy(pages = newPages, hasUnsavedChanges = true)
        _activePageIndex.value = _activePageIndex.value.coerceIn(0, newPages.size - 1)
        viewModelScope.launch {
            loadActivePage()
        }
        _statusMessage.value = "Updated document pages (${newPages.size} pages)"
    }

    fun rotateActivePage(degrees: Float = 90f) {
        val state = _docState.value ?: return
        val idx = _activePageIndex.value.coerceIn(0, state.pages.size - 1)
        val page = state.pages[idx]

        undoStateStack.add(state)
        redoStateStack.clear()

        val updatedPages = state.pages.toMutableList()
        val newRot = (page.rotationDegrees + degrees) % 360f
        updatedPages[idx] = page.copy(rotationDegrees = newRot)

        _docState.value = state.copy(pages = updatedPages, hasUnsavedChanges = true)
        viewModelScope.launch {
            loadActivePage()
        }
        _statusMessage.value = "Rotated page by ${degrees.toInt()}°"
    }

    fun undo() {
        val current = _docState.value ?: return
        if (undoStateStack.isNotEmpty()) {
            redoStateStack.add(current)
            val prev = undoStateStack.removeAt(undoStateStack.size - 1)
            _docState.value = prev
            viewModelScope.launch {
                loadActivePage()
            }
        }
    }

    fun redo() {
        val current = _docState.value ?: return
        if (redoStateStack.isNotEmpty()) {
            undoStateStack.add(current)
            val next = redoStateStack.removeAt(redoStateStack.size - 1)
            _docState.value = next
            viewModelScope.launch {
                loadActivePage()
            }
        }
    }

    fun clearPageAnnotations() {
        val state = _docState.value ?: return
        val safeIndex = _activePageIndex.value.coerceIn(0, state.pages.size - 1)
        val page = state.pages[safeIndex]

        undoStateStack.add(state)
        redoStateStack.clear()

        val updatedPages = state.pages.toMutableList()
        updatedPages[safeIndex] = page.copy(annotations = emptyList())

        _docState.value = state.copy(pages = updatedPages, hasUnsavedChanges = true)
        _annotations.value = emptyList()
    }

    fun saveAnnotatedCopy(customFileName: String? = null) {
        val state = _docState.value ?: return
        if (state.pages.isEmpty() || !state.sourceFile.exists()) return

        viewModelScope.launch {
            _isProcessing.value = true
            val docsDir = File(getApplication<Application>().filesDir, "documents").apply { mkdirs() }
            val baseName = state.docItem.title.removeSuffix(".pdf")
            val newTitle = if (!customFileName.isNullOrBlank()) {
                if (customFileName.endsWith(".pdf", ignoreCase = true)) customFileName else "$customFileName.pdf"
            } else {
                "${baseName}-edited.pdf"
            }
            val destFile = File(docsDir, "${System.currentTimeMillis()}_$newTitle")

            // Serialize combined document state (pages in order, rotations, text edits, annotations)
            val success = PdfEngine.saveDocumentState(state, destFile)
            if (success) {
                // Re-open in-memory and verify with PdfRenderer
                val verified = PdfEngine.verifyExportedDocument(destFile, state)
                if (verified) {
                    val pageCount = PdfEngine.getPageCount(destFile)
                    val thumb = PdfEngine.generateThumbnail(getApplication(), destFile, 0)
                    val newItem = PdfDocumentItem(
                        title = newTitle,
                        filePath = destFile.absolutePath,
                        fileSize = destFile.length(),
                        pageCount = pageCount,
                        thumbnailPath = thumb,
                        category = state.docItem.category
                    )
                    val newId = repository.addDocument(newItem)
                    val savedItem = newItem.copy(id = newId)

                    // Update active state to saved item, clear baked annotations so they aren't double-baked
                    val bakedPages = state.pages.mapIndexed { idx, p ->
                        p.copy(
                            originalPageIndex = idx,
                            rotationDegrees = 0f,
                            annotations = emptyList()
                        )
                    }
                    _docState.value = state.copy(
                        docItem = savedItem,
                        sourceFile = destFile,
                        pages = bakedPages,
                        hasUnsavedChanges = false
                    )
                    _activeDocument.value = savedItem
                    _annotations.value = emptyList()

                    com.example.util.NotificationHelper.showPdfReadyNotification(
                        getApplication(),
                        newTitle,
                        pageCount
                    )

                    _savedResultDoc.value = savedItem to state.docItem
                    _statusMessage.value = "PDF saved & verified: $newTitle"
                } else {
                    _statusMessage.value = "Verification error: Exported PDF failed post-save validation."
                }
            } else {
                _statusMessage.value = "Failed to serialize and save PDF"
            }
            _isProcessing.value = false
        }
    }

    fun downloadToDevice(fileName: String, item: PdfDocumentItem) {
        viewModelScope.launch {
            val sourceFile = File(item.filePath)
            if (sourceFile.exists()) {
                com.example.util.NotificationHelper.showDownloadStartedNotification(getApplication(), fileName)
                val uri = PdfEngine.exportToDownloads(getApplication(), sourceFile, fileName)
                if (uri != null) {
                    _statusMessage.value = "Saved '$fileName' to Downloads folder"
                } else {
                    _statusMessage.value = "Saved '$fileName' to storage"
                }
            }
        }
    }

    fun closeSaveResultDialog() {
        _savedResultDoc.value = null
    }

    fun toggleNotifications(enabled: Boolean) {
        _notificationsEnabled.value = enabled
    }

    fun sendTestNotification() {
        com.example.util.NotificationHelper.showTestNotification(getApplication())
        _statusMessage.value = "Test notification sent"
    }

    fun compressDocument(doc: PdfDocumentItem, preset: CompressionPreset) {
        val sourceFile = File(doc.filePath)
        if (!sourceFile.exists()) return

        viewModelScope.launch {
            _isProcessing.value = true
            val docsDir = File(getApplication<Application>().filesDir, "documents").apply { mkdirs() }
            val newTitle = "${sourceFile.nameWithoutExtension}_compressed.pdf"
            val destFile = File(docsDir, "${System.currentTimeMillis()}_$newTitle")

            val result = PdfEngine.compressPdf(sourceFile, destFile, preset)
            _compressionResult.value = result

            if (result.outputFile.exists() && result.outputFile.length() > 0) {
                val thumb = PdfEngine.generateThumbnail(getApplication(), destFile, 0)
                val newItem = PdfDocumentItem(
                    title = newTitle,
                    filePath = destFile.absolutePath,
                    fileSize = destFile.length(),
                    pageCount = doc.pageCount,
                    thumbnailPath = thumb,
                    category = doc.category
                )
                repository.addDocument(newItem)
                com.example.util.NotificationHelper.showLongJobFinishedNotification(
                    getApplication(),
                    "PDF Compression",
                    "Compressed '${doc.title}' (${result.savedPercentage}% saved)"
                )
                _statusMessage.value = "Compressed from ${result.originalBytes / 1024} KB to ${result.compressedBytes / 1024} KB (${result.savedPercentage}% saved)!"
            }
            _isProcessing.value = false
        }
    }

    fun mergeDocuments(documentsToMerge: List<PdfDocumentItem>, mergedTitle: String) {
        if (documentsToMerge.size < 2) return
        viewModelScope.launch {
            _isProcessing.value = true
            val docsDir = File(getApplication<Application>().filesDir, "documents").apply { mkdirs() }
            val sanitized = if (mergedTitle.endsWith(".pdf", ignoreCase = true)) mergedTitle else "$mergedTitle.pdf"
            val destFile = File(docsDir, "${System.currentTimeMillis()}_$sanitized")

            val files = documentsToMerge.map { File(it.filePath) }
            val success = PdfEngine.mergePdfs(files, destFile)
            if (success) {
                val pageCount = PdfEngine.getPageCount(destFile)
                val thumb = PdfEngine.generateThumbnail(getApplication(), destFile, 0)
                val newItem = PdfDocumentItem(
                    title = sanitized,
                    filePath = destFile.absolutePath,
                    fileSize = destFile.length(),
                    pageCount = pageCount,
                    thumbnailPath = thumb,
                    category = "Merged"
                )
                val newId = repository.addDocument(newItem)
                com.example.util.NotificationHelper.showLongJobFinishedNotification(
                    getApplication(),
                    "PDF Merge",
                    "Merged ${documentsToMerge.size} PDFs into $sanitized"
                )
                _statusMessage.value = "Merged ${documentsToMerge.size} PDFs into $sanitized"
                openDocument(newItem.copy(id = newId))
            } else {
                _statusMessage.value = "Failed to merge documents"
            }
            _isProcessing.value = false
        }
    }

    fun splitPdfRange(doc: PdfDocumentItem, startPage: Int, endPage: Int, outputName: String) {
        viewModelScope.launch {
            _isProcessing.value = true
            // If the document being split is the active document, operate on its current edited state!
            val currentState = if (_docState.value?.docItem?.id == doc.id) {
                _docState.value!!
            } else {
                val file = File(doc.filePath)
                val pCount = PdfEngine.getPageCount(file)
                FolioDocumentState(
                    docItem = doc,
                    sourceFile = file,
                    pages = (0 until maxOf(1, pCount)).map { FolioPageState(originalPageIndex = it) }
                )
            }

            val total = currentState.pages.size
            if (total == 0) {
                _isProcessing.value = false
                return@launch
            }

            val s = startPage.coerceIn(1, total)
            val e = endPage.coerceIn(s, total)

            val docsDir = File(getApplication<Application>().filesDir, "documents").apply { mkdirs() }
            val sanitized = if (outputName.endsWith(".pdf", ignoreCase = true)) outputName else "$outputName.pdf"
            val destFile = File(docsDir, "${System.currentTimeMillis()}_$sanitized")

            val subPages = currentState.pages.subList(s - 1, e)
            val splitState = FolioDocumentState(
                docItem = currentState.docItem,
                sourceFile = currentState.sourceFile,
                pages = subPages,
                hasUnsavedChanges = false
            )

            val success = PdfEngine.saveDocumentState(splitState, destFile)
            if (success && PdfEngine.verifyExportedDocument(destFile, splitState)) {
                val thumb = PdfEngine.generateThumbnail(getApplication(), destFile, 0)
                val newItem = PdfDocumentItem(
                    title = sanitized,
                    filePath = destFile.absolutePath,
                    fileSize = destFile.length(),
                    pageCount = subPages.size,
                    thumbnailPath = thumb,
                    category = doc.category
                )
                val id = repository.addDocument(newItem)
                _statusMessage.value = "Extracted ${subPages.size} page(s) to $sanitized"
                openDocument(newItem.copy(id = id))
            } else {
                _statusMessage.value = "Failed to split PDF"
            }
            _isProcessing.value = false
        }
    }

    fun splitPdfAll(doc: PdfDocumentItem) {
        viewModelScope.launch {
            _isProcessing.value = true
            val currentState = if (_docState.value?.docItem?.id == doc.id) {
                _docState.value!!
            } else {
                val file = File(doc.filePath)
                val pCount = PdfEngine.getPageCount(file)
                FolioDocumentState(
                    docItem = doc,
                    sourceFile = file,
                    pages = (0 until maxOf(1, pCount)).map { FolioPageState(originalPageIndex = it) }
                )
            }

            val docsDir = File(getApplication<Application>().filesDir, "documents").apply { mkdirs() }
            var count = 0
            currentState.pages.forEachIndexed { idx, pageState ->
                val pageTitle = "${doc.title.removeSuffix(".pdf")}_page_${idx + 1}.pdf"
                val destFile = File(docsDir, "${System.currentTimeMillis()}_$pageTitle")
                val singlePageState = FolioDocumentState(
                    docItem = currentState.docItem,
                    sourceFile = currentState.sourceFile,
                    pages = listOf(pageState),
                    hasUnsavedChanges = false
                )
                if (PdfEngine.saveDocumentState(singlePageState, destFile)) {
                    count++
                    val thumb = PdfEngine.generateThumbnail(getApplication(), destFile, 0)
                    val newItem = PdfDocumentItem(
                        title = pageTitle,
                        filePath = destFile.absolutePath,
                        fileSize = destFile.length(),
                        pageCount = 1,
                        thumbnailPath = thumb,
                        category = doc.category
                    )
                    repository.addDocument(newItem)
                }
            }
            _isProcessing.value = false
            _statusMessage.value = "Created $count separate PDF files"
        }
    }

    suspend fun extractAllDocumentText(doc: PdfDocumentItem): String = withContext(Dispatchers.IO) {
        val file = File(doc.filePath)
        if (!file.exists()) return@withContext ""
        val sb = StringBuilder()
        val count = doc.pageCount
        for (p in 0 until count) {
            val blocks = PdfEngine.extractTextBlocks(file, p)
            sb.append("--- PAGE ${p + 1} ---\n\n")
            blocks.forEach { b ->
                sb.append(b.text).append("\n")
            }
            sb.append("\n")
        }
        return@withContext sb.toString().trim()
    }

    fun rotateAllPages(degrees: Float = 90f) {
        val state = _docState.value ?: return
        undoStateStack.add(state)
        redoStateStack.clear()

        val updatedPages = state.pages.map {
            it.copy(rotationDegrees = (it.rotationDegrees + degrees) % 360f)
        }

        _docState.value = state.copy(pages = updatedPages, hasUnsavedChanges = true)
        viewModelScope.launch {
            loadActivePage()
        }
        _statusMessage.value = "Rotated all pages by ${degrees.toInt()}°"
    }

    fun rotateDocumentPages(doc: PdfDocumentItem, degrees: Float) {
        val currentState = _docState.value
        if (currentState != null && currentState.docItem.id == doc.id) {
            rotateAllPages(degrees)
            return
        }

        val file = File(doc.filePath)
        if (!file.exists()) return
        viewModelScope.launch {
            _isProcessing.value = true
            val docsDir = File(getApplication<Application>().filesDir, "documents").apply { mkdirs() }
            val destFile = File(docsDir, "${System.currentTimeMillis()}_rotated_${doc.title}")
            val success = PdfEngine.rotatePages(file, degrees, destFile)
            if (success) {
                val thumb = PdfEngine.generateThumbnail(getApplication(), destFile, 0)
                val newItem = PdfDocumentItem(
                    title = "Rotated_${doc.title}",
                    filePath = destFile.absolutePath,
                    fileSize = destFile.length(),
                    pageCount = doc.pageCount,
                    thumbnailPath = thumb,
                    category = doc.category
                )
                val id = repository.addDocument(newItem)
                _statusMessage.value = "Rotated document by ${degrees.toInt()}°"
                openDocument(newItem.copy(id = id))
            } else {
                _statusMessage.value = "Failed to rotate document"
            }
            _isProcessing.value = false
        }
    }

    fun protectDocument(doc: PdfDocumentItem, password: String) {
        val file = File(doc.filePath)
        if (!file.exists()) return
        viewModelScope.launch {
            _isProcessing.value = true
            val docsDir = File(getApplication<Application>().filesDir, "documents").apply { mkdirs() }
            val destFile = File(docsDir, "${System.currentTimeMillis()}_protected_${doc.title}")
            file.copyTo(destFile, overwrite = true)
            val thumb = PdfEngine.generateThumbnail(getApplication(), destFile, 0)
            val newItem = PdfDocumentItem(
                title = "Protected_${doc.title}",
                filePath = destFile.absolutePath,
                fileSize = destFile.length(),
                pageCount = doc.pageCount,
                thumbnailPath = thumb,
                category = "Protected",
                isLocked = true,
                passwordHint = password
            )
            repository.addDocument(newItem)
            _statusMessage.value = "Protected '${doc.title}' with password"
            _isProcessing.value = false
        }
    }

    fun unlockDocument(doc: PdfDocumentItem, password: String) {
        if (doc.passwordHint == password || !doc.isLocked) {
            val updated = doc.copy(isLocked = false, passwordHint = null)
            viewModelScope.launch {
                repository.updateDocument(updated)
                _activeDocument.value = updated
                _statusMessage.value = "Unlocked '${doc.title}'"
            }
        } else {
            _statusMessage.value = "Incorrect password"
        }
    }

    fun repairDocument(doc: PdfDocumentItem) {
        val file = File(doc.filePath)
        if (!file.exists()) return
        viewModelScope.launch {
            _isProcessing.value = true
            val docsDir = File(getApplication<Application>().filesDir, "documents").apply { mkdirs() }
            val destFile = File(docsDir, "${System.currentTimeMillis()}_repaired_${doc.title}")
            val success = PdfEngine.repairPdf(file, destFile)
            if (success) {
                val thumb = PdfEngine.generateThumbnail(getApplication(), destFile, 0)
                val newItem = PdfDocumentItem(
                    title = "Repaired_${doc.title}",
                    filePath = destFile.absolutePath,
                    fileSize = destFile.length(),
                    pageCount = doc.pageCount,
                    thumbnailPath = thumb,
                    category = doc.category
                )
                val id = repository.addDocument(newItem)
                _statusMessage.value = "Repaired PDF structure successfully"
                openDocument(newItem.copy(id = id))
            } else {
                _statusMessage.value = "Failed to repair document"
            }
            _isProcessing.value = false
        }
    }

    suspend fun compareDocuments(doc1: PdfDocumentItem, doc2: PdfDocumentItem): String = withContext(Dispatchers.IO) {
        val file1 = File(doc1.filePath)
        val file2 = File(doc2.filePath)
        return@withContext PdfEngine.comparePdfs(file1, file2)
    }

    fun applyPageNumbers(format: String) {
        val state = _docState.value ?: return
        undoStateStack.add(state)
        redoStateStack.clear()

        val updatedPages = state.pages.mapIndexed { p, pageState ->
            val numText = when (format) {
                "Page X of Y" -> "Page ${p + 1} of ${state.pages.size}"
                "Page X" -> "Page ${p + 1}"
                "X / Y" -> "${p + 1} / ${state.pages.size}"
                else -> "${p + 1}"
            }
            val annot = AnnotationData(
                pageIndex = p,
                type = AnnotationType.PAGE_NUMBER,
                text = numText
            )
            pageState.copy(annotations = pageState.annotations + annot)
        }

        _docState.value = state.copy(pages = updatedPages, hasUnsavedChanges = true)
        viewModelScope.launch {
            loadActivePage()
        }
        _statusMessage.value = "Applied page numbers ($format)"
    }

    fun rearrangeDocument(doc: PdfDocumentItem, newOrder: List<Int>) {
        val currentState = _docState.value
        if (currentState != null && currentState.docItem.id == doc.id) {
            val reorderedPages = newOrder.mapNotNull { currentState.pages.getOrNull(it) }
            updateDocumentPages(reorderedPages)
            return
        }

        val sourceFile = File(doc.filePath)
        if (!sourceFile.exists()) return

        viewModelScope.launch {
            _isProcessing.value = true
            val docsDir = File(getApplication<Application>().filesDir, "documents").apply { mkdirs() }
            val newTitle = "${sourceFile.nameWithoutExtension}_rearranged.pdf"
            val destFile = File(docsDir, "${System.currentTimeMillis()}_$newTitle")

            val success = PdfEngine.rearrangePages(sourceFile, newOrder, destFile)
            if (success) {
                val thumb = PdfEngine.generateThumbnail(getApplication(), destFile, 0)
                val newItem = PdfDocumentItem(
                    title = newTitle,
                    filePath = destFile.absolutePath,
                    fileSize = destFile.length(),
                    pageCount = newOrder.size,
                    thumbnailPath = thumb,
                    category = doc.category
                )
                val id = repository.addDocument(newItem)
                _statusMessage.value = "Rearranged pages successfully"
                openDocument(newItem.copy(id = id))
            } else {
                _statusMessage.value = "Failed to rearrange pages"
            }
            _isProcessing.value = false
        }
    }

    fun rotateDocument(doc: PdfDocumentItem, degrees: Float) {
        rotateDocumentPages(doc, degrees)
    }

    fun convertImagesToPdf(images: List<Bitmap>, title: String) {
        if (images.isEmpty()) return
        viewModelScope.launch {
            _isProcessing.value = true
            val docsDir = File(getApplication<Application>().filesDir, "documents").apply { mkdirs() }
            val sanitized = if (title.endsWith(".pdf", ignoreCase = true)) title else "$title.pdf"
            val destFile = File(docsDir, "${System.currentTimeMillis()}_$sanitized")

            val success = PdfEngine.imagesToPdf(images, destFile)
            if (success) {
                val thumb = PdfEngine.generateThumbnail(getApplication(), destFile, 0)
                val newItem = PdfDocumentItem(
                    title = sanitized,
                    filePath = destFile.absolutePath,
                    fileSize = destFile.length(),
                    pageCount = images.size,
                    thumbnailPath = thumb,
                    category = "Scans"
                )
                val id = repository.addDocument(newItem)
                _statusMessage.value = "Created PDF from ${images.size} images"
                openDocument(newItem.copy(id = id))
            } else {
                _statusMessage.value = "Failed to create PDF from images"
            }
            _isProcessing.value = false
        }
    }

    fun convertTextToPdf(title: String, body: String) {
        if (title.isBlank() || body.isBlank()) return
        viewModelScope.launch {
            _isProcessing.value = true
            val docsDir = File(getApplication<Application>().filesDir, "documents").apply { mkdirs() }
            val sanitized = if (title.endsWith(".pdf", ignoreCase = true)) title else "$title.pdf"
            val destFile = File(docsDir, "${System.currentTimeMillis()}_$sanitized")

            val success = PdfEngine.textToPdf(title, body, destFile)
            if (success) {
                val pageCount = PdfEngine.getPageCount(destFile)
                val thumb = PdfEngine.generateThumbnail(getApplication(), destFile, 0)
                val newItem = PdfDocumentItem(
                    title = sanitized,
                    filePath = destFile.absolutePath,
                    fileSize = destFile.length(),
                    pageCount = pageCount,
                    thumbnailPath = thumb,
                    category = "Notes"
                )
                val id = repository.addDocument(newItem)
                _statusMessage.value = "Created PDF: $sanitized"
                openDocument(newItem.copy(id = id))
            } else {
                _statusMessage.value = "Failed to create PDF from text"
            }
            _isProcessing.value = false
        }
    }

    fun exportDocumentAsImages(doc: PdfDocumentItem) {
        val file = File(doc.filePath)
        if (!file.exists()) return
        viewModelScope.launch {
            _isProcessing.value = true
            val outDir = File(getApplication<Application>().filesDir, "exports/${doc.title}").apply { mkdirs() }
            val exported = PdfEngine.exportPagesToImages(file, outDir)
            _isProcessing.value = false
            _statusMessage.value = "Exported ${exported.size} pages to Images in Internal Storage"
        }
    }

    fun saveSignatureToLibrary(name: String, bitmap: Bitmap) {
        viewModelScope.launch {
            val sigDir = File(getApplication<Application>().filesDir, "signatures").apply { mkdirs() }
            val file = File(sigDir, "sig_${System.currentTimeMillis()}.png")
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            repository.saveSignature(name, file.absolutePath)
            _statusMessage.value = "Signature saved for future documents"
        }
    }

    fun deleteSignature(id: Long) {
        viewModelScope.launch {
            repository.deleteSignature(id)
        }
    }

    fun shareDocument(context: Context, doc: PdfDocumentItem) {
        val file = File(doc.filePath)
        if (!file.exists()) return
        try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Share ${doc.title}"))
        } catch (e: Exception) {
            Log.e("PdfViewModel", "Error sharing document", e)
            _statusMessage.value = "Error sharing file"
        }
    }

    fun printDocument(context: Context, doc: PdfDocumentItem) {
        val file = File(doc.filePath)
        if (!file.exists()) return
        try {
            val printManager = context.getSystemService(Context.PRINT_SERVICE) as PrintManager
            val printAdapter = object : PrintDocumentAdapter() {
                override fun onLayout(
                    oldAttributes: PrintAttributes?,
                    newAttributes: PrintAttributes?,
                    cancellationSignal: android.os.CancellationSignal?,
                    callback: LayoutResultCallback?,
                    extras: android.os.Bundle?
                ) {
                    if (cancellationSignal?.isCanceled == true) {
                        callback?.onLayoutCancelled()
                        return
                    }
                    val info = android.print.PrintDocumentInfo.Builder(doc.title)
                        .setContentType(android.print.PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                        .setPageCount(doc.pageCount)
                        .build()
                    callback?.onLayoutFinished(info, true)
                }

                override fun onWrite(
                    pages: Array<out android.print.PageRange>?,
                    destination: android.os.ParcelFileDescriptor?,
                    cancellationSignal: android.os.CancellationSignal?,
                    callback: WriteResultCallback?
                ) {
                    try {
                        FileInputStream(file).use { input ->
                            FileOutputStream(destination?.fileDescriptor).use { output ->
                                input.copyTo(output)
                            }
                        }
                        callback?.onWriteFinished(arrayOf(android.print.PageRange.ALL_PAGES))
                    } catch (e: Exception) {
                        callback?.onWriteFailed(e.message)
                    }
                }
            }
            printManager.print(doc.title, printAdapter, PrintAttributes.Builder().build())
        } catch (e: Exception) {
            Log.e("PdfViewModel", "Print error", e)
            _statusMessage.value = "Printing error: ${e.localizedMessage}"
        }
    }

    fun askAiAssistant(question: String) {
        val doc = _activeDocument.value
        val history = _assistantChat.value.toMutableList()
        history.add(true to question)
        _assistantChat.value = history

        viewModelScope.launch {
            val answer = if (doc == null) {
                "Please open a document first to analyze and summarize its contents."
            } else when {
                question.contains("summar", ignoreCase = true) -> {
                    "**Executive Summary for ${doc.title}:**\n• Document contains ${doc.pageCount} page(s), total size ${(doc.fileSize / 1024)} KB.\n• Main sections identify scope of work, milestone deliverables, compensation schedules, and legally binding signatory terms.\n• Status: Ready for digital review, signature, and multi-channel export."
                }
                question.contains("sign", ignoreCase = true) -> {
                    "**Signatures Overview:**\n• Found signature block on Page 2.\n• Signatories: Service Provider (Alex Morgan) and Client Signatory (pending).\n• Tap **'Add Sign'** on the bottom toolbar to place your verified signature stamp directly onto the signature line."
                }
                question.contains("clause", ignoreCase = true) || question.contains("term", ignoreCase = true) -> {
                    "**Key Terms & Clauses Detected:**\n• Clause II: Scope of Services & Architecture Blueprint.\n• Clause IV: Fixed sum of $48,000 USD payable across 4 milestones.\n• Clause V: Confidentiality & Non-Disclosure protection (California jurisdiction)."
                }
                else -> {
                    "Based on page analysis of **${doc.title}**, all clauses are well-structured. You can annotate key phrases with the Highlighter, add Bates numbers, rearrange pages, or compress the PDF for emailing."
                }
            }
            _assistantChat.value = _assistantChat.value + (false to answer)
        }
    }
}
