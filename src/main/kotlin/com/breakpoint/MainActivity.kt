package com.breakpoint

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.foundation.layout.imePadding
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale
import androidx.navigation.NavGraph.Companion.findStartDestination
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.VisualTransformation
import kotlinx.coroutines.tasks.await
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import android.Manifest
import android.content.pm.PackageManager
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.CameraPosition
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import kotlin.math.abs

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
    // 401 handler: vuelve al login limpiando back stack
    ApiProvider.setOnUnauthorized {
        navController.navigate(Destinations.Login.route) {
            popUpTo(navController.graph.findStartDestination().id) { inclusive = true }
            launchSingleTop = true
        }
    }
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    Scaffold(
        bottomBar = {
            if (currentRoute != Destinations.Login.route) {
                BottomNavigationBar(navController)
            }
        }
    ) { padding ->
        // Autologin: si hay token en DataStore, úsalo
        val context = LocalContext.current
        val tokenManager = remember { TokenManager(context) }
        androidx.compose.runtime.LaunchedEffect(Unit) {
            tokenManager.tokenFlow.collect { token ->
                if (!token.isNullOrBlank()) {
                    ApiProvider.setToken(token)
                }
            }
        }
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
            composable(Destinations.Profile.route) { ProfileScreen(navController) }
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
    var selectedRole by remember { mutableStateOf("Student") }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var success by remember { mutableStateOf<String?>(null) }
    var showPassword by remember { mutableStateOf(false) }
    var showConfirm by remember { mutableStateOf(false) }
    
    // Estados de validación
    var emailError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }
    var confirmError by remember { mutableStateOf<String?>(null) }
    var nameError by remember { mutableStateOf<String?>(null) }
    
    val repo = remember { AuthRepository() }
    val ctx = LocalContext.current
    val tokenManager = remember(ctx) { TokenManager(ctx) }

    val scrollState = rememberScrollState()
    val focusManager = LocalFocusManager.current
    val nameRequester = remember { FocusRequester() }
    val usernameRequester = remember { FocusRequester() }
    val passwordRequester = remember { FocusRequester() }
    val confirmRequester = remember { FocusRequester() }

    // Funciones de validación
    fun validateEmail(email: String): String? {
        if (email.isBlank()) return "Email es requerido"
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            return "Email inválido"
        }
        if (!email.lowercase().endsWith("@uniandes.edu.co")) {
            return "Email debe ser @uniandes.edu.co"
        }
        return null
    }

    fun validatePassword(password: String): String? {
        if (password.isBlank()) return "Contraseña es requerida"
        if (password.trim().isEmpty()) return "La contraseña no puede ser vacía"
        if (password.length < 6) return "Contraseña mínimo 6 caracteres"
        return null
    }

    fun validateConfirmPassword(password: String, confirm: String): String? {
        if (confirm.isBlank()) return "Confirmar contraseña es requerido"
        if (password != confirm) return "Las contraseñas no coinciden"
        return null
    }

    fun validateName(name: String): String? {
        if (name.isNotBlank() && name.trim().isEmpty()) {
            return "Nombre no puede ser vacío"
        }
        return null
    }

    fun validateForm(): Boolean {
        val emailErr = validateEmail(username)
        val passwordErr = validatePassword(password)
        val confirmErr = if (isRegister) validateConfirmPassword(password, confirm) else null
        val nameErr = if (isRegister) validateName(name) else null
        
        emailError = emailErr
        passwordError = passwordErr
        confirmError = confirmErr
        nameError = nameErr
        
        return emailErr == null && passwordErr == null && confirmErr == null && nameErr == null
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .verticalScroll(scrollState)
            .imePadding()
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
                            .clickable { 
                                isRegister = false
                                // Limpiar errores al cambiar modo
                                error = null
                                success = null
                                emailError = null
                                passwordError = null
                                confirmError = null
                                nameError = null
                            }
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                    ) { Text("Login", color = if (!isRegister) MaterialTheme.colorScheme.onPrimary else Color.Black) }
                    Box(
                        modifier = Modifier
                            .clip(MaterialTheme.shapes.extraLarge)
                            .background(if (isRegister) MaterialTheme.colorScheme.primary else Color.White)
                            .clickable { 
                                isRegister = true
                                // Limpiar errores al cambiar modo
                                error = null
                                success = null
                                emailError = null
                                passwordError = null
                                confirmError = null
                                nameError = null
                            }
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
                    onValueChange = { 
                        name = it
                        nameError = validateName(it)
                    },
                    singleLine = true,
                    placeholder = { Text("Tu nombre") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(nameRequester),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(
                        onNext = { usernameRequester.requestFocus() }
                    ),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        disabledContainerColor = Color.White,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        errorContainerColor = Color.White,
                        errorIndicatorColor = Color.Red
                    ),
                    shape = MaterialTheme.shapes.extraLarge,
                    isError = nameError != null,
                    supportingText = nameError?.let { { Text(it, color = Color.Red) } }
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(text = "Rol")
            Spacer(modifier = Modifier.height(6.dp))
            Surface(
                shape = MaterialTheme.shapes.extraLarge,
                shadowElevation = 8.dp,
                tonalElevation = 0.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                var expanded by remember { mutableStateOf(false) }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.extraLarge)
                        .background(Color.White)
                        .clickable { expanded = true }
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                ) {
                    Text(text = selectedRole)
                    androidx.compose.material3.DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        androidx.compose.material3.DropdownMenuItem(
                            text = { Text("Student") },
                            onClick = { selectedRole = "Student"; expanded = false }
                        )
                        androidx.compose.material3.DropdownMenuItem(
                            text = { Text("Host") },
                            onClick = { selectedRole = "Host"; expanded = false }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        Text(text = "Email")
        Spacer(modifier = Modifier.height(6.dp))
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            shadowElevation = 8.dp,
            tonalElevation = 0.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            TextField(
                value = username,
                onValueChange = { 
                    username = it
                    emailError = validateEmail(it)
                },
                singleLine = true,
                placeholder = { Text("tu.email@uniandes.edu.co") },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(usernameRequester),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(
                    onNext = { passwordRequester.requestFocus() }
                ),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    disabledContainerColor = Color.White,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    errorContainerColor = Color.White,
                    errorIndicatorColor = Color.Red
                ),
                shape = MaterialTheme.shapes.extraLarge,
                isError = emailError != null,
                supportingText = emailError?.let { { Text(it, color = Color.Red) } }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text(text = "Contraseña")
        Spacer(modifier = Modifier.height(6.dp))
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            shadowElevation = 8.dp,
            tonalElevation = 0.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            TextField(
                value = password,
                onValueChange = { 
                    password = it
                    passwordError = validatePassword(it)
                    // Si estamos en modo registro, también validar confirmación
                    if (isRegister && confirm.isNotBlank()) {
                        confirmError = validateConfirmPassword(it, confirm)
                    }
                },
                singleLine = true,
                placeholder = { Text("Mínimo 6 caracteres") },
                visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = if (isRegister) ImeAction.Next else ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onNext = { if (isRegister) confirmRequester.requestFocus() },
                    onDone = {
                        if (!isRegister) {
                            if (validateForm()) {
                                error = null
                                success = null
                                loading = true
                            }
                        }
                    }
                ),
                trailingIcon = {
                    IconButton(onClick = { showPassword = !showPassword }) {
                        val icon = if (showPassword) androidx.compose.material.icons.Icons.Default.VisibilityOff else androidx.compose.material.icons.Icons.Default.Visibility
                        Icon(icon, contentDescription = if (showPassword) "Ocultar" else "Mostrar")
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(passwordRequester),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    disabledContainerColor = Color.White,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    errorContainerColor = Color.White,
                    errorIndicatorColor = Color.Red
                ),
                shape = MaterialTheme.shapes.extraLarge,
                isError = passwordError != null,
                supportingText = passwordError?.let { { Text(it, color = Color.Red) } }
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
                    onValueChange = { 
                        confirm = it
                        confirmError = validateConfirmPassword(password, it)
                    },
                    singleLine = true,
                    placeholder = { Text("Repite tu contraseña") },
                    visualTransformation = if (showConfirm) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            if (validateForm()) {
                                error = null
                                success = null
                                loading = true
                            }
                        }
                    ),
                    trailingIcon = {
                        IconButton(onClick = { showConfirm = !showConfirm }) {
                            val icon = if (showConfirm) androidx.compose.material.icons.Icons.Default.VisibilityOff else androidx.compose.material.icons.Icons.Default.Visibility
                            Icon(icon, contentDescription = if (showConfirm) "Ocultar" else "Mostrar")
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(confirmRequester),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        disabledContainerColor = Color.White,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        errorContainerColor = Color.White,
                        errorIndicatorColor = Color.Red
                    ),
                    shape = MaterialTheme.shapes.extraLarge,
                    isError = confirmError != null,
                    supportingText = confirmError?.let { { Text(it, color = Color.Red) } }
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        val coroutineScope = rememberCoroutineScope()
        Button(
            onClick = {
                if (loading) return@Button
                
                // Validar formulario antes de enviar
                if (!validateForm()) {
                    return@Button
                }
                
                error = null
                success = null
                loading = true
                coroutineScope.launch {
                    val result = if (isRegister) {
                        repo.register(username, password, name.ifBlank { null }, selectedRole)
                    } else {
                        repo.login(username, password)
                    }
                    result.fold(
                        onSuccess = {
                            if (isRegister) {
                                success = "Usuario creado exitosamente"
                                val login = repo.login(username, password)
                                login.fold(
                                    onSuccess = {
                                        ApiProvider.currentToken()?.let { tokenManager.saveToken(it) }
                                        onLoginSuccess()
                                    },
                                    onFailure = { error = it.message ?: "Error tras registro" }
                                )
                            } else {
                                ApiProvider.currentToken()?.let { tokenManager.saveToken(it) }
                                onLoginSuccess()
                            }
                        },
                        onFailure = { 
                            val errorMessage = when {
                                it.message?.contains("Credenciales inválidas") == true -> "Email o contraseña incorrectos"
                                it.message?.contains("El correo ya está registrado") == true -> "Este email ya está registrado"
                                it.message?.contains("Email debe ser con @uniandes.edu.co") == true -> "Email debe ser @uniandes.edu.co"
                                it.message?.contains("La contraseña no puede ser vacía") == true -> "La contraseña no puede ser vacía"
                                else -> it.message ?: if (isRegister) "Error en el registro" else "Error en el login"
                            }
                            error = errorMessage
                        }
                    )
                    loading = false
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ),
            shape = MaterialTheme.shapes.extraLarge,
            enabled = !loading && emailError == null && passwordError == null && confirmError == null && nameError == null && username.isNotBlank() && password.isNotBlank() && (!isRegister || confirm.isNotBlank())
        ) {
            val label = if (isRegister) "Crear cuenta" else stringResource(id = R.string.login_button)
            Text(text = if (loading) "Procesando..." else label, fontWeight = FontWeight.SemiBold)
        }

        if (success != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = success!!, color = Color(0xFF2E7D32))
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
    var filtered by remember { mutableStateOf<List<SpaceItem>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()
    var selectedDateMillis by remember { mutableStateOf<Long?>(null) }
    var showHourPicker by remember { mutableStateOf(false) }
    var startHour by remember { mutableStateOf<Int?>(null) }
    var endHour by remember { mutableStateOf<Int?>(null) }
    val context = LocalContext.current
    var showMap by remember { mutableStateOf(false) }
    var userLatLng by remember { mutableStateOf<LatLng?>(null) }
    val parseLatLng: (String) -> LatLng? = { text ->
        val regex = Regex("-?\\d+(?:\\.\\d+)?")
        val nums = regex.findAll(text).map { it.value.toDoubleOrNull() }.filterNotNull().toList()
        if (nums.size < 2) null else {
            val a = nums[0]
            val b = nums[1]
            val lat: Double
            val lng: Double
            if (abs(a) > 90 && abs(b) <= 90) { lat = b; lng = a } else { lat = a; lng = b }
            LatLng(lat, lng)
        }
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        val granted = perms[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                perms[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (!granted) {
            loading = false
            error = "Permiso de ubicación denegado"
        } else {
            val fused = com.google.android.gms.location.LocationServices.getFusedLocationProviderClient(context)
            coroutineScope.launch {
                try {
                    val location = fused.lastLocation.await()
                    if (location == null) {
                        loading = false
                        error = "No se pudo obtener ubicación"
                    } else {
                        val lat = location.latitude
                        val lng = location.longitude
                        val withDistance = items.map { s ->
                            val parts = s.address.split(",")
                            val maybeLat = parts.getOrNull(0)?.toDoubleOrNull()
                            val maybeLng = parts.getOrNull(1)?.toDoubleOrNull()
                            val d = if (maybeLat != null && maybeLng != null) {
                                val dLat = Math.toRadians(maybeLat - lat)
                                val dLng = Math.toRadians(maybeLng - lng)
                                val a = Math.sin(dLat/2)*Math.sin(dLat/2) + Math.cos(Math.toRadians(lat))*Math.cos(Math.toRadians(maybeLat))*Math.sin(dLng/2)*Math.sin(dLng/2)
                                val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a))
                                6371.0 * c
                            } else Double.MAX_VALUE
                            s to d
                        }
                        filtered = withDistance.sortedBy { it.second }.map { it.first }
                        loading = false
                    }
                } catch (t: Throwable) {
                    loading = false
                    error = t.message ?: "Error ubicando"
                }
            }
        }
    }
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
                    onValueChange = { 
                        query = it
                        filtered = if (query.isBlank()) items else items.filter { s ->
                            s.title.contains(query, ignoreCase = true) || s.address.contains(query, ignoreCase = true)
                        }
                    },
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
            // Botón: calcular cercanos y ETA caminando
            Surface(
                shape = MaterialTheme.shapes.extraLarge,
                shadowElevation = 8.dp,
                tonalElevation = 0.dp
            ) {
                Box(
                    modifier = Modifier
                        .clip(MaterialTheme.shapes.extraLarge)
                        .background(Color.White)
                        .clickable {
                            coroutineScope.launch {
                                error = null
                                loading = true
                                val fused = com.google.android.gms.location.LocationServices.getFusedLocationProviderClient(context)
                                try {
                                    // Runtime permission check
                                    val pm = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                                    val pmCoarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
                                    if (pm != PackageManager.PERMISSION_GRANTED && pmCoarse != PackageManager.PERMISSION_GRANTED) {
                                        permissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
                                        return@launch
                                    }
                                    val location = fused.lastLocation.await()
                                    if (location == null) {
                                        loading = false
                                        error = "No se pudo obtener ubicación"
                                    } else {
                                        val lat = location.latitude
                                        val lng = location.longitude
                                        userLatLng = LatLng(lat, lng)
                                        // Ordenar client-side por distancia (aproximada) usando geo si es lat,lng o dejando como está si no
                                        val withDistance = items.map { s ->
                                            val ll = parseLatLng(s.address)
                                            val d = if (ll != null) {
                                                val dLat = Math.toRadians(ll.latitude - lat)
                                                val dLng = Math.toRadians(ll.longitude - lng)
                                                val a = Math.sin(dLat/2)*Math.sin(dLat/2) + Math.cos(Math.toRadians(lat))*Math.cos(Math.toRadians(ll.latitude))*Math.sin(dLng/2)*Math.sin(dLng/2)
                                                val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a))
                                                6371.0 * c // km
                                            } else Double.MAX_VALUE
                                            s to d
                                        }
                                        filtered = withDistance.sortedBy { it.second }.map { it.first }
                                        showMap = true
                                        loading = false
                                    }
                                } catch (t: Throwable) {
                                    loading = false
                                    error = t.message ?: "Error ubicando"
                                }
                            }
                        }
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                ) { Text("Cerca de mí") }
            }
        }

        androidx.compose.runtime.LaunchedEffect(Unit) {
            val result = repo.getSpaces()
            loading = false
            result.fold(
                onSuccess = { items = it; filtered = it },
                onFailure = { error = it.message ?: "Error loading spaces" }
            )
        }
        if (loading) {
            SimpleCenter(text = "Loading...")
        } else if (error != null) {
            SimpleCenter(text = error!!)
        } else if (filtered.isEmpty()) {
            Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Text(text = "No hay resultados")
                Spacer(Modifier.height(8.dp))
                Button(onClick = {
                    coroutineScope.launch {
                        loading = true; error = null
                        val result = repo.getSpaces()
                        loading = false
                        result.fold(onSuccess = { items = it; filtered = it }, onFailure = { error = it.message })
                    }
                }) { Text("Reintentar") }
            }
        } else if (showMap) {
            val firstLatLng = filtered.firstOrNull()?.let { parseLatLng(it.address) }
            val center = firstLatLng ?: userLatLng ?: LatLng(4.65, -74.1)
            val cameraState = rememberCameraPositionState {
                position = CameraPosition.fromLatLngZoom(center, 12f)
            }
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraState
            ) {
                userLatLng?.let { Marker(state = MarkerState(it), title = "Tú") }
                filtered.forEach { s ->
                    parseLatLng(s.address)?.let { ll ->
                        Marker(state = MarkerState(position = ll), title = s.title)
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
            ) {
                item {
                    Button(onClick = { coroutineScope.launch { loading = true; error = null; val res = repo.getSpaces(); loading=false; res.fold(onSuccess={ items=it; filtered=it }, onFailure={ error=it.message }) } }) {
                        Text("Actualizar")
                    }
                    Spacer(Modifier.height(12.dp))
                }
                items(filtered) { space ->
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
                                // Restringe a máximo 7 días de anticipación
                                val oneWeekMs = 7L * 24 * 60 * 60 * 1000
                                val now = System.currentTimeMillis()
                                if (millis > now + oneWeekMs) {
                                    error = "Solo se permite reservar con máximo una semana de anticipación"
                                    return@Button
                                }
                                selectedDateMillis = millis
                                startHour = null
                                endHour = null
                                showHourPicker = true
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

    if (showHourPicker && selectedDateMillis != null) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showHourPicker = false },
            confirmButton = {},
            dismissButton = {},
            title = { Text("Selecciona horas") },
            text = {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    HourDropdown(label = "Desde", hour = startHour, onHourSelected = {
                        startHour = it
                        val sh = startHour; val eh = endHour
                        if (sh != null && eh != null && eh > sh) {
                            val sdf = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.US)
                            sdf.timeZone = java.util.TimeZone.getTimeZone("UTC")
                            val cal = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC"))
                            cal.time = java.util.Date(selectedDateMillis!!)
                            cal.set(java.util.Calendar.MINUTE, 0)
                            cal.set(java.util.Calendar.SECOND, 0)
                            cal.set(java.util.Calendar.MILLISECOND, 0)
                            cal.set(java.util.Calendar.HOUR_OF_DAY, sh)
                            val startIso = sdf.format(cal.time)
                            cal.set(java.util.Calendar.HOUR_OF_DAY, eh)
                            val endIso = sdf.format(cal.time)
                            loading = true; error = null
                            coroutineScope.launch {
                                val res = repo.getAvailable(startIso, endIso)
                                loading = false
                                res.fold(onSuccess = { items = it; filtered = it }, onFailure = { error = it.message ?: "Error loading availability" })
                            }
                            showHourPicker = false
                        }
                    })
                    HourDropdown(label = "Hasta", hour = endHour, onHourSelected = {
                        endHour = it
                        val sh = startHour; val eh = endHour
                        if (sh != null && eh != null && eh > sh) {
                            val sdf = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.US)
                            sdf.timeZone = java.util.TimeZone.getTimeZone("UTC")
                            val cal = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC"))
                            cal.time = java.util.Date(selectedDateMillis!!)
                            cal.set(java.util.Calendar.MINUTE, 0)
                            cal.set(java.util.Calendar.SECOND, 0)
                            cal.set(java.util.Calendar.MILLISECOND, 0)
                            cal.set(java.util.Calendar.HOUR_OF_DAY, sh)
                            val startIso = sdf.format(cal.time)
                            cal.set(java.util.Calendar.HOUR_OF_DAY, eh)
                            val endIso = sdf.format(cal.time)
                            loading = true; error = null
                            coroutineScope.launch {
                                val res = repo.getAvailable(startIso, endIso)
                                loading = false
                                res.fold(onSuccess = { items = it; filtered = it }, onFailure = { error = it.message ?: "Error loading availability" })
                            }
                            showHourPicker = false
                        }
                    })
                }
            }
        )
    }
}

