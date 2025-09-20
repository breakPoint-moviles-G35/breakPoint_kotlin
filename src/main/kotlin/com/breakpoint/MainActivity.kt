package com.breakpoint

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BreakPointTheme { BreakPointApp() }
        }
    }
}

@Composable
fun BreakPointTheme(content: @Composable () -> Unit) {
    val primary = Color(0xFF5C1B6C)
    val secondary = Color(0xFFAA8CAF)
    val tertiary = Color(0xFFFFFFFF)

    val colorScheme = androidx.compose.material3.lightColorScheme(
        primary = primary,
        onPrimary = tertiary,
        secondary = secondary,
        onSecondary = tertiary,
        tertiary = tertiary,
        onTertiary = Color.Black
    )

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}

@Composable
fun BreakPointApp() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    Scaffold(
        bottomBar = {
            if (currentRoute != Destinations.Login.route) {
                BottomNavigationBar(navController)
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Destinations.Login.route,
            modifier = Modifier.padding(padding)
        ) {
            composable(Destinations.Login.route) {
                LoginScreen(
                    onLoginSuccess = {
                        navController.navigate(Destinations.Explore.route) {
                            popUpTo(Destinations.Login.route) { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                )
            }
            composable(Destinations.Explore.route) { ExploreScreen() }
            composable(Destinations.Rate.route) { RateScreen() }
            composable(Destinations.Reservations.route) { ReservationsScreen() }
            composable(Destinations.DetailedSpace.route) { backStackEntry ->
                val spaceId = backStackEntry.arguments?.getString("spaceId") ?: ""
                DetailedSpaceScreen(spaceId = spaceId, navController = navController)
            }
            composable(Destinations.ReserveRoom.route) { backStackEntry ->
                val spaceId = backStackEntry.arguments?.getString("spaceId") ?: ""
                ReserveRoomScreen(spaceId = spaceId, navController = navController)
            }
        }
    }
}

sealed class Destinations(val route: String, val label: String) {
    data object Login : Destinations("login", "Login")
    data object Explore : Destinations("explore", "Explore")
    data object Rate : Destinations("rate", "Rate")
    data object Reservations : Destinations("reservations", "Reservations")
}

@Composable
private fun BottomNavigationBar(navController: NavHostController) {
    val items = listOf(Destinations.Explore, Destinations.Rate, Destinations.Reservations)
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary
    ) {
        items.forEach { destination ->
            NavigationBarItem(
                selected = currentRoute == destination.route,
                onClick = {
                    navController.navigate(destination.route) {
                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.onSecondary,
                    selectedTextColor = MaterialTheme.colorScheme.onSecondary,
                    indicatorColor = MaterialTheme.colorScheme.secondary,
                    unselectedIconColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f),
                    unselectedTextColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
                ),
                icon = {
                    val icon = when (destination) {
                        Destinations.Login -> Icons.Default.Star
                        Destinations.Explore -> Icons.Default.Search
                        Destinations.Rate -> Icons.Default.Star
                        Destinations.Reservations -> Icons.Default.Star
                        Destinations.ReserveRoom -> Icons.Default.Star
                    }
                    Icon(imageVector = icon, contentDescription = destination.label)
                },
                label = { Text(destination.label) }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(onLoginSuccess: () -> Unit) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = stringResource(id = R.string.login_title),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium
        )

        Spacer(modifier = Modifier.height(12.dp))
        Text(text = stringResource(id = R.string.login_username_label))
        Spacer(modifier = Modifier.height(6.dp))
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            shadowElevation = 8.dp,
            tonalElevation = 0.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            TextField(
                value = username,
                onValueChange = { username = it },
                singleLine = true,
                placeholder = { Text(stringResource(id = R.string.login_username_placeholder)) },
                modifier = Modifier.fillMaxWidth(),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    disabledContainerColor = Color.White,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                shape = MaterialTheme.shapes.extraLarge
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text(text = stringResource(id = R.string.login_password_label))
        Spacer(modifier = Modifier.height(6.dp))
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            shadowElevation = 8.dp,
            tonalElevation = 0.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            TextField(
                value = password,
                onValueChange = { password = it },
                singleLine = true,
                placeholder = { Text(stringResource(id = R.string.login_password_placeholder)) },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth(),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    disabledContainerColor = Color.White,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                shape = MaterialTheme.shapes.extraLarge
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onLoginSuccess,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ),
            shape = MaterialTheme.shapes.extraLarge
        ) {
            Text(text = stringResource(id = R.string.login_button), fontWeight = FontWeight.SemiBold)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExploreScreen() {
    var query by remember { mutableStateOf("") }
    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState()
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = MaterialTheme.shapes.extraLarge,
                shadowElevation = 8.dp,
                tonalElevation = 0.dp,
                modifier = Modifier.weight(1f)
            ) {
                TextField(
                    value = query,
                    onValueChange = { query = it },
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    placeholder = { Text("Lorem ipsum?") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        disabledContainerColor = Color.White,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    shape = MaterialTheme.shapes.extraLarge
                )
            }
            Surface(
                shape = MaterialTheme.shapes.extraLarge,
                shadowElevation = 8.dp,
                tonalElevation = 0.dp
            ) {
                IconButton(onClick = { showDatePicker = true }) {
                    Icon(Icons.Default.Tune, contentDescription = "Filter")
                }
            }
        }

        val items = remember { demoSpaces() }
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            items(items) { space ->
                SpaceCard(space = space, onClick = {
                    navController.navigate(Destinations.DetailedSpace.createRoute(space.id))
                })
                Spacer(modifier = Modifier.height(24.dp))
            }
        }

        if (showDatePicker) {
            DatePickerDialog(
                onDismissRequest = { showDatePicker = false },
                confirmButton = {
                    Button(
                        onClick = { showDatePicker = false },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Text(text = stringResource(id = R.string.action_apply))
                    }
                },
                dismissButton = {
                    Button(
                        onClick = { showDatePicker = false },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary,
                            contentColor = MaterialTheme.colorScheme.onSecondary
                        )
                    ) {
                        Text(text = stringResource(id = R.string.action_cancel))
                    }
                }
            ) {
                DatePicker(state = datePickerState)
            }
        }
    }
}

@Composable
fun SpaceCard(space: SpaceItem,  onClick: () -> Unit = {}) {
    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .clip(MaterialTheme.shapes.medium)
                .background(Color(0xFFE0E0E0))
        )
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                space.title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Star, contentDescription = null)
                Text(
                    text = String.format(Locale.getDefault(), "%.2f", space.rating),
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(space.address, color = Color.Gray)
        Text(space.hour, color = Color.Gray)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "$$$$",
            fontWeight = FontWeight.SemiBold,
            textDecoration = TextDecoration.Underline
        )
    }
}

