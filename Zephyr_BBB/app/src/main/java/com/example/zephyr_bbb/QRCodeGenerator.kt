package com.example.zephyr_bbb

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter


// Code for QRCodeGenerator came from stack overflow users "David Ferrand" and "Siddharth Kamaria"
// URL - https://stackoverflow.com/a/64504871
// More info given in the doc folder in the file SethCitations.txt


/**
 * A function that creates a unique QR code
 * Creates a unique QR code for information that is inputted into the content parameter.
 *
 * @param content The information that is to be stored in the QR code
 * @return A bitmap object that contains a QR code
 */
fun generateQRCode(
    content: String,
):Bitmap{
    val size = 512
    val bits = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, size, size)
    return Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565).also{bitmap->
        for (x in 0 until size){
            for (y in 0 until size){
                bitmap.setPixel(x, y, if (bits[x,y]) Color.BLACK else Color.WHITE)
            }
        }
    }
}