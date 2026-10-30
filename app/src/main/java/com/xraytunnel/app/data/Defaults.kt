package com.xraytunnel.app.data

object Defaults {
    /**
     * رابط ملف السيرفرات الذي يقرأه زر "Update" من خارج التطبيق.
     * ضع هنا رابط raw من GitHub (أو أي رابط اشتراك). الملف: روابط (vless://, vmess://...) سطراً لكل رابط،
     * أو نفس المحتوى مشفراً Base64.
     */
    const val SUBSCRIPTION_URL = "https://raw.githubusercontent.com/radwanbns/Redo-VPN/refs/heads/main/servers.txt"
    const val SUBSCRIPTION_NAME = "Default"
}
