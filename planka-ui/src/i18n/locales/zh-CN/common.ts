/**
 * 通用文本 - 中文
 */
export default {
  // 操作按钮
  action: {
    save: '保存',
    cancel: '取消',
    confirm: '确认',
    delete: '删除',
    edit: '编辑',
    create: '创建',
    add: '添加',
    remove: '移除',
    search: '搜索',
    filter: '筛选',
    refresh: '刷新',
    clear: '清除',
    apply: '应用',
    close: '关闭',
    submit: '提交',
    reset: '重置',
    loadMore: '加载更多',
    back: '返回',
    ok: '确定',
    export: '导出',
    exportPdf: '导出为 PDF',
    exportMarkdown: '导出为 Markdown',
    change: '更改',
    actions: '操作',
  },

  // 通用词
  more: '更多',
  collapse: '收起',

  // 时间
  time: {
    empty: '-',
    justNow: '刚刚',
    minutesAgo: '{value}分钟前',
    hoursAgo: '{value}小时前',
    daysAgo: '{value}天前',
  },

  // 状态
  state: {
    loading: '加载中...',
    saving: '保存中...',
    success: '操作成功',
    failed: '操作失败',
    empty: '暂无数据',
    current: '当前',
  },

  // 通用状态
  status: {
    inDevelopment: '功能开发中，敬请期待',
  },

  // 提示
  message: {
    confirmDelete: '确定要删除吗？',
    unsavedChanges: '有未保存的更改，确定要离开吗？',
    createSuccess: '创建成功',
    saveSuccess: '保存成功',
    saveFailed: '保存失败',
    deleteFailed: '删除失败',
    loadFailed: '加载失败',
    loginSuccess: '登录成功',
    loginFailed: '登录失败',
    pleaseInput: '请输入',
    pleaseSelect: '请选择',
    descriptionEmpty: '描述内容为空',
    cannotGetDescription: '无法获取描述内容',
    exportPdfSuccess: 'PDF 导出成功',
    exportPdfFailed: 'PDF 导出失败',
  },

  // 排序
  sort: {
    asc: '升序',
    desc: '降序',
  },

  // 布局
  layout: {
    profile: '个人设置',
    switchOrg: '切换组织',
    adminPanel: '管理后台',
    logout: '退出登录',
    pinnedMenus: '置顶菜单',
    allMenus: '全部菜单',
    noMenus: '暂无菜单',
  },

  // 登录
  login: {
    title: '看板管理系统',
    orgId: '组织 ID',
    orgIdPlaceholder: '请输入或选择组织 ID',
    enterAdmin: '进入管理后台',
    hint: '选择或输入组织 ID 后即可进入管理后台',
    testOrg: '测试组织',
    demoOrg: '演示组织',
  },

  // 工具栏
  toolbar: {
    searchPlaceholder: '请输入标题/描述/编号',
    quickFilter: '快捷过滤',
    selectField: '选择属性',
    rowHeight: '行高',
    rowHeightStandard: '标准',
    rowHeightDouble: '二倍',
    rowHeightTriple: '三倍',
    rowHeightQuadruple: '四倍',
    sort: '排序',
    myFocus: '我的关注',
    export: '视图导出',
    more: '更多',
  },

  // 视图类型
  viewType: {
    list: '列表',
    graph: '图形',
  },

  // ER图
  erDiagram: {
    reference: '引用关系',
    disable: '停用',
    sourceName: '源端名称',
    targetName: '目标端名称',
    multiSelect: '多选',
    singleSelect: '单选',
    implementTypes: '实现类型',
    noImplementTypes: '暂无实现类型',
  },

  // 条件
  condition: {
    depth: '深度',
    currentCard: '当前卡',
    linkIndex: '关联{index}',
    noSearchResult: '暂无搜索结果',
    // 条件编辑器
    and: '且',
    or: '或',
    addCondition: '添加条件',
    addConditionGroup: '添加条件组',
    andLogicHint: '满足以下全部条件',
    emptyHint: '暂无条件，点击下方按钮添加',
    clearAll: '清除全部',
    noConditions: '暂无条件，请添加',
    noFilterConditions: '暂无过滤条件',
    clickToSwitchOr: '点击切换为 或',
    clickToSwitchAnd: '点击切换为 且',
    conditionIndex: '条件 {index}',
    noAvailableFields: '暂无可用字段',
    selectField: '选择字段',
    maxNestingDepth: '最多支持{n}层嵌套',
    selectOperator: '操作符',
    inputText: '输入文本',
    inputNumber: '输入数字',
    noValueNeeded: '无需输入值',
    systemFields: '系统字段',
    customFields: '自定义字段',
    attributes: '属性',
    linkFields: '关联字段',
    loadFieldsFailed: '加载字段失败',
    searchField: '搜索字段',
  },

  // 系统字段
  systemField: {
    cardCycle: '卡片周期',
    valueStreamStatus: '价值流状态',
    title: '标题',
    number: '编号',
    createdAt: '创建时间',
    updatedAt: '更新时间',
    discardedAt: '丢弃时间',
    archivedAt: '归档时间',
  },

  // 条件操作符
  operator: {
    // 文本/标题/关键词/编码
    EQ: '等于',
    NE: '不等于',
    CONTAINS: '包含',
    NOT_CONTAINS: '不包含',
    STARTS_WITH: '以...开始',
    ENDS_WITH: '以...结束',
    // 数字
    GT: '大于',
    GE: '大于等于',
    LT: '小于',
    LE: '小于等于',
    BETWEEN: '在范围内',
    // 日期
    BEFORE: '早于',
    AFTER: '晚于',
    // 枚举/状态
    IN: '在列表中',
    NOT_IN: '不在列表中',
    // 状态特有
    REACHED: '已到达',
    NOT_REACHED: '未到达',
    PASSED: '已超过',
    // 用户特有
    IS_CURRENT_USER: '是当前用户',
    IS_NOT_CURRENT_USER: '不是当前用户',
    // 空值判断
    IS_EMPTY: '为空',
    IS_NOT_EMPTY: '不为空',
    // 关联特有
    HAS_ANY: '有关联',
  },

  // 卡片生命周期状态
  lifecycleState: {
    ACTIVE: '活跃',
    DISCARDED: '丢弃',
    ARCHIVED: '归档',
  },

  // 日期值编辑器
  dateValue: {
    specificDate: '具体日期',
    keyDate: '关键日期',
    // 关键日期选项
    TODAY: '今天',
    YESTERDAY: '昨天',
    TOMORROW: '明天',
    THIS_WEEK: '本周',
    LAST_WEEK: '上周',
    NEXT_WEEK: '下周',
    THIS_MONTH: '本月',
    LAST_MONTH: '上月',
    NEXT_MONTH: '下月',
    THIS_QUARTER: '本季度',
    LAST_QUARTER: '上季度',
    NEXT_QUARTER: '下季度',
    THIS_YEAR: '今年',
    LAST_YEAR: '去年',
    NEXT_YEAR: '明年',
    LAST_7_DAYS: '最近7天',
    LAST_30_DAYS: '最近30天',
    NEXT_7_DAYS: '未来7天',
    NEXT_30_DAYS: '未来30天',
  },

  // 枚举值编辑器
  enumValue: {
    selectOption: '选择选项',
  },

  // 用户值编辑器
  userValue: {
    inputUserId: '输入用户ID（多个用逗号分隔）',
  },

  // 关联值编辑器
  linkValue: {
    hasAny: '有任何关联',
    isEmpty: '没有关联',
    inList: '在列表中',
    notInList: '不在列表中',
    selectCard: '选择卡片...',
    searchAndSelectCard: '搜索并选择卡片',
    // 值类型
    static: '静态值',
    reference: '引用值',
    // 引用来源
    currentUser: '当前用户',
    selectPath: '选择属性',
  },

  // 价值流
  valueStream: {
    waiting: '等待',
    working: '工作中',
    todo: '待办',
    inProgress: '进行中',
    done: '已完成',
    cancelled: '已取消',
    valueStream: '价值流',
  },

  // Schema类型
  schemaType: {
    cardType: '卡片类型',
    fieldDefinition: '属性定义',
    linkType: '关联类型',
    view: '视图',
    structure: '架构线',
    menuGroup: '菜单分组',
    detailTemplate: '详情模板',
    unknown: '未知类型',
    notSupported: 'Schema类型 {type} 不支持跳转',
    popupBlocked: '新标签页被浏览器拦截，请允许弹出窗口后重试',
  },

  // 错误处理
  error: {
    cannotDelete: '无法删除',
    schemaReferenced: 'Schema被其他Schema引用，无法删除',
    unknownReference: '未知引用',
    referencedBy: '引用方:',
    referencedAlert: '该数据已被以下对象引用，无法直接删除。请先解除引用关系：',
    iKnow: '知道了',
  },

  // 路由标题
  route: {
    login: '登录',
    activate: '激活账号',
    selectOrg: '选择组织',
    workspace: '工作区',
    profile: '个人设置',
    changePassword: '修改密码',
    linkTypeList: '关联类型列表',
    linkTypeGraph: '关联类型图',
    detailTemplate: '详情页模板',
    createDetailTemplate: '新建详情页模板',
    editDetailTemplate: '编辑详情页模板',
    cardDetail: '卡片详情',
  },

  // 未知
  unknown: '未知',

  // 用户状态
  userStatus: {
    pendingActivation: '待激活',
    active: '正常',
    disabled: '已禁用',
    locked: '已锁定',
  },

  // 用户设置
  user: {
    profile: '个人设置',
    saveChanges: '保存修改',
    profileUpdated: '个人信息已更新',
    email: '邮箱',
    emailNotEditable: '邮箱不可修改',
    nickname: '昵称',
    nicknamePlaceholder: '请输入昵称',
    nicknameMaxLength: '昵称最多100个字符',
    avatarUrl: '头像 URL',
    avatarUrlPlaceholder: '请输入头像 URL',
    phone: '手机号',
    phonePlaceholder: '请输入手机号',
    phoneMaxLength: '手机号最多20个字符',
    password: '密码',
    changePassword: '修改密码',
    oldPassword: '原密码',
    oldPasswordPlaceholder: '请输入原密码',
    oldPasswordRequired: '请输入原密码',
    newPassword: '新密码',
    newPasswordPlaceholder: '请输入新密码（6-32位）',
    newPasswordRequired: '请输入新密码',
    passwordMinLength: '密码长度至少6位',
    passwordMaxLength: '密码长度最多32位',
    confirmNewPassword: '确认新密码',
    confirmNewPasswordPlaceholder: '请再次输入新密码',
    confirmNewPasswordRequired: '请确认新密码',
    passwordMismatch: '两次输入的密码不一致',
    confirmChange: '确认修改',
    passwordChanged: '密码修改成功',
  },

  // 组织设置
  org: {
    name: '组织名称',
    namePlaceholder: '请输入组织名称',
    nameRequired: '请输入组织名称',
    nameMaxLength: '组织名称最多200个字符',
    description: '组织描述',
    descriptionPlaceholder: '请输入组织描述',
    descriptionMaxLength: '组织描述最多1000个字符',
    logo: '组织 Logo',
    logoPlaceholder: '请输入 Logo URL',
    id: '组织 ID',
    updated: '组织信息已更新',
    select: '选择组织',
    switchedTo: '已切换到 {name}',
    switchFailed: '切换组织失败',
    createSuccess: '组织创建成功',
    create: '创建新组织',
    noMatch: '无匹配的组织',
    status: '状态',
    statusActive: '正常',
    featureSettings: '功能设置',
    attendanceEnabled: '考勤功能',
    attendanceEnabledTip: '开启后，组织成员可以使用考勤管理功能，包括签到、请假、加班等',
  },

  // 工作区
  workspace: {
    selectViewHint: '请从左侧选择一个视图',
  },

  // 卡片操作
  card: {
    confirmArchive: '确认归档',
    archiveConfirmContent: '确定要归档卡片「{title}」吗？',
    archive: '归档',
    archiveSuccess: '归档成功',
    confirmDiscard: '确认丢弃',
    discardConfirmContent: '确定要丢弃卡片「{title}」吗？',
    discard: '丢弃',
    discardSuccess: '丢弃成功',
    notFound: '找不到对应的卡片',
    selectTargetStatus: '请选择目标状态',
    noValueStream: '卡片未配置价值流',
    statusUpdateSuccess: '状态更新成功',
    invalidLinkFieldId: '无效的关联字段ID格式',
    linkUpdateSuccess: '关联更新成功',
  },

  // 视图类型名称
  viewTypeName: {
    list: '列表视图',
    kanban: '看板视图',
    gantt: '甘特图视图',
  },

  // 编辑器
  editor: {
    placeholder: '输入 / 打开命令菜单...',
    enterUrl: '请输入链接地址',
    // 工具栏
    heading1: '一级标题',
    heading2: '二级标题',
    heading3: '三级标题',
    bold: '加粗',
    italic: '斜体',
    underline: '下划线',
    strikethrough: '删除线',
    inlineCode: '行内代码',
    textColor: '字体颜色',
    backgroundColor: '背景颜色',
    bulletList: '无序列表',
    orderedList: '有序列表',
    taskList: '任务列表',
    quote: '引用',
    codeBlock: '代码块',
    table: '表格',
    link: '链接',
    undo: '撤销',
    redo: '重做',
    // 斜杠命令菜单
    slashMenu: {
      title: '基础块',
      heading1: '一级标题',
      heading1Desc: '大标题',
      heading2: '二级标题',
      heading2Desc: '中等标题',
      heading3: '三级标题',
      heading3Desc: '小标题',
      bulletList: '无序列表',
      bulletListDesc: '创建无序列表',
      orderedList: '有序列表',
      orderedListDesc: '创建有序列表',
      taskList: '任务列表',
      taskListDesc: '创建待办事项',
      quote: '引用',
      quoteDesc: '插入引用块',
      codeBlock: '代码块',
      codeBlockDesc: '插入代码块',
      table: '表格',
      tableDesc: '插入表格',
      image: '图片',
      imageDesc: '上传图片',
      divider: '分割线',
      dividerDesc: '插入水平分割线',
    },
    // 表格操作
    tableOperations: {
      addRowBefore: '在上方插入行',
      addRowAfter: '在下方插入行',
      deleteRow: '删除当前行',
      addColumnBefore: '在左侧插入列',
      addColumnAfter: '在右侧插入列',
      deleteColumn: '删除当前列',
      deleteTable: '删除表格',
    },
  },

  // Schema 类型名称
  schemaTypeName: {
    CARD_TYPE: '卡片类型',
    FIELD_DEFINITION: '属性定义',
    FORMULA_DEFINITION: '计算公式',
    LINK_TYPE: '关联类型',
    VIEW: '视图',
    CARD_DETAIL_TEMPLATE: '详情页模板',
    FIELD_CONFIG: '属性配置',
    VALUE_STREAM: '价值流',
    BIZ_RULE: '业务规则',
    FLOW_POLICY: '流转策略',
    CARD_FACE: '卡面',
    CARD_PERMISSION: '权限',
    MENU: '菜单',
    NOTIFICATION_TEMPLATE: '通知模板',
    notSupported: 'Schema类型 {type} 不支持跳转',
    popupBlocked: '新标签页被浏览器拦截，请允许弹窗或使用 Ctrl+点击 打开链接',
  },

  // 是/否
  yes: '是',
  no: '否',

  // 文本表达式模板编辑器
  textExpressionTemplate: {
    sources: {
      card: '当前卡',
      member: '当前成员',
      system: '系统',
    },
    system: {
      currentYear: '当前年份',
      currentMonth: '当前月份',
      currentDate: '当前日期',
      currentTime: '当前时间',
    },
    card: {
      id: 'ID',
      title: '标题',
      code: '编号',
      statusId: '价值流状态',
      createdAt: '创建时间',
      updatedAt: '更新时间',
    },
    placeholder: '输入文本，按 $ 插入变量',
    systemFields: '系统字段',
    customFields: '自定义字段',
    linkFields: '关联字段',
  },
}
