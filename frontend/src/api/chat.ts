import { useAuthStore } from '@/stores/auth'

export interface ChatRequest {
  prompt: string
  mode: string
}

/**
 * Open an SSE stream for chat. Returns the raw Response so the caller
 * can read the body as a ReadableStream of SSE events.
 */
export async function streamChat(data: ChatRequest): Promise<Response> {
  const auth = useAuthStore()
  return fetch('/api/chat', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      Authorization: `Bearer ${auth.token}`,
    },
    body: JSON.stringify(data),
  })
}
