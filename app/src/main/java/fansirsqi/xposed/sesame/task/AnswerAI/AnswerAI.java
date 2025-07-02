package fansirsqi.xposed.sesame.task.AnswerAI;

import java.util.List;

import fansirsqi.xposed.sesame.model.Model;
import fansirsqi.xposed.sesame.model.ModelFields;
import fansirsqi.xposed.sesame.model.ModelGroup;
import fansirsqi.xposed.sesame.model.modelFieldExt.ChoiceModelField;
import fansirsqi.xposed.sesame.model.modelFieldExt.StringModelField;
import fansirsqi.xposed.sesame.model.modelFieldExt.TextModelField;
import fansirsqi.xposed.sesame.model.modelFieldExt.BooleanModelField;
import fansirsqi.xposed.sesame.util.Log;

/**
 * AI答题模块，实现基于多种AI服务的答题功能
 */
public class AnswerAI extends Model {
    private static final String TAG = AnswerAI.class.getSimpleName();

    // 日志格式字符串
    private static final String QUESTION_LOG_FORMAT = "题目📒 [%s] | 选项: %s";
    private static final String AI_ANSWER_LOG_FORMAT = "AI回答🧠 [%s] | AI类型: [%s] | 模型名称: [%s]";
    private static final String NORMAL_ANSWER_LOG_FORMAT = "普通回答🤖 [%s]";
    private static final String ERROR_AI_ANSWER = "AI回答异常：无法获取有效答案，请检查AI服务配置是否正确";

    /**
     * 是否启用本模块，配置字段，默认启用
     */
    public static final BooleanModelField enable = new BooleanModelField("enableAnswerAI", "启用AI答题模块", true);

    /**
     * AI服务接口实例
     */
    private static AnswerAIInterface answerAIInterface = AnswerAIInterface.getInstance();

    @Override
    public String getName() {
        return "AI答题";
    }

    @Override
    public ModelGroup getGroup() {
        return ModelGroup.OTHER;
    }

    @Override
    public String getIcon() {
        return "AnswerAI.svg";
    }

    /**
     * AI类型枚举及对应显示名
     */
    public interface AIType {
        int TONGYI = 0;
        int GEMINI = 1;
        int DEEPSEEK = 2;
        int CUSTOM = 3;

        String[] nickNames = {
                "通义千问",
                "Gemini",
                "DeepSeek",
                "自定义"
        };
    }

    // 选择AI类型的配置字段，默认选通义千问
    private static final ChoiceModelField aiType = new ChoiceModelField("useGeminiAI", "AI类型", AIType.TONGYI, AIType.nickNames);

    // 各种AI服务令牌及说明链接，方便用户获取和配置
    private final TextModelField.UrlTextModelField getTongyiAIToken = new TextModelField.UrlTextModelField(
            "getTongyiAIToken", "通义千问 | 获取令牌", "https://help.aliyun.com/zh/dashscope/developer-reference/acquisition-and-configuration-of-api-key");
    private final StringModelField tongYiToken = new StringModelField("tongYiToken", "qwen-turbo | 设置令牌", "");

    private final TextModelField.UrlTextModelField getGeminiAIToken = new TextModelField.UrlTextModelField(
            "getGeminiAIToken", "Gemini | 获取令牌", "https://aistudio.google.com/app/apikey");
    private final StringModelField GeminiToken = new StringModelField("GeminiAIToken", "gemini-1.5-flash | 设置令牌", "");

    private final TextModelField.UrlTextModelField getDeepSeekToken = new TextModelField.UrlTextModelField(
            "getDeepSeekToken", "DeepSeek | 获取令牌", "https://platform.deepseek.com/usage");
    private final StringModelField DeepSeekToken = new StringModelField("DeepSeekToken", "DeepSeek-R1 | 设置令牌", "");

    private final TextModelField.ReadOnlyTextModelField getCustomServiceToken = new TextModelField.ReadOnlyTextModelField(
            "getCustomServiceToken", "粉丝福利😍", "下面这个不用动可以白嫖到3月10号让我们感谢讯飞大善人🙏");

    private final StringModelField CustomServiceToken = new StringModelField("CustomServiceToken", "自定义服务 | 设置令牌", "sk-pQF9jek0CTTh3boKDcA9DdD7340a4e929eD00a13F681Cd8e");
    private final StringModelField CustomServiceUrl = new StringModelField("CustomServiceBaseUrl", "自定义服务 | 设置BaseUrl", "https://maas-api.cn-huabei-1.xf-yun.com/v1");
    private final StringModelField CustomServiceModel = new StringModelField("CustomServiceModel", "自定义服务 | 设置模型", "xdeepseekr1");

