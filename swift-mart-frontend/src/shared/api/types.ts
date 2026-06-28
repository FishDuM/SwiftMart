export type ApiResponse<T> = {
  success: boolean
  message?: string
  errorCode?: string
  data: T
}

export class BizError extends Error {
  readonly code?: string

  constructor(message: string, code?: string) {
    super(message)
    this.name = 'BizError'
    this.code = code
  }
}