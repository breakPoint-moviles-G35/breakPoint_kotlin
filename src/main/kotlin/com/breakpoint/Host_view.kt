package com.breakpoint

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.breakpoint.ApiProvider
import com.breakpoint.CacheManager
import com.breakpoint.UserDto
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MarkerInfoWindow
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import com.breakpoint.showTopToast

@Composable
fun HostExploreScreen(navController: NavHostController) {
    val repo = remember { HostRepository() }
    var spaces by remember { mutableStateOf<List<SpaceItem>>(emptyList()) }
    var filtered by remember { mutableStateOf<List<SpaceItem>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var showPriceMenu by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    var collapsedInfo by remember { mutableStateOf<SpaceItem?>(null) }
    val context = LocalContext.current
    val cacheManager = remember { CacheManager(context.applicationContext) }
    val scope = rememberCoroutineScope()

    fun applyFilter(source: List<SpaceItem>, q: String) {
        filtered = if (q.isBlank()) source else source.filter {
            it.title.contains(q, true) ||
                it.subtitle.orEmpty().contains(q, true) ||
                it.address.contains(q, true)
        }
    }

    fun refreshSpaces() {
        scope.launch {
            val diskCached = cacheManager.loadHostSpaces()
            val initial = if (diskCached.isNotEmpty()) diskCached else emptyList()
            if (initial.isNotEmpty()) {
                // Pintar inmediato desde cache (memoria o DataStore) mientras llega el refresh de red.
                loading = false
                error = null
                spaces = initial
                applyFilter(initial, query)
            } else {
                loading = true
                error = null
            }

            val result = repo.listMySpaces(context)
            result.fold(onSuccess = { list ->
                spaces = list
                applyFilter(list, query)
                collapsedInfo = null
            }, onFailure = {
                if (spaces.isEmpty()) {
                    error = it.message ?: "No se pudieron cargar tus espacios"
                }
            })
            loading = false
        }
    }

    LaunchedEffect(Unit) { refreshSpaces() }

    when {
        loading -> HostCenteredMessage("Cargando espacios...")
        error != null -> HostErrorCard(message = error!!, onRetry = { refreshSpaces() })
        else -> {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SearchBarHost(
                    value = query,
                    onValueChange = {
                        query = it
                        applyFilter(spaces, it)
                    }
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = { navController.navigate(Destinations.HostCreateSpace.route) },
                        modifier = Modifier
                            .weight(1f)
                            .defaultMinSize(minHeight = 48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5C1B6C))
                    ) {
                        Text("Create Room", color = Color.White, fontWeight = FontWeight.SemiBold)
                    }
                    OutlinedButton(
                        onClick = { navController.navigate(Destinations.HostMap.route) },
                        modifier = Modifier
                            .weight(1f)
                            .defaultMinSize(minHeight = 48.dp)
                    ) {
                        Icon(Icons.Default.Map, contentDescription = null, tint = Color(0xFF5C1B6C))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Map", color = Color(0xFF5C1B6C))
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(onClick = { refreshSpaces() }) {
                        Text("Actualizar")
                    }
                    Button(
                        onClick = { showPriceMenu = !showPriceMenu },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEDE7F6), contentColor = Color(0xFF5C1B6C))
                    ) {
                        Text("Ordenar por precio")
                    }
                }
                if (showPriceMenu) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(onClick = {
                            filtered = spaces.sortedBy { it.price }
                            showPriceMenu = false
                        }) { Text("Menor a mayor") }
                        Button(onClick = {
                            filtered = spaces.sortedByDescending { it.price }
                            showPriceMenu = false
                        }) { Text("Mayor a menor") }
                    }
                }

                if (filtered.isEmpty()) {
                    HostEmptyListCard(onCreate = { navController.navigate(Destinations.HostCreateSpace.route) })
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(18.dp)
                    ) {
                        items(filtered, key = { it.id }) { space ->
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                SpaceCard(
                                    space = space,
                                    onClick = { navController.navigate(Destinations.DetailedSpace.createRoute(space.id)) },
                                    showLocation = true,
                                    showDetailsButton = true
                                )
                                HostQuickActions(
                                    space = space,
                                    onRefresh = { refreshSpaces() }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HostMapScreen(navController: NavHostController) {
    val repo = remember { HostRepository() }
    var spaces by remember { mutableStateOf<List<SpaceItem>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    val defaultLatLng = LatLng(4.65, -74.05)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(defaultLatLng, 12f)
    }
    val context = LocalContext.current
    val cacheManager = remember { CacheManager(context.applicationContext) }
    val scope = rememberCoroutineScope()
    var userLatLng by remember { mutableStateOf<LatLng?>(null) }
    var selectedIndex by remember { mutableStateOf(0) }
    var selectedMarkerId by remember { mutableStateOf<String?>(null) }
    var collapsedInfo by remember { mutableStateOf<SpaceItem?>(null) }
    var bookingCounts by remember { mutableStateOf<Map<String, Int>>(emptyMap()) }
    var usageInsights by remember { mutableStateOf<HostUsageInsights?>(null) }
    var insightsLoading by remember { mutableStateOf(false) }

    suspend fun fetchLocation(): LatLng? {
        val fused = com.google.android.gms.location.LocationServices.getFusedLocationProviderClient(context)
        return runCatching { fused.lastLocation.await() }.getOrNull()?.let { LatLng(it.latitude, it.longitude) }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        val granted = perms[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            perms[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            scope.launch {
                val loc = fetchLocation()
                loc?.let {
                    userLatLng = it
                    cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(it, 14f))
                }
            }
        } else {
            error = "Permiso de ubicacion denegado"
        }
    }

    fun focusSpaceOnMap(space: SpaceItem) {
        val index = spaces.indexOfFirst { it.id == space.id }
        if (index >= 0) {
            selectedIndex = index
            selectedMarkerId = space.id
        }
        space.latLng()?.let { latLng ->
            scope.launch {
                cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(latLng, 15f))
            }
        }
    }

    fun centerOnUser() {
        val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
        val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
        if (fine != PackageManager.PERMISSION_GRANTED && coarse != PackageManager.PERMISSION_GRANTED) {
            permissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
        } else {
            scope.launch {
                val loc = fetchLocation()
                loc?.let {
                    userLatLng = it
                    // Proteger contra CameraUpdateFactory no inicializado
                    runCatching {
                        cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(it, 14f))
                    }
                }
            }
        }
    }

    fun reload() {
        scope.launch {
            val diskCached = try { cacheManager.loadHostSpaces() } catch (_: Throwable) { emptyList() }
            if (diskCached.isNotEmpty()) {
                loading = false
                error = null
                spaces = diskCached
                bookingCounts = emptyMap()
                usageInsights = null
                selectedIndex = 0
                selectedMarkerId = diskCached.firstOrNull()?.id
                collapsedInfo = null
                diskCached.firstOrNull { item -> item.latLng() != null }?.latLng()?.let { latLng ->
                    cameraPositionState.position = CameraPosition.fromLatLngZoom(latLng, 13f)
                }
            } else {
                loading = true
                error = null
            }
            val result = repo.listMySpaces(context)
            result.fold(onSuccess = { list ->
                spaces = list
                bookingCounts = emptyMap()
                usageInsights = null
                selectedIndex = 0
                selectedMarkerId = list.firstOrNull()?.id
                collapsedInfo = null
                list.firstOrNull { item -> item.latLng() != null }?.latLng()?.let { latLng ->
                    cameraPositionState.position = CameraPosition.fromLatLngZoom(latLng, 13f)
                }
            }, onFailure = {
                if (spaces.isEmpty()) {
                    error = it.message ?: "No se pudo cargar tus espacios"
                }
            })
            loading = false
        }
    }

    LaunchedEffect(Unit) { reload() }

    LaunchedEffect(selectedIndex) {
        spaces.getOrNull(selectedIndex)?.let { space ->
            selectedMarkerId = space.id
            space.latLng()?.let {
                cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(it, 14f))
            }
        }
    }

    LaunchedEffect(spaces) {
        if (spaces.isEmpty()) {
            bookingCounts = emptyMap()
            usageInsights = null
            insightsLoading = false
            return@LaunchedEffect
        }
        insightsLoading = true
        val idToSpace = spaces.associateBy { it.id }
        try {
            // Multi-threaded: dispara una petición por espacio en paralelo para llenar las dos ventanas emergentes (stats + info).
            val counts = coroutineScope {
                spaces.map { space ->
                    async { space.id to repo.fetchBookingCount(space.id) }
                }.awaitAll()
                    .mapNotNull { (id, result) -> result.getOrNull()?.let { id to it } }
                    .toMap()
            }
            bookingCounts = counts
            if (counts.isEmpty()) {
                usageInsights = null
                return@LaunchedEffect
            }

            val most = counts.maxByOrNull { it.value }?.let { entry ->
                idToSpace[entry.key]?.let { HostUsageStat(it, entry.value) }
            }
            val least = counts.minByOrNull { it.value }?.let { entry ->
                idToSpace[entry.key]?.let { HostUsageStat(it, entry.value) }
            }

            // Analítica por amenity: promedio de reservas por espacio con/sin ese amenity
            val totalSpaces = spaces.size
            val totalBookings = counts.values.sum()
            val allAmenities = spaces.flatMap { it.amenities.orEmpty() }.toSet()
            val amenityImpacts = allAmenities.mapNotNull { amenity ->
                val withSpaces = spaces.filter { it.amenities?.contains(amenity) == true }
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

            usageInsights = HostUsageInsights(
                mostReserved = most,
                leastReserved = least,
                amenityImpacts = amenityImpacts
            )
        } finally {
            insightsLoading = false
        }
    }

    when {
        loading -> HostCenteredMessage("Cargando mapa...")
        error != null -> HostErrorCard(message = error!!, onRetry = { reload() })
        else -> {
            Box(modifier = Modifier.fillMaxSize()) {
                val underusedId = usageInsights?.leastReserved?.space?.id
                GoogleMap(
                    modifier = Modifier.fillMaxSize(),
                    cameraPositionState = cameraPositionState
                ) {
                    userLatLng?.let { location ->
                        val userIcon = remember(location) { createUserLocationBitmapDescriptor(context) }
                        Marker(
                            state = remember(location) { MarkerState(location) },
                            title = "Tu",
                            icon = userIcon,
                            zIndex = 1f
                        )
                    }
                    spaces.forEachIndexed { index, space ->
                        val latLng = space.latLng() ?: return@forEachIndexed
                        val markerState = remember(space.id, latLng) { MarkerState(latLng) }
                        val isSelected = selectedMarkerId == space.id
                        val highlightUnderused = underusedId == space.id
                        val icon = remember(space.id, space.price, isSelected, highlightUnderused) {
                            createPriceMarkerBitmapDescriptor(
                                context = context,
                                price = space.price,
                                selected = isSelected || highlightUnderused
                            )
                        }
                        MarkerInfoWindow(
                            state = markerState,
                            icon = icon,
                            onClick = {
                                selectedIndex = index
                                selectedMarkerId = space.id
                                collapsedInfo = null
                                false
                            }
                        ) {
                            SpaceMarkerInfoWindowContent(
                                title = space.title,
                                subtitle = space.subtitle ?: space.address,
                                rating = space.rating,
                                onNavigate = {
                                    navController.navigate(Destinations.DetailedSpace.createRoute(space.id))
                                },
                                onClose = {
                                    collapsedInfo = space
                                    selectedMarkerId = null
                                }
                            )
                        }
                    }
                }

                Button(
                    onClick = { centerOnUser() },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF5C1B6C),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(30.dp)
                ) {
                    Text("Centrar")
                }

                if (spaces.isNotEmpty()) {
                    HostInsightBanner(
                        stats = usageInsights ?: HostUsageInsights(null, null),
                        loading = insightsLoading,
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(horizontal = 16.dp, vertical = 16.dp)
                            .padding(top = 48.dp),
                        onPromote = { focusSpaceOnMap(it) },
                        onShowDetails = { navController.navigate(Destinations.DetailedSpace.createRoute(it.id)) }
                    )
                }

                collapsedInfo?.let { item ->
                    ExtendedFloatingActionButton(
                        onClick = {
                            val idx = spaces.indexOfFirst { it.id == item.id }
                            if (idx >= 0) {
                                selectedIndex = idx
                                selectedMarkerId = item.id
                            }
                            collapsedInfo = null
                        },
                        icon = { Icon(Icons.Filled.OpenInNew, contentDescription = "Mostrar información") },
                        text = { Text("Mostrar ${item.title}") },
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(start = 16.dp, bottom = 140.dp)
                    )
                }

                if (spaces.isNotEmpty()) {
                    spaces.getOrNull(selectedIndex)?.let { space ->
                        HostMapCarousel(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(16.dp),
                            space = space,
                            index = selectedIndex,
                            total = spaces.size,
                            bookingCount = bookingCounts[space.id],
                            onPrev = { if (selectedIndex > 0) selectedIndex -= 1 },
                            onNext = { if (selectedIndex < spaces.lastIndex) selectedIndex += 1 },
                            onViewDetails = {
                                navController.navigate(Destinations.DetailedSpace.createRoute(space.id))
                            }
                        )
                    }
                } else {
                    Text(
                        text = "Mapa sin espacios todavia",
                        color = Color.White,
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 16.dp)
                            .background(Color(0x66000000), shape = RoundedCornerShape(12.dp))
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun HostMapCarousel(
    modifier: Modifier = Modifier,
    space: SpaceItem,
    index: Int,
    total: Int,
    bookingCount: Int?,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onViewDetails: () -> Unit
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        shadowElevation = 10.dp,
        color = Color.White
    ) {
        Column(
            modifier = Modifier
                .width(300.dp)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                IconButton(onClick = onPrev, enabled = index > 0) {
                    Icon(Icons.Default.KeyboardArrowLeft, contentDescription = "Prev")
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = space.title, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(text = "${index + 1} / $total", color = Color.Gray, fontSize = 12.sp)
                }
                IconButton(onClick = onNext, enabled = index < total - 1) {
                    Icon(Icons.Default.KeyboardArrowRight, contentDescription = "Next")
                }
            }
            Text(text = space.subtitle ?: space.address, color = Color.Gray, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(
                text = if (space.price <= 0) "Gratis" else "${formatPriceLabel(space.price)} COP",
                fontWeight = FontWeight.SemiBold
            )
            bookingCount?.let {
                Text(
                    text = "Reservas registradas: $it",
                    color = Color.Gray,
                    fontSize = 13.sp
                )
            }
            Button(
                onClick = onViewDetails,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5C1B6C), contentColor = Color.White)
            ) {
                Text("Ver detalles")
            }
        }
    }
}

private data class HostUsageStat(val space: SpaceItem, val bookingCount: Int)

private data class HostUsageInsights(
    val mostReserved: HostUsageStat?,
    val leastReserved: HostUsageStat?,
    val amenityImpacts: List<AmenityImpact> = emptyList()
)

@Composable
private fun HostInsightBanner(
    stats: HostUsageInsights,
    loading: Boolean,
    modifier: Modifier = Modifier,
    onPromote: (SpaceItem) -> Unit,
    onShowDetails: (SpaceItem) -> Unit
) {
    if (!loading && stats.mostReserved == null && stats.leastReserved == null) return
    val underused = stats.leastReserved
    val crowded = stats.mostReserved
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        tonalElevation = 10.dp,
        shadowElevation = 12.dp,
        color = Color.White
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 360.dp)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Mapa inteligente",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
            if (loading) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp
                    )
                    Text("Calculando uso de tus espacios...", color = Color.Gray, fontSize = 13.sp)
                }
            } else {
                crowded?.let {
                    Text(
                        text = "Mas reservado: ${it.space.title} (${it.bookingCount} reservas)",
                        fontSize = 13.sp,
                        color = Color(0xFF5C1B6C),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                underused?.let {
                    Text(
                        text = "Promociona este espacio, esta infrautilizado.",
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = it.space.title,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Reservas registradas: ${it.bookingCount}",
                        color = Color.Gray,
                        fontSize = 13.sp
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = { onPromote(it.space) },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5C1B6C), contentColor = Color.White)
                        ) {
                            Text("Ver en mapa")
                        }
                        OutlinedButton(
                            onClick = { onShowDetails(it.space) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Ver detalles")
                        }
                    }
                }
                if (underused == null && crowded == null) {
                    Text(
                        text = "Aun no hay suficientes reservas para generar sugerencias.",
                        color = Color.Gray,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}

@Composable
fun HostCreateSpaceScreen(navController: NavHostController) {
    val scrollState = rememberScrollState()
    var title by remember { mutableStateOf("") }
    var subtitle by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var latitudeText by remember { mutableStateOf("") }
    var longitudeText by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var capacity by remember { mutableStateOf("") }
    var amenities by remember { mutableStateOf("") }
    var accessibility by remember { mutableStateOf("") }
    var rules by remember { mutableStateOf("") }
    var imageUrlText by remember { mutableStateOf("") }
    var submitting by remember { mutableStateOf(false) }
    var submitError by remember { mutableStateOf<String?>(null) }
    var submitMessage by remember { mutableStateOf<String?>(null) }
    var selectedImageUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var encodedImage by remember { mutableStateOf<String?>(null) }

    val context = LocalContext.current
    val hostRepo = remember { HostRepository() }
    val scope = rememberCoroutineScope()
    val pickImageLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        selectedImageUri = uri
        if (uri != null) {
            scope.launch { encodedImage = encodeImageToBase64(context, uri) }
        } else {
            encodedImage = null
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF7F2FB))
            .verticalScroll(scrollState)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            TextButton(onClick = { navController.popBackStack() }) {
                Text("Cancel", color = Color(0xFF5C1B6C))
            }
            Text(text = "Create a room", fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.width(64.dp))
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedButton(
                    onClick = { pickImageLauncher.launch("image/*") },
                    modifier = Modifier.fillMaxWidth(),
                    border = BorderStroke(1.dp, Color(0xFF5C1B6C)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .background(Color(0xFFF4C7F2), shape = CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.CloudUpload, contentDescription = null, tint = Color(0xFF5C1B6C))
                        }
                        Text(text = "Upload images of the room", fontWeight = FontWeight.SemiBold, color = Color(0xFF5C1B6C))
                    }
                }
                selectedImageUri?.let { uri ->
                    AsyncImage(
                        model = uri,
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .clip(RoundedCornerShape(18.dp))
                    )
                }

                TextFieldHost(value = title, onValueChange = { title = it }, label = "Title of the publication")
                TextFieldHost(value = subtitle, onValueChange = { subtitle = it }, label = "Subtitle")
                TextFieldHost(value = address, onValueChange = { address = it }, label = "Address")
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    Box(modifier = Modifier.weight(1f)) {
                        TextFieldHost(
                            value = latitudeText,
                            onValueChange = { latitudeText = it },
                            label = "Latitude",
                            keyboard = KeyboardType.Number
                        )
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        TextFieldHost(
                            value = longitudeText,
                            onValueChange = { longitudeText = it },
                            label = "Longitude",
                            keyboard = KeyboardType.Number
                        )
                    }
                }
                TextFieldHost(value = price, onValueChange = { price = it }, label = "Cost per hour", keyboard = KeyboardType.Number)
                TextFieldHost(value = capacity, onValueChange = { capacity = it }, label = "Capacity", keyboard = KeyboardType.Number)
                TextFieldHost(value = imageUrlText, onValueChange = { imageUrlText = it }, label = "Image URL (optional)")
                TextFieldHost(value = amenities, onValueChange = { amenities = it }, label = "Amenities (comma separated)", singleLine = false)
                TextFieldHost(value = accessibility, onValueChange = { accessibility = it }, label = "Accessibility notes", singleLine = false)
                TextFieldHost(value = rules, onValueChange = { rules = it }, label = "Rules", singleLine = false)

                submitError?.let { Text(text = it, color = Color(0xFFD32F2F)) }
                submitMessage?.let { Text(text = it, color = Color(0xFF2E7D32)) }

                Button(
                    onClick = {
                        if (submitting) return@Button
                        submitError = null
                        submitMessage = null
                        val priceValue = price.toDoubleOrNull()
                        val capacityValue = capacity.toIntOrNull()
                        when {
                            title.isBlank() -> submitError = "El tÃ­tulo es obligatorio"
                            address.isBlank() -> submitError = "La direcciÃ³n es obligatoria"
                            priceValue == null -> submitError = "Precio invÃ¡lido"
                            capacityValue == null -> submitError = "Capacidad invÃ¡lida"
                            else -> {
                                submitting = true
                                scope.launch {
                                    val geo = buildGeoString(latitudeText, longitudeText)
                                    val imagePayload = encodedImage?.let { "data:image/jpeg;base64,$it" }
                                        ?: imageUrlText.takeIf { it.isNotBlank() }
                                    val input = HostRepository.CreateSpaceInput(
                                        title = title,
                                        subtitle = subtitle,
                                        geo = geo,
                                        address = address,
                                        capacity = capacityValue,
                                        amenities = amenities.split(',').mapNotNull { str -> str.trim().takeIf { it.isNotEmpty() } },
                                        accessibility = accessibility.split(',').mapNotNull { str -> str.trim().takeIf { it.isNotEmpty() } },
                                        imageUrl = imagePayload,
                                        rules = rules,
                                        price = priceValue
                                    )
                                    val result = hostRepo.createSpace(input)
                                    submitting = false
                                    result.fold(onSuccess = {
                                        submitMessage = "Espacio creado correctamente"
                                        showTopToast(context, "Espacio creado")
                                        navController.navigate(Destinations.HostExplore.route) {
                                            popUpTo(Destinations.HostCreateSpace.route) { inclusive = true }
                                        }
                                    }, onFailure = {
                                        submitError = it.message ?: "Error creando espacio"
                                    })
                                }
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 54.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black),
                    border = BorderStroke(1.dp, Color.Black),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Text(if (submitting) "Creando..." else "Create room", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
fun HostProfileScreen(navController: NavHostController, initialUser: UserDto?, onLogout: () -> Unit) {
    ProfileScreen(navController = navController, onLogout = onLogout, roleDescription = "Rol: Host", initialUser = initialUser)
}

@Composable
private fun HostCenteredMessage(text: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(text = text, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun HostErrorCard(message: String, onRetry: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(text = message, color = Color(0xFFD32F2F), fontWeight = FontWeight.SemiBold)
                TextButton(onClick = onRetry) { Text("Reintentar") }
            }
        }
    }
}

private fun SpaceItem.latLng(): LatLng? {
    val lat = latitude
    val lng = longitude
    if (lat != null && lng != null) return LatLng(lat, lng)
    val raw = geo ?: address
    if (raw.isNullOrBlank()) return null
    val regex = Regex("-?\\d+(?:\\.\\d+)?")
    val matches = regex.findAll(raw).mapNotNull { it.value.toDoubleOrNull() }.toList()
    if (matches.size < 2) return null
    val a = matches[0]
    val b = matches[1]
    val latitude = if (kotlin.math.abs(a) > 90 && kotlin.math.abs(b) <= 90) b else a
    val longitude = if (kotlin.math.abs(a) > 90 && kotlin.math.abs(b) <= 90) a else b
    return LatLng(latitude, longitude)
}

@Composable
private fun SearchBarHost(value: String, onValueChange: (String) -> Unit) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 48.dp),
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color(0xFF5C1B6C)) },
        placeholder = { Text("Search your spaces") },
        singleLine = true,
        shape = RoundedCornerShape(30.dp),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Color.White,
            unfocusedContainerColor = Color.White,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent
        )
    )
}

