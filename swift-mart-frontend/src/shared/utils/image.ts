/**
 * 商品图兜底：图片 onerror 时切换到占位图，避免出现破图图标
 */
export const FALLBACK_GOODS_IMG =
  'data:image/svg+xml;utf8,<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 200 200"><rect width="200" height="200" fill="%23f5f5f7"/><g fill="%23c7c7cc" font-family="PingFang SC,sans-serif" font-size="14" text-anchor="middle"><text x="100" y="95">商品图片</text><text x="100" y="115">加载失败</text></g></svg>'

export const handleImageError = (event: Event, fallback: string = FALLBACK_GOODS_IMG) => {
  const target = event.target as HTMLImageElement | null
  if (!target) return
  if (target.dataset.fallback === 'true') return
  target.dataset.fallback = 'true'
  target.src = fallback
}