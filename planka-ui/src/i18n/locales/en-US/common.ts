/**
 * Common Text - English
 */
export default {
  // Action buttons
  action: {
    save: 'Save',
    cancel: 'Cancel',
    confirm: 'Confirm',
    delete: 'Delete',
    edit: 'Edit',
    create: 'Create',
    add: 'Add',
    remove: 'Remove',
    search: 'Search',
    filter: 'Filter',
    refresh: 'Refresh',
    clear: 'Clear',
    apply: 'Apply',
    close: 'Close',
    submit: 'Submit',
    reset: 'Reset',
    loadMore: 'Load More',
    back: 'Back',
    ok: 'OK',
    export: 'Export',
    exportPdf: 'Export as PDF',
    exportMarkdown: 'Export as Markdown',
    change: 'Change',
    actions: 'Actions',
  },

  // Common words
  more: 'more',
  collapse: 'Collapse',

  // Time
  time: {
    empty: '-',
    justNow: 'Just now',
    minutesAgo: '{value} minutes ago',
    hoursAgo: '{value} hours ago',
    daysAgo: '{value} days ago',
  },

  // States
  state: {
    loading: 'Loading...',
    saving: 'Saving...',
    success: 'Operation successful',
    failed: 'Operation failed',
    empty: 'No data',
    current: 'Current',
  },

  // Status
  status: {
    inDevelopment: 'Feature in development, coming soon',
  },

  // Messages
  message: {
    confirmDelete: 'Are you sure you want to delete?',
    unsavedChanges: 'You have unsaved changes. Are you sure you want to leave?',
    createSuccess: 'Created successfully',
    saveSuccess: 'Saved successfully',
    saveFailed: 'Failed to save',
    deleteFailed: 'Failed to delete',
    loadFailed: 'Failed to load',
    loginSuccess: 'Login successful',
    loginFailed: 'Login failed',
    pleaseInput: 'Please enter',
    pleaseSelect: 'Please select',
    descriptionEmpty: 'Description is empty',
    cannotGetDescription: 'Unable to get description',
    exportPdfSuccess: 'PDF exported successfully',
    exportPdfFailed: 'Failed to export PDF',
  },

  // Sort
  sort: {
    asc: 'Ascending',
    desc: 'Descending',
  },

  // Layout
  layout: {
    profile: 'Profile',
    switchOrg: 'Switch Organization',
    adminPanel: 'Admin Panel',
    logout: 'Logout',
    pinnedMenus: 'Pinned',
    allMenus: 'All Menus',
    noMenus: 'No menus',
  },

  // Login
  login: {
    title: 'planka Management System',
    orgId: 'Organization ID',
    orgIdPlaceholder: 'Enter or select organization ID',
    enterAdmin: 'Enter Admin Panel',
    hint: 'Select or enter organization ID to access admin panel',
    testOrg: 'Test Organization',
    demoOrg: 'Demo Organization',
  },

  // Toolbar
  toolbar: {
    searchPlaceholder: 'Search title/description/number',
    quickFilter: 'Quick Filter',
    selectField: 'Select field',
    rowHeight: 'Row Height',
    rowHeightStandard: 'Standard',
    rowHeightDouble: 'Double',
    rowHeightTriple: 'Triple',
    rowHeightQuadruple: 'Quadruple',
    sort: 'Sort',
    myFocus: 'My Focus',
    export: 'Export',
    more: 'More',
  },

  // View types
  viewType: {
    list: 'List',
    graph: 'Graph',
  },

  // ER Diagram
  erDiagram: {
    reference: 'References',
    disable: 'Disable',
    sourceName: 'Source Name',
    targetName: 'Target Name',
    multiSelect: 'Multi-select',
    singleSelect: 'Single-select',
    implementTypes: 'Implementation Types',
    noImplementTypes: 'No implementation types',
  },

  // Condition
  condition: {
    depth: 'Depth',
    currentCard: 'Current Card',
    linkIndex: 'Link {index}',
    noSearchResult: 'No search results',
    // Condition editor
    and: 'AND',
    or: 'OR',
    addCondition: 'Add Condition',
    addConditionGroup: 'Add Condition Group',
    andLogicHint: 'All of the following conditions must be met',
    emptyHint: 'No conditions yet, click the button below to add',
    clearAll: 'Clear All',
    noConditions: 'No conditions, please add',
    noFilterConditions: 'No filter conditions',
    clickToSwitchOr: 'Click to switch to OR',
    clickToSwitchAnd: 'Click to switch to AND',
    conditionIndex: 'Condition {index}',
    noAvailableFields: 'No available fields',
    selectField: 'Select field',
    maxNestingDepth: 'Maximum {n} levels of nesting',
    selectOperator: 'Operator',
    inputText: 'Enter text',
    inputNumber: 'Enter number',
    noValueNeeded: 'No value needed',
    systemFields: 'System Fields',
    customFields: 'Custom Fields',
    attributes: 'Attributes',
    linkFields: 'Link Fields',
    loadFieldsFailed: 'Failed to load fields',
    searchField: 'Search field',
  },

  // System fields
  systemField: {
    cardCycle: 'Card Cycle',
    valueStreamStatus: 'Value Stream Status',
    title: 'Title',
    number: 'Number',
    createdAt: 'Created At',
    updatedAt: 'Updated At',
    discardedAt: 'Discarded At',
    archivedAt: 'Archived At',
  },

  // Condition operators
  operator: {
    // Text/Title/Keyword/Code
    EQ: 'Equals',
    NE: 'Not equals',
    CONTAINS: 'Contains',
    NOT_CONTAINS: 'Does not contain',
    STARTS_WITH: 'Starts with',
    ENDS_WITH: 'Ends with',
    // Number
    GT: 'Greater than',
    GE: 'Greater than or equal',
    LT: 'Less than',
    LE: 'Less than or equal',
    BETWEEN: 'Between',
    // Date
    BEFORE: 'Before',
    AFTER: 'After',
    // Enum/Status
    IN: 'In list',
    NOT_IN: 'Not in list',
    // Status specific
    REACHED: 'Reached',
    NOT_REACHED: 'Not reached',
    PASSED: 'Passed',
    // User specific
    IS_CURRENT_USER: 'Is current user',
    IS_NOT_CURRENT_USER: 'Is not current user',
    // Empty check
    IS_EMPTY: 'Is empty',
    IS_NOT_EMPTY: 'Is not empty',
    // Link specific
    HAS_ANY: 'Has any',
  },

  // Card lifecycle states
  lifecycleState: {
    ACTIVE: 'Active',
    DISCARDED: 'Discarded',
    ARCHIVED: 'Archived',
  },

  // Date value editor
  dateValue: {
    specificDate: 'Specific date',
    keyDate: 'Key date',
    // Key date options
    TODAY: 'Today',
    YESTERDAY: 'Yesterday',
    TOMORROW: 'Tomorrow',
    THIS_WEEK: 'This week',
    LAST_WEEK: 'Last week',
    NEXT_WEEK: 'Next week',
    THIS_MONTH: 'This month',
    LAST_MONTH: 'Last month',
    NEXT_MONTH: 'Next month',
    THIS_QUARTER: 'This quarter',
    LAST_QUARTER: 'Last quarter',
    NEXT_QUARTER: 'Next quarter',
    THIS_YEAR: 'This year',
    LAST_YEAR: 'Last year',
    NEXT_YEAR: 'Next year',
    LAST_7_DAYS: 'Last 7 days',
    LAST_30_DAYS: 'Last 30 days',
    NEXT_7_DAYS: 'Next 7 days',
    NEXT_30_DAYS: 'Next 30 days',
  },

  // Enum value editor
  enumValue: {
    selectOption: 'Select option',
  },

  // User value editor
  userValue: {
    inputUserId: 'Enter user ID (separate multiple with comma)',
  },

  // Link value editor
  linkValue: {
    hasAny: 'Has any link',
    isEmpty: 'Has no link',
    inList: 'In list',
    notInList: 'Not in list',
    selectCard: 'Select card...',
    searchAndSelectCard: 'Search and select card',
    // Value types
    static: 'Static',
    reference: 'Reference',
    // Reference sources
    currentUser: 'Current User',
    selectPath: 'Select field',
  },

  // Value stream
  valueStream: {
    waiting: 'Waiting',
    working: 'Working',
    todo: 'To Do',
    inProgress: 'In Progress',
    done: 'Done',
    cancelled: 'Cancelled',
    valueStream: 'Value Stream',
  },

  // Schema types
  schemaType: {
    cardType: 'Card Type',
    fieldDefinition: 'Field Definition',
    linkType: 'Link Type',
    view: 'View',
    structure: 'Structure Line',
    menuGroup: 'Menu Group',
    detailTemplate: 'Detail Template',
    unknown: 'Unknown Type',
    notSupported: 'Schema type {type} does not support navigation',
    popupBlocked: 'Popup blocked by browser. Please allow popups and try again.',
  },

  // Error handling
  error: {
    cannotDelete: 'Cannot Delete',
    schemaReferenced: 'Schema is referenced by other schemas and cannot be deleted',
    unknownReference: 'Unknown reference',
    referencedBy: 'Referenced by:',
    referencedAlert: 'This data is referenced by the following objects and cannot be deleted directly. Please remove the references first:',
    iKnow: 'Got it',
  },

  // Route titles
  route: {
    login: 'Login',
    activate: 'Activate Account',
    selectOrg: 'Select Organization',
    workspace: 'Workspace',
    profile: 'Profile',
    changePassword: 'Change Password',
    linkTypeList: 'Link Type List',
    linkTypeGraph: 'Link Type Graph',
    detailTemplate: 'Detail Template',
    createDetailTemplate: 'Create Detail Template',
    editDetailTemplate: 'Edit Detail Template',
    cardDetail: 'Card Detail',
  },

  // Unknown
  unknown: 'Unknown',

  // User status
  userStatus: {
    pendingActivation: 'Pending Activation',
    active: 'Active',
    disabled: 'Disabled',
    locked: 'Locked',
  },

  // User settings
  user: {
    profile: 'Profile',
    saveChanges: 'Save Changes',
    profileUpdated: 'Profile updated',
    email: 'Email',
    emailNotEditable: 'Email cannot be changed',
    nickname: 'Nickname',
    nicknamePlaceholder: 'Enter nickname',
    nicknameMaxLength: 'Nickname cannot exceed 100 characters',
    avatarUrl: 'Avatar URL',
    avatarUrlPlaceholder: 'Enter avatar URL',
    phone: 'Phone',
    phonePlaceholder: 'Enter phone number',
    phoneMaxLength: 'Phone cannot exceed 20 characters',
    password: 'Password',
    changePassword: 'Change Password',
    oldPassword: 'Current Password',
    oldPasswordPlaceholder: 'Enter current password',
    oldPasswordRequired: 'Please enter current password',
    newPassword: 'New Password',
    newPasswordPlaceholder: 'Enter new password (6-32 characters)',
    newPasswordRequired: 'Please enter new password',
    passwordMinLength: 'Password must be at least 6 characters',
    passwordMaxLength: 'Password cannot exceed 32 characters',
    confirmNewPassword: 'Confirm New Password',
    confirmNewPasswordPlaceholder: 'Re-enter new password',
    confirmNewPasswordRequired: 'Please confirm new password',
    passwordMismatch: 'Passwords do not match',
    confirmChange: 'Confirm',
    passwordChanged: 'Password changed successfully',
  },

  // Organization settings
  org: {
    name: 'Organization Name',
    namePlaceholder: 'Enter organization name',
    nameRequired: 'Please enter organization name',
    nameMaxLength: 'Organization name cannot exceed 200 characters',
    description: 'Organization Description',
    descriptionPlaceholder: 'Enter organization description',
    descriptionMaxLength: 'Organization description cannot exceed 1000 characters',
    logo: 'Organization Logo',
    logoPlaceholder: 'Enter Logo URL',
    id: 'Organization ID',
    updated: 'Organization info updated',
    select: 'Select Organization',
    switchedTo: 'Switched to {name}',
    switchFailed: 'Failed to switch organization',
    createSuccess: 'Organization created successfully',
    create: 'Create New Organization',
    noMatch: 'No matching organizations',
    status: 'Status',
    statusActive: 'Active',
  },

  // Workspace
  workspace: {
    selectViewHint: 'Please select a view from the left',
  },

  // Card operations
  card: {
    confirmArchive: 'Confirm Archive',
    archiveConfirmContent: 'Are you sure you want to archive card "{title}"?',
    archive: 'Archive',
    archiveSuccess: 'Archived successfully',
    confirmDiscard: 'Confirm Discard',
    discardConfirmContent: 'Are you sure you want to discard card "{title}"?',
    discard: 'Discard',
    discardSuccess: 'Discarded successfully',
    notFound: 'Card not found',
    selectTargetStatus: 'Please select target status',
    noValueStream: 'Card has no value stream configured',
    statusUpdateSuccess: 'Status updated successfully',
    invalidLinkFieldId: 'Invalid link field ID format',
    linkUpdateSuccess: 'Link updated successfully',
  },

  // View type names
  viewTypeName: {
    list: 'List View',
    planka: 'planka View',
    gantt: 'Gantt View',
  },

  // Editor
  editor: {
    placeholder: 'Type / for commands...',
    enterUrl: 'Enter URL',
    // Toolbar
    heading1: 'Heading 1',
    heading2: 'Heading 2',
    heading3: 'Heading 3',
    bold: 'Bold',
    italic: 'Italic',
    underline: 'Underline',
    strikethrough: 'Strikethrough',
    inlineCode: 'Inline Code',
    textColor: 'Text Color',
    backgroundColor: 'Background Color',
    bulletList: 'Bullet List',
    orderedList: 'Ordered List',
    taskList: 'Task List',
    quote: 'Quote',
    codeBlock: 'Code Block',
    table: 'Table',
    link: 'Link',
    undo: 'Undo',
    redo: 'Redo',
    // Slash command menu
    slashMenu: {
      title: 'Basic Blocks',
      heading1: 'Heading 1',
      heading1Desc: 'Large heading',
      heading2: 'Heading 2',
      heading2Desc: 'Medium heading',
      heading3: 'Heading 3',
      heading3Desc: 'Small heading',
      bulletList: 'Bullet List',
      bulletListDesc: 'Create a bullet list',
      orderedList: 'Ordered List',
      orderedListDesc: 'Create an ordered list',
      taskList: 'Task List',
      taskListDesc: 'Create a todo list',
      quote: 'Quote',
      quoteDesc: 'Insert a quote block',
      codeBlock: 'Code Block',
      codeBlockDesc: 'Insert a code block',
      table: 'Table',
      tableDesc: 'Insert a table',
      image: 'Image',
      imageDesc: 'Upload an image',
      divider: 'Divider',
      dividerDesc: 'Insert a horizontal line',
    },
    // Table operations
    tableOperations: {
      addRowBefore: 'Insert row above',
      addRowAfter: 'Insert row below',
      deleteRow: 'Delete row',
      addColumnBefore: 'Insert column left',
      addColumnAfter: 'Insert column right',
      deleteColumn: 'Delete column',
      deleteTable: 'Delete table',
    },
  },

  // Schema type names
  schemaTypeName: {
    CARD_TYPE: 'Card Type',
    FIELD_DEFINITION: 'Field Definition',
    FORMULA_DEFINITION: 'Formula',
    LINK_TYPE: 'Link Type',
    VIEW: 'View',
    CARD_DETAIL_TEMPLATE: 'Detail Template',
    FIELD_CONFIG: 'Field Config',
    VALUE_STREAM: 'Value Stream',
    BIZ_RULE: 'Business Rule',
    FLOW_POLICY: 'Flow Policy',
    CARD_FACE: 'Card Face',
    CARD_PERMISSION: 'Permission',
    MENU: 'Menu',
    NOTIFICATION_TEMPLATE: 'Notification Template',
    notSupported: 'Schema type {type} does not support navigation',
    popupBlocked: 'New tab was blocked by browser. Please allow popups or use Ctrl+Click to open.',
  },

  // Yes/No
  yes: 'Yes',
  no: 'No',

  // Text Expression Template Editor
  textExpressionTemplate: {
    sources: {
      card: 'Current Card',
      member: 'Current Member',
      system: 'System',
    },
    system: {
      currentYear: 'Current Year',
      currentMonth: 'Current Month',
      currentDate: 'Current Date',
      currentTime: 'Current Time',
    },
    card: {
      id: 'ID',
      title: 'Title',
      code: 'Code',
      statusId: 'Status',
      createdAt: 'Created At',
      updatedAt: 'Updated At',
    },
    placeholder: 'Type text, press $ to insert variable',
    systemFields: 'System Fields',
    customFields: 'Custom Fields',
    linkFields: 'Link Fields',
  },
}