@Composable
private fun TextFieldHost(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    keyboard: KeyboardType = KeyboardType.Text,
    singleLine: Boolean = true
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(text = label, fontWeight = FontWeight.Medium)
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = singleLine,
            keyboardOptions = KeyboardOptions.Default.copy(keyboardType = keyboard),
            shape = RoundedCornerShape(28.dp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            )
        )
    }
}
@Composable
private fun HostQuickActions(
    space: SpaceItem,
    onRefresh: () -> Unit
) {
    val context = LocalContext.current
    val hostRepo = remember { HostRepository() }
    val scope = rememberCoroutineScope()
    var showEditPrice by remember { mutableStateOf(false) }
    var newPrice by remember { mutableStateOf(space.price.takeIf { it > 0 }?.toString().orEmpty()) }
    var actionError by remember { mutableStateOf<String?>(null) }

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = { showEditPrice = true },
                modifier = Modifier.weight(1f)
            ) { Text("Editar precio") }
            OutlinedButton(
                onClick = {
                    scope.launch {
                        actionError = null
                        val result = kotlin.runCatching { ApiProvider.space.getSpaceDetail(space.id) }
                        result.fold(onSuccess = {
                            val activeCount = it.bookings?.size ?: 0
                            showTopToast(context, "Reservas programadas: $activeCount")
                            actionError = null
                        }, onFailure = {
                            actionError = it.message ?: "No se pudo consultar"
                        })
                    }
                },
                modifier = Modifier.weight(1f)
            ) { Text("Ver reservas activas") }
        }
        actionError?.let { Text(it, color = Color(0xFFD32F2F)) }
    }

    if (showEditPrice) {
        AlertDialog(
            onDismissRequest = { showEditPrice = false },
            title = { Text("Actualizar precio por hora") },
            text = {
                OutlinedTextField(
                    value = newPrice,
                    onValueChange = { newPrice = it },
                    keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val parsed = newPrice.toDoubleOrNull()
                    if (parsed == null) {
                        actionError = "Precio invÃ¡lido"
                        return@TextButton
                    }
                    showEditPrice = false
                    scope.launch {
                        val result = hostRepo.updateSpacePrice(space.id, parsed)
                        result.fold(onSuccess = {
                            showTopToast(context, "Precio actualizado")
                            onRefresh()
                            actionError = null
                        }, onFailure = {
                            actionError = it.message ?: "Error actualizando precio"
                        })
                    }
                }) { Text("Guardar") }
            },
            dismissButton = {
                TextButton(onClick = { showEditPrice = false }) { Text("Cancelar") }
            }
        )
    }
}

@Composable
private fun HostEmptyListCard(onCreate: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF4E7FF))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Aún no has publicado espacios", fontWeight = FontWeight.Bold)
            Text(
                text = "Publica tu primer espacio para comenzar a recibir reservas.",
                textAlign = TextAlign.Center,
                color = Color(0xFF5C1B6C)
            )
            Button(
                onClick = onCreate,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5C1B6C), contentColor = Color.White)
            ) { Text("Crear mi primer espacio") }
        }
    }
}

private fun buildGeoString(latitude: String, longitude: String): String? {
    return if (latitude.isNotBlank() && longitude.isNotBlank()) {
        "${latitude.trim()},${longitude.trim()}"
    } else {
        null
    }
}

private suspend fun encodeImageToBase64(context: android.content.Context, uri: Uri): String? {
    return runCatching {
        context.contentResolver.openInputStream(uri)?.use { input ->
            val bytes = input.readBytes()
            Base64.encodeToString(bytes, Base64.NO_WRAP)
        }
    }.getOrNull()
}

