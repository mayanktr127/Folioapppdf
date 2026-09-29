package com.example.shadows

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Rect
import android.graphics.Matrix
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements
import org.robolectric.annotation.RealObject
import org.robolectric.shadow.api.Shadow
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.OutputStream

data class StoredPage(
    val pageNumber: Int,
    val width: Int,
    val height: Int,
    val bitmap: Bitmap
)

@Implements(PdfDocument::class)
class ShadowPdfDocument {

    private val pages = mutableListOf<StoredPage>()
    private var currentPage: PdfDocument.Page? = null
    private var currentBitmap: Bitmap? = null
    private var isClosed = false

    @Implementation
    fun __constructor__() {
        isClosed = false
    }

    @Implementation
    fun startPage(pageInfo: PdfDocument.PageInfo): PdfDocument.Page {
        if (isClosed) throw IllegalStateException("document is closed!")
        val w = maxOf(1, pageInfo.pageWidth)
        val h = maxOf(1, pageInfo.pageHeight)
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        canvas.drawColor(Color.WHITE)
        currentBitmap = bmp

        val page = Shadow.newInstanceOf(PdfDocument.Page::class.java)
        val shadowPage = Shadow.extract<ShadowPdfPage>(page)
        shadowPage.init(canvas, pageInfo, bmp)
        currentPage = page
        return page
    }

    @Implementation
    fun finishPage(page: PdfDocument.Page) {
        if (isClosed) throw IllegalStateException("document is closed!")
        val shadowPage = Shadow.extract<ShadowPdfPage>(page)
        val info = shadowPage.storedPageInfo ?: page.info
        val bmp = shadowPage.storedBitmap ?: currentBitmap ?: Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
        pages.add(StoredPage(info.pageNumber, info.pageWidth, info.pageHeight, bmp))
        currentPage = null
    }

    @Implementation
    fun writeTo(out: OutputStream) {
        if (isClosed) throw IllegalStateException("document is closed!")
        val byteOut = ByteArrayOutputStream()
        val dataOut = DataOutputStream(byteOut)
        dataOut.writeBytes("%PDF-1.4\n")
        dataOut.writeBytes("FOLIO_TEST_MAGIC\n")
        dataOut.writeInt(pages.size)
        for (p in pages) {
            dataOut.writeInt(p.pageNumber)
            dataOut.writeInt(p.width)
            dataOut.writeInt(p.height)
            val bmpStream = ByteArrayOutputStream()
            p.bitmap.compress(Bitmap.CompressFormat.PNG, 100, bmpStream)
            val bytes = bmpStream.toByteArray()
            dataOut.writeInt(bytes.size)
            dataOut.write(bytes)
        }
        dataOut.writeBytes("\n%%EOF\n")
        dataOut.flush()
        out.write(byteOut.toByteArray())
        out.flush()
    }

    @Implementation
    fun close() {
        isClosed = true
    }
}

@Implements(PdfDocument.Page::class)
class ShadowPdfPage {
    private var internalCanvas: Canvas? = null
    private var internalPageInfo: PdfDocument.PageInfo? = null
    private var internalBitmap: Bitmap? = null

    val storedBitmap: Bitmap?
        get() = internalBitmap

    val storedPageInfo: PdfDocument.PageInfo?
        get() = internalPageInfo

    fun init(c: Canvas, info: PdfDocument.PageInfo, bmp: Bitmap) {
        internalCanvas = c
        internalPageInfo = info
        internalBitmap = bmp
    }

    @Implementation
    fun getCanvas(): Canvas {
        return internalCanvas ?: Canvas()
    }

    @Implementation
    fun getInfo(): PdfDocument.PageInfo {
        return internalPageInfo ?: PdfDocument.PageInfo.Builder(600, 800, 1).create()
    }
}

@Implements(PdfRenderer::class)
class ShadowPdfRenderer {

    private val pages = mutableListOf<StoredPage>()
    private var isClosed = false

