# Xray Tunnel

عميل VPN لأندرويد (Kotlin + Jetpack Compose) مبني على نواة **Xray** عبر `libv2ray.aar`.

**البروتوكولات:** VMess · VLESS (TLS / REALITY / XTLS-Vision) · Trojan · Shadowsocks · SOCKS · Hysteria2 · WireGuard
**النقل:** TCP · WS · gRPC · HTTP/2 · mKCP · HTTPUpgrade · XHTTP

## زر Update (تحديث السيرفرات من خارج التطبيق)
1. ضع ملف `servers.txt` في مستودعك (روابط `vless://` `vmess://` ... سطر لكل رابط، أو نفس المحتوى Base64).
2. عدّل الرابط في `app/src/main/java/com/xraytunnel/app/data/Defaults.kt`:
   `https://raw.githubusercontent.com/USER/REPO/main/servers.txt`
3. من التطبيق: زر **Update servers**، ويمكن إضافة اشتراكات أخرى من القائمة ⋮ ← Subscriptions.

## البناء
- **GitHub Actions:** ارفع المشروع، يعمل `.github/workflows/build.yml` تلقائياً (يحمّل `libv2ray.aar` ويبني APK ثم يرفعه Artifact).
- **Android Studio:** ضع `libv2ray.aar` في `app/libs/` (من https://github.com/2dust/AndroidLibXrayLite/releases) ثم Build.

> واجهة النواة معزولة في ملف واحد: `core/XrayCore.kt`.
