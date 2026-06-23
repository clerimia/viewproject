import type { ChatMessage } from '@/types'
import { streamChat } from '@/api/chat'
import { ref, nextTick } from 'vue'

interface SseEvent {
  eventName: string
  data: string
}

function parseSseEvent(raw: string): SseEvent {
  if (!raw.trim()) return { eventName: 'message', data: '' }
  let eventName = 'message'
  const dataLines: string[] = []
  for (const line of raw.split(/\r?\n/)) {
    if (!line || line.startsWith(':')) continue
    if (line.startsWith('event:')) {
      eventName = line.substring(6).trim() || 'message'
    } else if (line.startsWith('data:')) {
      let d = line.substring(5)
      if (d.startsWith(' ')) d = d.substring(1)
      dataLines.push(d)
    }
  }
  return { eventName, data: dataLines.join('\n') }
}

function splitAssistantContent(raw: string): { thinking: string; answer: string } {
  const thinkingParts: string[] = []
  const answerParts: string[] = []
  const openTag = '<think>'
  const closeTag = '</think>'
  let cursor = 0

  while (cursor < raw.length) {
    const lower = raw.toLowerCase()
    const openIdx = lower.indexOf(openTag, cursor)
    if (openIdx < 0) {
      answerParts.push(raw.slice(cursor))
      break
    }

    answerParts.push(raw.slice(cursor, openIdx))
    const thinkStart = openIdx + openTag.length
    const closeIdx = lower.indexOf(closeTag, thinkStart)
    if (closeIdx < 0) {
      thinkingParts.push(raw.slice(thinkStart))
      break
    }

    thinkingParts.push(raw.slice(thinkStart, closeIdx))
    cursor = closeIdx + closeTag.length
  }

  return {
    thinking: thinkingParts.join('').trim(),
    answer: answerParts.join('').replace(/<\/?think>/gi, '').trimStart(),
  }
}

function handleEvent(raw: string, aiMsg: ChatMessage) {
  const { eventName, data } = parseSseEvent(raw)
  if (eventName === 'meta') {
    try {
      const meta = JSON.parse(data || '{}')
      aiMsg.interactionLogId = meta.interactionLogId || null
    } catch { /* ignore */ }
  } else if (eventName === 'references') {
    try {
      const refs = JSON.parse(data || '[]')
      aiMsg.references = Array.isArray(refs) ? refs : []
    } catch {
      aiMsg.references = []
    }
  } else if (eventName === 'message' || eventName === 'data') {
    aiMsg.rawContent = (aiMsg.rawContent || '') + data
    const parsed = splitAssistantContent(aiMsg.rawContent)
    aiMsg.thinkingContent = parsed.thinking
    aiMsg.content = parsed.answer
  } else if (eventName === 'error') {
    aiMsg.content += `\n[系统错误]: ${data}`
  }
}

export function useSSE() {
  const isGenerating = ref(false)

  async function sendChatMessage(
    prompt: string,
    mode: string,
    aiMsg: ChatMessage,
    onDone?: () => void
  ) {
    isGenerating.value = true
    try {
      const response = await streamChat({ prompt, mode })

      if (!response.ok) {
        const errText = await response.text()
        throw new Error(`请求失败: ${response.status} ${errText}`)
      }

      if (response.body && typeof response.body.getReader === 'function') {
        const reader = response.body.getReader()
        const decoder = new TextDecoder()
        let buffer = ''

        while (true) {
          const { done, value } = await reader.read()
          if (done) {
            const tail = decoder.decode()
            if (tail) buffer += tail
            break
          }
          buffer += decoder.decode(value, { stream: true })
          const parts = buffer.split(/\r?\n\r?\n/)
          buffer = parts.pop() || ''
          for (const part of parts) handleEvent(part, aiMsg)
          await nextTick()
        }
        if (buffer.trim()) handleEvent(buffer, aiMsg)
      } else {
        const text = await response.text()
        const parts = text.split(/\r?\n\r?\n/)
        for (const part of parts) {
          if (part.trim()) handleEvent(part, aiMsg)
        }
      }
    } catch (e: any) {
      aiMsg.content += `\n[系统错误]: ${e.message}`
    } finally {
      aiMsg.isTyping = false
      isGenerating.value = false
      onDone?.()
    }
  }

  return { isGenerating, sendChatMessage }
}
