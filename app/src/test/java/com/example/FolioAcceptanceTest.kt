package com.example

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.PdfDocumentItem
import com.example.engine.AnnotationData
import com.example.engine.AnnotationType
import com.example.engine.FolioDocumentState
import com.example.engine.FolioPageState
import com.example.engine.PdfEngine
import com.example.engine.PdfTextBlock
import com.example.engine.RectFData
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.io.FileOutputStream

import com.example.shadows.ShadowPdfDocument
import com.example.shadows.ShadowPdfPage
import com.example.shadows.ShadowPdfRenderer
import com.example.shadows.ShadowPdfRendererPage

@RunWith(RobolectricTestRunner::class)
@Config(
    sdk = [34],
    shadows = [
        ShadowPdfDocument::class,
        ShadowPdfPage::class,
        ShadowPdfRenderer::class,
        ShadowPdfRendererPage::class
    ]
)
class FolioAcceptanceTest {

    private fun create5PageTestPdf(context: Context): File {
        val doc = PdfDocument()
        val pageTexts = listOf(
            "Page 1: Alpha Line One keywordTarget",
            "Page 2: Beta Line Two keywordTarget",
            "Page 3: Gamma Line Three keywordTarget",
            "Page 4: Delta Line Four keywordTarget",
            "Page 5: Epsilon Line Five keywordTarget"
        )
        pageTexts.forEachIndexed { i, text ->
            val pageInfo = PdfDocument.PageInfo.Builder(600, 800, i + 1).create()
            val page = doc.startPage(pageInfo)
            val paint = Paint().apply {
                color = Color.BLACK
                textSize = 24f
            }
            page.canvas.drawText(text, 50f, 100f, paint)
            doc.finishPage(page)
        }
        val file = File(context.cacheDir, "test_source_5page_${System.currentTimeMillis()}.pdf")
        FileOutputStream(file).use { doc.writeTo(it) }
        doc.close()
        return file
    }

    @Test
    fun `test complete acceptance workflow - edits, find replace, rearrange, rotate, save and reopen`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val sourceFile = create5PageTestPdf(context)
        assertTrue(sourceFile.exists())
        assertEquals(5, PdfEngine.getPageCount(sourceFile))

        val docItem = PdfDocumentItem(
            id = 1L,
            title = sourceFile.name,
            filePath = sourceFile.absolutePath,
            fileSize = sourceFile.length(),
            pageCount = 5
        )

        // Initial shared document state with 5 pages
        var docState = FolioDocumentState(
            docItem = docItem,
            sourceFile = sourceFile,
            pages = (0 until 5).map { FolioPageState(originalPageIndex = it) },
            hasUnsavedChanges = false
        )

        // -------------------------------------------------------------
        // Step 1: Edit a line of text on Page 1
        // -------------------------------------------------------------
        val page1TextBlock = PdfTextBlock(
            pageIndex = 0,
            text = "Page 1: Alpha EDITED TEXT",
            originalText = "Page 1: Alpha Line One keywordTarget",
            rect = RectFData(0.08f, 0.10f, 0.70f, 0.15f),
            isModified = true
        )
        val editAnnot = AnnotationData(
            pageIndex = 0,
            type = AnnotationType.TEXT_REPLACE,
            rect = page1TextBlock.rect,
            text = "Page 1: Alpha EDITED TEXT",
            originalText = page1TextBlock.originalText
        )
        val updatedPagesStep1 = docState.pages.toMutableList()
        updatedPagesStep1[0] = updatedPagesStep1[0].copy(
            annotations = listOf(editAnnot),
            textBlocks = listOf(page1TextBlock)
        )
        docState = docState.copy(pages = updatedPagesStep1, hasUnsavedChanges = true)
        assertTrue(docState.hasUnsavedChanges)

        // -------------------------------------------------------------
        // Step 2: Use Find & Replace to change 'keywordTarget' -> 'REPLACED_WORD' across all pages
        // -------------------------------------------------------------
        val findWord = "keywordTarget"
        val replaceWord = "REPLACED_WORD"
        val updatedPagesStep2 = docState.pages.mapIndexed { idx, pState ->
            val frAnnot = AnnotationData(
                pageIndex = idx,
                type = AnnotationType.TEXT_REPLACE,
                rect = RectFData(0.08f, 0.10f, 0.70f, 0.15f),
                text = "Page ${pState.originalPageIndex + 1}: Line with $replaceWord"
            )
            pState.copy(annotations = pState.annotations + frAnnot)
        }
        docState = docState.copy(pages = updatedPagesStep2, hasUnsavedChanges = true)

        // -------------------------------------------------------------
        // Step 3: Page Organizer - delete page 3 (index 2), move page 5 to position 1, rotate page 2 by 90°
        // -------------------------------------------------------------
        // Current pages: [P0, P1, P2, P3, P4]
        val workingPages = docState.pages.toMutableList()
        // Delete page 3 (index 2): remaining are [P0, P1, P3, P4]
        workingPages.removeAt(2)
        assertEquals(4, workingPages.size)

        // Move page 5 (currently at index 3, originalPageIndex=4) to position 1 (index 0)
        val page5 = workingPages.removeAt(3)
        assertEquals(4, page5.originalPageIndex)
        workingPages.add(0, page5)
        // Now order is: [P4, P0, P1, P3]
        assertEquals(4, workingPages[0].originalPageIndex)
        assertEquals(0, workingPages[1].originalPageIndex)
        assertEquals(1, workingPages[2].originalPageIndex)
        assertEquals(3, workingPages[3].originalPageIndex)

