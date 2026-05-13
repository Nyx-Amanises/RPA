import org.springframework.jdbc.support.GeneratedKeyHolder
import java.math.BigDecimal
import java.math.RoundingMode
import java.sql.Statement
import java.sql.Timestamp
import java.time.LocalDateTime

if (jdbcTemplate == null) throw new RuntimeException("未注入 jdbcTemplate")
if (objectMapper == null) throw new RuntimeException("未注入 objectMapper")

def parsed = stepContext.parsed ?: stepContext.step_2
if (parsed == null) throw new RuntimeException("未获取到解析结果")
if (stepContext.analysisId == null) throw new RuntimeException("缺少 analysisId")

def decimal = { value ->
    if (value == null) return BigDecimal.ZERO
    if (value instanceof BigDecimal) return value
    try {
        return new BigDecimal(String.valueOf(value).replace(",", "").trim())
    } catch (Exception ignored) {
        return BigDecimal.ZERO
    }
}

def divide = { left, right ->
    def divisor = decimal(right)
    if (divisor.compareTo(BigDecimal.ZERO) == 0) return BigDecimal.ZERO
    return decimal(left).divide(divisor, 10, RoundingMode.HALF_UP)
}

def sumInvoices = { predicate ->
    BigDecimal total = BigDecimal.ZERO
    (parsed.invoices ?: []).each { inv ->
        if (predicate(inv)) {
            total = total.add(decimal(inv.jshj))
        }
    }
    return total.setScale(2, RoundingMode.HALF_UP)
}

def countInvoices = { predicate ->
    int count = 0
    (parsed.invoices ?: []).each { inv ->
        if (predicate(inv)) count++
    }
    return count
}

def inWindow = { inv -> Boolean.TRUE.equals(inv.inPrevious12Months) }
def isNormal = { inv -> String.valueOf(inv.state) == "正常" }

def saleJshjSum12m = sumInvoices { inv -> inWindow(inv) && isNormal(inv) && String.valueOf(inv.sign) == "销项" }
def purchaseJshjSum12m = sumInvoices { inv -> inWindow(inv) && isNormal(inv) && String.valueOf(inv.sign) == "进项" }
def abnormalInvoiceCount12m = countInvoices { inv -> inWindow(inv) && !isNormal(inv) }

def taxRevenue = decimal(parsed.taxReport?.revenue)
def taxPayable = decimal(parsed.taxReport?.taxPayable)
def taxPaid = decimal(parsed.taxReport?.taxPaid)
def taxRate = decimal(parsed.taxReport?.taxRate)
if (taxRate.compareTo(BigDecimal.ZERO) == 0) {
    taxRate = divide(taxPayable, taxRevenue)
}

def financeRevenue = decimal(parsed.financial?.revenue)
def netProfit = decimal(parsed.financial?.netProfit)
def totalAssets = decimal(parsed.financial?.totalAssets)
def totalLiabilities = decimal(parsed.financial?.totalLiabilities)
def currentAssets = decimal(parsed.financial?.currentAssets)
def currentLiabilities = decimal(parsed.financial?.currentLiabilities)
def arBalance = decimal(parsed.financial?.arBalance)
def cash = decimal(parsed.financial?.cash)

def profitRate = divide(netProfit, financeRevenue)
def assetDebtRatio = divide(totalLiabilities, totalAssets)
def currentRatio = divide(currentAssets, currentLiabilities)
def arTurnover = divide(financeRevenue, arBalance)

def baseCreditLimit = decimal(parsed.application?.baseCreditLimit)
def applyAmount = decimal(parsed.application?.applyAmount)
def riskScore = decimal(parsed.risk?.riskScore)
def overdueCount = decimal(parsed.risk?.overdueCount)
def lawsuitCount = decimal(parsed.risk?.lawsuitCount)
def blacklistFlag = Boolean.TRUE.equals(parsed.risk?.blacklistFlag)

def processed = [
        indicatorCode          : "BUSINESS_CREDIT_PROFILE",
        appDate                : parsed.appDate,
        enterpriseName         : parsed.enterprise?.name,
        taxpayerIdNo           : parsed.enterprise?.taxNo,
        saleJshjSum12m         : saleJshjSum12m,
        purchaseJshjSum12m     : purchaseJshjSum12m,
        abnormalInvoiceCount12m: abnormalInvoiceCount12m,
        taxRevenue             : taxRevenue,
        taxPayable             : taxPayable,
        taxPaid                : taxPaid,
        taxRate                : taxRate,
        financeRevenue         : financeRevenue,
        netProfit              : netProfit,
        profitRate             : profitRate,
        totalAssets            : totalAssets,
        totalLiabilities       : totalLiabilities,
        assetDebtRatio         : assetDebtRatio,
        currentAssets          : currentAssets,
        currentLiabilities     : currentLiabilities,
        currentRatio           : currentRatio,
        arBalance              : arBalance,
        arTurnover             : arTurnover,
        cash                   : cash,
        riskScore              : riskScore,
        overdueCount           : overdueCount,
        blacklistFlag          : blacklistFlag,
        lawsuitCount           : lawsuitCount,
        baseCreditLimit        : baseCreditLimit,
        applyAmount            : applyAmount
]

def validationDetail = [
        invoiceTotal           : (parsed.invoices ?: []).size(),
        saleInvoiceMatched12m  : countInvoices { inv -> inWindow(inv) && isNormal(inv) && String.valueOf(inv.sign) == "销项" },
        purchaseInvoiceMatched12m: countInvoices { inv -> inWindow(inv) && isNormal(inv) && String.valueOf(inv.sign) == "进项" },
        abnormalInvoiceCount12m: abnormalInvoiceCount12m,
        blacklisted            : blacklistFlag,
        overdueCount           : overdueCount
]

def validationResult = "发票总数=${validationDetail.invoiceTotal}, 近12月正常销项=${validationDetail.saleInvoiceMatched12m}, 异常发票=${abnormalInvoiceCount12m}, 风险评分=${riskScore}"
def now = LocalDateTime.now()
def actualTaxNo = String.valueOf(parsed.enterprise?.taxNo ?: taxpayerIdNo)
def actualEnterpriseName = String.valueOf(parsed.enterprise?.name ?: enterpriseName)

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
    ps.setString(4, actualTaxNo)
    ps.setString(5, actualEnterpriseName)
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
if (processingId == null) throw new RuntimeException("插入 data_processing 失败，未获取到主键")

stepContext.processed = processed
stepContext.validationDetail = validationDetail
stepContext.processingId = processingId

return [
        processingId          : processingId,
        indicatorCode         : processed.indicatorCode,
        saleJshjSum12m        : saleJshjSum12m,
        taxRate               : taxRate,
        profitRate            : profitRate,
        riskScore             : riskScore
]