private fun demoSpaces(): List<SpaceItem> = listOf(
    SpaceItem("Lorem Ipsum", "XXXX", "XXXX", 4.96, 30),
    SpaceItem("Lorem Ipsum", "XXXX", "XXXX", 4.96, 30),
    SpaceItem("Lorem Ipsum", "XXXX", "XXXX", 4.96, 30)
)

@Composable
private fun SimpleCenter(text: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) { Text(text = text, fontSize = 20.sp, fontWeight = FontWeight.Medium) }
}

@Composable
private fun RateScreen() {
    SimpleCenter(text = "Rate")
}

@Composable
fun ReservationsScreen() {
    var query by remember { mutableStateOf("") }
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = MaterialTheme.shapes.extraLarge,
                shadowElevation = 8.dp,
                tonalElevation = 0.dp,
                modifier = Modifier.weight(1f)
            ) {
                TextField(
                    value = query,
                    onValueChange = { query = it },
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    placeholder = { Text("Search reservations") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        disabledContainerColor = Color.White,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    shape = MaterialTheme.shapes.extraLarge
                )
            }
            Surface(
                shape = MaterialTheme.shapes.extraLarge,
                shadowElevation = 8.dp,
                tonalElevation = 0.dp
            ) {
                IconButton(onClick = { /* TODO: open reservation filters */ }) {
                    Icon(Icons.Default.Tune, contentDescription = "Filter")
                }
            }
        }

        val items = remember { demoReservations() }
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            items(items) { reservation ->
                ReservationCard(reservation)
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
fun ReservationCard(reservation: ReservationItem) {
    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .clip(MaterialTheme.shapes.medium)
                .background(Color(0xFFE0E0E0))
        )
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                reservation.title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Star, contentDescription = null)
                Text(
                    text = String.format(Locale.getDefault(), "%.2f", reservation.rating),
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(reservation.hour, color = Color.Gray)
        Text(reservation.address, color = Color.Gray)
    }
}

private fun demoReservations(): List<ReservationItem> = listOf(
    ReservationItem("Meeting Room A", "10:00 AM", "Calle 123 #45", 4.8),
    ReservationItem("Open Space", "2:30 PM", "Cra 7 #85", 4.6),
    ReservationItem("Private Office", "9:00 AM", "Av. 68 #30", 4.9)
)


