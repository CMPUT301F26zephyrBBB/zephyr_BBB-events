package com.example.zephyr_bbb

import androidx.test.ext.junit.runners.AndroidJUnit4
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith

// Tests for QRCodeGenerator.kt
// Test that it properly makes a QR Code and that the size is correct

@RunWith(AndroidJUnit4::class)
class QRCodeGeneratorTest {

    @Test
    fun generateQRCodeNotNull(){
        val content = "campsite-1"
        val bitmap = generateQRCode(content)
        assertNotNull("Generated Bitmap for QR Code should not be null", bitmap)
    }

    @Test
    fun generateQRCodeSizeIs512(){
        val content = "campsite-1"
        val bitmap = generateQRCode(content)
        assertEquals(512, bitmap.width)
        assertEquals(512, bitmap.height)
    }
}