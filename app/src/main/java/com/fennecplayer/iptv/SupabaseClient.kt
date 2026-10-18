
package com.fennecplayer.iptv

import java.net.HttpURLConnection
import java.net.URL

object SupabaseClient {

    fun testConnection(
        onResult: (Boolean, String) -> Unit
    ) {
        Thread {
            var connection: HttpURLConnection? = null

            try {
                val url = URL(
                    "${SupabaseConfig.PROJECT_URL}/rest/v1/"
                )

                connection =
                    url.openConnection() as HttpURLConnection

                connection.requestMethod = "GET"
                connection.connectTimeout = 10000
                connection.readTimeout = 10000

                connection.setRequestProperty(
                    "apikey",
                    SupabaseConfig.PUBLISHABLE_KEY
                )

                val code = connection.responseCode

                val result = if (code in 200..299) {
                    Pair(true, "تم الاتصال بنجاح")
                } else {
                    Pair(false, "استجابة الخادم: $code")
                }

                onResult(result.first, result.second)

            } catch (e: Exception) {
                onResult(
                    false,
                    e.message ?: "تعذر الاتصال"
                )
            } finally {
                connection?.disconnect()
            }
        }.start()
    }
}
