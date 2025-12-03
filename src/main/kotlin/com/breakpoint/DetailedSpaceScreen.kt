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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.People
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.style.TextAlign
import kotlinx.coroutines.launch
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.size.Precision
import coil.size.Scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalConfiguration

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailedSpaceScreen(spaceId: String, navController: NavHostController) {
    var space by remember { mutableStateOf<DetailedSpace?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(true) }
    var isFavorite by remember { mutableStateOf(false) }
    var popular by remember { mutableStateOf<List<Pair<Int, Int>>>(emptyList()) }
    var histogram by remember { mutableStateOf<List<Int>>(emptyList()) }
    var amenityImpacts by remember { mutableStateOf<List<AmenityImpact>>(emptyList()) }
    val scope = rememberCoroutineScope()
    val accentColor = Color(0xFF5C1B6C)

    LaunchedEffect(spaceId) {
        val repo = SpaceRepository()
        loading = true; error = null
        amenityImpacts = emptyList()

        coroutineScope {
            // Lanzamos en paralelo las tres consultas principales del detalle
            val spaceDeferred = async(Dispatchers.IO) { repo.getSpace(spaceId) }
            val popularDeferred = async(Dispatchers.IO) { repo.getPopularHours(spaceId) }
            val histogramDeferred = async(Dispatchers.IO) { repo.getHourlyHistogram(spaceId) }

            var currentAmenities: Set<String> = emptySet()

            // Esperamos el detalle del espacio y actualizamos UI
            val res = spaceDeferred.await()
            loading = false
            res.fold(onSuccess = {
                space = it
                currentAmenities = it.amenities.toSet()
                kotlin.runCatching { CacheManager(navController.context).saveDetail(it) }
            }, onFailure = { ex ->
                val cached = kotlin.runCatching { CacheManager(navController.context).loadDetail(spaceId) }.getOrNull()
                if (cached != null) {
                    space = cached
                    currentAmenities = cached.amenities.toSet()
                    error = null
                } else {
                    error = ex.message ?: "Error cargando espacio"
                }
            })

            // Estas dos respuestas ya se estaban resolviendo en paralelo
            popularDeferred.await().onSuccess { popular = it.take(5) }
            histogramDeferred.await().onSuccess { histogram = it }

            // Analytics de amenities: se calcula en un contexto de I/O y usa varias coroutines en paralelo
            if (currentAmenities.isNotEmpty()) {
                val impacts = withContext(Dispatchers.IO) {
                    computeAmenityImpactsForHost(currentAmenities)
                }
                amenityImpacts = impacts
            }
        }
    }
    
    androidx.compose.material3.Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = accentColor
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { isFavorite = !isFavorite }) {
                        Icon(
                            if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Favorite",
                            tint = if (isFavorite) accentColor else accentColor.copy(alpha = 0.5f)
                        )
                    }
                    IconButton(onClick = { /* TODO: Share functionality */ }) {
                        Icon(
                            Icons.Default.Share,
                            contentDescription = "Share",
                            tint = accentColor
                        )
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
                        res.fold(onSuccess = { 
                            space = it
                            kotlin.runCatching { CacheManager(navController.context).saveDetail(it) }
                        }, onFailure = { 
                            val cached = kotlin.runCatching { CacheManager(navController.context).loadDetail(spaceId) }.getOrNull()
                            if (cached != null) {
                                space = cached
                                error = null
                            } else {
                                error = it.message ?: "Error cargando espacio"
                            }
                        })
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
                Column {
                    val heroImage = s.images.firstOrNull()
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp)
                    ) {
                        if (heroImage != null) {
                            // Cargar héroe al tamaño exacto para evitar trabajo extra en GPU
                            val ctx = LocalContext.current
                            val density = LocalDensity.current
                            val widthDp = LocalConfiguration.current.screenWidthDp
                            val wPx = with(density) { widthDp.dp.roundToPx() }.coerceAtLeast(1)
                            val hPx = with(density) { 300.dp.roundToPx() }.coerceAtLeast(1)
                            val req = remember(heroImage, wPx, hPx) {
                                ImageRequest.Builder(ctx)
                                    .data(heroImage)
                                    .size(wPx, hPx)
                                    .precision(Precision.EXACT)
                                    .scale(Scale.FILL)
                                    .crossfade(false)
                                    .allowHardware(true)
                                    .build()
                            }
                            AsyncImage(
                                model = req,
                                contentDescription = "Imagen principal del espacio",
                                modifier = Modifier
                                    .fillMaxSize(),
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
                    if (s.images.size > 1) {
                        Spacer(modifier = Modifier.height(12.dp))
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(s.images.drop(1), key = { it }, contentType = { "thumb" }) { imageUrl ->
                                val ctx = LocalContext.current
                                val density = LocalDensity.current
                                val wPx = with(density) { 140.dp.roundToPx() }.coerceAtLeast(1)
                                val hPx = with(density) { 90.dp.roundToPx() }.coerceAtLeast(1)
                                val req = remember(imageUrl, wPx, hPx) {
                                    ImageRequest.Builder(ctx)
                                        .data(imageUrl)
                                        .size(wPx, hPx)
                                        .precision(Precision.EXACT)
                                        .scale(Scale.FILL)
                                        .crossfade(false)
                                        .allowHardware(true)
                                        .build()
                                }
                                AsyncImage(
                                    model = req,
                                    contentDescription = "Imagen adicional del espacio",
                                    modifier = Modifier
                                        .height(90.dp)
                                        .width(140.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color(0xFFE0E0E0)),
                                    contentScale = ContentScale.Crop
                                )
                            }
                        }
                    }
                }
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
                        Row(verticalAlignment = Alignment.CenterVertically) {
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
                    Text(
                        text = "Hosted by ${s.hostName}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFFD700))
                        Text(
                            text = String.format("%.1f", s.hostRating),
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(start = 4.dp)
                        )
                        Text(
                            text = "host rating",
                            color = Color.Gray,
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))

                    // Amenity insights for hosts (BQ)
                    if (amenityImpacts.isNotEmpty()) {
                        Text(
                            text = "Impacto de los amenities en tus reservas",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Para cada amenity comparamos cuántas reservas, en promedio, tienen tus salas que lo incluyen frente a las que no lo tienen.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        amenityImpacts.forEach { impact ->
                            val diff = impact.rateWith - impact.rateWithout
                            val hasThisAmenity = s.amenities.contains(impact.name)
                            Text(
                                text = buildString {
                                    append("• ${impact.name}: tus salas CON este amenity tienen en promedio ")
                                    append("%.2f".format(impact.rateWith))
                                    append(" reservas por sala; tus salas SIN este amenity tienen ")
                                    append("%.2f".format(impact.rateWithout))
                                    append(". Es decir, alrededor de ")
                                    append(if (diff >= 0) "+" else "")
                                    append("%.2f".format(diff))
                                    append(" reservas por sala de diferencia.")
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF374151)
                            )
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                    }

                    // Reserve Button
                    Button(
                        onClick = {
                            val ctx = navController.context
                            val cm = ctx.getSystemService(android.content.Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
                            val network = cm.activeNetwork
                            val caps = network?.let { cm.getNetworkCapabilities(it) }
                            val hasInternet = caps?.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
                            if (!hasInternet) {
                                navController.navigate(Destinations.Offline.route)
                            } else {
                                navController.navigate(Destinations.ReserveRoom.createRoute(s.id))
                            }
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
fun DetailItem(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String) {
    val accentColor = Color(0xFF5C1B6C)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, contentDescription = null, tint = accentColor)
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
    val accentColor = Color(0xFF5C1B6C)
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
                tint = accentColor
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = amenity,
                style = MaterialTheme.typography.bodySmall,
                color = accentColor
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

// Legacy fallback removed: the screen now consumes backend data via SpaceRepository

// Analytics helper: calcula el impacto de los amenities usando múltiples corrutinas en paralelo (multi-threading en IO)
private suspend fun computeAmenityImpactsForHost(currentAmenities: Set<String>): List<AmenityImpact> = coroutineScope {
    val hostRepo = HostRepository()
    val spacesRes = hostRepo.listMySpaces()
    val hostSpaces = spacesRes.getOrNull() ?: return@coroutineScope emptyList()
    if (hostSpaces.isEmpty()) return@coroutineScope emptyList()

    // Lanzar en paralelo el conteo de reservas por espacio
    val countDeferred = hostSpaces.associate { space ->
        space.id to async(Dispatchers.IO) {
            hostRepo.fetchBookingCount(space.id).getOrNull() ?: 0
        }
    }
    val counts = countDeferred.mapValues { it.value.await() }
    if (counts.values.all { it == 0 }) return@coroutineScope emptyList()

    val totalSpaces = hostSpaces.size
    val totalBookings = counts.values.sum()
    val allAmenities = hostSpaces.flatMap { it.amenities.orEmpty() }.toSet()

    allAmenities.mapNotNull { amenity ->
        // Solo consideramos amenities presentes en este espacio actual
        if (!currentAmenities.contains(amenity)) return@mapNotNull null

        val withSpaces = hostSpaces.filter { it.amenities?.contains(amenity) == true }
        val spacesWith = withSpaces.size
        if (spacesWith == 0 || spacesWith == totalSpaces) return@mapNotNull null

        val bookingsWith = withSpaces.sumOf { counts[it.id] ?: 0 }
        val spacesWithout = totalSpaces - spacesWith
        val bookingsWithout = totalBookings - bookingsWith

        val rateWith = if (spacesWith > 0) bookingsWith.toDouble() / spacesWith.toDouble() else 0.0
        val rateWithout = if (spacesWithout > 0) bookingsWithout.toDouble() / spacesWithout.toDouble() else 0.0
        val lift = if (rateWithout > 0.0) (rateWith - rateWithout) / rateWithout else 0.0

        AmenityImpact(
            name = amenity,
            rateWith = rateWith,
            rateWithout = rateWithout,
            lift = lift,
            spacesWith = spacesWith,
            spacesWithout = spacesWithout
        )
    }.sortedByDescending { it.lift }.take(3)
}
