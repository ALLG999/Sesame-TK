package fansirsqi.xposed.sesame.task.antGroup

import fansirsqi.xposed.sesame.util.Log
import org.json.JSONObject

/**
 * 响应检查器工具类
 */
object ResChecker {
    
    /**
     * 检查响应是否成功 - 基于抓包数据的响应格式
     */
    fun checkRes(tag: String, jsonResponse: JSONObject): Boolean {
        return try {
            when {
                jsonResponse.has("success") -> jsonResponse.getBoolean("success")
                jsonResponse.has("resultCode") -> jsonResponse.getInt("resultCode") == 1000 // 根据抓包数据
                jsonResponse.has("errorCode") -> jsonResponse.getString("errorCode") == "SUCCESS"
                jsonResponse.has("code") -> jsonResponse.getString("code") == "SUCCESS"
                jsonResponse.optBoolean("canRetry", true) -> false // 需要重试通常意味着失败
                else -> false
            }
        } catch (e: Exception) {
            Log.printStackTrace(tag, "响应检查异常", e)
            false
        }
    }
    
    /**
     * 获取错误信息 - 基于抓包数据的错误字段
     */
    fun getErrorMsg(jsonResponse: JSONObject): String {
        return when {
            jsonResponse.has("resultDesc") -> jsonResponse.getString("resultDesc")
            jsonResponse.has("errorMessage") -> jsonResponse.getString("errorMessage")
            jsonResponse.has("message") -> jsonResponse.getString("message")
            jsonResponse.has("msg") -> jsonResponse.getString("msg")
            jsonResponse.has("memo") -> jsonResponse.getString("memo")
            else -> "未知错误"
        }
    }

    /**
     * 检查是否有有效数据 - 基于抓包数据的结构
     */
    fun hasValidData(jsonResponse: JSONObject): Boolean {
        return jsonResponse.has("Data") || 
               jsonResponse.has("resData") || 
               jsonResponse.has("resultObject") ||
               jsonResponse.has("zhimaTreeHomePageQueryResult") ||
               jsonResponse.has("zhimaTreeAccountEnergyQueryResult") ||
               jsonResponse.has("extInfo")
    }
}
