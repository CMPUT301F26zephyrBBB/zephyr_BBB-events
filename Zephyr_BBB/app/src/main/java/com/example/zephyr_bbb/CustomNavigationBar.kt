package com.example.zephyr_bbb

// Knowledge of the navController and related functions came from Android Studio Developers Documentation
// Used some code from Android Studio Developers about the Navigation Bar documentation
// URL: https://developer.android.com/develop/ui/compose/components/navigation-bar
// Some also came from the GitHub url listed on the webpage which is just the big documentation
// file.
// GitHub URL: https://github.com/android/snippets/blob/main/compose/snippets/src/main/java/com/example/compose/snippets/components/Navigation.kt


import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController

@Composable
// Creates the menu for going through the screens with the nav bar
fun CustomNavigationBar(modifier: Modifier = Modifier) {
    val navController = rememberNavController()

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val startRoute = NavigationBottomBarItem.items.first().route

    Scaffold(
        modifier = modifier,
        bottomBar = {
            NavigationBottomBar(
                currentRoute = currentRoute,
                onItemClick = {clickedItem ->
                    if (currentRoute != clickedItem.route) {
                        navController.navigate(clickedItem.route) {
                            popUpTo(navController.graph.startDestinationId) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                }
            )
        }
    ) {contentPadding ->
        NavHost(
            navController = navController,
            startDestination = startRoute,
            modifier = Modifier.padding(contentPadding)
        ) {
            composable("home") { HomeScreen() }
            composable("campsites") { CampsitesScreen() }
            composable("lotteries") { LotteriesScreen() }
            composable("reservations") { ReservationsScreen() }
        }
    }
}

// Data Class for the individual items in the NavigationBar
data class NavigationBottomBarItem (
    val label: String,
    val route: String,
    val icon: Int,
){

    companion object {
        // List of all the Buttons that are displayed, and their information
        val items = listOf(
            NavigationBottomBarItem("Home", "home", R.drawable.house),
            NavigationBottomBarItem("Campsites", "campsites", R.drawable.tent),
            NavigationBottomBarItem("Lotteries", "lotteries", R.drawable.ticket),
            NavigationBottomBarItem("Reservations", "reservations", R.drawable.calendar),
        )
    }
}

// Creates the navigation bar at the bottom of the screen
@Composable
fun NavigationBottomBar(
    currentRoute: String?,
    onItemClick: (NavigationBottomBarItem) -> Unit,
    modifier: Modifier = Modifier
){
    NavigationBar(
        modifier = modifier,
        windowInsets = NavigationBarDefaults.windowInsets
    ){
        NavigationBottomBarItem.items.forEach{ item ->
            NavigationBarItem(
                selected = currentRoute == item.route,
                onClick = {onItemClick(item)},
                icon = {
                    Icon(
                        painter = painterResource(id = item.icon),
                        contentDescription = item.label
                    )
                },
                label = {Text(item.label)}
            )
        }
    }
}