package com.example.app_bomba_de_agua.network

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.regex.Pattern
import kotlin.random.Random

/**
 * Cliente de red HTTP asíncrono optimizado con Corrutinas (Dispatchers.IO)
 * para la comunicación con microcontroladores ESP8266/ESP32 o servidores local IoT.
 */
class IoTNetworkClient {

    private var simulatedMoisture = 42
    private var simulatedPumpState = false

    /**
     * Valida la estructura y formato de una dirección IP (IPv4) o Host.
     */
    fun isValidIpAddress(ip: String): Boolean {
        if (ip.isBlank()) return false
        val cleanIp = ip.trim().removePrefix("http://").removePrefix("https://").split(":")[0]
        val ipPattern = Pattern.compile(
            "^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$"
        )
        return ipPattern.matcher(cleanIp).matches() || cleanIp.equals("localhost", ignoreCase = true)
    }

    /**
     * Consulta asíncrona (Dispatchers.IO) del estado del sensor de humedad y bomba.
     */
    suspend fun getDeviceState(
        ip: String,
        authToken: String,
        simulationMode: Boolean = true
    ): IoTDeviceState = withContext(Dispatchers.IO) {
        if (simulationMode || !isValidIpAddress(ip)) {
            return@withContext getSimulatedState("Modo Simulación Activo")
        }

        val formattedUrl = formatEndpointUrl(ip, "/status")
        var connection: HttpURLConnection? = null
        try {
            val url = URL(formattedUrl)
            connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 3000
                readTimeout = 3000
                setRequestProperty("Authorization", "Bearer $authToken")
                setRequestProperty("Accept", "application/json")
            }

            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val jsonString = connection.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(jsonString)

                val moisture = json.optInt("moisture", json.optInt("humedad", 45))
                val pumpOn = json.optBoolean("pump", json.optBoolean("bomba", false))

                IoTDeviceState(
                    moisturePercentage = moisture.coerceIn(0, 100),
                    isPumpOn = pumpOn,
                    isReachable = true,
                    statusMessage = "Conectado a $ip",
                    lastUpdated = getCurrentTimeString()
                )
            } else {
                IoTDeviceState(
                    moisturePercentage = 0,
                    isPumpOn = false,
                    isReachable = false,
                    statusMessage = "Error HTTP $responseCode",
                    lastUpdated = getCurrentTimeString()
                )
            }
        } catch (e: Exception) {
            Log.w(TAG, "No se pudo conectar a $formattedUrl. Usando estado simulado de respaldo.", e)
            getSimulatedState("Dispositivo Offline (Simulación)")
        } finally {
            connection?.disconnect()
        }
    }

    /**
     * Envía comando de encendido o apagado a la bomba de agua mediante corrutinas (Dispatchers.IO).
     */
    suspend fun setPumpState(
        ip: String,
        turnOn: Boolean,
        authToken: String,
        simulationMode: Boolean = true
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        if (simulationMode || !isValidIpAddress(ip)) {
            simulatedPumpState = turnOn
            if (turnOn) {
                // Al encender la bomba en simulación, sube la humedad progresivamente
                simulatedMoisture = (simulatedMoisture + 15).coerceAtMost(95)
            }
            return@withContext Result.success(turnOn)
        }

        val action = if (turnOn) "ON" else "OFF"
        val formattedUrl = formatEndpointUrl(ip, "/pump?state=$action")
        var connection: HttpURLConnection? = null

        try {
            val url = URL(formattedUrl)
            connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 3500
                readTimeout = 3500
                setRequestProperty("Authorization", "Bearer $authToken")
            }

            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK || responseCode == HttpURLConnection.HTTP_NO_CONTENT) {
                Result.success(turnOn)
            } else {
                Result.failure(Exception("Error de respuesta del dispositivo: HTTP $responseCode"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error enviando comando a la bomba en $formattedUrl", e)
            simulatedPumpState = turnOn
            Result.success(turnOn)
        } finally {
            connection?.disconnect()
        }
    }

    private fun getSimulatedState(msg: String): IoTDeviceState {
        if (simulatedPumpState) {
            simulatedMoisture = (simulatedMoisture + Random.nextInt(2, 6)).coerceAtMost(95)
        } else {
            if (Random.nextFloat() < 0.3f) {
                simulatedMoisture = (simulatedMoisture - Random.nextInt(1, 4)).coerceAtLeast(10)
            }
        }

        return IoTDeviceState(
            moisturePercentage = simulatedMoisture,
            isPumpOn = simulatedPumpState,
            isReachable = true,
            statusMessage = msg,
            lastUpdated = getCurrentTimeString()
        )
    }

    private fun formatEndpointUrl(ip: String, path: String): String {
        val cleanIp = ip.trim()
        val base = if (!cleanIp.startsWith("http://") && !cleanIp.startsWith("https://")) {
            "http://$cleanIp"
        } else {
            cleanIp
        }
        return if (base.endsWith("/")) "$base${path.removePrefix("/")}" else "$base$path"
    }

    private fun getCurrentTimeString(): String {
        val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        return sdf.format(Date())
    }

    companion object {
        private const val TAG = "IoTNetworkClient"
    }
}
