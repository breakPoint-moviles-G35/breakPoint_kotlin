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
import androidx.compose.material.icons.filled.Person
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
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch

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
            composable(Destinations.Explore.route) { ExploreScreen(navController) }
            composable(Destinations.Rate.route) { SimpleCenter(text = "Rate") }
            composable(Destinations.Reservations.route) { ReservationsScreen() }
            composable(Destinations.Profile.route) { ProfileScreen() }
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
    data object Profile : Destinations("profile", "Profile")
    data object DetailedSpace : Destinations("detailed_space/{spaceId}", "Space Details") {
        fun createRoute(spaceId: String) = "detailed_space/$spaceId"
    }
    data object ReserveRoom : Destinations("reserve_room/{spaceId}", "Reserve Room") {
        fun createRoute(spaceId: String) = "reserve_room/$spaceId"
    }
}

@Composable
private fun BottomNavigationBar(navController: NavHostController) {
    val items = listOf(Destinations.Explore, Destinations.Rate, Destinations.Reservations, Destinations.Profile)
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
                        Destinations.Explore -> Icons.Default.Search
                        Destinations.Profile -> Icons.Default.Person
                        Destinations.Rate,
                        Destinations.Reservations,
                        Destinations.DetailedSpace,
                        Destinations.ReserveRoom,
                        Destinations.Login -> Icons.Default.Star
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
    var isRegister by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val repo = remember { AuthRepository() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(12.dp))
        // Toggle Login/Register
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            Surface(
                shape = MaterialTheme.shapes.extraLarge,
                shadowElevation = 8.dp,
                tonalElevation = 0.dp
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(MaterialTheme.shapes.extraLarge)
                        .background(Color.White)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(MaterialTheme.shapes.extraLarge)
                            .background(if (!isRegister) MaterialTheme.colorScheme.primary else Color.White)
                            .clickable { isRegister = false }
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                    ) { Text("Login", color = if (!isRegister) MaterialTheme.colorScheme.onPrimary else Color.Black) }
                    Box(
                        modifier = Modifier
                            .clip(MaterialTheme.shapes.extraLarge)
                            .background(if (isRegister) MaterialTheme.colorScheme.primary else Color.White)
                            .clickable { isRegister = true }
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                    ) { Text("Register", color = if (isRegister) MaterialTheme.colorScheme.onPrimary else Color.Black) }
                }
            }
        }

        Text(
            text = if (isRegister) "Crear cuenta" else stringResource(id = R.string.login_title),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium
        )

        if (isRegister) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(text = "Nombre (opcional)")
            Spacer(modifier = Modifier.height(6.dp))
            Surface(
                shape = MaterialTheme.shapes.extraLarge,
                shadowElevation = 8.dp,
                tonalElevation = 0.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                TextField(
                    value = name,
                    onValueChange = { name = it },
                    singleLine = true,
                    placeholder = { Text("Tu nombre") },
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
        }

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

        if (isRegister) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = "Confirmar contraseña")
            Spacer(modifier = Modifier.height(6.dp))
            Surface(
                shape = MaterialTheme.shapes.extraLarge,
                shadowElevation = 8.dp,
                tonalElevation = 0.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                TextField(
                    value = confirm,
                    onValueChange = { confirm = it },
                    singleLine = true,
                    placeholder = { Text("Repite tu contraseña") },
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
        }

        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = {
                error = null
                loading = true
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ),
            shape = MaterialTheme.shapes.extraLarge
        ) {
            val label = if (isRegister) "Crear cuenta" else stringResource(id = R.string.login_button)
            Text(text = if (loading) "Procesando..." else label, fontWeight = FontWeight.SemiBold)
        }

        if (loading) {
            androidx.compose.runtime.LaunchedEffect(username + "|" + password + "|" + isRegister + "|" + confirm + "|" + name) {
                val result = if (isRegister) {
                    if (password != confirm) {
                        loading = false
                        error = "Las contraseñas no coinciden"
                        return@LaunchedEffect
                    }
                    repo.register(username, password, name.ifBlank { null })
                } else {
                    repo.login(username, password)
                }
                loading = false
                result.fold(
                    onSuccess = { if (isRegister) { /* tras registro, intenta login */
                        val login = repo.login(username, password)
                        login.fold(onSuccess = { onLoginSuccess() }, onFailure = { error = it.message ?: "Error tras registro" })
                    } else onLoginSuccess() },
                    onFailure = { error = it.message ?: if (isRegister) "Registro fallido" else "Login fallido" }
                )
            }
        }

        if (error != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = error!!, color = Color.Red)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExploreScreen(navController: NavHostController) {
    var query by remember { mutableStateOf("") }
    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState()
    val repo = remember { SpaceRepository() }
    var items by remember { mutableStateOf<List<SpaceItem>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()
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

        androidx.compose.runtime.LaunchedEffect(Unit) {
            val result = repo.getSpaces()
            loading = false
            result.fold(
                onSuccess = { items = it },
                onFailure = { error = it.message ?: "Error loading spaces" }
            )
        }
        if (loading) {
            SimpleCenter(text = "Loading...")
        } else if (error != null) {
            SimpleCenter(text = error!!)
        } else {
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
        }

        if (showDatePicker) {
            DatePickerDialog(
                onDismissRequest = { showDatePicker = false },
                confirmButton = {
                    Button(
                        onClick = {
                            showDatePicker = false
                            val millis = datePickerState.selectedDateMillis
                            if (millis != null) {
                                val start = java.time.Instant.ofEpochMilli(millis).toString()
                                val end = java.time.Instant.ofEpochMilli(millis + 60L * 60 * 1000).toString()
                                loading = true
                                error = null
                                coroutineScope.launch {
                                    val result = repo.getAvailable(start, end)
                                    loading = false
                                    result.fold(
                                        onSuccess = { items = it },
                                        onFailure = { error = it.message ?: "Error loading availability" }
                                    )
                                }
                            }
                        },
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
    SpaceItem("1", "Lorem Ipsum", "XXXX", "XXXX", 4.96, 30),
    SpaceItem("2", "Lorem Ipsum", "XXXX", "XXXX", 4.96, 30),
    SpaceItem("3", "Lorem Ipsum", "XXXX", "XXXX", 4.96, 30)
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
fun ProfileScreen() {
    val repo = remember { AuthRepository() }
    var email by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    androidx.compose.runtime.LaunchedEffect(Unit) {
        val result = repo.profile()
        loading = false
        result.fold(
            onSuccess = {
                email = it.email
                name = it.name ?: ""
            },
            onFailure = { error = it.message ?: "Error cargando perfil" }
        )
    }

    if (loading) {
        SimpleCenter(text = "Cargando perfil...")
    } else if (error != null) {
        SimpleCenter(text = error!!)
    } else {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(text = "Mi perfil", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(text = email, style = MaterialTheme.typography.bodyLarge)
                    if (name.isNotBlank()) {
                        Text(text = name, style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = { /* TODO: cambiar contraseña */ }) {
                        Text("Cambiar contraseña")
                    }
                }
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


