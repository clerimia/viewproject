import request from './request'
import type { KnowledgeDocument } from '@/types'

export async function uploadKnowledge(file: File, sourceId: string, title?: string, category?: string) {
  const formData = new FormData()
  formData.append('file', file)
  formData.append('sourceId', sourceId)
  if (title) formData.append('title', title)
  if (category) formData.append('category', category)
  const res = await request.post('/api/admin/knowledge/upload', formData)
  return res.data
}

export async function ingestText(data: {
  text: string
  title: string
  category: string
  sourceId: string
  chunkSize?: number
  chunkOverlap?: number
}) {
  const res = await request.post('/api/admin/knowledge/ingest', data)
  return res.data
}

export async function listDocuments(keyword?: string, page = 0, size = 20) {
  const res = await request.get('/api/admin/knowledge/documents', {
    params: { keyword, page, size },
  })
  return res.data
}

export async function deleteDocument(id: number) {
  const res = await request.delete(`/api/admin/knowledge/documents/${id}`)
  return res.data
}
