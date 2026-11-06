package com.example.zephyr_bbb.entrant

import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.ReaderException
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeReader

// Reads the text out of a QR code in one camera frame using zxing, the same library the owner
// side uses to draw the QR code. The camera frame is its brightness values
// Outstanding issues: none known.

private val hints = mapOf(
    DecodeHintType.POSSIBLE_FORMATS to listOf(BarcodeFormat.QR_CODE),
    DecodeHintType.TRY_HARDER to true
)

/**
 * Reads the QR code in one frame of brightness values.
 * The code can be at any angle, but must be fully inside the frame.
 *
 * @param luminance the brightness of every pixel, one byte each, row after row
 * @param dataWidth the number of bytes in one row of [luminance] (the row stride, which can be
 * larger than [width])
 * @param dataHeight the number of rows in [luminance]
 * @param width the width in pixels of the part of the frame to read
 * @param height the height in pixels of the part of the frame to read
 * @return the text in the QR code, or null if the frame has no readable QR code
 */
fun decodeQrText(
    luminance: ByteArray,
    dataWidth: Int,
    dataHeight: Int,
    width: Int = dataWidth,
    height: Int = dataHeight
): String? {
    return try {
        val source = PlanarYUVLuminanceSource(
            luminance, dataWidth, dataHeight, 0, 0, width, height, false
        )
        QRCodeReader().decode(BinaryBitmap(HybridBinarizer(source)), hints).text
    } catch (_: ReaderException) {
        // No QR code was found in this frame, which is normal until the user points at one
        null
    } catch (_: IllegalArgumentException) {
        // The frame size did not match the data
        null
    }
}
