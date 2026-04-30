# Playwright Groovy 示例脚本

下面这套脚本是把 `spider_exc` 里的采集思路改成适配当前 RPA 项目的版本。

适配点：

- 当前任务执行器会自动注入 `page`、`browser`、`playwright`
- 当前任务执行器会自动注入 `taskId`、`taskCode`、`enterpriseName`、`taxpayerIdNo`
- 当前任务执行器现在支持共享变量 `stepContext`
- 每一步的返回值也会自动保存到 `stepContext.lastResult` 和 `stepContext.step_步骤号`

使用前提：

- 流程步骤里的 `scriptLang` 选择 `groovy`
- 目标测试网站可访问
- 下面脚本中的 `baseUrl`、企业名称、申请日期按你的实际情况修改

## 方案一：单步骤直接测试

适合先验证你项目里 `Playwright + Groovy` 是否已经打通。

```groovy
def baseUrl = "http://study.zmyfrank.com:18010/spider/#"
def appDate = "2024-02-06"

def safeText = { String selector ->
    try {
        return page.locator(selector).first().innerText().trim()
    } catch (Exception ignored) {
        return ""
    }
}

page.navigate(baseUrl + "/enterprise-info")
page.waitForSelector("#enterprise-name-input")
page.fill("#enterprise-name-input", enterpriseName)
page.locator('button:has-text("查询")').first().click()
page.waitForSelector("#tax-no")

def payload = [
    enterpriseName: enterpriseName,
    taxNo: safeText("#tax-no"),
    uscCode: safeText("#usc-code"),
    appDate: appDate
]

page.navigate(baseUrl + "/invoice-query")
page.waitForSelector("#invoice-tax-no")
page.fill("#invoice-tax-no", payload.taxNo)
page.fill("#invoice-usc-code", payload.uscCode)
page.locator('button:has-text("查询")').first().click()
page.waitForSelector("tbody tr")

def invoices = []
int rowCount = page.locator("tbody tr").count()
for (int i = 0; i < rowCount; i++) {
    invoices << [
        sign: safeText("#invoice-sign-${i}"),
        state: safeText("#invoice-state-${i}"),
        invoiceTime: safeText("#invoice-time-${i}"),
        jshjText: safeText("#invoice-jshj-${i}")
    ]
}

payload.invoices = invoices
return payload
```

## 方案二：拆成三步测试

适合验证你现在的“多步骤流程设计”。

### 步骤 1：采集

步骤名建议：`采集发票数据`

```groovy
def baseUrl = "http://study.zmyfrank.com:18010/spider/#"
def appDate = "2024-02-06"

def safeText = { String selector ->
    try {
        return page.locator(selector).first().innerText().trim()
    } catch (Exception ignored) {
        return ""
    }
}

page.navigate(baseUrl + "/enterprise-info")
page.waitForSelector("#enterprise-name-input")
page.fill("#enterprise-name-input", enterpriseName)
page.locator('button:has-text("查询")').first().click()
page.waitForSelector("#tax-no")

def payload = [
    enterpriseName: enterpriseName,
    taxNo: safeText("#tax-no"),
    uscCode: safeText("#usc-code"),
    appDate: appDate
]

page.navigate(baseUrl + "/invoice-query")
page.waitForSelector("#invoice-tax-no")
page.fill("#invoice-tax-no", payload.taxNo)
page.fill("#invoice-usc-code", payload.uscCode)
page.locator('button:has-text("查询")').first().click()
page.waitForSelector("tbody tr")

def invoices = []
int rowCount = page.locator("tbody tr").count()
for (int i = 0; i < rowCount; i++) {
    invoices << [
        sign: safeText("#invoice-sign-${i}"),
        state: safeText("#invoice-state-${i}"),
        invoiceTime: safeText("#invoice-time-${i}"),
        jshjText: safeText("#invoice-jshj-${i}")
    ]
}

payload.invoices = invoices
stepContext.collected = payload
return payload
```

### 步骤 2：解析

步骤名建议：`解析发票数据`

