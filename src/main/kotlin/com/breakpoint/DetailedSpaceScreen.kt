package com.breakpoint

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.style.TextAlign
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailedSpaceScreen(spaceId: String, navController: NavHostController) {
    var space by remember { mutableStateOf<DetailedSpace?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(true) }
    var isFavorite by remember { mutableStateOf(false) }
    var popular by remember { mutableStateOf<List<Pair<Int, Int>>>(emptyList()) }
    var histogram by remember { mutableStateOf<List<Int>>(emptyList()) }
    var hostProfileDetail by remember { mutableStateOf<HostProfileDetailDto?>(null) }
    var hostError by remember { mutableStateOf<String?>(null) }
    var reviewStats by remember { mutableStateOf<ReviewStatsDto?>(null) }
    var reviews by remember { mutableStateOf<List<ReviewDto>>(emptyList()) }
    var reviewError by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val hostProfileRepository = remember { HostProfileRepository() }
    val reviewRepository = remember { ReviewRepository() }

    LaunchedEffect(spaceId) {
        val repo = SpaceRepository()
        loading = true; error = null
        hostError = null
        reviewError = null
        hostProfileDetail = null
        reviewStats = null
        reviews = emptyList()
        val res = repo.getSpace(spaceId)
        loading = false
        res.fold(onSuccess = { detail ->
            space = detail
            val hostId = detail.hostProfileId
            if (!hostId.isNullOrBlank()) {
                hostProfileRepository.findById(hostId).fold(
                    onSuccess = { hostProfileDetail = it },
                    onFailure = { hostError = it.message ?: "No se pudo cargar el anfitrión" }
                )
            }
        }, onFailure = { error = it.message ?: "Error cargando espacio" })
        repo.getPopularHours(spaceId).onSuccess { popular = it.take(5) }
        repo.getHourlyHistogram(spaceId).onSuccess { histogram = it }
        reviewRepository.statsForSpace(spaceId).fold(
            onSuccess = { reviewStats = it },
            onFailure = { failure -> reviewError = failure.message ?: "No se pudieron cargar las reseñas" }
        )
        reviewRepository.listForSpace(spaceId).fold(
            onSuccess = { reviews = it },
            onFailure = { failure -> reviewError = failure.message ?: "No se pudieron cargar las reseñas" }
        )
    }
    
    androidx.compose.material3.Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { isFavorite = !isFavorite }) {
                        Icon(
                            if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Favorite",
                            tint = if (isFavorite) Color.Red else Color.Gray
                        )
                    }
                    IconButton(onClick = { /* TODO: Share functionality */ }) {
                        Icon(Icons.Default.Share, contentDescription = "Share")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        }
    ) { paddingValues ->
        if (loading) {
            androidx.compose.material3.CircularProgressIndicator()
        } else if (error != null) {
            Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Text(text = error!!)
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = {
                    error = null; loading = true
                    scope.launch {
                        val repo = SpaceRepository()
                        val res = repo.getSpace(spaceId)
                        loading = false
                        res.fold(onSuccess = { space = it }, onFailure = { error = it.message ?: "Error cargando espacio" })
                    }
                }) { Text("Reintentar") }
            }
        } else {
        val s = space ?: return@Scaffold
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Hero Image
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                        .background(Color(0xFFE0E0E0))
                )
            }
            
            // Content
            item {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    // Title and Rating
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = s.title,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                        val stats = reviewStats
                        val ratingValue = stats?.resolvedAverage ?: s.rating
                        val displayedRating = if (ratingValue <= 0.0) "N/A" else String.format(Locale.getDefault(), "%.1f", ratingValue)
                        val displayedCount = stats?.resolvedCount ?: s.reviewCount
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFFD700))
                            Text(
                                text = displayedRating,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(start = 4.dp)
                            )
                            Text(
                                text = if (displayedCount > 0) "($displayedCount)" else "(sin reseñas)",
                                color = Color.Gray,
                                modifier = Modifier.padding(start = 4.dp)
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Location
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color.Gray)
                        Text(
                            text = s.fullAddress,
                            color = Color.Gray,
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Space Details
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        DetailItem(
                            icon = Icons.Default.People,
                            label = "Capacity",
                            value = "${s.capacity} people"
                        )
                        DetailItem(
                            icon = Icons.Default.Star,
                            label = "Size",
                            value = s.size
                        )
                        DetailItem(
                            icon = Icons.Default.Star,
                            label = "Price",
                            value = "$${s.price}/hour"
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // Description
                    Text(
                        text = "About this space",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = s.description,
                        style = MaterialTheme.typography.bodyLarge,
                        lineHeight = 24.sp
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // Amenities
                    Text(
                        text = "Amenities",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(s.amenities) { amenity ->
                            AmenityChip(amenity = amenity)
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))

                    // Popular Hours (week)
                    Text(
                        text = "Horas más reservadas (semana)",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    if (popular.isEmpty()) {
                        Text(text = "Sin datos suficientes")
                    } else {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(popular) { (hour, count) ->
                                PopularHourChip(hour, count)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    ReservationBarChart(data = histogram)
                    
                    // Host Information
                    val hostNameDisplay = hostProfileDetail?.displayName?.takeIf { it.isNotBlank() } ?: s.hostName
                    Text(
                        text = "Hosted by $hostNameDisplay",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text(
                            text = hostNameDisplay,
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(start = 6.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    val hostRatingValue = hostProfileDetail?.ratingAverage ?: s.hostRating
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFFD700))
                        Text(
                            text = if (hostRatingValue <= 0.0) "N/A" else String.format(Locale.getDefault(), "%.1f", hostRatingValue),
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(start = 4.dp)
                        )
                        val hostReviews = hostProfileDetail?.totalReviews
                        if ((hostReviews ?: 0) > 0) {
                            Text(
                                text = "($hostReviews reviews)",
                                color = Color.Gray,
                                modifier = Modifier.padding(start = 4.dp)
                            )
                        } else {
                            Text(
                                text = "host rating",
                                color = Color.Gray,
                                modifier = Modifier.padding(start = 4.dp)
                            )
                        }
                    }
                    hostProfileDetail?.bio?.takeIf { it.isNotBlank() }?.let { bio ->
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = bio.trim(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Gray
                        )
                    }
                    if (hostError != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = hostError!!,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Red
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(32.dp))

                    Text(
                        text = "Opiniones de usuarios",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    when {
                        reviewError != null -> {
                            Text(
                                text = reviewError!!,
                                color = Color.Red,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        reviews.isEmpty() -> {
                            Text(
                                text = "Aún no hay reseñas para este espacio",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.Gray
                            )
                        }
                        else -> {
                            reviews.take(4).forEach { review ->
                                ReviewCard(review = review)
                                Spacer(modifier = Modifier.height(12.dp))
                            }
                            if (reviews.size > 4) {
                                Text(
                                    text = "+${reviews.size - 4} reseñas más disponibles",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.Gray
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Reserve Button
                    Button(
                        onClick = { 
                            navController.navigate(Destinations.ReserveRoom.createRoute(s.id))
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "Reserve this space",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
        }
    }
}

@Composable
fun ReviewCard(review: ReviewDto) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        shadowElevation = 0.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFFD700))
                Text(
                    text = "${review.rating}/5",
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(start = 6.dp)
                )
                formatReviewDate(review.updatedAt ?: review.createdAt)?.let { date ->
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = date,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
            }
            review.comment?.takeIf { it.isNotBlank() }?.let { comment ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = comment.trim(),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            review.userId?.takeIf { it.isNotBlank() }?.let { uid ->
                Spacer(modifier = Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Usuario ${uid.takeLast(6)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray,
                        modifier = Modifier.padding(start = 6.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun DetailItem(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, contentDescription = null, tint = Color.Gray)
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun AmenityChip(amenity: String) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f),
        modifier = Modifier.padding(vertical = 4.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Icon(
                Icons.Default.Wifi,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = amenity,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun PopularHourChip(hour: Int, count: Int) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
        modifier = Modifier.padding(vertical = 4.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Text(
                text = String.format("%02d:00", hour),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "x$count",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
        }
    }
}

@Composable
fun ReservationBarChart(data: List<Int>, maxCap: Int = 10) {
    if (data.isEmpty()) return
    val capped = data.map { it.coerceAtMost(maxCap) }
    val maxValue = (capped.maxOrNull() ?: 1).coerceAtLeast(1)
    val barColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Tap hours for details",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(8.dp))
        Canvas(modifier = Modifier
            .fillMaxWidth()
            .height(160.dp)) {
            val barCount = 24
            val gap = 8.dp.toPx()
            val barWidth = (size.width - gap * (barCount + 1)) / barCount
            val heightUnit = if (maxValue == 0) 0f else (size.height - 16.dp.toPx()) / maxValue
            for (i in 0 until barCount) {
                val value = capped.getOrElse(i) { 0 }
                val barHeight = value * heightUnit
                val left = gap + i * (barWidth + gap)
                val top = size.height - barHeight
                drawRoundRect(
                    color = barColor,
                    topLeft = androidx.compose.ui.geometry.Offset(left, top),
                    size = Size(barWidth, barHeight),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(10.dp.toPx(), 10.dp.toPx())
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            val labels = listOf("6a","9a","12p","3p","6p","9p")
            labels.forEach { lbl ->
                Text(text = lbl, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
            }
        }
        Text(
            text = "Tope visual en $maxCap reservas por hora",
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray
        )
    }
}

private fun formatReviewDate(iso: String?): String? {
    if (iso.isNullOrBlank()) return null
    return try {
        val instant = Instant.parse(iso)
        val formatter = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.getDefault())
        instant.atZone(ZoneId.systemDefault()).format(formatter)
    } catch (_: Throwable) {
        iso.take(16)
    }
}
