package com.example.zephyr_bbb.owner

import android.text.TextUtils.split
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.google.common.math.LinearTransformation.horizontal
import java.text.SimpleDateFormat
import java.util.Date

@Composable
fun OwnerMyReservationsScreen(
    onBackClick: () -> Unit,
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

    LaunchedEffect(Unit) {
        reservationRepository.getOwnerReservations(
            onSuccess = {list->
                reservations = list
                isLoading = false
            },
            onFailure = {error->
                errorMessage = error.message
                isLoading = false
            }
        )
    }

    // Shows a screen when you click view details on a reservation
    if(selectedReservation != null){
        showReservations = false
        val reservation = selectedReservation!!
        // Used to get info about the campsite
        campsiteRepository.getCampsite(
            campsiteId = reservation.campsiteId,
            onSuccess = {campsite->
                if (campsite != null){
                    campsiteName = campsite.name
                    campsiteLocation = campsite.locationName
                    campsiteRules = campsite.rules
                }
                isLoading = false
            },
            onFailure = {error->
                errorMessage = error.message
                isLoading = false
            }
        )
        // Used to get info about the user
        userRepository.getUser(
            userId = reservation.entrantId,
            onSuccess = {user->
                if (user != null){
                    entrantName = user.name
                }
                isLoading = false
            },
            onFailure = {error->
                errorMessage = error.message
                isLoading = false
            }
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Back Button
            Button(
                onClick = {
                    selectedReservation = null
                    showReservations = true
                }
            ) {
                Text("<")
            }

            Text(reservation.id)
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Campsite",
                color = Color(0xFF6E757C),
                fontSize = 13.sp,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(campsiteName)
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Entrant",
                color = Color(0xFF6E757C),
                fontSize = 13.sp,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(entrantName)
            Spacer(modifier = Modifier.height(2.dp))
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .padding(16.dp),
            ){
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Check In",
                        color = Color(0xFF6E757C),
                        fontSize = 13.sp,
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(reservation.checkInDate)
                }
                Spacer(modifier = Modifier.height(2.dp))
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Check Out",
                        color = Color(0xFF6E757C),
                        fontSize = 13.sp,
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(reservation.checkOutDate)
                }
            }
            Text("Rules Acknowledged: ${reservation.acceptedPolicyVersion}")
            Spacer(modifier = Modifier.height(2.dp))
            Text(campsiteRules)
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Accepted by $entrantName",
                color = Color(0xFF6E757C),
                fontSize = 13.sp,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text("${reservation.policiesAcceptedAt}")
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Reservation confirmed on",
                color = Color(0xFF6E757C),
                fontSize = 13.sp,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text("Rules acknowledgement is required before confirming a reservation")

            if(typeOfReservation == "Active" ) {
                Button(
                    onClick = {
                        showRejectionDialog = true
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Reject Reservation")
                }
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

            LazyColumn {
                items(reservations) { reservation ->
                    val checkInDate = (reservation.checkInDate).split("-")
                    val checkOutDate = (reservation.checkOutDate).split("-")
                    var entrantName1 = ""
                    var campsiteName1 = ""
                    userRepository.getUser(
                        userId = reservation.entrantId,
                        onSuccess = { user ->
                            if (user != null) {
                                entrantName1 = user.name
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
                                campsiteName1 = campsite.name
                            }
                            isLoading = false
                        },
                        onFailure = { error ->
                            errorMessage = error.message
                            isLoading = false
                        }
                    )
                    val checkInMonth = checkInDate[1]
                    val checkOutMonth = checkOutDate[1]
                    if (checkInMonth != checkOutMonth) {
                        Text(
                            "$checkInMonth ${checkInDate[2]},${checkInDate[0]} - $checkOutMonth ${checkOutDate[2]},${checkOutDate[0]}" +
                                    " - $entrantName1 - $campsiteName1"
                        )
                    } else {
                        Text("$checkInMonth ${checkInDate[2]} - ${checkOutDate[2]},${checkInDate[0]} - $entrantName1 - $campsiteName1")

                    }
                    HorizontalDivider(modifier = Modifier.padding(top = 8.dp))
                }
            }
        }
    }
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
                    // Knowledge of time stamp came from Android Developers and Stack Overflow
                    // Cited in SethCitations.txt
                    val datePast = checkOutDateChecker(reservation.checkOutDate, timeStamp)
                    Text(reservation.status)
                    // If reservation is rejected or cancelled
                    if ((reservation.status == "rejected" || reservation.status == "cancelled") && typeOfReservation == "Cancelled") {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(reservation.id)
                            Text(reservation.entrantId)
                            Text("${reservation.checkInDate}-${reservation.checkOutDate}")
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
                            Text(reservation.id)
                            Text(reservation.entrantId)
                            Text("${reservation.checkInDate}-${reservation.checkOutDate}")
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
                            Text(reservation.id)
                            Text(reservation.entrantId)
                            Text("${reservation.checkInDate}-${reservation.checkOutDate}")
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
                    } else Text("No Reservations")
                }
            }
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

// Function used to compare two dates
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

// Function to change a number to a month
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