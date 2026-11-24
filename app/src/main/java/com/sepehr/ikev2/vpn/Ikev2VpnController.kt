package com.sepehr.ikev2.vpn

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Ikev2VpnProfile
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.VpnManager
import android.os.Build
import com.sepehr.ikev2.data.SecretTunnelSpec
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class VpnConnectionState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    ERROR
}

class Ikev2VpnController(context: Context) {

    private val vpnManager = context.getSystemService(VpnManager::class.java)
    private val connectivityManager = context.getSystemService(ConnectivityManager::class.java)

    private val _state = MutableStateFlow(VpnConnectionState.DISCONNECTED)
    val state: StateFlow<VpnConnectionState> = _state.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private var pendingStartAfterConsent = false

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            val caps = connectivityManager.getNetworkCapabilities(network)
            if (caps?.hasTransport(NetworkCapabilities.TRANSPORT_VPN) == true) {
                _state.value = VpnConnectionState.CONNECTED
                _errorMessage.value = null
            }
        }

        override fun onLost(network: Network) {
            if (!hasActiveVpnTransport()) {
                _state.value = VpnConnectionState.DISCONNECTED
            }
        }
    }

    init {
        val request = NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_VPN)
            .removeCapability(NetworkCapabilities.NET_CAPABILITY_NOT_VPN)
            .build()
        connectivityManager.registerNetworkCallback(request, networkCallback)

        if (hasActiveVpnTransport()) {
            _state.value = VpnConnectionState.CONNECTED
        }
    }

    private fun hasActiveVpnTransport(): Boolean {
        val active = connectivityManager.activeNetwork ?: return false
        val caps = connectivityManager.getNetworkCapabilities(active) ?: return false
        return caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN)
    }

    /**
     * Builds a native Android Ikev2VpnProfile:
     * - serverAddr (Hostname) = spec.serverHost (IP address)
     * - identity (Remote ID) = spec.serverIdentity (Domain name)
     * - Auth = EAP-MSCHAPv2 username & password fetched live from JSON
     */
    fun connect(spec: SecretTunnelSpec): Intent? {
        return try {
            _errorMessage.value = null
            _state.value = VpnConnectionState.CONNECTING

            val profile = Ikev2VpnProfile.Builder(spec.serverHost, spec.serverIdentity)
                .setAuthUsernamePassword(spec.ikeUsername, spec.ikePassword, null)
                .setBypassable(false)
                .setMetered(false)
                .build()

            val consentIntent = vpnManager.provisionVpnProfile(profile)
            if (consentIntent != null) {
                pendingStartAfterConsent = true
                consentIntent
            } else {
                startProvisionedTunnel()
                null
            }
        } catch (e: Exception) {
            _state.value = VpnConnectionState.ERROR
            _errorMessage.value = "برقراری تونل IKEv2 با خطا مواجه شد"
            null
        }
    }

    fun onConsentResult(granted: Boolean) {
        if (granted && pendingStartAfterConsent) {
            pendingStartAfterConsent = false
            startProvisionedTunnel()
        } else {
            pendingStartAfterConsent = false
            _state.value = VpnConnectionState.DISCONNECTED
            _errorMessage.value = "مجوز اتصال VPN توسط کاربر تایید نشد"
        }
    }

    private fun startProvisionedTunnel() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                vpnManager.startProvisionedVpnProfileSession()
            } else {
                @Suppress("DEPRECATION")
                vpnManager.startProvisionedVpnProfile()
            }
            _state.value = VpnConnectionState.CONNECTED
        } catch (e: Exception) {
            _state.value = VpnConnectionState.ERROR
            _errorMessage.value = "امکان شروع اتصال IKEv2 وجود ندارد"
        }
    }

    fun disconnect() {
        try {
            vpnManager.stopProvisionedVpnProfile()
        } finally {
            _state.value = VpnConnectionState.DISCONNECTED
            _errorMessage.value = null
        }
    }

    fun release() {
        runCatching {
            connectivityManager.unregisterNetworkCallback(networkCallback)
        }
    }
}
