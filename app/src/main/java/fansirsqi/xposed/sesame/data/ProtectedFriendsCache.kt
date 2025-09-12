package fansirsqi.xposed.sesame.data

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.kotlinModule
import fansirsqi.xposed.sesame.util.Files
import fansirsqi.xposed.sesame.util.JsonUtil
import fansirsqi.xposed.sesame.util.Log
import fansirsqi.xposed.sesame.util.TimeFormatter
import fansirsqi.xposed.sesame.util.TimeUtil
import java.io.File
import java.util.*
import java.util.concurrent.ConcurrentHashMap

/**
 * 保护罩好友缓存管理类
 * 参考 DataCache 设计，使用 Kotlin 实现
 * @date 2025/2/21
 */
object ProtectedFriendsCache {
    private const val TAG: String = "ProtectedFriendsCache"
    private const val FILENAME = "bubble_stakeout.json"
    private val FILE_PATH = Files.CONFIG_DIR

    @get:JsonIgnore
    private var init = false
    
    // Key-Value -> "userId_bubbleId": StakeoutInfo
    val stakeoutMap: MutableMap<String, StakeoutInfo> = ConcurrentHashMap()
    var lastCleanupTime: Long = 0

    private val objectMapper: ObjectMapper = ObjectMapper().registerModule(kotlinModule())

    init {
        load()
        checkAndCleanupExpiredData()
    }

    /**
     * 添加能量球蹲点任务
     */
    fun addStakeout(userId: String, userName: String, bubbleId: Long, maturityTime: Long) {
        try {
            checkAndCleanupExpiredData()
            
            val key = "${userId}_${bubbleId}"
            // 只在没有记录时才添加，避免重复处理
            if (!stakeoutMap.containsKey(key)) {
                val info = StakeoutInfo(userId, userName, bubbleId, maturityTime)
                stakeoutMap[key] = info
                
                val remainingTime = maturityTime - System.currentTimeMillis()
                val remainingTimeStr = TimeFormatter.formatTimeDifference(remainingTime)
                Log.record(TAG, "添加能量球蹲点: [$userName] 能量球ID: $bubbleId, 成熟时间: ${TimeUtil.getCommonDate(maturityTime)}, 剩余: $remainingTimeStr")
                
                save()
            }
        } catch (e: Exception) {
            Log.printStackTrace(TAG, "添加能量球蹲点失败", e)
        }
    }

    /**
     * 移除用户所有的蹲点任务
     */
    fun removeAllStakeoutsForUser(userId: String) {
        try {
            val keysToRemove = stakeoutMap.keys.filter { it.startsWith("${userId}_") }
            if (keysToRemove.isNotEmpty()) {
                var userName = ""
                keysToRemove.forEach { key ->
                    if (userName.isEmpty()) {
                        userName = stakeoutMap[key]?.userName ?: ""
                    }
                    stakeoutMap.remove(key)
                }
                Log.record(TAG, "移除好友[$userName]的所有 ${keysToRemove.size} 个能量球蹲点")
                save()
            }
        } catch (e: Exception) {
            Log.printStackTrace(TAG, "移除用户蹲点任务失败", e)
        }
    }

    /**
     * 检查是否在蹲点列表
     */
    fun hasStakeout(userId: String): Boolean {
        return try {
            checkAndCleanupExpiredData()
            val stakeoutInfo = stakeoutMap.values.find { it.userId == userId }
            if (stakeoutInfo != null) {
                if (stakeoutInfo.maturityTime > System.currentTimeMillis()) {
                    true
                } else {
                    removeAllStakeoutsForUser(userId)
                    false
                }
            } else {
                false
            }
        } catch (e: Exception) {
            Log.printStackTrace(TAG, "检查蹲点状态失败", e)
            false
        }
    }

