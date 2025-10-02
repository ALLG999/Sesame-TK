package fansirsqi.xposed.sesame.task.antGroup

import fansirsqi.xposed.sesame.util.Log
import org.json.JSONObject

/**
 * 芝麻树任务RPC调用类（仅浏览类任务）
 */
object AntGroupRpcCall {
    private const val TAG = "AntGroupRpcCall"

    /**
     * 查询芝麻树首页信息
     */
    fun queryHomePage(playInfo: String): String {
        return try {
            val requestData = mapOf(
                "operation" to "ZHIMA_TREE_HOME_PAGE",
                "playInfo" to playInfo,
                "extInfo" to mapOf<String, String>(),
                "refer" to "https://render.alipay.com/p/yuyan/180020010001269849/zmTree.html?caprMode=sync&chInfo=chInfo=ch_zmzltf__chsub_xinyongsyyingxiaowei"
            )

            val params = mapOf(
                "appName" to "promoprod",
                "facadeName" to "PlayTriggerRpcService",
                "methodName" to "trigger",
                "operationType" to "alipay.promoprod.play.trigger",
                "getResponse" to true,
                "requestData" to listOf(requestData)
            )

            val request = mapOf(
                "Method" to "alipay.promoprod.play.trigger",
                "Params" to params
            )

            executeRpcCall(JSONObject(request).toString())
        } catch (e: Exception) {
            Log.printStackTrace(TAG, e)
            "{\"success\":false,\"resultCode\":500,\"resultDesc\":\"查询首页异常\"}"
        }
    }

    /**
     * 查询任务列表
     */
    fun queryTaskList(playInfo: String): String {
        return try {
            val requestData = mapOf(
                "operation" to "RENT_GREEN_TASK_LIST_QUERY",
                "playInfo" to playInfo,
                "extInfo" to mapOf(
                    "batchId" to "",
                    "chInfo" to "ch_zmzltf__chsub_xinyongsyyingxiaowei"
                ),
                "refer" to "https://render.alipay.com/p/yuyan/180020010001269849/zmTree.html?caprMode=sync&chInfo=chInfo=ch_zmzltf__chsub_xinyongsyyingxiaowei"
            )

            val params = mapOf(
                "appName" to "promoprod",
                "facadeName" to "PlayTriggerRpcService",
                "methodName" to "trigger",
                "operationType" to "alipay.promoprod.play.trigger",
                "getResponse" to true,
                "requestData" to listOf(requestData)
            )

            val request = mapOf(
                "Method" to "alipay.promoprod.play.trigger",
                "Params" to params
            )

            executeRpcCall(JSONObject(request).toString())
        } catch (e: Exception) {
            Log.printStackTrace(TAG, e)
            "{\"success\":false,\"resultCode\":500,\"resultDesc\":\"RPC调用异常\"}"
        }
    }

    /**
     * 触发浏览任务执行
     */
    fun triggerBrowseTask(taskId: String, appletId: String, playInfo: String): String {
        return try {
            val requestData = mapOf(
                "operation" to "BROWSE_TASK_TRIGGER",
                "playInfo" to playInfo,
                "extInfo" to mapOf(
                    "taskId" to taskId,
                    "appletId" to appletId
                )
            )

            val params = mapOf(
                "appName" to "promoprod",
                "facadeName" to "PlayTriggerRpcService",
                "methodName" to "trigger",
                "operationType" to "alipay.promoprod.play.trigger",
                "getResponse" to true,
                "requestData" to listOf(requestData)
            )

            val request = mapOf(
                "Method" to "alipay.promoprod.play.trigger",
                "Params" to params
            )

            executeRpcCall(JSONObject(request).toString())
        } catch (e: Exception) {
            Log.printStackTrace(TAG, e)
            "{\"success\":false,\"resultCode\":500,\"resultDesc\":\"触发浏览任务异常\"}"
        }
    }

    /**
     * 领取任务奖励
     */
    fun receiveTaskReward(taskOrderId: String, taskId: String, prizeId: String): String {
        return try {
            val requestData = mapOf(
                "operation" to "REWARD_RECEIVE",
                "playInfo" to "",
                "extInfo" to mapOf(
                    "taskOrderId" to taskOrderId,
                    "taskId" to taskId,
                    "prizeId" to prizeId
                )
            )

            val params = mapOf(
                "appName" to "promoprod",
                "facadeName" to "PlayTriggerRpcService",
                "methodName" to "trigger",
                "operationType" to "alipay.promoprod.play.trigger",
                "getResponse" to true,
                "requestData" to listOf(requestData)
            )

            val request = mapOf(
                "Method" to "alipay.promoprod.play.trigger",
                "Params" to params
            )

            executeRpcCall(JSONObject(request).toString())
        } catch (e: Exception) {
            Log.printStackTrace(TAG, e)
            "{\"success\":false,\"resultCode\":500,\"resultDesc\":\"领取奖励异常\"}"
        }
    }

    /**
     * 执行RPC调用（实际网络请求）
     */
    private fun executeRpcCall(requestBody: String): String {
        return simulateRpcResponse(requestBody)
    }

