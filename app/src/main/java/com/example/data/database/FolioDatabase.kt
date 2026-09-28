package com.example.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.dao.PdfDocumentDao
import com.example.data.dao.SignatureDao
import com.example.data.model.PdfDocumentItem
import com.example.data.model.SignatureItem

@Database(
    entities = [PdfDocumentItem::class, SignatureItem::class],
    version = 1,
    exportSchema = false
)
abstract class FolioDatabase : RoomDatabase() {
    abstract fun pdfDocumentDao(): PdfDocumentDao
    abstract fun signatureDao(): SignatureDao

    companion object {
        @Volatile
        private var INSTANCE: FolioDatabase? = null

        fun getDatabase(context: Context): FolioDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    FolioDatabase::class.java,
                    "folio_pdf_database"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
