package fansirsqi.xposed.sesame.hook;


import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Application;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;

import androidx.annotation.NonNull;

import org.luckypray.dexkit.DexKitBridge;

import java.io.File;
import java.io.IOException;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.lang.reflect.Method;
import java.lang.reflect.Member;
import java.lang.reflect.InvocationTargetException;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import fansirsqi.xposed.sesame.BuildConfig;
import fansirsqi.xposed.sesame.data.Config;
import fansirsqi.xposed.sesame.data.DataCache;
import fansirsqi.xposed.sesame.data.General;
import fansirsqi.xposed.sesame.data.Status;
import fansirsqi.xposed.sesame.entity.AlipayVersion;
import fansirsqi.xposed.sesame.hook.rpc.bridge.NewRpcBridge;
import fansirsqi.xposed.sesame.hook.rpc.bridge.OldRpcBridge;
import fansirsqi.xposed.sesame.hook.rpc.bridge.RpcBridge;
import fansirsqi.xposed.sesame.hook.rpc.bridge.RpcVersion;
import fansirsqi.xposed.sesame.hook.rpc.intervallimit.RpcIntervalLimit;
import fansirsqi.xposed.sesame.hook.server.ModuleHttpServer;
import fansirsqi.xposed.sesame.model.BaseModel;
import fansirsqi.xposed.sesame.model.Model;
import fansirsqi.xposed.sesame.newutil.DataStore;
import fansirsqi.xposed.sesame.task.BaseTask;
import fansirsqi.xposed.sesame.task.ModelTask;
import fansirsqi.xposed.sesame.task.TaskRunner;
import fansirsqi.xposed.sesame.util.AssetUtil;
import fansirsqi.xposed.sesame.util.Detector;
import fansirsqi.xposed.sesame.util.Files;
import fansirsqi.xposed.sesame.util.Log;
import fansirsqi.xposed.sesame.util.Notify;
import fansirsqi.xposed.sesame.util.PermissionUtil;
import fansirsqi.xposed.sesame.util.TimeUtil;
import fansirsqi.xposed.sesame.util.maps.UserMap;
import fansirsqi.xposed.sesame.hook.rpc.debug.DebugRpc;
import fi.iki.elonen.NanoHTTPD;
import kotlin.jvm.JvmStatic;
import lombok.Getter;

public class ApplicationHook implements IXposedHookLoadPackage {
    static final String TAG = ApplicationHook.class.getSimpleName();
    private ModuleHttpServer httpServer;
    private static final String modelVersion = BuildConfig.VERSION_NAME;
    /**
     * -- GETTER --
     *  获取闹钟调度器实例 - 供外部访问
     */
    // 统一的闹钟调度器
    @SuppressLint("StaticFieldLeak")
    @Getter
    private static AlarmScheduler alarmScheduler;
    @Getter
    private static ClassLoader classLoader = null;
    @Getter
    private static Object microApplicationContextObject = null;

    @SuppressLint("StaticFieldLeak")
    static Context appContext = null;

    @JvmStatic
    public static Context getAppContext() {
        return appContext;
    }

    @SuppressLint("StaticFieldLeak")
    static Context moduleContext = null;

    @Getter
    static AlipayVersion alipayVersion = new AlipayVersion("");
    private static volatile boolean hooked = false;

    @JvmStatic
    public static boolean isHooked() {
        return hooked;
    }

    private static volatile boolean init = false;
    static volatile Calendar dayCalendar;
    @Getter
    static volatile boolean offline = false;

    @Getter
    static final AtomicInteger reLoginCount = new AtomicInteger(0);
    @SuppressLint("StaticFieldLeak")
    static Service service;
    @Getter
    static Handler mainHandler;
    /**
     * -- GETTER --
     *  获取主任务实例 - 供AlarmScheduler使用
     */
    @Getter
    static BaseTask mainTask;
    static RpcBridge rpcBridge;
    @Getter
    private static RpcVersion rpcVersion;
    private static PowerManager.WakeLock wakeLock;

    public static void setOffline(boolean offline) {
        ApplicationHook.offline = offline;
    }

    private static volatile long lastExecTime = 0; // 添加为类成员变量
    private static final long MAX_INACTIVE_TIME = 3600000; // 最大不活动时间：1小时

    private XC_LoadPackage.LoadPackageParam modelLoadPackageParam;

    private static XC_LoadPackage.LoadPackageParam appLloadPackageParam;

    static {
        dayCalendar = Calendar.getInstance();
        dayCalendar.set(Calendar.HOUR_OF_DAY, 0);
        dayCalendar.set(Calendar.MINUTE, 0);
        dayCalendar.set(Calendar.SECOND, 0);
    }


    private final static Method deoptimizeMethod;

