package com.rpa.manage.service.impl.agent;

import com.rpa.manage.common.exception.BusinessException;
import com.rpa.manage.domain.dto.agent.AiModelConfigRequest;
import com.rpa.manage.domain.dto.agent.AiModelConfigResponse;
import com.rpa.manage.domain.dto.agent.AgentAssistRequest;
import com.rpa.manage.domain.dto.agent.AgentAssistResponse;
import com.rpa.manage.domain.dto.agent.AgentQuotaCalculateRequest;
import com.rpa.manage.domain.dto.agent.AgentQuotaCalculateResponse;
import com.rpa.manage.domain.dto.indicator.QuotaCalculateRequest;
import com.rpa.manage.domain.entity.AiModelConfig;
import com.rpa.manage.domain.entity.IndicatorDefinition;
import com.rpa.manage.domain.repository.AiModelConfigRepository;
import com.rpa.manage.domain.repository.IndicatorDefinitionRepository;
import com.rpa.manage.service.agent.AgentAssistService;
import com.rpa.manage.service.indicator.IndicatorService;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Service
@RequiredArgsConstructor
public class AgentAssistServiceImpl implements AgentAssistService {

    private static final Pattern IDENTIFIER_PATTERN = Pattern.compile("[\\p{L}_][\\p{L}\\p{N}_]*");
    private static final String DEFAULT_MODEL_CONFIG_KEY = "default";
    private static final Set<String> FORMULA_KEYWORDS = Set.of(
            "and", "or", "true", "false", "sum", "where", "between", "else"
    );
    private static final List<String> BUSINESS_VARIABLES = List.of(
            "saleJshjSum12m", "purchaseJshjSum12m", "abnormalInvoiceCount12m",
            "taxRevenue", "taxPayable", "taxPaid", "taxRate",
            "financeRevenue", "netProfit", "profitRate", "totalAssets", "totalLiabilities",
            "assetDebtRatio", "currentAssets", "currentLiabilities", "currentRatio",
            "arBalance", "arTurnover", "cash", "riskScore", "overdueCount",
            "blacklistFlag", "lawsuitCount", "baseCreditLimit", "applyAmount",
            "companyId", "enterpriseName", "taxpayerIdNo", "creditLimit", "investScore"
    );

    private final AiModelConfigRepository aiModelConfigRepository;
    private final IndicatorDefinitionRepository indicatorDefinitionRepository;
    private final IndicatorService indicatorService;
    private final ObjectMapper objectMapper;

    @Override
    public AgentAssistResponse assist(AgentAssistRequest request) {
        String taskType = normalizeTaskType(request.getTaskType());
        String localResult = switch (taskType) {
            case "FORMULA_DRAFT" -> draftFormulaLocally(request.getUserInput());
            case "FORMULA_CHECK" -> checkFormulaLocally(request.getUserInput());
            case "QUOTA_EXPLAIN" -> explainQuotaLocally(request.getUserInput(), request.getContext());
            default -> throw new BusinessException("不支持的 Agent 任务类型：" + request.getTaskType());
        };

        AgentAssistRequest effectiveRequest = withSavedModelConfig(request);
        if (!hasModelConfig(effectiveRequest)) {
            return new AgentAssistResponse(localResult, true, "");
        }

        String prompt = buildPrompt(taskType, request, localResult);
        String content = callChatCompletions(effectiveRequest, prompt);
        return new AgentAssistResponse(content, false, normalizeModel(effectiveRequest));
    }

