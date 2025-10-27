package com.breakpoint

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController

@Composable
fun RecommendationsBlock(
    navController: NavHostController,
    userId: String,
    modifier: Modifier = Modifier
) {
    if (userId.isBlank()) return

    val repository = remember { SpaceRepository() }
    var recommendations by remember { mutableStateOf<List<SpaceDto>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(userId) {
        loading = true
        val result = repository.getRecommendations(userId)
        loading = false
        result.fold(
            onSuccess = { spaces -> recommendations = spaces },
            onFailure = { recommendations = emptyList() }
        )
    }

    if (loading || recommendations.isEmpty()) {
        return
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Recomendadas para ti",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(recommendations) { space ->
                SpaceCard(
                    space = space,
                    onClick = {
                        navController.navigate(Destinations.DetailedSpace.createRoute(space.id))
                    },
                    modifier = Modifier.width(260.dp),
                    showLocation = false,
                    showPrice = true,
                    showDetailsButton = true,
                    compact = true
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
    }
}
