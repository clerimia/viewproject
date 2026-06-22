import request from './request'
import type { DashboardData } from '@/types'

export async function fetchDashboard(): Promise<DashboardData> {
  const res = await request.get('/api/admin/dashboard')
  return res.data
}

export async function fetchSentimentReport(days = 7) {
  const res = await request.get('/api/admin/report/sentiment', { params: { days } })
  return res.data
}

export async function submitSatisfaction(logId: number, score: number) {
  const res = await request.post(`/api/interactions/${logId}/satisfaction`, { score })
  return res.data
}
