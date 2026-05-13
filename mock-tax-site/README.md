# 税务经营数据测试站

这是给 RPA 流程使用的纯静态 mock 站点，路径兼容旧测试网站：

```text
http://127.0.0.1:18010/spider/#/enterprise-info
http://127.0.0.1:18010/spider/#/application
http://127.0.0.1:18010/spider/#/invoice-query
```

启动方式：

```powershell
cd C:\Users\雪\Desktop\RPA\mock-tax-site
python -m http.server 18010
```

然后把 RPA 流程脚本里的：

```groovy
def baseUrl = "http://study.zmyfrank.com:18010/spider/#"
```

改成：

```groovy
def baseUrl = "http://127.0.0.1:18010/spider/#"
```

已兼容旧流程使用的选择器：

- `#enterprise-name-input`
- `#tax-no`
- `#usc-code`
- `#app-tax-no`
- `#app-usc-code`
- `#app-date`
- `#app-type`
- `#app-date-display`
- `#invoice-tax-no`
- `#invoice-usc-code`
- `#invoice-sign-0`
- `#invoice-state-0`
- `#invoice-time-0`
- `#invoice-jshj-0`

新增可采集数据页：

- `/tax-report`：营业收入、应纳税额、已缴税额、税负率
- `/financial-report`：资产、负债、流动比率、利润率、应收账款
- `/risk-info`：异常发票、逾期次数、黑名单状态、风险评分
