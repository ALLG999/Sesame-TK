package fansirsqi.xposed.sesame.task.antSports;
import fansirsqi.xposed.sesame.hook.RequestManager;
public class AntSportsRpcCall {
    private static final String chInfo = "ch_appcenter__chsub_9patch",
            timeZone = "Asia\\/Shanghai", version = "3.0.1.2", alipayAppVersion = "0.0.852",
            cityCode = "330100", appId = "2021002116659397";
    private static final String features=                "[\n" +
            "            \"DAILY_STEPS_RANK_V2\",\n" +
            "            \"STEP_BATTLE\",\n" +
            "            \"CLUB_HOME_CARD\",\n" +
            "            \"NEW_HOME_PAGE_STATIC\",\n" +
            "            \"CLOUD_SDK_AUTH\",\n" +
            "            \"STAY_ON_COMPLETE\",\n" +
            "            \"EXTRA_TREASURE_BOX\",\n" +
            "            \"NEW_HOME_PAGE_STATIC\",\n" +
            "            \"SUPPORT_AI\",\n" +
            "            \"SUPPORT_TAB3\",\n" +
            "            \"SUPPORT_FLYRABBIT\",\n" +
            "            \"SUPPORT_NEW_MATCH\",\n" +
            "            \"EXTERNAL_ADVERTISEMENT_TASK\",\n" +
            "            \"PROP\",\n" +
            "            \"PROPV2\",\n" +
            "            \"ASIAN_GAMES\"\n" +
            "        ],\n" ;

    // 运动任务查询
    public static String queryCoinTaskPanel() {
        String args1 = "[\n" +
                "    {\n" +
                "        \"canAddHome\": false,\n" +
                "        \"chInfo\": \"ch_appcenter__chsub_9patch\",\n" +
                "        \"clientAuthStatus\": \"not_support\",\n" +
                "        \"clientOS\": \"android\",\n" +
                "        \"features\": " + features + ",\n" +   // ← 注意这里要加逗号
                "        \"topTaskId\": \"\"\n" +
                "    }\n" +
                "]";
        return RequestManager.requestString(
                "com.alipay.sportshealth.biz.rpc.SportsHealthCoinTaskRpc.queryCoinTaskPanel",
                args1
        );
    }

    // 去完成任务
    public static String completeExerciseTasks(String taskId) {
        String args1 = "[\n" +
                "    {\n" +
                "        \"chInfo\": \"ch_appcenter__chsub_9patch\",\n" +
                "        \"clientOS\": \"android\",\n" +
                "        \"features\": " + features + ",\n" +   // 注意这里加了逗号
                "        \"taskAction\": \"JUMP\",\n" +
                "        \"taskId\": \"" + taskId + "\"\n" +
                "    }\n" +
                "]";
        return RequestManager.requestString(
                "com.alipay.sportshealth.biz.rpc.SportsHealthCoinTaskRpc.completeTask",
                args1
        );
    }

    // 领取运动任务奖励
    public static String receiveCoinTaskAward(String taskId) {
        String args = "[\n" +
                "    {\n" +
                "        \"taskId\": \"" + taskId + "\",\n" +
                "        \"apiVersion\": \"energy\",\n" +
                "        \"chInfo\": \"medical_health\"\n" +
                "    }\n" +
                "]";
        return RequestManager.requestString(
                "com.alipay.sportshealth.biz.rpc.SportsHealthCoinTaskRpc.receiveCoinTaskAward",
                args
        );
    }

