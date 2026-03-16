/**
 * 文件类别枚举
 */
export enum FileCategory {
  /** 卡片附件 */
  ATTACHMENT = 'ATTACHMENT',
  /** 用户头像 */
  AVATAR = 'AVATAR',
  /** 组织 Logo */
  ORG_LOGO = 'ORG_LOGO',
  /** 评论图片 */
  COMMENT_IMAGE = 'COMMENT_IMAGE',
  /** 描述图片 */
  DESCRIPTION_IMAGE = 'DESCRIPTION_IMAGE',
}

/**
 * 文件信息
 */
export interface FileDTO {
  id: string
  orgId: string
  /** 操作者 ID（当前用户在当前组织对应的成员卡 ID，即当前成员 ID，不是用户 ID） */
  operatorId: string
  category: FileCategory
  originalName: string
  url: string
  size: number
  contentType: string
  createdAt: string
}

/**
 * 预签名上传请求
 */
export interface PresignedUploadRequest {
  orgId: string
  /** 操作者 ID（当前用户在当前组织对应的成员卡 ID，即当前成员 ID，不是用户 ID） */
  operatorId: string
  category: FileCategory
  fileName: string
  contentType: string
  expirationSeconds?: number
}

/**
 * 预签名上传响应
 */
export interface PresignedUploadResponse {
  uploadUrl: string
  objectKey: string
  expiresIn: number
}

/**
 * 下载 URL 响应
 */
export interface DownloadUrlResponse {
  url: string
  expiresIn: number
}

/**
 * 上传进度
 */
export interface UploadProgress {
  loaded: number
  total: number
  percent: number
}
