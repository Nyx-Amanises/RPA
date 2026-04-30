const profile = {
  id: 3,
  username: 'admin',
  realName: '雪',
  email: '3111471949@qq.com',
  mobile: '13617618489',
  roleIds: [14],
  roleNames: ['管理员'],
  avatarUrl: '',
  status: 1,
  createTime: '2026-03-23 09:14:38',
  updateTime: '2026-03-23 11:43:08'
}

const users = [
  {
    id: 3,
    username: 'admin',
    realName: '雪',
    email: '3111471949@qq.com',
    mobile: '13617618489',
    roleIds: [14],
    roleNames: ['管理员'],
    status: 1,
    createTime: '2026-03-23 09:14:38'
  },
  {
    id: 4,
    username: 'operator01',
    realName: '操作员甲',
    email: 'operator01@rpa.com',
    mobile: '13800138001',
    roleIds: [15],
    roleNames: ['操作员'],
    status: 1,
    createTime: '2026-03-23 09:14:38'
  },
  {
    id: 5,
    username: 'tester02',
    realName: '测试用户2',
    email: 'tester02@rpa.com',
    mobile: '13800138002',
    roleIds: [14],
    roleNames: ['管理员'],
    status: 0,
    createTime: '2026-03-23 11:34:49'
  }
]

const roles = [
  {
    id: 14,
    roleCode: 'ADMIN',
    roleName: '管理员',
    description: '系统管理员',
    userCount: 2,
    status: 1,
    createTime: '2026-03-23 09:14:38'
  },
  {
    id: 15,
    roleCode: 'OPERATOR',
    roleName: '操作员',
    description: '普通操作员',
    userCount: 1,
    status: 1,
    createTime: '2026-03-23 09:14:38'
  },
  {
    id: 16,
    roleCode: 'OPS_TEST',
    roleName: '运维测试角色',
    description: '给前端联调使用',
    userCount: 0,
    status: 1,
    createTime: '2026-03-23 11:32:59'
  }
]

const robots = [
  {
    id: 1,
    robotCode: 'ROBOT_0001',
    robotName: '机器人A001',
    robotType: '测试机器人',
    description: '用于联调测试的机器人',
    status: 1,
    currentTaskId: null,
    currentTaskCode: '',
    lastHeartbeatTime: '2026-03-25 11:15:00',
    createTime: '2026-03-25T10:39:35.205225',
    updateTime: '2026-03-25T11:12:26.702264'
  },
  {
    id: 2,
    robotCode: 'ROBOT_002',
    robotName: '机器人A002',
    robotType: '测试机器人',
    description: '用于联调测试的机器人',
    status: 1,
    currentTaskId: null,
    currentTaskCode: '',
    lastHeartbeatTime: '2026-03-25 10:58:27',
    createTime: '2026-03-25T10:58:27.212558',
    updateTime: '2026-03-25T10:58:27.212558'
  },
  {
    id: 3,
    robotCode: 'ROBOT_003',
    robotName: '机器人A003',
    robotType: '线程模拟',
    description: '课程设计测试机器人',
    status: 0,
    currentTaskId: null,
    currentTaskCode: '',
    lastHeartbeatTime: '',
    createTime: '2026-03-25T11:03:16.483068',
    updateTime: '2026-03-25T11:03:16.483068'
  }
]

const processes = [
  {
    id: 1,
    processCode: 'PROC001',
    processName: '税务采集流程',
    description: '自动登录税务系统并采集数据',
    stepCount: 3,
    status: 1,
    publishedVersionId: 1001,
    publishedVersionNo: 1,
    publishStatus: 1,
    createTime: '2026-03-17 08:00:00',
    updateTime: '2026-03-25 14:52:53'
  },
  {
    id: 2,
    processCode: 'PROC002',
    processName: '发票解析流程',
    description: '用于解析和整理发票字段',
    stepCount: 2,
    status: 0,
    publishedVersionId: null,
    publishedVersionNo: 0,
    publishStatus: 0,
    createTime: '2026-03-20 10:30:00',
    updateTime: '2026-03-25 13:18:20'
  }
]

const processSteps = {
  1: [
    {
      id: 101,
      stepNo: 1,
      stepName: '登录税务系统',
      stepType: 'ACTION',
      scriptLang: 'Python',
      scriptContent: "print('login tax system')",
      timeoutSeconds: 60,
      failureStrategy: 'STOP',
      createTime: '2026-03-25 14:40:00.000000',
      updateTime: '2026-03-25 14:40:00.000000'
    },
    {
      id: 102,
      stepNo: 2,
      stepName: '采集发票数据',
      stepType: 'ACTION',
      scriptLang: 'Python',
      scriptContent: "print('collect invoice data')",
      timeoutSeconds: 90,
      failureStrategy: 'RETRY',
      createTime: '2026-03-25 14:41:00.000000',
      updateTime: '2026-03-25 14:41:00.000000'
    },
    {
      id: 103,
      stepNo: 3,
      stepName: '保存采集结果',
      stepType: 'ACTION',
      scriptLang: 'Python',
      scriptContent: "print('save result')",
      timeoutSeconds: 60,
      failureStrategy: 'STOP',
      createTime: '2026-03-25 14:42:00.000000',
      updateTime: '2026-03-25 14:42:00.000000'
    }
  ],
  2: [
    {
      id: 201,
      stepNo: 1,
      stepName: '读取发票文件',
      stepType: 'ACTION',
      scriptLang: 'Python',
      scriptContent: "print('read invoice file')",
      timeoutSeconds: 60,
      failureStrategy: 'STOP',
      createTime: '2026-03-25 13:00:00.000000',
      updateTime: '2026-03-25 13:00:00.000000'
    },
    {
      id: 202,
      stepNo: 2,
      stepName: '解析发票字段',
      stepType: 'ACTION',
      scriptLang: 'Python',
      scriptContent: "print('parse invoice fields')",
      timeoutSeconds: 120,
      failureStrategy: 'CONTINUE',
      createTime: '2026-03-25 13:05:00.000000',
      updateTime: '2026-03-25 13:05:00.000000'
    }
  ]
}

const processVersions = {
  1: [
    {
      id: 1001,
      versionNo: 1,
      versionStatus: 1,
      stepCount: 3,
      publishTime: '2026-03-25 14:52:53',
      createTime: '2026-03-25 14:52:53',
      updateTime: '2026-03-25 14:52:53'
    }
  ],
  2: []
}

const tasks = [
  {
    id: 1,
    taskCode: 'TASK20260317001',
    taskName: '税务信息采集任务',
    taxpayerIdNo: '91500000MA5U123456',
    enterpriseName: '重庆某某科技有限公司',
    processId: 1,
    processCode: 'PROC001',
    processName: '税务采集流程',
    robotId: 1,
    robotCode: 'ROBOT_0001',
    robotName: '机器人A001',
    status: 3,
    remark: '每日执行一次',
    createTime: '2026-03-25T10:28:50',
    updateTime: '2026-03-25T10:28:58'
  },
  {
    id: 2,
    taskCode: 'TASK20260317002',
    taskName: '税票下载任务',
    taxpayerIdNo: '91500000MA5U654321',
    enterpriseName: '重庆某某贸易有限公司',
    processId: 2,
    processCode: 'PROC002',
    processName: '发票解析流程',
    robotId: 2,
    robotCode: 'ROBOT_002',
    robotName: '机器人A002',
    status: 0,
    remark: '课程设计演示数据',
    createTime: '2026-03-25T09:38:24',
    updateTime: '2026-03-25T09:38:24'
  },
  {
    id: 3,
    taskCode: 'TASK20260317003',
    taskName: '企业票据归档任务',
    taxpayerIdNo: '91500000MA5U888888',
    enterpriseName: '重庆某某制造有限公司',
    processId: 1,
    processCode: 'PROC001',
    processName: '税务采集流程',
    robotId: 1,
    robotCode: 'ROBOT_0001',
    robotName: '机器人A001',
    status: 2,
    remark: '工作日夜间执行',
    createTime: '2026-03-24T14:48:53',
    updateTime: '2026-03-24T14:49:02'
  }
]

