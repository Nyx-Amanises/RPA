import org.springframework.jdbc.support.GeneratedKeyHolder
import java.math.BigDecimal
import java.sql.Statement
import java.sql.Timestamp
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

if (jdbcTemplate == null) throw new RuntimeException("未注入 jdbcTemplate")
if (objectMapper == null) throw new RuntimeException("未注入 objectMapper")

def payload = stepContext.collected
if (payload == null) payload = stepContext.step_1
if (payload == null) throw new RuntimeException("未获取到采集结果")
if (stepContext.collectionId == null) throw new RuntimeException("缺少 collectionId")

def money = { value ->
    if (value == null) return BigDecimal.ZERO
    def text = String.valueOf(value).replace(",", "").replace("万元", "").replace("元", "").trim()
    if (!text) return BigDecimal.ZERO
    try { return new BigDecimal(text) } catch (Exception ignored) { return BigDecimal.ZERO }
}

def pct = { value ->
    if (value == null) return BigDecimal.ZERO
    def text = String.valueOf(value).replace(",", "").trim()
    if (!text) return BigDecimal.ZERO
    try {
        if (text.endsWith("%")) return new BigDecimal(text[0..-2]).divide(new BigDecimal("100"), 10, BigDecimal.ROUND_HALF_UP)
        return new BigDecimal(text)
    } catch (Exception ignored) { return BigDecimal.ZERO }
}

def num = { value ->
    if (value == null) return 0
    def text = String.valueOf(value).replaceAll("[^0-9-]", "")
    return text ? Integer.parseInt(text) : 0
}

def getMap = { root, key ->
    def value = root == null ? null : root[key]
    return value instanceof Map ? value : [:]
}

def enterprise = getMap(payload, "enterprise")
def applicationRaw = getMap(payload, "application")
def taxRaw = getMap(payload, "taxReport")
def financeRaw = getMap(payload, "financial")
def riskRaw = getMap(payload, "risk")
def appDateText = String.valueOf(payload.appDate ?: "2024-02-06")
def appDate = LocalDate.parse(appDateText, DateTimeFormatter.ofPattern("yyyy-MM-dd"))
def invoiceFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

def invoices = []
(payload.invoices ?: []).each { row ->
    def parsedTime = null
    def diff = null
    try {
        parsedTime = LocalDateTime.parse(String.valueOf(row.invoiceTime), invoiceFmt)
        diff = (appDate.year - parsedTime.year) * 12 + (appDate.monthValue - parsedTime.monthValue)
    } catch (Exception ignored) {
    }
    invoices.add([
            invoiceNo         : row.invoiceNo,
            sign              : row.sign,
            state             : row.state,
            invoiceTime       : row.invoiceTime,
            invoiceTimeParsed : parsedTime == null ? null : parsedTime.toString(),
            monthDiffToAppDate: diff,
            inPrevious12Months: diff != null && diff >= 1 && diff <= 12,
            buyer             : row.buyer,
            seller            : row.seller,
            taxExclusive      : money(row.taxExclusive),
            taxAmount         : money(row.taxAmount),
            jshj              : money(row.jshjText),
            jshjText          : row.jshjText,
            abnormal          : String.valueOf(row.state) != "正常"
    ])
}

def parsed = [
        appDate    : appDateText,
        enterprise : enterprise,
        invoices   : invoices,
        application: [applyId: applicationRaw.applyId, applyDate: applicationRaw.applyDate, appType: applicationRaw.appType, applyAmount: money(applicationRaw.applyAmount), baseCreditLimit: money(applicationRaw.baseCreditLimit)],
        taxReport  : [revenue: money(taxRaw.revenue), taxPayable: money(taxRaw.taxPayable), taxPaid: money(taxRaw.taxPaid), taxRate: pct(taxRaw.taxRate), vat: money(taxRaw.vat), incomeTax: money(taxRaw.incomeTax)],
        financial  : [revenue: money(financeRaw.revenue), netProfit: money(financeRaw.netProfit), profitRate: pct(financeRaw.profitRate), totalAssets: money(financeRaw.totalAssets), totalLiabilities: money(financeRaw.totalLiabilities), assetDebtRatio: pct(financeRaw.assetDebtRatio), currentAssets: money(financeRaw.currentAssets), currentLiabilities: money(financeRaw.currentLiabilities), currentRatio: money(financeRaw.currentRatio), arBalance: money(financeRaw.arBalance), cash: money(financeRaw.cash)],
        risk       : [abnormalInvoiceCount: num(riskRaw.abnormalInvoiceCount), overdueCount: num(riskRaw.overdueCount), blacklist: riskRaw.blacklist, blacklistFlag: String.valueOf(riskRaw.blacklist) == "是", riskScore: num(riskRaw.riskScore), taxViolation: riskRaw.taxViolation, lawsuitCount: num(riskRaw.lawsuitCount)]
]

def now = LocalDateTime.now()
def actualTaxNo = String.valueOf(enterprise.taxNo ?: taxpayerIdNo)
def actualEnterpriseName = String.valueOf(enterprise.name ?: enterpriseName)
def sql = """
    insert into data_analysis
    (task_id, execution_id, collection_id, taxpayer_id_no, enterprise_name, status, extracted_field_count, rule_name, parsed_data, error_message, analysis_time, create_time, update_time)
    values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
"""

def keyHolder = new GeneratedKeyHolder()
jdbcTemplate.update({ con ->
    def ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)
    ps.setLong(1, taskId as Long); ps.setLong(2, executionId as Long); ps.setLong(3, stepContext.collectionId as Long)
    ps.setString(4, actualTaxNo); ps.setString(5, actualEnterpriseName); ps.setInt(6, 1); ps.setInt(7, invoices.size() + 30)
    ps.setString(8, "经营授信源数据解析规则"); ps.setString(9, objectMapper.writeValueAsString(parsed)); ps.setString(10, null)
    ps.setTimestamp(11, Timestamp.valueOf(now)); ps.setTimestamp(12, Timestamp.valueOf(now)); ps.setTimestamp(13, Timestamp.valueOf(now))
    return ps
}, keyHolder)

def analysisId = keyHolder.key?.longValue()
if (analysisId == null) throw new RuntimeException("插入 data_analysis 失败，未获取到主键")
stepContext.parsed = parsed
stepContext.analysisId = analysisId

return [analysisId: analysisId, extractedFieldCount: invoices.size() + 30, invoiceCount: invoices.size(), enterpriseName: actualEnterpriseName]
