import request from './request'

export function listRecommendations(params: any) {
  return request.get('/recommendations', { params })
}