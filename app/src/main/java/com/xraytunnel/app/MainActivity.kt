package com.xraytunnel.app

import android.Manifest
import android.app.Activity
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xraytunnel.app.data.Server
import com.xraytunnel.app.ui.MainViewModel
import com.xraytunnel.app.vpn.VpnState
import com.xraytunnel.app.vpn.XrayVpnService

class MainActivity : ComponentActivity() {
    private val vm: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val scheme = if (isSystemInDarkTheme())
                darkColorScheme(primary = Color(0xFF38BDF8), background = Color(0xFF0F172A), surface = Color(0xFF1E293B))
            else lightColorScheme(primary = Color(0xFF0284C7))
            MaterialTheme(colorScheme = scheme) { MainScreen(vm) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(vm: MainViewModel) {
    val ctx = LocalContext.current
    val servers by vm.servers.collectAsStateWithLifecycle()
    val selectedId by vm.selectedId.collectAsStateWithLifecycle()
    val state by vm.vpnState.collectAsStateWithLifecycle()
    val updating by vm.updating.collectAsStateWithLifecycle()
    val message by vm.message.collectAsStateWithLifecycle()
    val error by XrayVpnService.lastError.collectAsStateWithLifecycle()
    val snack = remember { SnackbarHostState() }
    var menu by remember { mutableStateOf(false) }
    var showSubs by remember { mutableStateOf(false) }

    LaunchedEffect(message) { message?.let { snack.showSnackbar(it); vm.consumeMessage() } }
    LaunchedEffect(error) { error?.let { snack.showSnackbar(it) } }

    val notifPerm = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= 33) notifPerm.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    fun startVpn() = ContextCompat.startForegroundService(ctx, Intent(ctx, XrayVpnService::class.java))
    fun stopVpn() = ctx.startService(Intent(ctx, XrayVpnService::class.java).setAction(XrayVpnService.ACTION_STOP))

    val vpnPerm = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == Activity.RESULT_OK) startVpn()
    }
    fun toggle() {
        if (state != VpnState.Disconnected) { stopVpn(); return }
        if (servers.isEmpty()) return
        val intent = VpnService.prepare(ctx)
        if (intent != null) vpnPerm.launch(intent) else startVpn()
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snack) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                actions = {
                    IconButton(onClick = { menu = true }) { Icon(Icons.Default.MoreVert, null) }
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        DropdownMenuItem(text = { Text(stringResource(R.string.import_clipboard)) }, onClick = {
                            menu = false
                            val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            vm.importText(cm.primaryClip?.getItemAt(0)?.text?.toString().orEmpty())
                        })
                        DropdownMenuItem(text = { Text(stringResource(R.string.subscriptions)) },
                            onClick = { menu = false; showSubs = true })
                        DropdownMenuItem(text = { Text(stringResource(R.string.delete_all)) },
                            onClick = { menu = false; vm.clear() })
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = ::toggle,
                icon = { Icon(if (state == VpnState.Disconnected) Icons.Default.PlayArrow else Icons.Default.Close, null) },
                text = { Text(stringResource(if (state == VpnState.Disconnected) R.string.connect else R.string.disconnect)) }
            )
        }
    ) { pad ->
        Column(Modifier.padding(pad).padding(horizontal = 16.dp)) {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        stringResource(when (state) {
                            VpnState.Connected -> R.string.connected
                            VpnState.Connecting -> R.string.connecting
                            else -> R.string.disconnected
                        }),
                        style = MaterialTheme.typography.titleLarge
                    )
                    Spacer(Modifier.height(12.dp))
                    // زر تحديث السيرفرات من الرابط الخارجي
                    Button(onClick = vm::updateAll, enabled = !updating, modifier = Modifier.fillMaxWidth()) {
                        if (updating) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                        else Icon(Icons.Default.Refresh, null)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.update_servers))
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            if (servers.isEmpty()) {
                Text(stringResource(R.string.no_servers), style = MaterialTheme.typography.bodyMedium)
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(bottom = 88.dp)) {
                    items(servers, key = { it.id }) { s ->
                        ServerRow(s, s.id == selectedId, { vm.select(s.id) }, { vm.delete(s.id) })
                    }
                }
            }
        }
    }

    if (showSubs) SubsDialog(vm) { showSubs = false }
}

@Composable
private fun ServerRow(s: Server, selected: Boolean, onClick: () -> Unit, onDelete: () -> Unit) {
    Card(
        Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(Modifier.padding(start = 12.dp, top = 4.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            RadioButton(selected = selected, onClick = onClick)
            Column(Modifier.weight(1f)) {
                Text(s.name, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyLarge)
                Text("${s.protocol.uppercase()} • ${s.network} • ${s.security} • ${s.address}:${s.port}",
                    maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodySmall)
            }
            IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, null) }
        }
    }
}

@Composable
private fun SubsDialog(vm: MainViewModel, onClose: () -> Unit) {
    val subs by vm.subs.collectAsStateWithLifecycle()
    var name by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onClose,
        confirmButton = { TextButton(onClick = onClose) { Text(stringResource(R.string.close)) } },
        title = { Text(stringResource(R.string.subscriptions)) },
        text = {
            Column {
                if (subs.isEmpty()) Text(stringResource(R.string.no_subs))
                subs.forEach { sub ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(sub.name, style = MaterialTheme.typography.bodyLarge)
                            Text(sub.url, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodySmall)
                        }
                        IconButton(onClick = { vm.removeSub(sub.id) }) { Icon(Icons.Default.Delete, null) }
                    }
                }
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(name, { name = it }, label = { Text(stringResource(R.string.name)) }, singleLine = true)
                OutlinedTextField(url, { url = it }, label = { Text(stringResource(R.string.url)) }, singleLine = true)
                Button(onClick = { vm.addSub(name, url); name = ""; url = "" }, modifier = Modifier.padding(top = 8.dp)) {
                    Icon(Icons.Default.Add, null); Spacer(Modifier.width(6.dp)); Text(stringResource(R.string.add))
                }
            }
        }
    )
}
