package fansirsqi.xposed.sesame.task.antFarm;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import fansirsqi.xposed.sesame.entity.AlipayUser;
import fansirsqi.xposed.sesame.entity.MapperEntity;
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
import fansirsqi.xposed.sesame.task.AnswerAI.AnswerAI;
import fansirsqi.xposed.sesame.task.ModelTask;
import fansirsqi.xposed.sesame.task.TaskCommon;
import fansirsqi.xposed.sesame.util.JsonUtil;
import fansirsqi.xposed.sesame.util.Log;
import fansirsqi.xposed.sesame.util.Maps.UserMap;
import fansirsqi.xposed.sesame.util.RandomUtil;
import fansirsqi.xposed.sesame.util.ResUtil;
import fansirsqi.xposed.sesame.data.Status;
import fansirsqi.xposed.sesame.util.StringUtil;
import fansirsqi.xposed.sesame.util.ThreadUtil;
import fansirsqi.xposed.sesame.util.TimeUtil;
public class AntFarm extends ModelTask {
    private static final String TAG = AntFarm.class.getSimpleName();
    private String ownerFarmId;
    private String userId;
    private Animal[] animals;
    private Animal ownerAnimal = new Animal();
    private int foodStock;
    private int foodStockLimit;
    private String rewardProductNum;
    private RewardFriend[] rewardList;
    private double benevolenceScore;
    private double harvestBenevolenceScore;
    private int unreceiveTaskAward = 0;
    private double finalScore = 0d;
    private String familyGroupId;
    private FarmTool[] farmTools;
    private static final List<String> bizKeyList;
    static {
        bizKeyList = new ArrayList<>();
        bizKeyList.add("ADD_GONGGE_NEW");
        bizKeyList.add("USER_STARVE_PUSH");
        bizKeyList.add("YEB_PURCHASE");
        bizKeyList.add("WIDGET_addzujian");//添加庄园小组件
        bizKeyList.add("HIRE_LOW_ACTIVITY");//雇佣小鸡拿饲料
        bizKeyList.add("DIANTAOHUANDUAN");//去点淘逛一逛
        bizKeyList.add("TAO_GOLDEN_V2");//去逛一逛淘金币小镇
        bizKeyList.add("SHANGYEHUA_90_1");//去杂货铺逛一逛
        bizKeyList.add("TAOBAO_tab2gzy");// 去逛一逛淘宝视频
        bizKeyList.add("YITAO_appgyg");// 去一淘APP逛逛
        bizKeyList.add("ANTFARM_chouchoule");// 【抽抽乐】好运装扮来袭！
        bizKeyList.add("TB_qiandao2023");// 去淘宝签到逛一逛
        bizKeyList.add("BABAFARM_TB");// 去逛一逛淘宝芭芭农场
        bizKeyList.add("TB_chongzhi");// 逛一逛小羊农场
        bizKeyList.add("ALIPAIMAI_gygzy");// 逛一逛淘花岛
        bizKeyList.add("BABA_FARM_SPREAD_MANURE");// 去芭芭农场给果树施肥
        bizKeyList.add("ELM_hudong2024");// 去饿了么游乐园逛一逛
        bizKeyList.add("2024XIANYU_huanduan");// 去闲鱼逛一逛
        bizKeyList.add("JINGTAN_FEED_FISH");// 去鲸探喂鱼集福气
        bizKeyList.add("UC_gygzy");// 逛一逛UC浏览器
        bizKeyList.add("TAOBAO_renshenggyg");// 去淘宝人生逛一逛
        bizKeyList.add("TOUTIAO_daoduan");// 去今日头条极速版逛一逛
        bizKeyList.add("CAINIAOGUOGUO2023V2");// 去菜鸟公益
        bizKeyList.add("XJLYKBX1_s190");// 去开游戏宝箱（不确定有效性）
        bizKeyList.add("25WFYX_guaiwulianmeng");// 去怪物联盟（不确定有效性）
        bizKeyList.add("SLEEP");// 让小鸡去睡觉
    }
    @Override
    public String getName() {
        return "庄园";
    }
    @Override
    public ModelGroup getGroup() {
        return ModelGroup.FARM;
    }
    @Override
    public String getIcon() {
        return "AntFarm.png";
    }
    /**
     * 小鸡睡觉时间
     */
    private StringModelField sleepTime;
    /**
     * 小鸡睡觉时长
     */
    private IntegerModelField sleepMinutes;
    /**
     * 自动喂鸡
     */
    private BooleanModelField feedAnimal;
    /**
     * 打赏好友
     */
    private BooleanModelField rewardFriend;
    /**
     * 遣返小鸡
     */
    private BooleanModelField sendBackAnimal;
    /**
     * 遣返方式
     */
    private ChoiceModelField sendBackAnimalWay;
    /**
     * 遣返动作
     */
    private ChoiceModelField sendBackAnimalType;
    /**
     * 遣返好友列表
     */
    private SelectModelField sendBackAnimalList;
    /**
     * 召回小鸡
     */
    private ChoiceModelField recallAnimalType;
    /**
     * s收取道具奖励
     */
    private BooleanModelField receiveFarmToolReward;
    /**
     * 游戏改分
     */
    private BooleanModelField recordFarmGame;
    /**
     * 小鸡游戏时间
     */
    private ListModelField.ListJoinCommaToStringModelField farmGameTime;
    /**
     * 小鸡厨房
     */
    private BooleanModelField kitchen;
    /**
     * 使用特殊食品
     */
    private BooleanModelField useSpecialFood;
    private BooleanModelField useNewEggCard;
    private BooleanModelField harvestProduce;
    private BooleanModelField donation;
    private ChoiceModelField donationCount;
    private BooleanModelField answerQuestion;
    private BooleanModelField receiveFarmTaskAward;
    private BooleanModelField useAccelerateTool;
    private BooleanModelField useAccelerateToolContinue;
    private BooleanModelField useAccelerateToolWhenMaxEmotion;
    private SelectAndCountModelField feedFriendAnimalList;
    private BooleanModelField notifyFriend;
    private ChoiceModelField notifyFriendType;
    private SelectModelField notifyFriendList;
    private BooleanModelField acceptGift;
    private SelectAndCountModelField visitFriendList;
    private BooleanModelField chickenDiary;
    private BooleanModelField enableChouchoule;
    private BooleanModelField listOrnaments;
    private BooleanModelField hireAnimal;
    private ChoiceModelField hireAnimalType;
    private SelectModelField hireAnimalList;
    private BooleanModelField enableDdrawGameCenterAward;
    private BooleanModelField getFeed;
    private SelectModelField getFeedlList;
    private ChoiceModelField getFeedType;
    private BooleanModelField family;
    private SelectModelField familyOptions;
    private SelectModelField inviteFriendVisitFamily;
    private StringModelField giftFamilyDrawFragment;
    @Override
    public ModelFields getFields() {
        ModelFields modelFields = new ModelFields();
        modelFields.addField(sleepTime = new StringModelField("sleepTime", "小鸡睡觉时间(关闭:-1)", "2001"));
        modelFields.addField(sleepMinutes = new IntegerModelField("sleepMinutes", "小鸡睡觉时长(分钟)", 10 * 59, 1, 10 * 60));
        modelFields.addField(recallAnimalType = new ChoiceModelField("recallAnimalType", "召回小鸡", RecallAnimalType.ALWAYS, RecallAnimalType.nickNames));
        modelFields.addField(rewardFriend = new BooleanModelField("rewardFriend", "打赏好友", false));
        modelFields.addField(feedAnimal = new BooleanModelField("feedAnimal", "自动喂小鸡", false));
        modelFields.addField(feedFriendAnimalList = new SelectAndCountModelField("feedFriendAnimalList", "喂小鸡好友列表", new LinkedHashMap<>(), AlipayUser::getList));
        modelFields.addField(getFeed = new BooleanModelField("getFeed", "一起拿饲料", false));
        modelFields.addField(getFeedType = new ChoiceModelField("getFeedType", "一起拿饲料 | 动作", GetFeedType.GIVE, GetFeedType.nickNames));
        modelFields.addField(getFeedlList = new SelectModelField("getFeedlList", "一起拿饲料 | 好友列表", new LinkedHashSet<>(), AlipayUser::getList));
        modelFields.addField(acceptGift = new BooleanModelField("acceptGift", "收麦子", false));
        modelFields.addField(visitFriendList = new SelectAndCountModelField("visitFriendList", "送麦子好友列表", new LinkedHashMap<>(), AlipayUser::getList));
        modelFields.addField(hireAnimal = new BooleanModelField("hireAnimal", "雇佣小鸡 | 开启", false));
        modelFields.addField(hireAnimalType = new ChoiceModelField("hireAnimalType", "雇佣小鸡 | 动作", HireAnimalType.DONT_HIRE, HireAnimalType.nickNames));
        modelFields.addField(hireAnimalList = new SelectModelField("hireAnimalList", "雇佣小鸡 | 好友列表", new LinkedHashSet<>(), AlipayUser::getList));
        modelFields.addField(sendBackAnimal = new BooleanModelField("sendBackAnimal", "遣返 | 开启", false));
        modelFields.addField(sendBackAnimalWay = new ChoiceModelField("sendBackAnimalWay", "遣返 | 方式", SendBackAnimalWay.NORMAL, SendBackAnimalWay.nickNames));
        modelFields.addField(sendBackAnimalType = new ChoiceModelField("sendBackAnimalType", "遣返 | 动作", SendBackAnimalType.NOT_BACK, SendBackAnimalType.nickNames));
        modelFields.addField(sendBackAnimalList = new SelectModelField("dontSendFriendList", "遣返 | 好友列表", new LinkedHashSet<>(), AlipayUser::getList));
        modelFields.addField(notifyFriend = new BooleanModelField("notifyFriend", "通知赶鸡 | 开启", false));
        modelFields.addField(notifyFriendType = new ChoiceModelField("notifyFriendType", "通知赶鸡 | 动作", NotifyFriendType.NOTIFY, NotifyFriendType.nickNames));
        modelFields.addField(notifyFriendList = new SelectModelField("notifyFriendList", "通知赶鸡 | 好友列表", new LinkedHashSet<>(), AlipayUser::getList));
        modelFields.addField(donation = new BooleanModelField("donation", "每日捐蛋 | 开启", false));
        modelFields.addField(donationCount = new ChoiceModelField("donationCount", "每日捐蛋 | 次数", DonationCount.ONE, DonationCount.nickNames));
        modelFields.addField(useAccelerateTool = new BooleanModelField("useAccelerateTool", "加速卡 | 使用", false));
        modelFields.addField(useAccelerateToolContinue = new BooleanModelField("useAccelerateToolContinue", "加速卡 | 连续使用", false));
        modelFields.addField(useAccelerateToolWhenMaxEmotion = new BooleanModelField("useAccelerateToolWhenMaxEmotion", "加速卡 | 仅在满状态时使用", false));
        modelFields.addField(useSpecialFood = new BooleanModelField("useSpecialFood", "使用特殊食品", false));
        modelFields.addField(useNewEggCard = new BooleanModelField("useNewEggCard", "使用新蛋卡", false));
        modelFields.addField(receiveFarmTaskAward = new BooleanModelField("receiveFarmTaskAward", "收取饲料奖励", false));
        modelFields.addField(receiveFarmToolReward = new BooleanModelField("receiveFarmToolReward", "收取道具奖励", false));
        modelFields.addField(harvestProduce = new BooleanModelField("harvestProduce", "收获爱心鸡蛋", false));
        modelFields.addField(kitchen = new BooleanModelField("kitchen", "小鸡厨房", false));
        modelFields.addField(chickenDiary = new BooleanModelField("chickenDiary", "小鸡日记", false));
        modelFields.addField(enableChouchoule = new BooleanModelField("enableChouchoule", "开启小鸡抽抽乐", false));
        modelFields.addField(listOrnaments = new BooleanModelField("listOrnaments", "小鸡每日换装", false));
        modelFields.addField(enableDdrawGameCenterAward = new BooleanModelField("enableDdrawGameCenterAward", "开宝箱", false));
        modelFields.addField(answerQuestion = new BooleanModelField("answerQuestion", "开启答题", false));
        modelFields.addField(recordFarmGame = new BooleanModelField("recordFarmGame", "游戏改分(星星球、登山赛、飞行赛、揍小鸡)", false));
        List<String> farmGameTimeList = new ArrayList<>();
        farmGameTimeList.add("2200-2400");
        modelFields.addField(farmGameTime = new ListModelField.ListJoinCommaToStringModelField("farmGameTime", "小鸡游戏时间(范围)", farmGameTimeList));
        modelFields.addField(family = new BooleanModelField("family", "家庭 | 开启", false));
        modelFields.addField(familyOptions = new SelectModelField("familyOptions", "家庭 | 选项", new LinkedHashSet<>(), AntFarmFamilyOption::getAntFarmFamilyOptions));
        modelFields.addField(inviteFriendVisitFamily = new SelectModelField("inviteFriendVisitFamily", "家庭 | 好友分享列表", new LinkedHashSet<>(), AlipayUser::getList));
        modelFields.addField(giftFamilyDrawFragment = new StringModelField("giftFamilyDrawFragment", "家庭 | 扭蛋碎片赠送用户ID(配置目录查看)", ""));
        return modelFields;
    }
    @Override
    public void boot(ClassLoader classLoader) {
        super.boot(classLoader);
        RpcIntervalLimit.addIntervalLimit("com.alipay.antfarm.enterFarm", 2000);
    }
    @Override
    public Boolean check() {
        if (TaskCommon.IS_ENERGY_TIME){
            Log.record("⏰ 当前为只收能量时间【"+ BaseModel.getEnergyTime().getValue() +"】，停止执行" + getName() + "任务！");
            return false;
        }else if (TaskCommon.IS_MODULE_SLEEP_TIME) {
            Log.record("⏰ 模块休眠时间【"+ BaseModel.getModelSleepTime().getValue() +"】停止执行" + getName() + "任务！");
            return false;
        } else {
            return true;
        }
    }
    @Override
    public void run() {
        try {
            Log.record("执行开始-蚂蚁" + getName());
            if (enterFarm() == null) {
                return;
            }
            listFarmTool();
            if (rewardFriend.getValue()) {
                rewardFriend();
            }
            if (sendBackAnimal.getValue()) {
                sendBackAnimal();
            }
            if (!AnimalInteractStatus.HOME.name().equals(ownerAnimal.animalInteractStatus)) {
                if ("ORCHARD".equals(ownerAnimal.locationType)) {
                    Log.farm("庄园通知📣[你家的小鸡给拉去除草了！]");
                    JSONObject joRecallAnimal = new JSONObject(AntFarmRpcCall
                            .orchardRecallAnimal(ownerAnimal.animalId, ownerAnimal.currentFarmMasterUserId));
                    int manureCount = joRecallAnimal.getInt("manureCount");
                    Log.farm("召回小鸡📣[收获:肥料" + manureCount + "g]");
                } else {
                    syncAnimalStatusAtOtherFarm(ownerAnimal.currentFarmId);
                    boolean guest = false;
                    switch (SubAnimalType.valueOf(ownerAnimal.subAnimalType)) {
                        case GUEST:
                            guest = true;
                            Log.record("小鸡到好友家去做客了");
                            break;
                        case NORMAL:
                            Log.record("小鸡太饿，离家出走了");
                            break;
                        case PIRATE:
                            Log.record("小鸡外出探险了");
                            break;
                        case WORK:
                            Log.record("小鸡出去工作啦");
                            break;
                        default:
                            Log.record("小鸡不在庄园" + " " + ownerAnimal.subAnimalType);
                    }
                    boolean hungry = false;
                    String userName = UserMap
                            .getMaskName(AntFarmRpcCall.farmId2UserId(ownerAnimal.currentFarmId));
                    switch (AnimalFeedStatus.valueOf(ownerAnimal.animalFeedStatus)) {
                        case HUNGRY:
                            hungry = true;
                            Log.record("小鸡在[" + userName + "]的庄园里挨饿");
                            break;
                        case EATING:
                            Log.record("小鸡在[" + userName + "]的庄园里吃得津津有味");
                            break;
                    }
                    boolean recall = switch (recallAnimalType.getValue()) {
                        case RecallAnimalType.ALWAYS -> true;
                        case RecallAnimalType.WHEN_THIEF -> !guest;
                        case RecallAnimalType.WHEN_HUNGRY -> hungry;
                        default -> false;
                    };
                    if (recall) {
                        recallAnimal(ownerAnimal.animalId, ownerAnimal.currentFarmId, ownerFarmId, userName);
                        syncAnimalStatus(ownerFarmId);
                    }
                }
            }
            if (receiveFarmToolReward.getValue()) {
                receiveToolTaskReward();
            }
            if (recordFarmGame.getValue()) {
                for (String time : farmGameTime.getValue()) {
                    if (TimeUtil.checkNowInTimeRange(time)) {
                        recordFarmGame(GameType.starGame);
                        recordFarmGame(GameType.jumpGame);
                        recordFarmGame(GameType.flyGame);
                        recordFarmGame(GameType.hitGame);
                        break;
                    }
                }
            }
            if (kitchen.getValue()) {
                collectDailyFoodMaterial(userId);
                collectDailyLimitedFoodMaterial();
                cook(userId);
            }
            if (chickenDiary.getValue()) {
                queryChickenDiaryList();
            }
            if (useNewEggCard.getValue()) {
                useFarmTool(ownerFarmId, ToolType.NEWEGGTOOL);
                syncAnimalStatus(ownerFarmId);
            }
            if (harvestProduce.getValue() && benevolenceScore >= 1) {
                Log.record("有可收取的爱心鸡蛋");
                harvestProduce(ownerFarmId);
            }
            if (donation.getValue() && Status.canDonationEgg(userId) && harvestBenevolenceScore >= 1) {
                handleDonation(donationCount.getValue());
            }
            if (answerQuestion.getValue() && Status.canAnswerQuestionToday()) {
                answerQuestion();
            }
            if (receiveFarmTaskAward.getValue()) {
                doFarmDailyTask();
                receiveFarmTaskAward();
            }
            if (AnimalInteractStatus.HOME.name().equals(ownerAnimal.animalInteractStatus)) {
                boolean needReload = false;
                if (feedAnimal.getValue() && AnimalFeedStatus.HUNGRY.name().equals(ownerAnimal.animalFeedStatus)) {
                    Log.record("小鸡在挨饿");
                    if (feedAnimal(ownerFarmId)) {
                        needReload = true;
                    }
                }
                if (useAccelerateTool.getValue() && !AnimalFeedStatus.HUNGRY.name().equals(ownerAnimal.animalFeedStatus)) {
                    if (useAccelerateTool()) {
                        needReload = true;
                    }
                }
                if (needReload) {
                    enterFarm();
                    syncAnimalStatus(ownerFarmId);
                }
                autoFeedAnimal();
                // 小鸡换装
                if (listOrnaments.getValue() && Status.canOrnamentToday()) {
                    listOrnaments();
                }
                if (unreceiveTaskAward > 0) {
                    Log.record("还有待领取的饲料");
                    receiveFarmTaskAward();
                }
            }
            // 到访小鸡送礼
            visitAnimal();
            // 送麦子
            visit();
            // 帮好友喂鸡
            feedFriend();
            // 通知好友赶鸡
            if (notifyFriend.getValue()) {
                notifyFriend();
            }
            // 抽抽乐
            if (enableChouchoule.getValue()) {
                chouChouLe ccl = new chouChouLe();
                ccl.chouchoule();
            }
            // 雇佣小鸡
            if (hireAnimal.getValue()) {
                hireAnimal();
            }
            if (getFeed.getValue()) {
                letsGetChickenFeedTogether();
            }
            //家庭
            if (family.getValue()) {
                family();
            }
            // 开宝箱
            if (enableDdrawGameCenterAward.getValue()) {
                drawGameCenterAward();
            }
            //小鸡睡觉&起床
            animalSleepAndWake();
        } catch (Throwable t) {
            Log.runtime(TAG, "AntFarm.start.run err:");
            Log.printStackTrace(TAG, t);
        } finally {
            Log.record("执行结束-蚂蚁" + getName());
        }
    }
    private void animalSleepAndWake() {
        try {
            String sleepTimeStr = sleepTime.getValue();
            if ("-1".equals(sleepTimeStr)) {
                Log.runtime("当前已关闭小鸡睡觉");
                return;
            }
            Calendar now = TimeUtil.getNow();
            Calendar animalSleepTimeCalendar = TimeUtil.getTodayCalendarByTimeStr(sleepTimeStr);
            if (animalSleepTimeCalendar == null) {
                Log.record("小鸡睡觉时间格式错误，请重新设置");
                return;
            }
            Integer sleepMinutesInt = sleepMinutes.getValue();
            Calendar animalWakeUpTimeCalendar = (Calendar) animalSleepTimeCalendar.clone();
            animalWakeUpTimeCalendar.add(Calendar.MINUTE, sleepMinutesInt);
            long animalSleepTime = animalSleepTimeCalendar.getTimeInMillis();
            long animalWakeUpTime = animalWakeUpTimeCalendar.getTimeInMillis();
            if (animalSleepTime > animalWakeUpTime) {
                Log.record("小鸡睡觉设置有误，请重新设置");
                return;
            }
            boolean afterSleepTime = now.compareTo(animalSleepTimeCalendar) > 0;
            boolean afterWakeUpTime = now.compareTo(animalWakeUpTimeCalendar) > 0;
            if (afterSleepTime && afterWakeUpTime) {
                if (!Status.canAnimalSleep()) {
                    return;
                }
                Log.record("已错过小鸡今日睡觉时间");
                return;
            }
            String sleepTaskId = "AS|" + animalSleepTime;
            String wakeUpTaskId = "AW|" + animalWakeUpTime;
            if (!hasChildTask(sleepTaskId) && !afterSleepTime) {
                addChildTask(new ChildModelTask(sleepTaskId, "AS", this::animalSleepNow, animalSleepTime));
                Log.record("添加定时睡觉🛌[" + UserMap.getCurrentMaskName() + "]在[" + TimeUtil.getCommonDate(animalSleepTime) + "]执行");
            }
            if (!hasChildTask(wakeUpTaskId) && !afterWakeUpTime) {
                addChildTask(new ChildModelTask(wakeUpTaskId, "AW", this::animalWakeUpNow, animalWakeUpTime));
                Log.record("添加定时起床🛌[" + UserMap.getCurrentMaskName() + "]在[" + TimeUtil.getCommonDate(animalWakeUpTime) + "]执行");
            }
            if (afterSleepTime) {
                if (Status.canAnimalSleep()) {
                    animalSleepNow();
                }
            }
        } catch (Exception e) {
            Log.runtime(TAG, "animalSleepAndWake err:");
            Log.printStackTrace(e);
        }
    }
    private JSONObject enterFarm() {
        try {
            String s = AntFarmRpcCall.enterFarm("", UserMap.getCurrentUid());
            if (s == null) {
                throw new RuntimeException("庄园加载失败");
            }
            JSONObject jo = new JSONObject(s);
            if ("SUCCESS".equals(jo.getString("memo"))) {
                rewardProductNum = jo.getJSONObject("dynamicGlobalConfig").getString("rewardProductNum");
                JSONObject joFarmVO = jo.getJSONObject("farmVO");
                foodStock = joFarmVO.getInt("foodStock");
                foodStockLimit = joFarmVO.getInt("foodStockLimit");
                harvestBenevolenceScore = joFarmVO.getDouble("harvestBenevolenceScore");
                parseSyncAnimalStatusResponse(joFarmVO.toString());
                userId = joFarmVO.getJSONObject("masterUserInfoVO").getString("userId");
                familyGroupId = getFamilyGroupId(userId);
                if (useSpecialFood.getValue()) {
                    JSONArray cuisineList = jo.getJSONArray("cuisineList");
                    if (!AnimalFeedStatus.SLEEPY.name().equals(ownerAnimal.animalFeedStatus))
                        useFarmFood(cuisineList);
                }
                if (jo.has("lotteryPlusInfo")) {
                    drawLotteryPlus(jo.getJSONObject("lotteryPlusInfo"));
                }
                if (acceptGift.getValue() && joFarmVO.getJSONObject("subFarmVO").has("giftRecord")
                        && foodStockLimit - foodStock >= 10) {
                    acceptGift();
                }
                return jo;
            } else {
                Log.record(s);
            }
        } catch (Exception e) {
            Log.printStackTrace(e);
        }
        return null;
    }
    private void autoFeedAnimal() {
        if (feedAnimal.getValue()) {
            try {
                Long startEatTime = ownerAnimal.startEatTime;
                double allFoodHaveEatten = 0d;
                double allConsumeSpeed = 0d;
                for (Animal animal : animals) {
                    allFoodHaveEatten += animal.foodHaveEatten;
                    allConsumeSpeed += animal.consumeSpeed;
                }
                long nextFeedTime = startEatTime + (long) ((180 - (allFoodHaveEatten)) / (allConsumeSpeed)) * 1000;
                String taskId = "FA|" + ownerFarmId;
                if (!hasChildTask(taskId)) {
                    addChildTask(new ChildModelTask(taskId, "FA", () -> feedAnimal(ownerFarmId), nextFeedTime));
                    Log.record("添加蹲点投喂🥣[" + UserMap.getCurrentMaskName() + "]在[" + TimeUtil.getCommonDate(nextFeedTime) + "]执行");
                } else {
                    addChildTask(new ChildModelTask(taskId, "FA", () -> feedAnimal(ownerFarmId), nextFeedTime));
                }
            } catch (Exception e) {
                Log.printStackTrace(e);
            }
        }
    }
    private void animalSleepNow() {
        try {
            String s = AntFarmRpcCall.queryLoveCabin(UserMap.getCurrentUid());
            JSONObject jo = new JSONObject(s);
            if ("SUCCESS".equals(jo.getString("memo"))) {
                JSONObject sleepNotifyInfo = jo.getJSONObject("sleepNotifyInfo");
                if (sleepNotifyInfo.optBoolean("canSleep", false)) {
                    s = AntFarmRpcCall.sleep();
                    jo = new JSONObject(s);
                    if ("SUCCESS".equals(jo.getString("memo"))) {
                        Log.farm("小鸡睡觉🛌");
                        Status.animalSleep();
                    }
                } else {
                    Log.farm("小鸡无需睡觉🛌");
                }
            }
        } catch (Throwable t) {
            Log.runtime(TAG, "animalSleepNow err:");
            Log.printStackTrace(t);
        }
    }
    private void animalWakeUpNow() {
        try {
            String s = AntFarmRpcCall.queryLoveCabin(UserMap.getCurrentUid());
            JSONObject jo = new JSONObject(s);
            if ("SUCCESS".equals(jo.getString("memo"))) {
                JSONObject sleepNotifyInfo = jo.getJSONObject("sleepNotifyInfo");
                if (!sleepNotifyInfo.optBoolean("canSleep", true)) {
                    s = AntFarmRpcCall.wakeUp();
                    jo = new JSONObject(s);
                    if ("SUCCESS".equals(jo.getString("memo"))) {
                        Log.farm("小鸡起床\uD83D\uDD06");
                    }
                } else {
                    Log.farm("小鸡无需起床\uD83D\uDD06");
                }
            }
        } catch (Throwable t) {
            Log.runtime(TAG, "animalWakeUpNow err:");
            Log.printStackTrace(t);
        }
    }
    private void syncAnimalStatus(String farmId) {
        try {
            String s = AntFarmRpcCall.syncAnimalStatus(farmId);
            parseSyncAnimalStatusResponse(s);
        } catch (Throwable t) {
            Log.runtime(TAG, "syncAnimalStatus err:");
            Log.printStackTrace(TAG, t);
        }
    }
    private void syncAnimalStatusAtOtherFarm(String farmId) {
        try {
            String s = AntFarmRpcCall.enterFarm(farmId, "");
            JSONObject jo = new JSONObject(s);
            jo = jo.getJSONObject("farmVO").getJSONObject("subFarmVO");
            JSONArray jaAnimals = jo.getJSONArray("animals");
            for (int i = 0; i < jaAnimals.length(); i++) {
                jo = jaAnimals.getJSONObject(i);
                if (jo.getString("masterFarmId").equals(ownerFarmId)) {
                    Animal newOwnerAnimal = new Animal();
                    JSONObject animal = jaAnimals.getJSONObject(i);
                    newOwnerAnimal.animalId = animal.getString("animalId");
                    newOwnerAnimal.currentFarmId = animal.getString("currentFarmId");
                    newOwnerAnimal.currentFarmMasterUserId = animal.getString("currentFarmMasterUserId");
                    newOwnerAnimal.masterFarmId = ownerFarmId;
                    newOwnerAnimal.animalBuff = animal.getString("animalBuff");
                    newOwnerAnimal.locationType = animal.optString("locationType", "");
                    newOwnerAnimal.subAnimalType = animal.getString("subAnimalType");
                    animal = animal.getJSONObject("animalStatusVO");
                    newOwnerAnimal.animalFeedStatus = animal.getString("animalFeedStatus");
                    newOwnerAnimal.animalInteractStatus = animal.getString("animalInteractStatus");
                    ownerAnimal = newOwnerAnimal;
                    break;
                }
            }
        } catch (Throwable t) {
            Log.runtime(TAG, "syncAnimalStatusAtOtherFarm err:");
            Log.printStackTrace(TAG, t);
        }
    }
    private void rewardFriend() {
        try {
            if (rewardList != null) {
                for (RewardFriend rewardFriend : rewardList) {
                    String s = AntFarmRpcCall.rewardFriend(rewardFriend.consistencyKey, rewardFriend.friendId,
                            rewardProductNum, rewardFriend.time);
                    JSONObject jo = new JSONObject(s);
                    String memo = jo.getString("memo");
                    if ("SUCCESS".equals(memo)) {
                        double rewardCount = benevolenceScore - jo.getDouble("farmProduct");
                        benevolenceScore -= rewardCount;
                        Log.farm("打赏好友💰[" + UserMap.getMaskName(rewardFriend.friendId) + "]#得" + rewardCount
                                + "颗爱心鸡蛋");
                    } else {
                        Log.record(memo);
                        Log.runtime(s);
                    }
                }
                rewardList = null;
            }
        } catch (Throwable t) {
            Log.runtime(TAG, "rewardFriend err:");
            Log.printStackTrace(TAG, t);
        }
    }
    private void recallAnimal(String animalId, String currentFarmId, String masterFarmId, String user) {
        try {
            String s = AntFarmRpcCall.recallAnimal(animalId, currentFarmId, masterFarmId);
            JSONObject jo = new JSONObject(s);
            String memo = jo.getString("memo");
            if ("SUCCESS".equals(memo)) {
                double foodHaveStolen = jo.getDouble("foodHaveStolen");
                Log.farm("召回小鸡📣，偷吃[" + user + "]#" + foodHaveStolen + "g");
                // 这里不需要加
                // add2FoodStock((int)foodHaveStolen);
            } else {
                Log.record(memo);
                Log.runtime(s);
            }
        } catch (Throwable t) {
            Log.runtime(TAG, "recallAnimal err:");
            Log.printStackTrace(TAG, t);
        }
    }
    private void sendBackAnimal() {
        if (animals == null) {
            return;
        }
        try {
            for (Animal animal : animals) {
                if (AnimalInteractStatus.STEALING.name().equals(animal.animalInteractStatus)
                        && !SubAnimalType.GUEST.name().equals(animal.subAnimalType)
                        && !SubAnimalType.WORK.name().equals(animal.subAnimalType)) {
                    // 赶鸡
                    String user = AntFarmRpcCall.farmId2UserId(animal.masterFarmId);
                    boolean isSendBackAnimal = sendBackAnimalList.getValue().contains(user);
                    if (sendBackAnimalType.getValue() == SendBackAnimalType.BACK) {
                        isSendBackAnimal = !isSendBackAnimal;
                    }
                    if (isSendBackAnimal) {
                        continue;
                    }
                    int sendTypeInt = sendBackAnimalWay.getValue();
                    user = UserMap.getMaskName(user);
                    String s = AntFarmRpcCall.sendBackAnimal(
                            SendBackAnimalWay.nickNames[sendTypeInt], animal.animalId,
                            animal.currentFarmId, animal.masterFarmId);
                    JSONObject jo = new JSONObject(s);
                    String memo = jo.getString("memo");
                    if ("SUCCESS".equals(memo)) {
                        if (sendTypeInt == SendBackAnimalWay.HIT) {
                            if (jo.has("hitLossFood")) {
                                s = "胖揍小鸡🤺[" + user + "]，掉落[" + jo.getInt("hitLossFood") + "g]";
                                if (jo.has("finalFoodStorage"))
                                    foodStock = jo.getInt("finalFoodStorage");
                            } else
                                s = "[" + user + "]的小鸡躲开了攻击";
                        } else {
                            s = "驱赶小鸡🧶[" + user + "]";
                        }
                        Log.farm(s);
                    } else {
                        Log.record(memo);
                        Log.runtime(s);
                    }
                }
            }
        } catch (Throwable t) {
            Log.runtime(TAG, "sendBackAnimal err:");
            Log.printStackTrace(TAG, t);
        }
    }
    private void receiveToolTaskReward() {
        try {
            String s = AntFarmRpcCall.listToolTaskDetails();
            JSONObject jo = new JSONObject(s);
            String memo = jo.getString("memo");
            if ("SUCCESS".equals(memo)) {
                JSONArray jaList = jo.getJSONArray("list");
                for (int i = 0; i < jaList.length(); i++) {
                    JSONObject joItem = jaList.getJSONObject(i);
                    if (joItem.has("taskStatus")
                            && TaskStatus.FINISHED.name().equals(joItem.getString("taskStatus"))) {
                        JSONObject bizInfo = new JSONObject(joItem.getString("bizInfo"));
                        String awardType = bizInfo.getString("awardType");
                        ToolType toolType = ToolType.valueOf(awardType);
                        boolean isFull = false;
                        for (FarmTool farmTool : farmTools) {
                            if (farmTool.toolType == toolType) {
                                if (farmTool.toolCount == farmTool.toolHoldLimit) {
                                    isFull = true;
                                }
                                break;
                            }
                        }
                        if (isFull) {
                            Log.record("领取道具[" + toolType.nickName() + "]#已满，暂不领取");
                            continue;
                        }
                        int awardCount = bizInfo.getInt("awardCount");
                        String taskType = joItem.getString("taskType");
                        String taskTitle = bizInfo.getString("taskTitle");
                        s = AntFarmRpcCall.receiveToolTaskReward(awardType, awardCount, taskType);
                        jo = new JSONObject(s);
                        memo = jo.getString("memo");
                        if ("SUCCESS".equals(memo)) {
                            Log.farm("领取道具🎖️[" + taskTitle + "-" + toolType.nickName() + "]#" + awardCount + "张");
                        } else {
                            memo = memo.replace("道具", toolType.nickName());
                            Log.record(memo);
                            Log.runtime(s);
                        }
                    }
                }
            } else {
                Log.record(memo);
                Log.runtime(s);
            }
        } catch (Throwable t) {
            Log.runtime(TAG, "receiveToolTaskReward err:");
            Log.printStackTrace(TAG, t);
        }
    }
    private void harvestProduce(String farmId) {
        try {
            String s = AntFarmRpcCall.harvestProduce(farmId);
            JSONObject jo = new JSONObject(s);
            String memo = jo.getString("memo");
            if ("SUCCESS".equals(memo)) {
                double harvest = jo.getDouble("harvestBenevolenceScore");
                harvestBenevolenceScore = jo.getDouble("finalBenevolenceScore");
                Log.farm("收取鸡蛋🥚[" + harvest + "颗]#剩余" + harvestBenevolenceScore + "颗");
            } else {
                Log.record(memo);
                Log.runtime(s);
            }
        } catch (Throwable t) {
            Log.runtime(TAG, "harvestProduce err:");
            Log.printStackTrace(TAG, t);
        }
    }
    /* 捐赠爱心鸡蛋 */
    private void handleDonation(int donationType) {
        try {
            String s = AntFarmRpcCall.listActivityInfo();
            JSONObject jo = new JSONObject(s);
            String memo = jo.getString("memo");
            if ("SUCCESS".equals(memo)) {
                JSONArray jaActivityInfos = jo.getJSONArray("activityInfos");
                String activityId = null, activityName;
                boolean isDonation = false;
                for (int i = 0; i < jaActivityInfos.length(); i++) {
                    jo = jaActivityInfos.getJSONObject(i);
                    if (!jo.get("donationTotal").equals(jo.get("donationLimit"))) {
                        activityId = jo.getString("activityId");
                        activityName = jo.optString("projectName", activityId);
                        if (performDonation(activityId, activityName)) {
                            isDonation = true;
                            if (donationType == DonationCount.ONE) {
                                break;
                            }
                        }
                    }
                }
                if (isDonation) {
                    Status.donationEgg(userId);
                }
                if (activityId == null) {
                    Log.record("今日已无可捐赠的活动");
                }
            } else {
                Log.record(memo);
                Log.runtime(s);
            }
        } catch (Throwable t) {
            Log.runtime(TAG, "donation err:");
            Log.printStackTrace(TAG, t);
        }
    }
    private Boolean performDonation(String activityId, String activityName) {
        try {
            String s = AntFarmRpcCall.donation(activityId, 1);
            JSONObject donationResponse = new JSONObject(s);
            String memo = donationResponse.getString("memo");
            if ("SUCCESS".equals(memo)) {
                JSONObject donationDetails = donationResponse.getJSONObject("donation");
                harvestBenevolenceScore = donationDetails.getDouble("harvestBenevolenceScore");
                Log.farm("捐赠活动❤️[" + activityName + "]#累计捐赠" + donationDetails.getInt("donationTimesStat") + "次");
                return true;
            } else {
                Log.record(memo);
                Log.runtime(s);
            }
        } catch (Throwable t) {
            Log.printStackTrace(t);
        }
        return false;
    }
    private void answerQuestion() {
        try {
            String s = AntFarmRpcCall.listFarmTask();
            JSONObject jo = new JSONObject(s);
            if ("SUCCESS".equals(jo.getString("memo"))) {
                JSONArray jaFarmTaskList = jo.getJSONArray("farmTaskList");
                for (int i = 0; i < jaFarmTaskList.length(); i++) {
                    jo = jaFarmTaskList.getJSONObject(i);
                    if ("庄园小课堂".equals(jo.getString("title"))) {
                        switch (TaskStatus.valueOf((jo.getString("taskStatus")))) {
                            case TODO:
                                s = DadaDailyRpcCall.home("100");
                                jo = new JSONObject(s);
                                if (jo.optBoolean("success")) {
                                    JSONObject question = jo.getJSONObject("question");
                                    Log.runtime("题目:" + question, "");
                                    long questionId = question.getLong("questionId");
                                    JSONArray labels = question.getJSONArray("label");
                                    String answer = null;
                                    String anotherAnswer = null;
                                    boolean existsResult = false;
                                    Set<String> dadaDailySet = Status.getDadaDailySet();
                                    if (dadaDailySet.contains(TimeUtil.getDateStr() + labels.getString(0))) {
                                        answer = labels.getString(0);
                                        anotherAnswer = labels.getString(1);
                                        existsResult = true;
                                    } else if (dadaDailySet.contains(TimeUtil.getDateStr() + labels.getString(1))) {
                                        answer = labels.getString(1);
                                        anotherAnswer = labels.getString(0);
                                        existsResult = true;
                                    }
                                    if (!existsResult) {
                                        answer = AnswerAI.getAnswer(question.getString("title"), JsonUtil.jsonArrayToList(labels));
                                        if (answer == null || answer.isEmpty()) {
                                            answer = labels.getString(0);
                                        }
                                        anotherAnswer = labels.getString(1);
                                    }
                                    s = DadaDailyRpcCall.submit("100", answer, questionId);
                                    JSONObject joDailySubmit = new JSONObject(s);
                                    if (joDailySubmit.optBoolean("success")) {
                                        Log.record("提交完成");
                                        dadaDailySet = new HashSet<>();
                                        JSONObject extInfo = joDailySubmit.getJSONObject("extInfo");
                                        boolean correct = joDailySubmit.getBoolean("correct");
                                        if (!correct || !existsResult) {
                                            dadaDailySet.add(TimeUtil.getDateStr() + anotherAnswer);
                                        } else {
                                            dadaDailySet.add(TimeUtil.getDateStr() + answer);
                                        }
                                        Log.record("答题" + (correct ? "正确" : "错误") + "可领取［"
                                                + extInfo.getString("award") + "克］");
                                        Status.answerQuestionToday();
                                        JSONArray operationConfigList = joDailySubmit
                                                .getJSONArray("operationConfigList");
                                        for (int j = 0; j < operationConfigList.length(); j++) {
                                            JSONObject operationConfig = operationConfigList.getJSONObject(j);
                                            if ("PREVIEW_QUESTION".equals(operationConfig.getString("type"))) {
                                                JSONArray actionTitle = new JSONArray(
                                                        operationConfig.getString("actionTitle"));
                                                for (int k = 0; k < actionTitle.length(); k++) {
                                                    JSONObject joActionTitle = actionTitle.getJSONObject(k);
                                                    if (joActionTitle.getBoolean("correct")) {
                                                        dadaDailySet.add(TimeUtil.getDateStr(1) + joActionTitle.getString("title"));
                                                    }
                                                }
                                            }
                                        }
                                        Status.setDadaDailySet(dadaDailySet);
                                    } else {
                                        Log.runtime(s);
                                    }
                                    return;
                                } else {
                                    Log.runtime(s);
                                }
                                break;
                            case RECEIVED:
                                Log.record("今日答题已完成");
                                Status.answerQuestionToday();
                                break;
                            case FINISHED:
                                Log.record("已经答过题了，饲料待领取");
                                Status.answerQuestionToday();
                                break;
                        }
                        break;
                    }
                }
            } else {
                Log.runtime(s);
            }
        } catch (Throwable t) {
            Log.runtime(TAG, "answerQuestion err:");
            Log.printStackTrace(TAG, t);
        }
    }
    private void recordFarmGame(GameType gameType) {
        try {
            do {
                try {
                    JSONObject jo = new JSONObject(AntFarmRpcCall.initFarmGame(gameType.name()));
                    if ("SUCCESS".equals(jo.getString("memo"))) {
                        if (jo.getJSONObject("gameAward").getBoolean("level3Get")) {
                            return;
                        }
                        if (jo.optInt("remainingGameCount", 1) == 0) {
                            return;
                        }
                        jo = new JSONObject(AntFarmRpcCall.recordFarmGame(gameType.name()));
                        if ("SUCCESS".equals(jo.getString("memo"))) {
                            JSONArray awardInfos = jo.getJSONArray("awardInfos");
                            StringBuilder award = new StringBuilder();
                            for (int i = 0; i < awardInfos.length(); i++) {
                                JSONObject awardInfo = awardInfos.getJSONObject(i);
                                award.append(awardInfo.getString("awardName")).append("*").append(awardInfo.getInt("awardCount"));
                            }
                            if (jo.has("receiveFoodCount")) {
                                award.append(";肥料*").append(jo.getString("receiveFoodCount"));
                            }
                            Log.farm("庄园游戏🎮[" + gameType.gameName() + "]#" + award);
                            if (jo.optInt("remainingGameCount", 0) > 0) {
                                continue;
                            }
                        } else {
                            Log.runtime(TAG, jo.toString());
                        }
                    } else {
                        Log.runtime(TAG, jo.toString());
                    }
                    break;
                } finally {
                    ThreadUtil.sleep(2000);
                }
            } while (true);
        } catch (Throwable t) {
            Log.runtime(TAG, "recordFarmGame err:");
            Log.printStackTrace(TAG, t);
        }
    }
    private void doFarmDailyTask() {
        try {
            String s = AntFarmRpcCall.listFarmTask();
            JSONObject jo = new JSONObject(s);
            if ("SUCCESS".equals(jo.getString("memo"))) {
                JSONArray jaFarmTaskList = jo.getJSONArray("farmTaskList");
                for (int i = 0; i < jaFarmTaskList.length(); i++) {
                    jo = jaFarmTaskList.getJSONObject(i);
                    String title = null;
                    if (jo.has("title"))
                        title = jo.getString("title");
                    if ("TODO".equals(jo.getString("taskStatus"))) {
                        int awardCount = jo.optInt("awardCount");
                        String bizKey = jo.getString("bizKey");
                        if ("VIEW".equals(jo.optString("taskMode")) || bizKeyList.contains(bizKey)) {
//                            Log.debug(TAG+ ",doFarmDailyTask: " + title + " " + bizKey);
                            if (bizKey.equals("HEART_DONATION_ADVANCED_FOOD_V2")) {
                                continue; //为了本项目的长远发展，暂时屏蔽该任务
                            } else {
                                jo = new JSONObject(AntFarmRpcCall.doFarmTask(bizKey));
                            }
                            if ("SUCCESS".equals(jo.getString("memo"))) {
                                Log.farm("庄园任务🧾[" + title + "]#获得饲料" + jo.optString("awardCount") + "g");
                            } else {
                                Log.record(jo.getString("memo"));
                                Log.runtime(jo.toString());
                            }
                        } else if ("庄园小视频".equals(title)) {
                            jo = new JSONObject(AntFarmRpcCall.queryTabVideoUrl());
                            if ("SUCCESS".equals(jo.getString("memo"))) {
                                String videoUrl = jo.getString("videoUrl");
                                String contentId = videoUrl.substring(videoUrl.indexOf("&contentId=") + 1,
                                        videoUrl.indexOf("&refer"));
                                jo = new JSONObject(AntFarmRpcCall.videoDeliverModule(contentId));
                                if (jo.optBoolean("success")) {
                                    ThreadUtil.sleep(15100);
                                    jo = new JSONObject(AntFarmRpcCall.videoTrigger(contentId));
                                    if (jo.optBoolean("success")) {
                                        Log.farm("庄园任务🧾[" + title + "]#获得饲料" + awardCount + "g");
                                    } else {
                                        Log.record(jo.getString("resultMsg"));
                                        Log.runtime(jo.toString());
                                    }
                                } else {
                                    Log.record(jo.getString("resultMsg"));
                                    Log.runtime(jo.toString());
                                }
                            } else {
                                Log.record(jo.getString("memo"));
                                Log.runtime(jo.toString());
                            }
                        }
                    }
                }
            } else {
                Log.record(jo.getString("memo"));
                Log.runtime(s);
            }
        } catch (Throwable t) {
            Log.runtime(TAG, "doFarmDailyTask err:");
            Log.printStackTrace(TAG, t);
        }
    }
    private void receiveFarmTaskAward() {
        try {
            String s = AntFarmRpcCall.listFarmTask();
            JSONObject jo = new JSONObject(s);
            String memo = jo.getString("memo");
            if ("SUCCESS".equals(memo)) {
                JSONObject signList = jo.getJSONObject("signList");
                sign(signList);
                ThreadUtil.sleep(1000);
                JSONArray jaFarmTaskList = jo.getJSONArray("farmTaskList");
                for (int i = 0; i < jaFarmTaskList.length(); i++) {
                    jo = jaFarmTaskList.getJSONObject(i);
                    String taskTitle = null;
                    if (jo.has("title"))
                        taskTitle = jo.getString("title");
                    switch (TaskStatus.valueOf(jo.getString("taskStatus"))) {
                        case TODO:
                            break;
                        case FINISHED:
                            int awardCount = jo.getInt("awardCount");
                            if (Objects.equals(jo.optString("awardType"), "ALLPURPOSE")) {
                                if (awardCount + foodStock > foodStockLimit) {
                                    unreceiveTaskAward++;
                                    //Log.record("领取" + awardCount + "克饲料后将超过[" + foodStockLimit + "克]上限，终止领取");
                                    continue;
                                }
                            }
                            s = AntFarmRpcCall.receiveFarmTaskAward(jo.getString("taskId"));
                            ThreadUtil.sleep(1000);
                            jo = new JSONObject(s);
                            memo = jo.getString("memo");
                            if ("SUCCESS".equals(memo)) {
                                if (Objects.equals(jo.optString("awardType"), "ALLPURPOSE")) {
                                    add2FoodStock(awardCount);
                                    Log.farm("领取奖励🎖️[" + taskTitle + "]#" + awardCount + "g");
                                }
                                if (unreceiveTaskAward > 0)
                                    unreceiveTaskAward--;
                            } else {
                                Log.record(memo);
                                Log.runtime(s);
                            }
                            break;
                        case RECEIVED:
//                            Log.record("今日任务[]已完成");
                            break;
                    }
                }
            } else {
                Log.record(memo);
                Log.runtime(s);
            }
        } catch (Throwable t) {
            Log.runtime(TAG, "receiveFarmTaskAward err:");
            Log.printStackTrace(TAG, t);
        }
    }
    private void sign(JSONObject signList) {
    try {
        JSONArray jaFarmsignList = signList.getJSONArray("signList");
        JSONObject joSignItem = null;

        for (int i = 0; i < jaFarmsignList.length(); i++) {
            JSONObject jo = jaFarmsignList.getJSONObject(i);
            if (TimeUtil.getDateStr().equals(jo.getString("signKey"))) {
                joSignItem = jo;
                break;
            }
        }

        if (joSignItem == null) {
            Log.record("未找到今日签到信息");
            return;
        }

        boolean signed = joSignItem.getBoolean("signed");
        int awardCount = joSignItem.getInt("awardCount");

        if (!signed) {
            String signResponse = AntFarmRpcCall.sign();
            JSONObject joSign = new JSONObject(signResponse);
            String memo = joSign.getString("memo");

            if ("SUCCESS".equals(memo)) {
                Log.farm("庄园签到📅获得饲料" + awardCount + "g");
            } else {
                Log.record("签到失败：" + memo);
                Log.runtime(TAG, signResponse);
            }
        } else {
            Log.record("庄园今日已签到得奖励"+ awardCount + "g");
        }
    } catch (JSONException e) {
        Log.record("JSON解析错误：" + e.getMessage());
        Log.printStackTrace(TAG, e);
    } catch (Exception e) {
        Log.record("其他错误：" + e.getMessage());
        Log.printStackTrace(TAG, e);
    }
}

