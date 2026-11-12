package com.example.app_bomba_de_agua.network

/**
 * Modelo de datos que representa el estado del dispositivo IoT (ESP8266 / ESP32).
 */
data class IoTDeviceState(
    val moisturePercentage: Int = 50,
    val isPumpOn: Boolean = false,
    val isReachable: Boolean = false,
    val statusMessage: String = "Esperando conexión",
    val lastUpdated: String = "--:--:--"
)