@Composable
private fun HourDropdown(label: String, hour: Int?, onHourSelected: (Int) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val options = remember { (0..23).toList() }
    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        shadowElevation = 8.dp,
        tonalElevation = 0.dp
    ) {
        Box(
            modifier = Modifier
                .clip(MaterialTheme.shapes.extraLarge)
                .background(Color.White)
                .clickable { expanded = true }
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) { Text(text = (hour?.let { String.format("%s %02d:00", label, it) } ?: label)) }
        androidx.compose.material3.DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { h ->
                androidx.compose.material3.DropdownMenuItem(text = { Text(String.format("%02d:00", h)) }, onClick = {
                    expanded = false; onHourSelected(h)
                })
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
        if (!space.imageUrl.isNullOrBlank()) {
            AsyncImage(
                model = space.imageUrl,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .clip(MaterialTheme.shapes.medium),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .clip(MaterialTheme.shapes.medium)
                    .background(Color(0xFFE0E0E0))
            )
        }
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
    SpaceItem("1", "Lorem Ipsum", null, "XXXX", "XXXX", 4.96, 30),
    SpaceItem("2", "Lorem Ipsum", null, "XXXX", "XXXX", 4.96, 30),
    SpaceItem("3", "Lorem Ipsum", null, "XXXX", "XXXX", 4.96, 30)
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
    val repo = remember { BookingRepository() }
    var query by remember { mutableStateOf("") }
    var items by remember { mutableStateOf<List<BookingListItemDto>>(emptyList()) }
    var filtered by remember { mutableStateOf<List<BookingListItemDto>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    androidx.compose.runtime.LaunchedEffect(Unit) {
        val result = repo.listMyBookings()
        loading = false
        result.fold(
            onSuccess = { list -> items = list; filtered = list },
            onFailure = { t -> error = t.message ?: "Error cargando reservas" }
        )
    }

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
                    onValueChange = { q ->
                        query = q
                        filtered = if (q.isBlank()) items else items.filter { it.space?.title?.contains(q, ignoreCase = true) == true || it.status.contains(q, ignoreCase = true) }
                    },
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    placeholder = { Text("Buscar reservas") },
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
                IconButton(onClick = { /* filtros opcionales */ }) {
                    Icon(Icons.Default.Tune, contentDescription = "Filter")
                }
            }
        }

        if (loading) {
            SimpleCenter(text = "Cargando...")
        } else if (error != null) {
            SimpleCenter(text = error!!)
        } else if (filtered.isEmpty()) {
            SimpleCenter(text = "No tienes reservas")
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
            ) {
                items(filtered) { booking ->
                    BookingCard(booking)
                    Spacer(modifier = Modifier.height(8.dp))
                    androidx.compose.material3.Divider()
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun BookingCard(booking: BookingListItemDto) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(MaterialTheme.shapes.medium)
        ) {
            if (!booking.space?.imageUrl.isNullOrBlank()) {
                AsyncImage(
                    model = booking.space?.imageUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFFE0E0E0))
                )
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    booking.space?.title ?: "Espacio",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Text(text = booking.status, color = Color.Gray, style = MaterialTheme.typography.labelMedium)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(formatBookingTime(booking), color = Color.Gray, style = MaterialTheme.typography.bodySmall)
            val amount = booking.totalAmount?.let { amt ->
                val curr = booking.currency ?: ""
                if (curr.isNotBlank()) "$curr $amt" else amt
            }
            if (!amount.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(amount, color = Color.Gray, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

private fun formatBookingTime(b: BookingListItemDto): String {
    return try {
        val start = java.time.Instant.parse(b.slotStart).atZone(java.time.ZoneId.systemDefault()).toLocalDateTime()
        val end = java.time.Instant.parse(b.slotEnd).atZone(java.time.ZoneId.systemDefault()).toLocalDateTime()
        val dateFmt = java.time.format.DateTimeFormatter.ofPattern("MMM dd", java.util.Locale.getDefault())
        val timeFmt = java.time.format.DateTimeFormatter.ofPattern("h:mm a", java.util.Locale.getDefault())
        "${start.format(dateFmt)} • ${start.format(timeFmt)} - ${end.format(timeFmt)}"
    } catch (_: Throwable) {
        "${'$'}{b.slotStart} - ${'$'}{b.slotEnd}"
    }
}

@Composable
fun ProfileScreen(navController: NavHostController) {
    val repo = remember { AuthRepository() }
    var email by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var role by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    androidx.compose.runtime.LaunchedEffect(Unit) {
        val result = repo.profile()
        loading = false
        result.fold(
            onSuccess = {
                email = it.email
                name = it.name ?: ""
                role = it.role ?: ""
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
                    if (role.isNotBlank()) {
                        Text(text = role, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = { /* TODO: cambiar contraseña */ }) {
                        Text("Cambiar contraseña")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    val context = LocalContext.current
                    val tokenManager = remember { TokenManager(context) }
                    val scope = rememberCoroutineScope()
                    Button(onClick = { 
                        scope.launch {
                            tokenManager.clear()
                        }
                        ApiProvider.setToken(null)
                        navController.navigate(Destinations.Login.route) {
                            popUpTo(navController.graph.findStartDestination().id) { inclusive = true }
                            launchSingleTop = true
                        }
                    }) {
                        Text("Cerrar sesión")
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