```groovy
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

def payload = stepContext.collected ?: stepContext.step_1
if (payload == null) {
    throw new RuntimeException("未获取到上一步采集结果")
}

def appDate = LocalDate.parse(String.valueOf(payload.appDate), DateTimeFormatter.ofPattern("yyyy-MM-dd"))

def invoices = (payload.invoices ?: []).collect { row ->
    def item = [
        sign: row.sign,
        state: row.state,
        invoiceTime: row.invoiceTime,
        jshjText: row.jshjText
    ]

    try {
        def parsedTime = LocalDateTime.parse(String.valueOf(row.invoiceTime), DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
        int diff = (appDate.year - parsedTime.year) * 12 + (appDate.monthValue - parsedTime.monthValue)
        item.invoiceTimeParsed = parsedTime.toString()
        item.monthDiffToAppDate = diff
    } catch (Exception ignored) {
    }

    return item
}

def parsed = [
    enterpriseName: payload.enterpriseName,
    taxNo: payload.taxNo,
    uscCode: payload.uscCode,
    appDate: payload.appDate,
    invoices: invoices
]

stepContext.parsed = parsed
return parsed
```

### 步骤 3：加工统计

步骤名建议：`统计近12个月销项金额`

```groovy
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

def parsed = stepContext.parsed ?: stepContext.step_2
if (parsed == null) {
    throw new RuntimeException("未获取到上一步解析结果")
}

def appDate = LocalDate.parse(String.valueOf(parsed.appDate), DateTimeFormatter.ofPattern("yyyy-MM-dd"))
def invoices = parsed.invoices ?: []

def parseMoney = { value ->
    if (value == null) {
        return BigDecimal.ZERO
    }
    def cleaned = String.valueOf(value).trim().replace(",", "")
    if (!cleaned) {
        return BigDecimal.ZERO
    }
    try {
        return new BigDecimal(cleaned)
    } catch (Exception ignored) {
        return BigDecimal.ZERO
    }
}

def isInPrevious12Months = { LocalDate app, LocalDateTime invoiceTime ->
    int diff = (app.year - invoiceTime.year) * 12 + (app.monthValue - invoiceTime.monthValue)
    return diff >= 1 && diff <= 12
}

BigDecimal sum = BigDecimal.ZERO
int total = 0
int matched = 0
def matchedInvoices = []

invoices.each { inv ->
    total++
    if (String.valueOf(inv.sign) != "销项") {
        return
    }
    if (String.valueOf(inv.state) != "正常") {
        return
    }

    try {
        def invoiceTime = LocalDateTime.parse(String.valueOf(inv.invoiceTime), DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
        if (!isInPrevious12Months(appDate, invoiceTime)) {
            return
        }

        def amount = parseMoney(inv.jshjText)
        sum = sum.add(amount)
        matched++
        matchedInvoices << [
            sign: inv.sign,
            state: inv.state,
            invoiceTime: inv.invoiceTime,
            jshj: amount
        ]
    } catch (Exception ignored) {
    }
}

def processed = [
    indicatorCode: "inv_f1_12m_down_sale_jshj_sum_teach",
    saleJshjSum: sum.setScale(5, RoundingMode.HALF_UP),
    appDate: parsed.appDate,
    invoiceTotal: total,
    invoiceMatched: matched,
    matchedInvoices: matchedInvoices
]

stepContext.processed = processed
return processed
```

## 说明

- 这三步已经可以在你当前项目里串起来测试
- 目前还没有单独做“持久化结果表”的步骤脚本，因为你当前项目里没有现成的 `rpa_data_query` 那套实体和表结构
- 如果你后面想补“第四步入库”，建议先定下来你这个项目最终要落哪张结果表，我再按你现有数据库帮你补对应 Groovy

## 第四步：落库到 data_business_final

你当前项目里最终结果表建议写入 `data_business_final`。

当前后端执行器已经额外注入：

- `jdbcTemplate`
- `objectMapper`
- `executionId`
- `executionCode`

所以第四步可以直接写成下面这样。

步骤名建议：`保存最终业务结果`

```groovy
import java.time.LocalDateTime

def processed = stepContext.processed ?: stepContext.step_3
if (processed == null) {
    throw new RuntimeException("未获取到第三步加工结果")
}

def processingId = stepContext.processingId
if (processingId == null) {
    throw new RuntimeException("缺少 processingId。你的 data_business_final 表要求 processing_id 必须是真实存在的 data_processing 主键")
}

def businessData = [
    taskId: taskId,
    taskCode: taskCode,
    executionId: executionId,
    executionCode: executionCode,
    taxpayerIdNo: taxpayerIdNo,
    enterpriseName: enterpriseName,
    processCode: processCode,
    robotCode: robotCode,
    generatedAt: LocalDateTime.now().toString(),
    indicatorCode: processed.indicatorCode,
    result: processed
]

jdbcTemplate.update("""
    insert into data_business_final
    (task_id, execution_id, processing_id, taxpayer_id_no, enterprise_name, tax_area_id, tax_area_name, indicator_code, data_status, business_data)
    values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
""",
        taskId,
        executionId,
        processingId,
        taxpayerIdNo,
        enterpriseName,
        null,
        null,
        processed.indicatorCode,
        1,
        objectMapper.writeValueAsString(businessData)
)

stepContext.finalBusinessData = businessData
return [
    taskId: taskId,
    executionId: executionId,
    processingId: processingId,
    dataStatus: 1,
    saved: true
]
```

