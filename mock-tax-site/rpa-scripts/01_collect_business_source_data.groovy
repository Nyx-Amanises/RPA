import org.springframework.jdbc.support.GeneratedKeyHolder
import java.sql.Statement
import java.sql.Timestamp
import java.time.LocalDateTime

if (page == null) throw new RuntimeException("未注入 page")
if (jdbcTemplate == null) throw new RuntimeException("未注入 jdbcTemplate")
if (objectMapper == null) throw new RuntimeException("未注入 objectMapper")

def baseUrl = "http://127.0.0.1:18010/spider/#"
def appDate = "2024-02-06"
def now = LocalDateTime.now()

def safeText = { String selector ->
    try {
        return page.locator(selector).first().innerText().trim()
    } catch (Exception ignored) {
        return ""
    }
}

def safeValue = { String selector ->
    try {
        return page.locator(selector).first().inputValue().trim()
    } catch (Exception ignored) {
        return safeText(selector)
    }
}

def queryByTax = {
    page.fill("#query-tax-no", String.valueOf(stepContext.sourcePayload.enterprise.taxNo))
    page.fill("#query-usc-code", String.valueOf(stepContext.sourcePayload.enterprise.uscCode))
    page.locator('button:has-text("查询")').first().click()
}

def payload = [
        appDate    : appDate,
        enterprise : [:],
        application: [:],
        invoices   : [],
        taxReport  : [:],
        financial  : [:],
        risk       : [:]
]

page.navigate(baseUrl + "/enterprise-info")
page.waitForSelector("#enterprise-name-input")
page.fill("#enterprise-name-input", String.valueOf(enterpriseName ?: "重庆工程学院"))
page.locator('button:has-text("查询")').first().click()
page.waitForSelector("#tax-no")
payload.enterprise = [
        name             : safeText("#enterprise-name"),
        taxNo            : safeText("#tax-no"),
        uscCode          : safeText("#usc-code"),
        legalPerson      : safeText("#legal-person"),
        industry         : safeText("#industry"),
        taxArea          : safeText("#tax-area"),
        registeredCapital: safeText("#registered-capital"),
        establishedDate  : safeText("#established-date"),
        creditRating     : safeText("#credit-rating"),
        employeeCount    : safeText("#employee-count"),
        riskScore        : safeText("#risk-score"),
        abnormalInvoices : safeText("#abnormal-invoice-count")
]
stepContext.sourcePayload = payload

page.navigate(baseUrl + "/application")
page.waitForSelector("#app-tax-no")
page.fill("#app-tax-no", String.valueOf(payload.enterprise.taxNo))
page.fill("#app-usc-code", String.valueOf(payload.enterprise.uscCode))
page.fill("#app-date", appDate)
page.selectOption("#app-type", "credit")
page.locator('button[type="submit"]:has-text("提交申请")').click()
page.waitForSelector("#app-date-display")
payload.application = [
        applyId        : safeText("#apply-id"),
        applyDate      : safeText("#app-date-display"),
        appType        : safeValue("#app-type"),
        applyAmount    : safeText("#apply-amount"),
        baseCreditLimit: safeText("#base-credit-limit")
]

page.navigate(baseUrl + "/invoice-query")
page.waitForSelector("#invoice-tax-no")
page.fill("#invoice-tax-no", String.valueOf(payload.enterprise.taxNo))
page.fill("#invoice-usc-code", String.valueOf(payload.enterprise.uscCode))
page.locator('button:has-text("查询")').first().click()
page.waitForSelector("tbody tr")
payload.invoices = page.evaluate("""
() => Array.from(document.querySelectorAll("tbody tr")).map((row, i) => ({
  invoiceNo: document.querySelector("#invoice-no-" + i)?.innerText?.trim() || "",
  sign: document.querySelector("#invoice-sign-" + i)?.innerText?.trim() || "",
  state: document.querySelector("#invoice-state-" + i)?.innerText?.trim() || "",
  invoiceTime: document.querySelector("#invoice-time-" + i)?.innerText?.trim() || "",
  buyer: document.querySelector("#invoice-buyer-" + i)?.innerText?.trim() || "",
  seller: document.querySelector("#invoice-seller-" + i)?.innerText?.trim() || "",
  taxExclusive: document.querySelector("#invoice-tax-exclusive-" + i)?.innerText?.trim() || "",
  taxAmount: document.querySelector("#invoice-tax-amount-" + i)?.innerText?.trim() || "",
  jshjText: document.querySelector("#invoice-jshj-" + i)?.innerText?.trim() || ""
}))
""")

page.navigate(baseUrl + "/tax-report")
page.waitForSelector("#query-tax-no")
queryByTax()
page.waitForSelector("#tax-report-revenue")
payload.taxReport = [
        revenue   : safeText("#tax-report-revenue"),
        taxPayable: safeText("#tax-report-tax-payable"),
        taxPaid   : safeText("#tax-report-tax-paid"),
        taxRate   : safeText("#tax-report-tax-rate"),
        vat       : safeText("#tax-report-vat"),
        incomeTax : safeText("#tax-report-income-tax")
]

page.navigate(baseUrl + "/financial-report")
page.waitForSelector("#query-tax-no")
queryByTax()
page.waitForSelector("#finance-revenue")
payload.financial = [
        revenue           : safeText("#finance-revenue"),
        netProfit         : safeText("#finance-net-profit"),
        profitRate        : safeText("#finance-profit-rate"),
        totalAssets       : safeText("#finance-total-assets"),
        totalLiabilities  : safeText("#finance-total-liabilities"),
        assetDebtRatio    : safeText("#finance-debt-ratio"),
        currentAssets     : safeText("#finance-current-assets"),
        currentLiabilities: safeText("#finance-current-liabilities"),
        currentRatio      : safeText("#finance-current-ratio"),
        arBalance         : safeText("#finance-ar-balance"),
        cash              : safeText("#finance-cash")
]

page.navigate(baseUrl + "/risk-info")
page.waitForSelector("#query-tax-no")
queryByTax()
page.waitForSelector("#risk-score")
payload.risk = [
        abnormalInvoiceCount: safeText("#risk-abnormal-invoice-count"),
        overdueCount        : safeText("#risk-overdue-count"),
        blacklist           : safeText("#risk-blacklist"),
        riskScore           : safeText("#risk-score"),
        taxViolation        : safeText("#risk-tax-violation"),
        lawsuitCount        : safeText("#risk-lawsuit-count")
]

def actualTaxNo = String.valueOf(payload.enterprise.taxNo ?: taxpayerIdNo)
def actualEnterpriseName = String.valueOf(payload.enterprise.name ?: enterpriseName)

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
    ps.setString(3, actualTaxNo)
    ps.setString(4, actualEnterpriseName)
    ps.setString(5, "mock-tax-site")
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
if (collectionId == null) throw new RuntimeException("插入 data_collection 失败，未获取到主键")

stepContext.collected = payload
stepContext.collectionId = collectionId

return [
        collectionId  : collectionId,
        enterpriseName: actualEnterpriseName,
        taxpayerIdNo  : actualTaxNo,
        invoiceCount  : payload.invoices.size(),
        sourceName    : "mock-tax-site"
]
