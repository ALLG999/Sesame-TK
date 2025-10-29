package fansirsqi.xposed.sesame.task.antGroup

import fansirsqi.xposed.sesame.entity.RpcEntity
import fansirsqi.xposed.sesame.hook.RequestManager
import fansirsqi.xposed.sesame.util.CoroutineUtils
import fansirsqi.xposed.sesame.util.Log
import org.json.JSONObject

/**
 * 芝麻树RPC调用类 - 基于抓包数据精确实现
 */
object AntGroupRpcCall {
    private const val TAG = "AntGroupRpcCall"
    
    // 固定参数 - 从抓包数据中提取
    private const val FIXED_PLAY_INFO = "SwbtxJSo8OOUrymAU%2FHnY2jyFRc%2BkCJ3"
    private const val DEFAULT_REFER = "https://render.alipay.com/p/yuyan/180020010001269849/zmTree.html?caprMode=sync&chInfo=chInfo=ch_zmzltf__chsub_xinyongsyyingxiaowei"
    private const val OPERATION_TYPE = "alipay.promoprod.play.trigger"
    private const val APP_NAME = "promoprod"
    private const val FACADE_NAME = "PlayTriggerRpcService"
    private const val METHOD_NAME = "trigger"

    /**
     * 创建RPC实体
     */
    private fun createRpcEntity(requestData: Any, requestMethod: String = OPERATION_TYPE): RpcEntity {
        return RpcEntity(
            requestMethod = requestMethod,
            requestData = if (requestData is String) requestData else requestData.toString(),
            appName = APP_NAME,
            methodName = METHOD_NAME,
            facadeName = FACADE_NAME
        )
    }

    /**
     * 创建请求数据JSON - 精确匹配抓包数据结构
     */
    private fun createRequestData(operation: String, extInfo: JSONObject? = null, playInfo: String = FIXED_PLAY_INFO, refer: String? = DEFAULT_REFER): String {
        return JSONObject().apply {
            put("operation", operation)
            put("playInfo", playInfo)
            
            extInfo?.let { put("extInfo", it) }
            refer?.let { put("refer", it) }
        }.toString()
    }

    /**
     * 查询森林能量 - ZHIMA_TREE_FOREST_ENERGY_QUERY
     */
    fun queryForestEnergy(playInfo: String = FIXED_PLAY_INFO): String {
        return executeRpcCall("查询能量") {
            val requestData = createRequestData(
                operation = "ZHIMA_TREE_FOREST_ENERGY_QUERY",
                playInfo = playInfo
            )
            createRpcEntity(requestData)
        }
    }

    /**
     * 查询首页 - ZHIMA_TREE_HOME_PAGE
     */
    fun queryHomePage(playInfo: String = FIXED_PLAY_INFO): String {
        return executeRpcCall("查询首页") {
            val requestData = createRequestData(
                operation = "ZHIMA_TREE_HOME_PAGE",
                extInfo = JSONObject(), // 根据抓包，extInfo为空对象
                playInfo = playInfo,
                refer = DEFAULT_REFER
            )
            createRpcEntity(requestData)
        }
    }

    /**
     * 完成任务 - RENT_GREEN_TASK_FINISH
     */
    fun finishTask(taskId: String, playInfo: String = FIXED_PLAY_INFO): String {
        return executeRpcCall("完成任务") {
            val extInfo = JSONObject().apply {
                put("stageCode", "send") // 根据抓包数据
                put("taskId", taskId)
            }
            val requestData = createRequestData(
                operation = "RENT_GREEN_TASK_FINISH",
                extInfo = extInfo,
                playInfo = playInfo
            )
            createRpcEntity(requestData)
        }
    }

    /**
     * 领取任务奖励 - RENT_GREEN_TASK_RECEIVE
     */
    fun receiveTaskReward(taskOrderId: String, taskId: String, prizeId: String, playInfo: String = FIXED_PLAY_INFO): String {
        return executeRpcCall("领取奖励") {
            val extInfo = JSONObject().apply {
                put("taskOrderId", taskOrderId)
                put("taskId", taskId)
                put("prizeId", prizeId)
            }
            val requestData = createRequestData(
                operation = "RENT_GREEN_TASK_RECEIVE",
                extInfo = extInfo,
                playInfo = playInfo
            )
            createRpcEntity(requestData)
        }
    }

