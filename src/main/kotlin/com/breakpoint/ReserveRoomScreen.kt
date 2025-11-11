package com.breakpoint

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import kotlinx.coroutines.launch
import androidx.compose.ui.platform.LocalContext
import android.widget.Toast
import android.view.Gravity
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import android.util.Log
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.time.LocalDate
import java.time.LocalTime
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.Instant
import android.Manifest
import android.annotation.SuppressLint
import android.location.Location
import com.google.android.gms.location.LocationServices
import kotlin.math.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReserveRoomScreen(spaceId: String, navController: NavHostController, bookingId: String? = null) {
    var space by remember { mutableStateOf<DetailedSpace?>(null) }
    var spaceError by remember { mutableStateOf<String?>(null) }
    var spaceLoading by remember { mutableStateOf(true) }
    var selectedDate by remember { mutableStateOf("") }
    var selectedTime by remember { mutableStateOf("") }
    var duration by remember { mutableStateOf(1) }
    var guestCount by remember { mutableStateOf(1) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var success by remember { mutableStateOf<String?>(null) }
    // Estado ETA / Llegar a tiempo
    var etaMinutes by remember { mutableStateOf<Int?>(null) }
    var etaError by remember { mutableStateOf<String?>(null) }
    var suggestionTimeLabel by remember { mutableStateOf<String?>(null) } // p.ej. "1:30 PM" o "2:00 PM"
    var suggestionDateIso by remember { mutableStateOf<String?>(null) }   // yyyy-MM-dd
    var transportMode by remember { mutableStateOf("walk") } // "walk" | "drive" (futuro toggle)
    // Telemetría: días más reservados
    var weekdayHistogram by remember { mutableStateOf<List<Int>?>(null) }
    var weekdayError by remember { mutableStateOf<String?>(null) }
    val repo = remember { BookingRepository() }
    val scope = rememberCoroutineScope()
    val ctx = LocalContext.current
    
    LaunchedEffect(spaceId) {
        val repo = SpaceRepository()
        spaceLoading = true; spaceError = null
        val res = repo.getSpace(spaceId)
        spaceLoading = false
        res.fold(onSuccess = { 
            space = it
            kotlin.runCatching { CacheManager(ctx).saveDetail(it) }
        }, onFailure = { ex -> 
            val cached = kotlin.runCatching { CacheManager(ctx).loadDetail(spaceId) }.getOrNull()
            if (cached != null) {
                space = cached
                spaceError = null
            } else {
                spaceError = ex.message ?: "Error cargando espacio"
            }
        })
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Reservar espacio") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { paddingValues ->
        val s = space
        if (spaceLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Cargando espacio...") }
            return@Scaffold
        }
        if (spaceError != null || s == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(spaceError ?: "No se pudo cargar el espacio") }
            return@Scaffold
        }
        val totalPrice = s.price * duration

        // Cargar histograma de días más reservados del espacio
        LaunchedEffect(s.id) {
            weekdayError = null; weekdayHistogram = null
            val spaceRepo = SpaceRepository()
            val res = spaceRepo.getWeekdayHistogram(s.id)
            res.fold(
                onSuccess = { weekdayHistogram = it },
                onFailure = { weekdayError = it.message ?: "No fue posible cargar la telemetría" }
            )
        }

        // Cálculo ETA y sugerencia de primer horario alcanzable (best-effort)
        LaunchedEffect(s.fullAddress, transportMode) {
            etaError = null; etaMinutes = null; suggestionTimeLabel = null; suggestionDateIso = null
            try {
                val latLng = parseLatLngFromGeo(s.fullAddress)
                if (latLng == null) {
                    etaError = "Ubicación del espacio no disponible"
                    return@LaunchedEffect
                }
                val fused = LocationServices.getFusedLocationProviderClient(ctx)
                @SuppressLint("MissingPermission")
                fun fetchLastLocation(onReady: (Location?) -> Unit) {
                    // No forzamos permisos; si no hay permiso, simplemente no habrá ETA
                    fused.lastLocation
                        .addOnSuccessListener { loc -> onReady(loc) }
                        .addOnFailureListener { onReady(null) }
                }
                fetchLastLocation { current ->
                    val userLat = current?.latitude
                    val userLng = current?.longitude
                    if (userLat == null || userLng == null) {
                        etaError = "Activa tu ubicación para calcular el tiempo de llegada"
                        return@fetchLastLocation
                    }
                    val distKm = haversineKm(userLat, userLng, latLng.first, latLng.second)
                    val speedKmh = if (transportMode == "drive") 28.0 else 4.8 // aprox ciudad vs caminar
                    val eta = ceil((distKm / max(0.5, speedKmh)) * 60.0).toInt().coerceAtLeast(5)
                    etaMinutes = eta
                    val zone = ZoneId.systemDefault()
                    val now = java.time.ZonedDateTime.now(zone)
                    val readyTime = now.plusMinutes(eta.toLong())
                    val rounded = readyTime.withMinute(0).withSecond(0).withNano(0).let { rt ->
                        if (readyTime.minute > 0) rt.plusHours(1) else rt
                    }
                    val timeLabel = rounded.format(DateTimeFormatter.ofPattern("h:00 a", Locale.ENGLISH))
                    suggestionTimeLabel = timeLabel
                    suggestionDateIso = rounded.toLocalDate().format(DateTimeFormatter.ISO_LOCAL_DATE)
                }
            } catch (_: Throwable) {
                etaError = "No fue posible calcular tu tiempo de llegada"
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            // Space Info Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = s.title,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = s.fullAddress,
                            color = Color.Gray,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFFD700))
                            Text(
                                text = String.format("%.1f", s.rating),
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(start = 4.dp)
                            )
                            Text(
                                text = "(${s.reviewCount})",
                                color = Color.Gray,
                                modifier = Modifier.padding(start = 4.dp)
                            )
                        }
                    }
                }
            }
            
            // Date Selection
            item {
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "Selecciona la fecha",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(12.dp))
                DateSelector(
                    selectedDate = selectedDate,
                    onDateSelected = { selectedDate = it }
                )
            }
            
            // Time Selection
            item {
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "Selecciona la hora",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(12.dp))
                TimeSelector(
                    selectedTime = selectedTime,
                    onTimeSelected = { selectedTime = it }
                )
            }

            // Llegar a tiempo (ETA + sugerencia)
            item {
                Spacer(modifier = Modifier.height(12.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFEFF6FF)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Sugerencia para llegar a tiempo", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(6.dp))
                        when {
                            etaError != null -> {
                                Text(etaError ?: "", color = Color(0xFF374151))
                            }
                            etaMinutes != null && suggestionTimeLabel != null -> {
                                val eta = etaMinutes ?: 0
                                val sug = suggestionTimeLabel ?: ""
                                val sugDate = suggestionDateIso ?: selectedDate
                                val etaPretty = formatEtaMinutes(eta)
                                Text("Llegada estimada: $etaPretty. Primer horario alcanzable: $sug", color = Color(0xFF374151))
                                Spacer(Modifier.height(8.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(
                                        onClick = {
                                            // Ajustar fecha si la sugerencia cruza de día
                                            if (!sugDate.isNullOrBlank()) selectedDate = sugDate
                                            selectedTime = sug
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                        shape = RoundedCornerShape(10.dp)
                                    ) { Text("Usar sugerencia") }
                                    TextButton(onClick = {
                                        transportMode = if (transportMode == "walk") "drive" else "walk"
                                    }) {
                                        Text(if (transportMode == "walk") "Cambiar a vehículo" else "Cambiar a caminar")
                                    }
                                }
                            }
                            else -> {
                                Text("Calculando tiempo de llegada…", color = Color(0xFF374151))
                            }
                        }
                    }
                }
            }

            // Días más reservados (semana)
            item {
                Spacer(modifier = Modifier.height(12.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Días con más reservas esta semana", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        when {
                            weekdayError != null -> Text(weekdayError ?: "", color = Color(0xFF6B7280))
                            weekdayHistogram == null -> Text("Cargando...", color = Color(0xFF6B7280))
                            else -> {
                                val counts = weekdayHistogram ?: List(7) { 0 }
                                val labels = listOf("L", "M", "X", "J", "V", "S", "D")
                                val maxVal = (counts.maxOrNull() ?: 1).coerceAtLeast(1)
                                // Chips Top 3
                                val top = counts.mapIndexed { idx, v -> idx to v }
                                    .sortedByDescending { it.second }
                                    .take(3).filter { it.second > 0 }
                                if (top.isNotEmpty()) {
                                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        items(top) { (idx, v) ->
                                            TimeChip(time = "${labels[idx]}: x$v", isSelected = false, onClick = {})
                                        }
                                    }
                                    Spacer(Modifier.height(10.dp))
                                }
                                // Barras simples
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(96.dp)
                                        .padding(bottom = 4.dp),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.Bottom
                                ) {
                                    counts.forEachIndexed { i, value ->
                                        val h = if (maxVal == 0) 0f else (value.toFloat() / maxVal.toFloat())
                                        val barHeight = max(6f, h * 64f) // altura mínima y tope visual
                                        Column(
                                            modifier = Modifier.weight(1f),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .width(12.dp)
                                                    .height(barHeight.dp)
                                                    .background(
                                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                                                        RoundedCornerShape(6.dp)
                                                    )
                                            )
                                            Spacer(Modifier.height(6.dp))
                                            Text(labels[i], style = MaterialTheme.typography.bodySmall, color = Color(0xFF6B7280))
                                        }
                                    }
                                }
                                // Nota visual
                                Spacer(Modifier.height(6.dp))
                                Text("Escala basada en el día con más reservas", style = MaterialTheme.typography.bodySmall, color = Color(0xFF9CA3AF))
                            }
                        }
                    }
                }
            }
            
            // Duration Selection
            item {
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "Duración",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(12.dp))
                DurationSelector(
                    duration = duration,
                    onDurationChange = { duration = it }
                )
            }
            
            // Guest Count
            item {
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "Número de personas",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(12.dp))
                GuestCountSelector(
                    guestCount = guestCount,
                    maxGuests = s.capacity,
                    onGuestCountChange = { guestCount = it }
                )
            }
            
            // Price Summary
            item {
                Spacer(modifier = Modifier.height(32.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Precio por hora",
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                text = "$${s.price}",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Duration",
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                text = "${duration} hora${if (duration > 1) "s" else ""}",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Total",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "$${totalPrice}",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
            
            // Reserve Button
            item {
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = {
                        if (!isOnline(ctx)) {
                            navController.navigate(Destinations.Offline.route)
                            return@Button
                        }
                        error = null
                        loading = true
                        if (selectedDate.isBlank()) {
                            loading = false
                            Toast.makeText(ctx, "Selecciona una fecha", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        if (selectedTime.isBlank()) {
                            loading = false
                            Toast.makeText(ctx, "Selecciona una hora", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        // Aviso si la hora elegida no es alcanzable
                        try {
                            val zone = ZoneId.systemDefault()
                            val chosenDate = LocalDate.parse(selectedDate, DateTimeFormatter.ISO_LOCAL_DATE)
                            val chosenTime = LocalTime.parse(selectedTime, DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH))
                            val chosen = LocalDateTime.of(chosenDate, chosenTime).atZone(zone).toInstant()
                            val minReach = etaMinutes?.let { Instant.now().plusSeconds((it * 60).toLong()) }
                            if (minReach != null && chosen.isBefore(minReach)) {
                                loading = false
                                val sug = suggestionTimeLabel ?: "la siguiente hora disponible"
                                error = "No alcanzas a llegar a tiempo para esa hora. ¿Quieres ajustar a $sug?"
                                return@Button
                            }
                        } catch (_: Throwable) {}
                        if (duration <= 0) {
                            loading = false
                            Toast.makeText(ctx, "Selecciona una duración válida", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        if (guestCount <= 0) {
                            loading = false
                            Toast.makeText(ctx, "Selecciona el número de invitados", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        scope.launch {
                            try {
                                val date = LocalDate.parse(selectedDate, DateTimeFormatter.ISO_LOCAL_DATE)
                                val time = LocalTime.parse(selectedTime, DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH))
                                val startLdt = LocalDateTime.of(date, time)
                                val zone = ZoneId.systemDefault()
                                val startInstant = startLdt.atZone(zone).toInstant()
                                val endInstant = startLdt.plusHours(duration.toLong()).atZone(zone).toInstant()
                                // Validación previa: no permitir reservas en el pasado
                                if (startInstant.isBefore(Instant.now())) {
                                    loading = false
                                    error = "La hora de inicio ya pasó. Elige otra hora."
                                    return@launch
                                }
                                val startIso = startInstant.toString()
                                val endIso = endInstant.toString()
                                val res = if (bookingId.isNullOrBlank()) {
                                    repo.createBooking(spaceId, startIso, endIso, guestCount)
                                } else {
                                    repo.updateBooking(bookingId, slotStartIso = startIso, slotEndIso = endIso)
                                }
                                loading = false
                                res.fold(
                                    onSuccess = {
                                        success = if (bookingId.isNullOrBlank()) "Tu reserva fue creada exitosamente." else "Tu reserva fue actualizada."
                                    },
                                    onFailure = {
                                        error = it.message ?: "Error creando reserva"
                                    }
                                )
                            } catch (t: Throwable) {
                                loading = false
                                error = t.message ?: "Error creando reserva"
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                    shape = RoundedCornerShape(12.dp),
                    enabled = selectedDate.isNotEmpty() && selectedTime.isNotEmpty() && !loading
                ) {
                    Text(
                        text = if (loading) "Reservando..." else "Reservar por $${totalPrice}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
        if (error != null) {
            AlertDialog(
                onDismissRequest = { error = null },
                confirmButton = {
                    TextButton(onClick = { error = null }) { Text("OK") }
                },
                title = { Text("No se pudo crear la reserva") },
                text = { Text(error ?: "Intenta de nuevo") }
            )
        }
        if (success != null) {
            AlertDialog(
                onDismissRequest = { success = null },
                confirmButton = {
                    TextButton(onClick = {
                        success = null
                        navController.navigate(Destinations.Reservations.route)
                    }) { Text("Ver reservas") }
                },
                title = { Text("Reserva confirmada") },
                text = { Text(success ?: "Tu reserva fue creada exitosamente") }
            )
        }
    }
}

private fun parseLatLngFromGeo(raw: String?): Pair<Double, Double>? {
    if (raw.isNullOrBlank()) return null
    val regex = Regex("-?\\d+(?:\\.\\d+)?")
    val numbers = regex.findAll(raw).mapNotNull { it.value.toDoubleOrNull() }.toList()
    if (numbers.size < 2) return null
    val a = numbers[0]; val b = numbers[1]
    val lat: Double; val lng: Double
    if (abs(a) > 90 && abs(b) <= 90) { lat = b; lng = a } else { lat = a; lng = b }
    return lat to lng
}

private fun haversineKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val R = 6371.0
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)
    val a = sin(dLat / 2).pow(2.0) + cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2.0)
    val c = 2 * atan2(sqrt(a), sqrt(1 - a))
    return R * c
}

private fun formatEtaMinutes(totalMinutes: Int): String {
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return when {
        hours <= 0 -> "$minutes min"
        minutes == 0 -> "$hours h"
        else -> "$hours h $minutes min"
    }
}

@Composable
fun DateSelector(selectedDate: String, onDateSelected: (String) -> Unit) {
    val calendar = Calendar.getInstance()
    val dateFormat = SimpleDateFormat("MMM dd", Locale.getDefault())
    val fullDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    
    val dates = remember {
        (0..6).map { daysFromNow ->
            calendar.timeInMillis = System.currentTimeMillis() + (daysFromNow * 24 * 60 * 60 * 1000L)
            Pair(
                dateFormat.format(calendar.time),
                fullDateFormat.format(calendar.time)
            )
        }
    }
    
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(dates) { (displayDate, fullDate) ->
            DateChip(
                date = displayDate,
                isSelected = selectedDate == fullDate,
                onClick = { onDateSelected(fullDate) }
            )
        }
    }
}

@Composable
fun DateChip(date: String, isSelected: Boolean, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.clickable { onClick() },
        shape = RoundedCornerShape(8.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
        border = if (isSelected) null else androidx.compose.foundation.BorderStroke(1.dp, Color.Gray)
    ) {
        Text(
            text = date,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else Color.Black,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
fun TimeSelector(selectedTime: String, onTimeSelected: (String) -> Unit) {
    val timeSlots = remember {
        listOf(
            "6:00 AM", "7:00 AM", "8:00 AM", "9:00 AM", "10:00 AM", "11:00 AM", "12:00 PM",
            "1:00 PM", "2:00 PM", "3:00 PM", "4:00 PM", "5:00 PM", "6:00 PM", "7:00 PM", "8:00 PM", "9:00 PM", "10:00 PM",
            "11:00 PM", "12:00 AM", "1:00 AM"
        )
    }
    
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(timeSlots) { time ->
            TimeChip(
                time = time,
                isSelected = selectedTime == time,
                onClick = { onTimeSelected(time) }
            )
        }
    }
}

@Composable
fun TimeChip(time: String, isSelected: Boolean, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.clickable { onClick() },
        shape = RoundedCornerShape(8.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
        border = if (isSelected) null else androidx.compose.foundation.BorderStroke(1.dp, Color.Gray)
    ) {
        Text(
            text = time,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else Color.Black,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
fun DurationSelector(duration: Int, onDurationChange: (Int) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = { if (duration > 1) onDurationChange(duration - 1) },
            enabled = duration > 1
        ) {
            Icon(Icons.Default.Remove, contentDescription = "Decrease")
        }
        
        Text(
            text = "${duration} hour${if (duration > 1) "s" else ""}",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        
        IconButton(
            onClick = { if (duration < 8) onDurationChange(duration + 1) },
            enabled = duration < 8
        ) {
            Icon(Icons.Default.Add, contentDescription = "Increase")
        }
    }
}

@Composable
fun GuestCountSelector(guestCount: Int, maxGuests: Int, onGuestCountChange: (Int) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = { if (guestCount > 1) onGuestCountChange(guestCount - 1) },
            enabled = guestCount > 1
        ) {
            Icon(Icons.Default.Remove, contentDescription = "Decrease")
        }
        
        Text(
            text = "$guestCount guest${if (guestCount > 1) "s" else ""}",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        
        IconButton(
            onClick = { if (guestCount < maxGuests) onGuestCountChange(guestCount + 1) },
            enabled = guestCount < maxGuests
        ) {
            Icon(Icons.Default.Add, contentDescription = "Increase")
        }
    }
}
