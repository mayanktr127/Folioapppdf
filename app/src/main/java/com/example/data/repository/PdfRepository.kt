package com.example.data.repository

import android.content.Context
import android.net.Uri
import com.example.data.dao.PdfDocumentDao
import com.example.data.dao.SignatureDao
import com.example.data.model.PdfDocumentItem
import com.example.data.model.SignatureItem
import com.example.engine.PdfEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class PdfRepository(
    private val context: Context,
    private val documentDao: PdfDocumentDao,
    private val signatureDao: SignatureDao
) {
    val allDocuments: Flow<List<PdfDocumentItem>> = documentDao.getAllDocuments()
    val recentDocuments: Flow<List<PdfDocumentItem>> = documentDao.getRecentDocuments()
    val favoriteDocuments: Flow<List<PdfDocumentItem>> = documentDao.getFavoriteDocuments()
    val allSignatures: Flow<List<SignatureItem>> = signatureDao.getAllSignatures()

    fun searchDocuments(query: String): Flow<List<PdfDocumentItem>> {
        return documentDao.searchDocuments(query)
    }

    suspend fun getDocumentById(id: Long): PdfDocumentItem? = withContext(Dispatchers.IO) {
        documentDao.getDocumentById(id)
    }

    suspend fun checkAndSeedInitialDocuments() = withContext(Dispatchers.IO) {
        val existing = allDocuments.first()
        if (existing.isEmpty()) {
            val sampleFiles = PdfEngine.createSampleDocuments(context)
            val items = sampleFiles.mapIndexed { index, file ->
                val pageCount = PdfEngine.getPageCount(file)
                val thumbPath = PdfEngine.generateThumbnail(context, file, 0)
                PdfDocumentItem(
                    title = file.name,
                    filePath = file.absolutePath,
                    fileSize = file.length(),
                    pageCount = pageCount,
                    isFavorite = (index == 1), // Wedding invitation is favorite by default matching Image 2
                    isRecent = true,
                    thumbnailPath = thumbPath,
                    category = when (index) {
                        0 -> "Contracts"
                        1 -> "Personal"
                        2 -> "Invoices"
                        else -> "Reports"
                    }
                )
            }
            documentDao.insertDocuments(items)
        }
    }

    suspend fun importDocument(uri: Uri, displayName: String): PdfDocumentItem? = withContext(Dispatchers.IO) {
        try {
            val docsDir = File(context.filesDir, "documents").apply { mkdirs() }
            val sanitizedName = if (displayName.endsWith(".pdf", ignoreCase = true)) displayName else "$displayName.pdf"
            val destFile = File(docsDir, "${System.currentTimeMillis()}_$sanitizedName")

            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
            }

            if (!destFile.exists() || destFile.length() == 0L) return@withContext null

            val pageCount = PdfEngine.getPageCount(destFile)
            val thumbPath = PdfEngine.generateThumbnail(context, destFile, 0)

            val item = PdfDocumentItem(
                title = sanitizedName,
                filePath = destFile.absolutePath,
                fileSize = destFile.length(),
                pageCount = maxOf(1, pageCount),
                thumbnailPath = thumbPath
            )
            val id = documentDao.insertDocument(item)
            return@withContext item.copy(id = id)
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext null
        }
    }

    suspend fun addDocument(item: PdfDocumentItem): Long = withContext(Dispatchers.IO) {
        documentDao.insertDocument(item)
    }

    suspend fun toggleFavorite(id: Long, current: Boolean) = withContext(Dispatchers.IO) {
        documentDao.updateFavorite(id, !current)
    }

    suspend fun markAccessed(id: Long) = withContext(Dispatchers.IO) {
        documentDao.markAccessed(id)
    }

    suspend fun deleteDocument(item: PdfDocumentItem) = withContext(Dispatchers.IO) {
        documentDao.deleteById(item.id)
        try {
            val file = File(item.filePath)
            if (file.exists()) file.delete()
            item.thumbnailPath?.let {
                val thumb = File(it)
                if (thumb.exists()) thumb.delete()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun saveSignature(name: String, imagePath: String): Long = withContext(Dispatchers.IO) {
        signatureDao.insertSignature(
            SignatureItem(name = name, imagePath = imagePath)
        )
    }

    suspend fun deleteSignature(id: Long) = withContext(Dispatchers.IO) {
        signatureDao.deleteById(id)
    }
}
