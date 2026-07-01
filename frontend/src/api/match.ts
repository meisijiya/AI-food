// 匹配相关接口
import { request } from './http'

export const matchApi = {
  getRandomMatch: (excludeIds?: number[]) =>
    request('get', '/bloom/random-match', undefined, {
      params: excludeIds?.length ? { excludeIds } : {}
    })
}
