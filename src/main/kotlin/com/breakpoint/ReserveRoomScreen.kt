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
    val repo = remember { BookingRepository() }
    val scope = rememberCoroutineScope()
    val ctx = LocalContext.current
    
    LaunchedEffect(spaceId) {
        val repo = SpaceRepository()
        spaceLoading = true; spaceError = null
        val res = repo.getSpace(spaceId)
        spaceLoading = false
        res.fold(onSuccess = { space = it }, onFailure = { spaceError = it.message ?: "Error cargando espacio" })
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Reserve Room") },
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
                    text = "Select Date",
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
                    text = "Select Time",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(12.dp))
                TimeSelector(
                    selectedTime = selectedTime,
                    onTimeSelected = { selectedTime = it }
                )
            }
            
            // Duration Selection
            item {
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "Duration",
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
                    text = "Number of Guests",
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
                                text = "Price per hour",
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
                                text = "${duration} hour${if (duration > 1) "s" else ""}",
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
                                val startIso = startLdt.atZone(zone).toInstant().toString()
                                val endIso = startLdt.plusHours(duration.toLong()).atZone(zone).toInstant().toString()
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
                        text = if (loading) "Reservando..." else "Reserve for $${totalPrice}",
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
