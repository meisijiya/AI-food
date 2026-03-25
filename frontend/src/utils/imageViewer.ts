import { showImagePreview as vantPreview } from 'vant'

/**
 * 缩略图 URL 转原图 URL
 * 例: /uploads/avatars/abc_thumb.jpg → /uploads/avatars/abc.jpg
 */
export function toOriginalUrl(url: string): string {
  if (!url) return url
  return url.replace('_thumb.', '.')
}

/**
 * 点击缩略图展示原图
 */
export function showOriginalImage(thumbnailUrl: string) {
  if (!thumbnailUrl) return
  const original = toOriginalUrl(thumbnailUrl)
  vantPreview({
    images: [original],
    closeable: true,
    showIndex: false
  })
}