    public static String sportsCheck_in() {
        String args1 = "[\n" +
                "    {\n" +
                "        \"chInfo\": \"homecard\",\n" +
                "        \"clientOS\": \"android\",\n" +
                "        \"features\": " +features+
                "        \"operatorType\": \"signIn\"\n" +
                "    }\n" +
                "]";
        return RequestManager.requestString("com.alipay.sportshealth.biz.rpc.SportsHealthCoinTaskRpc.signInCoinTask", args1);
    }
    public static String queryCoinBubbleModule() {
        return RequestManager.requestString("com.alipay.sportshealth.biz.rpc.sportsHealthHomeRpc.queryCoinBubbleModule",
                "[{\"bubbleId\":\"\",\"canAddHome\":false,\"chInfo\":\"" + chInfo
                        + "\",\"clientAuthStatus\":\"not_support\",\"clientOS\":\"android\",\"distributionChannel\":\"\",\"features\":[\"DAILY_STEPS_RANK_V2\",\"STEP_BATTLE\",\"CLUB_HOME_CARD\",\"NEW_HOME_PAGE_STATIC\",\"CLOUD_SDK_AUTH\",\"STAY_ON_COMPLETE\",\"EXTRA_TREASURE_BOX\",\"NEW_HOME_PAGE_STATIC\",\"SUPPORT_AI\",\"SUPPORT_TAB3\",\"SUPPORT_FLYRABBIT\",\"PROP\",\"PROPV2\",\"ASIAN_GAMES\"]}]");
    }
    public static String receiveCoinAsset(String assetId, int coinAmount) {
        return RequestManager.requestString("com.alipay.sportshealth.biz.rpc.SportsHealthCoinCenterRpc.receiveCoinAsset",
                "[{\"assetId\":\"" + assetId
                        + "\",\"chInfo\":\"" + chInfo
                        + "\",\"clientOS\":\"android\",\"coinAmount\":"
                        + coinAmount
                        + ",\"features\":[\"DAILY_STEPS_RANK_V2\",\"STEP_BATTLE\",\"CLUB_HOME_CARD\",\"NEW_HOME_PAGE_STATIC\",\"CLOUD_SDK_AUTH\",\"STAY_ON_COMPLETE\",\"EXTRA_TREASURE_BOX\",\"NEW_HOME_PAGE_STATIC\",\"SUPPORT_TAB3\",\"SUPPORT_FLYRABBIT\",\"PROP\",\"PROPV2\",\"ASIAN_GAMES\"],\"tracertPos\":\"首页金币收集\"}]");
    }
    public static String queryMyHomePage() {
        return RequestManager.requestString("alipay.antsports.walk.map.queryMyHomePage", "[{\"alipayAppVersion\":\""
                + alipayAppVersion + "\",\"chInfo\":\"" + chInfo
                + "\",\"clientOS\":\"android\",\"features\":[\"DAILY_STEPS_RANK_V2\",\"STEP_BATTLE\",\"CLUB_HOME_CARD\",\"NEW_HOME_PAGE_STATIC\",\"CLOUD_SDK_AUTH\",\"STAY_ON_COMPLETE\",\"EXTRA_TREASURE_BOX\",\"NEW_HOME_PAGE_STATIC\",\"SUPPORT_TAB3\",\"SUPPORT_FLYRABBIT\",\"PROP\",\"PROPV2\",\"ASIAN_GAMES\"],\"pathListUsePage\":true,\"timeZone\":\""
                + timeZone + "\"}]");
    }
    public static String join(String pathId) {
        return RequestManager.requestString("alipay.antsports.walk.map.join", "[{\"chInfo\":\"" + chInfo
                + "\",\"clientOS\":\"android\",\"features\":[\"DAILY_STEPS_RANK_V2\",\"STEP_BATTLE\",\"CLUB_HOME_CARD\",\"NEW_HOME_PAGE_STATIC\",\"CLOUD_SDK_AUTH\",\"STAY_ON_COMPLETE\",\"EXTRA_TREASURE_BOX\",\"NEW_HOME_PAGE_STATIC\",\"SUPPORT_TAB3\",\"SUPPORT_FLYRABBIT\",\"PROP\",\"PROPV2\",\"ASIAN_GAMES\"],\"pathId\":\""
                + pathId + "\"}]");
    }
    public static String openAndJoinFirst() {
        return RequestManager.requestString("alipay.antsports.walk.user.openAndJoinFirst", "[{\"chInfo\":\"" + chInfo
                + "\",\"clientOS\":\"android\",\"features\":[\"DAILY_STEPS_RANK_V2\",\"STEP_BATTLE\",\"CLUB_HOME_CARD\",\"NEW_HOME_PAGE_STATIC\",\"CLOUD_SDK_AUTH\",\"STAY_ON_COMPLETE\",\"EXTRA_TREASURE_BOX\",\"NEW_HOME_PAGE_STATIC\",\"SUPPORT_TAB3\",\"SUPPORT_FLYRABBIT\",\"PROP\",\"PROPV2\",\"ASIAN_GAMES\"]}]");
    }
    public static String go(String day, String rankCacheKey, int stepCount) {
        return RequestManager.requestString("alipay.antsports.walk.map.go", "[{\"chInfo\":\"" + chInfo
                + "\",\"clientOS\":\"android\",\"day\":\"" + day
                + "\",\"features\":[\"DAILY_STEPS_RANK_V2\",\"STEP_BATTLE\",\"CLUB_HOME_CARD\",\"NEW_HOME_PAGE_STATIC\",\"CLOUD_SDK_AUTH\",\"STAY_ON_COMPLETE\",\"EXTRA_TREASURE_BOX\",\"NEW_HOME_PAGE_STATIC\",\"SUPPORT_TAB3\",\"SUPPORT_FLYRABBIT\",\"PROP\",\"PROPV2\",\"ASIAN_GAMES\"],\"needAllBox\":true,\"rankCacheKey\":\""
                + rankCacheKey + "\",\"timeZone\":\"" + timeZone + "\",\"useStepCount\":" + stepCount
                + "}]");
    }
    public static String openTreasureBox(String boxNo, String userId) {
        return RequestManager.requestString("alipay.antsports.walk.treasureBox.openTreasureBox", "[{\"boxNo\":\"" + boxNo
                + "\",\"chInfo\":\"" + chInfo
                + "\",\"clientOS\":\"android\",\"features\":[\"DAILY_STEPS_RANK_V2\",\"STEP_BATTLE\",\"CLUB_HOME_CARD\",\"NEW_HOME_PAGE_STATIC\",\"CLOUD_SDK_AUTH\",\"STAY_ON_COMPLETE\",\"EXTRA_TREASURE_BOX\",\"NEW_HOME_PAGE_STATIC\",\"SUPPORT_TAB3\",\"SUPPORT_FLYRABBIT\",\"PROP\",\"PROPV2\",\"ASIAN_GAMES\"],\"userId\":\""
                + userId + "\"}]");
    }
    public static String queryBaseList() {
        return RequestManager.requestString("alipay.antsports.walk.path.queryBaseList", "[{\"chInfo\":\"" + chInfo
                + "\",\"clientOS\":\"android\",\"features\":[\"DAILY_STEPS_RANK_V2\",\"STEP_BATTLE\",\"CLUB_HOME_CARD\",\"NEW_HOME_PAGE_STATIC\",\"CLOUD_SDK_AUTH\",\"STAY_ON_COMPLETE\",\"EXTRA_TREASURE_BOX\",\"NEW_HOME_PAGE_STATIC\",\"SUPPORT_TAB3\",\"SUPPORT_FLYRABBIT\",\"PROP\",\"PROPV2\",\"ASIAN_GAMES\"]}]");
    }
    public static String queryProjectList(int index) {
        return RequestManager.requestString("alipay.antsports.walk.charity.queryProjectList", "[{\"chInfo\":\"" + chInfo
                + "\",\"clientOS\":\"android\",\"features\":[\"DAILY_STEPS_RANK_V2\",\"STEP_BATTLE\",\"CLUB_HOME_CARD\",\"NEW_HOME_PAGE_STATIC\",\"CLOUD_SDK_AUTH\",\"STAY_ON_COMPLETE\",\"EXTRA_TREASURE_BOX\",\"NEW_HOME_PAGE_STATIC\",\"SUPPORT_TAB3\",\"SUPPORT_FLYRABBIT\",\"PROP\",\"PROPV2\",\"ASIAN_GAMES\"],\"index\":"
                + index + ",\"projectListUseVertical\":true}]");
    }
    public static String donate(int donateCharityCoin, String projectId) {
        return RequestManager.requestString("alipay.antsports.walk.charity.donate", "[{\"chInfo\":\"" + chInfo
                + "\",\"clientOS\":\"android\",\"donateCharityCoin\":" + donateCharityCoin
                + ",\"features\":[\"DAILY_STEPS_RANK_V2\",\"STEP_BATTLE\",\"CLUB_HOME_CARD\",\"NEW_HOME_PAGE_STATIC\",\"CLOUD_SDK_AUTH\",\"STAY_ON_COMPLETE\",\"EXTRA_TREASURE_BOX\",\"NEW_HOME_PAGE_STATIC\",\"SUPPORT_TAB3\",\"SUPPORT_FLYRABBIT\",\"PROP\",\"PROPV2\",\"ASIAN_GAMES\"],\"projectId\":\""
                + projectId + "\"}]");
    }
    public static String queryWalkStep() {
        return RequestManager.requestString("alipay.antsports.walk.user.queryWalkStep", "[{\"chInfo\":\"" + chInfo
                + "\",\"clientOS\":\"android\",\"features\":[\"DAILY_STEPS_RANK_V2\",\"STEP_BATTLE\",\"CLUB_HOME_CARD\",\"NEW_HOME_PAGE_STATIC\",\"CLOUD_SDK_AUTH\",\"STAY_ON_COMPLETE\",\"EXTRA_TREASURE_BOX\",\"NEW_HOME_PAGE_STATIC\",\"SUPPORT_TAB3\",\"SUPPORT_FLYRABBIT\",\"PROP\",\"PROPV2\",\"ASIAN_GAMES\"],\"timeZone\":\""
                + timeZone + "\"}]");
    }
    public static String walkDonateSignInfo(int count) {
        return RequestManager.requestString("alipay.charity.mobile.donate.walk.walkDonateSignInfo",
                "[{\"needDonateAction\":false,\"source\":\"walkDonateHome\",\"steps\":" + count
                        + ",\"timezoneId\":\""
                        + timeZone + "\"}]");
    }
    public static String donateWalkHome(int count) {
        return RequestManager.requestString("alipay.charity.mobile.donate.walk.home",
                "[{\"module\":\"3\",\"steps\":" + count + ",\"timezoneId\":\"" + timeZone + "\"}]");
    }
    public static String exchange(String actId, int count, String donateToken) {
        return RequestManager.requestString("alipay.charity.mobile.donate.walk.exchange",
                "[{\"actId\":\"" + actId + "\",\"count\":"
                        + count + ",\"donateToken\":\"" + donateToken + "\",\"timezoneId\":\""
                        + timeZone + "\",\"ver\":0}]");
    }
    // 运动币兑好礼
    public static String queryItemDetail(String itemId) {
        String arg = "[{\"itemId\":\"" + itemId + "\"}]";
        return RequestManager.requestString("com.alipay.sportshealth.biz.rpc.SportsHealthItemCenterRpc.queryItemDetail", arg);
    }
    public static String exchangeItem(String itemId, int coinAmount) {
        String arg = "[{\"coinAmount\":" + coinAmount + ",\"itemId\":\"" + itemId + "\"}]";
        return RequestManager.requestString("com.alipay.sportshealth.biz.rpc.SportsHealthItemCenterRpc.exchangeItem", arg);
    }
    public static String queryExchangeRecordPage(String exchangeRecordId) {
        String arg = "[{\"exchangeRecordId\":\"" + exchangeRecordId + "\"}]";
        return RequestManager.requestString("com.alipay.sportshealth.biz.rpc.SportsHealthItemCenterRpc.queryExchangeRecordPage", arg);
    }
    /*
     * 新版 走路线
     */
    // 查询用户
    public static String queryUser() {
        return RequestManager.requestString("com.alipay.sportsplay.biz.rpc.walk.queryUser",
                "[{\"source\":\"ch_appcenter__chsub_9patch\",\"timeZone\":\"" + timeZone + "\"}]");
    }
    // 查询主题列表
    public static String queryThemeList() {
        return RequestManager.requestString("com.alipay.sportsplay.biz.rpc.walk.theme.queryThemeList",
                "[{\"chInfo\":\"ch_appcenter__chsub_9patch\",\"clientOS\":\"android\","
                        + "\"features\":[\"DAILY_STEPS_RANK_V2\",\"STEP_BATTLE\",\"CLUB_HOME_CARD\",\"NEW_HOME_PAGE_STATIC\",\"CLOUD_SDK_AUTH\",\"STAY_ON_COMPLETE\",\"EXTRA_TREASURE_BOX\",\"SUPPORT_AI\",\"SUPPORT_FLYRABBIT\",\"SUPPORT_NEW_MATCH\",\"EXTERNAL_ADVERTISEMENT_TASK\",\"PROP\",\"PROPV2\",\"ASIAN_GAMES\"]"
                        + "}]");
    }
    // 查询世界地图
    public static String queryWorldMap(String themeId) {
        return RequestManager.requestString("com.alipay.sportsplay.biz.rpc.walk.queryWorldMap",
                "[{\"chInfo\":\"ch_appcenter__chsub_9patch\",\"clientOS\":\"android\","
                        + "\"features\":[\"DAILY_STEPS_RANK_V2\",\"STEP_BATTLE\",\"CLUB_HOME_CARD\",\"NEW_HOME_PAGE_STATIC\",\"CLOUD_SDK_AUTH\",\"STAY_ON_COMPLETE\",\"EXTRA_TREASURE_BOX\",\"SUPPORT_AI\",\"SUPPORT_FLYRABBIT\",\"SUPPORT_NEW_MATCH\",\"EXTERNAL_ADVERTISEMENT_TASK\",\"PROP\",\"PROPV2\",\"ASIAN_GAMES\"]"
                        + ",\"themeId\":\"" + themeId + "\"}]");
    }
    // 查询城市路线
    public static String queryCityPath(String cityId) {
        return RequestManager.requestString("com.alipay.sportsplay.biz.rpc.walk.queryCityPath",
                "[{\"chInfo\":\"ch_appcenter__chsub_9patch\",\"clientOS\":\"android\","
                        + "\"features\":[\"DAILY_STEPS_RANK_V2\",\"STEP_BATTLE\",\"CLUB_HOME_CARD\",\"NEW_HOME_PAGE_STATIC\",\"CLOUD_SDK_AUTH\",\"STAY_ON_COMPLETE\",\"EXTRA_TREASURE_BOX\",\"SUPPORT_AI\",\"SUPPORT_FLYRABBIT\",\"SUPPORT_NEW_MATCH\",\"EXTERNAL_ADVERTISEMENT_TASK\",\"PROP\",\"PROPV2\",\"ASIAN_GAMES\"]"
                        + ",\"cityId\":\"" + cityId + "\"}]");
    }
    // 查询路线
    public static String queryPath(String appId, String date, String pathId) {
        String wufuRewardType = "WUFU_CARD";
        return RequestManager.requestString("com.alipay.sportsplay.biz.rpc.walk.queryPath",
                "[{\"appId\":\"" + appId + "\",\"date\":\"" + date + "\",\"pathId\":\"" + pathId
                        + "\",\"source\":\"ch_appcenter__chsub_9patch\",\"timeZone\":\"" + timeZone
                        + "\",\"wufuRewardType\":\"" + wufuRewardType + "\"}]");
    }
    // 加入路线
    public static String joinPath(String pathId) {
        return RequestManager.requestString("com.alipay.sportsplay.biz.rpc.walk.joinPath",
                "[{\"pathId\":\"" + pathId + "\",\"source\":\"ch_appcenter__chsub_9patch\"}]");
    }
    // 行走路线
    public static String walkGo(String appId, String date, String pathId, int useStepCount) {
        return RequestManager.requestString("com.alipay.sportsplay.biz.rpc.walk.go",
                "[{\"appId\":\"" + appId + "\",\"date\":\"" + date + "\",\"pathId\":\"" + pathId
                        + "\",\"source\":\"ch_appcenter__chsub_9patch\",\"timeZone\":\"" + timeZone
                        + "\",\"useStepCount\":\"" + useStepCount + "\"}]");
    }
    // 开启宝箱
    // eventBillNo = boxNo(WalkGo)
    public static String receiveEvent(String eventBillNo) {
        return RequestManager.requestString("com.alipay.sportsplay.biz.rpc.walk.receiveEvent",
                "[{\"eventBillNo\":\"" + eventBillNo + "\"}]");
    }
    // 查询路线奖励
    public static String queryPathReward(String appId, String pathId) {
        return RequestManager.requestString("com.alipay.sportsplay.biz.rpc.walk.queryPathReward", "[{\"appId\":\""
                + appId + "\",\"pathId\":\"" + pathId + "\",\"source\":\"ch_appcenter__chsub_9patch\"}]");
    }
    /* 这个好像没用 */
    public static String exchangeSuccess(String exchangeId) {
        String args1 = "[{\"exchangeId\":\"" + exchangeId
                + "\",\"timezone\":\"GMT+08:00\",\"version\":\"" + version + "\"}]";
        return RequestManager.requestString("alipay.charity.mobile.donate.exchange.success", args1);
    }
    /* 文体中心 */
    public static String userTaskGroupQuery(String groupId) {
        return RequestManager.requestString("alipay.tiyubiz.sports.userTaskGroup.query",
                "[{\"cityCode\":\"" + cityCode + "\",\"groupId\":\"" + groupId + "\"}]");
    }
    public static String userTaskComplete(String bizType, String taskId) {
        return RequestManager.requestString("alipay.tiyubiz.sports.userTask.complete",
                "[{\"bizType\":\"" + bizType + "\",\"cityCode\":\"" + cityCode + "\",\"completedTime\":"
                        + System.currentTimeMillis() + ",\"taskId\":\"" + taskId + "\"}]");
    }
    public static String userTaskRightsReceive(String taskId, String userTaskId) {
        return RequestManager.requestString("alipay.tiyubiz.sports.userTaskRights.receive",
                "[{\"taskId\":\"" + taskId + "\",\"userTaskId\":\"" + userTaskId + "\"}]");
    }
    public static String queryAccount() {
        return RequestManager.requestString("alipay.tiyubiz.user.asset.query.account",
                "[{\"accountType\":\"TIYU_SEED\"}]");
    }
    public static String queryRoundList() {
        return RequestManager.requestString("alipay.tiyubiz.wenti.walk.queryRoundList",
                "[{}]");
    }
    public static String participate(int bettingPoints, String InstanceId, String ResultId, String roundId) {
        return RequestManager.requestString("alipay.tiyubiz.wenti.walk.participate",
                "[{\"bettingPoints\":" + bettingPoints + ",\"guessInstanceId\":\"" + InstanceId
                        + "\",\"guessResultId\":\"" + ResultId
                        + "\",\"newParticipant\":false,\"roundId\":\"" + roundId
                        + "\",\"stepTimeZone\":\"Asia/Shanghai\"}]");
    }
    public static String pathFeatureQuery() {
        return RequestManager.requestString("alipay.tiyubiz.path.feature.query",
                "[{\"appId\":\"" + appId
                        + "\",\"features\":[\"USER_CURRENT_PATH_SIMPLE\"],\"sceneCode\":\"wenti_shijiebei\"}]");
    }
    public static String pathMapJoin(String pathId) {
        return RequestManager.requestString("alipay.tiyubiz.path.map.join",
                "[{\"appId\":\"" + appId + "\",\"pathId\":\"" + pathId + "\"}]");
    }
    public static String pathMapHomepage(String pathId) {
        return RequestManager.requestString("alipay.tiyubiz.path.map.homepage",
                "[{\"appId\":\"" + appId + "\",\"pathId\":\"" + pathId + "\"}]");
    }
    public static String stepQuery(String countDate, String pathId) {
        return RequestManager.requestString("alipay.tiyubiz.path.map.step.query",
                "[{\"appId\":\"" + appId + "\",\"countDate\":\"" + countDate
                        + "\",\"pathId\":\""
                        + pathId + "\",\"timeZone\":\"Asia/Shanghai\"}]");
    }
    public static String tiyubizGo(String countDate, int goStepCount, String pathId, String userPathRecordId) {
        return RequestManager.requestString("alipay.tiyubiz.path.map.go",
                "[{\"appId\":\"" + appId + "\",\"countDate\":\"" + countDate
                        + "\",\"goStepCount\":"
                        + goStepCount + ",\"pathId\":\"" + pathId
                        + "\",\"timeZone\":\"Asia/Shanghai\",\"userPathRecordId\":\""
                        + userPathRecordId + "\"}]");
    }
    public static String rewardReceive(String pathId, String userPathRewardId) {
        return RequestManager.requestString("alipay.tiyubiz.path.map.reward.receive",
                "[{\"appId\":\"" + appId + "\",\"pathId\":\"" + pathId + "\",\"userPathRewardId\":\""
                        + userPathRewardId + "\"}]");
    }
    /* 抢好友大战 */
    public static String queryClubHome() {
        return RequestManager.requestString("alipay.antsports.club.home.queryClubHome",
                "[{\"chInfo\":\"healthstep\",\"timeZone\":\"Asia/Shanghai\"}]");
    }
    public static void collectBubble(String bubbleId) {
        RequestManager.requestString("alipay.antsports.club.home.collectBubble",
                "[{\"bubbleId\":\"" + bubbleId + "\",\"chInfo\":\"healthstep\"}]");
    }
    public static String queryTrainItem() {
        return RequestManager.requestString("alipay.antsports.club.train.queryTrainItem",
                "[{\"chInfo\":\"healthstep\"}]");
    }
    public static String trainMember(String itemType, String memberId, String originBossId) {
        return RequestManager.requestString("alipay.antsports.club.train.trainMember",
                "[{\"chInfo\":\"healthstep\",\"itemType\":\"" + itemType + "\",\"memberId\":\"" + memberId + "\",\"originBossId\":\"" + originBossId + "\"}]");
    }
    public static String queryMemberPriceRanking(String coinBalance) {
        return RequestManager.requestString("alipay.antsports.club.ranking.queryMemberPriceRanking",
                "[{\"buyMember\":\"true\",\"chInfo\":\"healthstep\",\"coinBalance\":\"" + coinBalance + "\"}]");
    }
    public static String queryClubMember(String memberId, String originBossId) {
        return RequestManager.requestString("alipay.antsports.club.trade.queryClubMember",
                "[{\"chInfo\":\"healthstep\",\"memberId\":\"" + memberId + "\",\"originBossId\":\"" + originBossId + "\"}]");
    }
    public static String buyMember(String currentBossId, String memberId, String originBossId, String priceInfo, String roomId) {
        String requestData = "[{\"chInfo\":\"healthstep\",\"currentBossId\":\"" + currentBossId + "\",\"memberId\":\"" + memberId + "\",\"originBossId\":\"" + originBossId + "\",\"priceInfo\":" + priceInfo + ",\"roomId\":\"" + roomId + "\"}]";
        return RequestManager.requestString("alipay.antsports.club.trade.buyMember", requestData);
    }

    /**
     * 查询运动首页气泡（可能已替换原 queryCoinBubbleModule）
     */
    public static String queryFriendClubHome() {
        return RequestManager.requestString(
                "com.alipay.neverland.biz.rpc.queryFriendClubHome",
                "[{\"apiVersion\":\"energy\",\"chInfo\":\"" + chInfo + "\",\"source\":\"SPORT\"}]"
        );
    }

    /**
     * 领取运动俱乐部气泡
     * @param bubbleId 气泡ID
     */
    public static String pickBubbleTaskEnergy(String bubbleId) {
        String payload = "[{" +
                "\"apiVersion\":\"energy\"," +
                "\"chInfo\":\"" + chInfo + "\"," +
                "\"medEnergyBallInfoRecordIds\":[\"" + bubbleId + "\"]," +
                "\"pickAllEnergyBall\":false," +
                "\"source\":\"SPORT\"" +
                "}]";

        return RequestManager.requestString(
                "com.alipay.neverland.biz.rpc.pickBubbleTaskEnergy",
                payload
        );
    }

}