package com.kimchi.deliverybot.storage

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.net.wifi.WifiManager
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map

// Extension property for Context to create a single DataStore instance
private val Context.ipDataStore: DataStore<Preferences> by preferencesDataStore(name = "ip_preferences")

/**
 * Repository class to handle storage operations using DataStore
 */
class DataStoreRepository(private val context: Context) {

    companion object {
        // Define the preference key
        private val IP_ADDRESS_KEY = stringPreferencesKey("ip_address")
    }

    /**
     * Save an IP address to DataStore
     * @param ipAddress The IP address to store
     */
    suspend fun saveIPAddress(ipAddress: String) {
        context.ipDataStore.edit { preferences ->
            preferences[IP_ADDRESS_KEY] = ipAddress
        }
    }

    /**
     * Get the stored IP address as a Flow
     * @return Flow containing the IP address string or null if not found
     */
    val ipAddressFlow: Flow<String?> = context.ipDataStore.data
        .map { preferences ->
            preferences[IP_ADDRESS_KEY]
        }

    /**
     * Clear the stored IP address
     */
    suspend fun clearIPAddress() {
        context.ipDataStore.edit { preferences ->
            preferences.remove(IP_ADDRESS_KEY)
        }
    }

    /**
     * Get the current IP address as a one-time operation
     * @return The current IP address or null if not set
     */
    suspend fun getCurrentIPAddress(): String? {
         val ip = context.ipDataStore.data.map { preferences ->
            preferences[IP_ADDRESS_KEY]
        }.firstOrNull()

        if (!checkIfIpIsInCurrentNetwork(ip)) {
            clearIPAddress()
            return null
        }

        return ip
    }

    /**
     * Checks if the passed IP is part of the current network where the device is connected.
     */
    private fun checkIfIpIsInCurrentNetwork(ip :String?): Boolean {
        if (ip == null) {
            return false
        }
        val wifiManager = context.getSystemService(Context.WIFI_SERVICE) as WifiManager

        val ipAddress = wifiManager.connectionInfo.ipAddress
        val formattedIP = String.format(
            "%d.%d.%d",
            ipAddress and 0xff,
            (ipAddress shr 8) and 0xff,
            (ipAddress shr 16) and 0xff
        )

        return ip.contains(formattedIP)
    }
}
