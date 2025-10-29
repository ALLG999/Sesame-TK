package fansirsqi.xposed.sesame.task.antGroup

import fansirsqi.xposed.sesame.data.RuntimeInfo
import fansirsqi.xposed.sesame.entity.AlipayUser
import fansirsqi.xposed.sesame.hook.Toast
import fansirsqi.xposed.sesame.model.BaseModel
import fansirsqi.xposed.sesame.model.ModelFields
import fansirsqi.xposed.sesame.model.ModelGroup
import fansirsqi.xposed.sesame.model.modelFieldExt.BooleanModelField
import fansirsqi.xposed.sesame.model.modelFieldExt.ChoiceModelField
import fansirsqi.xposed.sesame.model.modelFieldExt.SelectModelField
import fansirsqi.xposed.sesame.task.ModelTask
import fansirsqi.xposed.sesame.task.TaskCommon
import fansirsqi.xposed.sesame.util.GlobalThreadPools
import fansirsqi.xposed.sesame.util.Log
import fansirsqi.xposed.sesame.util.ResChecker
import org.json.JSONException
import org.json.JSONObject
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

/**
 * 芝麻树任务（仅浏览类任务）
 */
class AntGroup : ModelTask() {

    private val TAG = AntGroup::class.java.simpleName

    // 配置字段
    private var dailyTask: BooleanModelField? = null
    private var autoReceiveReward: BooleanModelField? = null
    private var taskFilterType: ChoiceModelField? = null
    private var excludedTaskList: SelectModelField? = null

    // 任务重试计数
    private val taskTryCount = ConcurrentHashMap<String, AtomicInteger>()

    // 固定的playInfo，从抓包中获取
    private val fixedPlayInfo = "SwbtxJSo8OOUrymAU%2FHnY2jyFRc%2BkCJ3"

    override fun getName(): String {
        return "芝麻树"
    }

    override fun getGroup(): ModelGroup {
        return ModelGroup.FOREST
    }

    override fun getIcon(): String {
        return "AntGroup.png"
    }

    override fun getFields(): ModelFields {
        val modelFields = ModelFields()

        modelFields.addField(BooleanModelField("dailyTask", "芝麻树任务", false).also { dailyTask = it })
        modelFields.addField(BooleanModelField("autoReceiveReward", "自动领取奖励", true).also { autoReceiveReward = it })
        modelFields.addField(ChoiceModelField("taskFilterType", "任务过滤类型", 0, arrayOf("全部任务", "仅50净化值任务", "仅高奖励任务")).also { taskFilterType = it })
        modelFields.addField(SelectModelField("excludedTaskList", "排除的任务", LinkedHashSet(), AlipayUser::getList).also { excludedTaskList = it })

        return modelFields
    }

    override fun check(): Boolean {
        val currentTime = System.currentTimeMillis()

        // 1. 先更新时间状态，保证状态正确
        TaskCommon.update()

        // 2. 异常等待状态检查
        val forestPauseTime = RuntimeInfo.getInstance().getLong(RuntimeInfo.RuntimeInfoKey.ForestPauseTime)
        if (forestPauseTime > currentTime) {
            Log.record(TAG, "芝麻树任务-异常等待中，暂不执行检测！")
            return false
        }

        // 3. 模块休眠时间检查
        if (TaskCommon.IS_MODULE_SLEEP_TIME) {
            Log.record(TAG, "💤 模块休眠时间【" + BaseModel.modelSleepTime.value + "】停止执行" + getName() + "任务！")
            return false
        }

        // 4. 只收能量时间段判断
        if (TaskCommon.IS_ENERGY_TIME) {
            Log.record(TAG, "⏸ 当前为只收能量时间【" + BaseModel.energyTime.value + "】，停止执行" + getName() + "任务！")
            return false
        }

        return true
    }

    override fun run() {
        try {
            Log.record(TAG, "执行开始-${getName()}")

            if (dailyTask!!.value != true) {
                Log.record(TAG, "芝麻树任务未开启，跳过执行")
                return
            }

            // 初始化RPC版本
            AntGroupRpcCall.init()

            // 查询当前能量状态
            queryEnergyStatus()

            // 先查询首页获取浏览任务
            val homePageTasks = processHomePageTasks()
            
            // 如果有首页浏览任务，先处理
            if (homePageTasks.isNotEmpty()) {
                Log.record(TAG, "处理${homePageTasks.size}个首页浏览任务")
                processBrowseTasks(homePageTasks)
            } else {
                Log.record(TAG, "未找到可执行的首页浏览任务")
            }

            // 自动领取奖励
            if (autoReceiveReward!!.value == true) {
                autoReceiveRewards(homePageTasks)
            }

            // 查询最终能量状态
            queryEnergyStatus()

        } catch (t: Throwable) {
            Log.runtime(TAG, "run error:")
            Log.error(TAG, "执行任务异常", t)
        } finally {
            Log.record(TAG, "执行结束-${getName()}")
        }
    }