    /**
     * 获取蹲点好友信息
     */
    fun getStakeoutInfo(userId: String): StakeoutInfo? {
        return try {
            checkAndCleanupExpiredData()
            val stakeoutInfo = stakeoutMap.values.find { it.userId == userId }
            if (stakeoutInfo != null && stakeoutInfo.maturityTime <= System.currentTimeMillis()) {
                removeAllStakeoutsForUser(userId)
                null
            } else {
                stakeoutInfo
            }
        } catch (e: Exception) {
            Log.printStackTrace(TAG, "获取蹲点信息失败", e)
            null
        }
    }

    /**
     * 获取所有蹲点好友
     */
    @JsonIgnore
    fun getAllStakeouts(): Set<StakeoutInfo> {
        return try {
            checkAndCleanupExpiredData()
            val currentTime = System.currentTimeMillis()
            stakeoutMap.values
                .filter { it.maturityTime > currentTime }
                .toSet()
        } catch (e: Exception) {
            Log.printStackTrace(TAG, "获取所有蹲点好友失败", e)
            emptySet()
        }
    }

    /**
     * 获取即将成熟的能量球（用于定时收取）
     * @param withinMinutes 多少分钟内到期
     * @return 即将成熟的能量球列表
     */
    @JsonIgnore
    fun getSoonMaturingBubbles(withinMinutes: Long = 2): List<StakeoutInfo> {
        return try {
            checkAndCleanupExpiredData()
            val currentTime = System.currentTimeMillis()
            val thresholdTime = currentTime + (withinMinutes * 60 * 1000L)
            
            stakeoutMap.values
                .filter { it.maturityTime in (currentTime + 1)..thresholdTime }
                .sortedBy { it.maturityTime }
        } catch (e: Exception) {
            Log.printStackTrace(TAG, "获取即将成熟的能量球失败", e)
            emptyList()
        }
    }
    
    /**
     * 获取下一个能量球成熟时间（用于定时任务）
     * @return 最近的能量球成熟时间戳，如果没有则返回0
     */
    @JsonIgnore
    fun getNextMaturityTime(): Long {
        return try {
            checkAndCleanupExpiredData()
            val currentTime = System.currentTimeMillis()
            
            stakeoutMap.values
                .filter { it.maturityTime > currentTime }
                .minByOrNull { it.maturityTime }
                ?.maturityTime ?: 0L
        } catch (e: Exception) {
            Log.printStackTrace(TAG, "获取下一个成熟时间失败", e)
            0L
        }
    }

    /**
     * 获取最近已成熟的能量球
     * @param withinMinutes 多少分钟内到期
     * @return 最近已成熟的能量球列表
     */
    @JsonIgnore
    fun getRecentlyMaturedBubbles(withinMinutes: Long = 5): List<StakeoutInfo> {
        return try {
            checkAndCleanupExpiredData()
            val currentTime = System.currentTimeMillis()
            val startTime = currentTime - (withinMinutes * 60 * 1000L)

            stakeoutMap.values
                .filter { it.maturityTime in startTime..currentTime }
                .sortedBy { it.maturityTime }
        } catch (e: Exception) {
            Log.printStackTrace(TAG, "获取最近成熟的能量球失败", e)
            emptyList()
        }
    }

    /**
     * 获取缓存统计信息
     */
    @JsonIgnore
    fun getCacheStats(): String {
        return try {
            checkAndCleanupExpiredData()
            val totalCount = stakeoutMap.size
            var expiredCount = 0
            var soonExpiredCount = 0
            val currentTime = System.currentTimeMillis()
            val soonExpiredThreshold = currentTime + (30 * 60 * 1000L) // 30分钟内过期
            
            for (info in stakeoutMap.values) {
                when {
                    info.maturityTime <= currentTime -> expiredCount++
                    info.maturityTime <= soonExpiredThreshold -> soonExpiredCount++
                }
            }
            
            "能量球蹲点缓存统计: 总数=$totalCount, 即将成熟=$soonExpiredCount, 已成熟=$expiredCount"
        } catch (e: Exception) {
            Log.printStackTrace(TAG, "获取缓存统计失败", e)
            "获取缓存统计失败"
        }
    }

