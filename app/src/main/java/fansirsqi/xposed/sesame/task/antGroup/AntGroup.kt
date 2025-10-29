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
import fansirsqi.xposed.sesame.util.CoroutineUtils
import fansirsqi.xposed.sesame.util.HttpUtil
import fansirsqi.xposed.sesame.util.Log
import org.json.JSONArray
import org.json.JSONObject
import java.util.*

/**
 * 芝麻树任务（仅浏览类任务，纯 RPC 版本）
 */
class AntGroup : ModelTask() {

    private val TAG = AntGroup::class.java.simpleName

    // 配置字段
    private var dailyTask: BooleanModelField? = null
    private var autoReceiveReward: BooleanModelField? = null
    private var taskFilterType: ChoiceModelField? = null
    private var excludedTaskList: SelectModelField? = null

    override fun getName(): String = "芝麻树"

    override fun getGroup(): ModelGroup = ModelGroup.FOREST

    override fun getIcon(): String = "AntGroup.png"

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
        TaskCommon.update()

        val forestPauseTime = RuntimeInfo.getInstance().getLong(RuntimeInfo.RuntimeInfoKey.ForestPauseTime)
        if (forestPauseTime > currentTime) {
            Log.record(TAG, "芝麻树任务-异常等待中，暂不执行检测！")
            return false
        }

        if (TaskCommon.IS_MODULE_SLEEP_TIME) {
            Log.record(TAG, "💤 模块休眠时间【${BaseModel.modelSleepTime.value}】停止执行${getName()}任务！")
            return false
        }

        if (TaskCommon.IS_ENERGY_TIME) {
            Log.record(TAG, "⏸ 当前为只收能量时间【${BaseModel.energyTime.value}】，停止执行${getName()}任务！")
            return false
        }

