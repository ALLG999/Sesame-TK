package fansirsqi.xposed.sesame.task.antGroup

import fansirsqi.xposed.sesame.entity.AlipayVersion
import fansirsqi.xposed.sesame.entity.RpcEntity
import fansirsqi.xposed.sesame.hook.ApplicationHook
import fansirsqi.xposed.sesame.hook.RequestManager
import fansirsqi.xposed.sesame.util.Log
import fansirsqi.xposed.sesame.util.RandomUtil
import org.json.JSONArray
import org.json.JSONObject
import java.util.*

/**
 * 芝麻树纯血RPC调用
 */
object AntGroupRpcCall {
    
    private const val TAG = "AntGroupRpcCall"
    private var VERSION = "20250813"
    
    /**
     * 初始化版本信息
     */
    fun init() {
        val alipayVersion = ApplicationHook.getAlipayVersion()
        Log.record(TAG, "当前支付宝版本: ${alipayVersion.versionString}")
        try {
            when (alipayVersion.versionString) {
                "10.7.30.8000" -> VERSION = "20250813"  // 2025年版本
                "10.5.88.8000" -> VERSION = "20240403"  // 2024年版本
                "10.3.96.8100" -> VERSION = "20230501"  // 2023年版本
                else -> VERSION = "20250813"
            }
            Log.record(TAG, "芝麻树使用API版本: $VERSION")
        } catch (e: Exception) {
            Log.error(TAG, "版本初始化异常，使用默认版本: $VERSION", e)
        }
    }
    
    /**
     * 查询芝麻树首页
     */
    fun queryHomePage(playInfo: String): String {
        try {
            val requestData = JSONObject().apply {
                put("extInfo", JSONObject())
                put("operation", "ZHIMA_TREE_HOME_PAGE")
                put("playInfo", playInfo)
                put("refer", "https://render.alipay.com/p/yuyan/180020010001269849/zmTree.html?caprMode=sync&chInfo=chInfo=ch_zmzltf__chsub_xinyongsyyingxiaowei")
            }
            return RequestManager.requestString(
                "alipay.promoprod.play.trigger",
                JSONArray().put(requestData).toString()
            )
        } catch (e: Exception) {
            Log.error(TAG, "查询芝麻树首页异常", e)
            return ""
        }
    }
    
    /**
     * 查询森林能量
     */
    fun queryForestEnergy(playInfo: String): String {
        try {
            val requestData = JSONObject().apply {
                put("operation", "ZHIMA_TREE_FOREST_ENERGY_QUERY")
                put("playInfo", playInfo)
            }
            return RequestManager.requestString(
                "alipay.promoprod.play.trigger",
                JSONArray().put(requestData).toString()
            )
        } catch (e: Exception) {
            Log.error(TAG, "查询森林能量异常", e)
            return ""
        }
    }
    
    /**
     * 完成任务
     */
    fun finishTask(taskId: String, stageCode: String = "send"): String {
        try {
            val requestData = JSONObject().apply {
                put("extInfo", JSONObject().apply {
                    put("stageCode", stageCode)
                    put("taskId", taskId)
                })
                put("operation", "RENT_GREEN_TASK_FINISH")
                put("playInfo", "SwbtxJSo8OOUrymAU%2FHnY2jyFRc%2BkCJ3")
            }
            return RequestManager.requestString(
                "alipay.promoprod.play.trigger",
                JSONArray().put(requestData).toString()
            )
        } catch (e: Exception) {
            Log.error(TAG, "完成任务异常", e)
            return ""
        }
    }
    
    /**
     * 领取任务奖励
     */
    fun receiveTaskReward(taskOrderId: String, taskId: String, prizeId: String): String {
        try {
            val requestData = JSONObject().apply {
                put("extInfo", JSONObject().apply {
                    put("taskOrderId", taskOrderId)
                    put("taskId", taskId)
                    put("prizeId", prizeId)
                })
                put("operation", "RECEIVE_TASK_REWARD")
                put("playInfo", "SwbtxJSo8OOUrymAU%2FHnY2jyFRc%2BkCJ3")
            }
            return RequestManager.requestString(
                "alipay.promoprod.play.trigger",
                JSONArray().put(requestData).toString()
            )
        } catch (e: Exception) {
            Log.error(TAG, "领取任务奖励异常", e)
            return ""
        }
    }
    
    /**
     * 清理垃圾（净化）
     */
    fun cleanAndPush(trashCode: String, trashCampId: String, clickNum: Int = 1): String {
        try {
            val requestData = JSONObject().apply {
                put("extInfo", JSONObject().apply {
                    put("clickNum", clickNum.toString())
                    put("trashCampId", trashCampId)
                    put("trashCode", trashCode)
                    put("treeCode", "ZHIMA_TREE")
                })
                put("operation", "ZHIMA_TREE_CLEAN_AND_PUSH")
                put("playInfo", "SwbtxJSo8OOUrymAU%2FHnY2jyFRc%2BkCJ3")
                put("refer", "https://render.alipay.com/p/yuyan/180020010001269849/zmTree.html?caprMode=sync&chInfo=chInfo=ch_zmzltf__chsub_xinyongsyyingxiaowei")
            }
            return RequestManager.requestString(
                "alipay.promoprod.play.trigger",
                JSONArray().put(requestData).toString()
            )
        } catch (e: Exception) {
            Log.error(TAG, "清理垃圾异常", e)
            return ""
        }
    }
    
