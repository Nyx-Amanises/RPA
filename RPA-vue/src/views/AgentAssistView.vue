<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { fetchAiModelConfig, runAgentAssist, runAgentQuotaCalculate, saveAiModelConfig } from '../api/agent'
import { fetchQuotaRulePage } from '../api/indicator'

const loading = ref(false)
const savingModelConfig = ref(false)
const activeTask = ref('FORMULA_DRAFT')
const output = ref('')
const quotaRuleOptions = ref([])

const modelConfig = reactive({
  provider: 'deepseek',
  baseUrl: '',
  model: 'deepseek-chat',
  apiKey: '',
  apiKeySaved: false,
  updateTime: ''
})

const forms = reactive({
  FORMULA_DRAFT: {
    userInput: '根据净利润和财报营业收入计算利润率',
    context: ''
  },
  FORMULA_CHECK: {
    userInput: 'creditLimit = baseCreditLimit + saleJshjSum12m * 0.15 + unknownVar',
    context: ''
  },
  QUOTA_EXPLAIN: {
    userInput: '经营授信额度命中优质企业分支，额度公式为 baseCreditLimit + saleJshjSum12m * 0.15 + purchaseJshjSum12m * 0.05',
    context: '{"baseCreditLimit":1250000,"saleJshjSum12m":2335000,"purchaseJshjSum12m":203000,"riskScore":82,"taxRate":0.0425,"creditLimit":1615400}'
  }
})

const providerOptions = [
  { label: 'DeepSeek', value: 'deepseek', model: 'deepseek-chat', baseUrl: 'https://api.deepseek.com/chat/completions' },
  { label: '豆包 Chat Completions', value: 'doubao', model: '', baseUrl: 'https://ark.cn-beijing.volces.com/api/v3/chat/completions' },
  { label: '豆包 Responses', value: 'doubao-responses', model: '', baseUrl: 'https://ark.cn-beijing.volces.com/api/v3/responses' },
  { label: 'OpenAI', value: 'openai', model: 'gpt-4o-mini', baseUrl: 'https://api.openai.com/v1/chat/completions' },
  { label: '自定义', value: 'custom', model: '', baseUrl: '' }
]

const quotaAgentForm = reactive({
  quotaRuleId: '',
  executionId: '',
  taxpayerIdNo: '',
  enterpriseName: '',
  autoCalculateMissingIndicators: true
})

const currentForm = computed(() => forms[activeTask.value])
const canUseModel = computed(() => (modelConfig.apiKey || modelConfig.apiKeySaved) && modelConfig.model && resolvedBaseUrl.value)
const resolvedBaseUrl = computed(() => {
  if (modelConfig.baseUrl) return modelConfig.baseUrl
  return providerOptions.find((item) => item.value === modelConfig.provider)?.baseUrl || ''
})

function handleProviderChange(value) {
  const option = providerOptions.find((item) => item.value === value)
  if (!option) return
  modelConfig.baseUrl = option.baseUrl
  if (option.model) {
    modelConfig.model = option.model
  }
}

async function loadModelConfig() {
  try {
    const result = await fetchAiModelConfig()
    modelConfig.provider = result.provider || 'deepseek'
    modelConfig.baseUrl = result.baseUrl || ''
    modelConfig.model = result.model || ''
    modelConfig.apiKey = ''
    modelConfig.apiKeySaved = Boolean(result.apiKeySaved)
    modelConfig.updateTime = result.updateTime || ''
  } catch (error) {
    ElMessage.error(error.message || 'AI 模型配置加载失败')
  }
}

