package com.onplus.vpn.service

import android.content.Intent
import android.net.VpnService
import android.os.ParcelFileDescriptor
import android.util.Log
import com.onplus.vpn.model.VpnProtocol

class OnPlusVpnService : VpnService() {

    private var vpnInterface: ParcelFileDescriptor? = null
    private var isRunning = false

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        when (action) {
            ACTION_CONNECT -> {
                val protocol = intent.getStringExtra(EXTRA_PROTOCOL)
                val config = intent.getStringExtra(EXTRA_CONFIG)
                connectVpn(protocol, config)
            }
            ACTION_DISCONNECT -> {
                disconnectVpn()
            }
        }
        return START_STICKY
    }

    private fun connectVpn(protocolStr: String?, config: String?) {
        if (isRunning) return

        try {
            val builder = Builder()
                .addAddress("10.0.0.2", 24)
                .addDnsServer("8.8.8.8")
                .addDnsServer("1.1.1.1")
                .addRoute("0.0.0.0", 0)
                .setSession("ON PLUS VPN")

            vpnInterface = builder.establish()
            isRunning = true

            when (protocolStr) {
                VpnProtocol.OPENVPN.name -> startOpenVpn(config)
                VpnProtocol.WIREGUARD.name -> startWireguard(config)
                VpnProtocol.V2RAY.name -> startV2Ray(config)
                VpnProtocol.IKEV2.name -> startIKEv2(config)
            }

        } catch (e: Exception) {
            Log.e("OnPlusVpn", "Error connecting VPN", e)
            disconnectVpn()
        }
    }

    private fun startOpenVpn(config: String?) {}
    private fun startWireguard(config: String?) {}
    private fun startV2Ray(config: String?) {}
    private fun startIKEv2(config: String?) {}

    private fun disconnectVpn() {
        try {
            vpnInterface?.close()
            vpnInterface = null
            isRunning = false
            stopSelf()
        } catch (e: Exception) {
            Log.e("OnPlusVpn", "Error disconnecting", e)
        }
    }

    override fun onDestroy() {
        disconnectVpn()
        super.onDestroy()
    }

    companion object {
        const val ACTION_CONNECT = "com.onplus.vpn.CONNECT"
        const val ACTION_DISCONNECT = "com.onplus.vpn.DISCONNECT"
        const val EXTRA_PROTOCOL = "EXTRA_PROTOCOL"
        const val EXTRA_CONFIG = "EXTRA_CONFIG"
    }
}
