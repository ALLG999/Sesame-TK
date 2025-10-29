package fansirsqi.xposed.sesame.util

import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

object HttpUtil {

    /**
     * 发起 POST 请求
     */
    fun post(apiName: String, params: String): String {
        val url = "https://yourapiurl.com/api/$apiName" // 替换为实际 URL
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.requestMethod = "POST"
        connection.doOutput = true
        connection.setRequestProperty("Content-Type", "application/json")
        connection.outputStream.write(params.toByteArray())

        val response = BufferedReader(InputStreamReader(connection.inputStream)).use {
            it.readText()
        }

        return response
    }
}
