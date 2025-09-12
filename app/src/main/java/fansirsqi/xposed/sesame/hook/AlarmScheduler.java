package fansirsqi.xposed.sesame.hook;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.PowerManager;

import androidx.annotation.RequiresApi;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import fansirsqi.xposed.sesame.data.General;
import fansirsqi.xposed.sesame.newutil.DataStore;
import fansirsqi.xposed.sesame.task.BaseTask;
import fansirsqi.xposed.sesame.util.Log;
import fansirsqi.xposed.sesame.util.Notify;
import fansirsqi.xposed.sesame.util.TimeUtil;
import org.json.JSONObject;

/**
 * 统一的闹钟调度管理器
 * <p>
 * 负责管理所有闹钟相关功能，包括：
 * 1. 闹钟的设置和取消
 * 2. 权限检查和处理
 * 3. 备份机制管理
 * 4. 唤醒锁管理
 */
public class AlarmScheduler {
    private static final String TAG = "AlarmScheduler";
    
    // 闹钟相关常量
    public static class Constants {
        public static final long WAKE_LOCK_SETUP_TIMEOUT = 5000L; // 5秒
        public static final long FIRST_BACKUP_DELAY = 20000L; // 20秒，缩短第一级备份延迟
        public static final long SECOND_BACKUP_DELAY = 50000L; // 50秒，缩短第二级备份延迟
        public static final long BACKUP_ALARM_DELAY = 15000L; // 15秒，缩短备份闹钟延迟
        public static final int BACKUP_REQUEST_CODE_OFFSET = 10000;
        
        private Constants() {} // 防止实例化
    }
    
    // 广播动作常量
    public static class Actions {
        public static final String EXECUTE = "com.eg.android.AlipayGphone.sesame.execute";
        public static final String ALARM_CATEGORY = "fansirsqi.xposed.sesame.ALARM_CATEGORY";
        
        private Actions() {} // 防止实例化
    }
    
    private final Context context;
    private final Handler mainHandler;
    private final Map<Integer, PendingIntent> scheduledAlarms = new ConcurrentHashMap<>();
    private static PowerManager.WakeLock wakeLock;
    
    public AlarmScheduler(Context context, Handler mainHandler) {
        this.context = context;
        this.mainHandler = mainHandler;
    }
    
    /**
     * 设置延迟执行闹钟（简化版本）
     */
    public void scheduleDelayedExecution(long delayMillis) {
        long exactTimeMillis = System.currentTimeMillis() + delayMillis;
        int requestCode = generateRequestCode(exactTimeMillis + 1); // +1避免与其他闹钟ID冲突
        
        Intent intent = createExecutionIntent(exactTimeMillis, requestCode);
        intent.putExtra("delayed_execution", true);

        scheduleAlarmWithBackup(exactTimeMillis, intent, requestCode, delayMillis);
    }
    
    /**
     * 设置精确时间执行闹钟（完整版本）
     */
    public void scheduleExactExecution(long delayMillis, long exactTimeMillis) {
        // 检查权限
        if (!checkAndRequestAlarmPermissions()) {
            // 权限不足时回退到简化版本
            scheduleDelayedExecution(delayMillis);
            return;
        }
        
        int requestCode = generateRequestCode(exactTimeMillis);
        Intent intent = createExecutionIntent(exactTimeMillis, requestCode);

        scheduleAlarmWithBackup(exactTimeMillis, intent, requestCode, delayMillis);
    }
    
    /**
     * 设置定时唤醒闹钟
     */
    public boolean scheduleWakeupAlarm(long triggerAtMillis, int requestCode, boolean isMainAlarm) {
        Intent intent = new Intent(Actions.EXECUTE);
        intent.putExtra("alarm_triggered", true);
        intent.putExtra("waken_at_time", true);
        
        if (!isMainAlarm) {
            intent.putExtra("waken_time", TimeUtil.getTimeStr(triggerAtMillis));
        }
        
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
            context, requestCode, intent, getPendingIntentFlags());
        
