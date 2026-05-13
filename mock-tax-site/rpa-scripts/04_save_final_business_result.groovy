import java.sql.Timestamp
import java.time.LocalDateTime

if (jdbcTemplate == null) throw new RuntimeException("未注入 jdbcTemplate")
if (objectMapper == null) throw new RuntimeException("未注入 objectMapper")

def processed = stepContext.processed ?: stepContext.step_3
if (processed == null) throw new RuntimeException("未获取到指标基础变量预处理结果")
if (stepContext.processingId == null) throw new RuntimeException("缺少 processingId")

def now = LocalDateTime.now()
def actualTaxNo = String.valueOf(processed.taxpayerIdNo ?: taxpayerIdNo)
def actualEnterpriseName = String.valueOf(processed.enterpriseName ?: enterpriseName)

def businessData = [
        taskId       : taskId,
        taskCode     : taskCode,
        executionId  : executionId,
        executionCode: executionCode,
        taxpayerIdNo : actualTaxNo,
        enterpriseName: actualEnterpriseName,
        processCode  : processCode,
        robotCode    : robotCode,
        generatedAt  : now.toString(),
        sourceRefs   : [
                collectionId : stepContext.collectionId,
                analysisId   : stepContext.analysisId,
                processingId : stepContext.processingId
        ],
        result       : processed
]

jdbcTemplate.update("""
    insert into data_business_final
    (task_id, execution_id, processing_id, taxpayer_id_no, enterprise_name, tax_area_id, indicator_code, data_status, business_data, create_time, update_time, tax_area_name)
    values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
""",
        taskId,
        executionId,
        stepContext.processingId,
        actualTaxNo,
        actualEnterpriseName,
        null,
        "BUSINESS_CREDIT_PROFILE",
        1,
        objectMapper.writeValueAsString(businessData),
        Timestamp.valueOf(now),
        Timestamp.valueOf(now),
        null
)

stepContext.finalBusinessData = businessData

return [
        executionId   : executionId,
        processingId  : stepContext.processingId,
        indicatorCode : "BUSINESS_CREDIT_PROFILE",
        enterpriseName: actualEnterpriseName,
        saved         : true
]