    static {
        Method m = null;
        try {
            m = XposedBridge.class.getDeclaredMethod("deoptimizeMethod", Member.class);
        } catch (Throwable t) {
            XposedBridge.log("E/" + TAG + " " + android.util.Log.getStackTraceString(t));
        }
        deoptimizeMethod = m;
    }

    static void deoptimizeMethod(Class < ? > c) throws InvocationTargetException, IllegalAccessException {
        for (Method m: c.getDeclaredMethods()) {
            if (deoptimizeMethod != null && m.getName().equals("makeApplicationInner")) {
                deoptimizeMethod.invoke(null, m);
                if (BuildConfig.DEBUG)
                    XposedBridge.log("D/" + TAG + " Deoptimized " + m.getName());
            }
        }
    }

    /**
     * 调度定时执行
     *
     * @param lastExecTime 上次执行时间
     */
    private void scheduleNextExecution(long lastExecTime) {
        try {
            // 检查长时间未执行的情况
            checkInactiveTime();
            int checkInterval = BaseModel.getCheckInterval().getValue();
            List < String > execAtTimeList = BaseModel.getExecAtTimeList().getValue();
            if (execAtTimeList != null && execAtTimeList.contains("-1")) {
                Log.record(TAG, "定时执行未开启");
                return;
            }

            long delayMillis = checkInterval; // 默认使用配置的检查间隔
            long targetTime = 0;

            try {
                if (execAtTimeList != null) {
                    Calendar lastExecTimeCalendar = TimeUtil.getCalendarByTimeMillis(lastExecTime);
                    Calendar nextExecTimeCalendar = TimeUtil.getCalendarByTimeMillis(lastExecTime + checkInterval);
                    for (String execAtTime: execAtTimeList) {
                        Calendar execAtTimeCalendar = TimeUtil.getTodayCalendarByTimeStr(execAtTime);
                        if (execAtTimeCalendar != null && lastExecTimeCalendar.compareTo(execAtTimeCalendar) < 0 && nextExecTimeCalendar.compareTo(execAtTimeCalendar) > 0) {
                            Log.record(TAG, "设置定时执行:" + execAtTime);
                            targetTime = execAtTimeCalendar.getTimeInMillis();
                            delayMillis = targetTime - lastExecTime;
                            break;
                        }
                    }
                }
            } catch (Exception e) {
                Log.runtime(TAG, "execAtTime err:：" + e.getMessage());
                Log.printStackTrace(TAG, e);
            }

            // 使用统一的闹钟调度器
            long l = targetTime > 0 ? targetTime : (lastExecTime + delayMillis);
            if (alarmScheduler != null) {
                alarmScheduler.scheduleExactExecution(delayMillis, l);
            } else {
                Log.error(TAG, "AlarmScheduler未初始化，无法设置闹钟");
            }
        } catch (Exception e) {
            Log.runtime(TAG, "scheduleNextExecution：" + e.getMessage());
            Log.printStackTrace(TAG, e);
        }
    }


