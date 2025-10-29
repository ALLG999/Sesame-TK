package fansirsqi.xposed.sesame.util

import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

object HttpUtil {

    /**
     * 发起 POST 请求
     */
    fun post(apiUrl: String, params: String): String {
        var connection: HttpURLConnection? = null
        try {
            val url = URL(apiUrl) // 使用传入的 apiUrl 参数
            connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/json")
            connection.outputStream.write(params.toByteArray()) // 发送 JSON 参数

            // 获取响应
            val responseCode = connection.responseCode
            if (responseCode != HttpURLConnection.HTTP_OK) {
                throw Exception("HTTP request failed with response code $responseCode")
            }

            val response = BufferedReader(InputStreamReader(connection.inputStream)).use {
                it.readText()
            }
            return response
        } catch (e: Exception) {
            throw Exception("POST request failed", e)
        } finally {
            connection?.disconnect()
        }
    }
}
