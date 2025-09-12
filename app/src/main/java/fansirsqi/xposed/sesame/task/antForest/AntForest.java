package fansirsqi.xposed.sesame.task.antForest;

import static fansirsqi.xposed.sesame.task.antForest.ForestUtil.hasBombCard;
import static fansirsqi.xposed.sesame.task.antForest.ForestUtil.hasShield;

import android.annotation.SuppressLint;

import androidx.annotation.NonNull;

import com.fasterxml.jackson.core.type.TypeReference;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.CountDownLatch;

import de.robv.android.xposed.XposedHelpers;
import fansirsqi.xposed.sesame.data.RuntimeInfo;
import fansirsqi.xposed.sesame.data.Status;
import fansirsqi.xposed.sesame.entity.AlipayUser;
import fansirsqi.xposed.sesame.entity.CollectEnergyEntity;
import fansirsqi.xposed.sesame.entity.KVMap;
import fansirsqi.xposed.sesame.entity.OtherEntityProvider;
import fansirsqi.xposed.sesame.entity.RpcEntity;
import fansirsqi.xposed.sesame.entity.VitalityStore;
import fansirsqi.xposed.sesame.hook.RequestManager;
import fansirsqi.xposed.sesame.hook.Toast;
import fansirsqi.xposed.sesame.hook.rpc.intervallimit.FixedOrRangeIntervalLimit;
import fansirsqi.xposed.sesame.hook.rpc.intervallimit.IntervalLimit;
import fansirsqi.xposed.sesame.hook.rpc.intervallimit.RpcIntervalLimit;
import fansirsqi.xposed.sesame.model.BaseModel;
import fansirsqi.xposed.sesame.model.ModelFields;
import fansirsqi.xposed.sesame.model.ModelGroup;
import fansirsqi.xposed.sesame.model.modelFieldExt.BooleanModelField;
import fansirsqi.xposed.sesame.model.modelFieldExt.ChoiceModelField;
import fansirsqi.xposed.sesame.model.modelFieldExt.IntegerModelField;
import fansirsqi.xposed.sesame.model.modelFieldExt.ListModelField;
import fansirsqi.xposed.sesame.model.modelFieldExt.SelectAndCountModelField;
import fansirsqi.xposed.sesame.model.modelFieldExt.SelectModelField;
import fansirsqi.xposed.sesame.model.modelFieldExt.StringModelField;
import fansirsqi.xposed.sesame.newutil.DataStore;
import fansirsqi.xposed.sesame.task.ModelTask;
import fansirsqi.xposed.sesame.task.TaskCommon;
import fansirsqi.xposed.sesame.task.TaskStatus;
import fansirsqi.xposed.sesame.ui.ObjReference;
import fansirsqi.xposed.sesame.util.Average;
import fansirsqi.xposed.sesame.util.GlobalThreadPools;
import fansirsqi.xposed.sesame.util.ListUtil;
import fansirsqi.xposed.sesame.util.Log;
import fansirsqi.xposed.sesame.util.maps.UserMap;
import fansirsqi.xposed.sesame.util.Notify;
import fansirsqi.xposed.sesame.util.RandomUtil;
import fansirsqi.xposed.sesame.util.ResChecker;
import fansirsqi.xposed.sesame.util.TimeUtil;
import fansirsqi.xposed.sesame.util.TimeFormatter;
import lombok.Getter;
/// lzw add begin
import fansirsqi.xposed.sesame.model.modelFieldExt.PriorityModelField;
import fansirsqi.xposed.sesame.util.TimeCounter;

/**
 * 蚂蚁森林V2
 */
public class AntForest extends ModelTask {
    public static final String TAG = AntForest.class.getSimpleName();

    private static final Average offsetTimeMath = new Average(5);

    private final AtomicInteger taskCount = new AtomicInteger(0);
    private String selfId;
    private Integer tryCountInt;
    private Integer retryIntervalInt;
    private IntervalLimit collectIntervalEntity;
    private IntervalLimit doubleCollectIntervalEntity;
    /**
     * 双击卡结束时间
     */
    private volatile long doubleEndTime = 0;
    /**
     * 隐身卡结束时间
     */
    private volatile long stealthEndTime = 0;
    /**
     * 保护罩结束时间
     */
    private volatile long shieldEndTime = 0;
    /**
     * 炸弹卡结束时间
     */
    private volatile long energyBombCardEndTime = 0;
    /**
     * 1.1倍能量卡结束时间
     */
    private volatile long robExpandCardEndTime = 0;
    /// lzw add begin
    private volatile boolean _is_monday = false;
    /// lzw add end
    private final Average delayTimeMath = new Average(5);
    private final ObjReference<Long> collectEnergyLockLimit = new ObjReference<>(0L);
    private final Object doubleCardLockObj = new Object();



    // 保持向后兼容
    /** 保护罩续写阈值（HHmm），例如 2355 表示 23小时55分 */
    private static final int SHIELD_RENEW_THRESHOLD_HHMM = 2359;
    private PriorityModelField collectEnergy;
    private BooleanModelField pkEnergy; // PK能量
    private BooleanModelField energyRain;
    private IntegerModelField advanceTime;
    private IntegerModelField tryCount;
    private IntegerModelField retryInterval;
    private SelectModelField dontCollectList;
    private BooleanModelField collectWateringBubble;
    private BooleanModelField batchRobEnergy;
    private BooleanModelField balanceNetworkDelay;
    private BooleanModelField closeWhackMole;
    private PriorityModelField collectProp;
    private StringModelField queryInterval;
    private StringModelField collectInterval;
    private StringModelField doubleCollectInterval;
    private ChoiceModelField doubleCard; // 双击卡
    private ListModelField.ListJoinCommaToStringModelField doubleCardTime; // 双击卡时间
    @Getter
    private IntegerModelField doubleCountLimit; // 双击卡次数限制
    private BooleanModelField doubleCardConstant; // 双击卡永动机
    private ChoiceModelField stealthCard; // 隐身卡
    private BooleanModelField stealthCardConstant; // 隐身卡永动机
    private ChoiceModelField shieldCard; // 保护罩
    private BooleanModelField shieldCardConstant;// 限时保护永动机
    private ChoiceModelField helpFriendCollectType;
    private SelectModelField helpFriendCollectList;
    /// lzw add begin
    private SelectModelField alternativeAccountList;
    // 显示背包内容
    private BooleanModelField showBagList;
    /// lzw add end
    private SelectAndCountModelField vitalityExchangeList;
    private IntegerModelField returnWater33;
    private IntegerModelField returnWater18;
    private IntegerModelField returnWater10;
    private PriorityModelField receiveForestTaskAward;
    private SelectAndCountModelField waterFriendList;
    private IntegerModelField waterFriendCount;
    private BooleanModelField notifyFriend;
    public static SelectModelField giveEnergyRainList; //能量雨赠送列表
    private PriorityModelField vitalityExchange;
    private PriorityModelField userPatrol;
    private BooleanModelField collectGiftBox;
    private PriorityModelField medicalHealth; //医疗健康开关
    public static SelectModelField medicalHealthOption; //医疗健康选项
    private PriorityModelField ForestMarket;
    private PriorityModelField combineAnimalPiece;
    private PriorityModelField consumeAnimalProp;
    private SelectModelField whoYouWantToGiveTo;
    private BooleanModelField dailyCheckIn;//青春特权签到
    private ChoiceModelField bubbleBoostCard;//加速卡
    private BooleanModelField youthPrivilege;//青春特权 森林道具
    public static SelectModelField ecoLifeOption;
    private PriorityModelField ecoLife;
    private PriorityModelField giveProp;

    private ChoiceModelField robExpandCard;//1.1倍能量卡
    private ListModelField robExpandCardTime; //1.1倍能量卡时间
    private IntegerModelField cycleinterval;      // 循环间隔


    /**
     * 异常返回检测开关
     **/
    private static Boolean errorWait = false;
    public static BooleanModelField ecoLifeOpen;
    private BooleanModelField energyRainChance;
    /**
     * 能量炸弹卡
     */
    private ChoiceModelField energyBombCardType;

    private final Map<String, String> cacheCollectedMap = new ConcurrentHashMap<>();
    /**
     * 空森林缓存，用于记录在本轮任务中已经确认没有能量的好友。
     * 在每轮蚂蚁森林任务开始时清空（见run方法finally块）。
     * “一轮任务”通常指由"执行间隔"触发的一次完整的好友遍历。
     */
    private final Map<String, Long> emptyForestCache = new ConcurrentHashMap<>();
    /**
     * 跳过用户缓存，用于记录有保护罩或其他需要跳过的用户
     * Key: 用户ID，Value: 跳过原因（如"baohuzhao"表示有保护罩）
     */
    private final Map<String, String> skipUsersCache = new ConcurrentHashMap<>();
    /**
     * 加速器定时
     */
    private ListModelField.ListJoinCommaToStringModelField bubbleBoostTime;

    private PriorityModelField forestChouChouLe;//森林抽抽乐
    private static boolean canConsumeAnimalProp;
    private static int totalCollected = 0;
    private static final int totalHelpCollected = 0;
    private static final int totalWatered = 0;

    private final Map<String, AtomicInteger> forestTaskTryCount = new ConcurrentHashMap<>();

    @Getter
    private Set<String> dsontCollectMap = new HashSet<>();
    ArrayList<String> emojiList = new ArrayList<>(Arrays.asList(
            "🍅", "🍓", "🥓", "🍂", "🍚", "🌰", "🟢", "🌴",
            "🥗", "🧀", "🥩", "🍍", "🌶️", "🍲", "🍆", "🥕",
            "✨", "🍑", "🍘", "🍀", "🥞", "🍈", "🥝", "🧅",
            "🌵", "🌾", "🥜", "🍇", "🌭", "🥑", "🥐", "🥖",
            "🍊", "🌽", "🍉", "🍖", "🍄", "🥚", "🥙", "🥦",
            "🍌", "🍱", "🍏", "🍎", "🌲", "🌿", "🍁", "🍒",
            "🥔", "🌯", "🌱", "🍐", "🍞", "🍳", "🍙", "🍋",
            "🍗", "🌮", "🍃", "🥘", "🥒", "🧄", "🍠", "🥥"
    ));
    private final Random random = new Random();

    @Override
    public String getName() {
        return "森林";
    }

    @Override
    public ModelGroup getGroup() {
        return ModelGroup.FOREST;
    }

    @Override
    public String getIcon() {
        return "AntForest.png";
    }

    private static final int MAX_BATCH_SIZE = 6;

    @SuppressWarnings("unused")
    public interface applyPropType {
        int CLOSE = 0;
        int ALL = 1;
        int ONLY_LIMIT_TIME = 2;
        String[] nickNames = {"关闭", "所有道具", "限时道具"};
    }

    public interface HelpFriendCollectType {
        int NONE = 0;
        int HELP = 1;
        int DONT_HELP = 2;
        String[] nickNames = {"关闭", "选中复活", "选中不复活"};
    }

    @Override
    public ModelFields getFields() {
        ModelFields modelFields = new ModelFields();
        modelFields.addField(collectEnergy = new PriorityModelField("collectEnergy", "收集能量 | 开关", priorityType.CLOSE, priorityType.nickNames));
        modelFields.addField(batchRobEnergy = new BooleanModelField("batchRobEnergy", "一键收取 | 开关", false));
        modelFields.addField(pkEnergy = new BooleanModelField("pkEnergy", "Pk榜收取 | 开关", false));
        modelFields.addField(closeWhackMole = new BooleanModelField("closeWhackMole", "自动关闭6秒拼手速 | 开关", false));
        modelFields.addField(energyRain = new BooleanModelField("energyRain", "能量雨 | 开关", false));
        modelFields.addField(dontCollectList = new SelectModelField("dontCollectList", "不收能量 | 配置列表", new LinkedHashSet<>(), AlipayUser::getList));
        modelFields.addField(giveEnergyRainList = new SelectModelField("giveEnergyRainList", "赠送能量雨 | 配置列表", new LinkedHashSet<>(), AlipayUser::getList));
        modelFields.addField(energyRainChance = new BooleanModelField("energyRainChance", "兑换使用能量雨次卡 | 开关", false));
        modelFields.addField(collectWateringBubble = new BooleanModelField("collectWateringBubble", "收取浇水金球 | 开关", false));
        modelFields.addField(doubleCard = new ChoiceModelField("doubleCard", "双击卡开关 | 消耗类型", applyPropType.CLOSE, applyPropType.nickNames));
        modelFields.addField(doubleCountLimit = new IntegerModelField("doubleCountLimit", "双击卡 | 使用次数", 6));
        modelFields.addField(doubleCardTime = new ListModelField.ListJoinCommaToStringModelField("doubleCardTime", "双击卡 | 使用时间/范围", ListUtil.newArrayList(
                "0700", "0730", "1200", "1230", "1700", "1730", "2000", "2030", "2359")));
        modelFields.addField(doubleCardConstant = new BooleanModelField("DoubleCardConstant", "限时双击永动机 | 开关", false));

        modelFields.addField(bubbleBoostCard = new ChoiceModelField("bubbleBoostCard", "加速器开关 | 消耗类型", applyPropType.CLOSE, applyPropType.nickNames));
        modelFields.addField(bubbleBoostTime = new ListModelField.ListJoinCommaToStringModelField("bubbleBoostTime", "加速器 | 使用时间/不能范围", ListUtil.newArrayList(
                "0030,0630", "0700", "0730", "1200", "1230", "1700", "1730", "2000", "2030", "2359")));

        modelFields.addField(shieldCard = new ChoiceModelField("shieldCard", "保护罩开关 | 消耗类型", applyPropType.CLOSE, applyPropType.nickNames));
        modelFields.addField(shieldCardConstant = new BooleanModelField("shieldCardConstant", "限时保护永动机 | 开关", false));

        modelFields.addField(energyBombCardType = new ChoiceModelField("energyBombCardType", "炸弹卡开关 | 消耗类型", applyPropType.CLOSE,
                applyPropType.nickNames, "若开启了保护罩，则不会使用炸弹卡"));

        modelFields.addField(robExpandCard = new ChoiceModelField("robExpandCard", "1.1倍能量卡开关 | 消耗类型", applyPropType.CLOSE, applyPropType.nickNames));
        modelFields.addField(robExpandCardTime = new ListModelField.ListJoinCommaToStringModelField("robExpandCardTime", "1.1倍能量卡 | 使用时间/不能范围",
                ListUtil.newArrayList("0700", "0730", "1200", "1230", "1700", "1730", "2000", "2030", "2359")));

        modelFields.addField(stealthCard = new ChoiceModelField("stealthCard", "隐身卡开关 | 消耗类型", applyPropType.CLOSE, applyPropType.nickNames));
        modelFields.addField(stealthCardConstant = new BooleanModelField("stealthCardConstant", "限时隐身永动机 | 开关", false));

        modelFields.addField(returnWater10 = new IntegerModelField("returnWater10", "返水 | 10克需收能量(关闭:0)", 0));
        modelFields.addField(returnWater18 = new IntegerModelField("returnWater18", "返水 | 18克需收能量(关闭:0)", 0));
        modelFields.addField(returnWater33 = new IntegerModelField("returnWater33", "返水 | 33克需收能量(关闭:0)", 0));
        modelFields.addField(waterFriendList = new SelectAndCountModelField("waterFriendList", "浇水 | 好友列表", new LinkedHashMap<>(), AlipayUser::getList, "设置浇水次数"));
        modelFields.addField(waterFriendCount = new IntegerModelField("waterFriendCount", "浇水 | 克数(10 18 33 66)", 66));
        modelFields.addField(notifyFriend = new BooleanModelField("notifyFriend", "浇水 | 通知好友", false));
        modelFields.addField(giveProp = new PriorityModelField("giveProp", "赠送道具", priorityType.PRIORITY_2, priorityType.nickNames));
        modelFields.addField(whoYouWantToGiveTo = new SelectModelField("whoYouWantToGiveTo", "赠送 | 道具", new LinkedHashSet<>(), AlipayUser::getList, "所有可赠送的道具将全部赠"));
        modelFields.addField(collectProp = new PriorityModelField("collectProp", "收集道具", priorityType.PRIORITY_2, priorityType.nickNames));
        modelFields.addField(helpFriendCollectType = new ChoiceModelField("helpFriendCollectType", "复活能量 | 选项", HelpFriendCollectType.NONE, HelpFriendCollectType.nickNames));
        modelFields.addField(helpFriendCollectList = new SelectModelField("helpFriendCollectList", "复活能量 | 好友列表", new LinkedHashSet<>(), AlipayUser::getList));
/// lzw add begin
        modelFields.addField(alternativeAccountList = new SelectModelField("alternativeAccountList", "小号列表", new LinkedHashSet<>(), AlipayUser::getList));
/// lzw add end
        modelFields.addField(vitalityExchange = new PriorityModelField("vitalityExchange", "活力值 | 兑换开关", priorityType.PRIORITY_2, priorityType.nickNames));
        modelFields.addField(vitalityExchangeList = new SelectAndCountModelField("vitalityExchangeList", "活力值 | 兑换列表", new LinkedHashMap<>(), VitalityStore::getList, "兑换次数"));
        modelFields.addField(userPatrol = new PriorityModelField("userPatrol", "保护地巡护", priorityType.PRIORITY_2, priorityType.nickNames));
        modelFields.addField(combineAnimalPiece = new PriorityModelField("combineAnimalPiece", "合成动物碎片", priorityType.PRIORITY_2, priorityType.nickNames));
        modelFields.addField(consumeAnimalProp = new PriorityModelField("consumeAnimalProp", "派遣动物伙伴", priorityType.PRIORITY_2, priorityType.nickNames));
        modelFields.addField(receiveForestTaskAward = new PriorityModelField("receiveForestTaskAward", "森林任务", priorityType.PRIORITY_2, priorityType.nickNames));

        modelFields.addField(forestChouChouLe = new PriorityModelField("forestChouChouLe", "森林寻宝任务", priorityType.PRIORITY_2, priorityType.nickNames));

        modelFields.addField(collectGiftBox = new BooleanModelField("collectGiftBox", "领取礼盒", false));

        modelFields.addField(medicalHealth = new PriorityModelField("medicalHealth", "健康医疗任务 | 开关", priorityType.PRIORITY_2, priorityType.nickNames));
        modelFields.addField(medicalHealthOption = new SelectModelField("medicalHealthOption", "健康医疗 | 选项", new LinkedHashSet<>(), OtherEntityProvider.listHealthcareOptions(), "医疗健康需要先完成一次医疗打卡"));

        modelFields.addField(ForestMarket = new PriorityModelField("ForestMarket", "森林集市", priorityType.PRIORITY_2, priorityType.nickNames));
        modelFields.addField(youthPrivilege = new BooleanModelField("youthPrivilege", "青春特权 | 森林道具", false));
        modelFields.addField(dailyCheckIn = new BooleanModelField("studentCheckIn", "青春特权 | 签到红包", false));
        modelFields.addField(ecoLife = new PriorityModelField("ecoLife", "绿色行动 | 开关", priorityType.PRIORITY_2, priorityType.nickNames));
        modelFields.addField(ecoLifeOpen = new BooleanModelField("ecoLifeOpen", "绿色任务 |  自动开通", false));
        modelFields.addField(ecoLifeOption = new SelectModelField("ecoLifeOption", "绿色行动 | 选项", new LinkedHashSet<>(), OtherEntityProvider.listEcoLifeOptions(), "光盘行动需要先完成一次光盘打卡"));

        modelFields.addField(queryInterval = new StringModelField("queryInterval", "查询间隔(毫秒或毫秒范围)", "1000-2000"));
        modelFields.addField(collectInterval = new StringModelField("collectInterval", "收取间隔(毫秒或毫秒范围)", "1000-1500"));
        modelFields.addField(doubleCollectInterval = new StringModelField("doubleCollectInterval", "双击间隔(毫秒或毫秒范围)", "800-2400"));
        modelFields.addField(balanceNetworkDelay = new BooleanModelField("balanceNetworkDelay", "平衡网络延迟", true));
        modelFields.addField(advanceTime = new IntegerModelField("advanceTime", "提前时间(毫秒)", 0, Integer.MIN_VALUE, 500));
        modelFields.addField(tryCount = new IntegerModelField("tryCount", "尝试收取(次数)", 1, 0, 5));
        modelFields.addField(retryInterval = new IntegerModelField("retryInterval", "重试间隔(毫秒)", 1200, 0, 10000));
        modelFields.addField(cycleinterval = new IntegerModelField("cycleinterval", "循环间隔(毫秒)", 5000, 0, 10000));
        modelFields.addField(showBagList = new BooleanModelField("showBagList", "显示背包内容", false));
        return modelFields;
    }