    private Boolean feedAnimal(String farmId) {
        try {
            if (foodStock < 180) {
                Log.record("喂鸡饲料不足");
            } else {
                String s = AntFarmRpcCall.feedAnimal(farmId);
                ThreadUtil.sleep(500);
                JSONObject jo = new JSONObject(s);
                String memo = jo.getString("memo");
                if ("SUCCESS".equals(memo)) {
                    int feedFood = foodStock - jo.getInt("foodStock");
                    add2FoodStock(-feedFood);
                    Log.farm("投喂小鸡🥣[" + feedFood + "g]#剩余" + foodStock + "g");
                    return true;
                } else {
                    Log.record(memo);
                    Log.runtime(s);
                }
            }
        } catch (Throwable t) {
            Log.runtime(TAG, "feedAnimal err:");
            Log.printStackTrace(TAG, t);
        }
        return false;
    }
    private void listFarmTool() {
        try {
            String s = AntFarmRpcCall.listFarmTool();
            JSONObject jo = new JSONObject(s);
            String memo = jo.getString("memo");
            if ("SUCCESS".equals(memo)) {
                JSONArray jaToolList = jo.getJSONArray("toolList");
                farmTools = new FarmTool[jaToolList.length()];
                for (int i = 0; i < jaToolList.length(); i++) {
                    jo = jaToolList.getJSONObject(i);
                    farmTools[i] = new FarmTool();
                    farmTools[i].toolId = jo.optString("toolId", "");
                    farmTools[i].toolType = ToolType.valueOf(jo.getString("toolType"));
                    farmTools[i].toolCount = jo.getInt("toolCount");
                    farmTools[i].toolHoldLimit = jo.optInt("toolHoldLimit", 20);
                }
            } else {
                Log.record(memo);
                Log.runtime(s);
            }
        } catch (Throwable t) {
            Log.runtime(TAG, "listFarmTool err:");
            Log.printStackTrace(TAG, t);
        }
    }
    private Boolean useAccelerateTool() {
        if (!Status.canUseAccelerateTool()) {
            return false;
        }
        if (!useAccelerateToolContinue.getValue() && AnimalBuff.ACCELERATING.name().equals(ownerAnimal.animalBuff)) {
            return false;
        }
        syncAnimalStatus(ownerFarmId);
        double consumeSpeed = 0d;
        double allFoodHaveEatten = 0d;
        long nowTime = System.currentTimeMillis() / 1000;
        for (Animal animal : animals) {
            if (animal.masterFarmId.equals(ownerFarmId)) {
                consumeSpeed = animal.consumeSpeed;
            }
            allFoodHaveEatten += animal.foodHaveEatten;
            allFoodHaveEatten += animal.consumeSpeed * (nowTime - (double) animal.startEatTime / 1000);
        }
        // consumeSpeed: g/s
        // AccelerateTool: -1h = -60m = -3600s
        boolean isUseAccelerateTool = false;
        while (180 - allFoodHaveEatten >= consumeSpeed * 3600) {
            if ((useAccelerateToolWhenMaxEmotion.getValue() && finalScore != 100)) {
                break;
            }
            if (useFarmTool(ownerFarmId, ToolType.ACCELERATETOOL)) {
                allFoodHaveEatten += consumeSpeed * 3600;
                isUseAccelerateTool = true;
                Status.useAccelerateTool();
                ThreadUtil.sleep(1000);
            } else {
                break;
            }
            if (!useAccelerateToolContinue.getValue()) {
                break;
            }
        }
        return isUseAccelerateTool;
    }
    private Boolean useFarmTool(String targetFarmId, ToolType toolType) {
        try {
            String s = AntFarmRpcCall.listFarmTool();
            JSONObject jo = new JSONObject(s);
            String memo = jo.getString("memo");
            if ("SUCCESS".equals(memo)) {
                JSONArray jaToolList = jo.getJSONArray("toolList");
                for (int i = 0; i < jaToolList.length(); i++) {
                    jo = jaToolList.getJSONObject(i);
                    if (toolType.name().equals(jo.getString("toolType"))) {
                        int toolCount = jo.getInt("toolCount");
                        if (toolCount > 0) {
                            String toolId = "";
                            if (jo.has("toolId"))
                                toolId = jo.getString("toolId");
                            s = AntFarmRpcCall.useFarmTool(targetFarmId, toolId, toolType.name());
                            jo = new JSONObject(s);
                            memo = jo.getString("memo");
                            if ("SUCCESS".equals(memo)) {
                                Log.farm("使用道具🎭[" + toolType.nickName() + "]#剩余" + (toolCount - 1) + "张");
                                return true;
                            } else {
                                Log.record(memo);
                            }
                            Log.runtime(s);
                        }
                        break;
                    }
                }
            } else {
                Log.record(memo);
                Log.runtime(s);
            }
        } catch (Throwable t) {
            Log.runtime(TAG, "useFarmTool err:");
            Log.printStackTrace(TAG, t);
        }
        return false;
    }
    private void feedFriend() {
        try {
            String s, memo;
            JSONObject jo;
            Map<String, Integer> feedFriendAnimalMap = feedFriendAnimalList.getValue();
            for (Map.Entry<String, Integer> entry : feedFriendAnimalMap.entrySet()) {
                String userId = entry.getKey();
                if (userId.equals(UserMap.getCurrentUid()))
                    continue;
                if (!Status.canFeedFriendToday(userId, entry.getValue()))
                    continue;
                s = AntFarmRpcCall.enterFarm("", userId);
                jo = new JSONObject(s);
                memo = jo.getString("memo");
                if ("SUCCESS".equals(memo)) {
                    jo = jo.getJSONObject("farmVO").getJSONObject("subFarmVO");
                    String friendFarmId = jo.getString("farmId");
                    JSONArray jaAnimals = jo.getJSONArray("animals");
                    for (int j = 0; j < jaAnimals.length(); j++) {
                        jo = jaAnimals.getJSONObject(j);
                        String masterFarmId = jo.getString("masterFarmId");
                        if (masterFarmId.equals(friendFarmId)) {
                            jo = jo.getJSONObject("animalStatusVO");
                            if (AnimalInteractStatus.HOME.name().equals(jo.getString("animalInteractStatus"))
                                    && AnimalFeedStatus.HUNGRY.name().equals(jo.getString("animalFeedStatus"))) {
                                feedFriendAnimal(friendFarmId, UserMap.getMaskName(userId));
                            }
                            break;
                        }
                    }
                } else {
                    Log.record(memo);
                    Log.runtime(s);
                }
            }
        } catch (Throwable t) {
            Log.runtime(TAG, "feedFriend err:");
            Log.printStackTrace(TAG, t);
        }
    }
    private void feedFriendAnimal(String friendFarmId, String user) {
        try {
            Log.record("[" + user + "]的小鸡在挨饿");
            if (foodStock < 180) {
                Log.record("喂鸡饲料不足");
                if (unreceiveTaskAward > 0) {
                    Log.record("还有待领取的饲料");
                    receiveFarmTaskAward();
                }
            }
            if (foodStock >= 180) {
                String s = AntFarmRpcCall.feedFriendAnimal(friendFarmId);
                JSONObject jo = new JSONObject(s);
                String memo = jo.getString("memo");
                if ("SUCCESS".equals(memo)) {
                    int feedFood = foodStock - jo.getInt("foodStock");
                    if (feedFood > 0) {
                        add2FoodStock(-feedFood);
                        Log.farm("帮喂好友🥣[" + user + "]的小鸡[" + feedFood + "g]#剩余" + foodStock + "g");
                        Status.feedFriendToday(AntFarmRpcCall.farmId2UserId(friendFarmId));
                    }
                } else {
                    Log.record(memo);
                    Log.runtime(s);
                }
            }
        } catch (Throwable t) {
            Log.runtime(TAG, "feedFriendAnimal err:");
            Log.printStackTrace(TAG, t);
        }
    }
    private void notifyFriend() {
        if (foodStock >= foodStockLimit)
            return;
        try {
            boolean hasNext = false;
            int pageStartSum = 0;
            String s;
            JSONObject jo;
            do {
                s = AntFarmRpcCall.rankingList(pageStartSum);
                jo = new JSONObject(s);
                String memo = jo.getString("memo");
                if ("SUCCESS".equals(memo)) {
                    hasNext = jo.getBoolean("hasNext");
                    JSONArray jaRankingList = jo.getJSONArray("rankingList");
                    pageStartSum += jaRankingList.length();
                    for (int i = 0; i < jaRankingList.length(); i++) {
                        jo = jaRankingList.getJSONObject(i);
                        String userId = jo.getString("userId");
                        String userName = UserMap.getMaskName(userId);
                        boolean isNotifyFriend = notifyFriendList.getValue().contains(userId);
                        if (notifyFriendType.getValue() == NotifyFriendType.DONT_NOTIFY) {
                            isNotifyFriend = !isNotifyFriend;
                        }
                        if (!isNotifyFriend || userId.equals(UserMap.getCurrentUid())) {
                            continue;
                        }
                        boolean starve = jo.has("actionType") && "starve_action".equals(jo.getString("actionType"));
                        if (jo.getBoolean("stealingAnimal") && !starve) {
                            s = AntFarmRpcCall.enterFarm("", userId);
                            jo = new JSONObject(s);
                            memo = jo.getString("memo");
                            if ("SUCCESS".equals(memo)) {
                                jo = jo.getJSONObject("farmVO").getJSONObject("subFarmVO");
                                String friendFarmId = jo.getString("farmId");
                                JSONArray jaAnimals = jo.getJSONArray("animals");
                                boolean notified = !notifyFriend.getValue();
                                for (int j = 0; j < jaAnimals.length(); j++) {
                                    jo = jaAnimals.getJSONObject(j);
                                    String animalId = jo.getString("animalId");
                                    String masterFarmId = jo.getString("masterFarmId");
                                    if (!masterFarmId.equals(friendFarmId) && !masterFarmId.equals(ownerFarmId)) {
                                        if (notified)
                                            continue;
                                        jo = jo.getJSONObject("animalStatusVO");
                                        notified = notifyFriend(jo, friendFarmId, animalId, userName);
                                    }
                                }
                            } else {
                                Log.record(memo);
                                Log.runtime(s);
                            }
                        }
                    }
                } else {
                    Log.record(memo);
                    Log.runtime(s);
                }
            } while (hasNext);
            Log.record("饲料剩余[" + foodStock + "g]");
        } catch (Throwable t) {
            Log.runtime(TAG, "notifyFriend err:");
            Log.printStackTrace(TAG, t);
        }
    }
    private boolean notifyFriend(JSONObject joAnimalStatusVO, String friendFarmId, String animalId, String user) {
        try {
            if (AnimalInteractStatus.STEALING.name().equals(joAnimalStatusVO.getString("animalInteractStatus"))
                    && AnimalFeedStatus.EATING.name().equals(joAnimalStatusVO.getString("animalFeedStatus"))) {
                String s = AntFarmRpcCall.notifyFriend(animalId, friendFarmId);
                JSONObject jo = new JSONObject(s);
                String memo = jo.getString("memo");
                if ("SUCCESS".equals(memo)) {
                    double rewardCount = jo.getDouble("rewardCount");
                    if (jo.getBoolean("refreshFoodStock"))
                        foodStock = (int) jo.getDouble("finalFoodStock");
                    else
                        add2FoodStock((int) rewardCount);
                    Log.farm("通知好友📧[" + user + "]被偷吃#奖励" + rewardCount + "g");
                    return true;
                } else {
                    Log.record(memo);
                    Log.runtime(s);
                }
            }
        } catch (Throwable t) {
            Log.runtime(TAG, "notifyFriend err:");
            Log.printStackTrace(TAG, t);
        }
        return false;
    }
    private void parseSyncAnimalStatusResponse(String resp) {
        try {
            JSONObject jo = new JSONObject(resp);
            if (!jo.has("subFarmVO")) {
                return;
            }
            if (jo.has("emotionInfo")) {
                finalScore = jo.getJSONObject("emotionInfo").getDouble("finalScore");
            }
            JSONObject subFarmVO = jo.getJSONObject("subFarmVO");
            if (subFarmVO.has("foodStock")) {
                foodStock = subFarmVO.getInt("foodStock");
            }
            if (subFarmVO.has("manureVO")) {
                JSONArray manurePotList = subFarmVO.getJSONObject("manureVO").getJSONArray("manurePotList");
                for (int i = 0; i < manurePotList.length(); i++) {
                    JSONObject manurePot = manurePotList.getJSONObject(i);
                    if (manurePot.getInt("manurePotNum") >= 100) {
                        JSONObject joManurePot = new JSONObject(
                                AntFarmRpcCall.collectManurePot(manurePot.getString("manurePotNO")));
                        if (joManurePot.optBoolean("success")) {
                            int collectManurePotNum = joManurePot.getInt("collectManurePotNum");
                            Log.farm("打扫鸡屎🧹[" + collectManurePotNum + "g]");
                        }
                    }
                }
            }
            ownerFarmId = subFarmVO.getString("farmId");
            JSONObject farmProduce = subFarmVO.getJSONObject("farmProduce");
            benevolenceScore = farmProduce.getDouble("benevolenceScore");
            if (subFarmVO.has("rewardList")) {
                JSONArray jaRewardList = subFarmVO.getJSONArray("rewardList");
                if (jaRewardList.length() > 0) {
                    rewardList = new RewardFriend[jaRewardList.length()];
                    for (int i = 0; i < rewardList.length; i++) {
                        JSONObject joRewardList = jaRewardList.getJSONObject(i);
                        if (rewardList[i] == null)
                            rewardList[i] = new RewardFriend();
                        rewardList[i].consistencyKey = joRewardList.getString("consistencyKey");
                        rewardList[i].friendId = joRewardList.getString("friendId");
                        rewardList[i].time = joRewardList.getString("time");
                    }
                }
            }
            JSONArray jaAnimals = subFarmVO.getJSONArray("animals");
            animals = new Animal[jaAnimals.length()];
            for (int i = 0; i < animals.length; i++) {
                Animal animal = new Animal();
                JSONObject animalJsonObject = jaAnimals.getJSONObject(i);
                animal.animalId = animalJsonObject.getString("animalId");
                animal.currentFarmId = animalJsonObject.getString("currentFarmId");
                animal.masterFarmId = animalJsonObject.getString("masterFarmId");
                animal.animalBuff = animalJsonObject.getString("animalBuff");
                animal.subAnimalType = animalJsonObject.getString("subAnimalType");
                animal.currentFarmMasterUserId = animalJsonObject.getString("currentFarmMasterUserId");
                animal.locationType = animalJsonObject.optString("locationType", "");
                JSONObject animalStatusVO = animalJsonObject.getJSONObject("animalStatusVO");
                animal.animalFeedStatus = animalStatusVO.getString("animalFeedStatus");
                animal.animalInteractStatus = animalStatusVO.getString("animalInteractStatus");
                animal.animalInteractStatus = animalStatusVO.getString("animalInteractStatus");
                animal.startEatTime = animalJsonObject.optLong("startEatTime");
                animal.consumeSpeed = animalJsonObject.optDouble("consumeSpeed");
                animal.foodHaveEatten = animalJsonObject.optDouble("foodHaveEatten");
                if (animal.masterFarmId.equals(ownerFarmId)) {
                    ownerAnimal = animal;
                }
                animals[i] = animal;
            }
        } catch (Throwable t) {
            Log.runtime(TAG, "parseSyncAnimalStatusResponse err:");
            Log.printStackTrace(TAG, t);
        }
    }
    private void add2FoodStock(int i) {
        foodStock += i;
        if (foodStock > foodStockLimit) {
            foodStock = foodStockLimit;
        }
        if (foodStock < 0) {
            foodStock = 0;
        }
    }
    private void collectDailyFoodMaterial(String userId) {
        try {
            JSONObject jo = new JSONObject(AntFarmRpcCall.enterKitchen(userId));
            if ("SUCCESS".equals(jo.getString("memo"))) {
                boolean canCollectDailyFoodMaterial = jo.getBoolean("canCollectDailyFoodMaterial");
                int dailyFoodMaterialAmount = jo.getInt("dailyFoodMaterialAmount");
                int garbageAmount = jo.optInt("garbageAmount", 0);
                if (jo.has("orchardFoodMaterialStatus")) {
                    JSONObject orchardFoodMaterialStatus = jo.getJSONObject("orchardFoodMaterialStatus");
                    if ("FINISHED".equals(orchardFoodMaterialStatus.optString("foodStatus"))) {
                        jo = new JSONObject(AntFarmRpcCall.farmFoodMaterialCollect());
                        if ("100".equals(jo.getString("resultCode"))) {
                            Log.farm("小鸡厨房👨🏻‍🍳[领取农场食材]#" + jo.getInt("foodMaterialAddCount") + "g");
                        } else {
                            Log.runtime(TAG, jo.toString());
                        }
                    }
                }
                if (canCollectDailyFoodMaterial) {
                    jo = new JSONObject(AntFarmRpcCall.collectDailyFoodMaterial(dailyFoodMaterialAmount));
                    if ("SUCCESS".equals(jo.getString("memo"))) {
                        Log.farm("小鸡厨房👨🏻‍🍳[领取今日食材]#" + dailyFoodMaterialAmount + "g");
                    } else {
                        Log.runtime(TAG, jo.toString());
                    }
                }
                if (garbageAmount > 0) {
                    jo = new JSONObject(AntFarmRpcCall.collectKitchenGarbage());
                    if ("SUCCESS".equals(jo.getString("memo"))) {
                        Log.farm("小鸡厨房👨🏻‍🍳[领取肥料]#" + jo.getInt("recievedKitchenGarbageAmount") + "g");
                    } else {
                        Log.runtime(TAG, jo.toString());
                    }
                }
            } else {
                Log.runtime(TAG, jo.toString());
            }
        } catch (Throwable t) {
            Log.runtime(TAG, "collectDailyFoodMaterial err:");
            Log.printStackTrace(TAG, t);
        }
    }
    private void collectDailyLimitedFoodMaterial() {
        try {
            JSONObject jo = new JSONObject(AntFarmRpcCall.queryFoodMaterialPack());
            if ("SUCCESS".equals(jo.getString("memo"))) {
                boolean canCollectDailyLimitedFoodMaterial = jo.getBoolean("canCollectDailyLimitedFoodMaterial");
                if (canCollectDailyLimitedFoodMaterial) {
                    int dailyLimitedFoodMaterialAmount = jo.getInt("dailyLimitedFoodMaterialAmount");
                    jo = new JSONObject(AntFarmRpcCall.collectDailyLimitedFoodMaterial(dailyLimitedFoodMaterialAmount));
                    if ("SUCCESS".equals(jo.getString("memo"))) {
                        Log.farm("小鸡厨房👨🏻‍🍳[领取爱心食材店食材]#" + dailyLimitedFoodMaterialAmount + "g");
                    } else {
                        Log.runtime(TAG, jo.toString());
                    }
                }
            } else {
                Log.runtime(TAG, jo.toString());
            }
        } catch (Throwable t) {
            Log.runtime(TAG, "collectDailyLimitedFoodMaterial err:");
            Log.printStackTrace(TAG, t);
        }
    }
    private void cook(String userId) {
        try {
            JSONObject jo = new JSONObject(AntFarmRpcCall.enterKitchen(userId));
            if ("SUCCESS".equals(jo.getString("memo"))) {
                int cookTimesAllowed = jo.getInt("cookTimesAllowed");
                if (cookTimesAllowed > 0) {
                    for (int i = 0; i < cookTimesAllowed; i++) {
                        jo = new JSONObject(AntFarmRpcCall.cook(userId));
                        if ("SUCCESS".equals(jo.getString("memo"))) {
                            JSONObject cuisineVO = jo.getJSONObject("cuisineVO");
                            Log.farm("小鸡厨房👨🏻‍🍳[" + cuisineVO.getString("name") + "]制作成功");
                        } else {
                            Log.runtime(TAG, jo.toString());
                        }
                        ThreadUtil.sleep(RandomUtil.delay());
                    }
                }
            } else {
                Log.runtime(TAG, jo.toString());
            }
        } catch (Throwable t) {
            Log.runtime(TAG, "cook err:");
            Log.printStackTrace(TAG, t);
        }
    }
    private void useFarmFood(JSONArray cuisineList) {
        try {
            JSONObject jo;
            String cookbookId;
            String cuisineId;
            String name;
            for (int i = 0; i < cuisineList.length(); i++) {
                jo = cuisineList.getJSONObject(i);
                if (jo.getInt("count") <= 0)
                    continue;
                cookbookId = jo.getString("cookbookId");
                cuisineId = jo.getString("cuisineId");
                name = jo.getString("name");
                jo = new JSONObject(AntFarmRpcCall.useFarmFood(cookbookId, cuisineId));
                if ("SUCCESS".equals(jo.getString("memo"))) {
                    double deltaProduce = jo.getJSONObject("foodEffect").getDouble("deltaProduce");
                    Log.farm("使用美食🍱[" + name + "]#加速" + deltaProduce + "颗爱心鸡蛋");
                } else {
                    Log.runtime(TAG, jo.toString());
                }
            }
        } catch (Throwable t) {
            Log.runtime(TAG, "useFarmFood err:");
            Log.printStackTrace(TAG, t);
        }
    }
    private void drawLotteryPlus(JSONObject lotteryPlusInfo) {
        try {
            if (!lotteryPlusInfo.has("userSevenDaysGiftsItem"))
                return;
            String itemId = lotteryPlusInfo.getString("itemId");
            JSONObject jo = lotteryPlusInfo.getJSONObject("userSevenDaysGiftsItem");
            JSONArray ja = jo.getJSONArray("userEverydayGiftItems");
            for (int i = 0; i < ja.length(); i++) {
                jo = ja.getJSONObject(i);
                if (jo.getString("itemId").equals(itemId)) {
                    if (!jo.getBoolean("received")) {
                        String singleDesc = jo.getString("singleDesc");
                        int awardCount = jo.getInt("awardCount");
                        if (singleDesc.contains("饲料") && awardCount + foodStock > foodStockLimit) {
                            Log.record("暂停领取[" + awardCount + "]克饲料，上限为[" + foodStockLimit + "]克");
                            break;
                        }
                        jo = new JSONObject(AntFarmRpcCall.drawLotteryPlus());
                        if ("SUCCESS".equals(jo.getString("memo"))) {
                            Log.farm("惊喜礼包🎁[" + singleDesc + "*" + awardCount + "]");
                        } else {
                            Log.runtime(TAG, jo.getString("memo"));
                        }
                    } else {
                        Log.record("当日奖励已领取");
                    }
                    break;
                }
            }
        } catch (Throwable t) {
            Log.runtime(TAG, "drawLotteryPlus err:");
            Log.printStackTrace(TAG, t);
        }
    }
    private void visit() {
        try {
            Map<String, Integer> map = visitFriendList.getValue();
            for (Map.Entry<String, Integer> entry : map.entrySet()) {
                String userId = entry.getKey();
                Integer count = entry.getValue();
                if (userId.equals(UserMap.getCurrentUid()))
                    continue;
                if (count <= 0)
                    continue;
                if (count > 3)
                    count = 3;
                if (Status.canVisitFriendToday(userId, count)) {
                    count = visitFriend(userId, count);
                    if (count > 0)
                        Status.visitFriendToday(userId, count);
                }
            }
        } catch (Throwable t) {
            Log.runtime(TAG, "visit err:");
            Log.printStackTrace(TAG, t);
        }
    }
    private int visitFriend(String userId, int count) {
        int visitedTimes = 0;
        try {
            String s = AntFarmRpcCall.enterFarm("", userId);
            JSONObject jo = new JSONObject(s);
            if ("SUCCESS".equals(jo.getString("memo"))) {
                JSONObject farmVO = jo.getJSONObject("farmVO");
                foodStock = farmVO.getInt("foodStock");
                JSONObject subFarmVO = farmVO.getJSONObject("subFarmVO");
                if (subFarmVO.optBoolean("visitedToday", true))
                    return 3;
                String farmId = subFarmVO.getString("farmId");
                for (int i = 0; i < count; i++) {
                    if (foodStock < 10)
                        break;
                    jo = new JSONObject(AntFarmRpcCall.visitFriend(farmId));
                    if ("SUCCESS".equals(jo.getString("memo"))) {
                        foodStock = jo.getInt("foodStock");
                        Log.farm("赠送麦子🌾[" + UserMap.getMaskName(userId) + "]#" + jo.getInt("giveFoodNum") + "g");
                        visitedTimes++;
                        if (jo.optBoolean("isReachLimit")) {
                            Log.record("今日给[" + UserMap.getMaskName(userId) + "]送麦子已达上限");
                            visitedTimes = 3;
                            break;
                        }
                    } else {
                        Log.record(jo.getString("memo"));
                        Log.runtime(jo.toString());
                    }
                    ThreadUtil.sleep(1000L);
                }
            } else {
                Log.record(jo.getString("memo"));
                Log.runtime(s);
            }
        } catch (Throwable t) {
            Log.runtime(TAG, "visitFriend err:");
            Log.printStackTrace(TAG, t);
        }
        return visitedTimes;
    }
    private void acceptGift() {
        try {
            JSONObject jo = new JSONObject(AntFarmRpcCall.acceptGift());
            if ("SUCCESS".equals(jo.getString("memo"))) {
                int receiveFoodNum = jo.getInt("receiveFoodNum");
                Log.farm("收取麦子🌾[" + receiveFoodNum + "g]");
            } else {
                Log.runtime(TAG, jo.toString());
            }
        } catch (Throwable t) {
            Log.runtime(TAG, "acceptGift err:");
            Log.printStackTrace(TAG, t);
        }
    }
    private void queryChickenDiary(String queryDayStr) {
        try {
            JSONObject jo = new JSONObject(AntFarmRpcCall.queryChickenDiary(queryDayStr));
            if (ResUtil.checkResCode(jo)) {
                JSONObject data = jo.getJSONObject("data");
                JSONObject chickenDiary = data.getJSONObject("chickenDiary");
                String diaryDateStr = chickenDiary.getString("diaryDateStr");
                if (data.has("hasTietie")) {
                    if (!data.optBoolean("hasTietie", true)) {
                        jo = new JSONObject(AntFarmRpcCall.diaryTietie(diaryDateStr, "NEW"));
                        if ("SUCCESS".equals(jo.getString("memo"))) {
                            String prizeType = jo.getString("prizeType");
                            int prizeNum = jo.optInt("prizeNum", 0);
                            Log.farm("贴贴小鸡💞[" + prizeType + "*" + prizeNum + "]");
                        } else {
                            Log.runtime(jo.getString("memo"), jo.toString());
                        }
                        if (!chickenDiary.has("statisticsList"))
                            return;
                        JSONArray statisticsList = chickenDiary.getJSONArray("statisticsList");
                        if (statisticsList.length() > 0) {
                            for (int i = 0; i < statisticsList.length(); i++) {
                                JSONObject tietieStatus = statisticsList.getJSONObject(i);
                                String tietieRoleId = tietieStatus.getString("tietieRoleId");
                                jo = new JSONObject(AntFarmRpcCall.diaryTietie(diaryDateStr, tietieRoleId));
                                if ("SUCCESS".equals(jo.getString("memo"))) {
                                    String prizeType = jo.getString("prizeType");
                                    int prizeNum = jo.optInt("prizeNum", 0);
                                    Log.farm("贴贴小鸡💞[" + prizeType + "*" + prizeNum + "]");
                                } else {
                                    Log.runtime(jo.getString("memo"), jo.toString());
                                }
                            }
                        }
                    }
                }
            } else {
                Log.runtime(jo.getString("resultDesc"), jo.toString());
            }
        } catch (Throwable t) {
            Log.runtime(TAG, "queryChickenDiary err:");
            Log.printStackTrace(TAG, t);
        }
    }
    private void queryChickenDiaryList() {
        try {
            JSONObject jo = new JSONObject(AntFarmRpcCall.queryChickenDiaryList());
            if (ResUtil.checkResCode(jo)) {
                JSONArray chickenDiaryBriefList = jo.getJSONObject("data").optJSONArray("chickenDiaryBriefList");
                if (chickenDiaryBriefList != null && chickenDiaryBriefList.length() > 0) {
                    for (int i = 0; i < chickenDiaryBriefList.length(); i++) {
                        jo = chickenDiaryBriefList.getJSONObject(i);
                        if (!jo.optBoolean("read", true)) {
                            String dateStr = jo.getString("dateStr");
                            queryChickenDiary(dateStr);
                            ThreadUtil.sleep(300);
                        }
                    }
                }
            } else {
                Log.runtime(jo.getString("resultDesc"), jo.toString());
            }
        } catch (Throwable t) {
            Log.runtime(TAG, "queryChickenDiaryList err:");
            Log.printStackTrace(TAG, t);
        }
    }
    private void visitAnimal() {
        try {
            JSONObject jo = new JSONObject(AntFarmRpcCall.visitAnimal());
            if ("SUCCESS".equals(jo.getString("memo"))) {
                if (!jo.has("talkConfigs"))
                    return;
                JSONArray talkConfigs = jo.getJSONArray("talkConfigs");
                JSONArray talkNodes = jo.getJSONArray("talkNodes");
                JSONObject data = talkConfigs.getJSONObject(0);
                String farmId = data.getString("farmId");
                jo = new JSONObject(AntFarmRpcCall.feedFriendAnimalVisit(farmId));
                if ("SUCCESS".equals(jo.getString("memo"))) {
                    for (int i = 0; i < talkNodes.length(); i++) {
                        jo = talkNodes.getJSONObject(i);
                        if (!"FEED".equals(jo.getString("type")))
                            continue;
                        String consistencyKey = jo.getString("consistencyKey");
                        jo = new JSONObject(AntFarmRpcCall.visitAnimalSendPrize(consistencyKey));
                        if ("SUCCESS".equals(jo.getString("memo"))) {
                            String prizeName = jo.getString("prizeName");
                            Log.farm("小鸡到访💞[" + prizeName + "]");
                        } else {
                            Log.runtime(jo.getString("memo"), jo.toString());
                        }
                    }
                } else {
                    Log.runtime(jo.getString("memo"), jo.toString());
                }
            } else {
                Log.runtime(jo.getString("resultDesc"), jo.toString());
            }
        } catch (Throwable t) {
            Log.runtime(TAG, "visitAnimal err:");
            Log.printStackTrace(TAG, t);
        }
    }

