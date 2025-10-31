package com.breakpoint

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun ReviewScreen(spaceId: String, navController: NavHostController) {
    val primary = MaterialTheme.colorScheme.primary
    val ctx = LocalContext.current
    var comment by remember { mutableStateOf("") }
    var ratingComfort by remember { mutableIntStateOf(0) }
    var ratingPunctuality by remember { mutableIntStateOf(0) }
    var ratingCleanliness by remember { mutableIntStateOf(0) }
    var isSubmitting by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Califica tu experiencia",
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            color = primary,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(20.dp))

        LabeledStars(label = "Comodidad", value = ratingComfort, onSelect = { ratingComfort = it }, primary = primary)
        Spacer(modifier = Modifier.height(12.dp))
        LabeledStars(label = "Puntualidad", value = ratingPunctuality, onSelect = { ratingPunctuality = it }, primary = primary)
        Spacer(modifier = Modifier.height(12.dp))
        LabeledStars(label = "Limpieza", value = ratingCleanliness, onSelect = { ratingCleanliness = it }, primary = primary)

        Spacer(modifier = Modifier.height(16.dp))

        TextField(
            value = comment,
            onValueChange = { comment = it },
            modifier = Modifier
                .fillMaxWidth(),
            minLines = 4,
            maxLines = 6,
            label = { Text("Comentarios (opcional)") },
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White
            )
        )

        Spacer(modifier = Modifier.height(24.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(
                enabled = !isSubmitting && listOf(ratingComfort, ratingPunctuality, ratingCleanliness).all { it in 1..5 },
                onClick = {
                    isSubmitting = true
                    CoroutineScope(Dispatchers.Main).launch {
                        val repo = ReviewRepository()
                        // Por ahora enviamos un único rating promedio y comentario opcional
                        val avg = listOf(ratingComfort, ratingPunctuality, ratingCleanliness).average().toInt().coerceIn(1,5)
                        val res = repo.submit(spaceId, avg, comment.trim())
                        res.fold(onSuccess = {
                            showTopToast(ctx, "¡Gracias por tu reseña!")
                            navController.navigate(Destinations.Reservations.route)
                        }, onFailure = {
                            showTopToast(ctx, it.message ?: "Error enviando reseña")
                        })
                        isSubmitting = false
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = primary),
                modifier = Modifier.weight(1f)
            ) {
                Text(text = if (isSubmitting) "Enviando…" else "Enviar")
            }
            Button(
                onClick = { navController.popBackStack() },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEDE7F0)),
                modifier = Modifier.weight(1f)
            ) {
                Text(text = "Cancelar", color = Color(0xFF5C1B6C))
            }
        }
    }
}


@Composable
private fun LabeledStars(label: String, value: Int, onSelect: (Int) -> Unit, primary: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Text(text = label.lowercase().replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() })
        Spacer(modifier = Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
            (1..5).forEach { idx ->
                val tint = if (idx <= value) primary else Color(0xFFCCCCCC)
                Icon(
                    imageVector = Icons.Filled.Star,
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier
                        .padding(horizontal = 8.dp)
                        .height(40.dp)
                        .clickable { onSelect(idx) }
                )
            }
        }
    }
}


