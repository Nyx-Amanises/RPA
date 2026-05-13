const MOCK_DATA = [
  {
    name: '重庆工程学院',
    taxNo: '0001',
    uscCode: '91500000MA5U123456',
    legalPerson: '张明',
    industry: '教育服务',
    taxArea: '重庆市巴南区税务局',
    registeredCapital: '12,000.00万元',
    establishedDate: '2001-08-21',
    employeeCount: 826,
    creditRating: 'A',
    application: {
      applyId: 'APPLY-20240206-0001',
      applyDate: '2024-02-06',
      appType: 'credit',
      applyAmount: 3500000,
      baseCreditLimit: 1250000
    },
    taxReport: {
      period: '2023年度',
      revenue: 12680000,
      taxPayable: 538900,
      taxPaid: 512300,
      vat: 392000,
      incomeTax: 146900,
      taxRate: 0.0425
    },
    financial: {
      revenue: 12680000,
      netProfit: 1420000,
      totalAssets: 28600000,
      totalLiabilities: 9500000,
      currentAssets: 14200000,
      currentLiabilities: 4200000,
      arBalance: 1680000,
      cash: 3220000
    },
    risk: {
      abnormalInvoiceCount: 2,
      overdueCount: 0,
      blacklist: '否',
      riskScore: 82,
      taxViolation: '无',
      lawsuitCount: 1
    },
    invoices: [
      invoice('INV-202401-001', '销项', '正常', '2024-01-15 10:12:30', '重庆某某科技有限公司', '重庆工程学院', 253097.35, 32902.65),
      invoice('INV-202312-006', '销项', '正常', '2023-12-20 16:04:11', '重庆智造科技有限公司', '重庆工程学院', 281415.93, 36584.07),
      invoice('INV-202312-011', '进项', '正常', '2023-12-02 09:31:18', '重庆工程学院', '重庆科教设备有限公司', 63716.81, 8283.19),
      invoice('INV-202311-003', '销项', '正常', '2023-11-07 11:36:45', '重庆启明教育服务有限公司', '重庆工程学院', 146017.70, 18982.30),
      invoice('INV-202310-012', '销项', '正常', '2023-10-26 14:02:22', '重庆城市服务集团', '重庆工程学院', 203539.82, 26460.18),
      invoice('INV-202310-018', '进项', '正常', '2023-10-03 15:12:07', '重庆工程学院', '重庆云网软件有限公司', 56637.17, 7362.83),
      invoice('INV-202309-009', '销项', '正常', '2023-09-12 08:42:53', '重庆南山科技有限公司', '重庆工程学院', 167256.64, 21743.36),
      invoice('INV-202308-004', '销项', '红冲', '2023-08-18 10:24:30', '重庆测试客户有限公司', '重庆工程学院', 21238.94, 2761.06),
      invoice('INV-202308-019', '进项', '正常', '2023-08-29 13:35:44', '重庆工程学院', '重庆星河物业有限公司', 51327.43, 6672.57),
      invoice('INV-202307-014', '销项', '正常', '2023-07-03 09:47:12', '重庆智慧校园有限公司', '重庆工程学院', 185840.71, 24159.29),
      invoice('INV-202306-008', '销项', '作废', '2023-06-22 17:18:01', '重庆作废测试客户', '重庆工程学院', 44247.79, 5752.21),
      invoice('INV-202305-002', '销项', '正常', '2023-05-11 12:08:39', '重庆北城数字科技有限公司', '重庆工程学院', 138053.10, 17946.90),
      invoice('INV-202304-015', '进项', '正常', '2023-04-18 16:50:24', '重庆工程学院', '重庆办公用品有限公司', 53982.30, 7017.70),
      invoice('INV-202303-005', '销项', '正常', '2023-03-09 10:40:10', '重庆产教融合有限公司', '重庆工程学院', 175221.24, 22778.76),
      invoice('INV-202302-010', '销项', '正常', '2023-02-16 15:22:30', '重庆协同创新有限公司', '重庆工程学院', 214159.29, 27840.71),
      invoice('INV-202401-020', '进项', '正常', '2024-01-08 09:15:44', '重庆工程学院', '重庆教学仪器有限公司', 76106.19, 9893.81),
      invoice('INV-202402-002', '销项', '正常', '2024-02-01 09:10:00', '重庆当月测试有限公司', '重庆工程学院', 70796.46, 9203.54),
      invoice('INV-202212-027', '销项', '正常', '2022-12-27 10:10:10', '重庆历史客户有限公司', '重庆工程学院', 88495.58, 11504.42)
    ]
  },
  {
    name: '重庆某某科技有限公司',
    taxNo: '91500000MA5U123456',
    uscCode: '91500000MA5U123456',
    legalPerson: '李华',
    industry: '软件和信息技术服务',
    taxArea: '重庆两江新区税务局',
    registeredCapital: '3,000.00万元',
    establishedDate: '2018-05-16',
    employeeCount: 132,
    creditRating: 'B',
    application: {
      applyId: 'APPLY-20240206-0002',
      applyDate: '2024-02-06',
      appType: 'loan',
      applyAmount: 1800000,
      baseCreditLimit: 780000
    },
    taxReport: {
      period: '2023年度',
      revenue: 8420000,
      taxPayable: 420600,
      taxPaid: 398200,
      vat: 286000,
      incomeTax: 134600,
      taxRate: 0.0499
    },
    financial: {
      revenue: 8420000,
      netProfit: 706000,
      totalAssets: 13200000,
      totalLiabilities: 6200000,
      currentAssets: 6500000,
      currentLiabilities: 3100000,
      arBalance: 1210000,
      cash: 980000
    },
    risk: {
      abnormalInvoiceCount: 4,
      overdueCount: 1,
      blacklist: '否',
      riskScore: 66,
      taxViolation: '轻微逾期申报1次',
      lawsuitCount: 2
    },
    invoices: [
      invoice('T-202401-101', '销项', '正常', '2024-01-19 13:21:00', '重庆工程学院', '重庆某某科技有限公司', 159292.04, 20707.96),
      invoice('T-202312-087', '销项', '正常', '2023-12-21 11:02:12', '重庆工业服务有限公司', '重庆某某科技有限公司', 119469.03, 15530.97),
      invoice('T-202311-066', '销项', '正常', '2023-11-13 16:33:54', '重庆云启科技有限公司', '重庆某某科技有限公司', 88495.58, 11504.42),
      invoice('T-202309-044', '销项', '红冲', '2023-09-19 09:20:35', '重庆红冲客户', '重庆某某科技有限公司', 17699.12, 2300.88),
      invoice('T-202308-029', '进项', '正常', '2023-08-09 14:15:00', '重庆某某科技有限公司', '重庆服务器有限公司', 74336.28, 9663.72),
      invoice('T-202306-017', '销项', '正常', '2023-06-25 10:45:06', '重庆智慧物流有限公司', '重庆某某科技有限公司', 139823.01, 18176.99),
      invoice('T-202305-011', '进项', '正常', '2023-05-06 10:30:00', '重庆某某科技有限公司', '重庆办公网络有限公司', 35398.23, 4601.77),
      invoice('T-202303-008', '销项', '正常', '2023-03-28 16:18:42', '重庆制造企业A', '重庆某某科技有限公司', 98230.09, 12769.91)
    ]
  },
  {
    name: '重庆智造科技有限公司',
    taxNo: '91500108MA7TEST001',
    uscCode: '91500108MA7TEST001',
    legalPerson: '王璐',
    industry: '智能制造',
    taxArea: '重庆市南岸区税务局',
    registeredCapital: '5,800.00万元',
    establishedDate: '2016-10-09',
    employeeCount: 246,
    creditRating: 'A',
    application: {
      applyId: 'APPLY-20240206-0003',
      applyDate: '2024-02-06',
      appType: 'credit',
      applyAmount: 5200000,
      baseCreditLimit: 2100000
    },
    taxReport: {
      period: '2023年度',
      revenue: 24100000,
      taxPayable: 1130000,
      taxPaid: 1123000,
      vat: 820000,
      incomeTax: 310000,
      taxRate: 0.0469
    },
    financial: {
      revenue: 24100000,
      netProfit: 3160000,
      totalAssets: 43800000,
      totalLiabilities: 13700000,
      currentAssets: 21800000,
      currentLiabilities: 6100000,
      arBalance: 2320000,
      cash: 5160000
    },
    risk: {
      abnormalInvoiceCount: 1,
      overdueCount: 0,
      blacklist: '否',
      riskScore: 89,
      taxViolation: '无',
      lawsuitCount: 0
    },
    invoices: [
      invoice('M-202401-023', '销项', '正常', '2024-01-23 09:11:20', '重庆汽车零部件有限公司', '重庆智造科技有限公司', 336283.19, 43716.81),
      invoice('M-202312-031', '销项', '正常', '2023-12-17 15:32:51', '成都供应链有限公司', '重庆智造科技有限公司', 283185.84, 36814.16),
      invoice('M-202311-020', '进项', '正常', '2023-11-28 14:04:30', '重庆智造科技有限公司', '重庆金属材料有限公司', 181415.93, 23584.07),
      invoice('M-202310-018', '销项', '正常', '2023-10-19 10:19:08', '重庆工业客户有限公司', '重庆智造科技有限公司', 247787.61, 32212.39),
      invoice('M-202308-014', '销项', '正常', '2023-08-02 17:03:10', '重庆成套设备有限公司', '重庆智造科技有限公司', 442477.88, 57522.12),
      invoice('M-202307-009', '进项', '正常', '2023-07-26 13:14:22', '重庆智造科技有限公司', '重庆电子元件有限公司', 126548.67, 16451.33),
      invoice('M-202305-004', '销项', '作废', '2023-05-11 09:00:00', '重庆作废测试客户', '重庆智造科技有限公司', 35398.23, 4601.77)
    ]
  }
];

