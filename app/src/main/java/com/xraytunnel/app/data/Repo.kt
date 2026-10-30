package com.xraytunnel.app.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

class Repo private constructor(ctx: Context) {
    private val sp = ctx.applicationContext.getSharedPreferences("xray_tunnel", Context.MODE_PRIVATE)

    private val _servers = MutableStateFlow(readServers())
    val servers: StateFlow<List<Server>> = _servers.asStateFlow()

    private val _subs = MutableStateFlow(readSubs())
    val subs: StateFlow<List<Subscription>> = _subs.asStateFlow()

    private val _selected = MutableStateFlow(sp.getString("selected", null))
    val selectedId: StateFlow<String?> = _selected.asStateFlow()

    fun selectedServer(): Server? = _servers.value.firstOrNull { it.id == _selected.value }

    fun select(id: String) { _selected.value = id; sp.edit().putString("selected", id).apply() }

    // ---------- servers ----------
    fun addServers(list: List<Server>) = saveServers(_servers.value + list)

    fun replaceSub(subId: String, list: List<Server>) =
        saveServers(_servers.value.filter { it.subId != subId } + list.map { it.copy(subId = subId) })

    fun delete(id: String) = saveServers(_servers.value.filter { it.id != id })
    fun clear() = saveServers(emptyList())

    private fun saveServers(list: List<Server>) {
        _servers.value = list
        sp.edit().putString("servers", JSONArray().apply { list.forEach { put(it.toJson()) } }.toString()).apply()
        if (list.none { it.id == _selected.value }) {
            list.firstOrNull()?.let { select(it.id) } ?: run { _selected.value = null; sp.edit().remove("selected").apply() }
        }
    }

    private fun readServers(): List<Server> = try {
        val a = JSONArray(sp.getString("servers", "[]"))
        (0 until a.length()).map { Server.fromJson(a.getJSONObject(it)) }
    } catch (e: Exception) { emptyList() }

    // ---------- subscriptions ----------
    fun addSub(name: String, url: String) = saveSubs(_subs.value + Subscription(name = name, url = url))
    fun removeSub(id: String) {
        saveSubs(_subs.value.filter { it.id != id })
        saveServers(_servers.value.filter { it.subId != id })
    }

    private fun saveSubs(list: List<Subscription>) {
        _subs.value = list
        sp.edit().putString("subs", JSONArray().apply {
            list.forEach { put(JSONObject().put("id", it.id).put("name", it.name).put("url", it.url)) }
        }.toString()).apply()
    }

    private fun readSubs(): List<Subscription> {
        if (!sp.contains("subs")) {
            return if (Defaults.SUBSCRIPTION_URL.isNotBlank())
                listOf(Subscription(name = Defaults.SUBSCRIPTION_NAME, url = Defaults.SUBSCRIPTION_URL))
            else emptyList()
        }
        return try {
            val a = JSONArray(sp.getString("subs", "[]"))
            (0 until a.length()).map {
                val o = a.getJSONObject(it)
                Subscription(o.getString("id"), o.getString("name"), o.getString("url"))
            }
        } catch (e: Exception) { emptyList() }
    }

    companion object {
        @Volatile private var inst: Repo? = null
        fun get(ctx: Context): Repo = inst ?: synchronized(this) { inst ?: Repo(ctx).also { inst = it } }
    }
}
