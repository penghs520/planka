/**
 * 认证模块 - 中文语言包
 */
export default {
  // 登录页
  login: {
    welcome: '欢迎登录',
    brandTitle: '高效协作 敏捷交付',
    brandSubtitle: '让团队专注于创造价值',
    email: '邮箱',
    emailPlaceholder: '请输入邮箱',
    emailRequired: '请输入邮箱',
    emailInvalid: '请输入正确的邮箱格式',
    password: '密码',
    passwordPlaceholder: '请输入密码',
    passwordRequired: '请输入密码',
    loginButton: '登录',
    loginSuccess: '登录成功',
  },

  // 选择组织页
  selectOrg: {
    title: '选择组织',
    selectPrompt: '请选择要进入的组织',
    noOrg: '您还没有加入任何组织',
    enterSuccess: '已进入 {name}',
    enterFailed: '进入组织失败',
    createOrg: '创建新组织',
    createSuccess: '组织创建成功',
    logout: '退出登录',
  },

  // 修改密码弹窗
  changePassword: {
    title: '修改密码',
    subtitle: '您正在使用默认密码登录，请先修改密码以保障账号安全',
    oldPassword: '原密码',
    oldPasswordPlaceholder: '请输入原密码（默认密码）',
    oldPasswordRequired: '请输入原密码',
    newPassword: '新密码',
    newPasswordPlaceholder: '请输入新密码（6-32位）',
    newPasswordRequired: '请输入新密码',
    newPasswordMinLength: '密码长度至少6位',
    newPasswordMaxLength: '密码长度最多32位',
    confirmPassword: '确认密码',
    confirmPasswordPlaceholder: '请再次输入新密码',
    confirmPasswordRequired: '请确认密码',
    passwordMismatch: '两次输入的密码不一致',
    cancel: '取消',
    submit: '修改密码',
    changeSuccess: '密码修改成功',
    changeFailed: '密码修改失败',
  },
}
