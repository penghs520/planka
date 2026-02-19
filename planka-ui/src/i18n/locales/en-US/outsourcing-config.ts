/**
 * Outsourcing Config English Language Pack
 */
export default {
  // Page Title
  title: 'Attendance Configuration',

    // Tab Titles
    tabs: {
      global: 'Global Settings',
      attendance: 'Attendance Settings',
      leave: 'Leave Settings',
      overtime: 'Overtime Settings',
      attendanceChange: 'Makeup Settings',
      settlement: 'Settlement Settings'
    },

    // Global Settings
    global: {
      durationUnit: 'Time Unit',
      durationUnitPlaceholder: 'Select time unit',
      decimalScale: 'Decimal Places',
      decimalScalePlaceholder: 'Enter decimal places',
      cardAttendanceRequired: 'Card Attendance Required',
      cardAttendanceRequiredTip: 'Cards must have attendance information',
      memberCardType: 'Member Card Type',
      memberCardTypePlaceholder: 'Select member card type',
      memberCardTypeTip: 'Select the member card type participating in attendance',
      memberFilter: 'Member Filter Conditions',
      memberFilterTip: 'Set which members of this card type participate in attendance, all members of this type participate when not configured'
    },

    // Attendance Settings
    attendance: {
      workStart: 'Work Start Time',
      workStartPlaceholder: 'Select work start time',
      workEnd: 'Work End Time',
      workEndPlaceholder: 'Select work end time',
      lunchStart: 'Lunch Start Time',
      lunchStartPlaceholder: 'Select lunch start time',
      lunchEnd: 'Lunch End Time',
      lunchEndPlaceholder: 'Select lunch end time',
      workDuration: 'Standard Work Duration',
      workDurationPlaceholder: 'Enter standard work duration',
      workDurationUnit: 'hours',
      impactWm: 'Work Time for Allocation',
      impactWmTip: 'Work time will be used for work allocation',
      accumulatedOvertime: 'Accumulate Overtime',
      accumulatedOvertimeTip: 'Allocatable work time will accumulate overtime',
      absenceWhenNoSignInOrOut: 'Absence When Only Sign In/Out',
      absenceWhenNoSignInOrOutTip: 'Count as absence when only sign in or only sign out'
    },

    // Leave Settings
    leave: {
      leaveUnit: 'Minimum Leave Unit',
      leaveUnitPlaceholder: 'Select minimum leave unit',
      enabledLeaveTypes: 'Enabled Leave Types',
      enabledLeaveTypesTooltip: 'Select enabled leave types in the system. Note: Enabled leave types will affect "Leave Limit Rules", "Overtime Conversion Rules", and "Special Leave Types" configurations. Unchecking a type will automatically delete related configurations. Please modify with caution.',
      limitRules: 'Leave Limit Rules',
      limitRulesEmpty: 'No leave limit rules',
      addLimitRule: 'Add Limit Rule',
      range: 'Period Range',
      rangePlaceholder: 'Select period range',
      itemId: 'Leave Type',
      itemIdPlaceholder: 'Select leave type',
      limit: 'Limit Days',
      limitPlaceholder: 'Enter limit days',
      limitUnit: 'days'
    },

    // Overtime Settings
    overtime: {
      calWay: 'Calculation Method',
      calWayPlaceholder: 'Select calculation method',
      startDuration: 'Overtime Start Duration',
      startDurationPlaceholder: 'Enter overtime start duration',
      startDurationUnit: 'minutes',
      minDuration: 'Minimum Overtime Duration',
      minDurationPlaceholder: 'Enter minimum overtime duration',
      minDurationUnit: 'minutes',
      limitRules: 'Overtime Duration Limits',
      limitRulesEmpty: 'No overtime duration limits',
      addLimitRule: 'Add Limit Rule',
      range: 'Period Range',
      rangePlaceholder: 'Select period range',
      limit: 'Duration Limit',
      limitPlaceholder: 'Enter duration limit',
      limitUnit: 'minutes',
      nonWorkOvertime: 'Non-Work Day Overtime Settings',
      nonWorkOvertimeLimit: 'Daily Limit',
      nonWorkOvertimeLimitPlaceholder: 'Enter daily limit',
      nonWorkOvertimeLimitUnit: 'minutes',
      calRule: 'Overtime Conversion Rules',
      calRuleEmpty: 'No overtime conversion rules',
      addCalRule: 'Add Conversion Rule',
      type: 'Overtime Type',
      typePlaceholder: 'Select overtime type',
      ratio: 'Conversion Ratio',
      ratioPlaceholder: 'Enter conversion ratio',
      leaveItemId: 'Associated Leave',
      leaveItemIdPlaceholder: 'Select associated leave'
    },

    // Makeup Settings
    attendanceChange: {
      count: 'Makeup Count',
      countPlaceholder: 'Enter makeup count',
      countUnit: 'times',
      window: 'Makeup Window',
      windowPlaceholder: 'Enter makeup window',
      windowUnit: 'Window Unit',
      windowUnitPlaceholder: 'Select window unit',
      allowWeekendOrHoliday: 'Allow Non-Work Day Makeup',
      allowWeekendOrHolidayTip: 'Allow makeup for non-work days',
      signIn: 'Sign In Time Limit',
      signOut: 'Sign Out Time Limit',
      timeStart: 'Start Time',
      timeStartPlaceholder: 'Select start time',
      timeEnd: 'End Time',
      timeEndPlaceholder: 'Select end time'
    },

    // Settlement Settings
    settlement: {
      method: 'Settlement Method',
      methodPlaceholder: 'Select settlement method',
      absenteeismDeductionCoefficient: 'Absenteeism Deduction Coefficient',
      absenteeismDeductionCoefficientPlaceholder: 'Enter absenteeism deduction coefficient',
      durationUnit: 'Settlement Unit',
      durationUnitPlaceholder: 'Select settlement unit',
      decimalScale: 'Decimal Places',
      decimalScalePlaceholder: 'Enter decimal places',
      specialLeaveItemIds: 'Special Leave Types',
      specialLeaveItemIdsPlaceholder: 'Select special leave types',
      vutIds: 'Member Card Types',
      vutIdsPlaceholder: 'Select member card types',
      leaveDateFieldId: 'Leave Date Field',
      leaveDateFieldIdPlaceholder: 'Select leave date field',
      personalServiceFeeConf: 'Personal Service Fee Configuration',
      baseFeeFieldId: 'Base Fee Field',
      baseFeeFieldIdPlaceholder: 'Select base fee field',
      overtimeFeeFieldId: 'Overtime Fee Field',
      overtimeFeeFieldIdPlaceholder: 'Select overtime fee field',
      subsidyFieldId: 'Subsidy Field',
      subsidyFieldIdPlaceholder: 'Select subsidy field',
      projectServiceFeeConf: 'Project Service Fee Allocation Configuration',
      columns: 'Allocation Dimensions',
      columnsEmpty: 'No allocation dimensions',
      addColumn: 'Add Dimension',
      column: 'Dimension Column',
      columnPlaceholder: 'Enter dimension column name',
      active: 'Active'
    },

    // Enum Labels
    enums: {
      durationUnit: {
        HOUR: 'Hour',
        MINUTE: 'Minute'
      },
      leaveUnit: {
        HALF_DAY: 'Half Day',
        DAY: 'Day'
      },
      dateUnit: {
        DAY: 'Day',
        WEEK: 'Week',
        MONTH: 'Month'
      },
      overtimeCalWay: {
        ACTUAL_ATTENDANCE: 'Actual Attendance'
      },
      overtimeType: {
        WORKDAY: 'Workday',
        WEEKEND: 'Weekend',
        HOLIDAY: 'Holiday'
      },
      windowUnit: {
        CALENDAR_DAY: 'Calendar Day',
        BUSINESS_DAY: 'Business Day'
      },
      settlementMethod: {
        AUTO: 'Auto Settlement',
        MANUAL: 'Manual Settlement'
      }
    },

    // Action Buttons
    actions: {
      save: 'Save',
      reset: 'Reset',
      add: 'Add',
      delete: 'Delete',
      edit: 'Edit',
      cancel: 'Cancel',
      confirm: 'Confirm'
    },

    // Messages
    messages: {
      saveSuccess: 'Saved successfully',
      saveFailed: 'Save failed',
      resetSuccess: 'Reset successfully',
      loadFailed: 'Failed to load configuration',
      deleteConfirm: 'Are you sure to delete?',
      hasUnsavedChanges: 'You have unsaved changes. Are you sure to leave?'
    },

    // Validation Messages
    validation: {
      required: 'This field is required',
      timeFormat: 'Time format must be HH:mm',
      workEndAfterStart: 'Work end time must be after start time',
      lunchEndAfterStart: 'Lunch end time must be after start time',
      lunchInWorkTime: 'Lunch time must be within work time',
      timeEndAfterStart: 'End time must be after start time',
      positiveNumber: 'Must be a positive number',
      nonNegativeNumber: 'Must be ≥ 0',
      integerNumber: 'Must be an integer',
      minValue: 'Minimum value is {min}',
      maxValue: 'Maximum value is {max}',
      minLength: 'Minimum length is {min}',
      maxLength: 'Maximum length is {max}',
      invalidFormat: 'Invalid format',
      vutIdsRequired: 'Member card types list cannot be empty'
    }
  }