    /**
     * 查询页面布局
     */
    fun queryPageLayout(frontPageId: String = "HP_1755094925"): String {
        try {
            val requestData = JSONObject().apply {
                put("aseChannelId", "RENT")
                put("extInfo", JSONObject().apply {
                    put("aseGlobalConfigPageId", "rent_config_page")
                    put("error", 11)
                    put("venuePageId", frontPageId)
                })
                put("frontPageId", frontPageId)
            }
            return RequestManager.requestString(
                "com.alipay.creditapollon.venue.page.layout.query",
                JSONArray().put(requestData).toString()
            )
        } catch (e: Exception) {
            Log.error(TAG, "查询页面布局异常", e)
            return ""
        }
    }
    
    /**
     * 批量查询内容
     */
    fun batchQueryContent(positionCode: String, pageIndex: Int = 0): String {
        try {
            val batchQuery = JSONObject().apply {
                put("aseChannelId", "RENT")
                put("aseContentRequestList", JSONArray().apply {
                    put(JSONObject().apply {
                        put("extInfo", JSONObject().apply {
                            put("chInfo", "chInfo=ch_zmzltf__chsub_xinyongsyyingxiaowei")
                            put("exposedItemList", "")
                            put("pageIndex", pageIndex)
                        })
                        put("positionCode", positionCode)
                    })
                })
                put("extInfo", JSONObject().apply {
                    put("venuePageId", "HP_1755094925")
                })
                put("frontPageId", "HP_1755094925")
            }
            
            val requestData = JSONObject().apply {
                put("batchQuery", batchQuery)
            }
            
            return RequestManager.requestString(
                "com.alipay.creditapollon.delivery.content.batchQuery",
                JSONArray().put(requestData).toString()
            )
        } catch (e: Exception) {
            Log.error(TAG, "批量查询内容异常", e)
            return ""
        }
    }
    
    /**
     * 查询任务列表
     */
    fun queryTaskList(): String {
        try {
            val requestData = JSONObject().apply {
                put("extend", JSONObject())
                put("fromAct", "home_task_list")
                put("source", "chInfo_ch_appcenter__chsub_9patch")
                put("version", VERSION)
            }
            return RequestManager.requestString(
                "alipay.antforest.forest.h5.queryTaskList",
                JSONArray().put(requestData).toString()
            )
        } catch (e: Exception) {
            Log.error(TAG, "查询任务列表异常", e)
            return ""
        }
    }
    
    /**
     * 完成任务（通用方法）
     */
    fun finishTask(sceneCode: String, taskType: String): String {
        try {
            val outBizNo = "${taskType}_${RandomUtil.nextDouble()}"
            val requestData = JSONObject().apply {
                put("outBizNo", outBizNo)
                put("requestType", "H5")
                put("sceneCode", sceneCode)
                put("source", "ANTFOREST")
                put("taskType", taskType)
            }
            return RequestManager.requestString(
                "com.alipay.antiep.finishTask",
                JSONArray().put(requestData).toString()
            )
        } catch (e: Exception) {
            Log.error(TAG, "完成任务异常", e)
            return ""
        }
    }
    
    /**
     * 领取任务奖励（通用方法）
     */
    fun receiveTaskAward(sceneCode: String, taskType: String): String {
        try {
            val requestData = JSONObject().apply {
                put("ignoreLimit", false)
                put("requestType", "H5")
                put("sceneCode", sceneCode)
                put("source", "ANTFOREST")
                put("taskType", taskType)
            }
            return RequestManager.requestString(
                "com.alipay.antiep.receiveTaskAward",
                JSONArray().put(requestData).toString()
            )
        } catch (e: Exception) {
            Log.error(TAG, "领取任务奖励异常", e)
            return ""
        }
    }
    
    /**
     * 查询道具列表
     */
    fun queryPropList(onlyGive: Boolean = false): String {
        try {
            val requestData = JSONObject().apply {
                put("onlyGive", if (onlyGive) "Y" else "")
                put("source", "chInfo_ch_appcenter__chsub_9patch")
                put("version", VERSION)
            }
            return RequestManager.requestString(
                "alipay.antforest.forest.h5.queryPropList",
                JSONArray().put(requestData).toString()
            )
        } catch (e: Exception) {
            Log.error(TAG, "查询道具列表异常", e)
            return ""
        }
    }
    
    /**
     * 使用道具
     */
    fun consumeProp(propId: String, propType: String, secondConfirm: Boolean? = null): String {
        try {
            val requestData = JSONObject().apply {
                put("propId", propId)
                put("propType", propType)
                put("sToken", "${System.currentTimeMillis()}_${RandomUtil.getRandomString(8)}")
                secondConfirm?.let { put("secondConfirm", it) }
                put("source", "chInfo_ch_appcenter__chsub_9patch")
                put("timezoneId", "Asia/Shanghai")
                put("version", VERSION)
            }
            return RequestManager.requestString(
                "alipay.antforest.forest.h5.consumeProp",
                JSONArray().put(requestData).toString()
            )
        } catch (e: Exception) {
            Log.error(TAG, "使用道具异常", e)
            return ""
        }
    }
    
    /**
     * 查询用户信息
     */
    fun queryUserInfo(): String {
        try {
            val requestData = JSONObject().apply {
                put("source", "chInfo_ch_appcenter__chsub_9patch")
                put("version", VERSION)
            }
            return RequestManager.requestString(
                "alipay.antmember.forest.h5.queryUserInfo",
                JSONArray().put(requestData).toString()
            )
        } catch (e: Exception) {
            Log.error(TAG, "查询用户信息异常", e)
            return ""
        }
    }
    
    /**
     * 测试RPC调用
     */
    fun testH5Rpc(operationType: String, requestData: String): String {
        return RequestManager.requestString(operationType, requestData)
    }
    
    /**
     * 创建RPC实体
     */
    private fun createRpcEntity(method: String, param: String, relationLocal: String? = null): RpcEntity {
        return if (relationLocal != null) {
            RpcEntity(method, param, relationLocal)
        } else {
            RpcEntity(method, param)
        }
    }
}
