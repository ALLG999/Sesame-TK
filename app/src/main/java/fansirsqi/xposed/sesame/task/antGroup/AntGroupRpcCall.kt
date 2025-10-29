package fansirsqi.xposed.sesame.task.antGroup

import fansirsqi.xposed.sesame.hook.RequestManager
import fansirsqi.xposed.sesame.util.Log
import org.json.JSONArray
import org.json.JSONObject

/**
 * 芝麻树纯血RPC调用
 */
object AntGroupRpcCall {
    
    private const val TAG = "AntGroupRpcCall"
    
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
            Log.printStackTrace(TAG, e)
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
            Log.printStackTrace(TAG, e)
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
            Log.printStackTrace(TAG, e)
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
            Log.printStackTrace(TAG, e)
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
            Log.printStackTrace(TAG, e)
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
            Log.printStackTrace(TAG, e)
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
            Log.printStackTrace(TAG, e)
            return ""
        }
    }
}
