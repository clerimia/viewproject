import { ref } from 'vue'
import { synthesizeSpeech, recognizeSpeech } from '@/api/speech'
import type { DigitalHumanConfig } from '@/types'

export function useSpeech(digitalHuman: () => DigitalHumanConfig | null) {
  const isSpeaking = ref<Record<number, boolean>>({})
  const isRecording = ref(false)
  const isRecognizing = ref(false)
  const supportsMic = ref(false)

  let mediaRecorder: MediaRecorder | null = null
  let audioChunks: Blob[] = []
  let recordingTimeout: ReturnType<typeof setTimeout> | null = null

  function checkMicSupport() {
    supportsMic.value = !!(navigator.mediaDevices && navigator.mediaDevices.getUserMedia)
  }

  function speechSpeedToRate(speed?: number): string {
    const numeric = Number(speed)
    if (!Number.isFinite(numeric) || numeric <= 0) return '+0%'
    const percent = Math.round((numeric - 1) * 100)
    return percent >= 0 ? `+${percent}%` : `${percent}%`
  }

  function stripMarkdown(text: string): string {
    return (text || '')
      .replace(/<think>[\s\S]*?<\/think>/gi, '')
      .replace(/<\/?think>/gi, '')
      .replace(/[#*_~`>|]{2,}/g, '')
      .replace(/(?:^|\n)\s*[-*+]\s+/g, '$1')
      .replace(/(?:^|\n)\s*\d+[.、]\s+/g, '$1')
      .replace(/\[([^\]]*)\]\([^)]+\)/g, '$1')
      .replace(/!\[([^\]]*)\]\([^)]+\)/g, '$1')
      .replace(/```[\s\S]*?```/g, '')
      .replace(/`([^`]+)`/g, '$1')
      .trim()
  }

  async function speakText(text: string, index: number) {
    if (isSpeaking.value[index]) return
    isSpeaking.value[index] = true
    let objectUrl = ''
    try {
      const dh = digitalHuman()
      const cleaned = stripMarkdown(text)
      if (!cleaned) {
        isSpeaking.value[index] = false
        return
      }
      const blob = await synthesizeSpeech(
        cleaned,
        dh?.voiceName,
        speechSpeedToRate(dh?.speechSpeed)
      )
      objectUrl = URL.createObjectURL(blob)
      const audio = new Audio(objectUrl)
      const cleanup = () => {
        isSpeaking.value[index] = false
        if (objectUrl) URL.revokeObjectURL(objectUrl)
      }
      audio.onended = cleanup
      audio.onerror = cleanup
      await audio.play()
    } catch (e) {
      console.warn('TTS error', e)
      isSpeaking.value[index] = false
      if (objectUrl) URL.revokeObjectURL(objectUrl)
    }
  }

  async function startRecording(onResult: (text: string) => void) {
    if (!supportsMic.value) return
    try {
      const stream = await navigator.mediaDevices.getUserMedia({ audio: true })
      let mimeType = 'audio/webm'
      if (!MediaRecorder.isTypeSupported(mimeType)) {
        mimeType = 'audio/mp4'
        if (!MediaRecorder.isTypeSupported(mimeType)) mimeType = ''
      }
      mediaRecorder = new MediaRecorder(stream, mimeType ? { mimeType } : {})
      audioChunks = []
      const capturedMime = mediaRecorder.mimeType || 'audio/webm'

      mediaRecorder.ondataavailable = (e) => {
        if (e.data?.size > 0) audioChunks.push(e.data)
      }

      mediaRecorder.onstop = async () => {
        stream.getTracks().forEach((t) => t.stop())
        if (audioChunks.length === 0) {
          isRecording.value = false
          return
        }
        const blob = new Blob(audioChunks, { type: capturedMime })
        const ext = capturedMime.includes('mp4') ? 'm4a' : 'webm'
        const file = new File([blob], 'recording.' + ext, { type: capturedMime })
        isRecognizing.value = true
        try {
          const text = await recognizeSpeech(file)
          if (text) onResult(text)
        } catch (e) {
          console.warn('ASR error', e)
        } finally {
          isRecognizing.value = false
        }
        isRecording.value = false
      }

      mediaRecorder.onerror = () => {
        stream.getTracks().forEach((t) => t.stop())
        isRecording.value = false
      }

      mediaRecorder.start(1000)
      isRecording.value = true
      recordingTimeout = setTimeout(() => {
        if (isRecording.value) stopRecording()
      }, 10000)
    } catch (e) {
      console.warn('麦克风权限被拒绝或不可用')
      isRecording.value = false
    }
  }

  function stopRecording() {
    if (recordingTimeout) {
      clearTimeout(recordingTimeout)
      recordingTimeout = null
    }
    if (mediaRecorder && mediaRecorder.state !== 'inactive') {
      mediaRecorder.stop()
    } else {
      isRecording.value = false
    }
  }

  function toggleRecording(onResult: (text: string) => void) {
    if (isRecording.value) stopRecording()
    else startRecording(onResult)
  }

  return {
    isSpeaking,
    isRecording,
    isRecognizing,
    supportsMic,
    checkMicSupport,
    speakText,
    toggleRecording,
    startRecording,
    stopRecording,
  }
}
