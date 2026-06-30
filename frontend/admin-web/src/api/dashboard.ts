import request from './request'

export function getSummary() {
  return request.get('/dashboard/summary')
}

export function getTrends(days: number) {
  return request.get(`/dashboard/trends?days=${days}`)
}