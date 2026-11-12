package com.example.app_bomba_de_agua.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.app_bomba_de_agua.network.IoTDeviceState
import com.example.app_bomba_de_agua.network.IoTNetworkClient
import com.example.app_bomba_de_agua.security.SecureStorage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    secureStorage: SecureStorage,
    networkClient: IoTNetworkClient,
    isWifiConnected: Boolean,
    onNavigateToSettings: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var deviceIp by remember { mutableStateOf(secureStorage.getDeviceIp()) }
    val authToken by remember { mutableStateOf(secureStorage.getAuthToken()) }
    var isSimulationMode by remember { mutableStateOf(secureStorage.isSimulationMode()) }

    var deviceState by remember { mutableStateOf(IoTDeviceState()) }
    var isFetching by remember { mutableStateOf(false) }
    var isActionInProgress by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    fun refreshDeviceState() {
        scope.launch {
            isFetching = true
            deviceState = networkClient.getDeviceState(deviceIp, authToken, isSimulationMode)
            isFetching = false
        }
    }

    LaunchedEffect(deviceIp, isSimulationMode) {
        while (true) {
            refreshDeviceState()
            delay(4000)
        }
    }

    val scrollState = rememberScrollState()

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(scrollState)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            WifiConnectionCard(
                isWifiConnected = isWifiConnected,
                deviceIp = deviceIp,
                isSimulationMode = isSimulationMode,
                statusMessage = deviceState.statusMessage,
                onToggleSimulation = { enabled ->
                    isSimulationMode = enabled
                    secureStorage.setSimulationMode(enabled)
                    refreshDeviceState()
                },
                onNavigateToSettings = onNavigateToSettings
            )

            Spacer(modifier = Modifier.height(16.dp))

            MoistureGaugeCard(
                moisturePercentage = deviceState.moisturePercentage,
                lastUpdated = deviceState.lastUpdated,
                isFetching = isFetching,
                onRefresh = { refreshDeviceState() }
            )

            Spacer(modifier = Modifier.height(16.dp))

            PumpControlCard(
                isPumpOn = deviceState.isPumpOn,
                isActionInProgress = isActionInProgress,
                onTogglePump = {
                    scope.launch {
                        isActionInProgress = true
                        val targetState = !deviceState.isPumpOn
                        val result = networkClient.setPumpState(
                            ip = deviceIp,
                            turnOn = targetState,
                            authToken = authToken,
                            simulationMode = isSimulationMode
                        )

                        result.onSuccess {
                            refreshDeviceState()
                            val msg = if (targetState) "Bomba Encendida Correctamente" else "Bomba Apagada"
                            snackbarHostState.showSnackbar(msg)
                        }.onFailure { err ->
                            snackbarHostState.showSnackbar("Error enviando comando: ${err.localizedMessage}")
                        }
                        isActionInProgress = false
                    }
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            SystemInfoCard(
                deviceState = deviceState,
                deviceIp = deviceIp
            )
        }
    }
}

