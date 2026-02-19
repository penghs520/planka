/**
 * 考勤模块中文翻译
 */
export default {
  // 页面标题
  title: '考勤',
  clock: '打卡',
  myRecords: '我的考勤',
  applications: '申请中心',
  approvals: '待审批',
  windowConfig: '窗口期配置',

  // 打卡页面
  clockPage: {
    title: '打卡',
    currentTime: '当前时间',
    todayStatus: '今日状态',
    clockIn: '签到',
    clockOut: '签退',
    notClockedIn: '未打卡',
    hasClockedIn: '已签到',
    hasClockedOut: '已签退',
    workHours: '工作时长',
    hours: '小时',
    recentRecords: '最近记录',
    clockInSuccess: '签到成功',
    clockOutSuccess: '签退成功',
    clockInFailed: '签到失败',
    clockOutFailed: '签退成功',
    alreadyClockedIn: '今日已签到',
    alreadyClockedOut: '今日已签退',
    pleaseClockInFirst: '请先签到',
  },

  // 我的考勤页面
  myRecordsPage: {
    title: '我的考勤',
    calendar: '日历视图',
    list: '列表视图',
    stats: '统计',
    selectMonth: '选择月份',
    attendanceDays: '出勤天数',
    lateDays: '迟到次数',
    earlyLeaveDays: '早退次数',
    absentDays: '旷工天数',
    leaveDays: '请假天数',
    overtimeHours: '加班时长',
    totalWorkHours: '总工作时长',
    days: '天',
    times: '次',
  },

  // 申请中心页面
  applicationsPage: {
    title: '申请中心',
    newApplication: '新建申请',
    leaveApplication: '请假申请',
    overtimeApplication: '加班申请',
    makeupApplication: '补卡申请',
    myApplications: '我的申请',
    filterByStatus: '按状态筛选',
    allStatus: '全部状态',
    applicationSuccess: '申请提交成功',
    applicationFailed: '申请提交失败',
  },

  // 待审批页面
  approvalsPage: {
    title: '待审批',
    pendingApprovals: '待审批事项',
    filterByType: '按类型筛选',
    allTypes: '全部类型',
    approve: '批准',
    reject: '拒绝',
    approvalComment: '审批意见',
    pleaseEnterComment: '请输入审批意见',
    approveSuccess: '审批成功',
    approveFailed: '审批失败',
    rejectSuccess: '拒绝成功',
    rejectFailed: '拒绝失败',
    confirmApprove: '确认批准',
    confirmReject: '确认拒绝',
    approveConfirmMessage: '确定要批准这个申请吗？',
    rejectConfirmMessage: '确定要拒绝这个申请吗？',
  },

  // 考勤状态
  status: {
    NORMAL: '正常',
    LATE: '迟到',
    EARLY_LEAVE: '早退',
    ABSENT: '旷工',
    LEAVE: '请假',
    OVERTIME: '加班',
  },

  // 审批状态
  approvalStatus: {
    PENDING: '待审批',
    APPROVED: '已批准',
    REJECTED: '已拒绝',
  },

  // 申请类型
  applicationType: {
    type: '申请类型',
    LEAVE: '请假',
    OVERTIME: '加班',
    MAKEUP: '补卡',
  },

  // 字段标签
  field: {
    date: '日期',
    clockInTime: '签到时间',
    clockOutTime: '签退时间',
    workHours: '工作时长',
    status: '状态',
    applicant: '申请人',
    applyTime: '申请时间',
    startTime: '开始时间',
    endTime: '结束时间',
    duration: '时长',
    reason: '原因',
    approver: '审批人',
    approvalTime: '审批时间',
    approvalComment: '审批意见',
    remark: '备注',
    title: '标题',
    description: '描述',
    leaveType: '请假类型',
  },

  // 通用
  common: {
    noData: '暂无数据',
    loading: '加载中...',
    loadFailed: '加载失败',
    today: '今天',
    yesterday: '昨天',
    thisWeek: '本周',
    thisMonth: '本月',
    lastMonth: '上月',
  },
}