    /**
     * 返回所有配置字段，用于UI展示和存取
     */
    @Override
    public ModelFields getFields() {
        ModelFields modelFields = new ModelFields();
        modelFields.addField(enable); // 启用开关
        modelFields.addField(aiType); // AI类型
        modelFields.addField(getTongyiAIToken);
        modelFields.addField(tongYiToken);
        modelFields.addField(getGeminiAIToken);
        modelFields.addField(GeminiToken);
        modelFields.addField(getDeepSeekToken);
        modelFields.addField(DeepSeekToken);
        modelFields.addField(getCustomServiceToken);
        modelFields.addField(CustomServiceToken);
        modelFields.addField(CustomServiceUrl);
        modelFields.addField(CustomServiceModel);
        return modelFields;
    }

    /**
     * 模块启动时调用，初始化AI服务
     */
    @Override
    public void boot(ClassLoader classLoader) {
        try {
            // 读取启用状态
            boolean enabled = enable.getValue();
            if (!enabled) {
                Log.runtime(TAG, "AI答题模块未启用，跳过初始化");
                return;
            }
            int selectedType = aiType.getValue();
            Log.runtime(String.format("初始化AI服务：已选择[%s]", AIType.nickNames[selectedType]));
            initializeAIService(selectedType);
        } catch (Exception e) {
            Log.error(TAG, "初始化AI服务失败: " + e.getMessage());
            Log.printStackTrace(TAG, e);
        }
    }

    /**
     * 根据选择的AI类型初始化对应的AI服务实例
     */
    private void initializeAIService(int selectedType) {
        // 释放旧服务资源
        if (answerAIInterface != null) {
            answerAIInterface.release();
        }

        switch (selectedType) {
            case AIType.TONGYI:
                answerAIInterface = new TongyiAI(tongYiToken.getValue());
                break;
            case AIType.GEMINI:
                answerAIInterface = new GeminiAI(GeminiToken.getValue());
                break;
            case AIType.DEEPSEEK:
                answerAIInterface = new DeepSeek(DeepSeekToken.getValue());
                break;
            case AIType.CUSTOM:
                answerAIInterface = new CustomService(CustomServiceToken.getValue(), CustomServiceUrl.getValue());
                answerAIInterface.setModelName(CustomServiceModel.getValue());
                Log.runtime(String.format("已配置自定义服务：URL=[%s], Model=[%s]", CustomServiceUrl.getValue(), CustomServiceModel.getValue()));
                break;
            default:
                // 默认空实现，防止空指针
                answerAIInterface = AnswerAIInterface.getInstance();
                break;
        }
    }

    /**
     * 根据日志类型选择输出日志位置
     */
    private static void selectLogger(String flag, String msg) {
        switch (flag) {
            case "farm":
                Log.farm(msg);
                break;
            case "forest":
                Log.forest(msg);
                break;
            default:
                Log.other(msg);
                break;
        }
    }

    /**
     * AI 获取答案接口
     * @param text 问题文本
     * @param answerList 备选答案列表
     * @param flag 日志分类标识
     * @return 返回选择的答案字符串
     */
    public static String getAnswer(String text, List<String> answerList, String flag) {
        if (text == null || answerList == null) {
            selectLogger(flag, "问题或答案列表为空");
            return "";
        }
        String answerStr = "";
        try {
            String msg = String.format(QUESTION_LOG_FORMAT, text, answerList);
            selectLogger(flag, msg);
            if (enable.getValue() && answerAIInterface != null) {
                Integer answer = answerAIInterface.getAnswer(text, answerList);
                if (answer != null && answer >= 0 && answer < answerList.size()) {
                    answerStr = answerList.get(answer);
                    selectLogger(flag, String.format(AI_ANSWER_LOG_FORMAT, answerStr, AIType.nickNames[aiType.getValue()], answerAIInterface.getModelName()));
                } else {
                    Log.error(ERROR_AI_ANSWER);
                }
            } else if (!answerList.isEmpty()) {
                // 如果未启用AI，则默认选第一个答案
                answerStr = answerList.get(0);
                selectLogger(flag, String.format(NORMAL_ANSWER_LOG_FORMAT, answerStr));
            }
        } catch (Throwable t) {
            Log.printStackTrace(TAG, "AI获取答案异常:", t);
        }
        return answerStr;
    }
}