const executions = [
  {
    id: 101,
    executionCode: 'EXEC20260317001',
    taskId: 1,
    taskCode: 'TASK20260317001',
    taskName: '税务信息采集任务',
    processCode: 'PROC001',
    processVersionNo: 1,
    robotCode: 'ROBOT_0001',
    executeStatus: 3,
    startTime: '2026-03-25T10:29:07',
    endTime: '2026-03-25T10:29:08',
    durationSeconds: 1,
    errorMessage: 'Java 步骤未配置代码(code)',
    logContent: '采集 (java)\n错误: Java 步骤未配置代码(code)'
  },
  {
    id: 102,
    executionCode: 'EXEC20260317002',
    taskId: 1,
    taskCode: 'TASK20260317001',
    taskName: '税务信息采集任务',
    processCode: 'PROC001',
    processVersionNo: 1,
    robotCode: 'ROBOT_0001',
    executeStatus: 3,
    startTime: '2026-03-25T10:28:55',
    endTime: '2026-03-25T10:28:58',
    durationSeconds: 3,
    errorMessage: '',
    logContent: '任务启动\n流程执行失败'
  },
  {
    id: 103,
    executionCode: 'EXEC20260317003',
    taskId: 3,
    taskCode: 'TASK20260317003',
    taskName: '企业票据归档任务',
    processCode: 'PROC001',
    processVersionNo: 1,
    robotCode: 'ROBOT_0001',
    executeStatus: 2,
    startTime: '2026-03-24T14:49:00',
    endTime: '2026-03-24T14:49:04',
    durationSeconds: 4,
    errorMessage: '',
    logContent: '任务启动\n流程执行完成'
  }
]

const collectionRecords = [
  {
    id: 1,
    taskId: 1,
    taskCode: 'TASK20260317001',
    executionId: 101,
    taxpayerIdNo: '91500000MA5U123456',
    enterpriseName: '重庆某某科技有限公司',
    sourceName: 'study-spider-demo',
    status: 2,
    errorMessage: '',
    collectionTime: '2026-03-24T11:14:30',
    createTime: '2026-03-24T11:14:30',
    updateTime: '2026-03-24T11:14:30',
    rawData: {
      source: 'study-spider-demo',
      site: 'http://study.zmyfrank.com:18010/spider/home#',
      enterpriseName: '重庆某某科技有限公司',
      taxNo: '91500000MA5U123456',
      uscCode: '91500000MA5U123456',
      appDate: '2026-03-24',
      invoiceStartDate: '2025-03-01',
      invoiceEndDate: '2026-03-24',
      totalSaleAmountText: '1,271,593.70',
      totalPurchaseAmountText: '456,789.01',
      invoices: [
        { invoiceCode: '1500012340', invoiceNumber: '12345678', sign: '销项', state: '正常', invoiceTime: '2024-01-15 10:30:00', jshjText: '123,456.79' },
        { invoiceCode: '1500012341', invoiceNumber: '12345679', sign: '销项', state: '正常', invoiceTime: '2024-02-20 14:20:00', jshjText: '234,567.89' }
      ]
    }
  },
  {
    id: 2,
    taskId: 3,
    taskCode: 'TASK20260317003',
    executionId: 103,
    taxpayerIdNo: '91500000MA5U888888',
    enterpriseName: '重庆某某制造有限公司',
    sourceName: 'study-spider-demo',
    status: 2,
    errorMessage: '',
    collectionTime: '2026-03-24T09:57:16',
    createTime: '2026-03-24T09:57:16',
    updateTime: '2026-03-24T09:57:16',
    rawData: {
      companyName: '重庆某某制造有限公司',
      taxLevel: 'A',
      invoiceCount: 15,
      declareStatus: '正常'
    }
  },
  {
    id: 3,
    taskId: 2,
    taskCode: 'TASK20260317002',
    executionId: 0,
    taxpayerIdNo: '91500000MA5U654321',
    enterpriseName: '重庆某某贸易有限公司',
    sourceName: '人工录入',
    status: 1,
    errorMessage: '',
    collectionTime: '2026-03-26T11:02:53.295319',
    createTime: '2026-03-26T11:02:53.295319',
    updateTime: '2026-03-26T11:02:53.295319',
    rawData: {
      taxNo: '91500000MA5U654321',
      appDate: '2026-03-26',
      enterpriseName: '重庆某某贸易有限公司',
      invoices: []
    }
  },
  {
    id: 4,
    taskId: 1,
    taskCode: 'TASK20260317001',
    executionId: 102,
    taxpayerIdNo: '91500000MA5U123456',
    enterpriseName: '重庆某某科技有限公司',
    sourceName: '电子税务局',
    status: 3,
    errorMessage: '采集流程执行失败',
    collectionTime: '2026-03-21T10:06:20',
    createTime: '2026-03-21T10:06:20',
    updateTime: '2026-03-21T10:06:20',
    rawData: {
      companyName: '重庆某某科技有限公司',
      taxLevel: 'B',
      invoiceCount: 0,
      declareStatus: '异常'
    }
  }
]