async function handleSaveModelConfig() {
  if (!modelConfig.provider) {
    ElMessage.warning('请选择供应商')
    return
  }
  if (!resolvedBaseUrl.value) {
    ElMessage.warning('请填写接口地址')
    return
  }
  if (!modelConfig.model) {
    ElMessage.warning('请填写模型名称')
    return
  }
  savingModelConfig.value = true
  try {
    const result = await saveAiModelConfig({
      provider: modelConfig.provider,
      baseUrl: resolvedBaseUrl.value,
      model: modelConfig.model,
      apiKey: modelConfig.apiKey
    })
    modelConfig.baseUrl = result.baseUrl || modelConfig.baseUrl
    modelConfig.model = result.model || modelConfig.model
    modelConfig.apiKey = ''
    modelConfig.apiKeySaved = Boolean(result.apiKeySaved)
    modelConfig.updateTime = result.updateTime || ''
    ElMessage.success('AI 模型配置已保存')
  } catch (error) {
    ElMessage.error(error.message || 'AI 模型配置保存失败')
  } finally {
    savingModelConfig.value = false
  }
}

async function handleRun() {
  if (activeTask.value === 'QUOTA_AGENT') {
    await handleQuotaAgentRun()
    return
  }
  if (!currentForm.value.userInput?.trim()) {
    ElMessage.warning('请输入内容')
    return
  }
  loading.value = true
  output.value = ''
  try {
    const result = await runAgentAssist({
      taskType: activeTask.value,
      provider: modelConfig.provider,
      baseUrl: resolvedBaseUrl.value,
      model: modelConfig.model,
      apiKey: modelConfig.apiKey,
      userInput: currentForm.value.userInput,
      context: currentForm.value.context
    })
    output.value = result.content || ''
    if (result.localFallback) {
      ElMessage.info('未使用外部模型，已返回本地规则结果')
    }
  } catch (error) {
    ElMessage.error(error.message || 'AI 辅助失败')
  } finally {
    loading.value = false
  }
}

async function handleQuotaAgentRun() {
  if (!quotaAgentForm.quotaRuleId) {
    ElMessage.warning('请选择额度计算规则')
    return
  }
  loading.value = true
  output.value = ''
  try {
    const result = await runAgentQuotaCalculate({
      quotaRuleId: quotaAgentForm.quotaRuleId,
      executionId: quotaAgentForm.executionId || null,
      taxpayerIdNo: quotaAgentForm.taxpayerIdNo,
      enterpriseName: quotaAgentForm.enterpriseName,
      autoCalculateMissingIndicators: quotaAgentForm.autoCalculateMissingIndicators,
      provider: modelConfig.provider,
      baseUrl: resolvedBaseUrl.value,
      model: modelConfig.model,
      apiKey: modelConfig.apiKey
    })
    output.value = formatQuotaAgentOutput(result)
    if (!result.modelUsed) {
      ElMessage.info('未调用外部模型，已使用本地 Agent 解释')
    }
  } catch (error) {
    ElMessage.error(error.message || 'AI 额度计算失败')
  } finally {
    loading.value = false
  }
}

async function loadQuotaRules() {
  try {
    const res = await fetchQuotaRulePage({ pageNum: 1, pageSize: 100 })
    quotaRuleOptions.value = res.data?.list || []
    if (!quotaAgentForm.quotaRuleId && quotaRuleOptions.value.length > 0) {
      quotaAgentForm.quotaRuleId = quotaRuleOptions.value[0].id
    }
  } catch (error) {
    ElMessage.error(error.message || '额度规则加载失败')
  }
}

function formatQuotaAgentOutput(result) {
  const sections = []
  if (result.summary) {
    sections.push(result.summary)
  }
  if (result.steps?.length) {
    sections.push(`执行步骤：\n${result.steps.map((item, index) => `${index + 1}. ${item}`).join('\n')}`)
  }
  if (result.warnings?.length) {
    sections.push(`提示：\n${result.warnings.map((item) => `- ${item}`).join('\n')}`)
  }
  if (result.calculatedIndicators?.length) {
    const indicators = result.calculatedIndicators
      .map((item) => `${item.indicatorCode || item.resultVarName}: ${item.displayValue || item.resultValue}`)
      .join('\n')
    sections.push(`自动计算指标：\n${indicators}`)
  }
  if (result.quotaResult) {
    sections.push(`额度结果：\n${JSON.stringify(result.quotaResult, null, 2)}`)
  }
  if (result.explanation) {
    sections.push(`Agent 解释：\n${result.explanation}`)
  }
  return sections.join('\n\n')
}