### 当前这一版的说明

- 这一步会直接新增一条 `data_business_final`
- 你的真实表结构里 `execution_id` 是必填，所以这里必须使用执行器注入的 `executionId`
- 你的真实表结构里 `processing_id` 也是必填且有关联外键，所以不能再用 `0` 兜底
- `business_data` 会保存完整 JSON，里面包含任务、纳税人、流程、机器人和加工结果

### 如果你想做到更标准

更完整的链路应该是：

1. 第一步采集时写 `data_collection`，并保存 `collectionId`
2. 第二步解析时写 `data_analysis`，并保存 `analysisId`
3. 第三步加工时写 `data_processing`，并保存 `processingId`
4. 第四步再把第三步生成的 `processingId` 和当前 `executionId` 一起写入 `data_business_final`

如果你要，我下一步可以继续把前 3 步也补成“每一步都真实落库”的完整版脚本。

## 完整版：四步真实落库

下面这套脚本是按你当前数据库真实表结构写的完整版。

特点：

- 第一步写 `data_collection`
- 第二步写 `data_analysis`
- 第三步写 `data_processing`
- 第四步写 `data_business_final`
- 每一步都会把生成的主键放入 `stepContext`

建议步骤名：

1. `采集并落库`
2. `解析并落库`
3. `加工并落库`
4. `保存最终业务结果`

### 第一步：采集并落库

```groovy
import org.springframework.jdbc.support.GeneratedKeyHolder
import java.sql.Statement
import java.sql.Timestamp
import java.time.LocalDateTime

def baseUrl = "http://study.zmyfrank.com:18010/spider/#"
def appDate = "2024-02-06"
def now = LocalDateTime.now()

def safeText = { String selector ->
    try {
        return page.locator(selector).first().innerText().trim()
    } catch (Exception ignored) {
        return ""
    }
}

page.navigate(baseUrl + "/enterprise-info")
page.waitForSelector("#enterprise-name-input")
page.fill("#enterprise-name-input", enterpriseName)
page.locator('button:has-text("查询")').first().click()
page.waitForSelector("#tax-no")

def payload = [
    enterpriseName: enterpriseName,
    taxNo: safeText("#tax-no"),
    uscCode: safeText("#usc-code"),
    appDate: appDate
]

page.navigate(baseUrl + "/invoice-query")
page.waitForSelector("#invoice-tax-no")
page.fill("#invoice-tax-no", payload.taxNo)
page.fill("#invoice-usc-code", payload.uscCode)
page.locator('button:has-text("查询")').first().click()
page.waitForSelector("tbody tr")

def invoices = []
int rowCount = page.locator("tbody tr").count()
for (int i = 0; i < rowCount; i++) {
    invoices << [
        sign: safeText("#invoice-sign-${i}"),
        state: safeText("#invoice-state-${i}"),
        invoiceTime: safeText("#invoice-time-${i}"),
        jshjText: safeText("#invoice-jshj-${i}")
    ]
}

payload.invoices = invoices

def sql = """
    insert into data_collection
    (task_id, execution_id, taxpayer_id_no, enterprise_name, source_name, status, raw_data, error_message, collect_time, collection_time, create_time, update_time)
    values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
"""

def keyHolder = new GeneratedKeyHolder()
jdbcTemplate.update({ con ->
    def ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)
    ps.setLong(1, taskId as Long)
    ps.setLong(2, executionId as Long)
    ps.setString(3, taxpayerIdNo)
    ps.setString(4, enterpriseName)
    ps.setString(5, "study-spider-demo")
    ps.setInt(6, 1)
    ps.setString(7, objectMapper.writeValueAsString(payload))
    ps.setString(8, null)
    ps.setTimestamp(9, Timestamp.valueOf(now))
    ps.setTimestamp(10, Timestamp.valueOf(now))
    ps.setTimestamp(11, Timestamp.valueOf(now))
    ps.setTimestamp(12, Timestamp.valueOf(now))
    return ps
}, keyHolder)

def collectionId = keyHolder.key?.longValue()
if (collectionId == null) {
    throw new RuntimeException("插入 data_collection 失败，未获取到主键")
}

stepContext.collected = payload
stepContext.collectionId = collectionId

return [
    collectionId: collectionId,
    invoiceCount: invoices.size(),
    taxpayerIdNo: taxpayerIdNo
]
```