const analysisRecords = [
  {
    id: 1,
    taskId: 7,
    collectionId: 1,
    taxpayerIdNo: '004',
    enterpriseName: '004',
    status: 1,
    extractedFieldCount: 5,
    ruleName: '发票解析规则',
    errorMessage: '',
    analysisTime: '2026-03-26T11:03:00',
    createTime: '2026-03-26T11:03:00',
    updateTime: '2026-03-26T11:03:00',
    parsedData: {
      taxNo: '91500000MA5U123456',
      appDate: '2024-02-06',
      uscCode: '91500000MA5U123456',
      invoices: [
        {
          sign: '销项',
          state: '正常',
          jshjText: '123,456.79',
          invoiceTime: '2024-01-15 10:30:00',
          invoiceTimeParsed: '2024-01-15T10:30',
          monthDiffToAppDate: 1
        },
        {
          sign: '销项',
          state: '正常',
          jshjText: '234,567.89',
          invoiceTime: '2024-02-20 14:20:00',
          invoiceTimeParsed: '2024-02-20T14:20',
          monthDiffToAppDate: 0
        }
      ],
      enterpriseName: '004'
    }
  },
  {
    id: 21,
    taskId: 1,
    collectionId: 11,
    taxpayerIdNo: '91500000MA5U123456',
    enterpriseName: '重庆某某科技有限公司',
    status: 2,
    extractedFieldCount: 8,
    ruleName: '税务信息解析规则',
    errorMessage: '',
    analysisTime: '2026-03-17 09:35:20',
    createTime: '2026-03-17 09:35:20',
    updateTime: '2026-03-17 09:35:20',
    parsedData: {
      enterpriseName: '重庆某某科技有限公司',
      taxNo: '91500000MA5U123456',
      uscCode: '91500000MA5U123456',
      appDate: '2026-03-26',
      invoiceStartDate: '2025-03-01',
      invoiceEndDate: '2026-03-26',
      invoiceCount: 5,
      invoices: [
        { invoiceCode: '1500012340', invoiceNumber: '12345678', sign: '销项', state: '正常', invoiceTime: '2024-01-15 10:30:00', jshjText: '123,456.79' },
        { invoiceCode: '1500012341', invoiceNumber: '12345679', sign: '销项', state: '正常', invoiceTime: '2024-02-20 14:20:00', jshjText: '234,567.89' }
      ],
      summary: {
        totalSaleAmountText: '1,271,593.70',
        totalPurchaseAmountText: '456,789.01'
      }
    }
  },
  {
    id: 22,
    taskId: 2,
    collectionId: 3,
    taxpayerIdNo: '91500000MA5U654321',
    enterpriseName: '重庆某某贸易有限公司',
    status: 2,
    extractedFieldCount: 4,
    ruleName: '税票解析规则',
    errorMessage: '',
    analysisTime: '2026-03-25T10:01:07',
    createTime: '2026-03-25T10:01:07',
    updateTime: '2026-03-25T10:01:07',
    parsedData: {
      taxpayerIdNo: '91500000MA5U654321',
      enterpriseName: '重庆某某贸易有限公司',
      taxLevel: 'A',
      invoiceCount: 15,
      declareStatus: '正常'
    }
  },
  {
    id: 23,
    taskId: 3,
    collectionId: 4,
    taxpayerIdNo: '91500000MA5U888888',
    enterpriseName: '重庆某某制造有限公司',
    status: 3,
    extractedFieldCount: 0,
    ruleName: '税务信息解析规则',
    errorMessage: '解析失败：字段映射缺失',
    analysisTime: '2026-03-20T10:10:59',
    createTime: '2026-03-20T10:10:59',
    updateTime: '2026-03-20T10:10:59',
    parsedData: {}
  }
]

const processingRecords = [
  {
    id: 31,
    taskId: 1,
    executionId: 101,
    analysisId: 21,
    taxpayerIdNo: '91500000MA5U123456',
    enterpriseName: '重庆某某科技有限公司',
    status: 2,
    validationResult: '校验通过',
    validationDetail: {
      passed: true,
      rules: [
        { field: 'taxpayerIdNo', result: '通过', message: '纳税人识别号格式正确' },
        { field: 'invoiceCount', result: '通过', message: '发票数量字段完整' },
        { field: 'amountSummary', result: '通过', message: '金额汇总计算正常' }
      ]
    },
    errorMessage: '',
    processTime: '2026-03-17 09:35:28',
    processingTime: '2026-03-17 09:35:28',
    createTime: '2026-03-17 09:35:28',
    updateTime: '2026-03-17 09:35:28',
    processedData: {
      taxpayerIdNo: '91500000MA5U123456',
      enterpriseName: '重庆某某科技有限公司',
      taxLevel: 'A',
      invoiceCount: 5,
      totalSaleAmount: 1271593.7,
      totalPurchaseAmount: 456789.01,
      abnormalInvoiceCount: 0,
      declareStatus: '正常'
    }
  },
  {
    id: 32,
    taskId: 2,
    executionId: 0,
    analysisId: 22,
    taxpayerIdNo: '91500000MA5U654321',
    enterpriseName: '重庆某某贸易有限公司',
    status: 1,
    validationResult: '校验中',
    validationDetail: {
      passed: false,
      rules: [{ field: 'declareStatus', result: '处理中', message: '等待加工规则执行完成' }]
    },
    errorMessage: '',
    processTime: '2026-03-25T10:05:30',
    processingTime: '2026-03-25T10:05:30',
    createTime: '2026-03-25T10:05:30',
    updateTime: '2026-03-25T10:05:30',
    processedData: {
      taxpayerIdNo: '91500000MA5U654321',
      enterpriseName: '重庆某某贸易有限公司',
      taxLevel: 'A',
      invoiceCount: 15
    }
  },
  {
    id: 33,
    taskId: 3,
    executionId: 103,
    analysisId: 23,
    taxpayerIdNo: '91500000MA5U888888',
    enterpriseName: '重庆某某制造有限公司',
    status: 3,
    validationResult: '校验失败',
    validationDetail: {
      passed: false,
      rules: [
        { field: 'enterpriseName', result: '失败', message: '企业名称缺失映射值' },
        { field: 'invoices', result: '失败', message: '发票列表为空，无法生成加工结果' }
      ]
    },
    errorMessage: '加工失败：字段映射不完整',
    processTime: '2026-03-20T10:12:12',
    processingTime: '2026-03-20T10:12:12',
    createTime: '2026-03-20T10:12:12',
    updateTime: '2026-03-20T10:12:12',
    processedData: {}
  }
]

const businessDataRecords = [
  {
    id: 41,
    taskId: 1,
    taskCode: 'TASK_20260326104426007',
    taxpayerIdNo: '91500000MA5U123456',
    enterpriseName: '重庆某某科技有限公司',
    taxAreaId: '500112',
    dataStatus: 1,
    createTime: '2026-03-17 09:35:40',
    updateTime: '2026-03-17 09:35:40',
    businessData: {
      result: {
        appDate: '2024-02-06',
        saleJshjSum: 123456.79,
        invoiceTotal: 5,
        indicatorCode: 'inv_f1_12m_down_sale_jshj_sum_teach',
        invoiceMatched: 1,
        matchedInvoices: [{ jshj: 123456.79, sign: '销项', state: '正常', invoiceTime: '2024-01-15 10:30:00' }]
      },
      taskId: 1,
      taskCode: 'TASK_20260326104426007',
      robotCode: 'ROBOT_TEST',
      executionId: 21,
      generatedAt: '2026-03-26T11:03:00.214858400',
      processCode: 'PROCESS_004',
      taxpayerIdNo: '91500000MA5U123456',
      executionCode: '20260326110247001',
      enterpriseName: '重庆某某科技有限公司'
    }
  },
  {
    id: 42,
    taskId: 2,
    taskCode: '2033834277000523778',
    taxpayerIdNo: '91500000MA5U654321',
    enterpriseName: '重庆某某贸易有限公司',
    taxAreaId: '-',
    dataStatus: 1,
    createTime: '2026-03-25 10:01:07',
    updateTime: '2026-03-25 10:01:07',
    businessData: {
      result: {
        taxpayerIdNo: '91500000MA5U654321',
        enterpriseName: '重庆某某贸易有限公司',
        taxAreaId: '-',
        taxLevel: 'A',
        invoiceCount: 15,
        declareStatus: '正常'
      },
      taskId: 2,
      taskCode: '2033834277000523778',
      robotCode: 'ROBOT_002',
      executionId: 0,
      generatedAt: '2026-03-25T10:01:07',
      processCode: 'PROC002',
      taxpayerIdNo: '91500000MA5U654321',
      executionCode: '',
      enterpriseName: '重庆某某贸易有限公司'
    }
  },
  {
    id: 43,
    taskId: 3,
    taskCode: '2036280447522123777',
    taxpayerIdNo: '91500000MA5U888888',
    enterpriseName: '重庆某某制造有限公司',
    taxAreaId: '500110',
    dataStatus: 0,
    createTime: '2026-03-24 11:14:30',
    updateTime: '2026-03-24 11:14:30',
    businessData: {
      result: {
        taxpayerIdNo: '91500000MA5U888888',
        enterpriseName: '重庆某某制造有限公司',
        taxAreaId: '500110',
        taxLevel: 'B',
        invoiceCount: 3,
        declareStatus: '待确认'
      },
      taskId: 3,
      taskCode: '2036280447522123777',
      robotCode: 'ROBOT_0001',
      executionId: 103,
      generatedAt: '2026-03-24T11:14:30',
      processCode: 'PROC001',
      taxpayerIdNo: '91500000MA5U888888',
      executionCode: 'EXEC20260317003',
      enterpriseName: '重庆某某制造有限公司'
    }
  }
]

