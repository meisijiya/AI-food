import request from './request'

export function listAuditLogs(params: any) {
  return request.get('/audit-logs', { params })
}