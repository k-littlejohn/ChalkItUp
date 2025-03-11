package com.example.chalkitup.ui

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class Connection private constructor(context: Context) {

    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private val _connectionStatus = MutableStateFlow(checkConnectivity())
    val connectionStatus: StateFlow<Boolean> get() = _connectionStatus

    companion object {
        @Volatile
        private var instance: Connection? = null

        fun getInstance(context: Context): Connection {
            return instance ?: synchronized(this) {
                instance ?: Connection(context.applicationContext).also { instance = it }
            }
        }
    }

    fun checkConnectivity(): Boolean {
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
    }

    val isConnected: Boolean
        get() = checkConnectivity()
}