@Composable
private fun WifiConnectionCard(
    isWifiConnected: Boolean,
    deviceIp: String,
    isSimulationMode: Boolean,
    statusMessage: String,
    onToggleSimulation: (Boolean) -> Unit,
    onNavigateToSettings: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (isWifiConnected) Icons.Default.Wifi else Icons.Default.WifiOff,
                        contentDescription = "Wi-Fi Status",
                        tint = if (isWifiConnected) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = if (isWifiConnected) "Wi-Fi Conectado" else "Sin Conexión Wi-Fi",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "IP IoT: $deviceIp",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                IconButton(onClick = onNavigateToSettings) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Configuración IP",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f))
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (isSimulationMode) Icons.Default.BugReport else Icons.Default.Router,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.secondary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isSimulationMode) "Modo Simulación (Demo)" else statusMessage,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Simular",
                        style = MaterialTheme.typography.labelSmall
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Switch(
                        checked = isSimulationMode,
                        onCheckedChange = onToggleSimulation,
                        modifier = Modifier.height(24.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun MoistureGaugeCard(
    moisturePercentage: Int,
    lastUpdated: String,
    isFetching: Boolean,
    onRefresh: () -> Unit
) {
    val isDry = moisturePercentage < 35
    val isOptimal = moisturePercentage in 35..75

    val gaugeColor by animateColorAsState(
        targetValue = when {
            isDry -> Color(0xFFD32F2F)
            isOptimal -> Color(0xFFF57C00)
            else -> Color(0xFF0288D1)
        },
        animationSpec = tween(durationMillis = 500),
        label = "gaugeColor"
    )

    val progressAnimated by animateFloatAsState(
        targetValue = moisturePercentage / 100f,
        animationSpec = tween(durationMillis = 800),
        label = "progressAnimated"
    )

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Sensor Humedad del Suelo",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )

                IconButton(onClick = onRefresh, enabled = !isFetching) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refrescar lectura",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(170.dp)
            ) {
                CircularProgressIndicator(
                    progress = { 1f },
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    strokeWidth = 14.dp,
                )
                CircularProgressIndicator(
                    progress = { progressAnimated },
                    modifier = Modifier.fillMaxSize(),
                    color = gaugeColor,
                    strokeWidth = 14.dp,
                )

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "$moisturePercentage%",
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 42.sp
                        ),
                        color = gaugeColor
                    )
                    Text(
                        text = "Humedad",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            LinearProgressIndicator(
                progress = { progressAnimated },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(5.dp)),
                color = gaugeColor,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            Surface(
                color = gaugeColor.copy(alpha = 0.12f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (isDry) Icons.Default.Warning else Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = gaugeColor,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = when {
                                isDry -> "¡ALERTA: SUELO SECO!"
                                isOptimal -> "Humedad Moderada"
                                else -> "Suelo Húmedo / Óptimo"
                            },
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = gaugeColor
                        )
                        Text(
                            text = when {
                                isDry -> "Se requiere encender la bomba de agua."
                                isOptimal -> "Nivel aceptable para el cultivo."
                                else -> "No se requiere riego adicional."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Última actualización: $lastUpdated",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun PumpControlCard(
    isPumpOn: Boolean,
    isActionInProgress: Boolean,
    onTogglePump: () -> Unit
) {
    val buttonColor by animateColorAsState(
        targetValue = if (isPumpOn) Color(0xFF1976D2) else Color(0xFF757575),
        animationSpec = tween(durationMillis = 400),
        label = "buttonColor"
    )

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 5.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Control Manual del Actuador",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onTogglePump,
                enabled = !isActionInProgress,
                modifier = Modifier
                    .size(140.dp)
                    .clip(CircleShape),
                colors = ButtonDefaults.buttonColors(
                    containerColor = buttonColor
                ),
                contentPadding = PaddingValues(0.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    if (isActionInProgress) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(36.dp)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.PowerSettingsNew,
                            contentDescription = "Encender/Apagar Bomba",
                            tint = Color.White,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (isPumpOn) "ENCENDIDA" else "APAGADA",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            ),
                            color = Color.White
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = if (isPumpOn) "Estado: BOMBA REGANDO AGUA 💧" else "Estado: Bomba en Reposo",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = if (isPumpOn) Color(0xFF1976D2) else MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun SystemInfoCard(
    deviceState: IoTDeviceState,
    deviceIp: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Detalles Técnicos IoT",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Dirección IP:", style = MaterialTheme.typography.bodySmall)
                Text(deviceIp, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Protocolo de Red:", style = MaterialTheme.typography.bodySmall)
                Text("HTTP GET / Corrutinas IO", style = MaterialTheme.typography.bodySmall)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Norma Seguridad:", style = MaterialTheme.typography.bodySmall)
                Text("ISO 27400 AES-256", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
            }
        }
    }
}