    /**
     * 清理过期数据
     */
    private fun checkAndCleanupExpiredData() {
        try {
            val currentTime = System.currentTimeMillis()
            
            // 检查是否需要跨天清理
            if (isNewDay(currentTime)) {
                stakeoutMap.clear()
                lastCleanupTime = currentTime
                Log.record(TAG, "新的一天开始，清空所有能量球蹲点缓存")
                save()
                return
            }
            
            // 常规清理过期数据
            var removedCount = 0
            val iterator = stakeoutMap.entries.iterator()
            while (iterator.hasNext()) {
                val entry = iterator.next()
                if (entry.value.maturityTime <= currentTime) {
                    iterator.remove()
                    removedCount++
                }
            }
            
            if (removedCount > 0) {
                Log.record(TAG, "清理了 $removedCount 个已成熟的能量球记录")
                save()
            }
        } catch (e: Exception) {
            Log.printStackTrace(TAG, "检查和清理过期数据失败", e)
        }
    }

    /**
     * 判断是否是新的一天
     */
    private fun isNewDay(currentTime: Long): Boolean {
        if (lastCleanupTime == 0L) return true
        
        val lastDay = Calendar.getInstance().apply { timeInMillis = lastCleanupTime }
        val currentDay = Calendar.getInstance().apply { timeInMillis = currentTime }
        
        return lastDay.get(Calendar.DAY_OF_YEAR) != currentDay.get(Calendar.DAY_OF_YEAR) ||
               lastDay.get(Calendar.YEAR) != currentDay.get(Calendar.YEAR)
    }

    /**
     * 保存数据到文件
     */
    @Synchronized
    private fun save(): Boolean {
        val targetFile = File(FILE_PATH, FILENAME)
        val tempFile = File(targetFile.parent, "${targetFile.name}.tmp")
        return try {
            val json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(this)
            tempFile.writeText(json)
            if (tempFile.exists()) {
                targetFile.delete()
                tempFile.renameTo(targetFile)
                true
            } else {
                Log.error(TAG, "临时文件写入失败")
                false
            }
        } catch (e: Exception) {
            Log.error(TAG, "保存缓存数据失败：${e.message}")
            false
        }
    }

    /**
     * 从文件加载数据
     */
    @Synchronized
    private fun load(): Boolean {
        if (init) return true
        
        val targetFile = Files.getTargetFileofDir(FILE_PATH, FILENAME)
        var success = false
        
        try {
            if (targetFile.exists()) {
                val json = Files.readFromFile(targetFile)
                if (json.isNotEmpty()) {
                    objectMapper.readerForUpdating(this).readValue<Any>(json)

                    val formatted = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(this)
                    if (formatted != json) {
                        Log.runtime(TAG, "format $TAG config")
                        save()
                    }
                } else {
                    Log.runtime(TAG, "缓存文件为空，初始化为空状态")
                    save()
                }
            } else {
                Log.runtime(TAG, "init $TAG config")
                save()
            }
            success = true
        } catch (e: Exception) {
            Log.error(TAG, "加载缓存数据失败，文件已重置：${e.message}")
            // 清理状态并保存一个全新的空文件
            stakeoutMap.clear()
            lastCleanupTime = 0
            save()
        } finally {
            init = success
        }
        return success
    }

    /**
     * 能量球蹲点信息数据类
     */
    data class StakeoutInfo(
        val userId: String = "",
        val userName: String = "",
        val bubbleId: Long = 0,
        val maturityTime: Long = 0
    ) {
        override fun toString(): String {
            return "StakeoutInfo{userId='$userId', userName='$userName', bubbleId=$bubbleId, " +
                    "maturityTime='${TimeUtil.getCommonDate(maturityTime)}'}"
        }
    }
}
