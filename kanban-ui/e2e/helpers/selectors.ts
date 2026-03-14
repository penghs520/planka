/**
 * Arco Design Vue 组件选择器封装
 * 基于 Arco Design 的 CSS 类名约定
 */
export const arco = {
  // 表单
  formItem: (label: string) =>
    `.arco-form-item:has(.arco-form-item-label:text("${label}"))`,
  input: (label: string) =>
    `.arco-form-item:has(.arco-form-item-label:text("${label}")) input`,
  select: (label: string) =>
    `.arco-form-item:has(.arco-form-item-label:text("${label}")) .arco-select`,

  // 表格
  table: '.arco-table',
  tableRow: (text: string) => `.arco-table-tr:has-text("${text}")`,
  tableCell: (row: string, col: number) =>
    `.arco-table-tr:has-text("${row}") .arco-table-td:nth-child(${col})`,

  // 弹窗
  modal: '.arco-modal',
  modalTitle: '.arco-modal-title',
  modalConfirm: '.arco-modal-footer button.arco-btn-primary',
  modalCancel: '.arco-modal-footer button:not(.arco-btn-primary)',

  // 抽屉
  drawer: '.arco-drawer',
  drawerTitle: '.arco-drawer-title',

  // 消息提示
  messageSuccess: '.arco-message-success',
  messageError: '.arco-message-error',
  messageWarning: '.arco-message-warning',

  // 菜单
  menuItem: (text: string) => `.arco-menu-item:has-text("${text}")`,
  subMenu: (text: string) => `.arco-menu-inline-header:has-text("${text}")`,

  // 按钮
  btnPrimary: '.arco-btn-primary',
  btnSecondary: '.arco-btn-secondary',

  // 标签页
  tabPane: (text: string) => `.arco-tabs-tab:has-text("${text}")`,
} as const
