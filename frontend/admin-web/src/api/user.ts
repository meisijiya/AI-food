import request from './request'

export function listUsers(params: any) {
  return request.get('/users', { params })
}

export function updateRole(id: number, role: string) {
  return request.patch(`/users/${id}/role`, { role })
}

export function disableUser(id: number) {
  return request.post(`/users/${id}/disable`)
}

export function enableUser(id: number) {
  return request.post(`/users/${id}/enable`)
}