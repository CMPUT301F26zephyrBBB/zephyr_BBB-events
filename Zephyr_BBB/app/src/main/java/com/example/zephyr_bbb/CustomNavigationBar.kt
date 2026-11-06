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
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.zephyr_bbb.admin.AdminCampsiteScreen
import com.example.zephyr_bbb.admin.AdminDashboardScreen
import com.example.zephyr_bbb.admin.AdminImageScreen
import com.example.zephyr_bbb.admin.AdminNotificationLogScreen
import com.example.zephyr_bbb.admin.AdminUserScreen
import com.example.zephyr_bbb.owner.OwnerAddCampsiteScreen
import com.example.zephyr_bbb.owner.OwnerCreateLotteryScreen
import com.example.zephyr_bbb.owner.OwnerDashboardScreen
import com.example.zephyr_bbb.owner.OwnerEditCampsiteScreen
import com.example.zephyr_bbb.owner.OwnerMyCampsitesScreen
import com.example.zephyr_bbb.owner.OwnerMyLotteriesScreen
import com.example.zephyr_bbb.owner.OwnerMyReservationsScreen
import com.example.zephyr_bbb.owner.OwnerNotificationsScreen
import com.example.zephyr_bbb.owner.OwnerEditLotteryScreen
import com.example.zephyr_bbb.owner.OwnerLotteryDetailsScreen


/**
 * Composable function that displays the bottom navigation bar of the app
 * The function manages a main navController nad uses a Scaffold function to create and contain the
 * layout of the navigation bar. The function also handles how a user can move from screen to screen
 *
 * @param modifier Used to adjust the layout behaviours if the scaffold
 */

