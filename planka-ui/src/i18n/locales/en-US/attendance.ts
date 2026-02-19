/**
 * Attendance module English translations
 */
export default {
  // Page titles
  title: 'Attendance',
  clock: 'Clock',
  myRecords: 'My Records',
  applications: 'Applications',
  approvals: 'Approvals',
  windowConfig: 'Window Config',

  // Clock page
  clockPage: {
    title: 'Clock',
    currentTime: 'Current Time',
    todayStatus: 'Today Status',
    clockIn: 'Clock In',
    clockOut: 'Clock Out',
    notClockedIn: 'Not Clocked In',
    hasClockedIn: 'Clocked In',
    hasClockedOut: 'Clocked Out',
    workHours: 'Work Hours',
    hours: 'hours',
    recentRecords: 'Recent Records',
    clockInSuccess: 'Clock in successful',
    clockOutSuccess: 'Clock out successful',
    clockInFailed: 'Clock in failed',
    clockOutFailed: 'Clock out failed',
    alreadyClockedIn: 'Already clocked in today',
    alreadyClockedOut: 'Already clocked out today',
    pleaseClockInFirst: 'Please clock in first',
  },

  // My records page
  myRecordsPage: {
    title: 'My Attendance',
    calendar: 'Calendar View',
    list: 'List View',
    stats: 'Statistics',
    selectMonth: 'Select Month',
    attendanceDays: 'Attendance Days',
    lateDays: 'Late Days',
    earlyLeaveDays: 'Early Leave Days',
    absentDays: 'Absent Days',
    leaveDays: 'Leave Days',
    overtimeHours: 'Overtime Hours',
    totalWorkHours: 'Total Work Hours',
    days: 'days',
    times: 'times',
  },

  // Applications page
  applicationsPage: {
    title: 'Applications',
    newApplication: 'New Application',
    leaveApplication: 'Leave Application',
    overtimeApplication: 'Overtime Application',
    makeupApplication: 'Makeup Application',
    myApplications: 'My Applications',
    filterByStatus: 'Filter by Status',
    allStatus: 'All Status',
    applicationSuccess: 'Application submitted successfully',
    applicationFailed: 'Application submission failed',
  },

  // Approvals page
  approvalsPage: {
    title: 'Approvals',
    pendingApprovals: 'Pending Approvals',
    filterByType: 'Filter by Type',
    allTypes: 'All Types',
    approve: 'Approve',
    reject: 'Reject',
    approvalComment: 'Approval Comment',
    pleaseEnterComment: 'Please enter approval comment',
    approveSuccess: 'Approved successfully',
    approveFailed: 'Approval failed',
    rejectSuccess: 'Rejected successfully',
    rejectFailed: 'Rejection failed',
    confirmApprove: 'Confirm Approval',
    confirmReject: 'Confirm Rejection',
    approveConfirmMessage: 'Are you sure you want to approve this application?',
    rejectConfirmMessage: 'Are you sure you want to reject this application?',
  },

  // Attendance status
  status: {
    NORMAL: 'Normal',
    LATE: 'Late',
    EARLY_LEAVE: 'Early Leave',
    ABSENT: 'Absent',
    LEAVE: 'Leave',
    OVERTIME: 'Overtime',
  },

  // Approval status
  approvalStatus: {
    PENDING: 'Pending',
    APPROVED: 'Approved',
    REJECTED: 'Rejected',
  },

  // Application type
  applicationType: {
    type: 'Application Type',
    LEAVE: 'Leave',
    OVERTIME: 'Overtime',
    MAKEUP: 'Makeup',
  },

  // Field labels
  field: {
    date: 'Date',
    clockInTime: 'Clock In Time',
    clockOutTime: 'Clock Out Time',
    workHours: 'Work Hours',
    status: 'Status',
    applicant: 'Applicant',
    applyTime: 'Apply Time',
    startTime: 'Start Time',
    endTime: 'End Time',
    duration: 'Duration',
    reason: 'Reason',
    approver: 'Approver',
    approvalTime: 'Approval Time',
    approvalComment: 'Approval Comment',
    remark: 'Remark',
    title: 'Title',
    description: 'Description',
    leaveType: 'Leave Type',
  },

  // Common
  common: {
    noData: 'No Data',
    loading: 'Loading...',
    loadFailed: 'Load Failed',
    today: 'Today',
    yesterday: 'Yesterday',
    thisWeek: 'This Week',
    thisMonth: 'This Month',
    lastMonth: 'Last Month',
  },
}
