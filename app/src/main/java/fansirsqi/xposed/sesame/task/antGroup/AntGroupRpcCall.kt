package fansirsqi.xposed.sesame.rpc

import fansirsqi.xposed.sesame.util.HttpUtil
import fansirsqi.xposed.sesame.util.CoroutineUtils
import fansirsqi.xposed.sesame.util.Log
import org.json.JSONObject

/**
 * 芝麻树（Apollon版）RPC 调用封装
 * 所有方法均返回原始 JSON 字符串，由上层模块自行解析。
 */
object AntGroupRpcCall {

    private const val ASE_CHANNEL_ID = "RENT" // 常用的渠道ID
    private const val PAGE_LAYOUT_QUERY = "com.alipay.creditapollon.venue.page.layout.query"
    private const val ENERGY_QUERY = "com.alipay.creditapollon.venue.energy.query"
    private const val ENERGY_FINISH = "com.alipay.creditapollon.venue.energy.finish"
    private const val TASK_QUERY = "com.alipay.creditapollon.venue.task.query"
    private const val TASK_REPORT = "com.alipay.creditapollon.venue.task.report"

    /**
     * 构建公共的请求 payload
     */
    private fun buildPayload(vararg params: Pair<String, Any>): JSONObject {
        return JSONObject().apply {
            put("aseChannelId", ASE_CHANNEL_ID)
            params.forEach { (key, value) -> 
                put(key, value)
            }
        }
    }

    /**
     * 发送 RPC 请求并记录日志
     */
    private fun sendRequest(apiName: String, payload: JSONObject): String? {
        val apiUrl = "https://yourapiurl.com/api/$apiName" // 请替换为正确的 API URL

        var responseString: String? = null
        CoroutineUtils.runOnIO {
            try {
                Log.debug("Sending request to $apiName with payload: ${payload.toString()}")
                
                // 通过 HttpUtil 发送 POST 请求
                responseString = HttpUtil.post(apiUrl, payload.toString())
                
                Log.debug("Received response from $apiName: $responseString")
            } catch (e: Exception) {
                Log.printStackTrace("Error while sending request to $apiName", e)
            }
        }

        return responseString // 返回原始 JSON 字符串
    }

    /**
     * 获取首页布局信息（入口模块）
     */
    fun pageLayoutQuery(): String? {
        val payload = buildPayload("venuePageId" to "HOME_PAGE")
        return sendRequest(PAGE_LAYOUT_QUERY, payload)
    }

    /**
     * 查询能量（获取当前净化值节点、剩余可收取能量等）
     */
    fun energyQuery(): String? {
        val payload = buildPayload() // 无额外参数
        return sendRequest(ENERGY_QUERY, payload)
    }

    /**
     * 点击净化（完成一次“浇水”或“净化”操作）
     * @param contentId 能量节点 ID
     */
    fun energyFinish(contentId: String): String? {
        val payload = buildPayload("contentId" to contentId)
        return sendRequest(ENERGY_FINISH, payload)
    }

    /**
     * 查询任务列表（包含每日任务、浏览任务等）
     */
    fun taskQuery(): String? {
        val payload = buildPayload() // 无额外参数
        return sendRequest(TASK_QUERY, payload)
    }

    /**
     * 上报任务完成状态（如浏览15秒）
     * @param taskId 任务ID
     * @param taskType 任务类型（如 BROWSE_15S）
     * @param status 状态（默认 FINISH）
     */
    fun taskReport(taskId: String, taskType: String, status: String = "FINISH"): String? {
        val extInfo = JSONObject().apply {
            put("taskId", taskId)
            put("taskType", taskType)
            put("status", status)
        }

        val payload = buildPayload("extInfo" to extInfo)
        return sendRequest(TASK_REPORT, payload)
    }
}