    /**
     * 查询任务列表 - RENT_GREEN_TASK_LIST_QUERY
     */
    fun queryTaskList(playInfo: String = FIXED_PLAY_INFO, chInfo: String = "ch_zmzltf__chsub_xinyongsyyingxiaowei"): String {
        return executeRpcCall("查询任务列表") {
            val extInfo = JSONObject().apply {
                put("batchId", "") // 根据抓包数据
                put("chInfo", chInfo)
            }
            val requestData = createRequestData(
                operation = "RENT_GREEN_TASK_LIST_QUERY",
                extInfo = extInfo,
                playInfo = playInfo,
                refer = DEFAULT_REFER
            )
            createRpcEntity(requestData)
        }
    }

    /**
     * 清理树木 - ZHIMA_TREE_CLEAN_AND_PUSH
     */
    fun cleanTree(playInfo: String = FIXED_PLAY_INFO, trashCode: String, trashCampId: String = "CP152834153"): String {
        return executeRpcCall("清理树木") {
            val extInfo = JSONObject().apply {
                put("clickNum", "1") // 根据抓包数据
                put("trashCampId", trashCampId)
                put("trashCode", trashCode)
                put("treeCode", "ZHIMA_TREE")
            }
            val requestData = createRequestData(
                operation = "ZHIMA_TREE_CLEAN_AND_PUSH",
                extInfo = extInfo,
                playInfo = playInfo,
                refer = DEFAULT_REFER
            )
            createRpcEntity(requestData)
        }
    }

    /**
     * 执行RPC调用（带重试和错误处理）
     */
    private fun executeRpcCall(operation: String, maxRetries: Int = 3, block: () -> RpcEntity): String {
        repeat(maxRetries) { attempt ->
            try {
                val rpcEntity = block()
                Log.record(TAG, "执行RPC调用: $operation (尝试 ${attempt + 1}/$maxRetries)")
                
                val result = RequestManager.requestString(rpcEntity)
                
                if (isValidResponse(result)) {
                    Log.record(TAG, "$operation 成功")
                    
                    // 检查响应内容是否有效
                    if (isResponseContentValid(result)) {
                        return result
                    } else {
                        Log.record(TAG, "$operation 返回内容无效")
                    }
                } else {
                    Log.record(TAG, "$operation 返回空响应")
                }
            } catch (e: Exception) {
                Log.printStackTrace(TAG, "$operation 第${attempt + 1}次尝试异常", e)
            }
            
            // 重试前等待（第一次重试等待2秒，后续等待时间递增）
            if (attempt < maxRetries - 1) {
                val waitTime = 2000L * (attempt + 1)
                Log.record(TAG, "等待 ${waitTime}ms 后重试...")
                CoroutineUtils.sleepCompat(waitTime)
            }
        }
        
        Log.runtime(TAG, "$operation 所有重试均失败")
        return "{}"
    }

    /**
     * 检查响应是否有效（基础检查）
     */
    private fun isValidResponse(response: String): Boolean {
        return response.isNotEmpty() && response != "{}" && response != "null"
    }

    /**
     * 检查响应内容是否有效（业务逻辑检查）
     */
    private fun isResponseContentValid(response: String): Boolean {
        return try {
            val json = JSONObject(response)
            // 根据抓包数据，成功响应通常有 success: true 或 resultCode: 1000
            when {
                json.has("success") -> json.getBoolean("success")
                json.has("resultCode") -> json.getInt("resultCode") == 1000
                json.has("errorCode") -> json.getString("errorCode") == "SUCCESS"
                else -> true // 如果无法判断，认为内容有效
            }
        } catch (e: Exception) {
            Log.printStackTrace(TAG, "检查响应内容异常", e)
            true // 如果解析失败，认为内容有效
        }
    }
}