    @Implementation
    fun __constructor__(input: ParcelFileDescriptor) {
        try {
            val fis = FileInputStream(input.fileDescriptor)
            val allBytes = fis.readBytes()
            val text = String(allBytes.take(50).toByteArray(), Charsets.ISO_8859_1)
            if (!text.startsWith("%PDF-")) {
                throw IOException("Not a valid PDF file")
            }
            if (text.contains("ENCRYPT") || text.contains("password")) {
                throw SecurityException("Password required to decrypt PDF")
            }

            // Check if written by ShadowPdfDocument
            val magicIndex = allBytes.indexOfSequence("FOLIO_TEST_MAGIC\n".toByteArray(Charsets.ISO_8859_1))
            if (magicIndex != -1) {
                val dataIn = DataInputStream(ByteArrayInputStream(allBytes, magicIndex + "FOLIO_TEST_MAGIC\n".length, allBytes.size - magicIndex))
                val pageCount = dataIn.readInt()
                for (i in 0 until pageCount) {
                    val pNum = dataIn.readInt()
                    val w = dataIn.readInt()
                    val h = dataIn.readInt()
                    val bSize = dataIn.readInt()
                    val bBytes = ByteArray(bSize)
                    dataIn.readFully(bBytes)
                    val bmp = android.graphics.BitmapFactory.decodeByteArray(bBytes, 0, bSize)
                    pages.add(StoredPage(pNum, w, h, bmp ?: Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)))
                }
            } else {
                // Generic PDF parsed: estimate page count from file or default to 1
                val contentStr = String(allBytes, Charsets.ISO_8859_1)
                val typePageCount = Regex("/Type\\s*/Page[^s]").findAll(contentStr).count()
                val count = maxOf(1, typePageCount)
                for (i in 0 until count) {
                    pages.add(StoredPage(i + 1, 600, 800, Bitmap.createBitmap(600, 800, Bitmap.Config.ARGB_8888)))
                }
            }
        } catch (e: SecurityException) {
            throw e
        } catch (e: IOException) {
            throw e
        } catch (e: Exception) {
            throw IOException("Failed to parse PDF", e)
        }
    }

    private fun ByteArray.indexOfSequence(seq: ByteArray): Int {
        for (i in 0..this.size - seq.size) {
            var match = true
            for (j in seq.indices) {
                if (this[i + j] != seq[j]) {
                    match = false
                    break
                }
            }
            if (match) return i
        }
        return -1
    }

    @Implementation
    fun getPageCount(): Int {
        if (isClosed) throw IllegalStateException("Renderer is closed")
        return pages.size
    }

    @Implementation
    fun openPage(index: Int): PdfRenderer.Page {
        if (isClosed) throw IllegalStateException("Renderer is closed")
        if (index < 0 || index >= pages.size) throw IllegalArgumentException("Invalid page index: $index")
        val page = Shadow.newInstanceOf(PdfRenderer.Page::class.java)
        val shadowPage = Shadow.extract<ShadowPdfRendererPage>(page)
        shadowPage.init(pages[index], index)
        return page
    }

    @Implementation
    fun close() {
        isClosed = true
        for (p in pages) {
            if (!p.bitmap.isRecycled) {
                // keep intact or recycle if needed
            }
        }
    }
}

@Implements(PdfRenderer.Page::class)
class ShadowPdfRendererPage {
    private var storedPage: StoredPage? = null
    private var pageIndex: Int = 0
    private var isClosed = false

    fun init(sp: StoredPage, idx: Int) {
        storedPage = sp
        pageIndex = idx
        isClosed = false
    }

    @Implementation
    fun getWidth(): Int = storedPage?.width ?: 600

    @Implementation
    fun getHeight(): Int = storedPage?.height ?: 800

    @Implementation
    fun getIndex(): Int = pageIndex

    @Implementation
    fun render(destination: Bitmap, destClip: Rect?, transform: Matrix?, renderMode: Int) {
        if (isClosed) throw IllegalStateException("Page is closed")
        val sp = storedPage ?: return
        val canvas = Canvas(destination)
        val src = Rect(0, 0, sp.bitmap.width, sp.bitmap.height)
        val dst = destClip ?: Rect(0, 0, destination.width, destination.height)
        canvas.drawBitmap(sp.bitmap, src, dst, null)
    }

    @Implementation
    fun close() {
        isClosed = true
    }
}
