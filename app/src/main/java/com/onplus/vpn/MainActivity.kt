package com.onplus.vpn

import android.content.Intent
import android.net.VpnService
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.onplus.vpn.databinding.ActivityMainBinding
import com.onplus.vpn.model.ServerItem
import com.onplus.vpn.repository.ServerRepository
import com.onplus.vpn.service.OnPlusVpnService
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val repository = ServerRepository()
    private var selectedServer: ServerItem? = null
    private var isConnected = false

    private val UPDATE_URL = "https://raw.githubusercontent.com/your-repo/vpn-servers/main/servers.json"

    private val vpnPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            startVpnService()
        } else {
            Toast.makeText(this, "Permission denied!", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.titleTextView.text = "ON PLUS VPN"

        binding.connectButton.setOnClickListener {
            if (!isConnected) {
                prepareAndConnect()
            } else {
                disconnect()
            }
        }

        binding.btnUpdateServers.setOnClickListener {
            fetchServersFromRemote()
        }

        fetchServersFromRemote()
    }

    private fun fetchServersFromRemote() {
        lifecycleScope.launch {
            binding.statusTextView.text = "Updating server list..."
            val result = repository.getUpdatedServers(UPDATE_URL)
            result.onSuccess { servers ->
                if (servers.isNotEmpty()) {
                    selectedServer = servers[0]
                    binding.statusTextView.text = "Servers updated: \${servers.size} available"
                    binding.selectedServerTextView.text = "Selected: \${selectedServer?.countryName} (\${selectedServer?.protocol})"
                }
            }.onFailure { error ->
                binding.statusTextView.text = "Update failed: \${error.localizedMessage}"
            }
        }
    }

    private fun prepareAndConnect() {
        if (selectedServer == null) {
            Toast.makeText(this, "Please update and select a server first", Toast.LENGTH_SHORT).show()
            return
        }

        val intent = VpnService.prepare(this)
        if (intent != null) {
            vpnPermissionLauncher.launch(intent)
        } else {
            startVpnService()
        }
    }

    private fun startVpnService() {
        val server = selectedServer ?: return
        val intent = Intent(this, OnPlusVpnService::class.java).apply {
            action = OnPlusVpnService.ACTION_CONNECT
            putExtra(OnPlusVpnService.EXTRA_PROTOCOL, server.protocol.name)
            putExtra(OnPlusVpnService.EXTRA_CONFIG, server.configData)
        }
        startService(intent)
        isConnected = true
        binding.connectButton.text = "DISCONNECT"
        binding.statusTextView.text = "Connected to \${server.countryName}"
    }

    private fun disconnect() {
        val intent = Intent(this, OnPlusVpnService::class.java).apply {
            action = OnPlusVpnService.ACTION_DISCONNECT
        }
        startService(intent)
        isConnected = false
        binding.connectButton.text = "CONNECT"
        binding.statusTextView.text = "Disconnected"
    }
}
