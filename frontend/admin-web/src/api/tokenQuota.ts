// Token 限额管理 API：全局默认 + 单用户覆盖
import request from './request'

export const tokenQuotaApi = {
  /** 获取全局默认 Token 限额 */
  getGlobalTokenLimit: () => request.get('/config/token-limit'),

  /** 更新全局默认 Token 限额 */
  updateGlobalTokenLimit: (value: number) =>
    request.put('/config/token-limit', { value }),

  /** 查询单用户 Token 覆盖 */
  getUserTokenQuota: (userId: number) =>
    request.get(`/token-quota/${userId}`),

  /** 设置单用户 Token 覆盖 */
  updateUserTokenQuota: (userId: number, dailyTokenLimit: number) =>
    request.put(`/token-quota/${userId}`, { dailyTokenLimit }),

  /** 删除单用户 Token 覆盖 */
  deleteUserTokenQuota: (userId: number) =>
    request.delete(`/token-quota/${userId}`)
}
