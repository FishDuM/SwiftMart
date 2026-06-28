/**
 * 全局 server clock 的便捷 composable 封装。
 * 大部分场景直接 `import { serverNow } from '@/shared/api/server-clock'` 即可，
 * 这个文件保留是为了对外提供统一的 composable 入口。
 */
import { serverNow } from '@/shared/api/server-clock'

export const useServerNow = () => serverNow

export { serverNow }