const resources = [
  {
    id: 28,
    parentId: null,
    resourceName: '系统管理',
    resourceCode: 'SYSTEM',
    resourceType: 1,
    path: '/system',
    icon: 'Setting',
    sortNo: 1,
    status: 1,
    children: [
      {
        id: 101,
        parentId: 28,
        resourceName: '个人信息',
        resourceCode: 'PROFILE',
        resourceType: 2,
        path: '/system/profile',
        icon: 'User',
        sortNo: 1,
        status: 1
      },
      {
        id: 30,
        parentId: 28,
        resourceName: '用户管理',
        resourceCode: 'USER_MANAGE',
        resourceType: 2,
        path: '/system/users',
        icon: 'UserFilled',
        sortNo: 2,
        status: 1
      },
      {
        id: 31,
        parentId: 28,
        resourceName: '角色管理',
        resourceCode: 'ROLE_MANAGE',
        resourceType: 2,
        path: '/system/roles',
        icon: 'Avatar',
        sortNo: 3,
        status: 1
      },
      {
        id: 37,
        parentId: 28,
        resourceName: '资源管理',
        resourceCode: 'RESOURCE_MANAGE',
        resourceType: 2,
        path: '/system/resources',
        icon: 'Files',
        sortNo: 4,
        status: 1,
        children: [
          {
            id: 38,
            parentId: 37,
            resourceName: '文件管理',
            resourceCode: 'FILE_MANAGE',
            resourceType: 2,
            path: '/system/resources/files',
            icon: 'FolderOpened',
            sortNo: 1,
            status: 1
          }
        ]
      }
    ]
  }
]

const roleResourceMap = {
  14: [28, 101, 30, 31, 37, 38],
  15: [28, 101, 30],
  16: [28, 31, 37]
}

function wait(data) {
  return new Promise((resolve) => {
    setTimeout(() => resolve({ code: 100200, message: '成功', data }), 200)
  })
}

export function getMockProfile() {
  return wait({ ...profile })
}

export function updateMockProfile(payload) {
  Object.assign(profile, payload, { updateTime: '2026-03-23 12:00:00' })
  return wait({ id: profile.id, updateTime: profile.updateTime })
}

export function uploadMockAvatar(file) {
  const ext = file?.name?.includes('.') ? file.name.slice(file.name.lastIndexOf('.')) : '.png'
  const avatarUrl = `https://dummyimage.com/160x160/21419a/ffffff&text=${encodeURIComponent(profile.realName?.slice(0, 1) || 'U')}${ext}`
  profile.avatarUrl = avatarUrl
  profile.updateTime = '2026-03-24 10:00:00'
  return wait({ avatarUrl })
}

export function updateMockPassword() {
  return wait(null)
}

export function getMockUsers(params = {}) {
  let list = [...users]
  if (params.username) {
    list = list.filter((item) => item.username.includes(params.username))
  }
  if (params.realName) {
    list = list.filter((item) => item.realName.includes(params.realName))
  }
  if (params.roleId) {
    list = list.filter((item) => item.roleIds.includes(Number(params.roleId)))
  }
  return wait({
    total: list.length,
    pageNum: Number(params.pageNum || 1),
    pageSize: Number(params.pageSize || 10),
    list
  })
}

export function saveMockUser(payload) {
  if (payload.id) {
    const target = users.find((item) => item.id === payload.id)
    Object.assign(target, payload)
    target.roleNames = roles.filter((item) => target.roleIds.includes(item.id)).map((item) => item.roleName)
    return wait({ id: target.id, username: target.username, updateTime: '2026-03-23 14:20:00' })
  }
  const nextId = Math.max(...users.map((item) => item.id)) + 1
  const roleNames = roles.filter((item) => payload.roleIds.includes(item.id)).map((item) => item.roleName)
  const user = { ...payload, id: nextId, roleNames, createTime: '2026-03-23 15:00:00' }
  users.unshift(user)
  return wait({ id: user.id, username: user.username })
}

export function resetMockUserPassword(id) {
  return wait({ id })
}

export function toggleMockUserStatus(id, status) {
  const target = users.find((item) => item.id === id)
  if (target) {
    target.status = status
  }
  return wait({ id, status, updateTime: '2026-03-23 14:30:00' })
}

export function deleteMockUser(id) {
  const index = users.findIndex((item) => item.id === id)
  if (index >= 0) {
    users.splice(index, 1)
  }
  return wait({ id })
}

export function getMockRoles(params = {}) {
  let list = [...roles]
  if (params.roleName) {
    list = list.filter((item) => item.roleName.includes(params.roleName))
  }
  if (params.roleCode) {
    list = list.filter((item) => item.roleCode.includes(params.roleCode))
  }
  return wait({
    total: list.length,
    pageNum: Number(params.pageNum || 1),
    pageSize: Number(params.pageSize || 10),
    list
  })
}

export function getMockRobotOverview() {
  const totalCount = robots.length
  const onlineCount = robots.filter((item) => item.status === 1).length
  const workingCount = robots.filter((item) => item.status === 2).length
  const offlineCount = robots.filter((item) => item.status === 0).length
  return wait({ totalCount, onlineCount, workingCount, offlineCount })
}