### 第二步：解析并落库

```groovy
import org.springframework.jdbc.support.GeneratedKeyHolder
import java.sql.Statement
import java.sql.Timestamp
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

def payload = stepContext.collected ?: stepContext.step_1
if (payload == null) {
    throw new RuntimeException("未获取到采集结果")
}
if (stepContext.collectionId == null) {
    throw new RuntimeException("缺少 collectionId")
}

def appDate = LocalDate.parse(String.valueOf(payload.appDate), DateTimeFormatter.ofPattern("yyyy-MM-dd"))

def invoices = (payload.invoices ?: []).collect { row ->
    def item = [
        sign: row.sign,
        state: row.state,
        invoiceTime: row.invoiceTime,
        jshjText: row.jshjText
    ]
    try {
        def parsedTime = LocalDateTime.parse(String.valueOf(row.invoiceTime), DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
        int diff = (appDate.year - parsedTime.year) * 12 + (appDate.monthValue - parsedTime.monthValue)
        item.invoiceTimeParsed = parsedTime.toString()
        item.monthDiffToAppDate = diff
    } catch (Exception ignored) {
    }
    return item
}

def parsed = [
    enterpriseName: payload.enterpriseName,
    taxNo: payload.taxNo,
    uscCode: payload.uscCode,
    appDate: payload.appDate,
    invoices: invoices
]

def now = LocalDateTime.now()
def sql = """
    insert into data_analysis
    (task_id, execution_id, collection_id, taxpayer_id_no, enterprise_name, status, extracted_field_count, rule_name, parsed_data, error_message, analysis_time, create_time, update_time)
    values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
"""

def keyHolder = new GeneratedKeyHolder()
jdbcTemplate.update({ con ->
    def ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)
    ps.setLong(1, taskId as Long)
    ps.setLong(2, executionId as Long)
    ps.setLong(3, stepContext.collectionId as Long)
    ps.setString(4, taxpayerIdNo)
    ps.setString(5, enterpriseName)
    ps.setInt(6, 1)
    ps.setInt(7, invoices.size())
    ps.setString(8, "发票解析规则")
    ps.setString(9, objectMapper.writeValueAsString(parsed))
    ps.setString(10, null)
    ps.setTimestamp(11, Timestamp.valueOf(now))
    ps.setTimestamp(12, Timestamp.valueOf(now))
    ps.setTimestamp(13, Timestamp.valueOf(now))
    return ps
}, keyHolder)

def analysisId = keyHolder.key?.longValue()
if (analysisId == null) {
    throw new RuntimeException("插入 data_analysis 失败，未获取到主键")
}

stepContext.parsed = parsed
stepContext.analysisId = analysisId

return [
    analysisId: analysisId,
    extractedFieldCount: invoices.size()
]
```

### 第三步：加工并落库

