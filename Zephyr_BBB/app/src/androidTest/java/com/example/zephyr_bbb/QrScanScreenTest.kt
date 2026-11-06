package com.example.zephyr_bbb

import androidx.compose.material3.Text
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.zephyr_bbb.entrant.CAMERA_PERMISSION_NEEDED
import com.example.zephyr_bbb.entrant.NOT_A_CAMPSITE_CODE
import com.example.zephyr_bbb.entrant.QrScanContent
import com.example.zephyr_bbb.entrant.decodeQrText
import com.example.zephyr_bbb.ui.theme.generateQRCode
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

// Tests for QrScanner.kt and QrDecoder.kt
// The screen is shown without a real camera, using a stand-in for the camera view.
// The last test draws a QR code with the owner side's generateQRCode and checks the scanner's
// reader gets the same campsite ID back

@RunWith(AndroidJUnit4::class)
class QrScanScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private fun show(
        hasPermission: Boolean = true,
        message: String? = null,
        onBackClick: () -> Unit = {},
        onAllowCameraClick: () -> Unit = {}
    ) {
        composeTestRule.setContent {
            QrScanContent(
                hasPermission = hasPermission,
                message = message,
                onBackClick = onBackClick,
                onAllowCameraClick = onAllowCameraClick,
                camera = { Text("Camera preview") }
            )
        }
    }

    @Test
    fun showsTitleInstructionAndCamera() {
        show()
        composeTestRule.onNodeWithText("Scan QR Code").assertIsDisplayed()
        composeTestRule.onNodeWithText("Point the camera at a campsite's QR code.").assertIsDisplayed()
        composeTestRule.onNodeWithText("Camera preview").assertIsDisplayed()
    }

    @Test
    fun withoutPermissionShowsAllowButtonInsteadOfCamera() {
        show(hasPermission = false)
        composeTestRule.onNodeWithText("Allow Camera").assertIsDisplayed()
        composeTestRule.onNodeWithText("Camera preview").assertDoesNotExist()
    }

    @Test
    fun allowCameraButtonWorks() {
        var clicked = false
        show(hasPermission = false, onAllowCameraClick = { clicked = true })
        composeTestRule.onNodeWithText("Allow Camera").performClick()
        assertTrue(clicked)
    }

    @Test
    fun backButtonWorks() {
        var backClicked = false
        show(onBackClick = { backClicked = true })
        composeTestRule.onNodeWithText("Back").performClick()
        assertTrue(backClicked)
    }

    @Test
    fun showsMessage() {
        show(message = NOT_A_CAMPSITE_CODE)
        composeTestRule.onNodeWithText(NOT_A_CAMPSITE_CODE).assertIsDisplayed()
    }

    @Test
    fun showsPermissionMessage() {
        show(hasPermission = false, message = CAMERA_PERMISSION_NEEDED)
        composeTestRule.onNodeWithText(CAMERA_PERMISSION_NEEDED).assertIsDisplayed()
    }

    @Test
    fun readsTheQrCodeTheOwnerSideGenerates() {
        val campsiteId = "Xk3pQ9mZr7TdLw2VbN5a"
        val bitmap = generateQRCode(campsiteId)
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        // Dark pixels become 0 and light ones 255, like the brightness of a camera frame
        val luminance = ByteArray(pixels.size) { if ((pixels[it] and 0xFF) < 128) 0 else 0xFF.toByte() }
        assertEquals(campsiteId, decodeQrText(luminance, bitmap.width, bitmap.height))
    }
}