    /**
     * 查询能量状态
     */
    private fun queryEnergyStatus() {
        try {
            val response = AntGroupRpcCall.queryForestEnergy(fixedPlayInfo)
            val jsonResponse = JSONObject(response)

            if (ResChecker.checkRes(TAG, jsonResponse)) {
                val data = jsonResponse.getJSONObject("Data")
                val resData = data.getJSONObject("resData")
                val extInfo = resData.getJSONObject("extInfo")
                val energyResult = extInfo.getJSONObject("zhimaTreeAccountEnergyQueryResult")
                
                val accountEnergy = energyResult.optString("accountEnergy", "0")
                Log.record(TAG, "当前芝麻树能量: ${accountEnergy}g")
                
            } else {
                Log.runtime(TAG, "查询能量状态失败: ${jsonResponse.optString("resultDesc", "未知错误")}")
            }
        } catch (e: JSONException) {
            Log.error(TAG, "能量查询JSON解析错误", e)
        } catch (t: Throwable) {
            Log.error(TAG, "查询能量状态异常", t)
        }
    }

    /**
     * 处理首页浏览任务
     */
    private fun processHomePageTasks(): List<TaskDetail> {
        val taskList = mutableListOf<TaskDetail>()
        
        try {
            val response = AntGroupRpcCall.queryHomePage(fixedPlayInfo)
            val jsonResponse = JSONObject(response)

            if (ResChecker.checkRes(TAG, jsonResponse)) {
                val data = jsonResponse.getJSONObject("Data")
                val resData = data.getJSONObject("resData")
                val extInfo = resData.getJSONObject("extInfo")
                val homePageResult = extInfo.getJSONObject("zhimaTreeHomePageQueryResult")

                // 获取当前能量值
                val accountEnergy = homePageResult.optString("accountEnergy", "0")
                Log.record(TAG, "芝麻树首页查询-当前能量: ${accountEnergy}g")

                // 获取首页浏览任务列表
                if (homePageResult.has("browseTaskList")) {
                    val browseTasks = homePageResult.getJSONArray("browseTaskList")
                    for (i in 0 until browseTasks.length()) {
                        val task = browseTasks.getJSONObject(i)
                        val taskDetail = parseHomeBrowseTask(task)
                        if (isTaskValid(taskDetail)) {
                            taskList.add(taskDetail)
                        }
                    }
                }

                // 获取树木状态信息
                if (homePageResult.has("trees")) {
                    val trees = homePageResult.getJSONArray("trees")
                    if (trees.length() > 0) {
                        val tree = trees.getJSONObject(0)
                        val scoreSummary = tree.optInt("scoreSummary", 0)
                        val currentLevelProcessState = tree.optInt("currentLevelProcessState", 0)
                        val treeLevel = tree.optInt("treeLevel", 1)
                        Log.record(TAG, "芝麻树状态: 等级${treeLevel}, 净化值${scoreSummary}, 进度${currentLevelProcessState}%")
                    }
                }

                Log.record(TAG, "首页查询成功，找到${taskList.size}个浏览任务")
            } else {
                Log.runtime(TAG, "查询首页失败: ${jsonResponse.optString("resultDesc", "未知错误")}")
            }
        } catch (e: JSONException) {
            Log.error(TAG, "首页JSON解析错误", e)
        } catch (t: Throwable) {
            Log.error(TAG, "处理首页任务异常", t)
        }
        
        return taskList
    }

    /**
     * 解析首页浏览任务
     */
    private fun parseHomeBrowseTask(taskData: JSONObject): TaskDetail {
        val taskMaterial = taskData.optJSONObject("taskMaterial") ?: JSONObject()
        val taskBaseInfo = taskData.optJSONObject("taskBaseInfo") ?: JSONObject()
        
        return TaskDetail(
            taskId = taskData.optString("taskId", ""),
            appletId = taskBaseInfo.optString("appletId", ""),
            taskName = taskBaseInfo.optString("appletName", ""),
            taskType = taskData.optString("taskType", ""),
            taskProcessStatus = taskData.optString("taskProcessStatus", "NOT_DONE"),
            canAccess = taskData.optBoolean("canAccess", false),
            needManuallyReceiveAward = taskData.optBoolean("needManuallyReceiveAward", false),
            needSignUp = taskData.optBoolean("needSignUp", false),
            accessLimitCount = taskData.optInt("accessLimitCount", 0),
            accessLimitDimension = taskData.optString("accessLimitDimension", "L"),
            periodCurrentCompleteNum = taskData.optInt("periodCurrentCompleteNum", 0),
            periodTotalCompleteNum = taskData.optInt("periodTotalCompleteNum", 1),
            finishOneTaskGetPurificationValue = taskMaterial.optString("finishOneTaskGetPurificationValue", "0").toIntOrNull() ?: 0,
            title = taskMaterial.optString("title", ""),
            subTitle = taskMaterial.optString("subTitle", ""),
            taskIcon = taskMaterial.optString("taskIcon", ""),
            buttonTextNotComplete = taskMaterial.optString("buttonTextNotComplete", ""),
            buttonTextFinished = taskMaterial.optString("buttonTextFinished", ""),
            browseTime = taskMaterial.optString("Input_GxmW", "15"),
            jumpUrl = null,
            taskOrderId = null,
            lastReceiveExpireTime = null,
            queryErrorCode = null,
            queryErrorMsg = null,
            prizeDetails = parsePrizeDetailsFromHomeTask(taskData)
        )
    }