    @Override
    public AgentQuotaCalculateResponse calculateQuota(AgentQuotaCalculateRequest request) {
        List<String> steps = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        List<Map<String, Object>> calculatedIndicators = new ArrayList<>();

        Map<String, Object> ruleDetail = indicatorService.quotaRuleDetail(request.getQuotaRuleId());
        String quotaName = textValue(ruleDetail.get("quotaName"));
        List<Long> indicatorIds = toLongList(ruleDetail.get("indicatorIds"));
        List<String> indicatorCodes = toStringList(ruleDetail.get("indicatorCodes"));
        if (indicatorIds.isEmpty()) {
            throw new BusinessException("额度规则未关联指标，无法由 Agent 执行额度计算");
        }

        steps.add("读取额度规则：" + quotaName);
        steps.add("识别规则关联指标：" + String.join(", ", indicatorCodes));

        if (Boolean.FALSE.equals(request.getAutoCalculateMissingIndicators())) {
            steps.add("按用户设置跳过自动补算指标，直接使用已有指标结果");
        } else if (request.getExecutionId() != null) {
            warnings.add("已指定执行记录ID，Agent 不会自动补算最新指标，避免与指定批次不一致");
            steps.add("使用指定执行记录ID：" + request.getExecutionId());
        } else {
            steps.add("自动调用指标计算工具，补齐规则需要的最新指标结果");
            for (Long indicatorId : indicatorIds) {
                Map<String, Object> indicatorResult = indicatorService.calculateIndicator(indicatorId);
                calculatedIndicators.add(indicatorResult);
                steps.add("完成指标计算：" + textValue(indicatorResult.get("indicatorCode"))
                        + " = " + textValue(indicatorResult.get("displayValue")));
            }
        }

        QuotaCalculateRequest quotaRequest = new QuotaCalculateRequest();
        quotaRequest.setExecutionId(request.getExecutionId());
        quotaRequest.setTaxpayerIdNo(request.getTaxpayerIdNo());
        quotaRequest.setEnterpriseName(request.getEnterpriseName());
        steps.add("调用确定性额度计算工具，执行分支匹配和公式计算");
        Map<String, Object> quotaResult = indicatorService.calculateQuotaRule(request.getQuotaRuleId(), quotaRequest);
        steps.add("额度计算完成，命中分支：" + textValue(quotaResult.get("branchName"))
                + "，结果：" + textValue(quotaResult.get("displayValue")));

        String localExplanation = buildLocalQuotaAgentExplanation(ruleDetail, calculatedIndicators, quotaResult, steps);
        String explanation = localExplanation;
        boolean modelUsed = false;
        String model = "";
        AgentAssistRequest modelRequest = withSavedModelConfig(
                toAssistRequest(request, quotaName, ruleDetail, calculatedIndicators, quotaResult, steps)
        );
        if (hasModelConfig(modelRequest)) {
            try {
                explanation = callChatCompletions(modelRequest, buildQuotaAgentPrompt(ruleDetail, calculatedIndicators, quotaResult, steps));
                modelUsed = true;
                model = normalizeModel(modelRequest);
            } catch (BusinessException ex) {
                warnings.add("模型解释生成失败，已保留本地解释：" + ex.getMessage());
            }
        } else {
            warnings.add("未配置完整模型信息，Agent 使用本地解释模板生成说明");
        }

        AgentQuotaCalculateResponse response = new AgentQuotaCalculateResponse();
        response.setSummary("AI Agent 已完成额度计算：" + quotaName + " = " + textValue(quotaResult.get("displayValue")));
        response.setSteps(steps);
        response.setWarnings(warnings);
        response.setCalculatedIndicators(calculatedIndicators);
        response.setQuotaResult(quotaResult);
        response.setExplanation(explanation);
        response.setModelUsed(modelUsed);
        response.setModel(model);
        return response;
    }

    @Override
    public AiModelConfigResponse getModelConfig() {
        return aiModelConfigRepository.findByConfigKey(DEFAULT_MODEL_CONFIG_KEY)
                .map(this::toModelConfigResponse)
                .orElseGet(() -> new AiModelConfigResponse(
                        "deepseek",
                        "https://api.deepseek.com/chat/completions",
                        "deepseek-chat",
                        false,
                        null
                ));
    }

