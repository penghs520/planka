import request from './request'
import type {
  FileDTO,
  FileCategory,
  PresignedUploadRequest,
  PresignedUploadResponse,
  DownloadUrlResponse,
  UploadProgress,
} from '@/types/oss'

const BASE_URL = '/api/v1/files'

/**
 * OSS 文件服务 API
 */
export const ossApi = {
  /**
   * 上传文件
   * @param file 文件
   * @param orgId 组织 ID
   * @param operatorId 操作者 ID（当前用户在当前组织对应的成员卡 ID，即当前成员 ID，不是用户 ID）
   * @param category 文件类别
   * @param onProgress 上传进度回调
   */
  upload(
    file: File,
    orgId: string,
    operatorId: string,
    category: FileCategory,
    onProgress?: (progress: UploadProgress) => void
  ): Promise<FileDTO> {
    const formData = new FormData()
    formData.append('file', file)
    formData.append('orgId', orgId)
    formData.append('operatorId', operatorId)
    formData.append('category', category)

    return request.post(`${BASE_URL}/upload`, formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
      onUploadProgress: (progressEvent) => {
        if (onProgress && progressEvent.total) {
          onProgress({
            loaded: progressEvent.loaded,
            total: progressEvent.total,
            percent: Math.round((progressEvent.loaded * 100) / progressEvent.total),
          })
        }
      },
    })
  },

  /**
   * 批量上传文件
   * @param files 文件列表
   * @param orgId 组织 ID
   * @param operatorId 操作者 ID（当前用户在当前组织对应的成员卡 ID，即当前成员 ID，不是用户 ID）
   * @param category 文件类别
   */
  uploadBatch(
    files: File[],
    orgId: string,
    operatorId: string,
    category: FileCategory
  ): Promise<FileDTO[]> {
    const formData = new FormData()
    files.forEach((file) => {
      formData.append('files', file)
    })
    formData.append('orgId', orgId)
    formData.append('operatorId', operatorId)
    formData.append('category', category)

    return request.post(`${BASE_URL}/upload/batch`, formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
    })
  },

  /**
   * 获取预签名上传 URL
   */
  getPresignedUploadUrl(data: PresignedUploadRequest): Promise<PresignedUploadResponse> {
    return request.post(`${BASE_URL}/presigned-upload-url`, data)
  },

  /**
   * 获取文件信息
   */
  getFile(fileId: string): Promise<FileDTO> {
    return request.get(`${BASE_URL}/${fileId}`)
  },

  /**
   * 获取下载 URL
   */
  getDownloadUrl(fileId: string, expirationSeconds = 3600): Promise<DownloadUrlResponse> {
    return request.get(`${BASE_URL}/${fileId}/download-url`, {
      params: { expirationSeconds },
    })
  },

  /**
   * 删除文件
   */
  delete(fileId: string): Promise<boolean> {
    return request.delete(`${BASE_URL}/${fileId}`)
  },

  /**
   * 通过预签名 URL 上传文件
   */
  async uploadWithPresignedUrl(
    uploadUrl: string,
    file: File,
    onProgress?: (progress: UploadProgress) => void
  ): Promise<void> {
    return new Promise((resolve, reject) => {
      const xhr = new XMLHttpRequest()
      xhr.open('PUT', uploadUrl)
      xhr.setRequestHeader('Content-Type', file.type)

      xhr.upload.onprogress = (event) => {
        if (onProgress && event.lengthComputable) {
          onProgress({
            loaded: event.loaded,
            total: event.total,
            percent: Math.round((event.loaded * 100) / event.total),
          })
        }
      }

      xhr.onload = () => {
        if (xhr.status >= 200 && xhr.status < 300) {
          resolve()
        } else {
          reject(new Error(`Upload failed with status ${xhr.status}`))
        }
      }

      xhr.onerror = () => {
        reject(new Error('Upload failed'))
      }

      xhr.send(file)
    })
  },
}
