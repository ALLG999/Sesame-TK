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

            // 先查询首页获取浏览任务
            processHomePageTasks()

            // 查询任务列表
            val taskList = queryTaskList()
            if (taskList.isEmpty()) {
                Log.record(TAG, "未获取到有效任务列表")
                return
            }

            // 过滤掉需要下单的任务，只处理浏览类任务
            val browseTasks = taskList.filter { isBrowseTask(it) }
            if (browseTasks.isEmpty()) {
                Log.record(TAG, "未找到可执行的浏览类任务")
                return
            }

            Log.record(TAG, "找到${browseTasks.size}个浏览类任务")

            // 处理浏览任务
            processBrowseTasks(browseTasks)

            // 自动领取奖励
            if (autoReceiveReward!!.value == true) {
                autoReceiveRewards(browseTasks)
            }

        } catch (t: Throwable) {
            Log.runtime(TAG, "run error:")
            Log.printStackTrace(TAG, t)
        } finally {
            Log.record(TAG, "执行结束-${getName()}")
        }
    }

    /**
     * 处理首页浏览任务
     */
    private fun processHomePageTasks() {
        try {
            val playInfo = "SwbtxJSo8OOUrymAU%2FHnY2jyFRc%2BkCJ3"
            val response = AntGroupRpcCall.queryHomePage(playInfo)
            val jsonResponse = JSONObject(response)

            if (ResChecker.checkRes(TAG, jsonResponse)) {
                val data = jsonResponse.getJSONObject("Data")
                val resData = data.getJSONObject("resData")
                val extInfo = resData.getJSONObject("extInfo")
                val homePageResult = extInfo.getJSONObject("zhimaTreeHomePageQueryResult")

                // 获取首页浏览任务列表
                if (homePageResult.has("browseTaskList")) {
                    val browseTasks = homePageResult.getJSONArray("browseTaskList")
                    for (i in 0 until browseTasks.length()) {
                        val task = browseTasks.getJSONObject(i)
                        processHomeBrowseTask(task)
                    }
                }

                // 显示当前净化值
                val accountEnergy = homePageResult.optString("accountEnergy", "0")
                Log.record(TAG, "当前芝麻树净化值: $accountEnergy")

            } else {
                Log.runtime(TAG, "查询首页失败: ${jsonResponse.optString("resultDesc", "未知错误")}")
            }
        } catch (e: JSONException) {
            Log.printStackTrace(TAG, "首页JSON解析错误", e)
        } catch (t: Throwable) {
            Log.printStackTrace(TAG, "处理首页任务异常", t)
        }
    }

    /**
     * 处理首页浏览任务
     */
    private fun processHomeBrowseTask(taskData: JSONObject) {
        try {
            val taskProcessStatus = taskData.optString("taskProcessStatus", "NOT_DONE")
            val canAccess = taskData.optBoolean("canAccess", false)
            val taskId = taskData.optString("taskId", "")
            val appletId = taskData.optString("appletId", "")

            if (canAccess && taskProcessStatus == "NOT_DONE") {
                Log.record(TAG, "开始执行首页浏览任务: ${taskData.optJSONObject("taskMaterial")?.optString("title", "")}")

                // 触发首页浏览任务执行
                val triggerResponse = AntGroupRpcCall.triggerBrowseTask(taskId, appletId, "mock_play_info")
                val triggerJson = JSONObject(triggerResponse)

                if (ResChecker.checkRes(TAG, triggerJson)) {
                    val data = triggerJson.getJSONObject("Data")
                    val resData = data.getJSONObject("resData")
                    val taskOrderId = resData.optString("taskOrderId", "")

                    if (taskOrderId.isNotEmpty()) {
                        val taskMaterial = taskData.optJSONObject("taskMaterial") ?: JSONObject()
                        val title = taskMaterial.optString("title", "首页浏览任务")
                        val browseTime = taskMaterial.optString("Input_GxmW", "15").toIntOrNull() ?: 15
                        val purificationValue = taskMaterial.optString("finishOneTaskGetPurificationValue", "50")

                        Log.forest("芝麻树🌳[完成首页浏览:$title]获得${purificationValue}净化值")
                        Toast.show("芝麻树完成: $title")

                        // 模拟浏览时间
                        Log.record(TAG, "模拟浏览${browseTime}秒...")
                        GlobalThreadPools.sleepCompat(browseTime * 1000L)
                    }
                } else {
                    Log.runtime(TAG, "执行首页浏览任务失败: ${triggerJson.optString("resultDesc", "未知错误")}")
                }
            } else if (taskProcessStatus == "RECEIVE_SUCCESS") {
                Log.record(TAG, "首页浏览任务已完成，等待领取奖励")
            }
        } catch (t: Throwable) {
            Log.printStackTrace(TAG, "处理首页浏览任务异常", t)
        }
    }

    /**
     * 查询任务列表
     */
    private fun queryTaskList(): List<TaskDetail> {
        val taskList = mutableListOf<TaskDetail>()

        try {
            val playInfo = "SwbtxJSo8OOUrymAU%2FHnY2jyFRc%2BkCJ3"
            val response = AntGroupRpcCall.queryTaskList(playInfo)
            val jsonResponse = JSONObject(response)

            if (ResChecker.checkRes(TAG, jsonResponse)) {
                val data = jsonResponse.getJSONObject("Data")
                val resData = data.getJSONObject("resData")
                val extInfo = resData.getJSONObject("extInfo")
                val taskDetailList = extInfo.getJSONObject("taskDetailList")
                val tasks = taskDetailList.getJSONArray("taskDetailList")

                for (i in 0 until tasks.length()) {
                    val task = tasks.getJSONObject(i)
                    val taskDetail = parseTaskDetail(task)
                    if (isTaskValid(taskDetail)) {
                        taskList.add(taskDetail)
                    }
                }

                Log.record(TAG, "成功获取${taskList.size}个任务")
            } else {
                Log.runtime(TAG, "查询任务列表失败: ${jsonResponse.optString("resultDesc", "未知错误")}")
            }
        } catch (e: JSONException) {
            Log.printStackTrace(TAG, "JSON解析错误", e)
        } catch (t: Throwable) {
            Log.printStackTrace(TAG, "查询任务列表异常", t)
        }

        return taskList
    }

    /**
     * 解析任务详情
     */
    private fun parseTaskDetail(taskData: JSONObject): TaskDetail {
        val taskBaseInfo = taskData.optJSONObject("taskBaseInfo") ?: JSONObject()
        val taskMaterial = taskData.optJSONObject("taskMaterial") ?: JSONObject()

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
            browseTime = taskMaterial.optString("browseTime"),
            jumpUrl = taskMaterial.optString("jumpUrl"),
            taskOrderId = taskData.optString("taskOrderId"),
            lastReceiveExpireTime = taskData.optLong("lastReceiveExpireTime", 0).takeIf { it > 0 },
            queryErrorCode = taskData.optString("queryErrorCode"),
            queryErrorMsg = taskData.optString("queryErrorMsg"),
            prizeDetails = parsePrizeDetails(taskData)
        )
    }

    /**
     * 解析奖励详情
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
                    prizeFrequency = displayInfo?.optJSONObject("extInfo")?.optString("PRIZE_FREQUENCY", "") ?: ""
                ))
            }
        } catch (e: Exception) {
            Log.printStackTrace(TAG, "解析奖励详情异常", e)
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

        // 根据过滤类型检查
        return when (taskFilterType!!.value) {
            1 -> task.finishOneTaskGetPurificationValue == 50 // 仅50净化值任务
            2 -> task.finishOneTaskGetPurificationValue >= 100 // 仅高奖励任务
            else -> true // 全部任务
        }
    }

    /**
     * 检查是否为浏览类任务
     */
    private fun isBrowseTask(task: TaskDetail): Boolean {
        // 只处理浏览类和引流类任务，排除下单类任务
        return task.taskType == "BROWSER" ||
                task.taskType == "DIVERSION" ||
                task.taskType == "COMMON_COUNT_DOWN_VIEW" ||
                (task.title.contains("浏览") || task.title.contains("看看") || task.title.contains("逛逛"))
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
                Log.printStackTrace(TAG, "处理浏览任务[${task.title}]异常", t)
            }
        }
    }

    /**
     * 执行浏览任务
     */
    private fun executeBrowseTask(task: TaskDetail) {
        try {
            Log.record(TAG, "开始执行浏览任务: ${task.title}")

            // 触发浏览任务执行
            val triggerResponse = AntGroupRpcCall.triggerBrowseTask(task.taskId, task.appletId, "mock_play_info")
            val triggerJson = JSONObject(triggerResponse)

            if (ResChecker.checkRes(TAG, triggerJson)) {
                val data = triggerJson.getJSONObject("Data")
                val resData = data.getJSONObject("resData")
                val taskOrderId = resData.optString("taskOrderId", "")

                if (taskOrderId.isNotEmpty()) {
                    Log.forest("芝麻树🌳[完成浏览:${task.title}]获得${task.finishOneTaskGetPurificationValue}净化值")
                    Toast.show("芝麻树完成: ${task.title}")

                    // 模拟浏览时间
                    val browseTime = task.browseTime?.toIntOrNull() ?: 15
                    Log.record(TAG, "模拟浏览${browseTime}秒...")
                    GlobalThreadPools.sleepCompat(browseTime * 1000L)
                }
            } else {
                Log.runtime(TAG, "执行浏览任务[${task.title}]失败: ${triggerJson.optString("resultDesc", "未知错误")}")
            }
        } catch (t: Throwable) {
            Log.printStackTrace(TAG, "执行浏览任务[${task.title}]异常", t)
        }
    }

    /**
     * 自动领取奖励
     */
    private fun autoReceiveRewards(tasks: List<TaskDetail>) {
        for (task in tasks) {
            try {
                if (task.taskProcessStatus == "RECEIVE_SUCCESS" && task.needManuallyReceiveAward && task.taskOrderId != null) {
                    // 获取主要奖励
                    val mainPrize = task.prizeDetails.firstOrNull()
                    if (mainPrize != null) {
                        val rewardResponse = AntGroupRpcCall.receiveTaskReward(
                            task.taskOrderId!!,
                            task.taskId,
                            mainPrize.prizeId
                        )
                        val rewardJson = JSONObject(rewardResponse)

                        if (ResChecker.checkRes(TAG, rewardJson)) {
                            val data = rewardJson.getJSONObject("Data")
                            val resData = data.getJSONObject("resData")
                            val rewardAmount = resData.optInt("rewardAmount", task.finishOneTaskGetPurificationValue)

                            Log.forest("芝麻树🌳[领取奖励:${task.title}]#${rewardAmount}净化值")
                            Toast.show("芝麻树领取: ${task.title}")
                        }
                    }

                    GlobalThreadPools.sleepCompat(800)
                }
            } catch (t: Throwable) {
                Log.printStackTrace(TAG, "领取任务[${task.title}]奖励异常", t)
            }
        }
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
        val prizeFrequency: String
    )
}