    @Override
    public AiModelConfigResponse saveModelConfig(AiModelConfigRequest request) {
        String provider = normalizeNullable(request.getProvider());
        String baseUrl = normalizeNullable(request.getBaseUrl());
        String model = normalizeNullable(request.getModel());
        if (!StringUtils.hasText(provider)) {
            throw new BusinessException("请选择模型供应商");
        }
        if (!StringUtils.hasText(baseUrl)) {
            throw new BusinessException("请填写模型接口地址");
        }
        if (!StringUtils.hasText(model)) {
            throw new BusinessException("请填写模型名称");
        }

        AiModelConfig config = aiModelConfigRepository.findByConfigKey(DEFAULT_MODEL_CONFIG_KEY)
                .orElseGet(() -> {
                    AiModelConfig created = new AiModelConfig();
                    created.setConfigKey(DEFAULT_MODEL_CONFIG_KEY);
                    return created;
                });
        config.setProvider(provider);
        config.setBaseUrl(baseUrl);
        config.setModel(model);
        if (Boolean.TRUE.equals(request.getClearApiKey())) {
            config.setApiKey(null);
        } else if (StringUtils.hasText(request.getApiKey())) {
            config.setApiKey(request.getApiKey().trim());
        }
        aiModelConfigRepository.save(config);
        return toModelConfigResponse(config);
    }

    private String normalizeTaskType(String taskType) {
        return StringUtils.hasText(taskType) ? taskType.trim().toUpperCase(Locale.ROOT) : "";
    }

    private AgentAssistRequest withSavedModelConfig(AgentAssistRequest request) {
        Optional<AiModelConfig> savedConfig = aiModelConfigRepository.findByConfigKey(DEFAULT_MODEL_CONFIG_KEY);
        if (savedConfig.isEmpty()) {
            return request;
        }
        AiModelConfig config = savedConfig.get();
        AgentAssistRequest merged = new AgentAssistRequest();
        merged.setTaskType(request.getTaskType());
        merged.setUserInput(request.getUserInput());
        merged.setContext(request.getContext());
        merged.setProvider(StringUtils.hasText(request.getProvider()) ? request.getProvider() : config.getProvider());
        merged.setBaseUrl(StringUtils.hasText(request.getBaseUrl()) ? request.getBaseUrl() : config.getBaseUrl());
        merged.setModel(StringUtils.hasText(request.getModel()) ? request.getModel() : config.getModel());
        merged.setApiKey(StringUtils.hasText(request.getApiKey()) ? request.getApiKey() : config.getApiKey());
        return merged;
    }

    private AiModelConfigResponse toModelConfigResponse(AiModelConfig config) {
        return new AiModelConfigResponse(
                config.getProvider(),
                config.getBaseUrl(),
                config.getModel(),
                StringUtils.hasText(config.getApiKey()),
                config.getUpdateTime()
        );
    }

    private AgentAssistRequest toAssistRequest(
            AgentQuotaCalculateRequest request,
            String quotaName,
            Map<String, Object> ruleDetail,
            List<Map<String, Object>> calculatedIndicators,
            Map<String, Object> quotaResult,
            List<String> steps
    ) {
        AgentAssistRequest modelRequest = new AgentAssistRequest();
        modelRequest.setTaskType("QUOTA_EXPLAIN");
        modelRequest.setProvider(request.getProvider());
        modelRequest.setBaseUrl(request.getBaseUrl());
        modelRequest.setModel(request.getModel());
        modelRequest.setApiKey(request.getApiKey());
        modelRequest.setUserInput("解释 AI Agent 本次额度计算过程：" + quotaName);
        modelRequest.setContext(toJsonText(Map.of(
                "ruleDetail", ruleDetail,
                "calculatedIndicators", calculatedIndicators,
                "quotaResult", quotaResult,
                "steps", steps
        )));
        return modelRequest;
    }

