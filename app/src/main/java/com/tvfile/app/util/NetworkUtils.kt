package com.tvfile.app.util

import java.net.Inet4Address
import java.net.NetworkInterface
import java.util.Collections

object NetworkUtils {

    fun getLocalIpAddress(): String {
        try {
            val interfaces = Collections.list(NetworkInterface.getNetworkInterfaces())
            // 优先考虑 wlan0 (WiFi) 和 eth0 (有线电视网口)
            val sorted = interfaces.sortedByDescending { 
                when {
                    it.name.startsWith("eth") -> 3
                    it.name.startsWith("wlan") -> 2
                    else -> 1
                }
            }

            for (intf in sorted) {
                if (!intf.isUp || intf.isLoopback) continue
                val addresses = Collections.list(intf.inetAddresses)
                for (addr in addresses) {
                    if (!addr.isLoopbackAddress && addr is Inet4Address) {
                        val host = addr.hostAddress
                        if (host != null && !host.startsWith("127.")) {
                            return host
                        }
                    }
                }
            }
        } catch (_: Exception) {}
        return "127.0.0.1"
    }
}
