package fansirsqi.xposed.sesame.task.antGroup

import fansirsqi.xposed.sesame.util.RpcCall
import fansirsqi.xposed.sesame.util.Log
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
        return RpcCall.call(
            method = "alipay.promoprod.play.trigger",
            operationType = "alipay.promoprod.play.trigger",
            requestData = listOf(mapOf(
                "extInfo" to mapOf<String, Any>(),
                "operation" to "ZHIMA_TREE_HOME_PAGE",
                "playInfo" to playInfo,
                "refer" to "https://render.alipay.com/p/yuyan/180020010001269849/zmTree.html?caprMode=sync&chInfo=chInfo=ch_zmzltf__chsub_xinyongsyyingxiaowei"
            ))
        )
    }
    
    /**
     * 查询森林能量
     */
    fun queryForestEnergy(playInfo: String): String {
        return RpcCall.call(
            method = "alipay.promoprod.play.trigger",
            operationType = "alipay.promoprod.play.trigger", 
            requestData = listOf(mapOf(
                "operation" to "ZHIMA_TREE_FOREST_ENERGY_QUERY",
                "playInfo" to playInfo
            ))
        )
    }
    
    /**
     * 完成任务
     */
    fun finishTask(taskId: String, stageCode: String = "send"): String {
        return RpcCall.call(
            method = "alipay.promoprod.play.trigger",
            operationType = "alipay.promoprod.play.trigger",
            requestData = listOf(mapOf(
                "extInfo" to mapOf(
                    "stageCode" to stageCode,
                    "taskId" to taskId
                ),
                "operation" to "RENT_GREEN_TASK_FINISH",
                "playInfo" to "SwbtxJSo8OOUrymAU%2FHnY2jyFRc%2BkCJ3"
            ))
        )
    }
    
    /**
     * 领取任务奖励
     */
    fun receiveTaskReward(taskOrderId: String, taskId: String, prizeId: String): String {
        return RpcCall.call(
            method = "alipay.promoprod.play.trigger", 
            operationType = "alipay.promoprod.play.trigger",
            requestData = listOf(mapOf(
                "extInfo" to mapOf(
                    "taskOrderId" to taskOrderId,
                    "taskId" to taskId,
                    "prizeId" to prizeId
                ),
                "operation" to "RECEIVE_TASK_REWARD",
                "playInfo" to "SwbtxJSo8OOUrymAU%2FHnY2jyFRc%2BkCJ3"
            ))
        )
    }
    
    /**
     * 清理垃圾（净化）
     */
    fun cleanAndPush(trashCode: String, trashCampId: String, clickNum: Int = 1): String {
        return RpcCall.call(
            method = "alipay.promoprod.play.trigger",
            operationType = "alipay.promoprod.play.trigger",
            requestData = listOf(mapOf(
                "extInfo" to mapOf(
                    "clickNum" to clickNum.toString(),
                    "trashCampId" to trashCampId,
                    "trashCode" to trashCode,
                    "treeCode" to "ZHIMA_TREE"
                ),
                "operation" to "ZHIMA_TREE_CLEAN_AND_PUSH",
                "playInfo" to "SwbtxJSo8OOUrymAU%2FHnY2jyFRc%2BkCJ3",
                "refer" to "https://render.alipay.com/p/yuyan/180020010001269849/zmTree.html?caprMode=sync&chInfo=chInfo=ch_zmzltf__chsub_xinyongsyyingxiaowei"
            ))
        )
    }
    
    /**
     * 查询页面布局
     */
    fun queryPageLayout(frontPageId: String = "HP_1755094925"): String {
        return RpcCall.call(
            method = "com.alipay.creditapollon.venue.page.layout.query",
            operationType = "com.alipay.creditapollon.venue.page.layout.query",
            requestData = listOf(mapOf(
                "aseChannelId" to "RENT",
                "extInfo" to mapOf(
                    "aseGlobalConfigPageId" to "rent_config_page",
                    "error" to 11,
                    "venuePageId" to frontPageId
                ),
                "frontPageId" to frontPageId
            ))
        )
    }
    
    /**
     * 批量查询内容
     */
    fun batchQueryContent(positionCode: String, pageIndex: Int = 0): String {
        return RpcCall.call(
            method = "com.alipay.creditapollon.delivery.content.batchQuery",
            operationType = "com.alipay.creditapollon.delivery.content.batchQuery",
            requestData = listOf(mapOf(
                "batchQuery" to mapOf(
                    "aseChannelId" to "RENT",
                    "aseContentRequestList" to listOf(mapOf(
                        "extInfo" to mapOf(
                            "chInfo" to "chInfo=ch_zmzltf__chsub_xinyongsyyingxiaowei",
                            "exposedItemList" to "",
                            "pageIndex" to pageIndex
                        ),
                        "positionCode" to positionCode
                    )),
                    "extInfo" to mapOf(
                        "venuePageId" to "HP_1755094925"
                    ),
                    "frontPageId" to "HP_1755094925"
                )
            ))
        )
    }
}
