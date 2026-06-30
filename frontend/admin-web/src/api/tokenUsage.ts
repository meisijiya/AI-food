import request from './request'

export function getStats(params: any) {
  return request.get('/token-usage/stats', { params })
}