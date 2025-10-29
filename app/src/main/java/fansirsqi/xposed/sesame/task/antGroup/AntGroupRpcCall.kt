package fansirsqi.xposed.sesame.rpc

import fansirsqi.xposed.sesame.util.HttpUtil
import org.json.JSONObject

/**
 * 芝麻树（Apollon版）RPC 调用封装
 * 所有方法均返回原始 JSON 字符串，由上层模块自行解析。
 *
 * 已适配：
 * 1. com.alipay.creditapollon.venue.page.layout.query
 * 2. com.alipay.creditapollon.venue.energy.query
 * 3. com.alipay.creditapollon.venue.energy.finish
 * 4. com.alipay.creditapollon.venue.task.query
 * 5. com.alipay.creditapollon.venue.task.report
 *
 * 舍弃旧的 zmtask / promoprod 接口。
 */
object AntGroupRpcCall {

    private const val PAGE_LAYOUT_QUERY = "com.alipay.creditapollon.venue.page.layout.query"
    private const val ENERGY_QUERY = "com.alipay.creditapollon.venue.energy.query"
    private const val ENERGY_FINISH = "com.alipay.creditapollon.venue.energy.finish"
    private const val TASK_QUERY = "com.alipay.creditapollon.venue.task.query"
    private const val TASK_REPORT = "com.alipay.creditapollon.venue.task.report"

    /**
     * 获取首页布局信息（入口模块）
     */
    fun pageLayoutQuery(): String? {
        val payload = JSONObject().apply {
            put("aseChannelId", "RENT")
            put("venuePageId", "HOME_PAGE")
        }
        return HttpUtil.post(PAGE_LAYOUT_QUERY, payload.toString())
    }

    /**
     * 查询能量（获取当前净化值节点、剩余可收取能量等）
     */
    fun energyQuery(): String? {
        val payload = JSONObject().apply {
            put("aseChannelId", "RENT")
        }
        return HttpUtil.post(ENERGY_QUERY, payload.toString())
    }

    /**
     * 点击净化（完成一次“浇水”或“净化”操作）
     * @param contentId 能量节点 ID
     */
    fun energyFinish(contentId: String): String? {
        val payload = JSONObject().apply {
            put("aseChannelId", "RENT")
            put("contentId", contentId)
        }
        return HttpUtil.post(ENERGY_FINISH, payload.toString())
    }

    /**
     * 查询任务列表（包含每日任务、浏览任务等）
     */
    fun taskQuery(): String? {
        val payload = JSONObject().apply {
            put("aseChannelId", "RENT")
        }
        return HttpUtil.post(TASK_QUERY, payload.toString())
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

        val payload = JSONObject().apply {
            put("aseChannelId", "RENT")
            put("extInfo", extInfo)
        }

        return HttpUtil.post(TASK_REPORT, payload.toString())
    }
}
