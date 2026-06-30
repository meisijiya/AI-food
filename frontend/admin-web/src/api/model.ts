import request from './request'

export function listModels() {
  return request.get('/models')
}