```groovy
import org.springframework.jdbc.support.GeneratedKeyHolder
import java.math.BigDecimal
import java.math.RoundingMode
import java.sql.Statement
import java.sql.Timestamp
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

def parsed = stepContext.parsed ?: stepContext.step_2
if (parsed == null) {
    throw new RuntimeException("未获取到解析结果")
}
if (stepContext.analysisId == null) {
    throw new RuntimeException("缺少 analysisId")
}

def appDate = LocalDate.parse(String.valueOf(parsed.appDate), DateTimeFormatter.ofPattern("yyyy-MM-dd"))
def invoices = parsed.invoices ?: []

def parseMoney = { value ->
    if (value == null) {
        return BigDecimal.ZERO
    }
    def cleaned = String.valueOf(value).trim().replace(",", "")
    if (!cleaned) {
        return BigDecimal.ZERO
    }
    try {
        return new BigDecimal(cleaned)
    } catch (Exception ignored) {
        return BigDecimal.ZERO
    }
}

def isInPrevious12Months = { LocalDate app, LocalDateTime invoiceTime ->
    int diff = (app.year - invoiceTime.year) * 12 + (app.monthValue - invoiceTime.monthValue)
    return diff >= 1 && diff <= 12
}

BigDecimal sum = BigDecimal.ZERO
int total = 0
int matched = 0
def matchedInvoices = []

invoices.each { inv ->
    total++
    if (String.valueOf(inv.sign) != "销项") {
        return
    }
    if (String.valueOf(inv.state) != "正常") {
        return
    }

    try {
        def invoiceTime = LocalDateTime.parse(String.valueOf(inv.invoiceTime), DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
        if (!isInPrevious12Months(appDate, invoiceTime)) {
            return
        }

        def amount = parseMoney(inv.jshjText)
        sum = sum.add(amount)
        matched++
        matchedInvoices << [
            sign: inv.sign,
            state: inv.state,
            invoiceTime: inv.invoiceTime,
            jshj: amount
        ]
    } catch (Exception ignored) {
    }
}

def processed = [
    indicatorCode: "inv_f1_12m_down_sale_jshj_sum_teach",
    saleJshjSum: sum.setScale(5, RoundingMode.HALF_UP),
    appDate: parsed.appDate,
    invoiceTotal: total,
    invoiceMatched: matched,
    matchedInvoices: matchedInvoices
]

def validationDetail = [
    invoiceTotal: total,
    invoiceMatched: matched,
    matchedInvoices: matchedInvoices
]

def validationResult = "发票总数=${total}, 命中数=${matched}"
def now = LocalDateTime.now()

def sql = """
    insert into data_processing
    (task_id, execution_id, analysis_id, taxpayer_id_no, enterprise_name, status, validation_result, processed_data, validation_detail, error_message, process_time, processing_time, create_time, update_time)
    values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
"""

def keyHolder = new GeneratedKeyHolder()
jdbcTemplate.update({ con ->
    def ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)
    ps.setLong(1, taskId as Long)
    ps.setLong(2, executionId as Long)
    ps.setLong(3, stepContext.analysisId as Long)
    ps.setString(4, taxpayerIdNo)
    ps.setString(5, enterpriseName)
    ps.setInt(6, 1)
    ps.setString(7, validationResult)
    ps.setString(8, objectMapper.writeValueAsString(processed))
    ps.setString(9, objectMapper.writeValueAsString(validationDetail))
    ps.setString(10, null)
    ps.setTimestamp(11, Timestamp.valueOf(now))
    ps.setTimestamp(12, Timestamp.valueOf(now))
    ps.setTimestamp(13, Timestamp.valueOf(now))
    ps.setTimestamp(14, Timestamp.valueOf(now))
    return ps
}, keyHolder)

def processingId = keyHolder.key?.longValue()
if (processingId == null) {
    throw new RuntimeException("插入 data_processing 失败，未获取到主键")
}

stepContext.processed = processed
stepContext.processingId = processingId

return [
    processingId: processingId,
    indicatorCode: processed.indicatorCode,
    invoiceMatched: matched,
    saleJshjSum: processed.saleJshjSum
]
```

### 第四步：保存最终业务结果

```groovy
import java.sql.Timestamp
import java.time.LocalDateTime

def processed = stepContext.processed ?: stepContext.step_3
if (processed == null) {
    throw new RuntimeException("未获取到第三步加工结果")
}
if (stepContext.processingId == null) {
    throw new RuntimeException("缺少 processingId")
}

def now = LocalDateTime.now()
def businessData = [
    taskId: taskId,
    taskCode: taskCode,
    executionId: executionId,
    executionCode: executionCode,
    taxpayerIdNo: taxpayerIdNo,
    enterpriseName: enterpriseName,
    processCode: processCode,
    robotCode: robotCode,
    generatedAt: now.toString(),
    result: processed
]

jdbcTemplate.update("""
    insert into data_business_final
    (task_id, execution_id, processing_id, taxpayer_id_no, enterprise_name, tax_area_id, indicator_code, data_status, business_data, create_time, update_time, tax_area_name)
    values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
""",
        taskId,
        executionId,
        stepContext.processingId,
        taxpayerIdNo,
        enterpriseName,
        null,
        processed.indicatorCode,
        1,
        objectMapper.writeValueAsString(businessData),
        Timestamp.valueOf(now),
        Timestamp.valueOf(now),
        null
)

stepContext.finalBusinessData = businessData

return [
    executionId: executionId,
    processingId: stepContext.processingId,
    indicatorCode: processed.indicatorCode,
    saved: true
]
```

### 测试完成后你应该能看到

- `data_collection` 新增 1 条
- `data_analysis` 新增 1 条
- `data_processing` 新增 1 条
- `data_business_final` 新增 1 条

并且外键关系是：

- `data_analysis.collection_id -> data_collection.id`
- `data_processing.analysis_id -> data_analysis.id`
- `data_business_final.processing_id -> data_processing.id`
- 四张表都会关联同一个 `execution_id`