export function getMockDashboardSummary() {
  const today = '2026-03-27'
  const taskSummary = {
    total: tasks.length,
    todayNew: tasks.filter((item) => String(item.createTime || '').startsWith(today)).length,
    pending: tasks.filter((item) => item.status === 0).length,
    queued: tasks.filter((item) => item.status === 4).length,
    running: tasks.filter((item) => item.status === 1).length,
    success: tasks.filter((item) => item.status === 2).length,
    failed: tasks.filter((item) => item.status === 3).length
  }
  const robotSummary = {
    total: robots.length,
    online: robots.filter((item) => item.status === 1).length,
    working: robots.filter((item) => item.status === 2).length,
    offline: robots.filter((item) => item.status === 0).length
  }
  const processSummary = {
    total: processes.length,
    enabled: processes.filter((item) => item.status === 1).length,
    disabled: processes.filter((item) => item.status === 0).length
  }
  const dataSummary = {
    total: businessDataRecords.length,
    available: businessDataRecords.filter((item) => item.dataStatus === 1).length,
    todayCollected: collectionRecords.filter((item) => String(item.createTime || '').startsWith(today)).length
  }
  const taskStatusOverview = {
    pending: taskSummary.pending,
    queued: taskSummary.queued || tasks.filter((item) => item.status === 4).length,
    running: taskSummary.running,
    success: taskSummary.success,
    failed: taskSummary.failed
  }
  const successExecutions = executions.filter((item) => item.executeStatus === 2).length
  const failedExecutions = executions.filter((item) => item.executeStatus === 3).length
  const finishedExecutions = successExecutions + failedExecutions
  const durations = executions.map((item) => Number(item.durationSeconds)).filter((item) => Number.isFinite(item))
  const averageDurationSeconds = durations.length
    ? Math.round((durations.reduce((sum, item) => sum + item, 0) / durations.length) * 10) / 10
    : 0
  const failureTopReasons = Object.entries(
    executions
      .filter((item) => item.executeStatus === 3)
      .reduce((acc, item) => {
        const reason = String(item.errorMessage || '未知错误').replace(/\s+/g, ' ').slice(0, 120)
        acc[reason] = (acc[reason] || 0) + 1
        return acc
      }, {})
  )
    .sort((a, b) => b[1] - a[1])
    .slice(0, 5)
    .map(([reason, count]) => ({ reason, count }))
  const executionMetrics = {
    totalExecutions: executions.length,
    successExecutions,
    failedExecutions,
    runningExecutions: executions.filter((item) => item.executeStatus === 1).length,
    queuedExecutions: executions.filter((item) => item.executeStatus === 4).length,
    successRate: finishedExecutions ? Math.round((successExecutions * 1000) / finishedExecutions) / 10 : 0,
    averageDurationSeconds
  }

  return wait({
    taskTotal: taskSummary.total,
    robotTotal: robotSummary.total,
    processTotal: processSummary.total,
    dataTotal: dataSummary.total,
    taskStatusSummary: { ...taskStatusOverview },
    taskSummary,
    robotSummary,
    processSummary,
    dataSummary,
    taskStatusOverview,
    executionMetrics,
    failureTopReasons,
    systemInfo: {
      systemName: 'RPA管理系统',
      version: '1.0.0',
      systemVersion: '1.0.0',
      runtimeDays: 32,
      runningDays: 32,
      database: 'MySQL 8.0',
      dataSource: 'MySQL 8.0',
      lastUpdateTime: '2026-03-27T10:30:00'
    }
  })
}

export function getMockDashboardRecentTasks(params = {}) {
  const limit = Number(params.limit || 5)
  const list = [...tasks]
    .sort((a, b) => new Date(b.createTime) - new Date(a.createTime))
    .slice(0, limit)
    .map((item) => {
      const execution = executions
        .filter((executionItem) => executionItem.taskId === item.id)
        .sort((a, b) => new Date(b.startTime) - new Date(a.startTime))[0]
      const statusLabelMap = {
        0: '待执行',
        1: '运行中',
        2: '已完成',
        3: '失败',
        4: '排队中'
      }

      return {
        ...item,
        statusLabel: statusLabelMap[item.status] || '--',
        startTime: execution?.startTime || null,
        endTime: execution?.endTime || null
      }
    })

  return wait(list)
}

export function getMockProcesses(params = {}) {
  let list = [...processes]
  if (params.processName) {
    list = list.filter((item) => item.processName.includes(params.processName))
  }
  if (params.processCode) {
    list = list.filter((item) => item.processCode.includes(params.processCode))
  }
  if (params.status !== '' && typeof params.status !== 'undefined' && params.status !== null) {
    list = list.filter((item) => item.status === Number(params.status))
  }
  return wait({
    total: list.length,
    pageNum: Number(params.pageNum || 1),
    pageSize: Number(params.pageSize || 10),
    list
  })
}

function enrichTaskProcessVersion(task = {}) {
  const process = processes.find((item) => item.id === Number(task.processId))
  return {
    ...task,
    processPublishedVersionNo: process?.publishedVersionNo || task.processPublishedVersionNo || 0,
    processPublishStatus: process?.publishStatus ?? task.processPublishStatus ?? 0
  }
}

export function getMockTasks(params = {}) {
  let list = [...tasks]
  if (params.keyword) {
    list = list.filter((item) => item.taskCode.includes(params.keyword) || item.taskName.includes(params.keyword))
  }
  if (params.status !== '' && typeof params.status !== 'undefined' && params.status !== null) {
    list = list.filter((item) => item.status === Number(params.status))
  }
  if (params.startTime) {
    list = list.filter((item) => new Date(item.createTime) >= new Date(params.startTime))
  }
  if (params.endTime) {
    list = list.filter((item) => new Date(item.createTime) <= new Date(params.endTime))
  }
  list.sort((a, b) => new Date(b.createTime) - new Date(a.createTime))
  list = list.map(enrichTaskProcessVersion)
  return wait({
    total: list.length,
    pageNum: Number(params.pageNum || 1),
    pageSize: Number(params.pageSize || 10),
    list
  })
}

export function getMockTaskDetail(id) {
  const task = tasks.find((item) => item.id === Number(id))
  const lastExecution = executions
    .filter((item) => item.taskId === Number(id))
    .sort((a, b) => new Date(b.startTime) - new Date(a.startTime))[0]
  return wait({
    ...enrichTaskProcessVersion(task),
    startTime: lastExecution?.startTime || '',
    endTime: lastExecution?.endTime || '',
    durationSeconds: lastExecution?.durationSeconds || 0,
    errorMessage: lastExecution?.errorMessage || '',
    logContent: lastExecution?.logContent || ''
  })
}

export function saveMockTask(payload) {
  if (payload.id) {
    const target = tasks.find((item) => item.id === payload.id)
    const process = processes.find((item) => item.id === Number(payload.processId))
    const robot = robots.find((item) => item.id === Number(payload.robotId))
    Object.assign(target, payload, {
      processId: Number(payload.processId),
      processCode: process?.processCode || '',
      processName: process?.processName || '',
      processPublishedVersionNo: process?.publishedVersionNo || 0,
      processPublishStatus: process?.publishStatus ?? 0,
      robotId: Number(payload.robotId),
      robotCode: robot?.robotCode || '',
      robotName: robot?.robotName || '',
      updateTime: '2026-03-25T16:19:13.160000'
    })
    return wait({ ...target })
  }

  const nextId = Math.max(...tasks.map((item) => item.id), 0) + 1
  const process = processes.find((item) => item.id === Number(payload.processId))
  const robot = robots.find((item) => item.id === Number(payload.robotId))
  const task = {
    id: nextId,
    taskCode: `TASK_20260325161509${String(nextId).padStart(3, '0')}`,
    taskName: payload.taskName,
    taxpayerIdNo: payload.taxpayerIdNo,
    enterpriseName: payload.enterpriseName,
    processId: Number(payload.processId),
    processCode: process?.processCode || '',
    processName: process?.processName || '',
    processPublishedVersionNo: process?.publishedVersionNo || 0,
    processPublishStatus: process?.publishStatus ?? 0,
    robotId: Number(payload.robotId),
    robotCode: robot?.robotCode || '',
    robotName: robot?.robotName || '',
    status: 0,
    remark: payload.remark || '',
    createTime: '2026-03-25T16:15:09.3643284',
    updateTime: '2026-03-25T16:15:09.3643284'
  }
  tasks.unshift(task)
  return wait({ ...task })
}

