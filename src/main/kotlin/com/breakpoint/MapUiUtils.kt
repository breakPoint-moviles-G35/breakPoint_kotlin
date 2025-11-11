package com.breakpoint

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.Color as AndroidColor
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import java.text.NumberFormat
import java.util.Locale

fun createPriceMarkerBitmapDescriptor(
    context: Context,
    price: Int,
    selected: Boolean
): BitmapDescriptor {
    val density = context.resources.displayMetrics.density
    val horizontalPadding = 12f * density
    val verticalPadding = 8f * density
    val cornerRadius = 16f * density
    val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 14f * density
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        color = if (selected) AndroidColor.WHITE else AndroidColor.parseColor("#5C1B6C")
    }
    val priceText = formatPriceLabel(price)
    val textWidth = textPaint.measureText(priceText)
    val textHeight = textPaint.fontMetrics.run { descent - ascent }
    val width = (textWidth + horizontalPadding * 2).toInt().coerceAtLeast((48f * density).toInt())
    val height = (textHeight + verticalPadding * 2).toInt()
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (selected) AndroidColor.parseColor("#5C1B6C") else AndroidColor.WHITE
    }
    val rect = RectF(0f, 0f, width.toFloat(), height.toFloat())
    canvas.drawRoundRect(rect, cornerRadius, cornerRadius, backgroundPaint)
    val textX = (width - textWidth) / 2f
    val textY = verticalPadding - textPaint.ascent()
    canvas.drawText(priceText, textX, textY, textPaint)
    return BitmapDescriptorFactory.fromBitmap(bitmap)
}

fun createUserLocationBitmapDescriptor(context: Context): BitmapDescriptor {
    val density = context.resources.displayMetrics.density
    val size = (16f * density).toInt().coerceAtLeast(12)
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val outerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.parseColor("#5C1B6C")
    }
    val innerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.WHITE
    }
    val center = size / 2f
    canvas.drawCircle(center, center, center.toFloat(), outerPaint)
    canvas.drawCircle(center, center, center * 0.55f, innerPaint)
    return BitmapDescriptorFactory.fromBitmap(bitmap)
}

fun formatPriceLabel(price: Int): String {
    return if (price <= 0) {
        "Gratis"
    } else {
        val formatter = NumberFormat.getNumberInstance(Locale.getDefault())
        "$${formatter.format(price)}"
    }
}

@Composable
fun SpaceMarkerInfoWindowContent(
    title: String,
    subtitle: String?,
    rating: Double,
    onNavigate: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        tonalElevation = 6.dp,
        shadowElevation = 12.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .widthIn(min = 180.dp, max = 260.dp)
            .clickable { onNavigate() }
    ) {
        Column(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.98f))
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            subtitle?.takeIf { it.isNotBlank() }?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    androidx.compose.material3.Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = Color(0xFFFFD54F)
                    )
                    Text(
                        text = if (rating > 0) String.format(Locale.getDefault(), "%.1f", rating) else "N/A",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Ver detalles",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                    androidx.compose.material3.Icon(
                        imageVector = Icons.Default.ArrowForward,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 2.dp).widthIn(min = 16.dp)
                    )
                }
            }
        }
    }
}
