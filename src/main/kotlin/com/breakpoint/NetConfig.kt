package com.breakpoint

// Pequeños helpers para evitar referencias no resueltas por nombres calificados
fun setServerBaseUrl(url: String) {
    ApiProvider.updateBaseUrl(url)
}

fun getServerBaseUrl(): String = ApiProvider.currentBaseUrl()

suspend fun pingServerUrl(url: String): Boolean = ApiProvider.testConnectivity(url)


