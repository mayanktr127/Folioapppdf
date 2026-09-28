package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.PdfDocumentItem
import com.example.data.model.SignatureItem
import kotlinx.coroutines.flow.Flow

@Dao
interface PdfDocumentDao {
    @Query("SELECT * FROM pdf_documents ORDER BY lastModified DESC")
    fun getAllDocuments(): Flow<List<PdfDocumentItem>>

    @Query("SELECT * FROM pdf_documents WHERE isRecent = 1 ORDER BY lastModified DESC LIMIT 20")
    fun getRecentDocuments(): Flow<List<PdfDocumentItem>>

    @Query("SELECT * FROM pdf_documents WHERE isFavorite = 1 ORDER BY lastModified DESC")
    fun getFavoriteDocuments(): Flow<List<PdfDocumentItem>>

    @Query("SELECT * FROM pdf_documents WHERE id = :id LIMIT 1")
    suspend fun getDocumentById(id: Long): PdfDocumentItem?

    @Query("SELECT * FROM pdf_documents WHERE title LIKE '%' || :query || '%' ORDER BY lastModified DESC")
    fun searchDocuments(query: String): Flow<List<PdfDocumentItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDocument(document: PdfDocumentItem): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDocuments(documents: List<PdfDocumentItem>)

    @Update
    suspend fun updateDocument(document: PdfDocumentItem)

    @Delete
    suspend fun deleteDocument(document: PdfDocumentItem)

    @Query("DELETE FROM pdf_documents WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("UPDATE pdf_documents SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun updateFavorite(id: Long, isFavorite: Boolean)

    @Query("UPDATE pdf_documents SET lastModified = :time, isRecent = 1 WHERE id = :id")
    suspend fun markAccessed(id: Long, time: Long = System.currentTimeMillis())
}

@Dao
interface SignatureDao {
    @Query("SELECT * FROM saved_signatures ORDER BY createdAt DESC")
    fun getAllSignatures(): Flow<List<SignatureItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSignature(signature: SignatureItem): Long

    @Delete
    suspend fun deleteSignature(signature: SignatureItem)

    @Query("DELETE FROM saved_signatures WHERE id = :id")
    suspend fun deleteById(id: Long)
}
