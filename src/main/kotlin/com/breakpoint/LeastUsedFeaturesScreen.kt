package com.breakpoint

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun LeastUsedFeaturesScreen(
    viewModel: FeatureUsageViewModel = hiltViewModel()
) {
    val dashboard by viewModel.dashboard.collectAsState()
    val loading by viewModel.loading.collectAsState()
    val error by viewModel.error.collectAsState()

    // Juan David – Sprint 3 concurrency: this screen triggers parallel feature-usage fetch
    LaunchedEffect(Unit) {
        viewModel.loadDashboard()
    }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        when {
            loading -> {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator()
                    Spacer(Modifier.height(12.dp))
                    Text("Cargando uso de funcionalidades...")
                }
            }
            error != null -> {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Error: $error", color = MaterialTheme.colorScheme.error)
                }
            }
            dashboard != null -> {
                val data = dashboard!!
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Least used features (BQ)", style = MaterialTheme.typography.titleMedium)
                    Text("Extend: ${data.extendCount}")
                    Text("Share: ${data.shareCount}")
                    Text("Reviews: ${data.reviewsCount}")
                    Text("Chat: ${data.chatCount}")
                    Spacer(Modifier.height(12.dp))
                    Text("Least used feature: ${data.leastUsedFeature}", style = MaterialTheme.typography.bodyLarge)
                    Text("Most used feature: ${data.mostUsedFeature}", style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
    }
}
