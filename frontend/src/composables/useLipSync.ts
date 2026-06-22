import { ref, onUnmounted } from 'vue'

export function useLipSync() {
  const mouthLevel = ref(0) // 0 = closed, 1 = wide open
  let audioContext: AudioContext | null = null
  let analyser: AnalyserNode | null = null
  let source: MediaElementAudioSourceNode | null = null
  let animFrameId: number | null = null
  let currentAudio: HTMLAudioElement | null = null

  function startLipSync(audio: HTMLAudioElement) {
    stopLipSync()
    currentAudio = audio

    try {
      audioContext = new AudioContext()
      analyser = audioContext.createAnalyser()
      analyser.fftSize = 256
      analyser.smoothingTimeConstant = 0.4

      source = audioContext.createMediaElementSource(audio)
      source.connect(analyser)
      analyser.connect(audioContext.destination)

      const dataArray = new Uint8Array(analyser.frequencyBinCount)

      const update = () => {
        if (!analyser) return
        analyser.getByteFrequencyData(dataArray)

        // Calculate RMS amplitude from low frequencies (voice range 80-1000Hz)
        const sampleRate = audioContext?.sampleRate || 44100
        const binWidth = sampleRate / analyser.fftSize
        const lowBin = Math.floor(80 / binWidth)
        const highBin = Math.floor(1000 / binWidth)

        let sum = 0
        let count = 0
        for (let i = lowBin; i <= highBin && i < dataArray.length; i++) {
          sum += dataArray[i]
          count++
        }
        const avg = count > 0 ? sum / count : 0

        // Normalize to 0-1, with some amplification for visibility
        mouthLevel.value = Math.min(1, (avg / 128) * 1.5)

        animFrameId = requestAnimationFrame(update)
      }

      // Resume audio context if suspended (browser autoplay policy)
      if (audioContext.state === 'suspended') {
        audioContext.resume()
      }

      update()
    } catch (e) {
      console.warn('LipSync init error', e)
      // Fallback: simple timer-based mouth animation
      startFallbackAnimation()
    }
  }

  function startFallbackAnimation() {
    const update = () => {
      mouthLevel.value = 0.3 + Math.random() * 0.5
      animFrameId = requestAnimationFrame(update)
    }
    update()
  }

  function stopLipSync() {
    if (animFrameId) {
      cancelAnimationFrame(animFrameId)
      animFrameId = null
    }
    if (source) {
      try { source.disconnect() } catch {}
      source = null
    }
    if (audioContext) {
      try { audioContext.close() } catch {}
      audioContext = null
    }
    analyser = null
    currentAudio = null
    mouthLevel.value = 0
  }

  onUnmounted(stopLipSync)

  return { mouthLevel, startLipSync, stopLipSync }
}