    /**
     * 从首页任务解析奖励详情
     */
    private fun parsePrizeDetailsFromHomeTask(taskData: JSONObject): List<PrizeDetail> {
        val prizeDetails = mutableListOf<PrizeDetail>()
        try {
            val prizeArray = taskData.optJSONArray("validPrizeDetailDTO") ?: return prizeDetails

            for (i in 0 until prizeArray.length()) {
                val prize = prizeArray.getJSONObject(i)
                val baseInfo = prize.getJSONObject("prizeBaseInfoDTO")
                val displayInfo = prize.optJSONObject("prizeCustomDisplayInfoDTO")

                prizeDetails.add(PrizeDetail(
                    prizeId = prize.optString("prizeId", ""),
                    prizeName = baseInfo.optString("prizeName", ""),
                    prizeStatus = baseInfo.optString("prizeStatus", ""),
                    budgetStatus = baseInfo.optString("budgetStatus", ""),
                    budgetAmount = baseInfo.optLong("budgetAmount", 0),
                    budgetType = baseInfo.optString("budgetType", ""),
                    amountUnitText = displayInfo?.optString("amountUnitText", "") ?: "",
                    formType = displayInfo?.optString("formType", "") ?: "",
                    prizeFrequency = "",
                    energyValue = 0 // 首页任务奖励主要是净化值，不是能量
                ))
            }
        } catch (e: Exception) {
            Log.error(TAG, "解析首页任务奖励详情异常", e)
        }
        return prizeDetails
    }

    /**
     * 检查任务是否有效
     */
    private fun isTaskValid(task: TaskDetail): Boolean {
        // 检查排除列表
        if (excludedTaskList!!.value.contains(task.taskId)) {
            return false
        }

        // 只处理未完成且可访问的任务
        if (!task.canAccess || task.taskProcessStatus != "NOT_DONE") {
            return false
        }

        // 根据过滤类型检查
        return when (taskFilterType!!.value) {
            1 -> task.finishOneTaskGetPurificationValue == 50 // 仅50净化值任务
            2 -> task.finishOneTaskGetPurificationValue >= 100 // 仅高奖励任务
            else -> true // 全部任务
        }
    }

    /**
     * 处理浏览任务
     */
    private fun processBrowseTasks(tasks: List<TaskDetail>) {
        for (task in tasks) {
            try {
                if (task.canAccess && task.taskProcessStatus == "NOT_DONE") {
                    // 执行浏览任务
                    executeBrowseTask(task)
                }

                GlobalThreadPools.sleepCompat(1000)
            } catch (t: Throwable) {
                Log.error(TAG, "处理浏览任务[${task.title}]异常", t)
            }
        }
    }

    /**
     * 执行浏览任务
     */
    private fun executeBrowseTask(task: TaskDetail) {
        try {
            Log.record(TAG, "开始执行浏览任务: ${task.title}")

            // 根据抓包，浏览任务完成后需要调用任务完成接口
            val finishResponse = AntGroupRpcCall.finishTask(taskId = task.taskId)
            val finishJson = JSONObject(finishResponse)

            if (ResChecker.checkRes(TAG, finishJson)) {
                Log.forest("芝麻树🌳[完成浏览:${task.title}]获得${task.finishOneTaskGetPurificationValue}净化值")
                Toast.show("芝麻树完成: ${task.title}")

                // 模拟浏览时间（从任务配置中获取）
                val browseTime = task.browseTime?.toIntOrNull() ?: 15
                Log.record(TAG, "模拟浏览${browseTime}秒...")
                GlobalThreadPools.sleepCompat(browseTime * 1000L)

                // 任务完成后短暂等待
                GlobalThreadPools.sleepCompat(2000)
            } else {
                Log.runtime(TAG, "执行浏览任务[${task.title}]失败: ${finishJson.optString("resultDesc", "未知错误")}")
            }
        } catch (t: Throwable) {
            Log.error(TAG, "执行浏览任务[${task.title}]异常", t)
        }
    }

