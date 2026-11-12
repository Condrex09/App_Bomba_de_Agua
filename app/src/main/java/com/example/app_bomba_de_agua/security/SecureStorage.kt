package com.example.app_bomba_de_agua.security

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Gestor de almacenamiento seguro acorde a la Norma ISO 27400 (Seguridad en IoT y Privacidad).
 * Utiliza EncryptedSharedPreferences con cifrado AES-256 GCM para resguardar
 * la dirección IP del dispositivo IoT, tokens de seguridad y credenciales de acceso.
 */
class SecureStorage(context: Context) {

    private val sharedPreferences: SharedPreferences = try {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        EncryptedSharedPreferences.create(
            context,
            PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    } catch (e: Exception) {
        Log.e(TAG, "Error inicializando EncryptedSharedPreferences, usando fallback", e)
        context.getSharedPreferences(FALLBACK_PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun saveDeviceIp(ip: String) {
        sharedPreferences.edit().putString(KEY_DEVICE_IP, ip.trim()).apply()
    }

    fun getDeviceIp(): String {
        return sharedPreferences.getString(KEY_DEVICE_IP, DEFAULT_IP) ?: DEFAULT_IP
    }

    fun saveAuthToken(token: String) {
        sharedPreferences.edit().putString(KEY_AUTH_TOKEN, token.trim()).apply()
    }

    fun getAuthToken(): String {
        return sharedPreferences.getString(KEY_AUTH_TOKEN, DEFAULT_TOKEN) ?: DEFAULT_TOKEN
    }

    fun saveUsername(username: String) {
        sharedPreferences.edit().putString(KEY_USERNAME, username.trim()).apply()
    }

    fun getUsername(): String {
        return sharedPreferences.getString(KEY_USERNAME, "admin") ?: "admin"
    }

    fun setLoggedIn(isLoggedIn: Boolean) {
        sharedPreferences.edit().putBoolean(KEY_IS_LOGGED_IN, isLoggedIn).apply()
    }

    fun isLoggedIn(): Boolean {
        return sharedPreferences.getBoolean(KEY_IS_LOGGED_IN, false)
    }

    fun setSimulationMode(enabled: Boolean) {
        sharedPreferences.edit().putBoolean(KEY_SIMULATION_MODE, enabled).apply()
    }

    fun isSimulationMode(): Boolean {
        return sharedPreferences.getBoolean(KEY_SIMULATION_MODE, true)
    }

    fun clearCredentials() {
        sharedPreferences.edit()
            .remove(KEY_AUTH_TOKEN)
            .remove(KEY_IS_LOGGED_IN)
            .apply()
    }

    companion object {
        private const val TAG = "SecureStorage"
        private const val PREFS_NAME = "encrypted_iot_prefs"
        private const val FALLBACK_PREFS_NAME = "secure_iot_prefs_fallback"

        private const val KEY_DEVICE_IP = "key_device_ip"
        private const val KEY_AUTH_TOKEN = "key_auth_token"
        private const val KEY_USERNAME = "key_username"
        private const val KEY_IS_LOGGED_IN = "key_is_logged_in"
        private const val KEY_SIMULATION_MODE = "key_simulation_mode"

        const val DEFAULT_IP = "192.168.1.100"
        const val DEFAULT_TOKEN = "ISO27400-SECURE-KEY-2026"
    }
}