export function executeMockTask(id) {
  const task = tasks.find((item) => item.id === Number(id))
  const process = processes.find((item) => item.id === Number(task?.processId))
  const nextExecutionId = Math.max(...executions.map((item) => item.id), 100) + 1
  const executionCode = `EXEC${Date.now()}`
  const executeStatus = 4
  const execution = {
    id: nextExecutionId,
    executionCode,
    taskId: task.id,
    taskCode: task.taskCode,
    taskName: task.taskName,
    processCode: task.processCode,
    processVersionNo: process?.publishedVersionNo || 0,
    robotCode: task.robotCode,
    executeStatus,
    startTime: '2026-03-25T16:23:50',
    endTime: '',
    durationSeconds: null,
    errorMessage: '',
    logContent: '任务已进入异步执行队列'
  }
  executions.unshift(execution)
  task.status = executeStatus
  task.updateTime = '2026-03-25T16:23:50'
  return wait({
    status: executeStatus,
    taskId: task.id,
    taskCode: task.taskCode,
    executionCode,
    errorMessage: execution.errorMessage,
    executionId: nextExecutionId
  })
}

export function deleteMockTask(id) {
  const index = tasks.findIndex((item) => item.id === id)
  if (index >= 0) {
    tasks.splice(index, 1)
  }
  return wait({ id })
}

export function getMockExecutions(params = {}) {
  let list = [...executions]
  if (params.taskId) {
    list = list.filter((item) => item.taskId === Number(params.taskId))
  }
  if (params.executeStatus !== '' && typeof params.executeStatus !== 'undefined' && params.executeStatus !== null) {
    list = list.filter((item) => item.executeStatus === Number(params.executeStatus))
  }
  if (params.startTime) {
    list = list.filter((item) => new Date(item.startTime) >= new Date(params.startTime))
  }
  if (params.endTime) {
    list = list.filter((item) => new Date(item.startTime) <= new Date(params.endTime))
  }
  list.sort((a, b) => new Date(b.startTime) - new Date(a.startTime))
  return wait({
    total: list.length,
    pageNum: Number(params.pageNum || 1),
    pageSize: Number(params.pageSize || 10),
    list
  })
}