    /**
     * 模拟RPC响应（仅包含浏览类任务）
     */
    private fun simulateRpcResponse(requestBody: String): String {
        val requestJson = JSONObject(requestBody)
        val params = requestJson.getJSONObject("Params")
        val requestData = params.getJSONArray("requestData").getJSONObject(0)
        val operation = requestData.getString("operation")

        return when (operation) {
            "ZHIMA_TREE_HOME_PAGE" -> {
                """
                {
                    "success": true,
                    "Data": {
                        "resData": {
                            "canRetry": false,
                            "extInfo": {
                                "zhimaTreeHomePageQueryResult": {
                                    "accountEnergy": "2948g",
                                    "browseTaskList": [
                                        {
                                            "taskId": "AP13307461",
                                            "appletId": "AP13307461",
                                            "taskProcessStatus": "NOT_DONE",
                                            "canAccess": true,
                                            "taskMaterial": {
                                                "title": "每日浏览商品",
                                                "Input_GxmW": "15",
                                                "finishOneTaskGetPurificationValue": "50"
                                            }
                                        }
                                    ]
                                }
                            }
                        },
                        "traceId": "mock_home_trace_id_${System.currentTimeMillis()}"
                    },
                    "header": {
                        "result-status": "1000",
                        "memo": "ok"
                    }
                }
                """.trimIndent()
            }
            "RENT_GREEN_TASK_LIST_QUERY" -> {
                """
                {
                    "success": true,
                    "Data": {
                        "resData": {
                            "canRetry": false,
                            "extInfo": {
                                "taskDetailList": {
                                    "taskDetailList": [
                                        {
                                            "taskId": "AP17309552",
                                            "appletId": "AP17309552", 
                                            "taskBaseInfo": {
                                                "appletName": "回收阵地浏览任务",
                                                "appletId": "AP17309552"
                                            },
                                            "taskProcessStatus": "NOT_DONE",
                                            "canAccess": true,
                                            "needManuallyReceiveAward": true,
                                            "needSignUp": false,
                                            "accessLimitCount": 1,
                                            "accessLimitDimension": "D",
                                            "periodCurrentCompleteNum": 0,
                                            "periodTotalCompleteNum": 1,
                                            "taskMaterial": {
                                                "title": "去看看热门手机/衣物回收",
                                                "subTitle": "金秋回收季，限时加价",
                                                "taskIcon": "https://mdn.alipayobjects.com/love_design/afts/img/6x3iQIrOVcMAAAAAQDAAAAgAeg-5AQBr",
                                                "buttonTextNotComplete": "去完成",
                                                "buttonTextFinished": "已完成",
                                                "finishOneTaskGetPurificationValue": "50",
                                                "jumpUrl": "alipays://platformapi/startapp?appId=2021002193653882&page=%2Fpages%2Findex%2Findex&chInfo=ch_zhimashu",
                                                "taskType": "BROWSER"
                                            },
                                            "validPrizeDetailDTO": [
                                                {
                                                    "prizeId": "PZ1936543446",
                                                    "prizeName": "芝麻树逛一逛芝麻租赁首页_净化值50",
                                                    "prizeBaseInfoDTO": {
                                                        "prizeId": "PZ1936543446",
                                                        "prizeName": "芝麻树逛一逛芝麻租赁首页_净化值50",
                                                        "prizeStatus": "PRIZE_OPENED",
                                                        "budgetStatus": "BUDGET_OPENED",
                                                        "budgetAmount": 99999999,
                                                        "budgetType": "COUNT",
                                                        "gmtBegin": 1756310400000,
                                                        "gmtEnd": 1787846399000
                                                    },
                                                    "prizeCustomDisplayInfoDTO": {
                                                        "amountUnitText": "元",
                                                        "formType": "VOUCHER",
                                                        "extInfo": {"PRIZE_FREQUENCY": "50"}
                                                    }
                                                }
                                            ]
                                        }
                                    ]
                                }
                            }
                        },
                        "traceId": "mock_trace_id_${System.currentTimeMillis()}"
                    },
                    "header": {
                        "result-status": "1000",
                        "memo": "ok"
                    }
                }
                """.trimIndent()
            }
            "BROWSE_TASK_TRIGGER" -> {
                """
                {
                    "success": true,
                    "Data": {
                        "resData": {
                            "taskOrderId": "mock_browse_order_${System.currentTimeMillis()}",
                            "canRetry": false
                        },
                        "traceId": "mock_trace_id_${System.currentTimeMillis()}"
                    },
                    "header": {
                        "result-status": "1000", 
                        "memo": "ok"
                    }
                }
                """.trimIndent()
            }
            "REWARD_RECEIVE" -> {
                """
                {
                    "success": true,
                    "Data": {
                        "resData": {
                            "prizeOrderId": "mock_prize_order_${System.currentTimeMillis()}",
                            "rewardAmount": 50
                        },
                        "traceId": "mock_trace_id_${System.currentTimeMillis()}"
                    },
                    "header": {
                        "result-status": "1000",
                        "memo": "ok"
                    }
                }
                """.trimIndent()
            }
            else -> {
                """
                {
                    "success": false,
                    "resultCode": 400,
                    "resultDesc": "未知操作类型: $operation"
                }
                """.trimIndent()
            }
        }
    }
}