    private String buildQuotaAgentPrompt(
            Map<String, Object> ruleDetail,
            List<Map<String, Object>> calculatedIndicators,
            Map<String, Object> quotaResult,
            List<String> steps
    ) {
        return """
                你是 RPA 指标额度计算 Agent。
                本次不是让你重新计算额度金额，额度金额已经由后端受限公式工具确定性计算完成。
                你的任务是解释这次 Agent 如何完成额度计算：读取了什么规则、自动计算了哪些指标、命中了哪个分支、最终结果是多少。
                如果发现风险，只说明风险，不要改写最终额度。

                Agent 步骤：
                %s

                额度规则详情：
                %s

                自动计算的指标：
                %s

                额度计算结果：
                %s

                请用中文输出，结构清晰，不要输出 Markdown 表格。
                """.formatted(
                String.join("\n", steps),
                toJsonText(ruleDetail),
                toJsonText(calculatedIndicators),
                toJsonText(quotaResult)
        );
    }

    private String buildLocalQuotaAgentExplanation(
            Map<String, Object> ruleDetail,
            List<Map<String, Object>> calculatedIndicators,
            Map<String, Object> quotaResult,
            List<String> steps
    ) {
        String quotaName = textValue(ruleDetail.get("quotaName"));
        String branchName = textValue(quotaResult.get("branchName"));
        String displayValue = textValue(quotaResult.get("displayValue"));
        StringBuilder builder = new StringBuilder();
        builder.append("本次由 AI Agent 编排完成“").append(quotaName).append("”额度计算。\n");
        builder.append("Agent 先读取额度规则和关联指标，再调用后端确定性工具完成指标补算、分支匹配和额度公式计算。\n");
        builder.append("执行步骤：\n");
        for (int i = 0; i < steps.size(); i++) {
            builder.append(i + 1).append(". ").append(steps.get(i)).append("\n");
        }
        if (!calculatedIndicators.isEmpty()) {
            builder.append("本次自动计算指标数量：").append(calculatedIndicators.size()).append("。\n");
        }
        builder.append("最终命中分支：").append(branchName).append("。\n");
        builder.append("最终额度结果：").append(displayValue).append("。\n");
        builder.append("额度金额仍由后端受限公式引擎计算，AI Agent 负责流程编排和解释。");
        return builder.toString();
    }

    private boolean hasModelConfig(AgentAssistRequest request) {
        return StringUtils.hasText(request.getApiKey())
                && StringUtils.hasText(request.getModel())
                && StringUtils.hasText(resolveBaseUrl(request));
    }

    private String buildPrompt(String taskType, AgentAssistRequest request, String localResult) {
        String capability = switch (taskType) {
            case "FORMULA_DRAFT" -> "把用户的自然语言需求转换为本系统受限公式草稿。";
            case "FORMULA_CHECK" -> "检查公式是否引用了不存在的指标或变量，并给出修复建议。";
            case "QUOTA_EXPLAIN" -> "解释某个指标额度计算结果为什么这样算，说明命中的分支、使用的指标和公式。";
            default -> "完成指标管理辅助任务。";
        };
        return """
                你是 RPA 指标管理平台的 AI Agent 助手。
                任务：%s

                系统公式规则：
                - 支持赋值：RESULT = 表达式
                - 支持 + - * /、括号、比较符、and/or、百分数、字符串
                - 支持 sum(发票列表 where 条件).jshj
                - 请优先使用平台已有变量，不要编造字段

                平台可用变量和指标：
                %s

                本地规则初步结果：
                %s

                用户输入：
                %s

                业务上下文：
                %s

                请用中文输出，内容要能直接帮助用户填表或定位问题。不要输出 Markdown 表格。
                """.formatted(
                capability,
                String.join(", ", availableVariables()),
                localResult,
                request.getUserInput(),
                StringUtils.hasText(request.getContext()) ? request.getContext() : "无"
        );
    }

