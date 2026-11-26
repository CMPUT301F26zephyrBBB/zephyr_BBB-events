package com.example.zephyr_bbb.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.zephyr_bbb.database.model.Campsite
import com.example.zephyr_bbb.database.model.Rating
import com.example.zephyr_bbb.database.model.Review
import com.example.zephyr_bbb.database.repository.AdminRepository
import com.example.zephyr_bbb.database.repository.RatingRepository
import com.example.zephyr_bbb.database.repository.ReviewRepository
import com.example.zephyr_bbb.database.repository.UserRepository

// admin screen for browsing and removing campsites, and their ratings and reviews
// outstanding: details page does not show amenities, tags, rules, or owner, lottery and qr buttons yet

/**
 * Lets an admin browse all registered campsites, view one, and remove it.
 * The details page also lists the campsite's ratings and reviews,
 * so an admin can remove any that violate app policy.
 * Removing a campsite also updates any lottery it was listed in.
 *
 * @param onBackClick called when the admin presses back on the campsite list
 */
@Composable
fun AdminCampsiteScreen(
    onBackClick: () -> Unit
) {

    val adminRepository = remember {
        AdminRepository()
    }

    val ratingRepository = remember {
        RatingRepository()
    }

    val reviewRepository = remember {
        ReviewRepository()
    }

    // state used for campsite data and errors
    var campsites by remember {
        mutableStateOf<List<Campsite>>(emptyList())
    }

    var selectedCampsite by remember {
        mutableStateOf<Campsite?>(null)
    }

    var campsiteToRemove by remember {
        mutableStateOf<Campsite?>(null)
    }

    var isLoading by remember {
        mutableStateOf(true)
    }

    var errorMessage by remember {
        mutableStateOf<String?>(null)
    }

    // state used for the selected campsite's ratings and reviews
    var ratings by remember {
        mutableStateOf<List<Rating>>(emptyList())
    }

    var reviews by remember {
        mutableStateOf<List<Review>>(emptyList())
    }

    var ratingToRemove by remember {
        mutableStateOf<Rating?>(null)
    }

    var reviewToRemove by remember {
        mutableStateOf<Review?>(null)
    }

    // user id to name, so ratings and reviews show who wrote them
    var userNames by remember {
        mutableStateOf<Map<String, String>>(emptyMap())
    }

    // load all registered campsites
    fun loadCampsites() {
        isLoading = true
        errorMessage = null

        adminRepository.getCampsites(
            onSuccess = { result ->
                campsites = result
                isLoading = false
            },
            onFailure = { error ->
                errorMessage = error.message
                isLoading = false
            }
        )
    }

    // load names for all active profiles
    fun loadUserNames() {
        adminRepository.getUsers(
            onSuccess = { result ->
                userNames = result.associate { user ->
                    user.id to user.name.ifEmpty { "Unnamed User" }
                }
            },
            onFailure = { error ->
                errorMessage = error.message
            }
        )
    }

    // load the ratings and reviews for one campsite
    fun loadFeedback(campsiteId: String) {
        ratingRepository.getRatings(
            campsiteId = campsiteId,
            onSuccess = { result ->
                ratings = result
            },
            onFailure = { error ->
                errorMessage = error.message
            }
        )

        reviewRepository.getReviews(
            campsiteId = campsiteId,
            onSuccess = { result ->
                reviews = result
            },
            onFailure = { error ->
                errorMessage = error.message
            }
        )
    }

    // name to show for a rating or review, falls back to the id
    fun authorName(entrantId: String): String {
        return userNames[entrantId] ?: entrantId
    }

    // sign in first, then load admin data
    LaunchedEffect(Unit) {
        UserRepository().getOrCreateCurrentUser(
            onSuccess = {
                loadCampsites()
                loadUserNames()
            },
            onFailure = { error ->
                errorMessage = error.message
                isLoading = false
            }
        )
    }

    // load ratings and reviews whenever a campsite is opened
    LaunchedEffect(selectedCampsite?.id) {
        ratings = emptyList()
        reviews = emptyList()

        selectedCampsite?.let { campsite ->
            loadFeedback(campsite.id)
        }
    }

    // confirmation before removing a campsite
    campsiteToRemove?.let { campsite ->

        AlertDialog(
            onDismissRequest = {
                campsiteToRemove = null
            },
            title = {
                Text("Remove Campsite?")
            },
            text = {
                Text(
                    "Removing this campsite will delete the listing from the system."
                )
            },
            confirmButton = {
                Button(
                    onClick = {

                        isLoading = true
                        errorMessage = null

                        adminRepository.removeCampsite(
                            campsiteId = campsite.id,
                            onSuccess = {
                                campsiteToRemove = null
                                selectedCampsite = null
                                loadCampsites()
                            },
                            onFailure = { error ->
                                campsiteToRemove = null
                                errorMessage = error.message
                                isLoading = false
                            }
                        )
                    }
                ) {
                    Text("Remove")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        campsiteToRemove = null
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    // confirmation before removing a rating
    ratingToRemove?.let { rating ->

        AlertDialog(
            onDismissRequest = {
                ratingToRemove = null
            },
            title = {
                Text("Remove Rating?")
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Rated by: ${authorName(rating.id)} · Score: ${rating.score}")
                    Text("Removing this rating will delete it and update the campsite's average.")
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        errorMessage = null

                        selectedCampsite?.let { campsite ->

                            // rating documents are stored under the entrant's id
                            adminRepository.removeRating(
                                campsiteId = campsite.id,
                                entrantId = rating.id,
                                onSuccess = {
                                    ratingToRemove = null
                                    loadFeedback(campsite.id)
                                },
                                onFailure = { error ->
                                    ratingToRemove = null
                                    errorMessage = error.message
                                }
                            )
                        }
                    }
                ) {
                    Text("Remove")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        ratingToRemove = null
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    // confirmation before removing a review
    reviewToRemove?.let { review ->

        AlertDialog(
            onDismissRequest = {
                reviewToRemove = null
            },
            title = {
                Text("Remove Review?")
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Written by: ${authorName(review.id)}")
                    Text("Removing this review will delete it from the database.")
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        errorMessage = null

                        selectedCampsite?.let { campsite ->

                            // review documents are stored under the entrant's id
                            adminRepository.removeReview(
                                campsiteId = campsite.id,
                                entrantId = review.id,
                                onSuccess = {
                                    reviewToRemove = null
                                    loadFeedback(campsite.id)
                                },
                                onFailure = { error ->
                                    reviewToRemove = null
                                    errorMessage = error.message
                                }
                            )
                        }
                    }
                ) {
                    Text("Remove")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        reviewToRemove = null
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    // show selected campsite details, ratings and reviews
    selectedCampsite?.let { campsite ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {

            Button(
                onClick = {
                    selectedCampsite = null
                }
            ) {
                Text("Back")
            }

            Text("Campsite Details")

            Text("Name: ${campsite.name}")
            Text("Location: ${campsite.locationName}")
            Text("Maximum occupancy: ${campsite.maxOccupancy}")
            Text("Status: ${campsite.status}")

            // show errors on the details page too
            errorMessage?.let { error ->
                Text("Error: $error")
            }

            Button(
                onClick = {
                    campsiteToRemove = campsite
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Remove Campsite")
            }

            HorizontalDivider(
                modifier = Modifier.padding(top = 8.dp)
            )

            // ratings for this campsite, average worked out from the list
            if (ratings.isEmpty()) {
                Text("Ratings (0)")
                Text("No ratings yet")
            } else {
                val average = ratings.map { it.score }.average()
                Text("Ratings (${ratings.size}) · ${"%.1f".format(average)} average")
            }

            ratings.forEach { rating ->

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(stars(rating.score))
                        Text(authorName(rating.id))
                    }

                    OutlinedButton(
                        onClick = {
                            ratingToRemove = rating
                        }
                    ) {
                        Text("Remove")
                    }
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(top = 8.dp)
            )

            // written reviews for this campsite
            Text("Reviews (${reviews.size})")

            if (reviews.isEmpty()) {
                Text("No reviews yet")
            }

            reviews.forEach { review ->

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(authorName(review.id))
                        Text("\"${review.text}\"")
                    }

                    OutlinedButton(
                        onClick = {
                            reviewToRemove = review
                        }
                    ) {
                        Text("Remove")
                    }
                }
            }
        }

        return
    }

    // display all registered campsites
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(16.dp)
    ) {

        Button(
            onClick = onBackClick
        ) {
            Text("Back")
        }

        Text(
            text = "All Campsites",
            modifier = Modifier.padding(vertical = 12.dp)
        )

        errorMessage?.let { error ->
            Text(
                text = "Error: $error",
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }

        if (isLoading && campsites.isEmpty()) {
            CircularProgressIndicator(
                modifier = Modifier.padding(top = 16.dp)
            )
        }

        LazyColumn {
            items(campsites) { campsite ->

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {

                    Text(campsite.name)
                    Text(campsite.locationName)

                    Button(
                        onClick = {
                            selectedCampsite = campsite
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("View Details")
                    }

                    HorizontalDivider(
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        }
    }
}

/**
 * Turns a 1–5 score into filled and empty stars, e.g. 3 becomes "★★★☆☆".
 * Scores outside 0 to 5 are clamped so the row always has five stars.
 * Therefore, anything before 0 becomes 0, anything after 5 becomes 5.
 *
 * @param score the rating score
 * @return five star characters
 */
fun stars(score: Long): String {
    val filled = score.coerceIn(0L, 5L).toInt()
    return "★".repeat(filled) + "☆".repeat(5 - filled)
}