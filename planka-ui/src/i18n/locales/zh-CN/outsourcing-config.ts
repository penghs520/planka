/**
 * 考勤配置中文语言包
 */
export default {
  // 页面标题
  title: '考勤配置',
  helpTooltip: '点击查看考勤配置说明',
  helpTitle: '考勤配置功能说明',

  // 帮助说明
  help: {
    global: '配置工时单位、小数精度、功能开关和参与考勤的成员范围',
    attendance: '配置每日工作时间、午休时间、正常工作时长等签到相关设置',
    leave: '配置启用的请假类型（病假、调休、年假、事假）和各类型的请假限制规则',
    overtime: '配置加班计算方式、起算时间、时长限制和非工作日加班设置',
    attendanceChange: '配置补卡次数、补卡窗口期、时间限制等补卡相关规则',
    settlement: '配置结算方式、缺勤抵扣系数、个人服务费和项目服务费分摊规则'
  },

  // Tab 标题
  tabs: {
    global: '全局配置',
    attendance: '签到配置',
    leave: '请假配置',
    overtime: '加班配置',
    attendanceChange: '补卡配置',
    settlement: '结算配置'
  },

    // 全局配置
    global: {
      durationUnit: '时间统计单位',
      durationUnitPlaceholder: '请选择时间单位',
      decimalScale: '小数位数',
      decimalScalePlaceholder: '请输入小数位数',
      attendanceNotEnabled: '考勤功能未启用。请先在组织设置中开启考勤功能，才能配置考勤相关设置。',
      goToOrgSettings: '前往组织设置',
      cardAttendanceRequired: '卡片考勤必填',
      cardAttendanceRequiredTip: '开启后卡片必须填写考勤信息',
      memberCardType: '成员卡片类型',
      memberCardTypePlaceholder: '请选择成员卡片类型',
      memberCardTypeTip: '选择参与考勤的成员卡片类型',
      memberFilter: '成员筛选条件',
      memberFilterTip: '设置该卡片类型下哪些成员参与考勤，未配置时该类型的所有成员都参与'
    },

    // 签到配置
    attendance: {
      workStart: '工作开始时间',
      workStartPlaceholder: '请选择工作开始时间',
      workEnd: '工作结束时间',
      workEndPlaceholder: '请选择工作结束时间',
      lunchStart: '午休开始时间',
      lunchStartPlaceholder: '请选择午休开始时间',
      lunchEnd: '午休结束时间',
      lunchEndPlaceholder: '请选择午休结束时间',
      workDuration: '标准工作时长',
      workDurationPlaceholder: '请输入标准工作时长',
      workDurationUnit: '小时',
      impactWm: '工作时间用于工时分配',
      impactWmTip: '开启后工作时间将用于工时分配',
      accumulatedOvertime: '累计加班时长',
      accumulatedOvertimeTip: '开启后可分配工作时长将累计加班时长',
      absenceWhenNoSignInOrOut: '只有签入或签出计入旷工',
      absenceWhenNoSignInOrOutTip: '开启后只有签入或只有签出时将计入旷工'
    },

    // 请假配置
    leave: {
      leaveUnit: '最小请假单位',
      leaveUnitPlaceholder: '请选择最小请假单位',
      enabledLeaveTypes: '启用的请假类型',
      enabledLeaveTypesTooltip: '选择系统中启用的请假类型。注意：启用的请假类型会影响"请假限制规则"、"加班换算规则"和"特殊请假类型"的配置，取消勾选某个类型会自动删除相关配置，请谨慎修改',
      limitRules: '请假限制规则',
      limitRulesEmpty: '暂无请假限制规则',
      addLimitRule: '添加限制规则',
      range: '周期范围',
      rangePlaceholder: '请选择周期范围',
      itemId: '请假类型',
      itemIdPlaceholder: '请选择请假类型',
      leaveType: '请假类型',
      leaveTypePlaceholder: '请选择请假类型',
      limit: '上限天数',
      limitPlaceholder: '请输入上限天数',
      limitUnit: '天'
    },

    // 加班配置
    overtime: {
      calWay: '计算方式',
      calWayPlaceholder: '请选择计算方式',
      startDuration: '加班起算时间',
      startDurationPlaceholder: '请输入加班起算时间',
      startDurationUnit: '分钟',
      minDuration: '最小加班时间',
      minDurationPlaceholder: '请输入最小加班时间',
      minDurationUnit: '分钟',
      limitRules: '加班时长限制',
      limitRulesEmpty: '暂无加班时长限制',
      addLimitRule: '添加限制规则',
      range: '周期范围',
      rangePlaceholder: '请选择周期范围',
      limit: '时长上限',
      limitPlaceholder: '请输入时长上限',
      limitUnit: '分钟',
      nonWorkOvertime: '非工作日加班设置',
      nonWorkOvertimeLimit: '单日上限',
      nonWorkOvertimeLimitPlaceholder: '请输入单日上限',
      nonWorkOvertimeLimitUnit: '分钟',
      calRule: '加班换算规则',
      calRuleEmpty: '暂无加班换算规则',
      addCalRule: '添加换算规则',
      type: '加班类型',
      typePlaceholder: '请选择加班类型',
      ratio: '换算比例',
      ratioPlaceholder: '请输入换算比例',
      leaveItemId: '关联假期',
      leaveItemIdPlaceholder: '请选择关联假期'
    },

    // 补卡配置
    attendanceChange: {
      count: '补卡次数',
      countPlaceholder: '请输入补卡次数',
      countUnit: '次',
      window: '补卡窗口',
      windowPlaceholder: '请输入补卡窗口',
      windowUnit: '窗口单位',
      windowUnitPlaceholder: '请选择窗口单位',
      allowWeekendOrHoliday: '允许补非工作日考勤',
      allowWeekendOrHolidayTip: '开启后可以补非工作日的考勤',
      signIn: '签入时间限制',
      signOut: '签出时间限制',
      timeStart: '开始时间',
      timeStartPlaceholder: '请选择开始时间',
      timeEnd: '结束时间',
      timeEndPlaceholder: '请选择结束时间'
    },

    // 结算配置
    settlement: {
      method: '结算方式',
      methodPlaceholder: '请选择结算方式',
      absenteeismDeductionCoefficient: '缺勤抵扣系数',
      absenteeismDeductionCoefficientPlaceholder: '请输入缺勤抵扣系数',
      durationUnit: '结算单位',
      durationUnitPlaceholder: '请选择结算单位',
      decimalScale: '小数位数',
      decimalScalePlaceholder: '请输入小数位数',
      specialLeaveItemIds: '特殊请假类型',
      specialLeaveItemIdsPlaceholder: '请选择特殊请假类型',
      vutIds: '成员卡片类型',
      vutIdsPlaceholder: '请选择成员卡片类型',
      leaveDateFieldId: '离职日期字段',
      leaveDateFieldIdPlaceholder: '请选择离职日期字段',
      personalServiceFeeConf: '个人服务费配置',
      baseFeeFieldId: '基础费用字段',
      baseFeeFieldIdPlaceholder: '请选择基础费用字段',
      overtimeFeeFieldId: '加班费用字段',
      overtimeFeeFieldIdPlaceholder: '请选择加班费用字段',
      subsidyFieldId: '补贴字段',
      subsidyFieldIdPlaceholder: '请选择补贴字段',
      projectServiceFeeConf: '项目服务费分摊配置',
      columns: '分摊维度',
      columnsEmpty: '暂无分摊维度',
      addColumn: '添加维度',
      column: '维度列',
      columnPlaceholder: '请输入维度列名称',
      active: '是否启用'
    },

    // 枚举值标签
    enums: {
      durationUnit: {
        HOUR: '小时',
        MINUTE: '分钟'
      },
      leaveUnit: {
        HALF_DAY: '半天',
        DAY: '天'
      },
      leaveType: {
        SICK_LEAVE: '病假',
        TIME_OFF: '调休',
        ANNUAL_LEAVE: '年假',
        PERSONAL_LEAVE: '事假'
      },
      dateUnit: {
        DAY: '天',
        WEEK: '周',
        MONTH: '月'
      },
      overtimeCalWay: {
        ACTUAL_ATTENDANCE: '实际考勤'
      },
      overtimeType: {
        WORKDAY: '工作日',
        WEEKEND: '周末',
        HOLIDAY: '节假日'
      },
      windowUnit: {
        CALENDAR_DAY: '自然日',
        BUSINESS_DAY: '工作日'
      },
      settlementMethod: {
        AUTO: '自动结算',
        MANUAL: '手动结算'
      }
    },

    // 操作按钮
    actions: {
      save: '保存',
      reset: '重置',
      add: '添加',
      delete: '删除',
      edit: '编辑',
      cancel: '取消',
      confirm: '确认'
    },

    // 提示信息
    messages: {
      saveSuccess: '保存成功',
      saveFailed: '保存失败',
      resetSuccess: '已重置',
      loadFailed: '加载配置失败',
      deleteConfirm: '确认删除吗？',
      hasUnsavedChanges: '有未保存的更改，确认离开吗？'
    },

    // 校验错误信息
    validation: {
      required: '此项为必填项',
      timeFormat: '时间格式必须为 HH:mm',
      workEndAfterStart: '工作结束时间必须晚于工作开始时间',
      lunchEndAfterStart: '午休结束时间必须晚于午休开始时间',
      lunchInWorkTime: '午休时间必须在工作时间段内',
      timeEndAfterStart: '结束时间必须晚于开始时间',
      positiveNumber: '必须为正数',
      nonNegativeNumber: '必须 ≥ 0',
      integerNumber: '必须为整数',
      minValue: '最小值为 {min}',
      maxValue: '最大值为 {max}',
      minLength: '最小长度为 {min}',
      maxLength: '最大长度为 {max}',
      invalidFormat: '格式不正确',
      vutIdsRequired: '成员卡片类型列表不能为空'
    }
  }