        // Rotate page 2 (which is at index 2, originalPageIndex=1) by 90°
        workingPages[2] = workingPages[2].copy(rotationDegrees = 90f)

        docState = docState.copy(pages = workingPages, hasUnsavedChanges = true)
        assertEquals(4, docState.pageCount)

        // -------------------------------------------------------------
        // Step 4: Tap Save copy
        // -------------------------------------------------------------
        val exportedFile = File(context.cacheDir, "exported_acceptance_test1.pdf")
        val saveSuccess = PdfEngine.saveDocumentState(docState, exportedFile)
        assertTrue("Save document state should succeed", saveSuccess)
        assertTrue(exportedFile.exists() && exportedFile.length() > 0)

        // -------------------------------------------------------------
        // Step 5: Open the exported file in-memory and confirm:
        // - 4 pages remain
        // - In the new order (P4 -> P0 -> P1(rotated) -> P3)
        // - Rotation applied
        // - Verify passes
        // -------------------------------------------------------------
        val pfd = ParcelFileDescriptor.open(exportedFile, ParcelFileDescriptor.MODE_READ_ONLY)
        val renderer = PdfRenderer(pfd)
        assertEquals("Exported PDF must have 4 pages", 4, renderer.pageCount)

        // Page 0 was P4 (original aspect ratio 600x800)
        val p0 = renderer.openPage(0)
        assertEquals(600, p0.width)
        assertEquals(800, p0.height)
        p0.close()

        // Page 2 was P1 rotated by 90° (width and height swap: 800x600)
        val p2 = renderer.openPage(2)
        assertEquals(800, p2.width)
        assertEquals(600, p2.height)
        p2.close()

        renderer.close()
        pfd.close()

        val verified = PdfEngine.verifyExportedDocument(exportedFile, docState)
        assertTrue("Exported document verification must pass", verified)

        // -------------------------------------------------------------
        // Step 6: Repeat in a DIFFERENT order (rearrange first, THEN edit text)
        // -------------------------------------------------------------
        val sourceFile2 = create5PageTestPdf(context)
        var docState2 = FolioDocumentState(
            docItem = docItem.copy(filePath = sourceFile2.absolutePath),
            sourceFile = sourceFile2,
            pages = (0 until 5).map { FolioPageState(originalPageIndex = it) },
            hasUnsavedChanges = false
        )

        // A. Rearrange first: delete page 3, move page 5 to position 1, rotate page 2 by 90°
        val workingPages2 = docState2.pages.toMutableList()
        workingPages2.removeAt(2) // Delete P2
        val movedP5 = workingPages2.removeAt(3) // Remove P4
        workingPages2.add(0, movedP5) // Put P4 at front -> [P4, P0, P1, P3]
        workingPages2[2] = workingPages2[2].copy(rotationDegrees = 90f) // Rotate P1
        docState2 = docState2.copy(pages = workingPages2, hasUnsavedChanges = true)

        // B. Then edit text on page 1 of new document (which is at index 1, original P0):
        val editAnnot2 = AnnotationData(
            pageIndex = 1,
            type = AnnotationType.TEXT_REPLACE,
            rect = RectFData(0.1f, 0.1f, 0.8f, 0.2f),
            text = "Replaced Text After Rearrange"
        )
        val pagesAfterEdit = docState2.pages.toMutableList()
        pagesAfterEdit[1] = pagesAfterEdit[1].copy(annotations = listOf(editAnnot2))
        docState2 = docState2.copy(pages = pagesAfterEdit, hasUnsavedChanges = true)

        val exportedFile2 = File(context.cacheDir, "exported_acceptance_test2_reverse_order.pdf")
        val saveSuccess2 = PdfEngine.saveDocumentState(docState2, exportedFile2)
        assertTrue("Save after reverse-order editing must succeed", saveSuccess2)

        val pfd2 = ParcelFileDescriptor.open(exportedFile2, ParcelFileDescriptor.MODE_READ_ONLY)
        val renderer2 = PdfRenderer(pfd2)
        assertEquals(4, renderer2.pageCount)
        val p2Rotated = renderer2.openPage(2)
        assertEquals(800, p2Rotated.width) // Rotated 90°
        assertEquals(600, p2Rotated.height)
        p2Rotated.close()
        renderer2.close()
        pfd2.close()

        // -------------------------------------------------------------
        // Step 7: Run Split on the edited-but-not-yet-saved document
        // -------------------------------------------------------------
        // Split pages 1 to 2 from docState2 (which has 4 pages: [P4, P0, P1(rot), P3] with edits on P0)
        val splitFile = File(context.cacheDir, "exported_acceptance_test_split.pdf")
        val splitSubPages = docState2.pages.subList(0, 2) // [P4, P0 with edits]
        val splitDocState = FolioDocumentState(
            docItem = docState2.docItem,
            sourceFile = docState2.sourceFile,
            pages = splitSubPages,
            hasUnsavedChanges = false
        )
        val splitSaveSuccess = PdfEngine.saveDocumentState(splitDocState, splitFile)
        assertTrue("Split must save successfully", splitSaveSuccess)

        val splitPfd = ParcelFileDescriptor.open(splitFile, ParcelFileDescriptor.MODE_READ_ONLY)
        val splitRenderer = PdfRenderer(splitPfd)
        assertEquals("Split output must contain exactly 2 pages", 2, splitRenderer.pageCount)
        splitRenderer.close()
        splitPfd.close()

        assertTrue("Split document must pass verification", PdfEngine.verifyExportedDocument(splitFile, splitDocState))
    }
}
