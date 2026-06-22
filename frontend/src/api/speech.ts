import request from './request'

export async function synthesizeSpeech(text: string, voice?: string, rate?: string, pitch?: string, volume?: string): Promise<Blob> {
  const res = await request.post(
    '/api/speech/synthesize',
    { text: text.substring(0, 500), voice, rate, pitch, volume },
    { responseType: 'blob' }
  )
  return res.data as Blob
}

export async function recognizeSpeech(file: File): Promise<string> {
  const formData = new FormData()
  formData.append('file', file)
  const res = await request.post('/api/speech/recognize', formData)
  return res.data?.text || ''
}
