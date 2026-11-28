package com.example.zephyr_bbb

import com.example.zephyr_bbb.entrant.EntrantUtils
import com.example.zephyr_bbb.entrant.ProfileImageUtils
import org.junit.Assert.assertEquals
import org.junit.Test

// unit tests for the profile photo helpers (US 01.08.02)
class ProfileImageUtilsTest {

    // initials for the placeholder avatar

    @Test
    fun initialsUseFirstAndLastName() {
        assertEquals("AL", EntrantUtils.initials("Aidan Lynch"))
    }

    @Test
    fun initialsSkipMiddleNames() {
        assertEquals("AL", EntrantUtils.initials("Aidan James Lynch"))
    }

    @Test
    fun initialsForOneWord() {
        assertEquals("C", EntrantUtils.initials("camper"))
    }

    @Test
    fun initialsIgnoreExtraWhitespace() {
        assertEquals("HC", EntrantUtils.initials("  happy   camper  "))
    }

    @Test
    fun initialsBlankForBlankName() {
        assertEquals("", EntrantUtils.initials(""))
        assertEquals("", EntrantUtils.initials("   "))
    }

    // sample size for decoding big photos

    @Test
    fun sampleSizeIsOneForSmallImages() {
        assertEquals(1, ProfileImageUtils.sampleSizeFor(400, 300, 512))
        assertEquals(1, ProfileImageUtils.sampleSizeFor(512, 512, 512))
    }

    @Test
    fun sampleSizeForTypicalPhonePhoto() {
        // 4000x3000 / 4 = 1000x750, / 8 would make the short side 375 < 512
        assertEquals(4, ProfileImageUtils.sampleSizeFor(4000, 3000, 512))
    }

    @Test
    fun sampleSizeNeverShrinksShortSideBelowMax() {
        val sample = ProfileImageUtils.sampleSizeFor(5000, 600, 512)
        assertEquals(1, sample)
    }

    @Test
    fun sampleSizeIsPowerOfTwo() {
        val sample = ProfileImageUtils.sampleSizeFor(8192, 8192, 512)
        assertEquals(16, sample)
    }

    // final scaled size

    @Test
    fun scaledSizeLeavesSmallImagesAlone() {
        val (width, height) = ProfileImageUtils.scaledSize(300, 200, 512)
        assertEquals(300, width)
        assertEquals(200, height)
    }

    @Test
    fun scaledSizeKeepsLandscapeShape() {
        val (width, height) = ProfileImageUtils.scaledSize(1000, 750, 512)
        assertEquals(512, width)
        assertEquals(384, height)
    }

    @Test
    fun scaledSizeKeepsPortraitShape() {
        val (width, height) = ProfileImageUtils.scaledSize(750, 1000, 512)
        assertEquals(384, width)
        assertEquals(512, height)
    }

    @Test
    fun scaledSizeNeverZero() {
        val (width, height) = ProfileImageUtils.scaledSize(10000, 1, 512)
        assertEquals(512, width)
        assertEquals(1, height)
    }
}
