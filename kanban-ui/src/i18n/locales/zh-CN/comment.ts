/**
 * 评论模块中文语言包
 */
export default {
  // 评论面板
  empty: '暂无评论',
  loadMore: '加载更多',
  loadError: '加载评论失败',

  // 评论编辑器
  placeholder: '写下你的评论...',
  send: '发送',
  supportMarkdown: '支持 Markdown 格式',
  emptyContent: '评论内容不能为空',
  imageUploadError: '图片上传失败',
  orgOrMemberNotFound: '未找到组织或成员信息，请重新登录',
  replyingTo: '回复 {name}',

  // 评论操作
  reply: '回复',
  withdraw: '撤回',
  delete: '删除',
  edit: '编辑',

  // 评论状态
  withdrawnMessage: '该评论已撤回',
  edited: '(已编辑)',
  anonymous: '匿名用户',
  replyTo: '回复',
  deleted: '已删除',

  // 操作来源
  operationSource: {
    bizRule: '通过业务规则「{ruleName}」',
    apiCall: '通过 {appName}',
  },

  // 操作结果
  createSuccess: '评论发送成功',
  createError: '评论发送失败',
  withdrawSuccess: '评论已撤回',
  withdrawError: '撤回失败，可能已超过撤回时间',
  deleteSuccess: '评论已删除',
  deleteError: '删除评论失败',
  updateSuccess: '评论更新成功',
  updateError: '评论更新失败',
}