    private String callChatCompletions(AgentAssistRequest request, String prompt) {
        try {
            String url = resolveBaseUrl(request);
            Map<String, Object> payload = buildModelPayload(request, prompt, url);
            String body = objectMapper.writeValueAsString(payload);
            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(300))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + request.getApiKey().trim())
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
            HttpResponse<String> response = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(120))
                    .build()
                    .send(httpRequest, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new BusinessException("模型调用失败，HTTP " + response.statusCode() + "：" + response.body());
            }
            return parseModelResponse(response.body());
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BusinessException("模型调用失败：" + ex.getMessage());
        }
    }

    private Map<String, Object> buildModelPayload(AgentAssistRequest request, String prompt, String url) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("model", normalizeModel(request));
        payload.put("temperature", 0.2);
        if (isResponsesEndpoint(url)) {
            payload.put("input", "你是严谨的 RPA 指标公式和额度规则助手。\n\n" + prompt);
            payload.put("stream", false);
            return payload;
        }
        payload.put("messages", List.of(
                Map.of("role", "system", "content", "你是严谨的 RPA 指标公式和额度规则助手。"),
                Map.of("role", "user", "content", prompt)
        ));
        return payload;
    }

    private boolean isResponsesEndpoint(String url) {
        return StringUtils.hasText(url) && url.toLowerCase(Locale.ROOT).contains("/responses");
    }

    private String parseModelResponse(String responseBody) throws Exception {
        JsonNode root = objectMapper.readTree(responseBody);
        JsonNode outputText = root.path("output_text");
        if (!outputText.isMissingNode() && StringUtils.hasText(outputText.asText())) {
            return outputText.asText();
        }
        JsonNode output = root.path("output");
        if (output.isArray()) {
            StringBuilder builder = new StringBuilder();
            for (JsonNode item : output) {
                JsonNode content = item.path("content");
                if (content.isArray()) {
                    for (JsonNode contentItem : content) {
                        String text = contentItem.path("text").asText();
                        if (StringUtils.hasText(text)) {
                            builder.append(text);
                        }
                    }
                }
            }
            if (!builder.isEmpty()) {
                return builder.toString();
            }
        }
        JsonNode choices = root.path("choices");
        if (choices.isArray() && !choices.isEmpty()) {
            JsonNode message = choices.get(0).path("message");
            if (!message.path("content").isMissingNode()) {
                return message.path("content").asText();
            }
            JsonNode text = choices.get(0).path("text");
            if (!text.isMissingNode()) {
                return text.asText();
            }
        }
        throw new BusinessException("模型响应缺少可解析的输出内容");
    }

    private String resolveBaseUrl(AgentAssistRequest request) {
        if (StringUtils.hasText(request.getBaseUrl())) {
            return request.getBaseUrl().trim();
        }
        String provider = StringUtils.hasText(request.getProvider())
                ? request.getProvider().trim().toLowerCase(Locale.ROOT)
                : "";
        return switch (provider) {
            case "openai" -> "https://api.openai.com/v1/chat/completions";
            case "deepseek" -> "https://api.deepseek.com/chat/completions";
            case "doubao" -> "https://ark.cn-beijing.volces.com/api/v3/chat/completions";
            case "doubao-responses" -> "https://ark.cn-beijing.volces.com/api/v3/responses";
            default -> "";
        };
    }

    private String normalizeModel(AgentAssistRequest request) {
        return StringUtils.hasText(request.getModel()) ? request.getModel().trim() : "";
    }

    private String normalizeNullable(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String draftFormulaLocally(String input) {
        String text = input == null ? "" : input;
        if (text.contains("税负率")) {
            return "公式草稿：TAX_RATE = taxPayable / taxRevenue\n结果变量名建议：taxRate";
        }
        if (text.contains("利润率")) {
            return "公式草稿：PROFIT_RATE = netProfit / financeRevenue\n结果变量名建议：profitRate";
        }
        if (text.contains("资产负债率")) {
            return "公式草稿：ASSET_DEBT_RATIO = totalLiabilities / totalAssets\n结果变量名建议：assetDebtRatio";
        }
        if (text.contains("流动比率")) {
            return "公式草稿：CURRENT_RATIO = currentAssets / currentLiabilities\n结果变量名建议：currentRatio";
        }
        if (text.contains("销项") || text.contains("销售")) {
            return "公式草稿：SALE_JSHJ_SUM = saleJshjSum12m\n结果变量名建议：saleJshjSum12m";
        }
        if (text.contains("评级") || text.contains("评分")) {
            return "公式草稿：INVEST_SCORE = riskScore * 0.6 + profitRate * 100 * 0.2 + currentRatio * 8 + (1 - assetDebtRatio) * 100 * 0.2 - abnormalInvoiceCount12m * 2 - overdueCount * 5\n结果变量名建议：investScore";
        }
        return "暂未匹配到固定模板。可以描述指标含义，例如“净利润除以营业收入得到利润率”，或配置模型后由 AI 生成草稿。";
    }

    private String checkFormulaLocally(String formula) {
        Set<String> known = new LinkedHashSet<>(availableVariables());
        Set<String> identifiers = extractIdentifiers(formula);
        List<String> missing = identifiers.stream()
                .filter(item -> !known.contains(item))
                .filter(item -> !FORMULA_KEYWORDS.contains(item.toLowerCase(Locale.ROOT)))
                .toList();
        if (missing.isEmpty()) {
            return "本地检查通过：未发现明显不存在的变量或指标引用。";
        }
        return "本地检查发现可能不存在的变量：%s\n请确认这些变量是否为指标编码、结果变量名，或是否需要先新增/计算对应指标。"
                .formatted(String.join(", ", missing));
    }

    private String explainQuotaLocally(String input, String context) {
        return """
                本地解释模板：
                1. 先根据判断逻辑从上到下匹配分支。
                2. 命中第一个条件为 true 的分支；如果都不命中，则使用默认分支。
                3. 使用该分支的额度计算公式，代入已选指标结果变量。
                4. 将计算出的结果变量渲染到输出数据模板。

                用户输入：
                %s

                上下文：
                %s
                """.formatted(input, StringUtils.hasText(context) ? context : "未提供");
    }

    private Set<String> extractIdentifiers(String formula) {
        String sanitized = formula == null ? "" : formula.replaceAll("'[^']*'|\"[^\"]*\"", " ");
        Set<String> result = new LinkedHashSet<>();
        Matcher matcher = IDENTIFIER_PATTERN.matcher(sanitized);
        while (matcher.find()) {
            result.add(matcher.group());
        }
        return result;
    }

    private List<String> availableVariables() {
        Set<String> variables = new LinkedHashSet<>(BUSINESS_VARIABLES);
        List<IndicatorDefinition> indicators = indicatorDefinitionRepository.findAll();
        for (IndicatorDefinition indicator : indicators) {
            addIfText(variables, indicator.getIndicatorCode());
            addIfText(variables, toLowerCamel(indicator.getIndicatorCode()));
            addIfText(variables, indicator.getResultVarName());
            addIfText(variables, indicator.getIndicatorName());
        }
        return new ArrayList<>(variables);
    }

    private void addIfText(Set<String> values, String value) {
        if (StringUtils.hasText(value)) {
            values.add(value.trim());
        }
    }

    private String toLowerCamel(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        StringBuilder result = new StringBuilder();
        boolean upperNext = false;
        for (char ch : value.toLowerCase(Locale.ROOT).toCharArray()) {
            if (ch == '_' || ch == '-' || ch == ' ') {
                upperNext = true;
            } else if (upperNext) {
                result.append(Character.toUpperCase(ch));
                upperNext = false;
            } else {
                result.append(ch);
            }
        }
        return result.toString();
    }

    private List<Long> toLongList(Object value) {
        if (!(value instanceof List<?> list)) {
            return List.of();
        }
        List<Long> result = new ArrayList<>();
        for (Object item : list) {
            if (item instanceof Number number) {
                result.add(number.longValue());
            } else if (item != null && StringUtils.hasText(String.valueOf(item))) {
                result.add(Long.parseLong(String.valueOf(item).trim()));
            }
        }
        return result;
    }

    private List<String> toStringList(Object value) {
        if (!(value instanceof List<?> list)) {
            return List.of();
        }
        return list.stream()
                .map(this::textValue)
                .filter(StringUtils::hasText)
                .toList();
    }

    private String textValue(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private String toJsonText(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception ignored) {
            return String.valueOf(value);
        }
    }
}
