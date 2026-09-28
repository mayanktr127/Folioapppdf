package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pdf_documents")
data class PdfDocumentItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val filePath: String,
    val fileSize: Long,
    val pageCount: Int = 1,
    val createdAt: Long = System.currentTimeMillis(),
    val lastModified: Long = System.currentTimeMillis(),
    val isFavorite: Boolean = false,
    val isRecent: Boolean = true,
    val thumbnailPath: String? = null,
    val category: String = "General",
    val isLocked: Boolean = false,
    val passwordHint: String? = null
)

@Entity(tableName = "saved_signatures")
data class SignatureItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val imagePath: String,
    val createdAt: Long = System.currentTimeMillis()
)