const state = {
  currentEnterprise: MOCK_DATA[0],
  applicationDate: '2024-02-06'
};

window.mockTaxData = MOCK_DATA;

function invoice(no, sign, stateText, invoiceTime, buyer, seller, amount, taxAmount) {
  const jshj = amount + taxAmount;
  return {
    no,
    sign,
    state: stateText,
    invoiceTime,
    buyer,
    seller,
    amount,
    taxAmount,
    jshj,
    category: sign === '销项' ? '收入类' : '成本类'
  };
}

function money(value) {
  return Number(value || 0).toLocaleString('zh-CN', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2
  });
}

function percent(value) {
  return `${(Number(value || 0) * 100).toFixed(2)}%`;
}

function normalizeText(value) {
  return String(value || '').trim();
}

function findEnterpriseByName(name) {
  const keyword = normalizeText(name);
  if (!keyword) return state.currentEnterprise;
  return MOCK_DATA.find(item => item.name === keyword)
    || MOCK_DATA.find(item => item.name.includes(keyword) || keyword.includes(item.name))
    || { ...MOCK_DATA[0], name: keyword };
}

function findEnterpriseByTax(taxNo, uscCode) {
  const tax = normalizeText(taxNo);
  const usc = normalizeText(uscCode);
  return MOCK_DATA.find(item => item.taxNo === tax || item.uscCode === usc) || state.currentEnterprise;
}

