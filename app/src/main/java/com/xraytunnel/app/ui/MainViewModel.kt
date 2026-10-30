package com.xraytunnel.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.xraytunnel.app.R
import com.xraytunnel.app.data.LinkParser
import com.xraytunnel.app.data.Repo
import com.xraytunnel.app.data.SubscriptionUpdater
import com.xraytunnel.app.vpn.XrayVpnService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = Repo.get(app)
    val servers = repo.servers
    val subs = repo.subs
    val selectedId = repo.selectedId
    val vpnState = XrayVpnService.state

    private val _updating = MutableStateFlow(false)
    val updating: StateFlow<Boolean> = _updating.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()
    fun consumeMessage() { _message.value = null }

    private fun str(id: Int, vararg a: Any) = getApplication<Application>().getString(id, *a)

    /** زر Update: يجلب كل الاشتراكات من الروابط الخارجية ويستبدل سيرفراتها. */
    fun updateAll() {
        if (_updating.value) return
        viewModelScope.launch {
            _updating.value = true
            var total = 0
            var error: String? = null
            for (sub in repo.subs.value) {
                try {
                    val list = SubscriptionUpdater.fetch(sub.url)
                    if (list.isNotEmpty()) { repo.replaceSub(sub.id, list); total += list.size }
                } catch (e: Exception) { error = e.message ?: e.javaClass.simpleName }
            }
            _message.value = if (error != null && total == 0) str(R.string.update_failed, error)
                             else str(R.string.updated_fmt, total)
            _updating.value = false
        }
    }

    fun importText(text: String) {
        val found = SubscriptionUpdater.parseContent(text)
        if (found.isEmpty()) _message.value = str(R.string.nothing_found)
        else { repo.addServers(found); _message.value = str(R.string.imported_fmt, found.size) }
    }

    fun select(id: String) = repo.select(id)
    fun delete(id: String) = repo.delete(id)
    fun clear() = repo.clear()
    fun addSub(name: String, url: String) { if (url.isNotBlank()) repo.addSub(name.ifBlank { url }, url.trim()) }
    fun removeSub(id: String) = repo.removeSub(id)
}