    @Override
    public Boolean check() {
        long currentTime = System.currentTimeMillis();

        // -----------------------------
        // 先更新时间状态，保证 IS_ENERGY_TIME 正确
        // -----------------------------
        TaskCommon.update();

        // 1️⃣ 异常等待状态
        long forestPauseTime = RuntimeInfo.getInstance().getLong(RuntimeInfo.RuntimeInfoKey.ForestPauseTime);
        if (forestPauseTime > currentTime) {
            Log.record(getName() + "任务-异常等待中，暂不执行检测！");
            return false;
        }

        // 2️⃣ 模块休眠时间
        if (TaskCommon.IS_MODULE_SLEEP_TIME) {
            Log.record(TAG, "💤 模块休眠时间【" + BaseModel.getModelSleepTime().getValue() + "】停止执行" + getName() + "任务！");
            return false;
        }

        // -----------------------------
        // 3️⃣ 只收能量时间段判断
        // -----------------------------
        Calendar now = Calendar.getInstance();
        int hour = now.get(Calendar.HOUR_OF_DAY);
        int minute = now.get(Calendar.MINUTE);

        boolean isEnergyTime = TaskCommon.IS_ENERGY_TIME || hour == 7 && minute < 30;

        if (isEnergyTime) {
            Log.record(TAG, "⏸ 当前为只收能量时间【07:00-07:30】，开始循环收取自己、好友和PK好友的能量");
            while (true) {
                // 每次循环更新状态
                TaskCommon.update();
                // 如果不在能量时间段，退出循环
                now = Calendar.getInstance();
                hour = now.get(Calendar.HOUR_OF_DAY);
                minute = now.get(Calendar.MINUTE);
                if (!(TaskCommon.IS_ENERGY_TIME || hour == 7 && minute < 30)) {
                    Log.record(TAG, "当前不在只收能量时间段，退出循环");
                    break;
                }
                // 收取自己能量
                JSONObject selfHomeObj = querySelfHome();
                if (selfHomeObj != null) {
                    collectEnergy(UserMap.getCurrentUid(), selfHomeObj, "self");
                }
                GlobalThreadPools.execute(this::collectEnergyByTakeLook); //找能量
                GlobalThreadPools.execute(this::collectFriendEnergy);  // 好友能量收取（异步）
                GlobalThreadPools.execute(this::collectPKEnergy);      // PK好友能量（异步）
                // 循环间隔
                    int sleepMillis = cycleinterval.getValue();
                    Log.record(TAG, "只收能量时间循环间隔: " + sleepMillis + "毫秒");
                    GlobalThreadPools.sleep(sleepMillis);
            }

            Log.record(TAG, "只收能量时间循环结束");
            return false; // 只收能量期间不执行正常任务
        }
        return true;
    }

    @Override
    public int getPriority() {
        return 1;
    }

    /**
     * 创建区间限制对象
     *
     * @param intervalStr 区间字符串，如 "1000-2000"
     * @param defaultMin 默认最小值
     * @param defaultMax 默认最大值
     * @param description 描述，用于日志
     * @return 区间限制对象
     */
    private FixedOrRangeIntervalLimit createSafeIntervalLimit(String intervalStr, int defaultMin, int defaultMax, String description) {
        // 记录原始输入值
        Log.record(TAG, description + "原始设置值: [" + intervalStr + "]");

        // 使用自定义区间限制类，处理所有边界情况
        FixedOrRangeIntervalLimit limit = new FixedOrRangeIntervalLimit(intervalStr, defaultMin, defaultMax);
        Log.record(TAG, description + "成功创建区间限制");
        return limit;
    }

    @Override
    public void boot(ClassLoader classLoader) {
        super.boot(classLoader);



        // 安全创建各种区间限制
        FixedOrRangeIntervalLimit queryIntervalLimit = createSafeIntervalLimit(
                queryInterval.getValue(), 10, 10000, "查询间隔");

        // 添加RPC间隔限制
        RpcIntervalLimit.INSTANCE.addIntervalLimit("alipay.antforest.forest.h5.queryHomePage", queryIntervalLimit);
        RpcIntervalLimit.INSTANCE.addIntervalLimit("alipay.antforest.forest.h5.queryFriendHomePage", queryIntervalLimit);
        RpcIntervalLimit.INSTANCE.addIntervalLimit("alipay.antmember.forest.h5.collectEnergy", 300);
        RpcIntervalLimit.INSTANCE.addIntervalLimit("alipay.antmember.forest.h5.queryEnergyRanking", 300);
        RpcIntervalLimit.INSTANCE.addIntervalLimit("alipay.antforest.forest.h5.fillUserRobFlag", 500);

        // 设置其他参数
        tryCountInt = tryCount.getValue();
        retryIntervalInt = retryInterval.getValue();
        Integer advanceTimeInt = advanceTime.getValue();


        dsontCollectMap = dontCollectList.getValue();

        // 创建收取间隔实体
        collectIntervalEntity = createSafeIntervalLimit(
                collectInterval.getValue(), 50, 10000, "收取间隔");

        // 创建双击收取间隔实体
        doubleCollectIntervalEntity = createSafeIntervalLimit(
                doubleCollectInterval.getValue(), 10, 5000, "双击间隔");
        delayTimeMath.clear();
        
        
        AntForestRpcCall.init();
    }

    @Override
    public void run() {
        try {
            // 每次运行时检查并更新计数器
            checkAndUpdateCounters();
            // 午夜强制任务
            if (isMidnight()) {
                JSONObject selfHomeObj = querySelfHome();
                if (selfHomeObj != null) {
                    collectEnergy(UserMap.getCurrentUid(), selfHomeObj, "self");  // 异步收取自己
                }
                // 先尝试使用找能量功能快速定位有能量的好友（异步）
                GlobalThreadPools.execute(this::collectEnergyByTakeLook); //找能量
                GlobalThreadPools.execute(this::collectFriendEnergy);  // 好友能量收取（异步）
                GlobalThreadPools.execute(this::collectPKEnergy);      // PK好友能量（异步）
                Log.record(TAG, "午夜任务刷新，强制执行收取PK好友能量和好友能量");
            }

            errorWait = false;

            // 计数器和时间记录
            _is_monday = true;
            TimeCounter tc = new TimeCounter(TAG);

            if (showBagList.getValue()) showBag();

            Log.record(TAG, "执行开始-蚂蚁" + getName());
            taskCount.set(0);
            selfId = UserMap.getCurrentUid();

            // -------------------------------
            // 自己使用道具
            // -------------------------------
            usePropBeforeCollectEnergy(selfId);
            tc.countDebug("使用自己道具卡");

            // -------------------------------
            // 收PK好友能量
            // -------------------------------
            Log.runtime(TAG, "🚀 异步执行PK好友能量收取");
            GlobalThreadPools.execute(this::collectPKEnergy);  // 好友道具在 collectFriendEnergy 内会自动处理
            tc.countDebug("收PK好友能量（异步）");

            // -------------------------------
            // 收自己能量
            // -------------------------------
            JSONObject selfHomeObj = querySelfHome();
            tc.countDebug("获取自己主页对象信息");
            if (selfHomeObj != null) {
                collectEnergy(UserMap.getCurrentUid(), selfHomeObj, "self"); // 异步收取自己的能量
                tc.countDebug("收取自己的能量（异步）");
            } else {
                Log.error(TAG, "获取自己主页信息失败，跳过能量收取");
                tc.countDebug("跳过自己的能量收取（主页获取失败）");
            }

            // -------------------------------
            // 收好友能量
            // -------------------------------
            // 先尝试使用找能量功能快速定位有能量的好友（异步）
            Log.runtime(TAG, "🚀 异步执行找能量功能");
            GlobalThreadPools.execute(this::collectEnergyByTakeLook);
            tc.countDebug("找能量收取（异步）");

            // 然后执行传统的好友排行榜收取（异步）
            Log.runtime(TAG, "🚀 异步执行好友能量收取");
            GlobalThreadPools.execute(this::collectFriendEnergy);  // 内部会自动调用 usePropBeforeCollectEnergy(userId, false)
            tc.countDebug("收取好友能量（异步）");

            // -------------------------------
            // 后续任务流程
            // -------------------------------
            if (selfHomeObj != null) {
                if (collectWateringBubble.getValue()) {
                    wateringBubbles(selfHomeObj);
                    tc.countDebug("收取浇水金球");
                }
                if (getRunCents() >= collectProp.getValue()) {
                    givenProps(selfHomeObj);
                    tc.countDebug("收取道具");
                }
                if (getRunCents() >= userPatrol.getValue()) {
                    queryUserPatrol();
                    tc.countDebug("动物巡护任务");
                }
                if (canConsumeAnimalProp && getRunCents() >= consumeAnimalProp.getValue()) {
                    queryAndConsumeAnimal();
                    tc.countDebug("森林巡护");
                } else {
                    Log.record("已经有动物伙伴在巡护森林~");
                }

                handleUserProps(selfHomeObj);
                tc.countDebug("收取动物派遣能量");

                if (getRunCents() >= combineAnimalPiece.getValue()) {
                    queryAnimalAndPiece();
                    tc.countDebug("合成动物碎片");
                }

                if (getRunCents() >= receiveForestTaskAward.getValue()) {
                    receiveTaskAward();
                    tc.countDebug("森林任务");
                }
                if (getRunCents() >= ecoLife.getValue()) {
                    EcoLife.ecoLife();
                    tc.countDebug("绿色行动");
                }

                waterFriends();
                tc.countDebug("给好友浇水");

                if (getRunCents() >= giveProp.getValue()) {
                    giveProp();
                    tc.countDebug("赠送道具");
                }

                if (getRunCents() >= vitalityExchange.getValue()) {
                    handleVitalityExchange();
                    tc.countDebug("活力值兑换");
                }

                if (energyRain.getValue()) {
                    EnergyRain.energyRain();
                    if (energyRainChance.getValue()) {
                        useEnergyRainChanceCard();
                        tc.countDebug("使用能量雨卡");
                    }
                    tc.countDebug("能量雨");
                }

                if (getRunCents() >= ForestMarket.getValue()) {
                    GreenLife.ForestMarket("GREEN_LIFE");
                    GreenLife.ForestMarket("ANTFOREST");
                    tc.countDebug("森林集市");
                }

                if (getRunCents() >= medicalHealth.getValue()) {
                    if (medicalHealthOption.getValue().contains("FEEDS")) {
                        Healthcare.queryForestEnergy("FEEDS");
                        tc.countDebug("绿色医疗");
                    }
                    if (medicalHealthOption.getValue().contains("BILL")) {
                        Healthcare.queryForestEnergy("BILL");
                        tc.countDebug("电子小票");
                    }
                }

                //青春特权森林道具领取
                if (youthPrivilege.getValue()) {
                    Privilege.INSTANCE.youthPrivilege();
                }

                if (dailyCheckIn.getValue()) {
                    Privilege.INSTANCE.studentSignInRedEnvelope();
                }

                if (getRunCents() >= forestChouChouLe.getValue()) {
                    ForestChouChouLe chouChouLe = new ForestChouChouLe();
                    chouChouLe.chouChouLe();
                    tc.countDebug("抽抽乐");
                }

                tc.stop();
            }

        } catch (Throwable t) {
            Log.printStackTrace(TAG, "执行蚂蚁森林任务时发生错误: ", t);
        } finally {
            try {
                synchronized (AntForest.this) {
                    int count = taskCount.get();
                    if (count > 0) {
                        AntForest.this.wait(TimeUnit.MINUTES.toMillis(30));
                        count = taskCount.get();
                    }
                    if (count > 0) Log.record(TAG, "执行超时-蚂蚁森林");
                    else if (count == 0) Log.record(TAG, "执行结束-蚂蚁森林");
                    else Log.record(TAG, "执行完成-蚂蚁森林");
                }
            } catch (InterruptedException ie) {
                Log.record(TAG, "执行中断-蚂蚁森林");
            }
            cacheCollectedMap.clear();
            // 清空本轮的空森林缓存，以便下一轮（如下次"执行间隔"到达）重新检查所有好友
            emptyForestCache.clear();
            // 清空跳过用户缓存，下一轮重新检测保护罩状态
            skipUsersCache.clear();
            // 清空好友主页缓存
            String str_totalCollected = "本次总 收:" + totalCollected + "g 帮:" + totalHelpCollected + "g 浇:" + totalWatered + "g";
            Notify.updateLastExecText(str_totalCollected);
        }
    }

    /**
     * 每日重置
     */
    private void checkAndUpdateCounters() {
        long currentTime = System.currentTimeMillis();
        long midnight = getMidnightTime(); // 计算当前日期的午夜时间戳

        if (currentTime >= midnight) {
            // 如果时间已经过了午夜，重置计数器
            resetTaskCounters();
            Log.record(TAG, "午夜重置计数器");
        }
    }

    // 判断当前时间是否已经过午夜
    private boolean isMidnight() {
        long currentTime = System.currentTimeMillis();
        long midnightTime = getMidnightTime();
        return currentTime >= midnightTime;
    }

