package com.example.zephyr_bbb.owner

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
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.zephyr_bbb.database.model.Reservation
import com.example.zephyr_bbb.database.repository.CampsiteRepository
import com.example.zephyr_bbb.database.repository.ReservationRepository
import com.example.zephyr_bbb.database.repository.UserRepository
import java.text.SimpleDateFormat
import java.util.Date

/**
 * A composable function that creates a screen for users with owner permissions to look at the reservations
 * that they have either ongoing, in the past or canceled reservations. Shows the screen for both the
 * list view and individual view
 *
 * I Know that this function is very long. I just do not know where I should have split it as there are
 * really only 2 screens that it is showing.
 *
 * @param onBackClick is invoked when the user clicks the back button
 * @param onMessageClick is invoked when the user clicks the MessageEntrant button
 */
@Suppress("SimplifyBooleanWithConstants")
@Composable
fun OwnerMyReservationsScreen(
    onBackClick: () -> Unit,
    onMessageClick: (String) -> Unit
) {
    val reservationRepository = remember {
        ReservationRepository()
    }
    var reservations by remember {
        mutableStateOf<List<Reservation>>(emptyList())
    }
    var isLoading by remember {
        mutableStateOf(true)
    }
    var errorMessage by remember {
        mutableStateOf<String?>(null)
    }
    var expanded by remember {
        mutableStateOf(false)
    }
    var typeOfReservation by remember {
        mutableStateOf("")
    }
    val timeStamp: String = SimpleDateFormat("yyyy-MM-dd").format(Date())
    var showRejectionDialog by remember{
        mutableStateOf(false)
    }
    var selectedReservation by remember {
        mutableStateOf<Reservation?>(null)
    }
    val campsiteRepository = remember {
        CampsiteRepository()
    }
    var campsiteName by remember{
        mutableStateOf("")
    }
    var campsiteLocation by remember{
        mutableStateOf("")
    }
    var campsiteRules by remember{
        mutableStateOf("")
    }
    val userRepository = remember {
        UserRepository()
    }
    var entrantName by remember{
        mutableStateOf("")
    }
    var showDates by remember{
        mutableStateOf(false)
    }
    var showReservations by remember{
        mutableStateOf(true)
    }
    var showSelectedReservation by remember{
        mutableStateOf(false)
    }
    var acceptedOrNot by remember{
        mutableStateOf("")
    }
    LaunchedEffect(showReservations) {
        if (showReservations == true) {
            reservationRepository.getOwnerReservations(
                onSuccess = { list ->
                    reservations = list
                    isLoading = false
                },
                onFailure = { error ->
                    errorMessage = error.message
                    isLoading = false
                }
            )
        }
    }
    // Shows a screen when you click view details on a reservation
    if(selectedReservation != null) {
        showSelectedReservation = true
    }
    // Creates a screen for
    if(showSelectedReservation == true){
        showReservations = false
        val reservation = selectedReservation!!
        // Used to get info about the campsite
        LaunchedEffect(reservation.entrantId, reservation.campsiteId) {
            campsiteRepository.getCampsite(
                campsiteId = reservation.campsiteId,
                onSuccess = { campsite ->
                    if (campsite != null) {
                        campsiteName = campsite.name
                        campsiteLocation = campsite.locationName
                        campsiteRules = campsite.rules
                    }
                    isLoading = false
                },
                onFailure = { error ->
                    errorMessage = error.message
                    isLoading = false
                }
            )
            // Used to get info about the user
            userRepository.getUser(
                userId = reservation.entrantId,
                onSuccess = { user ->
                    if (user != null) {
                        entrantName = user.name
                    }
                    isLoading = false
                },
                onFailure = { error ->
                    errorMessage = error.message
                    isLoading = false
                }
            )
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Back Button
            Button(
                onClick = {
                    showSelectedReservation = false
                    selectedReservation = null
                    showReservations = true
                }
            ) {
                Text("<")
            }
            // Prints the reservation id and status of the reservation
            Text("${reservation.id} - ${reservation.status}")
            // Prints Campsite
            Text(
                text = "Campsite",
                color = Color(0xFF6E757C),
                fontSize = 13.sp,
            )
            // Prints the name of the campsite
            Text(campsiteName)
            // Prints Entrant
            Text(
                text = "Entrant",
                color = Color(0xFF6E757C),
                fontSize = 13.sp,
            )
            // Prints the entrants name
            Text(entrantName)
            // A Row for the check in and out times
            Row(
                modifier = Modifier
                    .fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(40.dp)

            ){
                Column(
                    modifier = Modifier,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Check In",
                        color = Color(0xFF6E757C),
                        fontSize = 13.sp,
                    )
                    Text(reservation.checkInDate)
                }
                Column(
                    modifier = Modifier,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Check Out",
                        color = Color(0xFF6E757C),
                        fontSize = 13.sp,
                    )
                    Text(reservation.checkOutDate)
                }
            }
            if (reservation.acceptedPolicyVersion == 1L){
                acceptedOrNot = "Accepted"
            }
            else acceptedOrNot = "Not yet accepted"
            // Prints Rules Acknowledged and whether they were
            Text("Rules Acknowledged: $acceptedOrNot")
            // Prints the rules of the campsite
            Text(campsiteRules)
            // Prints accepted by then the entrants name
            Text(
                text = "Accepted by $entrantName",
                color = Color(0xFF6E757C),
                fontSize = 13.sp,
            )
            // Makes the timestamp into a readable date that is a string
            val acceptedAt = reservation.policiesAcceptedAt?.toDate()
            Text("${SimpleDateFormat("yyyy-MM-dd - HH:mm").format(acceptedAt)}")
            Text("Rules acknowledgement is required before confirming a reservation")
            // Button to reject the reservation
            if(typeOfReservation == "Active" || (typeOfReservation == "" && reservation.status == "confirmed")) {
                Button(
                    onClick = {
                        showRejectionDialog = true
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Reject Reservation")
                }
            }
            // Button to message the entrant
            Button(
                onClick = {
                    onMessageClick(reservation.id)
                },
                modifier = Modifier.fillMaxWidth()
            ){
                Text("Message Entrant")
            }
        }
        // Shows a notification if you press reject reservation
        if (showRejectionDialog == true){
            AlertDialog(
                onDismissRequest = {showRejectionDialog = false},
                title = {Text("Reject Reservation?")},
                text = {
                    Text("Rejecting cancels this reservation and releases the booked nights, the entrant will be notified")
                },
                confirmButton = {
                    Button(
                        onClick = {
                            reservationRepository.rejectReservation(
                                reservationId = reservation.id,
                                reason = "",
                                onSuccess = {
                                    showRejectionDialog = false
                                    selectedReservation = null
                                    showReservations = true
                                    showSelectedReservation = false
                                },
                                onFailure = {errorMessage = it.message}
                            )
                        }
                    ){
                        Text("Reject")
                    }
                },
                dismissButton = {
                    Button(
                        onClick = {
                            showRejectionDialog = false
                        }
                    ){
                        Text("Cancel")
                    }
                }
            )
        }
    }

    // Shows Booked Dates for All campgrounds
    if (showDates == true){
        showReservations = false
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // Back Button
                Button(
                    onClick = {
                        showDates = false
                        showReservations = true
                    },
                    modifier = Modifier.padding(horizontal = 10.dp)
                ) {
                    Text("<")
                }
                Text("Booked Dates")
            }
            if (reservations.isEmpty()) {
                Text("No Current Booked Dates")
            }
            // Prints the information of the selected reservation
            LazyColumn {
                items(reservations) { reservation ->
                    val checkInDate = (reservation.checkInDate).split("-")
                    val checkOutDate = (reservation.checkOutDate).split("-")
                    var entrantName by remember(reservation.id) {
                        mutableStateOf("")
                    }
                    var campsiteName by remember(reservation.id) {
                        mutableStateOf("")
                    }
                    LaunchedEffect(reservation.entrantId, reservation.campsiteId) {
                        userRepository.getUser(
                            userId = reservation.entrantId,
                            onSuccess = { user ->
                                if (user != null) {
                                    entrantName = user.name
                                }
                                isLoading = false
                            },
                            onFailure = { error ->
                                errorMessage = error.message
                                isLoading = false
                            }
                        )
                        campsiteRepository.getCampsite(
                            campsiteId = reservation.campsiteId,
                            onSuccess = { campsite ->
                                if (campsite != null) {
                                    campsiteName = campsite.name
                                }
                                isLoading = false
                            },
                            onFailure = { error ->
                                errorMessage = error.message
                                isLoading = false
                            }
                        )
                    }
                    val checkInMonthNum = checkInDate[1]
                    val checkOutMonthNum = checkOutDate[1]
                    val checkInMonth = numToMonth(checkInDate[1])
                    val checkOutMonth = numToMonth(checkOutDate[1])
                    if (checkInMonthNum != checkOutMonthNum) {
                        Text(
                            "${numToMonth(checkInMonth)} ${checkInDate[2]},${checkInDate[0]} - ${numToMonth(checkOutMonth)} ${checkOutDate[2]},${checkOutDate[0]}" +
                                    " - $entrantName - $campsiteName"
                        )
                    } else {
                        Text("${numToMonth(checkInMonth)} ${checkInDate[2]},${checkInDate[0]} - ${checkOutDate[2]},${checkInDate[0]} - $entrantName - $campsiteName")

                    }
                    HorizontalDivider(modifier = Modifier.padding(top = 8.dp))
                }
            }
        }
    }
    // Prints all the reservations that the owner has
    if(showReservations == true) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    onClick = onBackClick
                ) {
                    Text("<")
                }
                Text("Reservations")
                // Creates a dropdown menu for selecting what type of reservation you want to see
                // Knowledge of DropdownMenu came from Android Developers - Cited in SethCitations.txt
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    DropdownMenuItem(
                        text = {Text("All Reservations")},
                        onClick = {
                            typeOfReservation = ""
                            expanded = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Active Reservations") },
                        onClick = {
                            typeOfReservation = "Active"
                            expanded = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Past Reservations") },
                        onClick = {
                            typeOfReservation = "Past"
                            expanded = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Cancelled Reservations") },
                        onClick = {
                            typeOfReservation = "Cancelled"
                            expanded = false
                        }
                    )

                }
            }
            // Button that makes that pulls up the menu
            Button(
                onClick = { expanded = !expanded },
                modifier = Modifier.fillMaxWidth()
            ) {
                if (typeOfReservation == "") {
                    Text("Type of Reservation")
                }
                if (typeOfReservation == "Active") {
                    Text("Active Reservations")
                }
                if (typeOfReservation == "Past") {
                    Text("Past Reservations")
                }
                if (typeOfReservation == "Cancelled") {
                    Text("Cancelled Reservations")
                }
            }

            // Puts all the reservation items on the screen
            LazyColumn(
                modifier = Modifier.weight(1f)
            ) {
                items(reservations) { reservation ->
                    var entrantName by remember(reservation.id) {
                        mutableStateOf("")
                    }
                    var campsiteName by remember(reservation.id) {
                        mutableStateOf("")
                    }
                    LaunchedEffect(reservation.entrantId, reservation.campsiteId) {
                        userRepository.getUser(
                            userId = reservation.entrantId,
                            onSuccess = { user ->
                                if (user != null) {
                                    entrantName = user.name
                                }
                                isLoading = false
                            },
                            onFailure = { error ->
                                errorMessage = error.message
                                isLoading = false
                            }
                        )
                        campsiteRepository.getCampsite(
                            campsiteId = reservation.campsiteId,
                            onSuccess = { campsite ->
                                if (campsite != null) {
                                    campsiteName = campsite.name
                                }
                                isLoading = false
                            },
                            onFailure = { error ->
                                errorMessage = error.message
                                isLoading = false
                            }
                        )
                    }
                    // Knowledge of time stamp came from Android Developers and Stack Overflow
                    // Cited in SethCitations.txt
                    val datePast = checkOutDateChecker(reservation.checkOutDate, timeStamp)
                    // If reservation is rejected or canceled
                    if ((reservation.status == "rejected" || reservation.status == "cancelled") && typeOfReservation == "Cancelled") {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = campsiteName,
                                fontSize = 18.sp,
                            )
                            Text(
                                text = entrantName,
                                fontSize = 14.sp,
                            )
                            Text(
                                text = "${reservation.checkInDate}-${reservation.checkOutDate}",
                                fontSize = 14.sp
                            )
                            Text(
                                text = reservation.status,
                                fontSize = 12.sp,
                                color = Color(0xFF707572)
                            )
                            Button(
                                onClick = {
                                    selectedReservation = reservation
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
                    // If date has not yet passed
                    if (reservation.status == "confirmed" && datePast == true && typeOfReservation == "Active") {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = campsiteName,
                                fontSize = 18.sp,
                            )
                            Text(
                                text = entrantName,
                                fontSize = 14.sp,
                            )
                            Text(
                                text = "${reservation.checkInDate}-${reservation.checkOutDate}",
                                fontSize = 14.sp
                            )
                            Text(
                                text = reservation.status,
                                fontSize = 12.sp,
                                color = Color(0xFF707572)
                            )
                            Button(
                                onClick = {
                                    selectedReservation = reservation
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
                    // If date has past
                    if (reservation.status == "confirmed" && datePast == false && typeOfReservation == "Past") {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = campsiteName,
                                fontSize = 18.sp,
                            )
                            Text(
                                text = entrantName,
                                fontSize = 14.sp,
                            )
                            Text(
                                text = "${reservation.checkInDate}-${reservation.checkOutDate}",
                                fontSize = 14.sp
                            )
                            Text(
                                text = reservation.status,
                                fontSize = 12.sp,
                                color = Color(0xFF707572)
                            )
                            Button(
                                onClick = {
                                    selectedReservation = reservation
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
                    // If you want to see all the reservations again
                    if(typeOfReservation == ""){
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = campsiteName,
                                fontSize = 18.sp,
                            )
                            Text(
                                text = entrantName,
                                fontSize = 14.sp,
                            )
                            Text(
                                text = "${reservation.checkInDate}-${reservation.checkOutDate}",
                                fontSize = 14.sp
                            )
                            Text(
                                text = reservation.status,
                                fontSize = 12.sp,
                                color = Color(0xFF707572)
                            )
                            Button(
                                onClick = {
                                    selectedReservation = reservation
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
            // Text for if there are no reservations
            if (reservations.isEmpty()) {
                if (typeOfReservation == "Active") {
                    Text("No Active Reservations")
                }
                if (typeOfReservation == "Past") {
                    Text("No Past Reservations")
                }
                if (typeOfReservation == "Cancelled") {
                    Text("No Cancelled Reservations")
                }
            }
            Button(
                onClick = {
                    showDates = true
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("View Booked Dates")
            }
        }
    }
}


/**
 * Compares a checkout date and the current date and returns true or false depending on if the checkout date
 * has already past
 *
 * @param checkOutDate a string that is in the format of "yyyy-MM-DD" that represents the date you
 * want to compare
 * @param currentDate a string that is in the format of "yyyy-MM-DD"  that represents the current date
 * @return "true" if the current date is before the checkout date, or "false" if the current date is after the checkout date
 */
private fun checkOutDateChecker(
    // Returns false if the currentdate is after the checkoutdate
    // Returns true if the currentdate is before the checkoutdate
    checkOutDate: String,
    currentDate: String
):Boolean {
    val date1 = checkOutDate.split("-")
    val date2 = currentDate.split("-")

    val year1 = date1[0].toInt()
    val year2 = date2[0].toInt()
    val month1 = date1[1].toInt()
    val month2 = date2[1].toInt()
    val day1 = date1[2].toInt()
    val day2 = date2[2].toInt()

    if (year1 < year2) {
        // 2025<2026
        return false
    }
    if (year1 == year2) {
        //2026==2026
        if (month1 < month2) {
            //04<08
            return false
        }
        if (month1 > month2) {
            //04>01
            return true
        }
        if (month1 == month2) {
            //04==04
            if (day1 <= day2) {
                //13<14 or 13==13
                return false
            }
            if (day1 > day2) {
                // 12>11
                return true
            }
        }
    }
    if (year1 > year2) {
        return true
    }
    return true
}

/**
 * Converts a number that is in a string that represents a month, into a string with the month's name in it
 *
 * @param numToMon the string of the month represented in a number that you want to change into a string
 * @return a string that is a month
 */
private fun numToMonth(
    numToMon: String
):String{
    if (numToMon == "01"){
        return "January"
    }
    if (numToMon == "02"){
        return "February"
    }
    if (numToMon == "03"){
        return "March"
    }
    if (numToMon == "04"){
        return "April"
    }
    if (numToMon == "05"){
        return "May"
    }
    if (numToMon == "06"){
        return "June"
    }
    if (numToMon == "07"){
        return "July"
    }
    if (numToMon == "08"){
        return "August"
    }
    if (numToMon == "09"){
        return "September"
    }
    if (numToMon == "10"){
        return "October"
    }
    if (numToMon == "11"){
        return "November"
    }
    else return "December"
}