    /* 雇佣好友小鸡 */
    private void hireAnimal() {
        JSONArray animals = null;
        try {
            JSONObject jsonObject = enterFarm();
            if (jsonObject == null) {
                return;
            }
            if ("SUCCESS".equals(jsonObject.getString("memo"))) {
                JSONObject farmVO = jsonObject.getJSONObject("farmVO");
                JSONObject subFarmVO = farmVO.getJSONObject("subFarmVO");
                animals = subFarmVO.getJSONArray("animals");
            } else {
                Log.record(jsonObject.getString("memo"));
            }
        } catch (Throwable t) {
            Log.runtime(TAG, "getAnimalCount err:");
            Log.printStackTrace(TAG, t);
            return;
        }
        if (animals == null) {
            return;
        }
        try {
            for (int i = 0, len = animals.length(); i < len; i++) {
                JSONObject joo = animals.getJSONObject(i);
                if (Objects.equals(joo.getString("subAnimalType"), "WORK")) {
                    String taskId = "HIRE|" + joo.getString("animalId");
                    long beHiredEndTime = joo.getLong("beHiredEndTime");
                    if (!hasChildTask(taskId)) {
                        addChildTask(new ChildModelTask(taskId, "HIRE", () -> {
                            if (hireAnimal.getValue()) {
                                hireAnimal();
                            }
                        }, beHiredEndTime));
                        Log.record("添加蹲点雇佣👷在[" + TimeUtil.getCommonDate(beHiredEndTime) + "]执行");
                    } else {
                        addChildTask(new ChildModelTask(taskId, "HIRE", () -> {
                            if (hireAnimal.getValue()) {
                                hireAnimal();
                            }
                        }, beHiredEndTime));
                    }
                }
            }
            int animalCount = animals.length();
            if (animalCount >= 3) {
                return;
            }
            Log.farm("雇佣小鸡👷[当前可雇佣小鸡数量:" + (3 - animalCount) + "只]");
            if (foodStock < 50) {
                Log.record("饲料不足，暂不雇佣");
                return;
            }
            Set<String> hireAnimalSet = hireAnimalList.getValue();
            boolean hasNext;
            int pageStartSum = 0;
            String s;
            JSONObject jo;
            do {
                s = AntFarmRpcCall.rankingList(pageStartSum);
                jo = new JSONObject(s);
                String memo = jo.getString("memo");
                if ("SUCCESS".equals(memo)) {
                    hasNext = jo.getBoolean("hasNext");
                    JSONArray jaRankingList = jo.getJSONArray("rankingList");
                    pageStartSum += jaRankingList.length();
                    for (int i = 0; i < jaRankingList.length(); i++) {
                        JSONObject joo = jaRankingList.getJSONObject(i);
                        String userId = joo.getString("userId");
                        boolean isHireAnimal = hireAnimalSet.contains(userId);
                        if (hireAnimalType.getValue() == HireAnimalType.DONT_HIRE) {
                            isHireAnimal = !isHireAnimal;
                        }
                        if (!isHireAnimal || userId.equals(UserMap.getCurrentUid())) {
                            continue;
                        }
                        String actionTypeListStr = joo.getJSONArray("actionTypeList").toString();
                        if (actionTypeListStr.contains("can_hire_action")) {
                            if (hireAnimalAction(userId)) {
                                animalCount++;
                                break;
                            }
                        }
                    }
                } else {
                    Log.record(memo);
                    Log.runtime(s);
                    break;
                }
            } while (hasNext && animalCount < 3);
            if (animalCount < 3) {
                Log.farm("雇佣小鸡失败，没有足够的小鸡可以雇佣");
            } else {
                autoFeedAnimal();
            }
        } catch (Throwable t) {
            Log.runtime(TAG, "hireAnimal err:");
            Log.printStackTrace(TAG, t);
        }
    }
    private boolean hireAnimalAction(String userId) {
        try {
            String s = AntFarmRpcCall.enterFarm("", userId);
            JSONObject jo = new JSONObject(s);
            if ("SUCCESS".equals(jo.getString("memo"))) {
                JSONObject farmVO = jo.getJSONObject("farmVO");
                JSONObject subFarmVO = farmVO.getJSONObject("subFarmVO");
                String farmId = subFarmVO.getString("farmId");
                JSONArray animals = subFarmVO.getJSONArray("animals");
                for (int i = 0, len = animals.length(); i < len; i++) {
                    JSONObject animal = animals.getJSONObject(i);
                    if (Objects.equals(animal.getJSONObject("masterUserInfoVO").getString("userId"), userId)) {
                        String animalId = animal.getString("animalId");
                        jo = new JSONObject(AntFarmRpcCall.hireAnimal(farmId, animalId));
                        if ("SUCCESS".equals(jo.getString("memo"))) {
                            Log.farm("雇佣小鸡👷[" + UserMap.getMaskName(userId) + "] 成功");
                            JSONArray newAnimals = jo.getJSONArray("animals");
                            for (int ii = 0, newLen = newAnimals.length(); ii < newLen; ii++) {
                                JSONObject joo = newAnimals.getJSONObject(ii);
                                if (Objects.equals(joo.getString("animalId"), animalId)) {
                                    long beHiredEndTime = joo.getLong("beHiredEndTime");
                                    addChildTask(new ChildModelTask("HIRE|" + animalId, "HIRE", () -> {
                                        if (hireAnimal.getValue()) {
                                            hireAnimal();
                                        }
                                    }, beHiredEndTime));
                                    Log.record("添加蹲点雇佣👷在[" + TimeUtil.getCommonDate(beHiredEndTime) + "]执行");
                                    break;
                                }
                            }
                            return true;
                        } else {
                            Log.record(jo.getString("memo"));
                            Log.runtime(s);
                        }
                        return false;
                    }
                }
            } else {
                Log.record(jo.getString("memo"));
            }
        } catch (Throwable t) {
            Log.runtime(TAG, "hireAnimal err:");
            Log.printStackTrace(TAG, t);
        }
        return false;
    }
    private void drawGameCenterAward() {
        try {
            JSONObject jo = new JSONObject(AntFarmRpcCall.queryGameList());
            ThreadUtil.sleep(3000);
            if (jo.optBoolean("success")) {
                JSONObject gameDrawAwardActivity = jo.getJSONObject("gameDrawAwardActivity");
                int canUseTimes = gameDrawAwardActivity.getInt("canUseTimes");
                while (canUseTimes > 0) {
                    try {
                        jo = new JSONObject(AntFarmRpcCall.drawGameCenterAward());
                        ThreadUtil.sleep(3000);
                        if (jo.optBoolean("success")) {
                            canUseTimes = jo.getInt("drawRightsTimes");
                            JSONArray gameCenterDrawAwardList = jo.getJSONArray("gameCenterDrawAwardList");
                            ArrayList<String> awards = new ArrayList<>();
                            for (int i = 0; i < gameCenterDrawAwardList.length(); i++) {
                                JSONObject gameCenterDrawAward = gameCenterDrawAwardList.getJSONObject(i);
                                int awardCount = gameCenterDrawAward.getInt("awardCount");
                                String awardName = gameCenterDrawAward.getString("awardName");
                                awards.add(awardName + "*" + awardCount);
                            }
                            Log.farm("庄园小鸡🎁[开宝箱:获得" + StringUtil.collectionJoinString(",", awards) + "]");
                        } else {
                            Log.runtime(TAG, "drawGameCenterAward falsed result: " + jo);
                        }
                    } catch (Throwable t) {
                        Log.printStackTrace(TAG, t);
                    }
                }
            } else {
                Log.runtime(TAG, "queryGameList falsed result: " + jo);
            }
        } catch (Throwable t) {
            Log.runtime(TAG, "queryChickenDiaryList err:");
            Log.printStackTrace(TAG, t);
        }
    }
    // 小鸡换装
    private void listOrnaments() {
        try {
            String s = AntFarmRpcCall.queryLoveCabin(UserMap.getCurrentUid());
            JSONObject jsonObject = new JSONObject(s);
            if ("SUCCESS".equals(jsonObject.getString("memo"))) {
                JSONObject ownAnimal = jsonObject.getJSONObject("ownAnimal");
                String animalId = ownAnimal.getString("animalId");
                String farmId = ownAnimal.getString("farmId");
                String listResult = AntFarmRpcCall.listOrnaments();
                JSONObject jolistOrnaments = new JSONObject(listResult);
                // 检查是否有 achievementOrnaments 数组
                if (!jolistOrnaments.has("achievementOrnaments")) {
                    return; // 数组为空，直接返回
                }
                JSONArray achievementOrnaments = jolistOrnaments.getJSONArray("achievementOrnaments");
                Random random = new Random();
                List<String> possibleOrnaments = new ArrayList<>(); // 收集所有可保存的套装组合
                for (int i = 0; i < achievementOrnaments.length(); i++) {
                    JSONObject ornament = achievementOrnaments.getJSONObject(i);
                    if (ornament.getBoolean("acquired")) {
                        JSONArray sets = ornament.getJSONArray("sets");
                        List<JSONObject> availableSets = new ArrayList<>();
                        // 收集所有带有 cap 和 coat 的套装组合
                        for (int j = 0; j < sets.length(); j++) {
                            JSONObject set = sets.getJSONObject(j);
                            if ("cap".equals(set.getString("subType")) || "coat".equals(set.getString("subType"))) {
                                availableSets.add(set);
                            }
                        }
                        // 如果有可用的帽子和外套套装组合
                        if (availableSets.size() >= 2) {
                            // 将所有可保存的套装组合添加到 possibleOrnaments 列表中
                            for (int j = 0; j < availableSets.size() - 1; j++) {
                                JSONObject selectedCoat = availableSets.get(j);
                                JSONObject selectedCap = availableSets.get(j + 1);
                                String id1 = selectedCoat.getString("id"); // 外套 ID
                                String id2 = selectedCap.getString("id"); // 帽子 ID
                                String ornaments = id1 + "," + id2;
                                possibleOrnaments.add(ornaments);
                            }
                        }
                    }
                }
                // 如果有可保存的套装组合，则随机选择一个进行保存
                if (!possibleOrnaments.isEmpty()) {
                    String ornamentsToSave = possibleOrnaments.get(random.nextInt(possibleOrnaments.size()));
                    String saveResult = AntFarmRpcCall.saveOrnaments(animalId, farmId, ornamentsToSave);
                    JSONObject saveResultJson = new JSONObject(saveResult);
                    // 判断保存是否成功并输出日志
                    if (saveResultJson.optBoolean("success")) {
                        // 获取保存的整套服装名称
                        String[] ornamentIds = ornamentsToSave.split(",");
                        String wholeSetName = ""; // 整套服装名称
                        // 遍历 achievementOrnaments 查找对应的套装名称
                        for (int i = 0; i < achievementOrnaments.length(); i++) {
                            JSONObject ornament = achievementOrnaments.getJSONObject(i);
                            JSONArray sets = ornament.getJSONArray("sets");
                            // 找到对应的整套服装名称
                            if (sets.length() == 2 && sets.getJSONObject(0).getString("id").equals(ornamentIds[0])
                                    && sets.getJSONObject(1).getString("id").equals(ornamentIds[1])) {
                                wholeSetName = ornament.getString("name");
                                break;
                            }
                        }
                        // 输出日志
                        Log.farm("庄园小鸡💞[换装:" + wholeSetName + "]");
                        Status.setOrnamentToday();
                    } else {
                        Log.runtime(TAG, "保存时装失败，错误码： " + saveResultJson);
                    }
                }
            }
        } catch (Throwable t) {
            Log.runtime(TAG, "listOrnaments err: " + t.getMessage());
            Log.printStackTrace(TAG, t);
        }
    }
    // 一起拿小鸡饲料
    private void letsGetChickenFeedTogether() {
        try {
            JSONObject jo = new JSONObject(AntFarmRpcCall.letsGetChickenFeedTogether());
            if (jo.optBoolean("success")) {
                String bizTraceId = jo.getString("bizTraceId");
                JSONArray p2pCanInvitePersonDetailList = jo.getJSONArray("p2pCanInvitePersonDetailList");
                int canInviteCount = 0;
                int hasInvitedCount = 0;
                List<String> userIdList = new ArrayList<>(); // 保存 userId
                for (int i = 0; i < p2pCanInvitePersonDetailList.length(); i++) {
                    JSONObject personDetail = p2pCanInvitePersonDetailList.getJSONObject(i);
                    String inviteStatus = personDetail.getString("inviteStatus");
                    String userId = personDetail.getString("userId");
                    if (inviteStatus.equals("CAN_INVITE")) {
                        userIdList.add(userId);
                        canInviteCount++;
                    } else if (inviteStatus.equals("HAS_INVITED")) {
                        hasInvitedCount++;
                    }
                }
                int invitedToday = hasInvitedCount;
                int remainingInvites = 5 - invitedToday;
                int invitesToSend = Math.min(canInviteCount, remainingInvites);
                if (invitesToSend == 0) {
                    return;
                }
                Set<String> getFeedSet = getFeedlList.getValue();
                if (getFeedType.getValue() == GetFeedType.GIVE) {
                    for (String userId : userIdList) {
                        if (invitesToSend <= 0) {
//                            Log.record("已达到最大邀请次数限制，停止发送邀请。");
                            break;
                        }
                        if (getFeedSet.contains(userId)) {
                            jo = new JSONObject(AntFarmRpcCall.giftOfFeed(bizTraceId, userId));
                            if (jo.optBoolean("success")) {
                                Log.record("一起拿小鸡饲料🥡 [送饲料：" + UserMap.getMaskName(userId) + "]");
                                invitesToSend--; // 每成功发送一次邀请，减少一次邀请次数
                            } else {
                                Log.record("邀请失败：" + jo);
                                break;
                            }
                        }
                    }
                } else {
                    Random random = new Random();
                    for (int j = 0; j < invitesToSend; j++) {
                        int randomIndex = random.nextInt(userIdList.size());
                        String userId = userIdList.get(randomIndex);
                        jo = new JSONObject(AntFarmRpcCall.giftOfFeed(bizTraceId, userId));
                        if (jo.optBoolean("success")) {
                            Log.record("一起拿小鸡饲料🥡 [送饲料：" + UserMap.getMaskName(userId) + "]");
                        } else {
                            Log.record("邀请失败：" + jo);
                            break;
                        }
                        userIdList.remove(randomIndex);
                    }
                }
            }
        } catch (JSONException e) {
            Log.runtime(TAG, "letsGetChickenFeedTogether err:");
            Log.printStackTrace(e);
        }
    }
    @SuppressWarnings("unused")
    public interface DonationCount {
        int ONE = 0;
        int ALL = 1;
        String[] nickNames = {"随机一次", "随机多次"};
    }
    @SuppressWarnings("unused")
    public interface RecallAnimalType {
        int ALWAYS = 0;
        int WHEN_THIEF = 1;
        int WHEN_HUNGRY = 2;
        int NEVER = 3;
        String[] nickNames = {"始终召回", "偷吃召回", "饥饿召回", "暂不召回"};
    }
    public interface SendBackAnimalWay {
        int HIT = 0;
        int NORMAL = 1;
        String[] nickNames = {"攻击", "常规"};
    }
    public interface SendBackAnimalType {
        int BACK = 0;
        int NOT_BACK = 1;
        String[] nickNames = {"选中遣返", "选中不遣返"};
    }
    public enum AnimalBuff {
        ACCELERATING, INJURED, NONE
    }
    public enum AnimalFeedStatus {
        HUNGRY, EATING, SLEEPY
    }
    public enum AnimalInteractStatus {
        HOME, GOTOSTEAL, STEALING
    }
    public enum SubAnimalType {
        NORMAL, GUEST, PIRATE, WORK
    }
    public enum ToolType {
        STEALTOOL, ACCELERATETOOL, SHARETOOL, FENCETOOL, NEWEGGTOOL, DOLLTOOL;
        public static final CharSequence[] nickNames = {"蹭饭卡", "加速卡", "救济卡", "篱笆卡", "新蛋卡", "公仔补签卡"};
        public CharSequence nickName() {
            return nickNames[ordinal()];
        }
    }
    public enum GameType {
        starGame, jumpGame, flyGame, hitGame;
        public static final CharSequence[] gameNames = {"星星球", "登山赛", "飞行赛", "欢乐揍小鸡"};
        public CharSequence gameName() {
            return gameNames[ordinal()];
        }
    }
    private static class Animal {
        public String animalId, currentFarmId, masterFarmId,
                animalBuff, subAnimalType, animalFeedStatus, animalInteractStatus;
        public String locationType;
        public String currentFarmMasterUserId;
        public Long startEatTime;
        public Double consumeSpeed;
        public Double foodHaveEatten;
    }
    public enum TaskStatus {
        TODO, FINISHED, RECEIVED
    }
    private static class RewardFriend {
        public String consistencyKey, friendId, time;
    }
    private static class FarmTool {
        public ToolType toolType;
        public String toolId;
        public int toolCount, toolHoldLimit;
    }
    @SuppressWarnings("unused")
    public interface HireAnimalType {
        int HIRE = 0;
        int DONT_HIRE = 1;
        String[] nickNames = {"选中雇佣", "选中不雇佣"};
    }
    @SuppressWarnings("unused")
    public interface GetFeedType {
        int GIVE = 0;
        int RANDOM = 1;
        String[] nickNames = {"选中赠送", "随机赠送"};
    }
    public interface NotifyFriendType {
        int NOTIFY = 0;
        int DONT_NOTIFY = 1;
        String[] nickNames = {"选中通知", "选中不通知"};
    }
    public void family() {
        if (StringUtil.isEmpty(familyGroupId)) {
            return;
        }
        try {
            JSONObject jo = enterFamily();
            if (jo == null) {
                return;
            }
            familyGroupId = jo.getString("groupId");
            int familyAwardNum = jo.getInt("familyAwardNum");
            boolean familySignTips = jo.getBoolean("familySignTips");
            //顶梁柱
            JSONObject assignFamilyMemberInfo = jo.getJSONObject("assignFamilyMemberInfo");
            //美食配置
            JSONObject eatTogetherConfig = jo.getJSONObject("eatTogetherConfig");
            //扭蛋
            JSONObject familyDrawInfo = jo.getJSONObject("familyDrawInfo");
            JSONArray familyInteractActions = jo.getJSONArray("familyInteractActions");
            JSONArray animals = jo.getJSONArray("animals");
            List<String> familyUserIds = new ArrayList<>();
            for (int i = 0; i < animals.length(); i++) {
                jo = animals.getJSONObject(i);
                String userId = jo.getString("userId");
                familyUserIds.add(userId);
            }
            if (familySignTips && familyOptions.getValue().contains("familySign")) {
                familySign();
            }
            if (familyAwardNum > 0 && familyOptions.getValue().contains("familyClaimReward")) {
                familyClaimRewardList();
            }
            //顶梁柱特权
            if (!Objects.isNull(assignFamilyMemberInfo) && familyOptions.getValue().contains("assignRights")) {
                JSONObject assignRights = assignFamilyMemberInfo.getJSONObject("assignRights");
                if (Objects.equals(assignRights.getString("assignRightsOwner"), UserMap.getCurrentUid()) && Objects.equals(assignRights.getString("status"), "NOT_USED")) {
                    assignFamilyMember(assignFamilyMemberInfo, familyUserIds);
                }
            }
            //帮喂成员
            if (familyOptions.getValue().contains("feedFriendAnimal")) {
                familyFeedFriendAnimal(animals);
            }
            //请吃美食
            if (familyOptions.getValue().contains("eatTogetherConfig")) {
                familyEatTogether(eatTogetherConfig, familyInteractActions, familyUserIds);
            }
            //道早安
            if (familyOptions.getValue().contains("deliverMsgSend")) {
                deliverMsgSend(familyUserIds);
            }
            //好友分享
            if (familyOptions.getValue().contains("inviteFriendVisitFamily")) {
                inviteFriendVisitFamily(familyUserIds);
            }
            boolean drawActivitySwitch = familyDrawInfo.getBoolean("drawActivitySwitch");
            //扭蛋
            if (drawActivitySwitch && familyOptions.getValue().contains("familyDrawInfo")) {
                familyDrawTask(familyUserIds, familyDrawInfo);
            }
        } catch (Throwable t) {
            Log.runtime(TAG, "family err:");
            Log.printStackTrace(TAG, t);
        }
    }
    private String getFamilyGroupId(String userId) {
        try {
            JSONObject jo = new JSONObject(AntFarmRpcCall.queryLoveCabin(userId));
            if (!"SUCCESS".equals(jo.optString("memo"))) {
                if (jo.has("memo")) {
                    Log.record(jo.getString("memo"));
                    Log.runtime(jo.getString("memo"), jo.toString());
                } else {
                    Log.runtime(TAG, jo.toString());
                }
            } else {
                return jo.optString("groupId");
            }
        } catch (Throwable t) {
            Log.runtime(TAG, "家庭获取GroupId异常:");
            Log.printStackTrace(t);
        }
        return null;
    }
    private JSONObject enterFamily() {
        try {
            JSONObject jo = new JSONObject(AntFarmRpcCall.enterFamily());
            if ("SUCCESS".equals(jo.optString("memo"))) {
                return jo;
            }
        } catch (Throwable t) {
            Log.runtime(TAG, "庄园家庭异常:");
            Log.printStackTrace(TAG, t);
        }
        return null;
    }
    //签到
    private void familySign() {
        try {
            JSONObject jo = new JSONObject(AntFarmRpcCall.familyReceiveFarmTaskAward("FAMILY_SIGN_TASK"));
            if ("SUCCESS".equals(jo.optString("memo"))) {
                Log.farm("庄园家庭🏠提交任务[每日签到]");
            }
        } catch (Throwable t) {
            Log.runtime(TAG, "庄园家庭每日签到异常:");
            Log.printStackTrace(TAG, t);
        }
    }
    //领取奖励
    public void familyClaimRewardList() {
        try {
            JSONObject jo = new JSONObject(AntFarmRpcCall.familyAwardList());
            if (!"SUCCESS".equals(jo.optString("memo"))) {
                return;
            }
            JSONArray ja = jo.getJSONArray("familyAwardRecordList");
            for (int i = 0; i < ja.length(); i++) {
                jo = ja.getJSONObject(i);
                if (jo.optBoolean("expired")
                        || jo.optBoolean("received", true)
                        || jo.has("linkUrl")
                        || (jo.has("operability") && !jo.getBoolean("operability"))) {
                    continue;
                }
                String rightId = jo.getString("rightId");
                String awardName = jo.getString("awardName");
                int count = jo.optInt("count", 1);
                familyClaimReward(rightId, awardName, count);
            }
        } catch (Throwable t) {
            Log.runtime(TAG, "家庭领取奖励:");
            Log.printStackTrace(TAG, t);
        }
    }
    private void familyClaimReward(String rightId, String awardName, int count) {
        try {
            JSONObject jo = new JSONObject(AntFarmRpcCall.receiveFamilyAward(rightId));
            if ("SUCCESS".equals(jo.optString("memo"))) {
                Log.farm("亲密家庭🏠领取奖励[" + awardName + "*" + count + "]");
            }
        } catch (Throwable t) {
            Log.runtime(TAG, "领取奖励异常:");
            Log.printStackTrace(TAG, t);
        }
    }
    private void assignFamilyMember(JSONObject jsonObject, List<String> userIds) {
        try {
            userIds.remove(UserMap.getCurrentUid());
            //随机选一个家庭成员
            if (userIds.isEmpty()) {
                return;
            }
            String beAssignUser = userIds.get(RandomUtil.nextInt(0, userIds.size() - 1));
            //随机获取一个任务类型
            JSONArray assignConfigList = jsonObject.getJSONArray("assignConfigList");
            JSONObject assignConfig = assignConfigList.getJSONObject(RandomUtil.nextInt(0, assignConfigList.length() - 1));
            JSONObject jo = new JSONObject(AntFarmRpcCall.assignFamilyMember(assignConfig.getString("assignAction"), beAssignUser));
            if ("SUCCESS".equals(jo.optString("memo"))) {
                Log.farm("亲密家庭🏠提交任务[使用顶梁柱特权]");
                ThreadUtil.sleep(500);
                jo = new JSONObject(AntFarmRpcCall.sendChat(assignConfig.getString("chatCardType"), beAssignUser));
                if ("SUCCESS".equals(jo.optString("memo"))) {
                    ThreadUtil.sleep(500);
                    syncFamilyStatusIntimacy(familyGroupId);
                }
            }
        } catch (Throwable t) {
            Log.runtime(TAG, "assignFamilyMember err:");
            Log.printStackTrace(TAG, t);
        }
    }
    private void deliverMsgSend(List<String> friendUserIds) {
        try {
            Calendar currentTime = Calendar.getInstance();
            currentTime.get(Calendar.HOUR_OF_DAY);
            currentTime.get(Calendar.MINUTE);
            // 6-10点早安时间
            final int START_HOUR = 6;
            final int START_MINUTE = 0;
            final int END_HOUR = 10;
            final int END_MINUTE = 0;
            Calendar startTime = Calendar.getInstance();
            startTime.set(Calendar.HOUR_OF_DAY, START_HOUR);
            startTime.set(Calendar.MINUTE, START_MINUTE);
            Calendar endTime = Calendar.getInstance();
            endTime.set(Calendar.HOUR_OF_DAY, END_HOUR);
            endTime.set(Calendar.MINUTE, END_MINUTE);
            if (currentTime.before(startTime) || currentTime.after(endTime)) {
                return;
            }
            if (Objects.isNull(familyGroupId)) {
                return;
            }
            // 先移除当前用户ID，否则下面接口报错
            friendUserIds.remove(UserMap.getCurrentUid());
            if (friendUserIds.isEmpty()) {
                return;
            }
            if (Status.hasFlagToday("antFarm::deliverMsgSend")) {
                return;
            }
            JSONArray userIds = new JSONArray();
            for (String userId : friendUserIds) {
                userIds.put(userId);
            }
            String requestString = AntFarmRpcCall.deliverSubjectRecommend(userIds);
            JSONObject jo = new JSONObject(requestString);
            if (jo.optBoolean("success")) {
                ThreadUtil.sleep(500);
                jo = new JSONObject(AntFarmRpcCall.deliverContentExpand(userIds, jo.toString().substring(1, jo.toString().length() - 1)));
                if (jo.optBoolean("success")) {
                    ThreadUtil.sleep(500);
                    String content = jo.getString("content");
                    String deliverId = jo.getString("deliverId");
                    jo = new JSONObject(AntFarmRpcCall.deliverMsgSend(familyGroupId, userIds, content, deliverId));
                    if (jo.optBoolean("success")) {
                        Log.farm("亲密家庭🏠提交任务[道早安]");
                        Status.setFlagToday("antFarm::deliverMsgSend");
                        ThreadUtil.sleep(500);
                        syncFamilyStatusIntimacy(familyGroupId);
                    }
                }
            }
        } catch (Throwable t) {
            Log.runtime(TAG, "deliverMsgSend err:");
            Log.printStackTrace(TAG, t);
        }
    }
    private void syncFamilyStatusIntimacy(String groupId) {
        try {
            JSONObject jo = new JSONObject(AntFarmRpcCall.syncFamilyStatus(groupId, "INTIMACY_VALUE", userId));
            ResUtil.checkSuccess(TAG, jo);
        } catch (Throwable t) {
            Log.runtime(TAG, "syncFamilyStatus err:");
            Log.printStackTrace(TAG, t);
        }
    }
    private void inviteFriendVisitFamily(List<String> friendUserIds) {
        try {
            if (Status.hasFlagToday("antFarm::inviteFriendVisitFamily")) {
                return;
            }
            Set<String> familyValue = inviteFriendVisitFamily.getValue();
            if (familyValue.isEmpty()) {
                return;
            }
            if (Objects.isNull(friendUserIds) || friendUserIds.isEmpty()) {
                return;
            }
            JSONArray userIdArray = new JSONArray();
            for (String u : familyValue) {
                if (!friendUserIds.contains(u) && userIdArray.length() < 6) {
                    userIdArray.put(u);
                }
                if (userIdArray.length() >= 6) {
                    break;
                }
            }
            JSONObject jo = new JSONObject(AntFarmRpcCall.inviteFriendVisitFamily(userIdArray));
            if (Objects.equals("SUCCESS", jo.getString("memo"))) {
                Log.farm("亲密家庭🏠提交任务[分享好友]");
                Status.setFlagToday("antFarm::inviteFriendVisitFamily");
                ThreadUtil.sleep(500);
                syncFamilyStatusIntimacy(familyGroupId);
            }
        } catch (Throwable t) {
            Log.runtime(TAG, "inviteFriendVisitFamily err:");
            Log.printStackTrace(TAG, t);
        }
    }
    private void familyBatchInviteP2PTask(List<String> friendUserIds, JSONObject familyDrawInfo) {
        try {
            if (Status.hasFlagToday("antFarm::familyBatchInviteP2P")) {
                return;
            }
            if (Objects.isNull(friendUserIds) || friendUserIds.isEmpty()) {
                return;
            }
            String activityId = familyDrawInfo.optString("activityId");
            String sceneCode = "ANTFARM_FD_VISIT_" + activityId;
            JSONObject jo = new JSONObject(AntFarmRpcCall.familyShareP2PPanelInfo(sceneCode));
            if (ResUtil.checkSuccess(TAG, jo)) {
                JSONArray p2PFriendVOList = jo.getJSONArray("p2PFriendVOList");
                if (Objects.isNull(p2PFriendVOList) || p2PFriendVOList.length() <= 0) {
                    return;
                }
                JSONArray inviteP2PVOList = new JSONArray();
                for (int i = 0; i < p2PFriendVOList.length(); i++) {
                    if (inviteP2PVOList.length() < 6) {
                        JSONObject object = new JSONObject();
                        object.put("beInvitedUserId", p2PFriendVOList.getJSONObject(i).getString("userId"));
                        object.put("bizTraceId", "");
                        inviteP2PVOList.put(object);
                    }
                    if (inviteP2PVOList.length() >= 6) {
                        break;
                    }
                }
                jo = new JSONObject(AntFarmRpcCall.familyBatchInviteP2P(inviteP2PVOList, sceneCode));
                if (ResUtil.checkSuccess(TAG, jo)) {
                    Log.farm("亲密家庭🏠提交任务[好友串门送扭蛋]");
                    Status.setFlagToday("antFarm::familyBatchInviteP2P");
                    ThreadUtil.sleep(500);
                }
            }
        } catch (Throwable t) {
            Log.runtime(TAG, "familyBatchInviteP2PTask err:");
            Log.printStackTrace(TAG, t);
        }
    }
    private void familyDrawTask(List<String> friendUserIds, JSONObject familyDrawInfo) {
        try {
            JSONArray listFarmTask = familyDrawListFarmTask();
            if (listFarmTask == null) {
                return;
            }
            for (int i = 0; i < listFarmTask.length(); i++) {
                JSONObject jo = listFarmTask.getJSONObject(i);
                TaskStatus taskStatus = TaskStatus.valueOf(jo.getString("taskStatus"));
                String taskId = jo.optString("taskId");
                String title = jo.optString("title");
                if (taskStatus == TaskStatus.RECEIVED) {
                    continue;
                }
                if (taskStatus == TaskStatus.TODO && Objects.equals(taskId, "FAMILY_DRAW_VISIT_TASK") && familyOptions.getValue().contains("batchInviteP2P")) {
                    //分享
                    familyBatchInviteP2PTask(friendUserIds, familyDrawInfo);
                    continue;
                }
                if (taskStatus == TaskStatus.FINISHED && Objects.equals(taskId, "FAMILY_DRAW_FREE_TASK")) {
                    //签到
                    familyDrawSignReceiveFarmTaskAward(taskId, title);
                    continue;
                }
                ThreadUtil.sleep(1000);
            }
            JSONObject jo = new JSONObject(AntFarmRpcCall.queryFamilyDrawActivity());
            if (ResUtil.checkSuccess(TAG, jo)) {
                ThreadUtil.sleep(1000);
                int drawTimes = jo.optInt("familyDrawTimes");
                //碎片个数
                int giftNum = jo.optInt("mengliFragmentCount");
                if (giftNum >= 20 && !Objects.isNull(giftFamilyDrawFragment.getValue())) {
                    giftFamilyDrawFragment(giftFamilyDrawFragment.getValue(), giftNum);
                }
                for (int i = 0; i < drawTimes; i++) {
                    if (!familyDraw()) {
                        return;
                    }
                    ThreadUtil.sleep(1500);
                }
            }
        } catch (Throwable t) {
            Log.runtime(TAG, "familyDrawTask err:");
            Log.printStackTrace(TAG, t);
        }
    }
    private void giftFamilyDrawFragment(String giftUserId, int giftNum) {
        try {
            JSONObject jo = new JSONObject(AntFarmRpcCall.giftFamilyDrawFragment(giftUserId, giftNum));
            if (ResUtil.checkSuccess(TAG, jo)) {
                Log.farm("亲密家庭🏠赠送扭蛋碎片#" + giftNum + "个#" + giftUserId);
            }
        } catch (Throwable t) {
            Log.runtime(TAG, "giftFamilyDrawFragment err:");
            Log.printStackTrace(TAG, t);
        }
    }
    private JSONArray familyDrawListFarmTask() {
        try {
            JSONObject jo = new JSONObject(AntFarmRpcCall.familyDrawListFarmTask());
            if (ResUtil.checkSuccess(TAG, jo)) {
                return jo.getJSONArray("farmTaskList");
            }
        } catch (Throwable t) {
            Log.runtime(TAG, "familyDrawListFarmTask err:");
            Log.printStackTrace(TAG, t);
        }
        return null;
    }
    private Boolean familyDraw() {
        try {
            JSONObject jo = new JSONObject(AntFarmRpcCall.familyDraw());
            if (ResUtil.checkSuccess(TAG, jo)) {
                JSONObject familyDrawPrize = jo.getJSONObject("familyDrawPrize");
                String title = familyDrawPrize.optString("title");
                String awardCount = familyDrawPrize.getString("awardCount");
                int familyDrawTimes = jo.optInt("familyDrawTimes");
                Log.farm("开扭蛋🎟️抽中[" + title + "]#[" + awardCount + "]");
                return familyDrawTimes != 0;
            }
        } catch (Throwable t) {
            Log.runtime(TAG, "familyDraw err:");
            Log.printStackTrace(TAG, t);
        }
        return false;
    }
    private void familyEatTogether(JSONObject eatTogetherConfig, JSONArray familyInteractActions, List<String> friendUserIds) {
        try {
            boolean isEat = false;
            JSONArray periodItemList = eatTogetherConfig.getJSONArray("periodItemList");
            if (Objects.isNull(periodItemList) || periodItemList.length() <= 0) {
                return;
            }
            if (!Objects.isNull(familyInteractActions) && familyInteractActions.length() > 0) {
                for (int i = 0; i < familyInteractActions.length(); i++) {
                    JSONObject familyInteractAction = familyInteractActions.getJSONObject(i);
                    if ("EatTogether".equals(familyInteractAction.optString("familyInteractType"))) {
                        return;
                    }
                }
            }
            String periodName = "";
            Calendar currentTime = Calendar.getInstance();
            for (int i = 0; i < periodItemList.length(); i++) {
                JSONObject periodItem = periodItemList.getJSONObject(i);
                int startHour = periodItem.optInt("startHour");
                int startMinute = periodItem.optInt("startMinute");
                int endHour = periodItem.optInt("endHour");
                int endMinute = periodItem.optInt("endMinute");
                Calendar startTime = Calendar.getInstance();
                startTime.set(Calendar.HOUR_OF_DAY, startHour);
                startTime.set(Calendar.MINUTE, startMinute);
                Calendar endTime = Calendar.getInstance();
                endTime.set(Calendar.HOUR_OF_DAY, endHour);
                endTime.set(Calendar.MINUTE, endMinute);
                if (currentTime.after(startTime) && currentTime.before(endTime)) {
                    periodName = periodItem.optString("periodName");
                    isEat = true;
                    break;
                }
            }
            if (!isEat) {
                return;
            }
            if (Objects.isNull(friendUserIds) || friendUserIds.isEmpty()) {
                return;
            }
            JSONArray array = queryRecentFarmFood(friendUserIds.size());
            if (array == null) {
                return;
            }
            JSONArray friendUserIdList = new JSONArray();
            for (String userId : friendUserIds) {
                friendUserIdList.put(userId);
            }
            JSONObject jo = new JSONObject(AntFarmRpcCall.familyEatTogether(familyGroupId, friendUserIdList, array));
            if (ResUtil.checkSuccess(TAG, jo)) {
                Log.farm("庄园家庭🏠" + periodName + "请客#消耗美食" + friendUserIdList.length() + "份");
                ThreadUtil.sleep(500);
                syncFamilyStatusIntimacy(familyGroupId);
            }
        } catch (Throwable t) {
            Log.runtime(TAG, "familyEatTogether err:");
            Log.printStackTrace(TAG, t);
        }
    }
    private void familyDrawSignReceiveFarmTaskAward(String taskId, String title) {
        try {
            JSONObject jo = new JSONObject(AntFarmRpcCall.familyDrawSignReceiveFarmTaskAward(taskId));
            if (ResUtil.checkSuccess(TAG, jo)) {
                Log.farm("亲密家庭🏠扭蛋任务#" + title + "#奖励领取成功");
            }
        } catch (Throwable t) {
            Log.runtime(TAG, "familyDrawSignReceiveFarmTaskAward err:");
            Log.printStackTrace(TAG, t);
        }
    }
    private JSONArray queryRecentFarmFood(int queryNum) {
        try {
            JSONObject jo = new JSONObject(AntFarmRpcCall.queryRecentFarmFood(queryNum));
            if (!ResUtil.checkSuccess(TAG, jo)) {
                return null;
            }
            JSONArray cuisines = jo.getJSONArray("cuisines");
            if (Objects.isNull(cuisines) || cuisines.length() == 0) {
                return null;
            }
            int count = 0;
            for (int i = 0; i < cuisines.length(); i++) {
                count += cuisines.getJSONObject(i).optInt("count");
            }
            if (count >= queryNum) {
                return cuisines;
            }
        } catch (Throwable t) {
            Log.runtime(TAG, "queryRecentFarmFood err:");
            Log.printStackTrace(TAG, t);
        }
        return null;
    }
    private void familyFeedFriendAnimal(JSONArray animals) {
        try {
            for (int i = 0; i < animals.length(); i++) {
                JSONObject animal = animals.getJSONObject(i);
                JSONObject animalStatusVo = animal.getJSONObject("animalStatusVO");
                if (AnimalInteractStatus.HOME.name().equals(animalStatusVo.getString("animalInteractStatus")) && AnimalFeedStatus.HUNGRY.name().equals(animalStatusVo.getString("animalFeedStatus"))) {
                    String groupId = animal.getString("groupId");
                    String farmId = animal.getString("farmId");
                    String userId = animal.getString("userId");
                    if (!UserMap.getUserIdSet().contains(userId)) {
                        //非好友
                        continue;
                    }
                    JSONObject jo = new JSONObject(AntFarmRpcCall.feedFriendAnimal(farmId, groupId));
                    if ("SUCCESS".equals(jo.getString("memo"))) {
                        int feedFood = foodStock - jo.getInt("foodStock");
                        if (feedFood > 0) {
                            add2FoodStock(-feedFood);
                        }
                        Log.farm("庄园家庭🏠帮喂好友🥣[" + UserMap.getMaskName(userId) + "]的小鸡[" + feedFood + "g]#剩余" + foodStock + "g");
                    }
                }
            }
        } catch (Throwable t) {
            Log.runtime(TAG, "familyFeedFriendAnimal err:");
            Log.printStackTrace(TAG, t);
        }
    }
    static class AntFarmFamilyOption extends MapperEntity {
        public AntFarmFamilyOption(String i, String n) {
            id = i;
            name = n;
        }
        public static List<AntFarmFamilyOption> getAntFarmFamilyOptions() {
            List<AntFarmFamilyOption> list = new ArrayList<>();
            list.add(new AntFarmFamilyOption("familySign", "每日签到"));
            list.add(new AntFarmFamilyOption("eatTogetherConfig", "请吃美食"));
            list.add(new AntFarmFamilyOption("feedFamilyAnimal", "帮喂小鸡"));
//            list.add(new AntFarmFamilyOption("deliverMsgSend", "道早安"));
            list.add(new AntFarmFamilyOption("familyClaimReward", "领取奖励"));
            list.add(new AntFarmFamilyOption("inviteFriendVisitFamily", "好友分享"));
            list.add(new AntFarmFamilyOption("assignRights", "使用顶梁柱特权"));
            list.add(new AntFarmFamilyOption("familyDrawInfo", "开扭蛋"));
            list.add(new AntFarmFamilyOption("batchInviteP2P", "串门送扭蛋"));
            return list;
        }
    }
}
