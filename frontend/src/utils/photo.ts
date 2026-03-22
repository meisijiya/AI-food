/**
 * 将相对照片路径转为完整 URL
 * 开发环境：Vite proxy 处理，直接用相对路径
 * 生产环境：拼接后端域名
 */
export function resolvePhotoUrl(path: string | null | undefined): string {
  if (!path) return ''
  if (path.startsWith('http')) return path
  const base = import.meta.env.VITE_BACKEND_URL || ''
  return base + path
}
