import request from './request'
import type { LoginRequest, LoginResponse } from '@/types'

export async function fetchCaptcha(): Promise<{ uuid: string; blob: Blob }> {
  const res = await request.get('/api/auth/captcha', { responseType: 'blob' })
  const uuid = res.headers['x-captcha-uuid'] || ''
  return { uuid, blob: res.data as Blob }
}

export async function login(data: LoginRequest): Promise<LoginResponse> {
  const res = await request.post<LoginResponse>('/api/auth/login', data)
  return res.data
}
