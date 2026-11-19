package com.example.zephyr_bbb

import com.example.zephyr_bbb.entrant.decodeQrText
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

// Unit tests for QrDecoder.kt
// Makes real QR codes with zxing (the library the owner uses) and checks they are read back,
// the way a camera frame of brightness values would be

class QrDecoderTest {

    private val campsiteId = "Xk3pQ9mZr7TdLw2VbN5a"

    // A QR code as brightness bytes: black is 0 white is 255, with a white border around it
    private fun qrFrame(text: String, size: Int = 300, rowPadding: Int = 0): Triple<ByteArray, Int, Int> {
        val hints = mapOf(EncodeHintType.MARGIN to 4)
        val matrix = QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, size, size, hints)
        val rowBytes = size + rowPadding
        val bytes = ByteArray(rowBytes * size) { 0xFF.toByte() }
        for (y in 0 until size) {
            for (x in 0 until size) {
                if (matrix[x, y]) bytes[y * rowBytes + x] = 0
            }
        }
        return Triple(bytes, rowBytes, size)
    }

    @Test
    fun readsACampsiteIdQrCode() {
        val (bytes, rowBytes, size) = qrFrame(campsiteId)
        assertEquals(campsiteId, decodeQrText(bytes, rowBytes, size))
    }

    @Test
    fun readsAQrCodeWhenRowsArePadded() {
        // Camera images having extra bytes at the end of every row
        val (bytes, rowBytes, size) = qrFrame(campsiteId, rowPadding = 24)
        assertEquals(campsiteId, decodeQrText(bytes, rowBytes, size, width = size, height = size))
    }

    @Test
    fun readsAWebAddressToo() {
        // The reader returns whatever text is there rejecting it is done by campsiteIdFromScan
        val (bytes, rowBytes, size) = qrFrame("https://example.com/menu")
        assertEquals("https://example.com/menu", decodeQrText(bytes, rowBytes, size))
    }

    @Test
    fun readsASmallerQrCode() {
        val (bytes, rowBytes, size) = qrFrame(campsiteId, size = 120)
        assertEquals(campsiteId, decodeQrText(bytes, rowBytes, size))
    }

    @Test
    fun blankFrame_hasNoCode() {
        val bytes = ByteArray(200 * 200) { 0xFF.toByte() }
        assertNull(decodeQrText(bytes, 200, 200))
    }

    @Test
    fun noise_hasNoCode() {
        val random = java.util.Random(7)
        val bytes = ByteArray(200 * 200).also { random.nextBytes(it) }
        assertNull(decodeQrText(bytes, 200, 200))
    }

    @Test
    fun wrongFrameSize_hasNoCodeAndDoesNotCrash() {
        val (bytes, _, _) = qrFrame(campsiteId)
        assertNull(decodeQrText(bytes, dataWidth = 10, dataHeight = 10, width = 500, height = 500))
    }
}