    // 获取午夜时间戳
    private long getMidnightTime() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis();
    }

    // 重置任务计数器（你需要根据具体任务的计数器来调整）
    private void resetTaskCounters() {
        taskCount.set(0); // 重置任务计数
        Log.record(TAG, "任务计数器已重置");
    }

    /**
     * 定义一个 处理器接口
     */
    @FunctionalInterface
    private interface JsonArrayHandler {
        void handle(JSONArray array);
    }

    private void processJsonArray(JSONObject initialObj, String arrayKey, JsonArrayHandler handler) {
        boolean hasMore;
        JSONObject currentObj = initialObj;
        do {
            JSONArray jsonArray = currentObj.optJSONArray(arrayKey);
            if (jsonArray != null && jsonArray.length() > 0) {
                handler.handle(jsonArray);
                // 判断是否还有更多数据（比如返回满20个）
                hasMore = jsonArray.length() >= 20;
            } else {
                hasMore = false;
            }
            if (hasMore) {
                GlobalThreadPools.sleep(2000L); // 防止请求过快被限制
                currentObj = querySelfHome(); // 获取下一页数据
            }
        } while (hasMore);
    }

    private void wateringBubbles(JSONObject selfHomeObj) {
        processJsonArray(selfHomeObj, "wateringBubbles", this::collectWateringBubbles);
    }

    private void givenProps(JSONObject selfHomeObj) {
        processJsonArray(selfHomeObj, "givenProps", this::collectGivenProps);
    }


    /**
     * 收取回赠能量，好友浇水金秋，好友复活能量
     *
     * @param wateringBubbles 包含不同类型金球的对象数组
     */
    private void collectWateringBubbles(JSONArray wateringBubbles) {
        for (int i = 0; i < wateringBubbles.length(); i++) {
            try {
                JSONObject wateringBubble = wateringBubbles.getJSONObject(i);
                String bizType = wateringBubble.getString("bizType");
                switch (bizType) {
                    case "jiaoshui":
                        collectWater(wateringBubble);
                        break;
                    case "fuhuo":
                        collectRebornEnergy();
                        break;
                    case "baohuhuizeng":
                        collectReturnEnergy(wateringBubble);
                        break;
                    default:
                        Log.record(TAG, "未知bizType: " + bizType);
                        continue;
                }
                GlobalThreadPools.sleep(1000L);
            } catch (JSONException e) {
                Log.record(TAG, "浇水金球JSON解析错误: " + e.getMessage());
            } catch (RuntimeException e) {
                Log.record(TAG, "浇水金球处理异常: " + e.getMessage());
            }
        }
    }

    private void collectWater(JSONObject wateringBubble) {
        try {
            long id = wateringBubble.getLong("id");
            String response = AntForestRpcCall.collectEnergy("jiaoshui", selfId, id);
            processCollectResult(response, "收取金球🍯浇水");
        } catch (JSONException e) {
            Log.record(TAG, "收取浇水JSON解析错误: " + e.getMessage());
        }
    }

    private void collectRebornEnergy() {
        try {
            String response = AntForestRpcCall.collectRebornEnergy();
            processCollectResult(response, "收取金球🍯复活");
        } catch (RuntimeException e) {
            Log.record(TAG, "收取金球运行时异常: " + e.getMessage());
        }
    }

    private void collectReturnEnergy(JSONObject wateringBubble) {
        try {
            String friendId = wateringBubble.getString("userId");
            long id = wateringBubble.getLong("id");
            String response = AntForestRpcCall.collectEnergy("baohuhuizeng", selfId, id);
            processCollectResult(response, "收取金球🍯[" + UserMap.getMaskName(friendId) + "]复活回赠");
        } catch (JSONException e) {
            Log.record(TAG, "收取金球回赠JSON解析错误: " + e.getMessage());
        }
    }

    /**
     * 处理金球-浇水、收取结果
     *
     * @param response       收取结果
     * @param successMessage 成功提示信息
     */
    private void processCollectResult(String response, String successMessage) {
        try {
            JSONObject joEnergy = new JSONObject(response);
            if (ResChecker.checkRes(TAG + "收集能量失败:", joEnergy)) {
                JSONArray bubbles = joEnergy.getJSONArray("bubbles");
                if (bubbles.length() > 0) {
                    int collected = bubbles.getJSONObject(0).getInt("collectedEnergy");
                    if (collected > 0) {
                        String msg = successMessage + "[" + collected + "g]";
                        Log.forest(msg);
                        Toast.show(msg);
                    } else {
                        Log.record(successMessage + "失败");
                    }
                } else {
                    Log.record(successMessage + "失败: 未找到金球信息");
                }
            } else {
                Log.record(successMessage + "失败:" + joEnergy.getString("resultDesc"));
                Log.runtime(response);
            }
        } catch (JSONException e) {
            Log.runtime(TAG, "JSON解析错误: " + e.getMessage());
        } catch (Exception e) {
            Log.runtime(TAG, "处理收能量结果错误: " + e.getMessage());
        }
    }

    /**
     * 领取道具
     *
     * @param givenProps 给的道具
     */
    private void collectGivenProps(JSONArray givenProps) {
        try {
            for (int i = 0; i < givenProps.length(); i++) {
                JSONObject jo = givenProps.getJSONObject(i);
                String giveConfigId = jo.getString("giveConfigId");
                String giveId = jo.getString("giveId");
                JSONObject propConfig = jo.getJSONObject("propConfig");
                String propName = propConfig.getString("propName");
                try {
                    String response = AntForestRpcCall.collectProp(giveConfigId, giveId);
                    JSONObject responseObj = new JSONObject(response);
                    if (ResChecker.checkRes(TAG + "领取道具失败:", responseObj)) {
                        String str = "领取道具🎭[" + propName + "]";
                        Log.forest(str);
                        Toast.show(str);
                    } else {
                        Log.record(TAG, "领取道具🎭[" + propName + "]失败:" + responseObj.getString("resultDesc"));
                        Log.runtime(response);
                    }
                } catch (Exception e) {
                    Log.record(TAG, "领取道具时发生错误: " + e.getMessage());
                    Log.printStackTrace(e);
                }
                GlobalThreadPools.sleep(1000L);
            }
        } catch (JSONException e) {
            Log.record(TAG, "givenProps JSON解析错误: " + e.getMessage());
            Log.printStackTrace(e);
        }
    }

    /**
     * 处理用户派遣道具, 如果用户有派遣道具，则收取派遣动物滴能量
     *
     * @param selfHomeObj 用户主页信息的JSON对象
     */
    private void handleUserProps(JSONObject selfHomeObj) {
        try {
            JSONArray usingUserProps = selfHomeObj.optJSONArray("usingUserPropsNew");
            if (usingUserProps == null || usingUserProps.length() == 0) {
                return; // 如果没有使用中的用户道具，直接返回
            }
//            Log.runtime(TAG, "尝试遍历使用中的道具:" + usingUserProps);
            for (int i = 0; i < usingUserProps.length(); i++) {
                JSONObject jo = usingUserProps.getJSONObject(i);
                if (!"animal".equals(jo.getString("propGroup"))) {
                    continue; // 如果当前道具不是动物类型，跳过
                }
                JSONObject extInfo = new JSONObject(jo.getString("extInfo"));
                if (extInfo.optBoolean("isCollected")) {
                    Log.runtime(TAG, "动物派遣能量已被收取");
                    continue; // 如果动物能量已经被收取，跳过
                }
                canConsumeAnimalProp = false; // 设置标志位，表示不可再使用动物道具
                String propId = jo.getString("propId");
                String propType = jo.getString("propType");
                String shortDay = extInfo.getString("shortDay");
                String animalName = extInfo.getJSONObject("animal").getString("name");
                String response = AntForestRpcCall.collectAnimalRobEnergy(propId, propType, shortDay);
                JSONObject responseObj = new JSONObject(response);
                if (ResChecker.checkRes(TAG + "收取动物派遣能量失败:", responseObj)) {
                    int energy = extInfo.optInt("energy", 0);
                    totalCollected += energy;
                    String str = "收取[" + animalName + "]派遣能量🦩[" + energy + "g]";
                    Toast.show(str);
                    Log.forest(str);
                } else {
                    Log.record(TAG, "收取动物能量失败: " + responseObj.getString("resultDesc"));
                    Log.runtime(response);
                }
                GlobalThreadPools.sleep(300L);
                break; // 收取到一个动物能量后跳出循环
            }
        } catch (JSONException e) {
            Log.printStackTrace(e);
        } catch (Exception e) {
            Log.runtime(TAG, "handleUserProps err");
            Log.printStackTrace(e);
        }
    }

    /**
     * 给好友浇水
     */
    private void waterFriends() {
        try {
            Map<String, Integer> friendMap = waterFriendList.getValue();
            boolean notify = notifyFriend.getValue(); // 获取通知开关状态

            for (Map.Entry<String, Integer> friendEntry : friendMap.entrySet()) {
                String uid = friendEntry.getKey();
                if (selfId.equals(uid)) {
                    continue;
                }
                Integer waterCount = friendEntry.getValue();
                if (waterCount == null || waterCount <= 0) {
                    continue;
                }
                waterCount = Math.min(waterCount, 3);

                if (Status.canWaterFriendToday(uid, waterCount)) {
                    try {
                        String response = AntForestRpcCall.queryFriendHomePage(uid, null);
                        JSONObject jo = new JSONObject(response);
                        if (ResChecker.checkRes(TAG, jo)) {
                            String bizNo = jo.getString("bizNo");

                            // ✅ 关键改动：传入通知开关
                            KVMap<Integer, Boolean> waterCountKVNode = returnFriendWater(
                                    uid, bizNo, waterCount, waterFriendCount.getValue(), notify
                            );

                            int actualWaterCount = waterCountKVNode.getKey();
                            if (actualWaterCount > 0) {
                                Status.waterFriendToday(uid, actualWaterCount);
                            }
                            if (Boolean.FALSE.equals(waterCountKVNode.getValue())) {
                                break;
                            }
                        } else {
                            Log.record(jo.getString("resultDesc"));
                        }
                    } catch (JSONException e) {
                        Log.runtime(TAG, "waterFriends JSON解析错误: " + e.getMessage());
                    } catch (Throwable t) {
                        Log.printStackTrace(TAG, t);
                    }
                }
            }
        } catch (Exception e) {
            Log.record(TAG, "未知错误: " + e.getMessage());
            Log.printStackTrace(TAG, e);
        }
    }

    private void handleVitalityExchange() {
        try {
//            JSONObject bag = getBag();

            Vitality.initVitality("SC_ASSETS");
            Map<String, Integer> exchangeList = vitalityExchangeList.getValue();
//            Map<String, Integer> maxLimitList = vitalityExchangeMaxList.getValue();
            for (Map.Entry<String, Integer> entry : exchangeList.entrySet()) {
                String skuId = entry.getKey();
                Integer count = entry.getValue();
                if (count == null || count <= 0) {
                    Log.record(TAG, "无效的count值: skuId=" + skuId + ", count=" + count);
                    continue;
                }
                // 处理活力值兑换
                while (Status.canVitalityExchangeToday(skuId, count)) {
                    if (!Vitality.handleVitalityExchange(skuId)) {
                        Log.record(TAG, "活力值兑换失败: " + VitalityStore.getNameById(skuId));
                        break;
                    }
                    GlobalThreadPools.sleep(5000L);
                }
            }
        } catch (Throwable t) {
            handleException("handleVitalityExchange", t);
        }
    }

    private void notifyMain() {
        if (taskCount.decrementAndGet() < 1) {
            synchronized (AntForest.this) {
                AntForest.this.notifyAll();
            }
        }
    }

    /**
     * 获取自己主页对象信息
     *
     * @return 用户的主页信息，如果发生错误则返回null。
     */
    private JSONObject querySelfHome() {
        JSONObject userHomeObj = null;
        try {
            long start = System.currentTimeMillis();
            String response = AntForestRpcCall.queryHomePage();
            if (response.trim().isEmpty()) {
                Log.error(TAG, "获取自己主页信息失败：响应为空"+response);
                return null;
            }
            
            userHomeObj = new JSONObject(response);
            
            // 检查响应是否成功
            if (!ResChecker.checkRes(TAG + "查询自己主页失败:", userHomeObj)) {
                Log.error(TAG, "查询自己主页失败: " + userHomeObj.optString("resultDesc", "未知错误"));
                return null;
            }
            
            updateSelfHomePage(userHomeObj);
            long end = System.currentTimeMillis();
            // 安全获取服务器时间，如果没有则使用当前时间
            long serverTime = userHomeObj.optLong("now", System.currentTimeMillis());
            int offsetTime = offsetTimeMath.nextInteger((int) ((start + end) / 2 - serverTime));
            Log.runtime(TAG, "服务器时间：" + serverTime + "，本地与服务器时间差：" + offsetTime);
        } catch (Throwable t) {
            Log.printStackTrace(TAG, "查询自己主页异常", t);
        }
        return userHomeObj;
    }

    /**
     * 更新好友主页信息
     *
     * @param userId 好友ID
     * @return 更新后的好友主页信息，如果发生错误则返回null。
     */

    private JSONObject queryFriendHome(String userId, String fromAct) {
        JSONObject friendHomeObj = null;
        try {
            long start = System.currentTimeMillis();
            String response = AntForestRpcCall.queryFriendHomePage(userId, fromAct);
            if (response.trim().isEmpty()) {
                Log.error(TAG, "获取好友主页信息失败：响应为空, userId: " + UserMap.getMaskName(userId)+response);
                return null;
            }
            
            friendHomeObj = new JSONObject(response);
            // 检查响应是否成功
            if (!ResChecker.checkRes(TAG + "查询好友主页失败:", friendHomeObj)) {
                Log.error(TAG, "查询好友主页失败: " + friendHomeObj.optString("resultDesc", "未知错误"));
                return null;
            }
            long end = System.currentTimeMillis();
            // 安全获取服务器时间，如果没有则使用当前时间
            long serverTime = friendHomeObj.optLong("now", System.currentTimeMillis());
            int offsetTime = offsetTimeMath.nextInteger((int) ((start + end) / 2 - serverTime));
            Log.runtime(TAG, "服务器时间：" + serverTime + "，本地与服务器时间差：" + offsetTime);
        } catch (Throwable t) {
            Log.printStackTrace(TAG, "查询好友主页异常, userId: " + UserMap.getMaskName(userId), t);
        }
        return friendHomeObj; // 返回用户主页对象
    }


    
    /**
     * 格式化时间差为人性化的字符串（保持向后兼容）
     * @param milliseconds 时差毫秒
     */
    private String formatTimeDifference(long milliseconds) {
        return TimeFormatter.formatTimeDifference(milliseconds);
    }

    /**
     * 收集能量前，是否执行拼手速操作
     *
     * @return 首次收取后用户的能量信息，如果发生错误则返回null。
     */
    private JSONObject collectSelfEnergy() {
        try {

            JSONObject selfHomeObj = querySelfHome();
            if (selfHomeObj != null) {
                if (closeWhackMole.getValue()) {
                    JSONObject propertiesObject = selfHomeObj.optJSONObject("properties");
                    if (propertiesObject != null) {
                        // 如果用户主页的属性中标记了"whackMole"
                        if (Objects.equals("Y", propertiesObject.optString("whackMoleEntry"))) {
                            // 尝试关闭"6秒拼手速"功能
                            boolean success = WhackMole.closeWhackMole();
                            Log.record(success ? "6秒拼手速关闭成功" : "6秒拼手速关闭失败");
                        }
                    }
                }
                String nextAction = selfHomeObj.optString("nextAction");
                if ("WhackMole".equalsIgnoreCase(nextAction)) {
                    Log.record(TAG, "检测到6秒拼手速强制弹窗，先执行拼手速");
                    WhackMole.startWhackMole();
                }
                return collectEnergy(UserMap.getCurrentUid(), selfHomeObj, "self");
            }
        } catch (Throwable t) {
            Log.printStackTrace(t);
        }
        return null;
    }


    /**
     * 收取用户的蚂蚁森林能量。
     *
     * @param userId      用户ID
     * @param userHomeObj 用户主页的JSON对象，包含用户的蚂蚁森林信息
     * @return 更新后的用户主页JSON对象，如果发生异常返回null
     */
    private JSONObject collectEnergy(String userId, JSONObject userHomeObj, String fromTag) {
        try {
            // 1. 检查接口返回是否成功
             if (!ResChecker.checkRes(TAG + "载入用户主页失败:", userHomeObj)) {
                 Log.debug(TAG, "载入失败: " + userHomeObj.optString("resultDesc", "未知错误"));
                 return userHomeObj;
             }
             long serverTime = userHomeObj.optLong("now", System.currentTimeMillis());
            boolean isSelf = Objects.equals(userId, selfId);

            if (cacheCollectedMap.containsKey(userId)) {
                return userHomeObj;
            }
            String userName = getAndCacheUserName(userId, userHomeObj, fromTag);
            String bizType = "GREEN";

            // 3. 判断是否允许收取能量
            if ((collectEnergy.getValue() <= 0) || dsontCollectMap.contains(userId)) {
                Log.debug(TAG, "[" + userName + "] 不允许收取能量，跳过");
                return userHomeObj;
            }
            // 4. 获取所有可收集的能量球
            List<Long> availableBubbles = new ArrayList<>();
            extractBubbleInfo(userHomeObj, serverTime, availableBubbles, userId);
            // 如果没有任何能量球（可收），则标记为空林并直接返回
            if (availableBubbles.isEmpty()) {
                emptyForestCache.put(userId, System.currentTimeMillis());
                return userHomeObj;
            }
            // 检查是否有能量罩保护（影响当前收取）
            boolean hasProtection = false;
            if (!isSelf) {
                if (hasShield(userHomeObj, serverTime)) {
                    hasProtection = true;
                    Log.record(TAG, "[" + userName + "]被能量罩❤️保护着哟，跳过收取");
                }
                if (hasBombCard(userHomeObj, serverTime)) {
                    hasProtection = true;
                    Log.record(TAG, "[" + userName + "]开着炸弹卡💣，跳过收取");
                }
            }
            // 7. 只有没有保护时才收集当前可用能量
            if (!hasProtection) {
                collectVivaEnergy(userId, userHomeObj, availableBubbles, bizType, fromTag);
            }
            return userHomeObj;
        } catch (JSONException | NullPointerException e) {
            Log.printStackTrace(TAG, "collectUserEnergy JSON解析错误", e);
            return null;
        } catch (Throwable t) {
            Log.printStackTrace(TAG, "collectUserEnergy 出现异常", t);
            return null;
        }
    }



    /**
     * 提取能量球状态
     *
     * @param userHomeObj      用户主页的JSON对象
     * @param serverTime       服务器时间
     * @param availableBubbles 可收集的能量球ID列表
     * @param userId          用户ID
     * @throws JSONException JSON解析异常
     */

    private void extractBubbleInfo(JSONObject userHomeObj, long serverTime, List<Long> availableBubbles, String userId) throws JSONException {
        if (!userHomeObj.has("bubbles")) return;
        JSONArray jaBubbles = userHomeObj.getJSONArray("bubbles");
        if (jaBubbles.length() == 0) return;
        
        for (int i = 0; i < jaBubbles.length(); i++) {
            JSONObject bubble = jaBubbles.getJSONObject(i);
            long bubbleId = bubble.getLong("id");
            String statusStr = bubble.getString("collectStatus");
            CollectStatus status = CollectStatus.valueOf(statusStr);
            
            // 只收集可收取的能量球，跳过等待成熟的
            if (status == CollectStatus.AVAILABLE) {
                availableBubbles.add(bubbleId);
            }
        }
    }



    /**
     * 批量或逐一收取能量
     *
     * @param userId      用户ID
     * @param userHomeObj 用户主页的JSON对象
     * @param bubbleIds   能量球ID列表
     * @param bizType     业务类型
     * @param fromTag     收取来源标识
     */
    private void collectVivaEnergy(String userId, JSONObject userHomeObj, List<Long> bubbleIds, String bizType, String fromTag) throws JSONException {
        if (bubbleIds.isEmpty()) return;
        boolean isBatchCollect = batchRobEnergy.getValue();
        if (isBatchCollect) {
            for (int i = 0; i < bubbleIds.size(); i += MAX_BATCH_SIZE) {
                List<Long> subList = bubbleIds.subList(i, Math.min(i + MAX_BATCH_SIZE, bubbleIds.size()));
                collectEnergy(new CollectEnergyEntity(userId, userHomeObj, AntForestRpcCall.batchEnergyRpcEntity(bizType, userId, subList), fromTag));
            }
        } else {
            for (Long id : bubbleIds) {
                collectEnergy(new CollectEnergyEntity(userId, userHomeObj, AntForestRpcCall.energyRpcEntity(bizType, userId, id), fromTag));
            }
        }
    }

    /**
     * 函数式接口，用于提供RPC调用
     */
    @FunctionalInterface
    private interface RpcSupplier<T> {
        T get() throws Exception;
    }

    /**
     * 函数式接口，用于对JSON对象进行断言
     */
    @FunctionalInterface
    private interface JsonPredicate<T> {
        boolean test(T t) throws Exception;
    }

    private void collectRankings(String rankingName, RpcSupplier<String> rpcCall, String jsonArrayKey, String flag, JsonPredicate<JSONObject> preCondition) {
        try {
            TimeCounter tc = new TimeCounter(TAG);
            JSONObject rankingObject = new JSONObject(rpcCall.get());
            if (!ResChecker.checkRes(TAG + "获取" + rankingName + "失败:", rankingObject)) {
                Log.error(TAG, "获取" + rankingName + "失败: " + rankingObject.optString("resultDesc"));
                return;
            }
            tc.countDebug("获取" + rankingName);
            if (preCondition != null && !preCondition.test(rankingObject)) {
                return;
            }
            // 处理前20个
            collectUserEnergy(rankingObject, flag);
            tc.countDebug("处理" + rankingName + "靠前的好友");
            // 分批并行处理后续的
            JSONArray totalDatas = rankingObject.optJSONArray(jsonArrayKey);
            if (totalDatas == null || totalDatas.length() <= 20) {
                Log.record(TAG, rankingName + "没有更多的好友需要处理，跳过");
                return;
            }
            List<String> idList = new ArrayList<>();
            int batchSize = 30;
            int remainingSize = totalDatas.length() - 20;
            int batches = (remainingSize + batchSize - 1) / batchSize;
            CountDownLatch latch = new CountDownLatch(batches);
            for (int pos = 20; pos < totalDatas.length(); pos++) {
                JSONObject friend = totalDatas.getJSONObject(pos);
                String userId = friend.getString("userId");
                if (Objects.equals(userId, selfId)) continue;
                idList.add(userId);
                if (idList.size() == batchSize) {
                    final List<String> batch = new ArrayList<>(idList);
                    GlobalThreadPools.execute(() -> processLastdEnergy(batch, flag, latch));
                    idList.clear();
                }
            }
            if (!idList.isEmpty()) {
                GlobalThreadPools.execute(() -> processLastdEnergy(idList, flag, latch));
            }
            latch.await();
            tc.countDebug("分批处理" + rankingName + "其他好友");
            Log.record(TAG, "收取" + rankingName + "能量完成！");
        } catch (Exception e) {
            Log.error(TAG, "处理" + rankingName + "时发生异常");
            Log.printStackTrace(TAG, "collectRankings 异常", e);
        }
    }

    private void collectPKEnergy() {
        collectRankings("PK排行榜",
                AntForestRpcCall::queryTopEnergyChallengeRanking,
                "totalData",
                "pk",
                pkObject -> {
                    if (!pkObject.getString("rankMemberStatus").equals("JOIN")) {
                        Log.runtime(TAG, "未加入PK排行榜,跳过,尝试关闭");
                        pkEnergy.setValue(false);
                        return false;
                    }
                    return true;
                });
    }


    /**
     * 使用找能量功能收取好友能量
     * 这是一个更高效的收取方式，可以直接找到有能量的好友
     */
    private void collectEnergyByTakeLook() {
        try {
            TimeCounter tc = new TimeCounter(TAG);
            int foundCount = 0;
            int maxAttempts = 10; // 减少到10次，避免过度循环
            int consecutiveEmpty = 0; // 连续空结果计数
            Log.record(TAG, "开始使用找能量功能收取好友能量");
            for (int attempt = 1; attempt <= maxAttempts; attempt ++) {
                // 构建跳过用户列表（有保护罩的用户）
                JSONObject skipUsers = buildSkipUsersMap();
                // 调用找能量接口
                String takeLookResponse = AntForestRpcCall.takeLook(skipUsers);
                JSONObject takeLookResult = new JSONObject(takeLookResponse);
                if (!ResChecker.checkRes(TAG + "找能量失败:", takeLookResult)) {
                    Log.error(TAG, "找能量失败: " + takeLookResult.optString("resultDesc"));
                    break;
                }
                // 获取找到的好友ID
                String friendId = takeLookResult.optString("friendId");
                if (friendId.isEmpty() || Objects.equals(friendId, selfId)) {
                    Log.record(TAG, "第" + attempt + "次找能量没有发现新好友，继续尝试:"+skipUsers);
                    continue;
                }
                  // 查询好友主页并收取能量
                  JSONObject friendHomeObj = queryFriendHome(friendId, "TAKE_LOOK_FRIEND");
                  if (friendHomeObj != null) {
                    foundCount++;
                    String friendName = UserMap.getMaskName(friendId);
                    if (friendName == null || friendName.isEmpty() || friendName.equals(friendId)) {
                        // 如果UserMap没有返回有效的用户名，使用通用的获取用户名方法
                        friendName = getAndCacheUserName(friendId,friendHomeObj,null);
                    }
                      long currentTime = System.currentTimeMillis();
                      // 检查是否有保护，如果有则添加到跳过列表
                      boolean hasShieldProtection = hasShield(friendHomeObj, currentTime);
                      boolean hasBombProtection = hasBombCard(friendHomeObj, currentTime);
                      if (hasShieldProtection || hasBombProtection) {
                          String protectionType = hasShieldProtection ? "保护罩" : "炸弹卡";
                          addToSkipUsers(friendId);
                          Log.record(TAG, "找能量第" + attempt + "次发现好友[" + friendName + "]有" + protectionType + "，跳过收取");
                      } else {
                          // 没有保护才进行收取处理
                          collectEnergy(friendId, friendHomeObj, "takeLook");
                      }
                      // 优化间隔：找到好友时减少等待时间，提高效率
                    GlobalThreadPools.sleep(1500L);
                    consecutiveEmpty = 0; // 重置连续空结果计数
                } else {
                    consecutiveEmpty++;
                    // 检查friendId是否为null或空，给出更详细的信息
                      Log.record(TAG, "找能量第" + attempt + "次：发现好友但是自己，跳过");
                      // 连续2次空结果就提前结束，避免浪费时间
                    if (consecutiveEmpty >= 2) {
                        Log.record(TAG, "连续" + consecutiveEmpty + "次无结果，提前结束找能量");
                        break;
                    }
                }
            }
            tc.countDebug("找能量收取完成");
            Log.record(TAG, "找能量功能完成，共发现 " + foundCount + " 个好友");
        } catch (Exception e) {
            Log.error(TAG, "找能量过程中发生异常");
            Log.printStackTrace(TAG, "collectEnergyByTakeLook 异常", e);
        }
    }
    
    /**
     * 构建跳过用户映射表
     * @return 包含需要跳过用户的JSON对象
     */
    private JSONObject buildSkipUsersMap() {
        JSONObject skipUsers = new JSONObject();
        try {
            // 从缓存中获取有保护罩的用户列表
            for (Map.Entry<String, String> entry : skipUsersCache.entrySet()) {
                String userId = entry.getKey();
                String reason = entry.getValue();
                skipUsers.put(userId, reason);
            }
            skipUsers.length();
        } catch (Exception e) {
            Log.printStackTrace(TAG, "构建跳过用户列表失败", e);
        }
        return skipUsers;
    }
    
    /**
     * 将用户添加到跳过列表（内存缓存）
     *
     * @param userId 用户ID
     */
    private void addToSkipUsers(String userId) {
        try {
            skipUsersCache.put(userId, "baohuzhao");
        } catch (Exception e) {
            Log.printStackTrace(TAG, "添加跳过用户失败", e);
        }
    }

    private void collectFriendEnergy() {
        collectRankings("好友排行榜",
                AntForestRpcCall::queryFriendsEnergyRanking,
                "totalDatas",
                "",
                null);
    }

    /**
     * 收取排名靠后的能量
     *
     * @param userIds 用户id列表
     */
    private void processLastdEnergy(List<String> userIds, String flag, CountDownLatch latch) {
        try {
            if (errorWait) return;
            String jsonStr;
            if (flag.equals("pk")) {
                jsonStr = AntForestRpcCall.fillUserRobFlag(new JSONArray(userIds), true);
            } else {
                jsonStr = AntForestRpcCall.fillUserRobFlag(new JSONArray(userIds));
            }
            JSONObject batchObj = new JSONObject(jsonStr);
            JSONArray friendList = batchObj.optJSONArray("friendRanking");
            if (friendList == null) return;
            CountDownLatch innerLatch = new CountDownLatch(friendList.length());
            for (int i = 0; i < friendList.length(); i++) {
                JSONObject friendObj = friendList.getJSONObject(i);
                GlobalThreadPools.execute(() -> processEnergy(friendObj, flag, innerLatch));
            }
            innerLatch.await();
        } catch (JSONException e) {
            Log.printStackTrace(TAG, "解析批量好友数据失败", e);
        } catch (Exception e) {
            Log.printStackTrace(TAG, "处理批量好友出错", e);
        } finally {
            latch.countDown();
        }
    }

    /**
     * 处理单个好友 - 收能量
     * 最终判断是否收能量步骤
     *
     * @param obj 好友/PK好友 的JSON对象
     */
    private void processEnergy(JSONObject obj, String flag, CountDownLatch latch) {
        try {
            processEnergyInternal(obj, flag);
        } catch (Exception e) {
            Log.printStackTrace(TAG, "处理好友异常", e);
        } finally {
            latch.countDown();
        }
    }

    /**
     * 处理单个好友的核心逻辑（无锁）
     *
     * @param obj  好友/PK好友 的JSON对象
     * @param flag 标记是普通好友还是PK好友
     */
    private void processEnergyInternal(JSONObject obj, String flag) throws Exception {
        if (errorWait) return;
        String userId = obj.getString("userId");
        if (Objects.equals(userId, selfId)) return; // 跳过自己
        String userName = obj.optString("displayName", UserMap.getMaskName(userId));
        if (emptyForestCache.containsKey(userId)) { //本轮已知为空的树林
            return;
        }

        boolean isPk = "pk".equals(flag);
        if (isPk) {
            userName = "PK榜好友|" + userName;
        }
        //  Log.record(TAG, "  processEnergy 开始处理用户: [" + userName + "], 类型: " + (isPk ? "PK" : "普通"));
        if (isPk) {
            boolean needCollectEnergy = (collectEnergy.getValue() > 0) && pkEnergy.getValue();
            if (!needCollectEnergy) {
                Log.record(TAG, "    PK好友: [" + userName + "], 不满足收取条件，跳过");
                return;
            }
            collectEnergy(userId, queryFriendHome(userId, "PKContest"), "pk");
        } else { // 普通好友
            boolean needCollectEnergy = (collectEnergy.getValue() > 0) && !dsontCollectMap.contains(userId);
            boolean needHelpProtect = helpFriendCollectType.getValue() != HelpFriendCollectType.NONE && obj.optBoolean("canProtectBubble") && Status.canProtectBubbleToday(selfId);
            boolean needCollectGiftBox = collectGiftBox.getValue() && obj.optBoolean("canCollectGiftBox");
            if (!needCollectEnergy && !needHelpProtect && !needCollectGiftBox) {
                Log.record(TAG, "    普通好友: [" + userName + "], 所有条件不满足，跳过");
                return;
            }
            JSONObject userHomeObj = null;
            // 只要开启了收能量，就进去看看，以便添加蹲点
            if (needCollectEnergy) {
                // 即使排行榜信息显示没有可收能量，也进去检查，以便添加蹲点任务
                userHomeObj = collectEnergy(userId, queryFriendHome(userId, null), "friend");
            }
            if (needHelpProtect) {
                boolean isProtected = isIsProtected(userId);
/// lzw add end
                if (isProtected) {
                    if (userHomeObj == null) {
                        userHomeObj = queryFriendHome(userId, null);
                    }
                    if (userHomeObj != null) {
                        protectFriendEnergy(userHomeObj);
                    }
                }
            }
            // 尝试领取礼物盒
            if (needCollectGiftBox) {
                if (userHomeObj == null) {
                    userHomeObj = queryFriendHome(userId, null);
                }
                if (userHomeObj != null) {
                    collectGiftBox(userHomeObj);
                }
            }
        }
    }

    private boolean isIsProtected(String userId) {
        boolean isProtected;
        // Log.forest("is_monday:"+_is_monday);
        if(_is_monday) {
            isProtected = alternativeAccountList.getValue().contains(userId);
        } else {
            isProtected = helpFriendCollectList.getValue().contains(userId);
            if (helpFriendCollectType.getValue() != HelpFriendCollectType.HELP) {
                isProtected = !isProtected;
            }
        }
        return isProtected;
    }
    /// lzw add end
    /**
     * 收取排名靠前好友能量
     *
     * @param friendsObject 好友列表的JSON对象
     */
    private void collectUserEnergy(JSONObject friendsObject, String flag) {
        try {
            if (errorWait) return;
            JSONArray friendRanking = friendsObject.optJSONArray("friendRanking");
            if (friendRanking == null) {
                Log.runtime(TAG, "无好友数据(friendRanking)可处理");
                return;
            }
            for (int i = 0; i < friendRanking.length(); i++) {
                final JSONObject finalFriendObj = friendRanking.getJSONObject(i);
                GlobalThreadPools.execute(() -> {
                    try {
                        processEnergyInternal(finalFriendObj, flag);
                    } catch (Exception e) {
                        Log.printStackTrace(TAG, "处理好友(top)异常", e);
                    }
                });
            }
        } catch (JSONException e) {
            Log.printStackTrace(TAG, "解析好友排行榜子项失败", e);
        } catch (Exception e) {
            Log.printStackTrace(TAG, "处理好友列表异常", e);
        }
    }

    private void collectGiftBox(JSONObject userHomeObj) {
        try {
            JSONObject giftBoxInfo = userHomeObj.optJSONObject("giftBoxInfo");
            JSONObject userEnergy = userHomeObj.optJSONObject("userEnergy");
            String userId = userEnergy == null ? UserMap.getCurrentUid() : userEnergy.optString("userId");
            if (giftBoxInfo != null) {
                JSONArray giftBoxList = giftBoxInfo.optJSONArray("giftBoxList");
                if (giftBoxList != null && giftBoxList.length() > 0) {
                    for (int ii = 0; ii < giftBoxList.length(); ii++) {
                        try {
                            JSONObject giftBox = giftBoxList.getJSONObject(ii);
                            String giftBoxId = giftBox.getString("giftBoxId");
                            String title = giftBox.getString("title");
                            JSONObject giftBoxResult = new JSONObject(AntForestRpcCall.collectFriendGiftBox(giftBoxId, userId));
                            if (!ResChecker.checkRes(TAG + "领取好友礼盒失败:", giftBoxResult)) {
                                Log.record(giftBoxResult.getString("resultDesc"));
                                Log.runtime(giftBoxResult.toString());
                                continue;
                            }
                            int energy = giftBoxResult.optInt("energy", 0);
                            Log.forest("礼盒能量🎁[" + UserMap.getMaskName(userId) + "-" + title + "]#" + energy + "g");
                        } catch (Throwable t) {
                            Log.printStackTrace(t);
                            break;
                        } finally {
                            GlobalThreadPools.sleep(500L);
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.printStackTrace(e);
        }
    }

    private void protectFriendEnergy(JSONObject userHomeObj) {
        try {
            JSONArray wateringBubbles = userHomeObj.optJSONArray("wateringBubbles");
            JSONObject userEnergy = userHomeObj.optJSONObject("userEnergy");
            String userId = userEnergy == null ? UserMap.getCurrentUid() : userEnergy.optString("userId");
            if (wateringBubbles != null && wateringBubbles.length() > 0) {
                for (int j = 0; j < wateringBubbles.length(); j++) {
                    try {
                        JSONObject wateringBubble = wateringBubbles.getJSONObject(j);
                        if (!"fuhuo".equals(wateringBubble.getString("bizType"))) {
                            continue;
                        }
                        if (wateringBubble.getJSONObject("extInfo").optInt("restTimes", 0) == 0) {
                            Status.protectBubbleToday(selfId);
                        }
                        if (!wateringBubble.getBoolean("canProtect")) {
                            continue;
                        }
                        JSONObject joProtect = new JSONObject(AntForestRpcCall.protectBubble(userId));
                        if (!ResChecker.checkRes(TAG + "复活能量失败:", joProtect)) {
                            Log.record(joProtect.getString("resultDesc"));
                            Log.runtime(joProtect.toString());
                            continue;
                        }
                        int vitalityAmount = joProtect.optInt("vitalityAmount", 0);
                        int fullEnergy = wateringBubble.optInt("fullEnergy", 0);
                        String str = "复活能量🚑[" + UserMap.getMaskName(userId) + "-" + fullEnergy + "g]" + (vitalityAmount > 0 ? "#活力值+" + vitalityAmount : "");
                        Log.forest(str);
                        break;
                    } catch (Throwable t) {
                        Log.printStackTrace(t);
                        break;
                    } finally {
                        GlobalThreadPools.sleep(500);
                    }
                }
            }
        } catch (Exception e) {
            Log.printStackTrace(e);
        }
    }

    private void collectEnergy(CollectEnergyEntity collectEnergyEntity) {
        if (errorWait) {
            Log.record(TAG, "异常⌛等待中...不收取能量");
            return;
        }
        Runnable runnable = () -> {
            try {
                String userId = collectEnergyEntity.getUserId();
                usePropBeforeCollectEnergy(userId);
                RpcEntity rpcEntity = collectEnergyEntity.getRpcEntity();
                boolean needDouble = collectEnergyEntity.getNeedDouble();
                boolean needRetry = collectEnergyEntity.getNeedRetry();
                int tryCount = collectEnergyEntity.addTryCount();
                int collected = 0;
                long startTime;

                synchronized (collectEnergyLockLimit) {
                    long sleep;
                    if (needDouble) {
                        collectEnergyEntity.unsetNeedDouble();
                        Integer interval = doubleCollectIntervalEntity.getInterval();
                        sleep = (interval != null ? interval : 1000) - System.currentTimeMillis() + collectEnergyLockLimit.get();
                    } else if (needRetry) {
                        collectEnergyEntity.unsetNeedRetry();
                        sleep = retryIntervalInt - System.currentTimeMillis() + collectEnergyLockLimit.get();
                    } else {
                        Integer interval = collectIntervalEntity.getInterval();
                        sleep = (interval != null ? interval : 1000) - System.currentTimeMillis() + collectEnergyLockLimit.get();
                    }
                    if (sleep > 0) {
                        GlobalThreadPools.sleep(sleep);
                    }
                    startTime = System.currentTimeMillis();
                    collectEnergyLockLimit.setForce(startTime);
                }

                RequestManager.requestString(rpcEntity, 0, 0);
                long spendTime = System.currentTimeMillis() - startTime;
                if (balanceNetworkDelay.getValue()) {
                    delayTimeMath.nextInteger((int) (spendTime / 3));
                }

                if (rpcEntity.getHasError()) {
                    String errorCode = (String) XposedHelpers.callMethod(rpcEntity.getResponseObject(), "getString", "error");
                    if ("1004".equals(errorCode)) {
                        if (BaseModel.getWaitWhenException().getValue() > 0) {
                            long waitTime = System.currentTimeMillis() + BaseModel.getWaitWhenException().getValue();
                            RuntimeInfo.getInstance().put(RuntimeInfo.RuntimeInfoKey.ForestPauseTime, waitTime);
                            Notify.updateStatusText("异常");
                            Log.record(TAG, "触发异常,等待至" + TimeUtil.getCommonDate(waitTime));
                            errorWait = true;
                            return;
                        }
                        GlobalThreadPools.sleep(600 + RandomUtil.delay());
                    }
                    if (tryCount < tryCountInt) {
                        collectEnergyEntity.setNeedRetry();
                        collectEnergy(collectEnergyEntity);
                    }
                    return;
                }

                JSONObject jo = new JSONObject(rpcEntity.getResponseString());
                String resultCode = jo.getString("resultCode");
                if (!"SUCCESS".equalsIgnoreCase(resultCode)) {
                    if ("PARAM_ILLEGAL2".equals(resultCode)) {
                        Log.record(TAG, "[" + getAndCacheUserName(userId) + "]" + "能量已被收取,取消重试 错误:" + jo.getString("resultDesc"));
                        return;
                    }
                    Log.record(TAG, "[" + getAndCacheUserName(userId) + "]" + jo.getString("resultDesc"));
                    if (tryCount < tryCountInt) {
                        collectEnergyEntity.setNeedRetry();
                        collectEnergy(collectEnergyEntity);
                    }
                    return;
                }

                // --- 收能量逻辑保持原样 ---
                JSONArray jaBubbles = jo.getJSONArray("bubbles");
                int jaBubbleLength = jaBubbles.length();
                if (jaBubbleLength > 1) {
                    List<Long> newBubbleIdList = new ArrayList<>();
                    for (int i = 0; i < jaBubbleLength; i++) {
                        JSONObject bubble = jaBubbles.getJSONObject(i);
                        if (bubble.getBoolean("canBeRobbedAgain")) {
                            newBubbleIdList.add(bubble.getLong("id"));
                        }
                        collected += bubble.getInt("collectedEnergy");
                    }
                    if (collected > 0) {
                        int randomIndex = random.nextInt(emojiList.size());
                        String randomEmoji = emojiList.get(randomIndex);
                        String collectType = "takeLook".equals(collectEnergyEntity.getFromTag()) ? "找能量一键收取️" : "一键收取️";
                        String str = collectType + randomEmoji + collected + "g[" + getAndCacheUserName(userId) + "]#";
                        totalCollected += collected;
                        if (needDouble) {
                            Log.forest(str + "耗时[" + spendTime + "]ms[双击]");
                            Toast.show(str + "[双击]");
                        } else {
                            Log.forest(str + "耗时[" + spendTime + "]ms");
                            Toast.show(str);
                        }
                    }
                    if (!newBubbleIdList.isEmpty()) {
                        collectEnergyEntity.setRpcEntity(AntForestRpcCall.batchEnergyRpcEntity("", userId, newBubbleIdList));
                        collectEnergyEntity.setNeedDouble();
                        collectEnergyEntity.resetTryCount();
                        collectEnergy(collectEnergyEntity);
                    }
                } else if (jaBubbleLength == 1) {
                    JSONObject bubble = jaBubbles.getJSONObject(0);
                    collected += bubble.getInt("collectedEnergy");
                    if (collected > 0) {
                        int randomIndex = random.nextInt(emojiList.size());
                        String randomEmoji = emojiList.get(randomIndex);
                        String collectType = "takeLook".equals(collectEnergyEntity.getFromTag()) ? "找能量收取" : "普通收取";
                        String str = collectType + randomEmoji + collected + "g[" + getAndCacheUserName(userId) + "]";
                        totalCollected += collected;
                        if (needDouble) {
                            Log.forest(str + "耗时[" + spendTime + "]ms[双击]");
                            Toast.show(str + "[双击]");
                        } else {
                            Log.forest(str + "耗时[" + spendTime + "]ms");
                            Toast.show(str);
                        }
                    }
                    if (bubble.getBoolean("canBeRobbedAgain")) {
                        collectEnergyEntity.setNeedDouble();
                        collectEnergyEntity.resetTryCount();
                        collectEnergy(collectEnergyEntity);
                        return;
                    }

                    JSONObject userHome = collectEnergyEntity.getUserHome();
                    if (userHome != null) {
                        String bizNo = userHome.optString("bizNo");
                        if (!bizNo.isEmpty()) {
                            int returnCount = getReturnCount(collected);
                            if (returnCount > 0) {
                                // ✅ 调用 returnFriendWater 增加通知好友开关
                                boolean notify = notifyFriend.getValue(); // 从配置获取
                                returnFriendWater(userId, bizNo, 1, returnCount, notify);
                            }
                        }
                    }
                }
            } catch (Exception e) {
                Log.runtime(TAG, "collectEnergy err");
                Log.printStackTrace(e);
            } finally {
                String str_totalCollected = "本次总 收:" + totalCollected + "g 帮:" + totalHelpCollected + "g 浇:" + totalWatered + "g";
                Notify.updateLastExecText(str_totalCollected);
                notifyMain();
            }
        };
        taskCount.incrementAndGet();
        runnable.run();
    }

    private int getReturnCount(int collected) {
        int returnCount = 0;
        if (returnWater33.getValue() > 0 && collected >= returnWater33.getValue()) {
            returnCount = 33;
        } else if (returnWater18.getValue() > 0 && collected >= returnWater18.getValue()) {
            returnCount = 18;
        } else if (returnWater10.getValue() > 0 && collected >= returnWater10.getValue()) {
            returnCount = 10;
        }
        return returnCount;
    }

    /**
     * 更新使用中的的道具剩余时间
     */
    private void updateSelfHomePage() throws JSONException {
        String s = AntForestRpcCall.queryHomePage();
        GlobalThreadPools.sleep(100);
        JSONObject joHomePage = new JSONObject(s);
        updateSelfHomePage(joHomePage);
    }

    /**
     * 更新使用中的的道具剩余时间
     *
     * @param joHomePage 首页 JSON 对象
     */
    private void updateSelfHomePage(JSONObject joHomePage) {
        try {
            JSONArray usingUserPropsNew = joHomePage.getJSONArray("loginUserUsingPropNew");
            if (usingUserPropsNew.length() == 0) {
                usingUserPropsNew = joHomePage.getJSONArray("usingUserPropsNew");
            }
            for (int i = 0; i < usingUserPropsNew.length(); i++) {
                JSONObject userUsingProp = usingUserPropsNew.getJSONObject(i);
                String propGroup = userUsingProp.getString("propGroup");
                switch (propGroup) {
                    case "doubleClick": // 双击卡
                        doubleEndTime = userUsingProp.getLong("endTime");
                        Log.runtime(TAG, "双击卡剩余时间⏰：" + formatTimeDifference(doubleEndTime - System.currentTimeMillis()));
                        break;
                    case "stealthCard": // 隐身卡
                        stealthEndTime = userUsingProp.getLong("endTime");
                        Log.runtime(TAG, "隐身卡剩余时间⏰️：" + formatTimeDifference(stealthEndTime - System.currentTimeMillis()));
                        break;
                    case "shield": // 能量保护罩
                        shieldEndTime = userUsingProp.getLong("endTime");
                        Log.runtime(TAG, "保护罩剩余时间⏰：" + formatTimeDifference(shieldEndTime - System.currentTimeMillis()));
                        break;
                    case "energyBombCard": // 能量炸弹卡
                        energyBombCardEndTime = userUsingProp.getLong("endTime");
                        Log.runtime(TAG, "能量炸弹卡剩余时间⏰：" + formatTimeDifference(energyBombCardEndTime - System.currentTimeMillis()));
                        break;
                    case "robExpandCard": // 1.1倍能量卡
                        String extInfo = userUsingProp.optString("extInfo");
                        robExpandCardEndTime = userUsingProp.getLong("endTime");
                        Log.runtime(TAG, "1.1倍能量卡剩余时间⏰：" + formatTimeDifference(robExpandCardEndTime - System.currentTimeMillis()));
                        if (!extInfo.isEmpty()) {
                            JSONObject extInfoObj = new JSONObject(extInfo);
                            double leftEnergy = Double.parseDouble(extInfoObj.optString("leftEnergy", "0"));
                            if (leftEnergy > 3000 || ("true".equals(extInfoObj.optString("overLimitToday", "false")) && leftEnergy >= 1)) {
                                String propId = userUsingProp.getString("propId");
                                String propType = userUsingProp.getString("propType");
                                JSONObject jo = new JSONObject(AntForestRpcCall.collectRobExpandEnergy(propId, propType));
                                if (ResChecker.checkRes(TAG, jo)) {
                                    int collectEnergy = jo.optInt("collectEnergy");
                                    Log.forest("额外能量🌳[" + collectEnergy + "g][1.1倍能量卡]");
                                }
                            }
                        }
                        break;
                }
            }
        } catch (Throwable th) {
            Log.runtime(TAG, "updateDoubleTime err");
            Log.printStackTrace(TAG, th);
        }
    }

    /**
     * 为好友浇水并返回浇水次数和是否可以继续浇水的状态。
     *
     * @param userId       好友的用户ID
     * @param bizNo        业务编号
     * @param count        需要浇水的次数
     * @param waterEnergy  每次浇水的能量值
     * @param notifyFriend 是否通知好友
     * @return KVMap 包含浇水次数和是否可以继续浇水的状态
     */
    private KVMap<Integer, Boolean> returnFriendWater(String userId, String bizNo, int count, int waterEnergy, boolean notifyFriend) {
        // bizNo为空直接返回默认
        if (bizNo == null || bizNo.isEmpty()) {
            return new KVMap<>(0, true);
        }

        int wateredTimes = 0;   // 已浇水次数
        boolean isContinue = true; // 是否可以继续浇水

        try {
            int energyId = getEnergyId(waterEnergy);

            // 循环浇水
            label:
            for (int waterCount = 1; waterCount <= count; waterCount++) {
                // 调用RPC进行浇水，并传入是否通知好友
                String rpcResponse = AntForestRpcCall.transferEnergy(userId, bizNo, energyId, notifyFriend);

                if (rpcResponse.isEmpty()) {
                    Log.record(TAG, "好友浇水返回空: " + UserMap.getMaskName(userId));
                    isContinue = false;
                    break;
                }

                JSONObject jo = new JSONObject(rpcResponse);

                // 先处理可能的错误码
                String errorCode = jo.optString("error");
                if ("1009".equals(errorCode)) { // 访问被拒绝
                    Log.record(TAG, "好友浇水🚿访问被拒绝: " + UserMap.getMaskName(userId));
                    isContinue = false;
                    break;
                } else if ("3000".equals(errorCode)) { // 系统错误
                    Log.record(TAG, "好友浇水🚿系统错误，稍后重试: " + UserMap.getMaskName(userId));
                    Thread.sleep(500);
                    waterCount--; // 重试当前次数
                    continue;
                }

                // 处理正常返回
                String resultCode = jo.optString("resultCode");
                switch (resultCode) {
                    case "SUCCESS":
                        JSONObject treeEnergy = jo.optJSONObject("treeEnergy");
                        String currentEnergy = treeEnergy != null ? treeEnergy.optString("currentEnergy", "未知") : "未知";
                        Log.forest("好友浇水🚿[" + UserMap.getMaskName(userId) + "]#" + waterEnergy + "g，剩余能量[" + currentEnergy + "g]");
                        wateredTimes++;
                        GlobalThreadPools.sleep(1200L);
                        break;

                    case "WATERING_TIMES_LIMIT":
                        Log.record(TAG, "好友浇水🚿今日已达上限: " + UserMap.getMaskName(userId));
                        wateredTimes = 3; // 上限假设3次
                        break label;

                    case "ENERGY_INSUFFICIENT":
                        Log.record(TAG, "好友浇水🚿" + jo.optString("resultDesc"));
                        isContinue = false;
                        break label;

                    default:
                        Log.record(TAG, "好友浇水🚿" + jo.optString("resultDesc"));
                        Log.runtime(jo.toString());
                        break;
                }
            }
        } catch (Throwable t) {
            Log.runtime(TAG, "returnFriendWater err");
            Log.printStackTrace(TAG, t);
        }

        return new KVMap<>(wateredTimes, isContinue);
    }

    /**
     * 获取能量ID
     */
    private int getEnergyId(int waterEnergy) {
        if (waterEnergy <= 0) return 0;
        if (waterEnergy >= 66) return 42;
        if (waterEnergy >= 33) return 41;
        if (waterEnergy >= 18) return 40;
        return 39;
    }

    /**
     * 兑换能量保护罩
     * 类别 spuid skuid price
     * 限时 CR20230517000497  CR20230516000370  166
     * 永久 CR20230517000497  CR20230516000371  500
     */
    private boolean exchangeEnergyShield() {
        String spuId = "CR20230517000497";
        String skuId = "CR20230516000370";
        if (!Status.canVitalityExchangeToday(skuId, 1)) {
            return false;
        }
        return Vitality.VitalityExchange(spuId, skuId, "保护罩");
    }

    /**
     * 兑换隐身卡
     */
    private boolean exchangeStealthCard() {
        String skuId = "SK20230521000206";
        String spuId = "SP20230521000082";
        if (!Status.canVitalityExchangeToday(skuId, 1)) {
            return false;
        }
        return Vitality.VitalityExchange(spuId, skuId, "隐身卡");
    }


    /**
     * 执行当天森林签到任务
     *
     * @param forestSignVOList 森林签到列表
     * @return 获得的能量，如果签到失败或已签到则返回 0
     */
    private int dailyTask(JSONArray forestSignVOList) {
        try {
            JSONObject forestSignVO = forestSignVOList.getJSONObject(0);
            String currentSignKey = forestSignVO.getString("currentSignKey"); // 当前签到的 key
            String signId = forestSignVO.getString("signId"); // 签到ID
            String sceneCode = forestSignVO.getString("sceneCode"); // 场景代码
            JSONArray signRecords = forestSignVO.getJSONArray("signRecords"); // 签到记录
            for (int i = 0; i < signRecords.length(); i++) { //遍历签到记录
                JSONObject signRecord = signRecords.getJSONObject(i);
                String signKey = signRecord.getString("signKey");
                int awardCount = signRecord.optInt("awardCount", 0);
                if (signKey.equals(currentSignKey) && !signRecord.getBoolean("signed")) {
                    JSONObject joSign = new JSONObject(AntForestRpcCall.antiepSign(signId, UserMap.getCurrentUid(), sceneCode));
                    GlobalThreadPools.sleep(300); // 等待300毫秒
                    if (ResChecker.checkRes(TAG + "森林签到失败:", joSign)) {
                        Log.forest("森林签到📆成功");
                        return awardCount;
                    }
                    break;
                }
            }
            return 0; // 如果没有签到，则返回 0
        } catch (Exception e) {
            Log.printStackTrace(e);
            return 0;
        }
    }

    /**
     * 森林任务:
     * 逛支付宝会员,去森林寻宝抽1t能量
     * 防治荒漠化和干旱日,给随机好友一键浇水
     * 开通高德活动领,去吉祥林许个愿
     * 逛森林集市得能量,逛一逛618会场
     * 逛一逛点淘得红包,去一淘签到领红包
     */
    private void receiveTaskAward() {
        try {
            // 修复：使用new HashSet包装从缓存获取的数据，兼容List/Set类型
            Set<String> presetBad = new LinkedHashSet<>(List.of(
                    "ENERGYRAIN", //能量雨
                    "ENERGY_XUANJIAO", //践行绿色行为
                    "FOREST_TOTAL_COLLECT_ENERGY_3",//累积3天收自己能量
                    "TEST_LEAF_TASK",//逛农场得落叶肥料
                    "SHARETASK" //邀请好友助力
            ));

            /* 3️⃣ 失败任务集合：空文件时自动创建空 HashSet 并立即落盘 */
            TypeReference<Set<String>> typeRef = new TypeReference<>() {};
            Set<String> badTaskSet = DataStore.INSTANCE.getOrCreate("badForestTaskSet", typeRef);
            /* 3️⃣ 首次运行时把预设黑名单合并进去并立即落盘 */
            if (badTaskSet.isEmpty()) {
                badTaskSet.addAll(presetBad);
                DataStore.INSTANCE.put("badForestTaskSet", badTaskSet);   // 持久化
            }

            while (true) {
                boolean doubleCheck = false; // 标记是否需要再次检查任务
                String s = AntForestRpcCall.queryTaskList(); // 查询任务列表
                JSONObject jo = new JSONObject(s); // 解析响应为 JSON 对象

                if (!ResChecker.checkRes(TAG + "查询森林任务失败:", jo)) {
                    Log.record(jo.getString("resultDesc")); // 记录失败描述
                    Log.runtime(s); // 打印响应内容
                    break;
                }

                // 提取森林任务列表
                JSONArray forestSignVOList = jo.getJSONArray("forestSignVOList");
                int SumawardCount = 0;
                int DailyawardCount = dailyTask(forestSignVOList); // 执行每日任务
                SumawardCount = DailyawardCount + SumawardCount;

                // 提取森林任务
                JSONArray forestTasksNew = jo.optJSONArray("forestTasksNew");
                if (forestTasksNew == null || forestTasksNew.length() == 0) {
                    break; // 如果没有新任务，则返回
                }

                // 遍历任务
                for (int i = 0; i < forestTasksNew.length(); i++) {
                    JSONObject forestTask = forestTasksNew.getJSONObject(i);
                    JSONArray taskInfoList = forestTask.getJSONArray("taskInfoList"); // 获取任务信息列表

                    for (int j = 0; j < taskInfoList.length(); j++) {
                        JSONObject taskInfo = taskInfoList.getJSONObject(j);

                        JSONObject taskBaseInfo = taskInfo.getJSONObject("taskBaseInfo"); // 获取任务基本信息
                        String taskType = taskBaseInfo.getString("taskType"); // 获取任务类型
                        String sceneCode = taskBaseInfo.getString("sceneCode"); // 获取场景代码
                        String taskStatus = taskBaseInfo.getString("taskStatus"); // 获取任务状态

                        JSONObject bizInfo = new JSONObject(taskBaseInfo.getString("bizInfo")); // 获取业务信息
                        String taskTitle = bizInfo.optString("taskTitle", taskType); // 获取任务标题

                        JSONObject taskRights = new JSONObject(taskInfo.getString("taskRights")); // 获取任务权益
                        int awardCount = taskRights.optInt("awardCount", 0); // 获取奖励数量

                        // 判断任务状态
                        if (TaskStatus.FINISHED.name().equals(taskStatus)) {
                            // 领取任务奖励
                            JSONObject joAward = new JSONObject(AntForestRpcCall.receiveTaskAward(sceneCode, taskType)); // 领取奖励请求
                            if (ResChecker.checkRes(TAG + "领取森林任务奖励失败:", joAward)) {
                                Log.forest("森林奖励🎖️[" + taskTitle + "]# " + awardCount + "活力值");
                                SumawardCount += awardCount;
                                doubleCheck = true; // 标记需要重新检查任务
                            } else {
                                Log.error(TAG, "领取失败: " + taskTitle); // 记录领取失败信息
                                Log.runtime(joAward.toString()); // 打印奖励响应
                            }
                            GlobalThreadPools.sleep(500);

                        } else if (TaskStatus.TODO.name().equals(taskStatus)) {
                            // 跳过已失败的任务
                            if (badTaskSet.contains(taskType)) continue;

                            if (!badTaskSet.contains(taskType)) {
                                String bizKey = sceneCode + "_" + taskType;
                                int count = forestTaskTryCount
                                        .computeIfAbsent(bizKey, k -> new AtomicInteger(0))
                                        .incrementAndGet();

                                // 完成任务请求
                                JSONObject joFinishTask = new JSONObject(AntForestRpcCall.finishTask(sceneCode, taskType)); // 完成任务请求
                                if (count > 1) {
                                    Log.error(TAG, "完成森林任务失败超过1次" + taskTitle + "\n" + joFinishTask); // 记录完成任务失败信息
                                    badTaskSet.add(taskType);
                                    DataStore.INSTANCE.put("badForestTaskSet", badTaskSet);
                                } else {
                                    Log.forest("森林任务🧾️[" + taskTitle + "]");
                                    doubleCheck = true; // 标记需要重新检查任务
                                }
                            }
                        }

                        // 如果是游戏任务类型，查询并处理游戏任务
                        if ("mokuai_senlin_hlz".equals(taskType)) {
                            // 游戏任务跳转
                            String gameUrl = bizInfo.getString("taskJumpUrl");
                            Log.runtime(TAG, "跳转到游戏: " + gameUrl);

                            // 模拟跳转游戏任务URL（根据需要可能需要在客户端实际触发）
                            Log.runtime(TAG, "等待30S");
                            GlobalThreadPools.sleep(30000); // 等待任务完成
                            // 完成任务请求
                            JSONObject joFinishTask = new JSONObject(AntForestRpcCall.finishTask(sceneCode, taskType)); // 完成任务请求
                            if (ResChecker.checkRes(TAG + "完成游戏任务失败:", joFinishTask)) {
                                Log.forest("游戏任务完成 🎮️[" + taskTitle + "]# " + awardCount + "活力值");
                                SumawardCount += awardCount;
                                doubleCheck = true; // 标记需要重新检查任务
                            } else {
                                Log.error(TAG, "游戏任务完成失败: " + taskTitle); // 记录任务完成失败信息
                            }
                        }
                    }
                }

                if (!doubleCheck) break;
            }

        } catch (Throwable t) {
            handleException("receiveTaskAward", t);
        }
    }

    /**
     * 在收集能量之前使用道具。
     * 这个方法检查是否需要使用增益卡
     * 并在需要时使用相应的道具。
     *
     * @param userId 用户的ID。
     */
    private void usePropBeforeCollectEnergy(String userId) {
        try {
            /*
             * 在收集能量之前决定是否使用增益类道具卡。
             *
             * 主要逻辑:
             * 1. 定义时间常量，用于判断道具剩余有效期。
             * 2. 获取当前时间及各类道具的到期时间，计算剩余时间。
             * 3. 根据以下条件判断是否需要使用特定道具:
             *    - needDouble: 双击卡开关已打开，且当前没有生效的双击卡。
             *    - needrobExpand: 1.1倍能量卡开关已打开，且当前没有生效的卡。
             *    - needStealth: 隐身卡开关已打开，且当前没有生效的隐身卡。
             *    - needShield: 保护罩开关已打开，炸弹卡开关已关闭，且保护罩剩余时间不足一天。
             *    - needEnergyBombCard: 炸弹卡开关已打开，保护罩开关已关闭，且炸弹卡剩余时间不足三天。
             *    - needBubbleBoostCard: 加速卡开关已打开。
             * 4. 如果有任何一个道具需要使用，则同步查询背包信息，并调用相应的使用道具方法。
             */

            long now = System.currentTimeMillis();
            // 双击卡判断
            boolean needDouble = !doubleCard.getValue().equals(applyPropType.CLOSE)
                    && shouldRenewDoubleCard(doubleEndTime, now);

            boolean needrobExpand = !robExpandCard.getValue().equals(applyPropType.CLOSE)
                    && robExpandCardEndTime < now;
            boolean needStealth = !stealthCard.getValue().equals(applyPropType.CLOSE)
                    && stealthEndTime < now;

            // 保护罩判断
            boolean needShield = !shieldCard.getValue().equals(applyPropType.CLOSE)
                    && energyBombCardType.getValue().equals(applyPropType.CLOSE)
                    && shouldRenewShield(shieldEndTime, now);
            // 炸弹卡判断
            boolean needEnergyBombCard = !energyBombCardType.getValue().equals(applyPropType.CLOSE)
                    && shieldCard.getValue().equals(applyPropType.CLOSE)
                    && shouldRenewEnergyBomb(energyBombCardEndTime, now);
            boolean needBubbleBoostCard = !bubbleBoostCard.getValue().equals(applyPropType.CLOSE);

            Log.runtime(TAG, "道具使用检查: needDouble=" + needDouble + ", needrobExpand=" + needrobExpand +
                    ", needStealth=" + needStealth + ", needShield=" + needShield +
                    ", needEnergyBombCard=" + needEnergyBombCard + ", needBubbleBoostCard=" + needBubbleBoostCard);
            if (needDouble || needStealth || needShield || needEnergyBombCard || needrobExpand || needBubbleBoostCard) {
                synchronized (doubleCardLockObj) {
                    JSONObject bagObject = queryPropList();
                    // Log.runtime(TAG, "bagObject=" + (bagObject == null ? "null" : bagObject.toString()));

                    if (needDouble) useDoubleCard(bagObject);           // 使用双击卡
                    if (needrobExpand) userobExpandCard();              // 使用1.1倍能量卡
                    if (needStealth) useStealthCard(bagObject);         // 使用隐身卡
                    if (needBubbleBoostCard) useBubbleBoostCard();      // 使用加速卡
                    if (needShield) {
                        Log.runtime(TAG, "尝试使用保护罩罩");
                        useShieldCard(bagObject);
                    } else if (needEnergyBombCard) {
                        Log.runtime(TAG, "准备使用能量炸弹卡");
                        useEnergyBombCard(bagObject);
                    }
                }
            } else {
                Log.runtime(TAG, "没有需要使用的道具");
            }
        } catch (Exception e) {
            Log.printStackTrace(e);
        }
    }

    /**
     * 保护罩剩余时间判断
     * 以整数 HHmm 指定保护罩续写阈值。
     * 例如：2355 表示 23 小时 55 分钟，0955 可直接写为 955。
     * 校验规则：0 ≤ HH ≤ 99，0 ≤ mm ≤ 59；非法值将回退为默认值。
     */
    @SuppressLint("DefaultLocale")
    private boolean shouldRenewShield(long shieldEnd, long nowMillis) {
        // 解析阈值配置
        int hours, minutes;
        if (SHIELD_RENEW_THRESHOLD_HHMM >= 0 && SHIELD_RENEW_THRESHOLD_HHMM <= 9959) {
            try {
                int abs = Math.abs(SHIELD_RENEW_THRESHOLD_HHMM);
                hours = abs / 100;    // 提取小时部分
                minutes = abs % 100;  // 提取分钟部分
                
                // 验证分钟有效性（0-59）
            } catch (Exception e) {
                Log.record(TAG, "[保护罩] 解析阈值配置异常: " + e.getMessage() + ", 使用默认值");
                hours = 23;
                minutes = 59;
            }
        } else {
            // 使用默认值
            hours = 23;
            minutes = 59;
        }
        long thresholdMs = hours * TimeFormatter.ONE_HOUR_MS + minutes * TimeFormatter.ONE_MINUTE_MS;
        if (shieldEnd <= nowMillis) { // 未生效或已过期
            Log.record(TAG, "[保护罩] 未生效/已过期，立即续写；end=" + TimeUtil.getCommonDate(shieldEnd) + ", now=" + TimeUtil.getCommonDate(nowMillis));
            return true;
        }
        long remain = shieldEnd - nowMillis;
        boolean needRenew = remain <= thresholdMs;
        // 格式化剩余时间和阈值时间为更直观的显示
        String remainTimeStr = TimeFormatter.formatRemainingTime(remain);
        String thresholdTimeStr = String.format("%02d小时%02d分", hours, minutes);
        if (needRenew) {
            Log.record(TAG, String.format("[保护罩] 🔄 需要续写 - 剩余时间[%s] ≤ 续写阈值[%s]", 
                remainTimeStr, thresholdTimeStr));
        } else {
            Log.record(TAG, String.format("[保护罩] ✅ 无需续写 - 剩余时间[%s] > 续写阈值[%s]", 
                remainTimeStr, thresholdTimeStr));
        }
        // 详细调试信息（可选）
        Log.runtime(TAG, String.format("[保护罩] 详细对比: %dms ≤ %dms = %s", 
            remain, thresholdMs, needRenew));
        return needRenew;
    }

    /**
     * 炸弹卡剩余时间判断
     * 当炸弹卡剩余时间低于3天时，需要续用
     * 最多可续用到4天
     */
    @SuppressLint("DefaultLocale")
    private boolean shouldRenewEnergyBomb(long bombEnd, long nowMillis) {
        // 炸弹卡最长有效期为4天
        long MAX_BOMB_DURATION = 4 * TimeFormatter.ONE_DAY_MS;
        // 炸弹卡续用阈值为3天
        long BOMB_RENEW_THRESHOLD = 3 * TimeFormatter.ONE_DAY_MS;
        if (bombEnd <= nowMillis) { // 未生效或已过期
            Log.runtime(TAG, "[炸弹卡] 未生效/已过期，立即续写；end=" + TimeUtil.getCommonDate(bombEnd) + ", now=" + TimeUtil.getCommonDate(nowMillis));
            return true;
        }
        long remain = bombEnd - nowMillis;
        // 如果剩余时间小于阈值且当前总时长未超过最大有效期，则需要续用
        boolean needRenew = remain <= BOMB_RENEW_THRESHOLD && (bombEnd - nowMillis + remain) <= MAX_BOMB_DURATION;
        
        String remainTimeStr = TimeFormatter.formatRemainingTime(remain);
        String thresholdTimeStr = TimeFormatter.formatRemainingTime(BOMB_RENEW_THRESHOLD);
        
        if (needRenew) {
            Log.runtime(TAG, String.format("[炸弹卡] 🔄 需要续写 - 剩余时间[%s] ≤ 续写阈值[%s]", 
                remainTimeStr, thresholdTimeStr));
        } else {
            Log.runtime(TAG, String.format("[炸弹卡] ✅ 无需续写 - 剩余时间[%s] > 续写阈值[%s]", 
                remainTimeStr, thresholdTimeStr));
        }
        
        // 详细调试信息
        Log.runtime(TAG, String.format("[炸弹卡] 详细对比: %dms ≤ %dms = %s, 总时长检查: %dms ≤ %dms", 
            remain, BOMB_RENEW_THRESHOLD, (remain <= BOMB_RENEW_THRESHOLD),
            (bombEnd - nowMillis + remain), MAX_BOMB_DURATION));
            
        return needRenew;
    }

    /**
     * 双击卡剩余时间判断
     * 当双击卡剩余时间低于31天时，需要续用
     * 最多可续用到31+31天，但不建议，因为平时有5分钟、3天、7天等短期双击卡
     */
    @SuppressLint("DefaultLocale")
    private boolean shouldRenewDoubleCard(long doubleEnd, long nowMillis) {
        // 双击卡最长有效期为62天（31+31）
        // 双击卡续用阈值为31天
        long DOUBLE_RENEW_THRESHOLD = 31 * TimeFormatter.ONE_DAY_MS;

        if (doubleEnd <= nowMillis) { // 未生效或已过期
            Log.runtime(TAG, "[双击卡] 未生效/已过期，立即续写；end=" + TimeUtil.getCommonDate(doubleEnd) + ", now=" + TimeUtil.getCommonDate(nowMillis));
            return true;
        }

        long remain = doubleEnd - nowMillis;
        // 如果剩余时间小于阈值，则需要续用
        boolean needRenew = remain <= DOUBLE_RENEW_THRESHOLD;
        
        String remainTimeStr = TimeFormatter.formatRemainingTime(remain);
        String thresholdTimeStr = TimeFormatter.formatRemainingTime(DOUBLE_RENEW_THRESHOLD);
        
        if (needRenew) {
            Log.runtime(TAG, String.format("[双击卡] 🔄 需要续写 - 剩余时间[%s] ≤ 续写阈值[%s]", 
                remainTimeStr, thresholdTimeStr));
        } else {
            Log.runtime(TAG, String.format("[双击卡] ✅ 无需续写 - 剩余时间[%s] > 续写阈值[%s]", 
                remainTimeStr, thresholdTimeStr));
        }
        
        // 详细调试信息
        Log.runtime(TAG, String.format("[双击卡] 详细对比: %dms ≤ %dms = %s", 
            remain, DOUBLE_RENEW_THRESHOLD, needRenew));
            
        return needRenew;
    }

    /**
     * 检查当前时间是否在设置的使用双击卡时间内
     *
     * @return 如果当前时间在双击卡的有效时间范围内，返回true；否则返回false。
     */
    private boolean hasDoubleCardTime() {
        long currentTimeMillis = System.currentTimeMillis();
        return TimeUtil.checkInTimeRange(currentTimeMillis, doubleCardTime.getValue());
    }

    private void giveProp() {
        Set<String> set = whoYouWantToGiveTo.getValue();
        if (!set.isEmpty()) {
            for (String userId : set) {
                if (!selfId.equals(userId)) {
                    giveProp(userId);
                    break;
                }
            }
        }
    }

    /**
     * 向指定用户赠送道具。 这个方法首先查询可用的道具列表，然后选择一个道具赠送给目标用户。 如果有多个道具可用，会尝试继续赠送，直到所有道具都赠送完毕。
     *
     * @param targetUserId 目标用户的ID。
     */
    private void giveProp(String targetUserId) {
        try {
            do {
                // 查询道具列表
                JSONObject propListJo = new JSONObject(AntForestRpcCall.queryPropList(true));
                if (ResChecker.checkRes(TAG + "查询道具列表失败:", propListJo)) {
                    JSONArray forestPropVOList = propListJo.optJSONArray("forestPropVOList");
                    if (forestPropVOList != null && forestPropVOList.length() > 0) {
                        JSONObject propJo = forestPropVOList.getJSONObject(0);
                        String giveConfigId = propJo.getJSONObject("giveConfigVO").getString("giveConfigId");
                        int holdsNum = propJo.optInt("holdsNum", 0);
                        String propName = propJo.getJSONObject("propConfigVO").getString("propName");
                        String propId = propJo.getJSONArray("propIdList").getString(0);
                        JSONObject giveResultJo = new JSONObject(AntForestRpcCall.giveProp(giveConfigId, propId, targetUserId));
                        if (ResChecker.checkRes(TAG + "赠送道具失败:", giveResultJo)) {
                            Log.forest("赠送道具🎭[" + UserMap.getMaskName(targetUserId) + "]#" + propName);
                            GlobalThreadPools.sleep(1500);
                        } else {
                            String rt = giveResultJo.getString("resultDesc");
                            Log.record(rt);
                            Log.runtime(giveResultJo.toString());
                            if (rt.contains("异常")) {
                                return;
                            }
                        }
                        // 如果持有数量大于1或道具列表中有多于一个道具，则继续赠送
                        if (holdsNum <= 1 && forestPropVOList.length() == 1) {
                            break;
                        }
                    }
                } else {
                    // 如果查询道具列表失败，则记录失败的日志
                    Log.record(TAG, "赠送道具查询结果" + propListJo.getString("resultDesc"));
                }
                // 等待1.5秒后再继续
            } while (true);
        } catch (Throwable th) {
            // 打印异常信息
            Log.runtime(TAG, "giveProp err");
            Log.printStackTrace(TAG, th);
        }
    }

    /**
     * 查询并管理用户巡护任务
     */
    private void queryUserPatrol() {
        long waitTime = 300L;//增大查询等待时间，减少异常
        try {
            do {
                // 查询当前巡护任务
                JSONObject jo = new JSONObject(AntForestRpcCall.queryUserPatrol());
                // GlobalThreadPools.sleep(waitTime);
                // 如果查询成功
                if (ResChecker.checkRes(TAG + "查询巡护任务失败:", jo)) {
                    // 查询我的巡护记录
                    JSONObject resData = new JSONObject(AntForestRpcCall.queryMyPatrolRecord());
                    // GlobalThreadPools.sleep(waitTime);
                    if (resData.optBoolean("canSwitch")) {
                        JSONArray records = resData.getJSONArray("records");
                        for (int i = 0; i < records.length(); i++) {
                            JSONObject record = records.getJSONObject(i);
                            JSONObject userPatrol = record.getJSONObject("userPatrol");
                            // 如果存在未到达的节点，且当前模式为"silent"，则尝试切换巡护地图
                            if (userPatrol.getInt("unreachedNodeCount") > 0) {
                                if ("silent".equals(userPatrol.getString("mode"))) {
                                    JSONObject patrolConfig = record.getJSONObject("patrolConfig");
                                    String patrolId = patrolConfig.getString("patrolId");
                                    resData = new JSONObject(AntForestRpcCall.switchUserPatrol(patrolId));
                                    GlobalThreadPools.sleep(waitTime);
                                    // 如果切换成功，打印日志并继续
                                    if (ResChecker.checkRes(TAG + "切换巡护地图失败:", resData)) {
                                        Log.forest("巡护⚖️-切换地图至" + patrolId);
                                    }
                                    continue; // 跳过当前循环
                                }
                                break; // 如果当前不是silent模式，则结束循环
                            }
                        }
                    }
                    // 获取用户当前巡护状态信息
                    JSONObject userPatrol = jo.getJSONObject("userPatrol");
                    int currentNode = userPatrol.getInt("currentNode");
                    String currentStatus = userPatrol.getString("currentStatus");
                    int patrolId = userPatrol.getInt("patrolId");
                    JSONObject chance = userPatrol.getJSONObject("chance");
                    int leftChance = chance.getInt("leftChance");
                    int leftStep = chance.getInt("leftStep");
                    int usedStep = chance.getInt("usedStep");
                    if ("STANDING".equals(currentStatus)) {// 当前巡护状态为"STANDING"
                        if (leftChance > 0) {// 如果还有剩余的巡护次数，则开始巡护
                            jo = new JSONObject(AntForestRpcCall.patrolGo(currentNode, patrolId));
                            GlobalThreadPools.sleep(waitTime);
                            patrolKeepGoing(jo.toString(), currentNode, patrolId); // 继续巡护
                            continue; // 跳过当前循环
                        } else if (leftStep >= 2000 && usedStep < 10000) {// 如果没有剩余的巡护次数但步数足够，则兑换巡护次数
                            jo = new JSONObject(AntForestRpcCall.exchangePatrolChance(leftStep));
                            // GlobalThreadPools.sleep(waitTime);
                            if (ResChecker.checkRes(TAG + "兑换巡护次数失败:", jo)) {// 兑换成功，增加巡护次数
                                int addedChance = jo.optInt("addedChance", 0);
                                Log.forest("步数兑换⚖️[巡护次数*" + addedChance + "]");
                                continue; // 跳过当前循环
                            } else {
                                Log.runtime(TAG, jo.getString("resultDesc"));
                            }
                        }
                    }
                    // 如果巡护状态为"GOING"，继续巡护
                    else if ("GOING".equals(currentStatus)) {
                        patrolKeepGoing(null, currentNode, patrolId);
                    }
                } else {
                    Log.runtime(TAG, jo.getString("resultDesc"));
                }
                break; // 完成一次巡护任务后退出循环
            } while (true);
        } catch (Throwable t) {
            Log.runtime(TAG, "queryUserPatrol err");
            Log.printStackTrace(TAG, t); // 打印异常堆栈
        }
    }

    /**
     * 持续巡护森林，直到巡护状态不再是"进行中"
     *
     * @param s         巡护请求的响应字符串，若为null将重新请求
     * @param nodeIndex 当前节点索引
     * @param patrolId  巡护任务ID
     */
    private void patrolKeepGoing(String s, int nodeIndex, int patrolId) {
        try {
            do {
                if (s == null) {
                    s = AntForestRpcCall.patrolKeepGoing(nodeIndex, patrolId, "image");
                }
                JSONObject jo;
                try {
                    jo = new JSONObject(s);
                } catch (JSONException e) {
                    Log.record(TAG, "JSON解析错误: " + e.getMessage());
                    Log.printStackTrace(TAG, e);
                    return; // 解析失败，退出循环
                }
                if (!ResChecker.checkRes(TAG, jo)) {
                    Log.runtime(TAG, jo.getString("resultDesc"));
                    break;
                }
                JSONArray events = jo.optJSONArray("events");
                if (events == null || events.length() == 0) {
                    return; // 无事件，退出循环
                }
                JSONObject event = events.getJSONObject(0);
                JSONObject userPatrol = jo.getJSONObject("userPatrol");
                int currentNode = userPatrol.getInt("currentNode");
                // 获取奖励信息，并处理动物碎片奖励
                JSONObject rewardInfo = event.optJSONObject("rewardInfo");
                if (rewardInfo != null) {
                    JSONObject animalProp = rewardInfo.optJSONObject("animalProp");
                    if (animalProp != null) {
                        JSONObject animal = animalProp.optJSONObject("animal");
                        if (animal != null) {
                            Log.forest("巡护森林🏇🏻[" + animal.getString("name") + "碎片]");
                        }
                    }
                }
                // 如果巡护状态不是"进行中"，则退出循环
                if (!"GOING".equals(jo.getString("currentStatus"))) {
                    return;
                }
                // 请求继续巡护
                JSONObject materialInfo = event.getJSONObject("materialInfo");
                String materialType = materialInfo.optString("materialType", "image");
                s = AntForestRpcCall.patrolKeepGoing(currentNode, patrolId, materialType);
                GlobalThreadPools.sleep(100); // 等待100毫秒后继续巡护
            } while (true);
        } catch (Throwable t) {
            Log.runtime(TAG, "patrolKeepGoing err");
            Log.printStackTrace(TAG, t);
        }
    }

    /**
     * 查询并派遣伙伴
     */
    private void queryAndConsumeAnimal() {
        try {
            // 查询动物属性列表
            JSONObject jo = new JSONObject(AntForestRpcCall.queryAnimalPropList());
            if (!ResChecker.checkRes(TAG, jo)) {
                Log.runtime(TAG, jo.getString("resultDesc"));
                return;
            }
            // 获取所有动物属性并选择可以派遣的伙伴
            JSONArray animalProps = jo.getJSONArray("animalProps");
            JSONObject bestAnimalProp = null;
            for (int i = 0; i < animalProps.length(); i++) {
                jo = animalProps.getJSONObject(i);
                if (bestAnimalProp == null || jo.getJSONObject("main").getInt("holdsNum") > bestAnimalProp.getJSONObject("main").getInt("holdsNum")) {
                    bestAnimalProp = jo; // 默认选择最大数量的伙伴
                }
            }
            // 派遣伙伴
            consumeAnimalProp(bestAnimalProp);
        } catch (Throwable t) {
            Log.runtime(TAG, "queryAnimalPropList err");
            Log.printStackTrace(TAG, t);
        }
    }

    /**
     * 派遣伙伴进行巡护
     *
     * @param animalProp 选择的动物属性
     */
    private void consumeAnimalProp(JSONObject animalProp) {
        if (animalProp == null) return; // 如果没有可派遣的伙伴，则返回
        try {
            // 获取伙伴的属性信息
            String propGroup = animalProp.getJSONObject("main").getString("propGroup");
            String propType = animalProp.getJSONObject("main").getString("propType");
            String name = animalProp.getJSONObject("partner").getString("name");
            // 调用API进行伙伴派遣
            JSONObject jo = new JSONObject(AntForestRpcCall.consumeProp(propGroup, propType, false));
            if (ResChecker.checkRes(TAG + "巡护派遣失败:", jo)) {
                Log.forest("巡护派遣🐆[" + name + "]");
            } else {
                Log.runtime(TAG, jo.getString("resultDesc"));
            }
        } catch (Throwable t) {
            Log.runtime(TAG, "consumeAnimalProp err");
            Log.printStackTrace(TAG, t);
        }
    }

    /**
     * 查询动物及碎片信息，并尝试合成可合成的动物碎片。
     */
    private void queryAnimalAndPiece() {
        try {
            // 调用远程接口查询动物及碎片信息
            JSONObject response = new JSONObject(AntForestRpcCall.queryAnimalAndPiece(0));
            String resultCode = response.optString("resultCode");
            // 检查接口调用是否成功
            if (!"SUCCESS".equals(resultCode)) {
                Log.runtime(TAG, "查询失败: " + response.optString("resultDesc"));
                return;
            }
            // 获取动物属性列表
            JSONArray animalProps = response.optJSONArray("animalProps");
            if (animalProps == null || animalProps.length() == 0) {
                Log.runtime(TAG, "动物属性列表为空");
                return;
            }
            // 遍历动物属性
            for (int i = 0; i < animalProps.length(); i++) {
                JSONObject animalObject = animalProps.optJSONObject(i);
                if (animalObject == null) {
                    continue;
                }
                JSONArray pieces = animalObject.optJSONArray("pieces");
                if (pieces == null || pieces.length() == 0) {
                    Log.runtime(TAG, "动物碎片列表为空");
                    continue;
                }
                int animalId = Objects.requireNonNull(animalObject.optJSONObject("animal")).optInt("id", -1);
                if (animalId == -1) {
                    Log.runtime(TAG, "动物ID缺失");
                    continue;
                }
                // 检查碎片是否满足合成条件
                if (canCombinePieces(pieces)) {
                    combineAnimalPiece(animalId);
                }
            }
        } catch (Exception e) {
            Log.runtime(TAG, "查询动物及碎片信息时发生错误:");
            Log.printStackTrace(TAG, e);
        }
    }

    /**
     * 检查碎片是否满足合成条件。
     *
     * @param pieces 动物碎片数组
     * @return 如果所有碎片满足合成条件，返回 true；否则返回 false
     */
    private boolean canCombinePieces(JSONArray pieces) {
        for (int j = 0; j < pieces.length(); j++) {
            JSONObject pieceObject = pieces.optJSONObject(j);
            if (pieceObject == null || pieceObject.optInt("holdsNum", 0) <= 0) {
                return false;
            }
        }
        return true;
    }

    /**
     * 合成动物碎片。
     *
     * @param animalId 动物ID
     */
    private void combineAnimalPiece(int animalId) {
        try {
            while (true) {
                // 查询动物及碎片信息
                JSONObject response = new JSONObject(AntForestRpcCall.queryAnimalAndPiece(animalId));
                String resultCode = response.optString("resultCode");
                if (!"SUCCESS".equals(resultCode)) {
                    Log.runtime(TAG, "查询失败: " + response.optString("resultDesc"));
                    break;
                }
                JSONArray animalProps = response.optJSONArray("animalProps");
                if (animalProps == null || animalProps.length() == 0) {
                    Log.runtime(TAG, "动物属性数据为空");
                    break;
                }
                // 获取第一个动物的属性
                JSONObject animalProp = animalProps.getJSONObject(0);
                JSONObject animal = animalProp.optJSONObject("animal");
                assert animal != null;
                int id = animal.optInt("id", -1);
                String name = animal.optString("name", "未知动物");
                // 获取碎片信息
                JSONArray pieces = animalProp.optJSONArray("pieces");
                if (pieces == null || pieces.length() == 0) {
                    Log.runtime(TAG, "碎片数据为空");
                    break;
                }
                boolean canCombineAnimalPiece = true;
                JSONArray piecePropIds = new JSONArray();
                // 检查所有碎片是否可用
                for (int j = 0; j < pieces.length(); j++) {
                    JSONObject piece = pieces.optJSONObject(j);
                    if (piece == null || piece.optInt("holdsNum", 0) <= 0) {
                        canCombineAnimalPiece = false;
                        Log.runtime(TAG, "碎片不足，无法合成动物");
                        break;
                    }
                    // 添加第一个道具ID
                    piecePropIds.put(Objects.requireNonNull(piece.optJSONArray("propIdList")).optString(0, ""));
                }
                // 如果所有碎片可用，则尝试合成
                if (canCombineAnimalPiece) {
                    JSONObject combineResponse = new JSONObject(AntForestRpcCall.combineAnimalPiece(id, piecePropIds.toString()));
                    resultCode = combineResponse.optString("resultCode");
                    if ("SUCCESS".equals(resultCode)) {
                        Log.forest("成功合成动物💡[" + name + "]");
                        animalId = id;
                        GlobalThreadPools.sleep(100); // 等待一段时间再查询
                        continue;
                    } else {
                        Log.runtime(TAG, "合成失败: " + combineResponse.optString("resultDesc"));
                    }
                }
                break; // 如果不能合成或合成失败，跳出循环
            }
        } catch (Exception e) {
            Log.runtime(TAG, "合成动物碎片时发生错误:");
            Log.printStackTrace(TAG, e);
        }
    }

    /**
     * 获取背包信息
     */
    private JSONObject queryPropList() {
        try {
            JSONObject bagObject = new JSONObject(AntForestRpcCall.queryPropList(false));
            if (ResChecker.checkRes(TAG + "查询背包失败:", bagObject)) {
                return bagObject;
            }
            Log.error(TAG, "获取背包信息失败: " + bagObject);
        } catch (Exception e) {
            Log.printStackTrace(TAG, "获取背包信息失败:", e);
        }
        return null;
    }

    /**
     * 查找背包道具
     *
     * @param bagObject 背包对象
     * @param propType  道具类型 LIMIT_TIME_ENERGY_SHIELD_TREE,...
     */
    private JSONObject findPropBag(JSONObject bagObject, String propType) {
        if (Objects.isNull(bagObject)) {
            return null;
        }
        try {
            JSONArray forestPropVOList = bagObject.getJSONArray("forestPropVOList");
            for (int i = 0; i < forestPropVOList.length(); i++) {
                JSONObject forestPropVO = forestPropVOList.getJSONObject(i);
                JSONObject propConfigVO = forestPropVO.getJSONObject("propConfigVO");
                String currentPropType = propConfigVO.getString("propType");
               // String propName = propConfigVO.getString("propName");
                if (propType.equals(currentPropType)) {
                    return forestPropVO; // 找到后直接返回
                }
            }
        } catch (Exception e) {
            Log.error(TAG, "查找背包道具出错:");
            Log.printStackTrace(TAG, e);
        }

        return null; // 未找到或出错时返回 null
    }

    /**
     * 返回背包道具信息
     */
    private void showBag() {
        JSONObject bagObject = queryPropList();
        if (Objects.isNull(bagObject)) {
            return;
        }
        try {
            JSONArray forestPropVOList = Objects.requireNonNull(bagObject).getJSONArray("forestPropVOList");
            for (int i = 0; i < forestPropVOList.length(); i++) {
                JSONObject forestPropVO = forestPropVOList.getJSONObject(i);
                JSONObject propConfigVO = forestPropVO.getJSONObject("propConfigVO");
                String currentPropType = propConfigVO.getString("propType");
                String propName = propConfigVO.getString("propName");
                Log.record("道具名称:"+propName+",道具代码:"+currentPropType);
            }
        } catch (Exception e) {
            Log.error(TAG, "查找背包道具出错:");
            Log.printStackTrace(TAG, e);
        }

    }

    /**
     * 使用背包道具
     *
     * @param propJsonObj 道具对象
     */
    private boolean usePropBag(JSONObject propJsonObj) {
        if (propJsonObj == null) {
            Log.record(TAG, "要使用的道具不存在！");
            return false;
        }
        try {
            String propId = propJsonObj.getJSONArray("propIdList").getString(0);
            JSONObject propConfigVO = propJsonObj.getJSONObject("propConfigVO");
            String propType = propConfigVO.getString("propType");
            String propName = propConfigVO.getString("propName");
            String tag = propEmoji(propName);
            JSONObject jo;
            boolean isRenewable = isRenewableProp(propType);
            Log.record(TAG, "道具 " + propName + " (类型: " + propType + "), 是否可续用: " + isRenewable);
            String propGroup = AntForestRpcCall.getPropGroup(propType);
            if (isRenewable) {
                // 第一步：发送检查/尝试使用请求 (secondConfirm=false)
                String checkResponseStr = AntForestRpcCall.consumeProp(propGroup, propId, propType, false);
                JSONObject checkResponse = new JSONObject(checkResponseStr);
                // Log.record(TAG, "发送检查请求: " + checkResponse);
                JSONObject resData = checkResponse.optJSONObject("resData");
                if (resData == null) {
                    resData = checkResponse;
                }

                String status = resData.optString("usePropStatus");
                Log.record(TAG, "查成功, 状态: " + status);

                if ("NEED_CONFIRM_CAN_PROLONG".equals(status)) {
                    // 情况1: 需要二次确认 (真正的续写)
                    Log.record(TAG, "需要二次确认，发送确认请求...");
                    GlobalThreadPools.sleep(2000);
                    String confirmResponseStr = AntForestRpcCall.consumeProp(propGroup, propId, propType, true);
                    jo = new JSONObject(confirmResponseStr);
                    // Log.record(TAG, "发送确认请求: " + jo);
                }  else {
                    // 其他所有情况都视为最终结果，通常是失败
                    Log.record(TAG, "道具状态异常或使用失败。");
                    jo = checkResponse;
                }
            } else {
                // 非续用类道具，直接使用
                Log.record(TAG, "非续用类道具，直接使用");
                String consumeResponse = AntForestRpcCall.consumeProp2(propGroup, propId, propType);
                jo = new JSONObject(consumeResponse);
            }

            // 统一结果处理
            if (ResChecker.checkRes(TAG + "使用道具失败:", jo)) {
                Log.forest("使用道具" + tag + "[" + propName + "]");
                updateSelfHomePage();
                return true;
            } else {
                JSONObject errorData = jo.optJSONObject("resData");
                if (errorData == null) {
                    errorData = jo;
                }
                String resultDesc = errorData.optString("resultDesc", "未知错误");
                Log.record("使用道具失败: " + resultDesc);
                Toast.show(resultDesc);
                return false;
            }

        } catch (Throwable th) {
            Log.runtime(TAG, "usePropBag err");
            Log.printStackTrace(TAG, th);
            return false;
        }
    }
    /**
     * 判断是否是可续用类道具
     */
    private boolean isRenewableProp(String propType) {
        return propType.contains("SHIELD")   // 保护罩
                || propType.contains("BOMB_CARD") // 炸弹卡
                || propType.contains("DOUBLE_CLICK");     // 双击卡
    }


    @NonNull
    private static String propEmoji(String propName) {
        String tag;
        if (propName.contains("保")) {
            tag = "🛡️";
        } else if (propName.contains("双")) {
            tag = "👥";
        } else if (propName.contains("加")) {
            tag = "🌪";
        } else if (propName.contains("雨")) {
            tag = "🌧️";
        } else if (propName.contains("炸")) {
            tag = "💥";
        } else {
            tag = "🥳";
        }
        return tag;
    }


    /**
     * 使用双击卡道具
     * 功能：提高能量收取效率，可以进行双击收取
     * 
     * @param bagObject 背包的JSON对象
     */
    private void useDoubleCard(JSONObject bagObject) {
        PropConfig config = new PropConfig(
            "双击卡",
            new String[]{"LIMIT_TIME_ENERGY_DOUBLE_CLICK", "ENERGY_DOUBLE_CLICK_31DAYS", "ENERGY_DOUBLE_CLICK"},
            () -> hasDoubleCardTime() && Status.canDoubleToday(),
            () -> Vitality.handleVitalityExchange("SK20240805004754") || 
                  Vitality.handleVitalityExchange("CR20230516000363"),
            (time) -> {
                doubleEndTime = time + 5 * TimeFormatter.ONE_MINUTE_MS;
                Status.DoubleToday();
            }
        );
        
        usePropTemplate(bagObject, config, doubleCardConstant.getValue());
    }

    /**
     * 使用隐身卡道具
     * 功能：隐藏收取行为，避免被好友发现偷取能量
     * 
     * @param bagObject 背包的JSON对象
     */
    private void useStealthCard(JSONObject bagObject) {
        PropConfig config = new PropConfig(
            "隐身卡",
            new String[]{"LIMIT_TIME_STEALTH_CARD", "STEALTH_CARD"},
            null, // 无特殊条件
            this::exchangeStealthCard,
            (time) -> stealthEndTime = time + TimeFormatter.ONE_DAY_MS
        );
        
        usePropTemplate(bagObject, config, stealthCardConstant.getValue());
    }

    /**
     * 使用保护罩道具
     * 功能：保护自己的能量不被好友偷取，防止能量被收走
     * 一般是限时保护罩，可通过青春特权森林道具领取
     * 
     * @param bagObject 背包的JSON对象
     */
    private void useShieldCard(JSONObject bagObject) {
        try {
            Log.record(TAG, "尝试使用保护罩...");
            JSONObject jo = findPropBag(bagObject, "LIMIT_TIME_ENERGY_SHIELD_TREE");
            if (jo == null) {
                Log.record(TAG, "背包中没有森林保护罩(LIMIT_TIME_ENERGY_SHIELD_TREE)，继续查找其他类型...");
                if (youthPrivilege.getValue()) {
                    Log.runtime(TAG, "尝试通过青春特权获取保护罩...");
                    if (Privilege.INSTANCE.youthPrivilege()) {
                        jo = findPropBag(querySelfHome(), "LIMIT_TIME_ENERGY_SHIELD_TREE");
                    }
                }
            }
            if (jo == null) {
                if (shieldCardConstant.getValue()) {
                    Log.record(TAG, "尝试通过活力值兑换保护罩...");
                    if (exchangeEnergyShield()) {
                        jo = findPropBag(querySelfHome(), "LIMIT_TIME_ENERGY_SHIELD");
                    }
                }
            }
            if (jo == null) {
                Log.record(TAG, "尝试能量保护罩(ENERGY_SHIELD)...");
                jo = findPropBag(bagObject, "ENERGY_SHIELD");
            }
            if (jo != null) {
                Log.runtime(TAG, "找到保护罩，准备使用: " + jo);
                if (usePropBag(jo)) {
                    return; // 使用成功，直接返回
                }
            }
            Log.record(TAG, "背包中未找到任何可用保护罩。");
            // 如果未使用成功，也刷新一次
            updateSelfHomePage();
        } catch (Throwable th) {
            Log.error(TAG + "使用能量保护罩， err");
            Log.printStackTrace(th);
        }
    }


    /**
     * 使用加速卡道具
     * 功能：加速能量球成熟时间，让等待中的能量球提前成熟
     */
    private void useBubbleBoostCard() {
        useBubbleBoostCard(queryPropList());
    }

    /**
     * 使用1.1倍能量卡道具
     * 功能：增加能量收取倍数，收取好友能量时获得1.1倍效果
     */
    private void userobExpandCard() {
        userobExpandCard(queryPropList());
    }

    private void useBubbleBoostCard(JSONObject bag) {
        try {
            // 在背包中查询限时加速器
            JSONObject jo = findPropBag(bag, "LIMIT_TIME_ENERGY_BUBBLE_BOOST");
            if (jo == null) {
                Privilege.INSTANCE.youthPrivilege();
                jo = findPropBag(queryPropList(), "LIMIT_TIME_ENERGY_BUBBLE_BOOST"); // 重新查找
                if (jo == null) {
                    jo = findPropBag(bag, "BUBBLE_BOOST"); // 尝试查找 普通加速器，一般用不到
                }
            }
            if (jo != null) {
                usePropBag(jo);
            }
        } catch (Throwable th) {
            Log.runtime(TAG, "useBubbleBoostCard err");
            Log.printStackTrace(TAG, th);
        }
    }

    private void userobExpandCard(JSONObject bag) {
        try {
            JSONObject jo = findPropBag(bag, "VITALITY_ROB_EXPAND_CARD_1.1_3DAYS");
            if (jo != null && usePropBag(jo)) {
                robExpandCardEndTime = System.currentTimeMillis() + 1000 * 60 * 5;
            }
            jo = findPropBag(bag, "SHAMO_ROB_EXPAND_CARD_1.5_1DAYS");
            if (jo != null && usePropBag(jo)) {
                robExpandCardEndTime = System.currentTimeMillis() + 1000 * 60 * 5;
            }
        } catch (Throwable th) {
            Log.runtime(TAG, "useBubbleBoostCard err");
            Log.printStackTrace(TAG, th);
        }
    }

    private void useEnergyRainChanceCard() {
        try {
            if (Status.hasFlagToday("AntForest::useEnergyRainChanceCard")) {
                return;
            }
            // 背包查找 限时能量雨机会
            JSONObject jo = findPropBag(queryPropList(), "LIMIT_TIME_ENERGY_RAIN_CHANCE");
            // 活力值商店兑换
            if (jo == null) {
                JSONObject skuInfo = Vitality.findSkuInfoBySkuName("能量雨次卡");
                if (skuInfo == null) {
                    return;
                }
                String skuId = skuInfo.getString("skuId");
                if (Status.canVitalityExchangeToday(skuId, 1) && Vitality.VitalityExchange(skuInfo.getString("spuId"), skuId, "限时能量雨机会")) {
                    jo = findPropBag(queryPropList(), "LIMIT_TIME_ENERGY_RAIN_CHANCE");
                }
            }
            // 使用 道具
            if (jo != null && usePropBag(jo)) {
                Status.setFlagToday("AntForest::useEnergyRainChanceCard");
                GlobalThreadPools.sleep(500);
                EnergyRain.startEnergyRain();
            }
        } catch (Throwable th) {
            Log.runtime(TAG, "useEnergyRainChanceCard err");
            Log.printStackTrace(TAG, th);
        }
    }

    /**
     * 使用炸弹卡道具
     * 功能：对有保护罩的好友使用，可以破坏其保护罩并收取能量
     * 注意：与保护罩功能冲突，通常二选一使用
     * 
     * @param bagObject 背包的JSON对象
     */
    private void useEnergyBombCard(JSONObject bagObject) {
        try {
            Log.runtime(TAG, "尝试使用炸弹卡...");
            JSONObject jo = findPropBag(bagObject, "ENERGY_BOMB_CARD");
            if (jo == null) {
                Log.runtime(TAG, "背包中没有炸弹卡，尝试兑换...");
                JSONObject skuInfo = Vitality.findSkuInfoBySkuName("能量炸弹卡");
                if (skuInfo == null) {
                    Log.runtime(TAG, "活力值商店中未找到炸弹卡。");
                    return;
                }

                String skuId = skuInfo.getString("skuId");
                if (Status.canVitalityExchangeToday(skuId, 1)) {
                    if (Vitality.VitalityExchange(skuInfo.getString("spuId"), skuId, "能量炸弹卡")) {
                        jo = findPropBag(queryPropList(), "ENERGY_BOMB_CARD");
                    }
                } else {
                    Log.runtime(TAG, "今日炸弹卡兑换次数已达上限。");
                }
            }

            if (jo != null) {
                Log.runtime(TAG, "找到炸弹卡，准备使用: " + jo);
                if (usePropBag(jo)) {
                    // 使用成功后刷新真实结束时间
                    updateSelfHomePage();
                    Log.runtime(TAG, "能量炸弹卡使用成功，已刷新结束时间");
                }
            } else {
                Log.runtime(TAG, "背包中未找到任何可用炸弹卡。");
                updateSelfHomePage();
            }
        } catch (Throwable th) {
            Log.error(TAG + "useEnergyBombCard err");
            Log.printStackTrace(th);
        }
    }

    /**
     * 收取状态的枚举类型
     */
    public enum CollectStatus {AVAILABLE, WAITING, INSUFFICIENT, ROBBED}


    /**
     * 统一获取和缓存用户名的方法
     * @param userId 用户ID
     * @param userHomeObj 用户主页对象（可选）
     * @param fromTag 来源标记（可选）
     * @return 用户名
     */
    private String getAndCacheUserName(String userId, JSONObject userHomeObj, String fromTag) {
        // 1. 尝试从缓存获取
        String userName = cacheCollectedMap.get(userId);
        if (userName != null && !userName.equals(userId)) { // 如果缓存的不是userId本身
            return userName;
        }
        
        // 2. 根据上下文解析
        userName = resolveUserNameFromContext(userId, userHomeObj, fromTag);
        // 3. Fallback处理
        if (userName == null || userName.isEmpty()) {
            userName = userId;
        }
        
        // 4. 存入缓存
        cacheCollectedMap.put(userId, userName);
        return userName;
    }

    /**
     * 统一获取用户名的简化方法（无上下文）
     */
    private String getAndCacheUserName(String userId) {
        return getAndCacheUserName(userId, null, null);
    }
    
    /**
     * 通用错误处理器
     * @param operation 操作名称
     * @param throwable 异常对象
     */
    private void handleException(String operation, Throwable throwable) {
        if (throwable instanceof JSONException) {
            Log.error(TAG, operation + " JSON解析错误: " + throwable.getMessage());
        } else {
            Log.error(TAG, operation + " 错误: " + throwable.getMessage());
        }
        Log.printStackTrace(TAG, throwable);
    }


    /**
         * 道具使用配置类
         */
        private record PropConfig(String propName, String[] propTypes,
                                  java.util.function.Supplier<Boolean> condition,
                                  java.util.function.Supplier<Boolean> exchangeFunction,
                                  java.util.function.Consumer<Long> endTimeUpdater) {
    }
    
    /**
     * 通用道具使用模板方法
     *
     * @param bagObject    背包对象
     * @param config       道具配置
     * @param constantMode 是否开启永动机模式
     */
    private void usePropTemplate(JSONObject bagObject, PropConfig config, boolean constantMode) {
        try {
            if (config.condition != null && !config.condition.get()) {
                Log.runtime(TAG, "不满足使用" + config.propName + "的条件");
                return;
            }
            Log.runtime(TAG, "尝试使用" + config.propName + "...");
            // 按优先级查找道具
            JSONObject propObj = null;
            for (String propType : config.propTypes) {
                propObj = findPropBag(bagObject, propType);
                if (propObj != null) break;
            }
            // 如果背包中没有道具且开启永动机，尝试兑换
            if (propObj == null && constantMode && config.exchangeFunction != null) {
                Log.runtime(TAG, "背包中没有" + config.propName + "，尝试兑换...");
                if (config.exchangeFunction.get()) {
                    // 重新查找兑换后的道具
                    for (String propType : config.propTypes) {
                        propObj = findPropBag(queryPropList(), propType);
                        if (propObj != null) break;
                    }
                }
            }
            if (propObj != null) {
                Log.runtime(TAG, "找到" + config.propName + "，准备使用: " + propObj);
                if (usePropBag(propObj)) {
                    if (config.endTimeUpdater != null) {
                        config.endTimeUpdater.accept(System.currentTimeMillis());
                    }
                }
            } else {
                Log.runtime(TAG, "背包中未找到任何可用的" + config.propName);
                updateSelfHomePage();
            }
        } catch (Throwable th) {
            handleException("use" + config.propName, th);
        }
    }


    /**
     * 从上下文中解析用户名
     */
    private String resolveUserNameFromContext(String userId, JSONObject userHomeObj, String fromTag) {
        String userName = null;
        
        if ("pk".equals(fromTag) && userHomeObj != null) {
            JSONObject userEnergy = userHomeObj.optJSONObject("userEnergy");
            if (userEnergy != null) {
                userName = "PK榜好友|" + userEnergy.optString("displayName");
            }
        } else {
            userName = UserMap.getMaskName(userId);
            if ((userName == null || userName.equals(userId)) && userHomeObj != null) {
                JSONObject userEnergy = userHomeObj.optJSONObject("userEnergy");
                if (userEnergy != null) {
                    String displayName = userEnergy.optString("displayName");
                    if (!displayName.isEmpty()) {
                        userName = displayName;
                    }
                }
            }
        }
        
        return userName;
    }
}

