package com.example.zephyr_bbb.entrant

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

/**
 * One titled section of the policy screen, shown as a heading and a bulleted list.
 *
 * @property title the section heading
 * @property points the bullet points under the heading, in order
 */
internal data class PolicySection(
    val title: String,
    val points: List<String>
)

/** The app policy: what the app stores and how its rules work. */
internal val APP_POLICY = PolicySection(
    title = "App Policy",
    points = listOf(
        "Your identity is tied to this device. The app signs you in automatically, " +
            "so there is no username or password.",
        "We store the profile details you enter (name, and optionally email, phone " +
            "number and a profile image), along with your lottery entries, " +
            "reservations, ratings, reviews and messages.",
        "Lotteries are drawn at random once registration closes. Entering does not " +
            "guarantee a spot, and each person can enter a lottery only once.",
        "If you win, you will be notified and must choose your campsite and dates " +
            "to confirm the reservation.",
        "You can turn notifications about lottery results and reservations off or " +
            "on at any time from My Profile.",
        "Messages between entrants and campsite owners are kept with the " +
            "reservation and can be reviewed by administrators.",
        "Administrators may remove profiles, images, campsites, ratings and " +
            "reviews that break these guidelines."
    )
)

/** The guidelines entrants are expected to follow. */
internal val APP_GUIDELINES = PolicySection(
    title = "Guidelines",
    points = listOf(
        "Keep your profile information accurate and up to date.",
        "Only upload images that you own and that are appropriate for everyone.",
        "Be respectful when messaging campsite owners, and keep messages about " +
            "your reservation.",
        "Follow each campsite's posted rules, including quiet hours, fire rules " +
            "and guest limits.",
        "Cancel reservations you can't use so other entrants can book them.",
        "Leave honest ratings and reviews based on your own stay."
    )
)

/**
 * Shows the app policy and guidelines to the entrant. US 01.11.01.
 *
 * This screen has no Firebase access, so it can be shown and tested on its own.
 *
 * @param onBackClick is invoked when the user clicks the back button
 */
@Composable
fun AppPolicyScreen(onBackClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        TextButton(onClick = onBackClick) { Text("Back") }
        Text(
            "App Policy and Guidelines",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.semantics { heading() }
        )

        PolicySectionView(APP_POLICY)
        HorizontalDivider()
        PolicySectionView(APP_GUIDELINES)
    }
}

/**
 * Shows one policy section as a heading followed by its bullet points.
 *
 * @param section the section to show
 */
@Composable
private fun PolicySectionView(section: PolicySection) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            section.title,
            style = MaterialTheme.typography.titleLarge,
            // Lets screen reader users jump between sections
            modifier = Modifier.semantics { heading() }
        )
        section.points.forEach { point ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("•", style = MaterialTheme.typography.bodyMedium)
                Text(point, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
