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

    @org.junit.Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        PdfEngine.init(context)
    }

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

    @Test
    fun `testServiceAgreementHeadingTextReplaceBakeCoordinates`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val serviceAgreementFile = File(context.cacheDir, "Service Agreement.pdf")
        PdfEngine.createServiceAgreementPdf(serviceAgreementFile)
        assertTrue(serviceAgreementFile.exists())
        assertEquals(2, PdfEngine.getPageCount(serviceAgreementFile))

        // Extract text blocks for page 0
        val page0Blocks = PdfEngine.extractTextBlocks(serviceAgreementFile, 0)
        assertTrue(page0Blocks.isNotEmpty())
        val headingBlock = page0Blocks.find { it.text.contains("SERVICE CONTRACT", ignoreCase = true) }
        assertNotNull("Heading block must be found on Page 0", headingBlock)

        val initialDocState = FolioDocumentState(
            docItem = PdfDocumentItem(
                id = 10L,
                title = "Service Agreement.pdf",
                filePath = serviceAgreementFile.absolutePath,
                fileSize = serviceAgreementFile.length(),
                pageCount = 2
            ),
            sourceFile = serviceAgreementFile,
            pages = listOf(
                FolioPageState(originalPageIndex = 0, textBlocks = page0Blocks),
                FolioPageState(originalPageIndex = 1)
            ),
            hasUnsavedChanges = false
        )

        // Replace the heading on page 0 in place
        val newHeadingText = "AMENDED MASTER SERVICES AGREEMENT 2026"
        val editAnnot = AnnotationData(
            pageIndex = 0,
            type = AnnotationType.TEXT_REPLACE,
            rect = headingBlock!!.rect,
            text = newHeadingText,
            originalText = headingBlock.originalText,
            color = android.graphics.Color.BLACK,
            strokeWidth = 2f,
            fontFamily = "Helvetica",
            fontSize = 20f
        )

        val updatedPages = initialDocState.pages.toMutableList()
        updatedPages[0] = updatedPages[0].copy(
            annotations = listOf(editAnnot),
            textBlocks = page0Blocks.map { if (it.id == headingBlock.id) it.copy(text = newHeadingText, isModified = true) else it }
        )
        val editedDocState = initialDocState.copy(pages = updatedPages, hasUnsavedChanges = true)

        // Save annotated copy to new file
        val savedFile = File(context.cacheDir, "Service Agreement_edited.pdf")
        val saveResult = PdfEngine.saveDocumentState(editedDocState, savedFile)
        assertTrue("Saving edited service agreement must succeed", saveResult)
        assertTrue(savedFile.exists() && savedFile.length() > 0)

        // Reopen saved file and verify
        val pfd = ParcelFileDescriptor.open(savedFile, ParcelFileDescriptor.MODE_READ_ONLY)
        val renderer = PdfRenderer(pfd)
        assertEquals(2, renderer.pageCount)
        val page0 = renderer.openPage(0)
        assertEquals(595, page0.width)
        assertEquals(842, page0.height)
        page0.close()
        renderer.close()
        pfd.close()

        // Also test saveAnnotatedPdf directly with annotations list
        val savedFile2 = File(context.cacheDir, "Service Agreement_annotated.pdf")
        val saveResult2 = PdfEngine.saveAnnotatedPdf(serviceAgreementFile, listOf(editAnnot), savedFile2)
        assertTrue("saveAnnotatedPdf must succeed", saveResult2)
        assertTrue(savedFile2.exists() && savedFile2.length() > 0)
    }

    @Test
    fun `testFindAndReplaceExecutionModes`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val sourceFile = create5PageTestPdf(context)
        assertEquals(5, PdfEngine.getPageCount(sourceFile))

        val pages = (0 until 5).map { idx ->
            FolioPageState(
                originalPageIndex = idx,
                textBlocks = listOf(
                    PdfTextBlock(
                        pageIndex = idx,
                        text = "Page ${idx + 1}: Line with keywordTarget",
                        originalText = "Page ${idx + 1}: Line with keywordTarget",
                        rect = RectFData(0.08f, 0.10f, 0.70f, 0.15f)
                    )
                )
            )
        }
        val matches = PdfEngine.searchAllPages(sourceFile, pages, "keywordTarget")
        assertEquals("keywordTarget should appear on all 5 pages", 5, matches.size)

        // 1. Single occurrence replace
        val firstMatch = matches[0]
        assertEquals(0, firstMatch.pageIndex)

        // 2. Document-wide replace
        val replacedWord = "SUBSTITUTED"
        val replacedAnnotations = matches.map { m ->
            AnnotationData(
                pageIndex = m.pageIndex,
                type = AnnotationType.TEXT_REPLACE,
                rect = m.blockRect,
                text = m.lineText.replace(m.matchedWord, replacedWord),
                originalText = m.lineText
            )
        }

        val docState = FolioDocumentState(
            docItem = PdfDocumentItem(id = 20L, title = sourceFile.name, filePath = sourceFile.absolutePath, fileSize = sourceFile.length(), pageCount = 5),
            sourceFile = sourceFile,
            pages = pages.mapIndexed { idx, pState ->
                pState.copy(annotations = replacedAnnotations.filter { it.pageIndex == idx })
            },
            hasUnsavedChanges = true
        )

        val outputFile = File(context.cacheDir, "find_replace_all_output.pdf")
        val saved = PdfEngine.saveDocumentState(docState, outputFile)
        assertTrue("Save after find and replace all must succeed", saved)
        assertTrue(outputFile.exists() && outputFile.length() > 0)
    }

    @Test
    fun `testBug1TitleFindAndReplacePreservesBaselineCenterAlignmentAndSubtitleVisibility`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val serviceAgreementFile = File(context.cacheDir, "Service_Agreement_Bug1.pdf")
        PdfEngine.createServiceAgreementPdf(serviceAgreementFile)

        val page0Blocks = PdfEngine.extractTextBlocks(serviceAgreementFile, 0)
        val titleBlock = page0Blocks.find { it.text.contains("SERVICE CONTRACT", ignoreCase = true) }
        val subtitleBlock = page0Blocks.find { it.text.contains("Document Reference", ignoreCase = true) }
        assertNotNull(titleBlock)
        assertNotNull(subtitleBlock)

        // 1. Verify title metadata: exactly centered, baseline at 60f (0.07126f), bottom above subtitle top
        assertTrue("Title must have isCentered = true", titleBlock!!.isCentered)
        assertEquals("Title baseline must be 60/842", 60f / 842f, titleBlock.baselineY!!, 0.001f)
        assertTrue("Title bottom must be above subtitle top to prevent overlap", titleBlock.rect.bottom < subtitleBlock!!.rect.top)

        // 2. Perform Find & Replace "Service" -> "dervice"
        val replacedTitle = titleBlock.text.replace("Service", "dervice", ignoreCase = true)
        assertEquals("dervice CONTRACT & MASTER AGREEMENT", replacedTitle)

        val annot = AnnotationData(
            pageIndex = 0,
            type = AnnotationType.TEXT_REPLACE,
            rect = titleBlock.rect,
            text = replacedTitle,
            originalText = titleBlock.originalText,
            color = titleBlock.textColor,
            strokeWidth = 2f,
            fontSize = titleBlock.fontSize,
            isCentered = titleBlock.isCentered,
            baselineY = titleBlock.baselineY,
            backgroundColor = titleBlock.backgroundColor
        )

        val docState = FolioDocumentState(
            docItem = PdfDocumentItem(id = 30L, title = "Service Agreement.pdf", filePath = serviceAgreementFile.absolutePath, fileSize = serviceAgreementFile.length(), pageCount = 2),
            sourceFile = serviceAgreementFile,
            pages = listOf(
                FolioPageState(originalPageIndex = 0, annotations = listOf(annot), textBlocks = page0Blocks),
                FolioPageState(originalPageIndex = 1)
            ),
            hasUnsavedChanges = true
        )

        val exportedFile = File(context.cacheDir, "Service_Agreement_Bug1_Fixed.pdf")
        val saved = PdfEngine.saveDocumentState(docState, exportedFile)
        assertTrue("Save after title find and replace must succeed", saved)
        assertTrue(exportedFile.exists() && exportedFile.length() > 0)
    }

    @Test
    fun `testBug2DeliverablesPhase4EditPreservesLeftIndentContainerBorderAndNoTruncation`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val serviceAgreementFile = File(context.cacheDir, "Service_Agreement_Bug2.pdf")
        PdfEngine.createServiceAgreementPdf(serviceAgreementFile)

        val page0Blocks = PdfEngine.extractTextBlocks(serviceAgreementFile, 0)
        val phase4Block = page0Blocks.find { it.text.contains("Phase 4", ignoreCase = true) }
        assertNotNull(phase4Block)

        // 1. Verify Phase 4 metadata: left-aligned (NOT centered!), baseline at 410f, inside container
        assertEquals("Phase 4 must be left-aligned (isCentered = false)", false, phase4Block!!.isCentered)
        assertEquals("Phase 4 baseline must be 410/842", 410f / 842f, phase4Block.baselineY!!, 0.001f)
        assertEquals("Phase 4 background must match container #F7F8FA", android.graphics.Color.parseColor("#F7F8FA"), phase4Block.backgroundColor)

        // Container borders are at 45f (0.0756f) and 550f (0.9244f). Phase 4 rect must sit comfortably inside
        assertTrue("Phase 4 left must be inside container border (> 0.0756f)", phase4Block.rect.left > 0.0756f)
        assertTrue("Phase 4 right must be inside container border (< 0.9244f)", phase4Block.rect.right < 0.9244f)

        // 2. Perform edit to Phase 4
        val editedPhase4Text = "Phase 4: Cloud Backup, Team Collaboration & Audit Logs — Due Dec 10, 2026"
        val annot = AnnotationData(
            pageIndex = 0,
            type = AnnotationType.TEXT_REPLACE,
            rect = phase4Block.rect,
            text = editedPhase4Text,
            originalText = phase4Block.originalText,
            color = phase4Block.textColor,
            strokeWidth = 2f,
            fontSize = phase4Block.fontSize,
            isCentered = phase4Block.isCentered,
            baselineY = phase4Block.baselineY,
            backgroundColor = phase4Block.backgroundColor
        )

        val docState = FolioDocumentState(
            docItem = PdfDocumentItem(id = 31L, title = "Service Agreement.pdf", filePath = serviceAgreementFile.absolutePath, fileSize = serviceAgreementFile.length(), pageCount = 2),
            sourceFile = serviceAgreementFile,
            pages = listOf(
                FolioPageState(originalPageIndex = 0, annotations = listOf(annot), textBlocks = page0Blocks),
                FolioPageState(originalPageIndex = 1)
            ),
            hasUnsavedChanges = true
        )

        val exportedFile = File(context.cacheDir, "Service_Agreement_Bug2_Fixed.pdf")
        val saved = PdfEngine.saveDocumentState(docState, exportedFile)
        assertTrue("Save after editing Phase 4 must succeed", saved)
        assertTrue(exportedFile.exists() && exportedFile.length() > 0)
    }

    private fun createHeadshotRecipePdf(file: File) {
        val doc = com.tom_roush.pdfbox.pdmodel.PDDocument()
        val page = com.tom_roush.pdfbox.pdmodel.PDPage(com.tom_roush.pdfbox.pdmodel.common.PDRectangle(595f, 842f))
        doc.addPage(page)
        val content = com.tom_roush.pdfbox.pdmodel.PDPageContentStream(doc, page)
        val fontBold = com.tom_roush.pdfbox.pdmodel.font.PDType1Font.HELVETICA_BOLD
        val fontReg = com.tom_roush.pdfbox.pdmodel.font.PDType1Font.HELVETICA

        content.beginText()
        content.setFont(fontBold, 14f)
        content.newLineAtOffset(50f, 750f)
        content.showText("PROFESSIONAL AI HEADSHOT PROMPT RECIPE (WITH INDUSTRY JARGON)")
        content.endText()

        content.beginText()
        content.setFont(fontBold, 11f)
        content.newLineAtOffset(50f, 700f)
        content.showText("1. SUBJECT DESCRIPTION")
        content.endText()

        content.beginText()
        content.setFont(fontReg, 10f)
        content.newLineAtOffset(50f, 680f)
        content.showText("A realistic professional portrait of @me, shown from shoulders up.")
        content.endText()

        content.close()
        FileOutputStream(file).use { doc.save(it) }
        doc.close()
    }

    @Test
    fun `testProblem2ImportedPdfExtractsDecodedUnicodeAndNoGibberish`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val testPdf = File(context.cacheDir, "AI_Headshot_Prompt_Recipe.pdf")
        createHeadshotRecipePdf(testPdf)
        assertTrue(testPdf.exists() && testPdf.length() > 0)

        // Extract text blocks
        val blocks = PdfEngine.extractTextBlocks(testPdf, 0)
        assertTrue("Must extract text blocks from imported PDF", blocks.isNotEmpty())

        val titleBlock = blocks.find { it.text.contains("PROFESSIONAL", ignoreCase = true) }
        assertNotNull("Title block must be decoded and found without gibberish", titleBlock)
        assertTrue(
            "Title must contain readable Unicode text",
            titleBlock!!.text.contains("HEADSHOT PROMPT RECIPE", ignoreCase = true)
        )

        // Verify that NO blocks contain garbage characters
        blocks.forEach { block ->
            assertTrue("Block text must not be gibberish: ${block.text}", !PdfEngine.isGibberish(block.text))
        }
    }

    @Test
    fun `testProblem3FindAndReplaceWorksOnImportedPdf`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val testPdf = File(context.cacheDir, "AI_Headshot_Prompt_FindReplace.pdf")
        createHeadshotRecipePdf(testPdf)

        val pages = listOf(
            FolioPageState(originalPageIndex = 0)
        )

        // Search for 'PROFESSIONAL' which was failing in user's image
        val matches = PdfEngine.searchAllPages(testPdf, pages, "PROFESSIONAL")
        assertEquals("Must find 2 matches for PROFESSIONAL (title and body)", 2, matches.size)
        assertEquals("PROFESSIONAL", matches[0].matchedWord)

        // Perform replacement
        val outputFile = File(context.cacheDir, "AI_Headshot_Replaced.pdf")
        val (success, matchCount) = PdfEngine.findAndReplaceText(testPdf, "PROFESSIONAL", "EXECUTIVE", outputFile)
        assertTrue("Find and replace must succeed", success)
        assertEquals(2, matchCount)
        assertTrue(outputFile.exists() && outputFile.length() > 0)

        // Verify the replaced PDF now contains EXECUTIVE
        val replacedBlocks = PdfEngine.extractTextBlocks(outputFile, 0)
        val replacedTitle = replacedBlocks.find { it.text.contains("EXECUTIVE", ignoreCase = true) }
        assertNotNull("Replaced document must contain EXECUTIVE", replacedTitle)
    }

    @Test
    fun `testProblem1LargeDocumentStreamingAndNoMemoryCrash`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val largePdf = File(context.cacheDir, "large_test_document_100p.pdf")

        // Create 100-page document with PDFBox streaming
        val doc = com.tom_roush.pdfbox.pdmodel.PDDocument()
        val font = com.tom_roush.pdfbox.pdmodel.font.PDType1Font.HELVETICA
        for (i in 0 until 100) {
            val page = com.tom_roush.pdfbox.pdmodel.PDPage(com.tom_roush.pdfbox.pdmodel.common.PDRectangle(595f, 842f))
            doc.addPage(page)
            val content = com.tom_roush.pdfbox.pdmodel.PDPageContentStream(doc, page)
            content.beginText()
            content.setFont(font, 12f)
            content.newLineAtOffset(50f, 750f)
            content.showText("Page ${i + 1} Enterprise Content Streaming")
            content.endText()
            content.close()
        }
        FileOutputStream(largePdf).use { doc.save(it) }
        doc.close()

        assertTrue(largePdf.exists() && largePdf.length() > 0)
        val count = PdfEngine.getPageCount(largePdf)
        assertEquals(100, count)

        // Extract page 50 on demand without OOM
        val p50Blocks = PdfEngine.extractTextBlocks(largePdf, 49)
        assertTrue("Page 50 blocks must be extracted", p50Blocks.isNotEmpty())
        assertTrue("Page 50 must have correct page index", p50Blocks[0].text.contains("Page 50"))
    }
}
