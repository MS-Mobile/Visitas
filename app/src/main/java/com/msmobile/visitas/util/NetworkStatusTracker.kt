package com.msmobile.visitas.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate

class NetworkStatusTracker(context: Context) {
    private var hasAcknowledgedCurrentState = false
    private val connectivityManager by lazy {
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
    }

    val networkStatus: Flow<Boolean> = callbackFlow {
        val networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                if (!hasAcknowledgedCurrentState) {
                    hasAcknowledgedCurrentState = true
                    return
                }
                trySend(true)
            }

            override fun onLost(network: Network) {
                if (!hasAcknowledgedCurrentState) {
                    hasAcknowledgedCurrentState = true
                    return
                }
                trySend(false)
            }
        }

        val networkRequest = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        connectivityManager?.registerNetworkCallback(networkRequest, networkCallback)

        awaitClose {
            connectivityManager?.unregisterNetworkCallback(networkCallback)
        }
    }.conflate()
}