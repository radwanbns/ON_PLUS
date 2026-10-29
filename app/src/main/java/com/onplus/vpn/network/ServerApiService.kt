package com.onplus.vpn.network

import com.onplus.vpn.model.ServerResponse
import retrofit2.http.GET
import retrofit2.http.Url

interface ServerApiService {
    @GET
    suspend fun fetchServers(@Url updateUrl: String): ServerResponse
}
