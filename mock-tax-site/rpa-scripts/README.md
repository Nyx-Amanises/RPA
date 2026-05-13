# 经营授信数据采集与指标预处理流程脚本

按顺序新建 4 个 RPA 流程步骤，脚本语言选择 Groovy：

1. `01_collect_business_source_data.groovy`：采集企业、申请、发票、纳税、财务、风险数据，写入 `data_collection`
2. `02_parse_business_source_data.groovy`：清洗金额、比例、发票日期，写入 `data_analysis`
3. `03_preprocess_indicator_variables.groovy`：生成指标基础变量，写入 `data_processing`
4. `04_save_final_business_result.groovy`：保存最终业务结果，写入 `data_business_final`

运行前先启动 mock 测试站：

```powershell
cd C:\Users\雪\Desktop\RPA\mock-tax-site
python -m http.server 18010
```

第一步脚本里的测试站地址是：

```groovy
def baseUrl = "http://127.0.0.1:18010/spider/#"
```

流程跑成功后，可以在指标管理里配置公式，例如：

```text
SALE_JSHJ_SUM = saleJshjSum12m
TAX_RATE = taxPayable / taxRevenue
PROFIT_RATE = netProfit / financeRevenue
ASSET_DEBT_RATIO = totalLiabilities / totalAssets
CURRENT_RATIO = currentAssets / currentLiabilities
RISK_SCORE = riskScore
```
