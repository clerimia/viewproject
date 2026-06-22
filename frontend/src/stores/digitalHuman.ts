import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { DigitalHumanConfig } from '@/types'
import { fetchActiveDigitalHuman } from '@/api/digitalHuman'

export const useDigitalHumanStore = defineStore('digitalHuman', () => {
  const config = ref<DigitalHumanConfig | null>(null)

  async function load() {
    try {
      const data = await fetchActiveDigitalHuman()
      if (data) config.value = data
    } catch (e) {
      console.warn('数字人配置加载失败', e)
    }
  }

  return { config, load }
})
