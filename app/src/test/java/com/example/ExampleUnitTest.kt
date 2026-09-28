package com.example

import com.example.engine.AnnotationData
import com.example.engine.AnnotationType
import com.example.engine.CompressionPreset
import com.example.engine.RectFData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun testRectFDataDimensions() {
        val rect = RectFData(left = 10f, top = 20f, right = 110f, bottom = 120f)
        assertEquals(100f, rect.width, 0.01f)
        assertEquals(100f, rect.height, 0.01f)
    }

    @Test
    fun testCompressionPresets() {
        assertEquals("Standard Compression", CompressionPreset.STANDARD.title)
        assertEquals(1.0f, CompressionPreset.STANDARD.scale, 0.01f)
        assertEquals(75, CompressionPreset.STANDARD.jpegQuality)
    }

    @Test
    fun testAnnotationCreation() {
        val annot = AnnotationData(
            pageIndex = 0,
            type = AnnotationType.HIGHLIGHT,
            rect = RectFData(0.1f, 0.1f, 0.5f, 0.2f),
            color = android.graphics.Color.YELLOW
        )
        assertNotNull(annot.id)
        assertEquals(0, annot.pageIndex)
        assertEquals(AnnotationType.HIGHLIGHT, annot.type)
    }
}
