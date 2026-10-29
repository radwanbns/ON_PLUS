package com.onplus.vpn

import android.net.VpnService
import android.os.ParcelFileDescriptor

class VpnTun : VpnService() {

    private var vpnInterface: ParcelFileDescriptor? = null

    override fun onStartCommand(intent: android.content.Intent?, flags: Int, startId: Int): Int {
        val builder = Builder()
            .addAddress("10.0.0.2", 24)
            .addRoute("0.0.0.0", 0)
            .setSession("ON PLUS VPN")

        vpnInterface = builder.establish()
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        vpnInterface?.close()
    }
}
