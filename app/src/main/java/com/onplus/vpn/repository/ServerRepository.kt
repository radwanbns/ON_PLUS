package com.onplus.vpn.repository

import com.onplus.vpn.model.ServerItem
import com.onplus.vpn.network.ServerApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class ServerRepository {

    private val apiService: ServerApiService by lazy {
        Retrofit.Builder()
            .baseUrl("https://dummy.base.url/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ServerApiService::class.java)
    }

    suspend fun getUpdatedServers(url: String): Result<List<ServerItem>> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.fetchServers(url)
                Result.success(response.servers)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
}
