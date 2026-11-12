package com.example.app_bomba_de_agua.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.app_bomba_de_agua.network.IoTNetworkClient
import com.example.app_bomba_de_agua.security.SecureStorage
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    secureStorage: SecureStorage,
    networkClient: IoTNetworkClient,
    onSavedAndBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var ipInput by remember { mutableStateOf(secureStorage.getDeviceIp()) }
    var tokenInput by remember { mutableStateOf(secureStorage.getAuthToken()) }

    var isTestingConnection by remember { mutableStateOf(false) }
    var testResultMsg by remember { mutableStateOf<String?>(null) }
    var isTestSuccess by remember { mutableStateOf(false) }

    val isValidIp = remember(ipInput) { networkClient.isValidIpAddress(ipInput) }
    val scrollState = rememberScrollState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(scrollState)
                .padding(vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Router,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Configuración del Dispositivo IoT",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "Dirección IP del ESP8266 / ESP32 o Servidor",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    OutlinedTextField(
                        value = ipInput,
                        onValueChange = {
                            ipInput = it
                            testResultMsg = null
                        },
                        label = { Text("Dirección IP (Ej: 192.168.1.100)") },
                        leadingIcon = {
                            Icon(Icons.Default.Dns, contentDescription = "IP")
                        },
                        trailingIcon = {
                            if (ipInput.isNotBlank()) {
                                Icon(
                                    imageVector = if (isValidIp) Icons.Default.CheckCircle else Icons.Default.Error,
                                    contentDescription = "Estado formato IP",
                                    tint = if (isValidIp) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error
                                )
                            }
                        },
                        isError = ipInput.isNotBlank() && !isValidIp,
                        supportingText = {
                            if (ipInput.isNotBlank() && !isValidIp) {
                                Text(
                                    text = "Formato de dirección IP inválido (Ejemplo válido: 192.168.1.50)",
                                    color = MaterialTheme.colorScheme.error
                                )
                            } else if (isValidIp) {
                                Text(
                                    text = "Formato de IP válido",
                                    color = Color(0xFF2E7D32)
                                )
                            }
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                isTestingConnection = true
                                testResultMsg = null
                                val state = networkClient.getDeviceState(
                                    ip = ipInput,
                                    authToken = tokenInput,
                                    simulationMode = false
                                )
                                isTestingConnection = false
                                isTestSuccess = state.isReachable
                                testResultMsg = if (state.isReachable) {
                                    "¡Conexión Exitosa! Dispositivo respondiendo a la IP $ipInput"
                                } else {
                                    "No se pudo contactar con $ipInput. Verifique red Wi-Fi o active Modo Simulación."
                                }
                            }
                        },
                        enabled = isValidIp && !isTestingConnection,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (isTestingConnection) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Probando Conexión...")
                        } else {
                            Icon(Icons.Default.NetworkCheck, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Probar Conexión con Dispositivo")
                        }
                    }

                    AnimatedVisibility(visible = testResultMsg != null) {
                        testResultMsg?.let { msg ->
                            Spacer(modifier = Modifier.height(12.dp))
                            Surface(
                                color = if (isTestSuccess) Color(0xFFE8F5E9) else MaterialTheme.colorScheme.errorContainer,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = msg,
                                    modifier = Modifier.padding(12.dp),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (isTestSuccess) Color(0xFF1B5E20) else MaterialTheme.colorScheme.onErrorContainer,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Token / Clave de Acceso (ISO 27400)",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = tokenInput,
                        onValueChange = { tokenInput = it },
                        label = { Text("Token de Autenticación de la Red IoT") },
                        leadingIcon = {
                            Icon(Icons.Default.VpnKey, contentDescription = null)
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    if (isValidIp) {
                        secureStorage.saveDeviceIp(ipInput)
                        secureStorage.saveAuthToken(tokenInput)
                        onSavedAndBack()
                    }
                },
                enabled = isValidIp,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Default.Save, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Guardar Configuración Segura", fontSize = 16.sp)
            }
        }
    }
}
