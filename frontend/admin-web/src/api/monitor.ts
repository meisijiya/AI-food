import request from './request'

export function getHealth() {
  return request.get('/monitor/health')
}

export function getJvm() {
  return request.get('/monitor/jvm')
}