@Composable
// Creates the menu for going through the screens with the nav bar
fun CustomNavigationBar(
    modifier: Modifier = Modifier
) {
    val navController = rememberNavController()

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val startRoute = NavigationBottomBarItem.items.first().route

    Scaffold(
        modifier = modifier,
        bottomBar = {

            // only show bottom navigation on the main screens
            if (currentRoute in NavigationBottomBarItem.items.map { it.route }) {

                NavigationBottomBar(
                    currentRoute = currentRoute,
                    onItemClick = { clickedItem ->
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
        }
    ) { contentPadding ->
        NavHost(
            navController = navController,
            startDestination = startRoute,
            modifier = Modifier.padding(contentPadding)
        ) {
            composable("home") {
                HomeScreen(
                    onAdminClick = {
                        navController.navigate("admin")
                    },
                    onOwnerClick = {
                        navController.navigate("owner")
                    }
                )
            }

            composable("campsites") {
                CampsitesScreen()
            }

            composable("lotteries") {
                LotteriesScreen()
            }

            composable("reservations") {
                ReservationsScreen()
            }

            composable("admin") {
                AdminDashboardScreen(
                    onBackClick = {
                        navController.popBackStack()
                    },
                    onProfilesClick = {
                        navController.navigate("admin_profiles")
                    },
                    onCampsitesClick = {
                        navController.navigate("admin_campsites")
                    },
                    onImagesClick = {
                        navController.navigate("admin_images")
                    },
                    onNotificationsClick = {
                        navController.navigate("admin_notifications")
                    }
                )
            }

            composable("admin_profiles") {
                AdminUserScreen(
                    onBackClick = {
                        navController.popBackStack()
                    }
                )
            }

            composable("admin_campsites") {
                AdminCampsiteScreen(
                    onBackClick = {
                        navController.popBackStack()
                    }
                )
            }

            composable("admin_images") {
                AdminImageScreen(
                    onBackClick = {
                        navController.popBackStack()
                    }
                )
            }

            composable("admin_notifications") {
                AdminNotificationLogScreen(
                    onBackClick = {
                        navController.popBackStack()
                    }
                )
            }

            composable("owner") {
                OwnerDashboardScreen(
                    onBackClick = {
                        navController.popBackStack()
                    },
                    onCampsitesClick = {
                        navController.navigate("owner_campsites")
                    },
                    onLotteriesClick = {
                        navController.navigate("owner_lotteries")
                    },
                    onReservationsClick =  {
                        navController.navigate("owner_reservations")
                    },
                    onNotificationsClick = {
                        navController.navigate("owner_notifications")
                    }
                )
            }

            composable("owner_campsites") {
                OwnerMyCampsitesScreen(
                    onBackClick = {
                        navController.popBackStack()
                    },
                    onAddCampsiteClick = {
                        navController.navigate("owner_add_campsite")
                    },
                    onCampsiteClick = {campsite->
                        navController.navigate("owner_edit_campsite/${campsite.id}")
                    },
                    onCreateLotteryClick = {campsite->
                        navController.navigate("owner_create_lottery?campsiteId=${campsite.id}")
                    }
                )
            }

            composable("owner_lotteries") {
                OwnerMyLotteriesScreen(
                    onBackClick = {
                        navController.popBackStack()
                    },
                    onCreateLotteryClick = {
                        navController.navigate("owner_create_lottery")
                    },
                    onLotteryClick = {lottery->
                        navController.navigate("owner_lottery_details/${lottery.id}")
                    }
                )
            }

            composable(
                route = "owner_create_lottery?campsiteId={campsiteId}",
                arguments = listOf(
                    navArgument("campsiteId") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    }
                )
            ) {backStackEntry->
                OwnerCreateLotteryScreen(
                    preselectedCampsiteId = backStackEntry.arguments?.getString("campsiteId"),
                    onBackClick = {
                        navController.popBackStack()
                    }
                )
            }

            composable("owner_lottery_details/{lotteryId}") { backStackEntry ->
                val lotteryId = backStackEntry.arguments?.getString("lotteryId") ?: ""
                OwnerLotteryDetailsScreen(
                    lotteryId = lotteryId,
                    onBackClick = {
                        navController.popBackStack()
                    },
                    onEditLotteryClick = { id ->
                        navController.navigate("owner_edit_lottery/$id")
                    }
                )
            }

            composable("owner_edit_lottery/{lotteryEditId}") {backStackEntry->
                val lotteryId = backStackEntry.arguments?.getString("lotteryEditId") ?: ""
                OwnerEditLotteryScreen(
                    lotteryEditId = lotteryId,
                    onBackClick = {
                        navController.popBackStack()
                    }
                )
            }

            composable("owner_reservations") {
                OwnerMyReservationsScreen(
                    onBackClick = {
                        navController.popBackStack()
                    }
                )
            }

            composable("owner_notifications") {
                OwnerNotificationsScreen(
                    onBackClick = {
                        navController.popBackStack()
                    }
                )
            }

            composable("owner_add_campsite") {
                OwnerAddCampsiteScreen (
                    onBackClick = {
                        navController.popBackStack()
                    }
                )
            }

            composable("owner_edit_campsite/{campsiteEditId}"){backStackEntry->
                val campsiteId = backStackEntry.arguments?.getString("campsiteEditId") ?: ""
                OwnerEditCampsiteScreen(
                    campsiteEditId = campsiteId,
                    onBackClick = {
                        navController.popBackStack()
                    }
                )
            }
        }
    }
}

/**
 * Data class representing a item within the bottom navigation bar
 *
 * @property label A name applied to the item that appears under the icon that the user sees
 * @property route The destination that the button takes a user to
 * @property icon The image that appears on the bottom navigation bar that the user sees
 */
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

/**
 * Composable function that creates and shows the bottom navigation bar
 * It goes through the collection of NavigationBottomBarItem.items to create and display the individual
 * buttons that are at the bottom of the screen in the navigation bar.
 *
 * @param currentRoute The route of the current bottom navigation button that is pressed or highlighted
 * @param onItemClick A callback that is invoked when a user clicks one of the bottom navigation buttons
 * @param modifier Used to adjust the layout behaviours if the scaffold
 */
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