function setActiveRoute(route) {
  document.querySelectorAll('[data-route]').forEach(link => {
    link.classList.toggle('active', link.dataset.route === route);
  });
}

function route() {
  return location.hash.replace(/^#/, '') || '/enterprise-info';
}

function render() {
  const currentRoute = route();
  setActiveRoute(currentRoute);
  const app = document.getElementById('app');
  const renderers = {
    '/enterprise-info': renderEnterpriseInfo,
    '/application': renderApplication,
    '/invoice-query': renderInvoiceQuery,
    '/tax-report': renderTaxReport,
    '/financial-report': renderFinancialReport,
    '/risk-info': renderRiskInfo
  };
  app.innerHTML = (renderers[currentRoute] || renderEnterpriseInfo)();
}

function pageHeader(title, subtitle) {
  return `
    <div class="page-head">
      <div>
        <h1>${title}</h1>
        <p>${subtitle}</p>
      </div>
      <span class="tag ok">稳定测试数据</span>
    </div>
  `;
}

function enterpriseToolbar() {
  return `
    <div class="toolbar">
      <div class="field grow">
        <label for="enterprise-name-input">企业名称</label>
        <input id="enterprise-name-input" value="${state.currentEnterprise.name}" placeholder="请输入企业名称" />
      </div>
      <button data-action="query-enterprise">查询</button>
      <button class="secondary" data-action="switch-sample">切换样本</button>
    </div>
  `;
}

function renderEnterpriseInfo() {
  const e = state.currentEnterprise;
  return `
    ${pageHeader('企业信息查询', '兼容旧流程：#enterprise-name-input、#tax-no、#usc-code')}
    ${enterpriseToolbar()}
    <section class="panel">
      <h2>企业基础信息</h2>
      <div class="summary-grid">
        ${kv('企业名称', e.name, 'enterprise-name')}
        ${kv('纳税人识别号', e.taxNo, 'tax-no')}
        ${kv('统一社会信用代码', e.uscCode, 'usc-code')}
        ${kv('法定代表人', e.legalPerson, 'legal-person')}
        ${kv('所属行业', e.industry, 'industry')}
        ${kv('主管税务机关', e.taxArea, 'tax-area')}
        ${kv('注册资本', e.registeredCapital, 'registered-capital')}
        ${kv('成立日期', e.establishedDate, 'established-date')}
      </div>
    </section>
    <section class="panel">
      <h2>测试摘要</h2>
      <div class="metric-grid">
        ${metric('信用评级', e.creditRating, 'credit-rating', 'ok')}
        ${metric('员工人数', `${e.employeeCount} 人`, 'employee-count')}
        ${metric('风险评分', e.risk.riskScore, 'risk-score')}
        ${metric('异常发票数', e.risk.abnormalInvoiceCount, 'abnormal-invoice-count', e.risk.abnormalInvoiceCount > 2 ? 'warn' : 'ok')}
      </div>
    </section>
  `;
}

function renderApplication() {
  const e = state.currentEnterprise;
  const app = e.application;
  return `
    ${pageHeader('授信申请', '兼容旧流程：#app-tax-no、#app-usc-code、#app-date、#app-type、#app-date-display')}
    <form class="toolbar" data-action="submit-application">
      <div class="field">
        <label for="app-tax-no">纳税人识别号</label>
        <input id="app-tax-no" value="${e.taxNo}" />
      </div>
      <div class="field">
        <label for="app-usc-code">统一社会信用代码</label>
        <input id="app-usc-code" value="${e.uscCode}" />
      </div>
      <div class="field">
        <label for="app-date">申请日期</label>
        <input id="app-date" type="date" value="${app.applyDate}" />
      </div>
      <div class="field">
        <label for="app-type">申请类型</label>
        <select id="app-type">
          <option value="credit" ${app.appType === 'credit' ? 'selected' : ''}>综合授信</option>
          <option value="loan" ${app.appType === 'loan' ? 'selected' : ''}>经营贷款</option>
          <option value="renewal" ${app.appType === 'renewal' ? 'selected' : ''}>续贷申请</option>
        </select>
      </div>
      <button type="submit">提交申请</button>
    </form>
    <section class="split">
      <div class="panel">
        <h2>申请信息</h2>
        <div class="summary-grid">
          ${kv('申请编号', app.applyId, 'apply-id')}
          ${kv('申请日期', state.applicationDate || app.applyDate, 'app-date-display')}
          ${kv('申请金额', money(app.applyAmount), 'apply-amount')}
          ${kv('基础额度', money(app.baseCreditLimit), 'base-credit-limit')}
        </div>
      </div>
      <aside class="panel">
        <h2>可采集变量</h2>
        <p class="notice">RPA 可以把申请编号、申请金额、基础额度和申请日期作为额度计算上下文，例如 baseCreditLimit、applyAmount。</p>
      </aside>
    </section>
  `;
}

function renderInvoiceQuery() {
  const e = state.currentEnterprise;
  return `
    ${pageHeader('发票查询', '兼容旧流程：#invoice-tax-no、#invoice-usc-code、#invoice-sign-0、#invoice-jshj-0')}
    <div class="toolbar">
      <div class="field">
        <label for="invoice-tax-no">纳税人识别号</label>
        <input id="invoice-tax-no" value="${e.taxNo}" />
      </div>
      <div class="field">
        <label for="invoice-usc-code">统一社会信用代码</label>
        <input id="invoice-usc-code" value="${e.uscCode}" />
      </div>
      <button data-action="query-by-tax">查询</button>
    </div>
    <section class="panel">
      <h2>发票汇总</h2>
      <div class="metric-grid">
        ${metric('发票总数', e.invoices.length, 'invoice-total')}
        ${metric('正常销项价税合计', money(sumInvoice(e, row => row.sign === '销项' && row.state === '正常')), 'normal-sale-sum', 'ok')}
        ${metric('正常进项价税合计', money(sumInvoice(e, row => row.sign === '进项' && row.state === '正常')), 'normal-purchase-sum')}
        ${metric('异常发票数', e.invoices.filter(row => row.state !== '正常').length, 'bad-invoice-count', 'warn')}
      </div>
    </section>
    <section class="data-grid">
      <h2>发票明细</h2>
      <div class="table-wrap">
        <table>
          <thead>
            <tr>
              <th>发票号码</th>
              <th>方向</th>
              <th>状态</th>
              <th>开票时间</th>
              <th>购方</th>
              <th>销方</th>
              <th>不含税金额</th>
              <th>税额</th>
              <th>价税合计</th>
            </tr>
          </thead>
          <tbody>
            ${e.invoices.map(invoiceRow).join('')}
          </tbody>
        </table>
      </div>
    </section>
  `;
}

function renderTaxReport() {
  const e = state.currentEnterprise;
  const tax = e.taxReport;
  return `
    ${pageHeader('纳税申报', '提供营业收入、应纳税额、已缴税额、税负率等指标测试数据')}
    ${taxToolbar(e)}
    <section class="panel">
      <h2>${tax.period} 纳税摘要</h2>
      <div class="metric-grid">
        ${metric('营业收入', money(tax.revenue), 'tax-report-revenue')}
        ${metric('应纳税额', money(tax.taxPayable), 'tax-report-tax-payable')}
        ${metric('已缴税额', money(tax.taxPaid), 'tax-report-tax-paid', 'ok')}
        ${metric('税负率', percent(tax.taxRate), 'tax-report-tax-rate')}
        ${metric('增值税', money(tax.vat), 'tax-report-vat')}
        ${metric('企业所得税', money(tax.incomeTax), 'tax-report-income-tax')}
      </div>
    </section>
  `;
}

function renderFinancialReport() {
  const e = state.currentEnterprise;
  const f = e.financial;
  const debtRatio = f.totalLiabilities / f.totalAssets;
  const currentRatio = f.currentAssets / f.currentLiabilities;
  const profitRate = f.netProfit / f.revenue;
  return `
    ${pageHeader('财务报表', '提供资产负债率、流动比率、利润率、应收账款等指标测试数据')}
    ${taxToolbar(e)}
    <section class="panel">
      <h2>财务指标</h2>
      <div class="metric-grid">
        ${metric('营业收入', money(f.revenue), 'finance-revenue')}
        ${metric('净利润', money(f.netProfit), 'finance-net-profit', 'ok')}
        ${metric('利润率', percent(profitRate), 'finance-profit-rate')}
        ${metric('总资产', money(f.totalAssets), 'finance-total-assets')}
        ${metric('总负债', money(f.totalLiabilities), 'finance-total-liabilities')}
        ${metric('资产负债率', percent(debtRatio), 'finance-debt-ratio')}
        ${metric('流动资产', money(f.currentAssets), 'finance-current-assets')}
        ${metric('流动负债', money(f.currentLiabilities), 'finance-current-liabilities')}
        ${metric('流动比率', currentRatio.toFixed(2), 'finance-current-ratio', 'ok')}
        ${metric('应收账款', money(f.arBalance), 'finance-ar-balance')}
        ${metric('货币资金', money(f.cash), 'finance-cash')}
      </div>
    </section>
  `;
}

function renderRiskInfo() {
  const e = state.currentEnterprise;
  const r = e.risk;
  return `
    ${pageHeader('风险信息', '提供异常发票、逾期、黑名单、涉诉等风控测试数据')}
    ${taxToolbar(e)}
    <section class="panel">
      <h2>风险摘要</h2>
      <div class="metric-grid">
        ${metric('异常发票数', r.abnormalInvoiceCount, 'risk-abnormal-invoice-count', r.abnormalInvoiceCount > 2 ? 'warn' : 'ok')}
        ${metric('逾期次数', r.overdueCount, 'risk-overdue-count', r.overdueCount > 0 ? 'warn' : 'ok')}
        ${metric('黑名单状态', r.blacklist, 'risk-blacklist', r.blacklist === '否' ? 'ok' : 'danger')}
        ${metric('风险评分', r.riskScore, 'risk-score', r.riskScore >= 80 ? 'ok' : 'warn')}
        ${metric('税务处罚', r.taxViolation, 'risk-tax-violation')}
        ${metric('涉诉数量', r.lawsuitCount, 'risk-lawsuit-count')}
      </div>
    </section>
  `;
}

function taxToolbar(e) {
  return `
    <div class="toolbar">
      <div class="field">
        <label for="query-tax-no">纳税人识别号</label>
        <input id="query-tax-no" value="${e.taxNo}" />
      </div>
      <div class="field">
        <label for="query-usc-code">统一社会信用代码</label>
        <input id="query-usc-code" value="${e.uscCode}" />
      </div>
      <button data-action="query-generic-tax">查询</button>
    </div>
  `;
}

function kv(label, value, id) {
  return `
    <div class="kv">
      <span>${label}</span>
      <strong id="${id}">${value}</strong>
    </div>
  `;
}

function metric(label, value, id, tone = '') {
  return `
    <div class="metric">
      <span>${label}</span>
      <strong id="${id}" class="${tone}">${value}</strong>
    </div>
  `;
}

function invoiceRow(row, index) {
  const stateClass = row.state === '正常' ? 'ok' : row.state === '作废' ? 'danger' : 'warn';
  return `
    <tr id="invoice-row-${index}">
      <td id="invoice-no-${index}">${row.no}</td>
      <td id="invoice-sign-${index}">${row.sign}</td>
      <td><span id="invoice-state-${index}" class="tag ${stateClass}">${row.state}</span></td>
      <td id="invoice-time-${index}">${row.invoiceTime}</td>
      <td id="invoice-buyer-${index}">${row.buyer}</td>
      <td id="invoice-seller-${index}">${row.seller}</td>
      <td id="invoice-tax-exclusive-${index}">${money(row.amount)}</td>
      <td id="invoice-tax-amount-${index}">${money(row.taxAmount)}</td>
      <td id="invoice-jshj-${index}">${money(row.jshj)}</td>
    </tr>
  `;
}

function sumInvoice(enterprise, predicate) {
  return enterprise.invoices.filter(predicate).reduce((sum, row) => sum + row.jshj, 0);
}

document.addEventListener('click', event => {
  const action = event.target.closest('[data-action]')?.dataset.action;
  if (!action) return;

  if (action === 'query-enterprise') {
    state.currentEnterprise = findEnterpriseByName(document.getElementById('enterprise-name-input')?.value);
    render();
  }

  if (action === 'switch-sample') {
    const currentIndex = MOCK_DATA.findIndex(item => item.taxNo === state.currentEnterprise.taxNo);
    state.currentEnterprise = MOCK_DATA[(currentIndex + 1 + MOCK_DATA.length) % MOCK_DATA.length];
    render();
  }

  if (action === 'query-by-tax') {
    state.currentEnterprise = findEnterpriseByTax(
      document.getElementById('invoice-tax-no')?.value,
      document.getElementById('invoice-usc-code')?.value
    );
    render();
  }

  if (action === 'query-generic-tax') {
    state.currentEnterprise = findEnterpriseByTax(
      document.getElementById('query-tax-no')?.value,
      document.getElementById('query-usc-code')?.value
    );
    render();
  }
});

document.addEventListener('submit', event => {
  const form = event.target.closest('[data-action="submit-application"]');
  if (!form) return;
  event.preventDefault();
  state.currentEnterprise = findEnterpriseByTax(
    document.getElementById('app-tax-no')?.value,
    document.getElementById('app-usc-code')?.value
  );
  state.applicationDate = document.getElementById('app-date')?.value || state.currentEnterprise.application.applyDate;
  render();
});

window.addEventListener('hashchange', render);

if (!location.hash) {
  location.hash = '#/enterprise-info';
} else {
  render();
}