function fillFormulaDraftExample(type) {
  const examples = {
    tax: '根据应纳税额和纳税申报营业收入计算税负率',
    profit: '根据净利润和财报营业收入计算利润率',
    sale: '计算近12个月正常销项价税合计',
    invest: '根据风险评分、利润率、流动比率、资产负债率、异常发票数和逾期次数计算投资评级得分'
  }
  forms.FORMULA_DRAFT.userInput = examples[type]
}

onMounted(() => {
  loadModelConfig()
  loadQuotaRules()
})
</script>

<template>
  <div class="agent-page">
    <div class="page-actions">
      <el-alert
        type="info"
        show-icon
        :closable="false"
        title="AI Agent 可辅助配置，也可编排额度计算；最终金额仍由后端受限公式引擎确定性计算。模型配置只随本次请求发送，不会保存。"
      />
    </div>

    <el-card shadow="never" class="agent-panel">
      <template #header>
        <div class="panel-title">模型配置</div>
      </template>
      <el-form label-width="110px" class="model-form">
        <el-row :gutter="16">
          <el-col :span="6">
            <el-form-item label="供应商">
              <el-select v-model="modelConfig.provider" style="width: 100%" @change="handleProviderChange">
                <el-option v-for="item in providerOptions" :key="item.value" :label="item.label" :value="item.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item label="模型">
              <el-input v-model="modelConfig.model" placeholder="如 deepseek-chat / gpt-4o-mini" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="接口地址">
              <el-input v-model="modelConfig.baseUrl" placeholder="chat/completions 或 responses 完整地址" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="API Key">
          <el-input
            v-model="modelConfig.apiKey"
            type="password"
            show-password
            :placeholder="modelConfig.apiKeySaved ? '已保存；留空继续使用已保存 Key' : '不填写时仅使用本地规则'"
          />
        </el-form-item>
        <div class="model-config-actions">
          <el-button type="primary" plain :loading="savingModelConfig" @click="handleSaveModelConfig">保存模型配置</el-button>
          <span class="run-hint">
            {{ modelConfig.apiKeySaved ? 'API Key 已保存在数据库中，刷新页面后可继续使用' : '尚未保存 API Key' }}
          </span>
        </div>
      </el-form>
    </el-card>

    <el-card shadow="never" class="agent-panel">
      <template #header>
        <div class="panel-title">Agent 辅助</div>
      </template>
      <el-tabs v-model="activeTask">
        <el-tab-pane label="自然语言转公式" name="FORMULA_DRAFT">
          <div class="quick-buttons">
            <el-button text type="primary" @click="fillFormulaDraftExample('tax')">税负率</el-button>
            <el-button text type="primary" @click="fillFormulaDraftExample('profit')">利润率</el-button>
            <el-button text type="primary" @click="fillFormulaDraftExample('sale')">销项金额</el-button>
            <el-button text type="primary" @click="fillFormulaDraftExample('invest')">投资评级</el-button>
          </div>
          <el-input v-model="forms.FORMULA_DRAFT.userInput" type="textarea" :rows="5" placeholder="描述你想计算的指标" />
        </el-tab-pane>

        <el-tab-pane label="检查公式引用" name="FORMULA_CHECK">
          <el-input v-model="forms.FORMULA_CHECK.userInput" type="textarea" :rows="5" placeholder="粘贴指标公式或额度公式" />
        </el-tab-pane>

        <el-tab-pane label="解释额度结果" name="QUOTA_EXPLAIN">
          <el-input v-model="forms.QUOTA_EXPLAIN.userInput" type="textarea" :rows="4" placeholder="描述额度规则、命中分支或计算结果" />
          <el-input v-model="forms.QUOTA_EXPLAIN.context" type="textarea" :rows="5" class="context-input" placeholder="可粘贴指标值、输出模板、额度结果 JSON" />
        </el-tab-pane>

        <el-tab-pane label="AI额度计算" name="QUOTA_AGENT">
          <el-form label-width="120px" class="quota-agent-form">
            <el-row :gutter="16">
              <el-col :span="12">
                <el-form-item label="额度规则">
                  <el-select v-model="quotaAgentForm.quotaRuleId" style="width: 100%" placeholder="请选择额度计算规则">
                    <el-option
                      v-for="item in quotaRuleOptions"
                      :key="item.id"
                      :label="`${item.quotaName}（ID: ${item.id}）`"
                      :value="item.id"
                    />
                  </el-select>
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="执行记录ID">
                  <el-input v-model.number="quotaAgentForm.executionId" placeholder="可选；指定时使用该批次指标结果" />
                </el-form-item>
              </el-col>
            </el-row>
            <el-row :gutter="16">
              <el-col :span="12">
                <el-form-item label="纳税人识别号">
                  <el-input v-model="quotaAgentForm.taxpayerIdNo" placeholder="可选；按企业筛选指标结果" />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="企业名称">
                  <el-input v-model="quotaAgentForm.enterpriseName" placeholder="可选；纳税人识别号为空时使用" />
                </el-form-item>
              </el-col>
            </el-row>
            <el-form-item label="自动补算指标">
              <el-switch
                v-model="quotaAgentForm.autoCalculateMissingIndicators"
                active-text="启用"
                inactive-text="停用"
              />
            </el-form-item>
          </el-form>
        </el-tab-pane>
      </el-tabs>

      <div class="run-row">
        <el-button type="primary" :loading="loading" @click="handleRun">
          {{ activeTask === 'QUOTA_AGENT' ? '运行 Agent' : (canUseModel ? '调用模型' : '本地辅助') }}
        </el-button>
        <span class="run-hint">
          {{ activeTask === 'QUOTA_AGENT'
            ? 'Agent 将调用指标计算和额度计算工具，模型只用于生成解释'
            : (canUseModel ? '将调用后端模型适配器' : '未填完整模型配置，将使用本地规则') }}
        </span>
      </div>
    </el-card>

    <el-card shadow="never" class="agent-panel result-panel">
      <template #header>
        <div class="panel-title">输出结果</div>
      </template>
      <pre v-if="output" class="agent-output">{{ output }}</pre>
      <el-empty v-else description="暂无输出" />
    </el-card>
  </div>
</template>

<style scoped>
.agent-page {
  display: grid;
  gap: 16px;
}

.page-actions {
  max-width: 1180px;
}

.agent-panel {
  max-width: 1180px;
  border-radius: 8px;
}

.panel-title {
  font-weight: 700;
  color: #1f2a44;
}

.model-form :deep(.el-form-item) {
  margin-bottom: 14px;
}

.model-config-actions {
  display: flex;
  align-items: center;
  gap: 12px;
  padding-left: 110px;
}

.quick-buttons {
  margin-bottom: 10px;
}

.context-input {
  margin-top: 12px;
}

.quota-agent-form {
  max-width: 1040px;
}

.quota-agent-form :deep(.el-form-item) {
  margin-bottom: 14px;
}

.run-row {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-top: 16px;
}

.run-hint {
  color: #667085;
  font-size: 13px;
}

.result-panel {
  min-height: 220px;
}

.agent-output {
  margin: 0;
  white-space: pre-wrap;
  word-break: break-word;
  font-family: "Microsoft YaHei", Arial, sans-serif;
  line-height: 1.7;
  color: #1f2a44;
}
</style>