    @SuppressLint("UnsafeDynamicallyLoadedCode")
    private void loadNativeLibs(Context context, File soFile) {
        try {
            File finalSoFile = AssetUtil.INSTANCE.copyStorageSoFileToPrivateDir(context, soFile);
            if (finalSoFile != null) {
                System.load(finalSoFile.getAbsolutePath());
                Log.runtime(TAG, "Loading " + soFile.getName() + " from :" + finalSoFile.getAbsolutePath());
            } else {
                Detector.INSTANCE.loadLibrary(soFile.getName().replace(".so", "").replace("lib", ""));
            }
        } catch (Exception e) {
            Log.error(TAG, "载入so库失败！！");
            Log.printStackTrace(e);
        }
    }

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam loadPackageParam) {
        if (General.MODULE_PACKAGE_NAME.equals(loadPackageParam.packageName)) {
            try {
                Class < ? > applicationClass = loadPackageParam.classLoader.loadClass("android.app.Application");
                XposedHelpers.findAndHookMethod(applicationClass, "onCreate", new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        moduleContext = (Context) param.thisObject;
                        // 可以在这里调用其他需要 Context 的 Hook 方法
                        HookUtil.INSTANCE.hookActive(loadPackageParam);
                    }
                });
            } catch (Exception e) {
                Log.printStackTrace(e);
            }
        } else if (General.PACKAGE_NAME.equals(loadPackageParam.packageName) && General.PACKAGE_NAME.equals(loadPackageParam.processName)) {
            try {
                if (hooked) return;
                appLloadPackageParam = loadPackageParam;
                classLoader = appLloadPackageParam.classLoader;
                // 在Hook Application.attach 之前，先 deoptimize LoadedApk.makeApplicationInner
                try {
                    @SuppressLint("PrivateApi") Class < ? > loadedApkClass = classLoader.loadClass("android.app.LoadedApk");
                    deoptimizeMethod(loadedApkClass);
                } catch (Throwable t) {
                    Log.runtime(TAG, "deoptimize makeApplicationInner err:");
                    Log.printStackTrace(TAG, t);
                }
                XposedHelpers.findAndHookMethod(Application.class, "attach", Context.class, new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        mainHandler = new Handler(Looper.getMainLooper());
                        appContext = (Context) param.args[0];
                        
                        // 初始化闹钟调度器
                        alarmScheduler = new AlarmScheduler(appContext, mainHandler);
                        PackageInfo pInfo = appContext.getPackageManager().getPackageInfo(appContext.getPackageName(), 0);
                        assert pInfo.versionName != null;
                        alipayVersion = new AlipayVersion(pInfo.versionName);
                        Log.runtime(TAG, "handleLoadPackage alipayVersion: " + alipayVersion.getVersionString());
                        loadNativeLibs(appContext, AssetUtil.INSTANCE.getCheckerDestFile());
                        loadNativeLibs(appContext, AssetUtil.INSTANCE.getDexkitDestFile());
                        HookUtil.INSTANCE.fuckAccounLimit(loadPackageParam);
                        if (BuildConfig.DEBUG) {
                            try {
                                Log.runtime(TAG, "start service for debug rpc");
                                httpServer = new ModuleHttpServer(8080, "ET3vB^#td87sQqKaY*eMUJXP");
                                httpServer.start(NanoHTTPD.SOCKET_READ_TIMEOUT, false);
                            } catch (IOException e) {
                                Log.printStackTrace(e);
                            }
                        } else {
                            Log.runtime(TAG, "need not start service for debug rpc");
                        }
                        super.afterHookedMethod(param);
                    }
                });
            } catch (Exception e) {
                Log.printStackTrace(e);
            }

            try {
                XposedHelpers.findAndHookMethod("com.alipay.mobile.quinox.LauncherActivity", classLoader, "onResume",
                        new XC_MethodHook() {
                            @Override
                            protected void afterHookedMethod(MethodHookParam param) {
                                Log.runtime(TAG, "hook onResume after start");
                                String targetUid = getUserId();
                                Log.runtime(TAG, "onResume targetUid: " + targetUid);
                                if (targetUid == null) {
                                    Log.record(TAG, "onResume:用户未登录");
                                    Toast.show("用户未登录");
                                    return;
                                }
                                if (!init) {
                                    if (initHandler(true)) {
                                        init = true;
                                    }
                                    Log.runtime(TAG, "initHandler success");
                                    return;
                                }
                                String currentUid = UserMap.getCurrentUid();
                                Log.runtime(TAG, "onResume currentUid: " + currentUid);
                                if (!targetUid.equals(currentUid)) {
                                    if (currentUid != null) {
                                        initHandler(true);
                                        Log.record(TAG, "用户已切换");
                                        Toast.show("用户已切换");
                                        return;
                                    }
                                    //                                    UserMap.initUser(targetUid);
                                    HookUtil.INSTANCE.hookUser(appLloadPackageParam);
                                }
                                if (offline) {
                                    offline = false;
                                    execHandler();
                                    ((Activity) param.thisObject).finish();
                                    Log.runtime(TAG, "Activity reLogin");
                                }
                                // 如果所有特殊情况都未命中，执行一次常规任务检查
                                execHandler();
                                Log.runtime(TAG, "hook onResume after end");
                            }
                        });
                Log.runtime(TAG, "hook login successfully");
            } catch (Throwable t) {
                Log.runtime(TAG, "hook login err");
                Log.printStackTrace(TAG, t);
            }
            try {
                XposedHelpers.findAndHookMethod("android.app.Service", classLoader, "onCreate",
                        new XC_MethodHook() {
                            @Override
                            protected void afterHookedMethod(MethodHookParam param) {
                                Service appService = (Service) param.thisObject;
                                if (!General.CURRENT_USING_SERVICE.equals(appService.getClass().getCanonicalName())) {
                                    return;
                                }

                                Log.runtime(TAG, "Service onCreate");
                                appContext = appService.getApplicationContext();
                                boolean isok = Detector.INSTANCE.isLegitimateEnvironment(appContext);
                                if (isok) {
                                    Detector.INSTANCE.dangerous(appContext);
                                    return;
                                }
                                String apkPath = loadPackageParam.appInfo.sourceDir;
                                try (DexKitBridge ignored = DexKitBridge.create(apkPath)) {
                                    // Other use cases
                                    Log.runtime(TAG, "hook dexkit successfully");
                                }
                                service = appService;
                                mainTask = BaseTask.newInstance("MAIN_TASK", () -> {
                                    try {
                                        if (!init) {
                                            Log.record(TAG, "️🐣跳过执行-未初始化");
                                            return;
                                        }
                                        if (!Config.isLoaded()) {
                                            Log.record(TAG, "️⚙跳过执行-用户模块配置未加载");
                                            return;
                                        }
                                        String threadName = Thread.currentThread().getName();
                                        boolean isAlarmTriggered = threadName.startsWith("AlarmTriggered_");
                                        if (isAlarmTriggered) {
                                            try {
                                                String alarmId = threadName.split("_")[1];
                                                Log.record(TAG, "⏰ 开始新一轮任务 (闹钟触发, ID: " + alarmId + ")");
                                            } catch (Exception e) {
                                                Log.record(TAG, "⏰ 开始新一轮任务 (闹钟触发)");
                                            }
                                        } else {
                                            Log.record(TAG, "▶️ 开始新一轮任务 (手动触发)");
                                        }
                                        long currentTime = System.currentTimeMillis();
                                        // 获取最小执行间隔（2秒）
                                        final long MIN_EXEC_INTERVAL = 2000;
                                        // 计算距离上次执行的时间间隔
                                        long timeSinceLastExec = currentTime - lastExecTime;
                                        // 检查执行条件
                                        boolean isIntervalTooShort = timeSinceLastExec < MIN_EXEC_INTERVAL;
                                        boolean shouldSkipExecution = isIntervalTooShort && !isAlarmTriggered;
                                        // 记录执行间隔信息（无论是否跳过）
                                        Log.record(TAG, "执行间隔: " + timeSinceLastExec + "ms，最小间隔: " + MIN_EXEC_INTERVAL +
                                                "ms，闹钟触发: " + (isAlarmTriggered ? "是" : "否"));
                                        // 只有在非闹钟触发且间隔太短的情况下才跳过执行
                                        if (shouldSkipExecution) {
                                            Log.record(TAG, "⚠️ 执行间隔较短，跳过执行，安排下次执行");
                                            // 使用统一的闹钟调度器
                                            if (alarmScheduler != null) {
                                                alarmScheduler.scheduleDelayedExecution(BaseModel.getCheckInterval().getValue());
                                            } else {
                                                Log.error(TAG, "AlarmScheduler未初始化，无法设置延迟执行");
                                            }
                                            return;
                                        }

                                        // 闹钟触发的执行总是允许的
                                        if (isAlarmTriggered) {
                                            Log.record(TAG, "闹钟触发执行，忽略间隔时间检查");
                                        }
                                        String currentUid = UserMap.getCurrentUid();
                                        String targetUid = HookUtil.INSTANCE.getUserId(appLloadPackageParam.classLoader);
                                        if (targetUid == null || !targetUid.equals(currentUid)) {
                                            Log.record(TAG, "用户切换或为空，重新登录");
                                            reLogin();
                                            return;
                                        }
                                        lastExecTime = currentTime; // 更新最后执行时间
                                        new TaskRunner(List.of(Model.getModelArray())).run(true, ModelTask.TaskExecutionMode.PARALLEL);
                                        scheduleNextExecution(lastExecTime);
                                    } catch (Exception e) {
                                        Log.record(TAG, "❌执行异常");
                                        Log.printStackTrace(TAG, e);
                                    }
                                });
                                registerBroadcastReceiver(appService);
                                dayCalendar = Calendar.getInstance();
                                if (initHandler(true)) {
                                    init = true;
                                }
                            }
                        }

                );
                Log.runtime(TAG, "hook service onCreate successfully");
            } catch (Throwable t) {
                Log.runtime(TAG, "hook service onCreate err");
                Log.printStackTrace(TAG, t);
            }

            try {
                XposedHelpers.findAndHookMethod("android.app.Service", classLoader, "onDestroy",
                        new XC_MethodHook() {
                            @Override
                            protected void afterHookedMethod(MethodHookParam param) {
                                Service service = (Service) param.thisObject;
                                if (!General.CURRENT_USING_SERVICE.equals(service.getClass().getCanonicalName()))
                                    return;
                                Log.record(TAG, "支付宝前台服务被销毁");
                                Notify.updateStatusText("支付宝前台服务被销毁");
                                destroyHandler(true);
                                httpServer.stop();
                                restartByBroadcast();
                            }
                        });
            } catch (Throwable t) {
                Log.runtime(TAG, "hook service onDestroy err");
                Log.printStackTrace(TAG, t);
            }

            HookUtil.INSTANCE.hookOtherService(loadPackageParam);

            hooked = true;
            Log.runtime(TAG, "load success: " + loadPackageParam.packageName);
        }
    }

    /**
     * 设置定时唤醒
     */
    private static void setWakenAtTimeAlarm() {
        try {
            List<String> wakenAtTimeList = BaseModel.getWakenAtTimeList().getValue();
            if (wakenAtTimeList != null && wakenAtTimeList.contains("-1")) {
                Log.record(TAG, "定时唤醒未开启");
                return;
            }
            // 清理旧的唤醒闹钟
            unsetWakenAtTimeAlarm();

            // 设置0点唤醒
                Calendar calendar = Calendar.getInstance();
                calendar.add(Calendar.DAY_OF_MONTH, 1);
                calendar.set(Calendar.HOUR_OF_DAY, 0);
                calendar.set(Calendar.MINUTE, 0);
                calendar.set(Calendar.SECOND, 0);
                calendar.set(Calendar.MILLISECOND, 0);

            if (alarmScheduler != null) {
                if (alarmScheduler.scheduleWakeupAlarm(calendar.getTimeInMillis(), 0, true)) {
                    Log.record(TAG, "⏰ 设置0点定时唤醒");
                }
            } else {
                Log.error(TAG, "AlarmScheduler未初始化，无法设置定时唤醒");
            }

            // 设置自定义时间点唤醒
            if (wakenAtTimeList != null && !wakenAtTimeList.isEmpty()) {
                Calendar nowCalendar = Calendar.getInstance();
                for (int i = 1, len = wakenAtTimeList.size(); i < len; i++) {
                    try {
                        String wakenAtTime = wakenAtTimeList.get(i);
                        Calendar wakenAtTimeCalendar = TimeUtil.getTodayCalendarByTimeStr(wakenAtTime);
                        if (wakenAtTimeCalendar != null && wakenAtTimeCalendar.compareTo(nowCalendar) > 0) {
                            if (alarmScheduler != null) {
                                if (alarmScheduler.scheduleWakeupAlarm(wakenAtTimeCalendar.getTimeInMillis(), i, false)) {
                                    Log.record(TAG, "⏰ 设置定时唤醒: " + wakenAtTime);
                                }
                            } else {
                                Log.error(TAG, "AlarmScheduler未初始化，无法设置定时唤醒");
                            }
                        }
                    } catch (Exception e) {
                        Log.runtime(TAG, "设置自定义唤醒时间失败: " + e.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            Log.runtime(TAG, "setWakenAtTimeAlarm err:");
            Log.printStackTrace(TAG, e);
        }
    }

    /**
     * 取消所有定时唤醒
     */
    private static void unsetWakenAtTimeAlarm() {
        if (alarmScheduler != null) {
            // AlarmScheduler内部没有提供仅取消唤醒闹钟的方法，
            // 但在destroyHandler中会取消所有闹钟，这里可以依赖该逻辑
            // 如果需要精细控制，需要在AlarmScheduler中增加按分类取消的功能
            Log.debug(TAG, "取消定时唤醒将由destroyHandler统一处理");
        }
    }

    private static synchronized Boolean initHandler(Boolean force) {
        try {
            if (init && !force) { // 如果已经初始化且非强制，则跳过
                return true;
            }
            // 只有在重新初始化时才销毁旧的handler
            if (init) {
                destroyHandler(true);
            }
            Model.initAllModel(); //在所有服务启动前装模块配置
            if (service == null) {
                return false;
            }
            if (force) {
                String userId = HookUtil.INSTANCE.getUserId(appLloadPackageParam.classLoader);
                if (userId == null) {
                    Log.record(TAG, "initHandler:用户未登录");
                    Toast.show("initHandler:用户未登录");
                    return false;
                }
                HookUtil.INSTANCE.hookUser(appLloadPackageParam);
                String startMsg = "芝麻粒-TK 开始初始化...";
                Log.record(TAG, startMsg);
                Log.record(TAG, "⚙️模块版本：" + modelVersion);
                Log.record(TAG, "📦应用版本：" + alipayVersion.getVersionString());
                Config.load(userId); //加载配置
                if (!Config.isLoaded()) {
                    Log.record(TAG, "用户模块配置加载失败");
                    Toast.show("用户模块配置加载失败");
                    return false;
                }
                //闹钟权限申请
                if (!PermissionUtil.checkAlarmPermissions()) {
                    Log.record(TAG, "❌ 支付宝无闹钟权限");
                    mainHandler.postDelayed(
                            () -> {
                                if (!PermissionUtil.checkOrRequestAlarmPermissions(appContext)) {
                                    Toast.show("请授予支付宝使用闹钟权限");
                                }
                            },
                            2000);
                    return false;
                }
                // 检查并请求后台运行权限
                if (BaseModel.getBatteryPerm().getValue() && !init && !PermissionUtil.checkBatteryPermissions()) {
                    Log.record(TAG, "支付宝无始终在后台运行权限");
                    mainHandler.postDelayed(
                            () -> {
                                if (!PermissionUtil.checkOrRequestBatteryPermissions(appContext)) {
                                    Toast.show("请授予支付宝始终在后台运行权限");
                                }
                            },
                            2000);
                }
                Notify.start(service);
                // 获取 BaseModel 实例
                BaseModel baseModel = Model.getModel(BaseModel.class);
                if (baseModel == null) {
                    Log.error(TAG, "BaseModel 未找到 初始化失败");
                    Notify.setStatusTextDisabled();
                    return false;
                }
                // 检查 enableField 的值
                if ((0 == baseModel.getEnableField().getValue())) {
                    Log.record(TAG, "❌ 芝麻粒已禁用");
                    Toast.show("❌ 芝麻粒已禁用");
                    Notify.setStatusTextDisabled();
                    return false;
                }
                // 保持唤醒锁，防止设备休眠
                if (BaseModel.getStayAwake().getValue()) {
                    try {
                        PowerManager pm = (PowerManager) service.getSystemService(Context.POWER_SERVICE);
                        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, service.getClass().getName());
                        wakeLock.acquire(10*60*1000L /*10 minutes*/); // 确保唤醒锁在前台服务启动前
                    } catch (Throwable t) {
                        Log.record(TAG, "唤醒锁申请失败:");
                        Log.printStackTrace(t);
                    }
                }

                setWakenAtTimeAlarm();

                if (BaseModel.getNewRpc().getValue()) {
                    rpcBridge = new NewRpcBridge();
                } else {
                    rpcBridge = new OldRpcBridge();
                }
                rpcBridge.load();
                rpcVersion = rpcBridge.getVersion();
                if (BaseModel.getNewRpc().getValue() && BaseModel.getDebugMode().getValue()) {
                    HookUtil.INSTANCE.hookRpcBridgeExtension(appLloadPackageParam, BaseModel.getSendHookData().getValue(), BaseModel.getSendHookDataUrl().getValue());
                    HookUtil.INSTANCE.hookDefaultBridgeCallback(appLloadPackageParam);
                }
                Model.bootAllModel(classLoader);
                Status.load(userId);
                DataCache.INSTANCE.load();
                DataStore.INSTANCE.init(Files.CONFIG_DIR);
                updateDay(userId);
                String successMsg = "芝麻粒-TK 加载成功✨";
                Log.record(successMsg);
                Toast.show(successMsg);
            }
            offline = false;
            execHandler();
            return true;
        } catch (Throwable th) {
            Log.printStackTrace(TAG, "startHandler", th);
            Toast.show("芝麻粒加载失败 🎃");
            return false;
        }
    }

    private static boolean isCrossedMidnight(long currentTime) {
        Calendar lastExecCalendar = Calendar.getInstance();
        lastExecCalendar.setTimeInMillis(lastExecTime);
        Calendar currentCalendar = Calendar.getInstance();
        currentCalendar.setTimeInMillis(currentTime);
        return lastExecCalendar.get(Calendar.DAY_OF_YEAR) != currentCalendar.get(Calendar.DAY_OF_YEAR) ||
                lastExecCalendar.get(Calendar.YEAR) != currentCalendar.get(Calendar.YEAR);
    }

    /**
     * 销毁处理程序
     *
     * @param force 是否强制销毁
     */
    static synchronized void destroyHandler(Boolean force) {
        try {
            if (force) {
                if (service != null) {
                    stopHandler();
                    BaseModel.destroyData();
                    Status.unload();
                    Notify.stop();
                    RpcIntervalLimit.INSTANCE.clearIntervalLimit();
                    Config.unload();
                    UserMap.unload();
                }
                if (wakeLock != null) {
                    wakeLock.release();
                    wakeLock = null;
                }
                if (rpcBridge != null) {
                    rpcVersion = null;
                    rpcBridge.unload();
                    rpcBridge = null;
                }
            } else {
                ModelTask.stopAllTask();
            }
        } catch (Throwable th) {
            Log.runtime(TAG, "stopHandler err:");
            Log.printStackTrace(TAG, th);
        }
    }

    static void execHandler() {
        mainTask.startTask(false);
    }



    /**
     * 检查长时间未执行的情况，如果超过阈值则自动重启
     * 特别针对0点后可能出现的执行中断情况
     */
    private void checkInactiveTime() {
        try {
            if (lastExecTime == 0) {
                return; // 首次执行，跳过检查
            }
            long currentTime = System.currentTimeMillis();
            long inactiveTime = currentTime - lastExecTime;
            // 检查是否经过了0点
            Calendar lastExecCalendar = Calendar.getInstance();
            lastExecCalendar.setTimeInMillis(lastExecTime);
            Calendar currentCalendar = Calendar.getInstance();
            currentCalendar.setTimeInMillis(currentTime);
            boolean crossedMidnight = lastExecCalendar.get(Calendar.DAY_OF_YEAR) != currentCalendar.get(Calendar.DAY_OF_YEAR) ||
                    lastExecCalendar.get(Calendar.YEAR) != currentCalendar.get(Calendar.YEAR);
            // 如果超过最大不活动时间或者跨越了0点但已经过了一段时间
            if (inactiveTime > MAX_INACTIVE_TIME ||
                    (crossedMidnight && currentCalendar.get(Calendar.HOUR_OF_DAY) >= 1)) {
                Log.record(TAG, "⚠️ 检测到长时间未执行(" + (inactiveTime / 60000) + "分钟)，可能跨越0点，尝试重新登录");
                reLogin();
            }
        } catch (Exception e) {
            Log.runtime(TAG, "checkInactiveTime err:" + e.getMessage());
            Log.printStackTrace(TAG, e);
        }
    }

    private static void stopHandler() {
        mainTask.stopTask();
        ModelTask.stopAllTask();
    }

    public static void updateDay(String userId) {
        Calendar nowCalendar = Calendar.getInstance();
        try {
            if (dayCalendar == null) {
                dayCalendar = (Calendar) nowCalendar.clone();
                dayCalendar.set(Calendar.HOUR_OF_DAY, 0);
                dayCalendar.set(Calendar.MINUTE, 0);
                dayCalendar.set(Calendar.SECOND, 0);
                Log.record(TAG, "初始化日期为：" + dayCalendar.get(Calendar.YEAR) + "-" + (dayCalendar.get(Calendar.MONTH) + 1) + "-" + dayCalendar.get(Calendar.DAY_OF_MONTH));
                setWakenAtTimeAlarm();
                return;
            }

            int nowYear = nowCalendar.get(Calendar.YEAR);
            int nowMonth = nowCalendar.get(Calendar.MONTH);
            int nowDay = nowCalendar.get(Calendar.DAY_OF_MONTH);
            if (dayCalendar.get(Calendar.YEAR) != nowYear || dayCalendar.get(Calendar.MONTH) != nowMonth || dayCalendar.get(Calendar.DAY_OF_MONTH) != nowDay) {
                dayCalendar = (Calendar) nowCalendar.clone();
                dayCalendar.set(Calendar.HOUR_OF_DAY, 0);
                dayCalendar.set(Calendar.MINUTE, 0);
                dayCalendar.set(Calendar.SECOND, 0);
                Log.record(TAG, "日期更新为：" + nowYear + "-" + (nowMonth + 1) + "-" + nowDay);
                setWakenAtTimeAlarm();
            }
        } catch (Exception e) {
            Log.printStackTrace(e);
        }

        try {
            Status.save(nowCalendar);
        } catch (Exception e) {
            Log.printStackTrace(e);
        }
    }


    public static void reLoginByBroadcast() {
        try {
            appContext.sendBroadcast(new Intent("com.eg.android.AlipayGphone.sesame.reLogin"));
        } catch (Throwable th) {
            Log.runtime(TAG, "sesame sendBroadcast reLogin err:");
            Log.printStackTrace(TAG, th);
        }
    }

    public static void restartByBroadcast() {
        try {
            appContext.sendBroadcast(new Intent("com.eg.android.AlipayGphone.sesame.restart"));
        } catch (Throwable th) {
            Log.runtime(TAG, "sesame sendBroadcast restart err:");
            Log.printStackTrace(TAG, th);
        }
    }

    @SuppressLint("ObsoleteSdkInt")
    private static int getPendingIntentFlag() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT;
        } else {
            return PendingIntent.FLAG_UPDATE_CURRENT;
        }
    }

    public static Object getMicroApplicationContext() {
        if (microApplicationContextObject == null) {
            try {
                Class < ? > alipayApplicationClass = XposedHelpers.findClass(
                        "com.alipay.mobile.framework.AlipayApplication", classLoader
                );
                Object alipayApplicationInstance = XposedHelpers.callStaticMethod(
                        alipayApplicationClass, "getInstance"
                );
                if (alipayApplicationInstance == null) {
                    return null;
                }
                microApplicationContextObject = XposedHelpers.callMethod(
                        alipayApplicationInstance, "getMicroApplicationContext"
                );
            } catch (Throwable t) {
                Log.printStackTrace(t);
            }
        }
        return microApplicationContextObject;
    }

    public static Object getServiceObject(String service) {
        try {
            return XposedHelpers.callMethod(getMicroApplicationContext(), "findServiceByInterface", service);
        } catch (Throwable th) {
            Log.runtime(TAG, "getUserObject err");
            Log.printStackTrace(TAG, th);
        }
        return null;
    }

    public static Object getUserObject() {
        try {
            return XposedHelpers.callMethod(
                    getServiceObject(
                            XposedHelpers.findClass("com.alipay.mobile.personalbase.service.SocialSdkContactService", classLoader).getName()
                    ),
                    "getMyAccountInfoModelByLocal");
        } catch (Throwable th) {
            Log.runtime(TAG, "getUserObject err");
            Log.printStackTrace(TAG, th);
        }
        return null;
    }

    public static String getUserId() {
        try {
            Object userObject = getUserObject();
            if (userObject != null) {
                return (String) XposedHelpers.getObjectField(userObject, "userId");
            }
        } catch (Throwable th) {
            Log.runtime(TAG, "getUserId err");
            Log.printStackTrace(TAG, th);
        }
        return null;
    }

    public static void reLogin() {
        mainHandler.post(
                () -> {
                    long delayMillis;
                    if (reLoginCount.get() < 5) {
                        delayMillis = reLoginCount.getAndIncrement() * 5000L;
                    } else {
                        delayMillis = Math.max(BaseModel.getCheckInterval().getValue(), 180_000);
                    }
                    
                    // 使用统一的闹钟调度器
                    if (alarmScheduler != null) {
                        alarmScheduler.scheduleDelayedExecution(delayMillis);
                    } else {
                        Log.error(TAG, "AlarmScheduler未初始化，无法设置延迟执行");
                    }
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setClassName(General.PACKAGE_NAME, General.CURRENT_USING_ACTIVITY);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    offline = true;
                    appContext.startActivity(intent);
                });
    }


    static class AlipayBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            try {
                String action = intent.getAction();
                Log.runtime(TAG, "Alipay got Broadcast " + action + " intent:" + intent);
                if (action != null) {
                    switch (action) {
                        case "com.eg.android.AlipayGphone.sesame.restart":
                            initHandler(true);
                            break;
                        case "com.eg.android.AlipayGphone.sesame.execute":
                            initHandler(false);
                            break;
                        case "com.eg.android.AlipayGphone.sesame.reLogin":
                            reLogin();
                            break;
                        case "com.eg.android.AlipayGphone.sesame.status":
                            // 状态查询处理
                            Log.system(TAG, "收到状态查询广播");
                            break;
                        case "com.eg.android.AlipayGphone.sesame.rpctest":
                            try {
                                String method = intent.getStringExtra("method");
                                String data = intent.getStringExtra("data");
                                String type = intent.getStringExtra("type");
                                Log.runtime(TAG, "收到RPC测试请求 - Method: " + method + ", Type: " + type);
                                DebugRpc rpcInstance = new DebugRpc();
                                rpcInstance.start(method, data, type);
                            } catch (Throwable th) {
                                Log.runtime(TAG, "sesame 测试RPC请求失败:");
                                Log.printStackTrace(TAG, th);
                            }
                            break;
                        default:
                            // 处理闹钟相关的广播
                            if (alarmScheduler != null) {
                                alarmScheduler.handleAlarmTrigger();
                                int requestCode = intent.getIntExtra("request_code", -1);
                                alarmScheduler.consumeAlarm(requestCode);
                            }
                            break;
                    }
                }
            } catch (Throwable t) {
                Log.printStackTrace(TAG, "AlipayBroadcastReceiver.onReceive err:", t);
            }
        }
    }

    /**
     * 注册广播接收器以监听支付宝相关动作。
     *
     * @param context 应用程序上下文
     */
    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    // 忽略Lint关于注册广播接收器时未指定导出属性的警告
    void registerBroadcastReceiver(Context context) {
        //创建一个IntentFilter实例，用于过滤出我们需要捕获的广播
        try {
            IntentFilter intentFilter = getIntentFilter();
            // 根据Android SDK版本注册广播接收器
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                // 在Android 13及以上版本，注册广播接收器并指定其可以被其他应用发送的广播触发
                context.registerReceiver(new AlipayBroadcastReceiver(), intentFilter, Context.RECEIVER_EXPORTED);
            } else {
                // 在Android 13以下版本，注册广播接收器
                context.registerReceiver(new AlipayBroadcastReceiver(), intentFilter);
            }
            // 记录成功注册广播接收器的日志
            Log.runtime(TAG, "hook registerBroadcastReceiver successfully");
        } catch (Throwable th) {
            // 记录注册广播接收器失败的日志
            Log.runtime(TAG, "hook registerBroadcastReceiver err:");
            // 打印异常堆栈信息
            Log.printStackTrace(TAG, th);
        }
    }

    @NonNull
    private static IntentFilter getIntentFilter() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("com.eg.android.AlipayGphone.sesame.restart"); // 重启支付宝服务的动作
        intentFilter.addAction("com.eg.android.AlipayGphone.sesame.execute"); // 执行特定命令的动作
        intentFilter.addAction("com.eg.android.AlipayGphone.sesame.reLogin"); // 重新登录支付宝的动作
        intentFilter.addAction("com.eg.android.AlipayGphone.sesame.status"); // 查询支付宝状态的动作
        intentFilter.addAction("com.eg.android.AlipayGphone.sesame.rpctest"); // 调试RPC的动作
        return intentFilter;
    }
}