        return true
    }

    override fun run() {
        try {
            Log.record(TAG, "执行开始-${getName()}")

            if (!checkFieldEnabled(dailyTask)) {
                Log.record(TAG, "芝麻树任务未开启，跳过执行")
                return
            }

            queryEnergyStatus()

            val homePageTasks = processHomePageTasks()

            if (homePageTasks.isNotEmpty()) {
                Log.record(TAG, "处理${homePageTasks.size}个首页浏览任务")
                processBrowseTasks(homePageTasks)
            } else {
                Log.record(TAG, "未找到可执行的首页浏览任务")
            }

            if (checkFieldEnabled(autoReceiveReward)) {
                autoReceiveRewards(homePageTasks)
            }

            queryEnergyStatus()

        } catch (t: Throwable) {
            Log.runtime(TAG, "run error:")
            Log.printStackTrace(TAG, t)
        } finally {
            Log.record(TAG, "执行结束-${getName()}")
        }
    }

    // 检查配置字段是否启用
    private fun checkFieldEnabled(field: BooleanModelField?): Boolean {
        return field?.value == true
    }

    // 发送RPC请求
    private fun sendRpcRequest(apiName: String, payload: JSONObject): JSONObject? {
        val response = HttpUtil.post(apiName, payload.toString()) // 使用 HttpUtil 的 post 方法
        val jsonResponse = JSONObject(response)
        return if (ResChecker.checkRes(TAG, jsonResponse)) jsonResponse else null
    }

    private fun queryEnergyStatus() {
        try {
            val payload = JSONObject().apply {
                put("aseChannelId", "RENT")
            }
            val response = sendRpcRequest("com.alipay.creditapollon.venue.energy.query", payload)

            response?.let {
                val energyResult = it
                    .getJSONObject("Data")
                    .getJSONObject("resData")
                    .getJSONObject("extInfo")
                    .getJSONObject("zhimaTreeAccountEnergyQueryResult")
                val accountEnergy = energyResult.optString("accountEnergy", "0")
                Log.record(TAG, "当前芝麻树能量: ${accountEnergy}g")
            }

        } catch (t: Throwable) {
            Log.printStackTrace(TAG, "能量查询异常", t)
        }
    }

    private fun processBrowseTasks(tasks: List<TaskDetail>) {
        val successfulTasks = mutableListOf<String>()
        val failedTasks = mutableListOf<String>()

        tasks.forEachIndexed { index, task ->
            try {
                Log.record(TAG, "处理任务[${index + 1}/${tasks.size}]: ${task.title}")
                if (executeBrowseTask(task)) {
                    successfulTasks.add(task.title)
                } else {
                    failedTasks.add(task.title)
                }
                if (index < tasks.size - 1) CoroutineUtils.sleepCompat(1500)
            } catch (t: Throwable) {
                Log.printStackTrace(TAG, "处理任务异常", t)
                failedTasks.add(task.title)
            }
        }

        if (successfulTasks.isNotEmpty()) Log.forest("芝麻树🌳成功完成${successfulTasks.size}个任务")
        if (failedTasks.isNotEmpty()) Log.runtime(TAG, "芝麻树失败任务: ${failedTasks.joinToString()}")
    }

    private fun executeBrowseTask(task: TaskDetail): Boolean {
        return try {
            Log.record(TAG, "开始执行浏览任务: ${task.title} (ID: ${task.taskId})")
            val finishResponse = sendRpcRequest("com.alipay.creditapollon.venue.task.report", JSONObject().apply {
                put("taskId", task.taskId)
                put("taskType", "BROWSE_15S")
                put("status", "FINISH")
            })

            finishResponse?.let {
                val browseTime = task.browseTime?.toIntOrNull() ?: 15
                Log.record(TAG, "模拟浏览${browseTime}秒...")
                CoroutineUtils.sleepCompat(browseTime * 1000L)
                Log.forest("芝麻树🌳[完成浏览:${task.title}]获得${task.finishOneTaskGetPurificationValue}净化值")
                Toast.show("芝麻树完成: ${task.title}")
                CoroutineUtils.sleepCompat(2000)
                true
            } ?: false

        } catch (t: Throwable) {
            Log.printStackTrace(TAG, "执行浏览任务异常", t)
            false
        }
    }

    private fun autoReceiveRewards(tasks: List<TaskDetail>) {
        var rewardCount = 0
        tasks.forEach { task ->
            try {
                if (task.hasRewardToReceive) {
                    val mainPrize = task.prizeDetails.firstOrNull()
                    if (mainPrize != null && task.taskOrderId != null) {
                        val rewardResponse = sendRpcRequest("com.alipay.creditapollon.venue.task.report", JSONObject().apply {
                            put("taskOrderId", task.taskOrderId)
                            put("taskId", task.taskId)
                            put("prizeId", mainPrize.prizeId)
                        })
                        rewardResponse?.let {
                            rewardCount++
                            Log.forest("芝麻树🌳[领取奖励:${task.title}]#${task.finishOneTaskGetPurificationValue}净化值")
                            Toast.show("芝麻树领取: ${task.title}")
                        }
                    }
                    CoroutineUtils.sleepCompat(800)
                }
            } catch (t: Throwable) {
                Log.printStackTrace(TAG, "领取奖励异常", t)
            }
        }
        if (rewardCount > 0) Log.record(TAG, "成功领取${rewardCount}个任务奖励")
    }

    private fun processHomePageTasks(): List<TaskDetail> {
        val taskList = mutableListOf<TaskDetail>()
        try {
            val response = sendRpcRequest("com.alipay.creditapollon.venue.page.layout.query", JSONObject().apply {
                put("aseChannelId", "RENT")
                put("page", 1)
            })
            response?.let {
                val tasks = it.getJSONArray("tasks")
                for (i in 0 until tasks.length()) {
                    taskList.add(parseTaskDetail(tasks.getJSONObject(i)))
                }
            }
        } catch (t: Throwable) {
            Log.printStackTrace(TAG, "查询首页任务失败", t)
        }
        return taskList
    }

    private fun parseTaskDetail(taskJson: JSONObject): TaskDetail {
        return TaskDetail(
            taskId = taskJson.optString("taskId"),
            taskType = taskJson.optString("taskType"),
            taskStatus = taskJson.optString("taskStatus"),
            taskTitle = taskJson.optString("taskTitle"),
            reward = taskJson.optString("reward"),
            browseTime = taskJson.optString("browseTime"),
            finishReward = taskJson.optString("finishReward")
        )
    }
}
