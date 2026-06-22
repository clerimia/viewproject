import request from './request'
import type { DigitalHumanConfig } from '@/types'

export async function fetchActiveDigitalHuman(): Promise<DigitalHumanConfig | null> {
  const res = await request.get('/api/digital-human/active')
  if (res.data?.success && res.data?.data) return res.data.data
  return null
}

// Admin APIs
export async function listDigitalHumans(): Promise<DigitalHumanConfig[]> {
  const res = await request.get('/api/admin/digital-human')
  return res.data?.data || []
}

export async function getDigitalHuman(id: number): Promise<DigitalHumanConfig> {
  const res = await request.get(`/api/admin/digital-human/${id}`)
  return res.data?.data
}

export async function createDigitalHuman(data: Partial<DigitalHumanConfig>) {
  const res = await request.post('/api/admin/digital-human', data)
  return res.data
}

export async function updateDigitalHuman(id: number, data: Partial<DigitalHumanConfig>) {
  const res = await request.put(`/api/admin/digital-human/${id}`, data)
  return res.data
}

export async function deleteDigitalHuman(id: number) {
  const res = await request.delete(`/api/admin/digital-human/${id}`)
  return res.data
}

export async function activateDigitalHuman(id: number) {
  const res = await request.post(`/api/admin/digital-human/${id}/activate`)
  return res.data
}