function formatMockDateTime(value) {
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) {
    return value || ''
  }

  const pad = (num) => String(num).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`
}

function addSecondsToMockTime(value, seconds) {
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) {
    return value || ''
  }

  date.setSeconds(date.getSeconds() + Number(seconds || 0))
  return formatMockDateTime(date)
}

function buildMockExecutionStepLogs(execution) {
  if (!execution) {
    return []
  }

  const task = tasks.find((item) => item.id === Number(execution.taskId))
  const steps = structuredClone(processSteps[task?.processId] || [])

  if (!steps.length) {
    return []
  }

  const totalDuration = Math.max(Number(execution.durationSeconds) || steps.length, steps.length)
  const stepDuration = Math.max(Math.floor(totalDuration / steps.length), 1)

  return steps.map((step, index) => {
    const isFailedStep = Number(execution.executeStatus) === 3 && index === steps.length - 1
    const isRunningStep = Number(execution.executeStatus) === 1 && index === steps.length - 1

    return {
      id: `${execution.id}-${step.stepNo || index + 1}`,
      stepNo: step.stepNo || index + 1,
      stepName: step.stepName || `步骤${index + 1}`,
      stepType: step.stepType || 'ACTION',
      scriptLang: step.scriptLang || '',
      message: isFailedStep ? execution.errorMessage || `${step.stepName || `步骤${index + 1}`}执行失败` : `${step.stepName || `步骤${index + 1}`}执行完成`,
      executeTime: addSecondsToMockTime(execution.startTime, stepDuration * index),
      executeStatus: isFailedStep ? 3 : isRunningStep ? 1 : 2,
      stackTrace: isFailedStep ? `MockStackTrace: ${execution.errorMessage || '执行失败'}` : '',
      screenshotUrl: isFailedStep ? execution.screenshotUrl || '' : '',
      durationMillis: stepDuration * 1000
    }
  })
}

export function getMockExecutionDetail(id) {
  const execution = executions.find((item) => item.id === Number(id))
  return wait({
    ...execution,
    stepLogs: buildMockExecutionStepLogs(execution)
  })
}

export function deleteMockExecution(id) {
  const index = executions.findIndex((item) => item.id === id)
  if (index >= 0) {
    executions.splice(index, 1)
  }
  return wait({ id })
}

export function getMockCollectionPage(params = {}) {
  let list = [...collectionRecords]
  if (params.taskId) {
    list = list.filter((item) => item.taskId === Number(params.taskId))
  }
  if (params.keyword) {
    list = list.filter((item) => item.taxpayerIdNo.includes(params.keyword) || item.enterpriseName.includes(params.keyword))
  }
  if (params.status !== '' && typeof params.status !== 'undefined' && params.status !== null) {
    list = list.filter((item) => item.status === Number(params.status))
  }
  if (params.startTime) {
    list = list.filter((item) => new Date(item.collectionTime) >= new Date(params.startTime))
  }
  if (params.endTime) {
    list = list.filter((item) => new Date(item.collectionTime) <= new Date(params.endTime))
  }
  list.sort((a, b) => new Date(b.collectionTime) - new Date(a.collectionTime))
  const summary = {
    total: list.length,
    success: list.filter((item) => item.status === 2).length,
    processing: list.filter((item) => item.status === 1).length,
    failed: list.filter((item) => item.status === 3).length
  }
  return wait({
    summary,
    total: list.length,
    pageNum: Number(params.pageNum || 1),
    pageSize: Number(params.pageSize || 10),
    list
  })
}

export function getMockCollectionDetail(id) {
  return wait(structuredClone(collectionRecords.find((item) => item.id === Number(id))))
}

export function createMockCollectionRecord(payload) {
  const nextId = Math.max(...collectionRecords.map((item) => item.id), 0) + 1
  const task = tasks.find((item) => item.id === Number(payload.taskId))
  const executionId = task ? executions.find((item) => item.taskId === task.id)?.id || 0 : 0
  const status = typeof payload.status === 'undefined' || payload.status === '' ? 0 : Number(payload.status)
  const record = {
    id: nextId,
    taskId: Number(payload.taskId),
    taskCode: task?.taskCode || '',
    executionId,
    status,
    taxpayerIdNo: payload.taxpayerIdNo,
    enterpriseName: payload.enterpriseName,
    sourceName: payload.sourceName,
    errorMessage: payload.errorMessage || '',
    collectionTime: '2026-03-26T11:42:40.3757896',
    createTime: '2026-03-26T11:42:40.3757896',
    updateTime: '2026-03-26T11:42:40.3757896',
    rawData: payload.rawData
  }
  collectionRecords.unshift(record)
  return wait({
    id: record.id,
    taskId: record.taskId,
    taskCode: record.taskCode,
    executionId: record.executionId,
    status: record.status,
    taxpayerIdNo: record.taxpayerIdNo,
    enterpriseName: record.enterpriseName,
    sourceName: record.sourceName,
    collectionTime: record.collectionTime
  })
}

export function deleteMockCollectionRecord(id) {
  const index = collectionRecords.findIndex((item) => item.id === Number(id))
  if (index >= 0) {
    collectionRecords.splice(index, 1)
  }
  return wait({ id: Number(id) })
}

export function getMockAnalysisPage(params = {}) {
  let list = [...analysisRecords]
  if (params.taskId) {
    list = list.filter((item) => item.taskId === Number(params.taskId))
  }
  if (params.status !== '' && typeof params.status !== 'undefined' && params.status !== null) {
    list = list.filter((item) => item.status === Number(params.status))
  }
  if (params.startTime) {
    list = list.filter((item) => new Date(item.analysisTime) >= new Date(params.startTime))
  }
  if (params.endTime) {
    list = list.filter((item) => new Date(item.analysisTime) <= new Date(params.endTime))
  }
  list.sort((a, b) => new Date(b.analysisTime) - new Date(a.analysisTime))
  const summary = {
    total: list.length,
    success: list.filter((item) => item.status === 2).length,
    processing: list.filter((item) => item.status === 1).length,
    failed: list.filter((item) => item.status === 3).length
  }
  return wait({
    summary,
    total: list.length,
    pageNum: Number(params.pageNum || 1),
    pageSize: Number(params.pageSize || 10),
    list: list.map(({ parsedData, taxpayerIdNo, enterpriseName, errorMessage, createTime, updateTime, ...rest }) => rest)
  })
}

export function getMockAnalysisDetail(id) {
  return wait(structuredClone(analysisRecords.find((item) => item.id === Number(id))))
}

export function deleteMockAnalysisRecord(id) {
  const index = analysisRecords.findIndex((item) => item.id === Number(id))
  if (index >= 0) {
    analysisRecords.splice(index, 1)
  }
  return wait({ id: Number(id) })
}

export function getMockProcessingPage(params = {}) {
  let list = [...processingRecords]
  if (params.taskId) {
    list = list.filter((item) => item.taskId === Number(params.taskId))
  }
  if (params.status !== '' && typeof params.status !== 'undefined' && params.status !== null) {
    list = list.filter((item) => item.status === Number(params.status))
  }
  if (params.startTime) {
    list = list.filter((item) => new Date(item.processingTime) >= new Date(params.startTime))
  }
  if (params.endTime) {
    list = list.filter((item) => new Date(item.processingTime) <= new Date(params.endTime))
  }
  list.sort((a, b) => new Date(b.processingTime) - new Date(a.processingTime))
  const summary = {
    total: list.length,
    success: list.filter((item) => item.status === 2).length,
    processing: list.filter((item) => item.status === 1).length,
    failed: list.filter((item) => item.status === 3).length
  }
  return wait({
    summary,
    total: list.length,
    pageNum: Number(params.pageNum || 1),
    pageSize: Number(params.pageSize || 10),
    list: list.map(
      ({ taxpayerIdNo, enterpriseName, errorMessage, processedData, validationDetail, executionId, createTime, updateTime, processTime, ...rest }) => rest
    )
  })
}

export function getMockProcessingDetail(id) {
  return wait(structuredClone(processingRecords.find((item) => item.id === Number(id))))
}

export function deleteMockProcessingRecord(id) {
  const index = processingRecords.findIndex((item) => item.id === Number(id))
  if (index >= 0) {
    processingRecords.splice(index, 1)
  }
  return wait({ id: Number(id) })
}

export function getMockBusinessDataPage(params = {}) {
  let list = [...businessDataRecords]
  if (params.keyword) {
    list = list.filter(
      (item) => item.taxpayerIdNo.includes(params.keyword) || item.enterpriseName.includes(params.keyword)
    )
  }
  if (params.taskId) {
    list = list.filter((item) => item.taskId === Number(params.taskId))
  }
  if (params.taxAreaId) {
    list = list.filter((item) => String(item.taxAreaId || '').includes(params.taxAreaId))
  }
  if (params.dataStatus !== '' && typeof params.dataStatus !== 'undefined' && params.dataStatus !== null) {
    list = list.filter((item) => item.dataStatus === Number(params.dataStatus))
  }
  if (params.startTime) {
    list = list.filter((item) => new Date(item.createTime) >= new Date(params.startTime))
  }
  if (params.endTime) {
    list = list.filter((item) => new Date(item.createTime) <= new Date(params.endTime))
  }
  list.sort((a, b) => new Date(b.createTime) - new Date(a.createTime))
  return wait({
    total: list.length,
    pageNum: Number(params.pageNum || 1),
    pageSize: Number(params.pageSize || 10),
    list: list.map(({ updateTime, businessData, taskCode, ...rest }) => rest)
  })
}

export function getMockBusinessDataDetail(id) {
  return wait(structuredClone(businessDataRecords.find((item) => item.id === Number(id))))
}

export function deleteMockBusinessDataRecord(id) {
  const index = businessDataRecords.findIndex((item) => item.id === Number(id))
  if (index >= 0) {
    businessDataRecords.splice(index, 1)
  }
  return wait({ id: Number(id) })
}

export function getMockProcessDetail(id) {
  const process = processes.find((item) => item.id === Number(id))
  return wait({
    ...process,
    steps: structuredClone(processSteps[id] || []),
    versions: structuredClone(processVersions[id] || [])
  })
}

export function getMockProcessDesignDetail(id) {
  const process = processes.find((item) => item.id === Number(id))
  return wait({
    ...process,
    steps: structuredClone(processSteps[id] || []).sort((a, b) => (a.stepNo || 0) - (b.stepNo || 0)),
    versions: structuredClone(processVersions[id] || []).sort((a, b) => (b.versionNo || 0) - (a.versionNo || 0))
  })
}

export function saveMockProcess(payload) {
  if (payload.id) {
    const target = processes.find((item) => item.id === payload.id)
    Object.assign(target, payload, {
      updateTime: '2026-03-25T15:20:00.000000'
    })
    return wait({
      id: target.id,
      processCode: target.processCode,
      processName: target.processName,
      updateTime: target.updateTime
    })
  }

  const nextId = Math.max(...processes.map((item) => item.id), 0) + 1
  const process = {
    ...payload,
    id: nextId,
    stepCount: 0,
    publishedVersionId: null,
    publishedVersionNo: 0,
    publishStatus: 0,
    createTime: '2026-03-25T15:00:00.000000',
    updateTime: '2026-03-25T15:00:00.000000'
  }
  processes.unshift(process)
  processSteps[nextId] = []
  processVersions[nextId] = []
  return wait({ ...process })
}

export function saveMockProcessDesign(id, payload = {}) {
  const target = processes.find((item) => item.id === Number(id))
  const baseId = Number(id) * 1000
  const steps = (payload.steps || []).map((item, index) => ({
    id: item.id || baseId + index + 1,
    stepNo: item.stepNo,
    stepName: item.stepName,
    stepType: item.stepType,
    scriptLang: item.scriptLang,
    scriptContent: item.scriptContent,
    timeoutSeconds: item.timeoutSeconds || 60,
    failureStrategy: item.failureStrategy || 'STOP',
    createTime: item.createTime || '2026-03-25 15:30:00.000000',
    updateTime: '2026-03-25 15:30:00.000000'
  }))

  processSteps[id] = steps
  if (target) {
    target.stepCount = steps.length
    target.publishStatus = 0
    target.publishedVersionNo = target.publishedVersionNo || 0
    target.updateTime = '2026-03-25T15:30:00.000000'
  }

  return wait({
    ...target,
    steps: structuredClone(steps)
  })
}

export function publishMockProcess(id) {
  const numericId = Number(id)
  const target = processes.find((item) => item.id === numericId)
  const steps = processSteps[numericId] || []
  if (!target || !steps.length) {
    return wait(target || { id: numericId })
  }
  processVersions[numericId] = processVersions[numericId] || []
  processVersions[numericId].forEach((version) => {
    version.versionStatus = 2
  })
  const nextVersionNo = Math.max(...processVersions[numericId].map((item) => item.versionNo), 0) + 1
  const version = {
    id: numericId * 1000 + nextVersionNo,
    versionNo: nextVersionNo,
    versionStatus: 1,
    stepCount: steps.length,
    publishTime: '2026-03-25 15:45:00',
    createTime: '2026-03-25 15:45:00',
    updateTime: '2026-03-25 15:45:00'
  }
  processVersions[numericId].unshift(version)
  Object.assign(target, {
    publishedVersionId: version.id,
    publishedVersionNo: version.versionNo,
    publishStatus: 1,
    stepCount: steps.length,
    updateTime: '2026-03-25T15:45:00.000000'
  })
  return wait({
    ...target,
    steps: structuredClone(steps),
    versions: structuredClone(processVersions[numericId])
  })
}

export function disableMockProcessVersion(id) {
  const numericId = Number(id)
  const target = processes.find((item) => item.id === numericId)
  if (target) {
    target.publishStatus = 2
    target.updateTime = '2026-03-25T15:50:00.000000'
  }
  ;(processVersions[numericId] || []).forEach((version) => {
    if (version.id === target?.publishedVersionId) {
      version.versionStatus = 2
      version.updateTime = '2026-03-25 15:50:00'
    }
  })
  return wait(target || { id: numericId })
}

export function deleteMockProcess(id) {
  const index = processes.findIndex((item) => item.id === id)
  if (index >= 0) {
    processes.splice(index, 1)
    delete processSteps[id]
    delete processVersions[id]
  }
  return wait({ id })
}

export function getMockRobots(params = {}) {
  let list = [...robots]
  if (params.robotName) {
    list = list.filter((item) => item.robotName.includes(params.robotName))
  }
  if (params.robotCode) {
    list = list.filter((item) => item.robotCode.includes(params.robotCode))
  }
  if (params.status !== '' && typeof params.status !== 'undefined' && params.status !== null) {
    list = list.filter((item) => item.status === Number(params.status))
  }
  return wait({
    total: list.length,
    pageNum: Number(params.pageNum || 1),
    pageSize: Number(params.pageSize || 10),
    list
  })
}

export function getMockRobotDetail(id) {
  return wait({ ...robots.find((item) => item.id === Number(id)) })
}

export function saveMockRobot(payload) {
  if (payload.id) {
    const target = robots.find((item) => item.id === payload.id)
    Object.assign(target, payload, {
      updateTime: '2026-03-25T12:20:00.000000'
    })
    return wait({ ...target })
  }

  const nextId = Math.max(...robots.map((item) => item.id)) + 1
  const robot = {
    ...payload,
    id: nextId,
    currentTaskId: null,
    currentTaskCode: '',
    lastHeartbeatTime: '',
    createTime: '2026-03-25T12:10:00.000000',
    updateTime: '2026-03-25T12:10:00.000000'
  }
  robots.unshift(robot)
  return wait({ ...robot })
}

export function deleteMockRobot(id) {
  const index = robots.findIndex((item) => item.id === id)
  if (index >= 0) {
    robots.splice(index, 1)
  }
  return wait({ id })
}

export function saveMockRole(payload) {
  if (payload.id) {
    const target = roles.find((item) => item.id === payload.id)
    Object.assign(target, payload)
    return wait({ id: target.id, roleCode: target.roleCode, roleName: target.roleName, updateTime: '2026-03-23 15:10:00' })
  }
  const nextId = Math.max(...roles.map((item) => item.id)) + 1
  const role = { ...payload, id: nextId, userCount: 0, createTime: '2026-03-23 15:20:00' }
  roles.unshift(role)
  roleResourceMap[nextId] = []
  return wait({ id: role.id, roleCode: role.roleCode, status: role.status })
}

export function deleteMockRole(id) {
  const index = roles.findIndex((item) => item.id === id)
  if (index >= 0) {
    roles.splice(index, 1)
    delete roleResourceMap[id]
  }
  return wait({ id })
}

export function getMockResourceTree(params = {}) {
  const keyword = String(params.resourceName || '').trim()
  const hasTypeFilter = params.resourceType !== '' && typeof params.resourceType !== 'undefined' && params.resourceType !== null
  const resourceType = hasTypeFilter ? Number(params.resourceType) : null

  if (!keyword && resourceType === null) {
    return wait(structuredClone(resources))
  }

  const filterNodes = (nodes) =>
    nodes
      .map((item) => {
        const children = item.children?.length ? filterNodes(item.children) : []
        const hitSelf = String(item.resourceName || '').includes(keyword)
        const hitType = resourceType === null || Number(item.resourceType) === resourceType

        if ((!hitSelf || !hitType) && !children.length) {
          return null
        }

        return {
          ...structuredClone(item),
          ...(item.children ? { children } : {})
        }
      })
      .filter(Boolean)

  return wait(filterNodes(resources))
}

export function saveMockResource(payload) {
  if (payload.id) {
    const updateNode = (nodes) => {
      for (const node of nodes) {
        if (node.id === payload.id) {
          Object.assign(node, payload)
          return true
        }
        if (node.children?.length && updateNode(node.children)) {
          return true
        }
      }
      return false
    }
    updateNode(resources)
    return wait({ id: payload.id, resourceName: payload.resourceName, updateTime: '2026-03-23 16:00:00' })
  }

  const nextId = Math.max(...flattenResources(resources).map((item) => item.id)) + 1
  const item = { ...payload, id: nextId, children: payload.resourceType === 1 ? [] : undefined }

  if (!payload.parentId) {
    resources.push(item)
  } else {
    const parent = flattenResources(resources).find((node) => node.id === payload.parentId)
    if (parent) {
      parent.children = parent.children || []
      parent.children.push(item)
    }
  }

  return wait({ id: nextId, resourceCode: payload.resourceCode, resourceName: payload.resourceName })
}

export function deleteMockResource(id) {
  const removeNode = (nodes) => {
    const index = nodes.findIndex((item) => item.id === id)
    if (index >= 0) {
      nodes.splice(index, 1)
      return true
    }
    return nodes.some((item) => item.children?.length && removeNode(item.children))
  }

  removeNode(resources)
  return wait({ id })
}

export function saveMockRoleResources(id, resourceIds) {
  roleResourceMap[id] = [...resourceIds]
  return wait({ roleId: id, resourceCount: resourceIds.length })
}

export function getMockRoleResources(id) {
  return wait(roleResourceMap[id] || [])
}

function flattenResources(nodes) {
  const list = []
  const walk = (items) => {
    items.forEach((item) => {
      list.push(item)
      if (item.children?.length) {
        walk(item.children)
      }
    })
  }
  walk(nodes)
  return list
}
