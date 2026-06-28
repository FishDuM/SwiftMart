/**
 * 全局通用格式化函数
 */

const PRICE_FORMATTER = new Intl.NumberFormat('zh-CN', {
  minimumFractionDigits: 2,
  maximumFractionDigits: 2,
})

export const formatPrice = (value: number | string | null | undefined): string => {
  if (value === null || value === undefined || value === '') return '¥0.00'
  const num = Number(value)
  if (Number.isNaN(num)) return '¥0.00'
  return `¥${PRICE_FORMATTER.format(num)}`
}

export const formatPriceSimple = (value: number | string | null | undefined): string => {
  if (value === null || value === undefined || value === '') return '0.00'
  const num = Number(value)
  if (Number.isNaN(num)) return '0.00'
  return PRICE_FORMATTER.format(num)
}

export const formatMobile = (mobile: string): string => {
  if (!mobile || mobile.length !== 11) return mobile
  return `${mobile.slice(0, 3)} ${mobile.slice(3, 7)} ${mobile.slice(7)}`
}

export const formatCountdown = (totalSeconds: number): string => {
  if (totalSeconds <= 0) return '00:00:00'
  const hours = Math.floor(totalSeconds / 3600)
  const minutes = Math.floor((totalSeconds % 3600) / 60)
  const seconds = Math.floor(totalSeconds % 60)
  return [hours, minutes, seconds]
    .map((n) => String(n).padStart(2, '0'))
    .join(':')
}

export const formatCountdownShort = (totalSeconds: number): string => {
  if (totalSeconds <= 0) return '00:00'
  const minutes = Math.floor(totalSeconds / 60)
  const seconds = Math.floor(totalSeconds % 60)
  return [minutes, seconds].map((n) => String(n).padStart(2, '0')).join(':')
}

export const formatPercent = (current: number, total: number): number => {
  if (!total) return 0
  const used = total - current
  return Math.max(0, Math.min(100, Math.round((used / total) * 100)))
}

export const truncate = (value: string, max = 20): string =>
  value.length > max ? `${value.slice(0, max)}…` : value