        return setAlarm(triggerAtMillis, pendingIntent, requestCode);
    }
    
    /**
     * 取消指定闹钟
     */
    public void cancelAlarm(PendingIntent pendingIntent) {
        try {
            if (pendingIntent != null) {
                AlarmManager alarmManager = getAlarmManager();
                if (alarmManager != null) {
                    alarmManager.cancel(pendingIntent);
                }
            }
        } catch (Exception e) {
            Log.error(TAG, "取消闹钟失败: " + e.getMessage());
            Log.printStackTrace(e);
        }
    }

    /**
     * 消费并取消一个已触发的闹钟
     * @param requestCode 闹钟的请求码
     */
    public void consumeAlarm(int requestCode) {
        if (requestCode == -1) {
            return;
        }
        PendingIntent pendingIntent = scheduledAlarms.get(requestCode);
        if (pendingIntent != null) {
            cancelAlarm(pendingIntent);
            scheduledAlarms.remove(requestCode);
            Log.record(TAG, "已消费并取消闹钟: ID=" + requestCode);
        }
    }
    
    /**
     * 核心闹钟设置方法
     */
    @SuppressLint("DefaultLocale")
    private boolean setAlarm(long triggerAtMillis, PendingIntent pendingIntent, int requestCode) {
        try {
            AlarmManager alarmManager = getAlarmManager();
            if (alarmManager == null) return false;
            
            // 取消旧闹钟（如果存在）
            cancelOldAlarm(requestCode);
            // 获取临时唤醒锁
            try (WakeLockManager ignored = new WakeLockManager(context, Constants.WAKE_LOCK_SETUP_TIMEOUT)) {
                // 根据Android版本和权限选择合适的闹钟类型
                // 1. 使用setAlarmClock以获得最高优先级
                AlarmManager.AlarmClockInfo alarmClockInfo = new AlarmManager.AlarmClockInfo(
                    triggerAtMillis,
                    // 创建一个用于显示闹钟设置界面的PendingIntent
                    PendingIntent.getActivity(context, 0, new Intent(), PendingIntent.FLAG_IMMUTABLE)
                );
                alarmManager.setAlarmClock(alarmClockInfo, pendingIntent);
                // 2. 同时设置一个备用的精确闹钟
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                );
                // 3. 获取PowerManager.WakeLock
                PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
                PowerManager.WakeLock wakeLock = powerManager.newWakeLock(
                    PowerManager.PARTIAL_WAKE_LOCK,
                    "Sesame:AlarmWakeLock:" + requestCode
                );
                wakeLock.acquire(5000); // 持有5秒钟以确保闹钟设置成功
                // 保存闹钟引用
                scheduledAlarms.put(requestCode, pendingIntent);
                return true;
            }
        } catch (Exception e) {
            Log.error(TAG, "设置闹钟失败: " + e.getMessage());
            Log.printStackTrace(e);
        }
        return false;
    }
    
    /**
     * 设置闹钟并配置备份机制
     */
    private void scheduleAlarmWithBackup(long exactTimeMillis, Intent intent, int requestCode, long delayMillis) {
        try {
            // 创建主闹钟
            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context, requestCode, intent, getPendingIntentFlags() | PendingIntent.FLAG_CANCEL_CURRENT);
            boolean success = setAlarm(exactTimeMillis, pendingIntent, requestCode);
            if (success) {
                // 设置备份机制
                scheduleBackupMechanisms(exactTimeMillis, delayMillis, requestCode);
                                // 更新通知
                updateNotification(exactTimeMillis);
                // 保存执行状态
                saveExecutionState(System.currentTimeMillis(), exactTimeMillis);
                Log.record(TAG, "已设置闹钟唤醒执行，ID=" + requestCode +
                    "，时间：" + TimeUtil.getCommonDate(exactTimeMillis) +
                    "，延迟：" + delayMillis / 1000 + "秒");
            }

        } catch (Exception e) {
            Log.error(TAG, "设置闹钟备份失败：" + e.getMessage());
            Log.printStackTrace(e);
            
            // 失败时使用Handler备份
            scheduleHandlerBackup(delayMillis);
        }
    }
    
    /**
     * 设置备份机制
     */
    private void scheduleBackupMechanisms(long exactTimeMillis, long delayMillis, int requestCode) {
        // 1. Handler第一级备份
        if (mainHandler != null) {
            mainHandler.postDelayed(() -> {
                long currentTime = System.currentTimeMillis();
                if (currentTime > exactTimeMillis + Constants.FIRST_BACKUP_DELAY) {
                    Log.error(TAG, "闹钟可能未触发，使用Handler备份执行 (第一级备份)");
                    executeBackupTask();
                }
            }, delayMillis + Constants.FIRST_BACKUP_DELAY);
            
            // 2. Handler第二级备份
            mainHandler.postDelayed(() -> {
                long currentTime = System.currentTimeMillis();
                if (currentTime > exactTimeMillis + Constants.SECOND_BACKUP_DELAY) {
                    Log.error(TAG, "闹钟和第一级备份可能都未触发，使用Handler备份执行 (第二级备份)");
                    executeBackupTask();
                }
            }, delayMillis + Constants.SECOND_BACKUP_DELAY);
        }
        // 3. 备份闹钟
        scheduleBackupAlarm(exactTimeMillis, requestCode);
    }
    
    /**
     * 设置备份闹钟
     */
    @SuppressLint("DefaultLocale")
    private void scheduleBackupAlarm(long exactTimeMillis, int mainRequestCode) {
        try {
            int backupRequestCode = mainRequestCode + Constants.BACKUP_REQUEST_CODE_OFFSET;
            Intent backupIntent = new Intent(Actions.EXECUTE);
            long backupTriggerTime = exactTimeMillis + Constants.BACKUP_ALARM_DELAY;
            backupIntent.putExtra("execution_time", backupTriggerTime);
            backupIntent.putExtra("request_code", backupRequestCode);
            backupIntent.putExtra("scheduled_at", System.currentTimeMillis());
            backupIntent.putExtra("alarm_triggered", true);
            backupIntent.putExtra("is_backup_alarm", true);
            backupIntent.setPackage(General.PACKAGE_NAME);
            PendingIntent backupPendingIntent = PendingIntent.getBroadcast(
                context, backupRequestCode, backupIntent, getPendingIntentFlags());
            AlarmManager alarmManager = getAlarmManager();
            if (alarmManager != null) {
                // 备份闹钟也使用AlarmClock以确保可靠性
                AlarmManager.AlarmClockInfo backupAlarmInfo = new AlarmManager.AlarmClockInfo(
                    backupTriggerTime,
                    PendingIntent.getActivity(context, 0, new Intent(), PendingIntent.FLAG_IMMUTABLE)
                );
                alarmManager.setAlarmClock(backupAlarmInfo, backupPendingIntent);
                scheduledAlarms.put(backupRequestCode, backupPendingIntent);
                Log.record(TAG, String.format("已设置备份闹钟: ID=%d, 预定时间=%s (+%d秒)", 
                    backupRequestCode, 
                    TimeUtil.getTimeStr(backupTriggerTime),
                    Constants.BACKUP_ALARM_DELAY / 1000));
            }
        } catch (Exception e) {
            Log.error(TAG, "设置备份闹钟失败: " + e.getMessage());
        }
    }
    
    /**
     * 创建执行Intent
     */
    private Intent createExecutionIntent(long exactTimeMillis, int requestCode) {
        Intent intent = new Intent(Actions.EXECUTE);
        intent.putExtra("execution_time", exactTimeMillis);
        intent.putExtra("request_code", requestCode);
        intent.putExtra("scheduled_at", System.currentTimeMillis());
        intent.putExtra("alarm_triggered", true);
        intent.putExtra("unique_id", System.currentTimeMillis() + "_" + requestCode);
        intent.setPackage(General.PACKAGE_NAME);
        intent.addCategory(Actions.ALARM_CATEGORY);
        return intent;
    }
    
    /**
     * 生成唯一请求码
     */
    private int generateRequestCode(long timeMillis) {
        return (int)((timeMillis % 10000) * 10 + (int)(Math.random() * 10));
    }
    
    /**
     * 检查并请求闹钟权限
     */
    private boolean checkAndRequestAlarmPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            AlarmManager alarmManager = getAlarmManager();
            if (alarmManager != null && !alarmManager.canScheduleExactAlarms()) {
                requestAlarmPermission();
                return false;
            }
        }
        return true;
    }
    
    /**
     * 请求闹钟权限
     */
    @RequiresApi(api = Build.VERSION_CODES.S)
    private void requestAlarmPermission() {
        try {
            Intent intent = new Intent(android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
            intent.setData(android.net.Uri.parse("package:" + General.PACKAGE_NAME));
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
            
            Log.record(TAG, "已发送精确闹钟权限请求，等待用户授权");
            Notify.updateStatusText("请授予精确闹钟权限以确保定时任务正常执行");
        } catch (Exception e) {
            Log.error(TAG, "请求精确闹钟权限失败: " + e.getMessage());
        }
    }

    /**
     * 取消旧闹钟
     */
    private void cancelOldAlarm(int requestCode) {
        PendingIntent oldPendingIntent = scheduledAlarms.get(requestCode);
        if (oldPendingIntent != null) {
            AlarmManager alarmManager = getAlarmManager();
            if (alarmManager != null) {
                alarmManager.cancel(oldPendingIntent);
                scheduledAlarms.remove(requestCode);
                Log.debug(TAG, "已取消旧闹钟: ID=" + requestCode);
            }
        }
    }
    
    /**
     * 获取AlarmManager实例
     */
    private AlarmManager getAlarmManager() {
        return (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
    }
    
    /**
     * 获取PendingIntent标志
     */
    private int getPendingIntentFlags() {
        return PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT;
    }
    
    /**
     * 执行备份任务
     */
    private void executeBackupTask() {
        try {
            // 通过反射调用ApplicationHook的方法，避免循环依赖
            Class<?> appHookClass = Class.forName("fansirsqi.xposed.sesame.hook.ApplicationHook");
            java.lang.reflect.Method getTaskMethod = appHookClass.getDeclaredMethod("getMainTask");
            getTaskMethod.setAccessible(true);
            Object mainTask = getTaskMethod.invoke(null);

            // 检查主任务是否已在运行
            if (mainTask != null) {
                Thread taskThread = ((BaseTask) mainTask).getThread();
                if (taskThread != null && taskThread.isAlive()) {
                    Log.record(TAG, "主任务正在运行，备份任务跳过执行。");
                    return;
                }
            }

            java.lang.reflect.Method initMethod = appHookClass.getDeclaredMethod("initHandler", Boolean.class);
            initMethod.setAccessible(true);
            
            Boolean initResult = (Boolean) initMethod.invoke(null, true);
            if (initResult != null && initResult) {
                if (mainTask != null) {
                    java.lang.reflect.Method startTaskMethod = mainTask.getClass().getMethod("startTask", Boolean.class);
                    startTaskMethod.setAccessible(true);
                    startTaskMethod.invoke(mainTask, true);
                }
            }
            Log.record(TAG, "执行备份任务完成");
        } catch (Exception e) {
            Log.error(TAG, "执行备份任务失败: " + e.getMessage());
        }
    }

    /**
     * Handler备份执行
     */
    private void scheduleHandlerBackup(long delayMillis) {
        if (mainHandler != null) {
            mainHandler.postDelayed(() -> {
                Log.record(TAG, "闹钟设置失败，使用Handler备份执行");
                executeBackupTask();
            }, delayMillis);
        }
    }
    
    /**
     * 更新通知
     */
    private void updateNotification(long exactTimeMillis) {
        String nt = "⏰ 下次执行(Alarm) " + TimeUtil.getTimeStr(exactTimeMillis);
        Notify.updateNextExecText(exactTimeMillis);
        Toast.show(nt);
        Log.record(TAG, nt);
    }
    
    /**
     * 保存执行状态
     */
    private void saveExecutionState(long lastExecTime, long nextExecTime) {
        try {
            JSONObject state = new JSONObject();
            state.put("lastExecTime", lastExecTime);
            state.put("nextExecTime", nextExecTime);
            state.put("timestamp", System.currentTimeMillis());
            String stateJson = state.toString();
            DataStore.INSTANCE.put("execution_state", stateJson);
            Log.record(TAG, "已保存执行状态: " + stateJson);
        } catch (Exception e) {
            Log.error(TAG, "保存执行状态失败: " + e.getMessage());
        }
    }
    
    /**
     * 唤醒锁管理器 - 自动释放资源
     */
    private static class WakeLockManager implements AutoCloseable {
        private final PowerManager.WakeLock wakeLock;
        
        public WakeLockManager(Context context, long timeout) {
            PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Sesame:AlarmSetupWakeLock");
            try {
                wakeLock.acquire(timeout);
            } catch (Exception e) {
                Log.error(TAG, "获取唤醒锁失败: " + e.getMessage());
            }
        }
        
        @Override
        public void close() {
            if (wakeLock != null && wakeLock.isHeld()) {
                try {
                    wakeLock.release();
                } catch (Exception e) {
                    Log.error(TAG, "释放唤醒锁失败: " + e.getMessage());
                }
            }
        }
    }

    /**
     * 由BroadcastReceiver调用，用于处理闹钟触发
     */
    public void handleAlarmTrigger() {
        // 获取唤醒锁
        acquireWakeLock();
        // 执行任务
        executeBackupTask();
    }

    /**
     * 获取唤醒锁
     */
    private void acquireWakeLock() {
        if (wakeLock == null) {
            PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Sesame:AlarmExecutionWakeLock");
            wakeLock.setReferenceCounted(false);
        }
        if (!wakeLock.isHeld()) {
            // 设置15分钟超时，防止任务卡死导致无法释放
            wakeLock.acquire(15 * 60 * 1000L);
            Log.record(TAG, "闹钟触发，已获取唤醒锁以确保任务持续执行");
        }
    }

}