    /**
     * 自动领取奖励
     */
    private fun autoReceiveRewards(tasks: List<TaskDetail>) {
        // 根据抓包数据，浏览任务完成后奖励是自动发放的，不需要单独领取
        // 这里主要处理其他可能需要手动领取的任务类型
        var rewardCount = 0
        
        for (task in tasks) {
            try {
                if (task.taskProcessStatus == "RECEIVE_SUCCESS" && task.needManuallyReceiveAward) {
                    // 对于需要手动领取的任务，调用领取接口
                    val mainPrize = task.prizeDetails.firstOrNull()
                    if (mainPrize != null && task.taskOrderId != null) {
                        val rewardResponse = AntGroupRpcCall.receiveTaskReward(
                            task.taskOrderId!!,
                            task.taskId,
                            mainPrize.prizeId
                        )
                        val rewardJson = JSONObject(rewardResponse)

                        if (ResChecker.checkRes(TAG, rewardJson)) {
                            rewardCount++
                            Log.forest("芝麻树🌳[领取奖励:${task.title}]#${task.finishOneTaskGetPurificationValue}净化值")
                            Toast.show("芝麻树领取: ${task.title}")
                        }
                    }

                    GlobalThreadPools.sleepCompat(800)
                }
            } catch (t: Throwable) {
                Log.error(TAG, "领取任务[${task.title}]奖励异常", t)
            }
        }
        
        if (rewardCount > 0) {
            Log.record(TAG, "成功领取${rewardCount}个任务奖励")
        }
    }

    /**
     * 解析奖励详情（通用方法）
     */
    private fun parsePrizeDetails(taskData: JSONObject): List<PrizeDetail> {
        val prizeDetails = mutableListOf<PrizeDetail>()
        try {
            val prizeArray = taskData.optJSONArray("validPrizeDetailDTO") ?: return prizeDetails

            for (i in 0 until prizeArray.length()) {
                val prize = prizeArray.getJSONObject(i)
                val baseInfo = prize.getJSONObject("prizeBaseInfoDTO")
                val displayInfo = prize.optJSONObject("prizeCustomDisplayInfoDTO")

                prizeDetails.add(PrizeDetail(
                    prizeId = prize.optString("prizeId", ""),
                    prizeName = baseInfo.optString("prizeName", ""),
                    prizeStatus = baseInfo.optString("prizeStatus", ""),
                    budgetStatus = baseInfo.optString("budgetStatus", ""),
                    budgetAmount = baseInfo.optLong("budgetAmount", 0),
                    budgetType = baseInfo.optString("budgetType", ""),
                    amountUnitText = displayInfo?.optString("amountUnitText", "") ?: "",
                    formType = displayInfo?.optString("formType", "") ?: "",
                    prizeFrequency = displayInfo?.optJSONObject("extInfo")?.optString("PRIZE_FREQUENCY", "") ?: "",
                    energyValue = prize.optInt("energyValue", 0)
                ))
            }
        } catch (e: Exception) {
            Log.error(TAG, "解析奖励详情异常", e)
        }
        return prizeDetails
    }

    /**
     * 任务详情数据类
     */
    data class TaskDetail(
        val taskId: String,
        val appletId: String,
        val taskName: String,
        val taskType: String,
        val taskProcessStatus: String,
        val canAccess: Boolean,
        val needManuallyReceiveAward: Boolean,
        val needSignUp: Boolean,
        val accessLimitCount: Int,
        val accessLimitDimension: String,
        val periodCurrentCompleteNum: Int,
        val periodTotalCompleteNum: Int,
        val finishOneTaskGetPurificationValue: Int,
        val title: String,
        val subTitle: String,
        val taskIcon: String,
        val buttonTextNotComplete: String,
        val buttonTextFinished: String,
        val browseTime: String?,
        val jumpUrl: String?,
        val taskOrderId: String?,
        val lastReceiveExpireTime: Long?,
        val queryErrorCode: String?,
        val queryErrorMsg: String?,
        val prizeDetails: List<PrizeDetail>
    ) {
        val isCompleted: Boolean get() = taskProcessStatus == "RECEIVE_SUCCESS"
        val canComplete: Boolean get() = canAccess && taskProcessStatus == "NOT_DONE"
        val hasRewardToReceive: Boolean get() = taskProcessStatus == "RECEIVE_SUCCESS" && needManuallyReceiveAward
    }

    /**
     * 奖励详情数据类
     */
    data class PrizeDetail(
        val prizeId: String,
        val prizeName: String,
        val prizeStatus: String,
        val budgetStatus: String,
        val budgetAmount: Long,
        val budgetType: String,
        val amountUnitText: String,
        val formType: String,
        val prizeFrequency: String,
        val energyValue: Int
    )
}
