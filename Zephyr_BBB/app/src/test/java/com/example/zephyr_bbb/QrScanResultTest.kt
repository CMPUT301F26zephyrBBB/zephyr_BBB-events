package com.example.zephyr_bbb

import com.example.zephyr_bbb.entrant.campsiteIdFromScan
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

// Unit tests for QrScanResult.kt
// Checks which scanned QR text is accepted as a campsite ID and which is rejected

class QrScanResultTest {

    // The shape of an id Firestore generates for a campsite
    private val campsiteId = "Xk3pQ9mZr7TdLw2VbN5a"

    @Test
    fun campsiteId_isAccepted() {
        assertEquals(campsiteId, campsiteIdFromScan(campsiteId))
    }

    @Test
    fun surroundingWhitespace_isRemoved() {
        assertEquals(campsiteId, campsiteIdFromScan("  $campsiteId\n"))
    }

    @Test
    fun nullText_isRejected() {
        assertNull(campsiteIdFromScan(null))
    }

    @Test
    fun emptyOrBlankText_isRejected() {
        assertNull(campsiteIdFromScan(""))
        assertNull(campsiteIdFromScan("   "))
    }

    @Test
    fun webAddress_isRejected() {
        assertNull(campsiteIdFromScan("https://example.com/menu"))
    }

    @Test
    fun textWithSlash_isRejected() {
        assertNull(campsiteIdFromScan("campsites/$campsiteId"))
    }

    @Test
    fun textWithSpaces_isRejected() {
        assertNull(campsiteIdFromScan("Pine Creek A12"))
    }

    @Test
    fun veryLongText_isRejected() {
        assertNull(campsiteIdFromScan("a".repeat(500)))
    }
}
