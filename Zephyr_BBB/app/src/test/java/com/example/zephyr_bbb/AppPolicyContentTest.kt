package com.example.zephyr_bbb

import com.example.zephyr_bbb.entrant.APP_GUIDELINES
import com.example.zephyr_bbb.entrant.APP_POLICY
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

// unit tests for the app policy and guidelines text (US 01.11.01)
class AppPolicyContentTest {

    private val sections = listOf(APP_POLICY, APP_GUIDELINES)

    // the sections have the titles the screen shows
    @Test
    fun sectionTitlesAreCorrect() {
        assertEquals("App Policy", APP_POLICY.title)
        assertEquals("Guidelines", APP_GUIDELINES.title)
    }

    // neither section is empty
    @Test
    fun everySectionHasPoints() {
        sections.forEach { section ->
            assertTrue("${section.title} has no points", section.points.isNotEmpty())
        }
    }

    // no blank bullet points
    @Test
    fun noPointIsBlank() {
        sections.flatMap { it.points }.forEach { point ->
            assertFalse("Found a blank point", point.isBlank())
        }
    }

    // no point is listed twice
    @Test
    fun noDuplicatePoints() {
        val points = sections.flatMap { it.points }
        assertEquals(points.size, points.toSet().size)
    }

    // the policy tells entrants where to turn off notifications (ties to US 01.10.01)
    @Test
    fun policyMentionsHowToTurnOffNotifications() {
        assertTrue(APP_POLICY.points.any { it.contains("notifications") && it.contains("My Profile") })
    }
}
