package com.onplus.vpn.model

import com.google.gson.annotations.SerializedName

enum class VpnProtocol {
    @SerializedName("openvpn") OPENVPN,
    @SerializedName("wireguard") WIREGUARD,
    @SerializedName("v2ray") V2RAY,
    @SerializedName("ikev2") IKEV2
}

data class ServerResponse(
    @SerializedName("version") val version: Int,
    @SerializedName("servers") val servers: List<ServerItem>
)

data class ServerItem(
    @SerializedName("id") val id: String,
    @SerializedName("country_name") val countryName: String,
    @SerializedName("country_code") val countryCode: String,
    @SerializedName("flag_url") val flagUrl: String,
    @SerializedName("protocol") val protocol: VpnProtocol,
    @SerializedName("config_data") val configData: String,
    @SerializedName("ip") val ip: String,
    @SerializedName("port") val port: Int,
    @SerializedName("is_premium") val isPremium: Boolean = false
)
