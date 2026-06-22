<script setup lang="ts">
import { computed } from 'vue'
import type { AvatarStatus, DigitalHumanConfig } from '@/types'

const props = defineProps<{
  status: AvatarStatus
  statusText: string
  config: DigitalHumanConfig | null
  size?: 'normal' | 'large'
  mouthLevel?: number // 0 = closed, 1 = wide open
}>()

const statusEmoji: Record<AvatarStatus, string> = {
  idle: '😊',
  listening: '👂',
  thinking: '🤔',
  speaking: '💬',
}

const gender = computed(() => props.config?.avatarGender || 'female')
const avatarStyle = computed(() => props.config?.avatarStyle || 'scenic-guide')
const clothingStyle = computed(() => props.config?.clothingStyle || props.config?.clothing || 'traditional_hanfu')
const clothingColor = computed(() => props.config?.clothingColor || '#2f7d4a')
const bgColor = computed(() => props.config?.backgroundColor || '#edf7ef')
const isLarge = computed(() => props.size === 'large')
const mouthHeight = computed(() => {
  if (props.status !== 'speaking') return isLarge.value ? 3 : 2
  return Math.max(isLarge.value ? 4 : 3, (props.mouthLevel || 0) * (isLarge.value ? 18 : 12))
})
const expression = computed(() => {
  if (props.status === 'listening') return { leftEye: '◉', rightEye: '◉', brow: 'raised' }
  if (props.status === 'thinking') return { leftEye: '•', rightEye: '•', brow: 'thinking' }
  if (props.status === 'speaking') return { leftEye: '◕', rightEye: '◕', brow: 'happy' }
  return { leftEye: '●', rightEye: '●', brow: 'normal' }
})
</script>

<template>
  <div class="relative inline-block">
    <!-- Main avatar -->
    <div
      :class="['guide-avatar flex-shrink-0 overflow-hidden', status, size === 'large' ? 'w-28 h-28 rounded-[2.5rem]' : 'w-[92px] h-[92px] rounded-[34px]']"
      :style="{ background: `linear-gradient(145deg, #fffaf0, ${bgColor})` }"
      :title="statusText"
    >
      <img v-if="config?.avatarImageUrl" :src="config.avatarImageUrl" alt="数字人形象" />
      <div v-else class="relative w-full h-full flex items-center justify-center">
        <!-- hair / head -->
        <div
          :class="[
            'absolute rounded-full transition-all duration-300',
            isLarge ? 'w-[70px] h-[70px] top-3' : 'w-[58px] h-[58px] top-2',
            gender === 'male' ? 'bg-amber-950' : gender === 'neutral' ? 'bg-stone-700' : 'bg-zinc-900',
            avatarStyle === 'hanfu-guide' ? 'shadow-[0_0_0_4px_rgba(217,164,65,.18)]' : '',
          ]"
        ></div>
        <div
          :class="['absolute rounded-full bg-[#ffd8bd] border border-amber-200', isLarge ? 'w-[58px] h-[58px] top-5' : 'w-[48px] h-[48px] top-4']"
        ></div>

        <!-- expression brows -->
        <div
          :class="[
            'absolute left-1/2 -translate-x-1/2 flex justify-between text-emerald-950 font-bold transition-all duration-200',
            isLarge ? 'w-10 top-[42px] text-sm' : 'w-8 top-[34px] text-xs',
            expression.brow === 'thinking' ? '-rotate-6' : expression.brow === 'raised' ? '-translate-y-1' : '',
          ]"
        >
          <span>{{ expression.brow === 'thinking' ? '╲' : '︵' }}</span>
          <span>{{ expression.brow === 'thinking' ? '╱' : '︵' }}</span>
        </div>

        <!-- eyes -->
        <div
          :class="['absolute left-1/2 -translate-x-1/2 flex justify-between text-emerald-950 transition-all duration-200', isLarge ? 'w-9 top-[52px] text-xs' : 'w-7 top-[42px] text-[10px]']"
        >
          <span>{{ expression.leftEye }}</span>
          <span>{{ expression.rightEye }}</span>
        </div>

        <!-- lip-sync mouth -->
        <div
          class="absolute left-1/2 -translate-x-1/2 rounded-full bg-rose-700 border border-rose-900 transition-all duration-75"
          :class="isLarge ? 'top-[68px]' : 'top-[55px]'"
          :style="{
            width: status === 'speaking' ? `${10 + (mouthLevel || 0) * 12}px` : '12px',
            height: `${mouthHeight}px`,
            opacity: status === 'thinking' ? 0.45 : 0.9,
          }"
        ></div>

        <!-- clothing -->
        <div
          class="absolute bottom-0 left-1/2 -translate-x-1/2 transition-colors duration-300"
          :class="[
            isLarge ? 'w-[82px] h-8 rounded-t-[28px]' : 'w-[68px] h-7 rounded-t-[24px]',
            clothingStyle === 'traditional_hanfu' || avatarStyle === 'hanfu-guide' ? 'border-t-4 border-amber-300' : '',
            clothingStyle === 'outdoor' ? 'border-t-4 border-sky-300' : '',
          ]"
          :style="{ backgroundColor: clothingColor }"
        >
          <div v-if="clothingStyle === 'traditional_hanfu' || avatarStyle === 'hanfu-guide'" class="absolute inset-x-4 top-1 h-5 border-l-2 border-r-2 border-white/70 rotate-6"></div>
          <div v-if="clothingStyle === 'scenic_uniform'" class="absolute left-1/2 top-1 -translate-x-1/2 w-5 h-3 rounded bg-white/80"></div>
        </div>

        <!-- voice energy bars while speaking -->
        <div v-if="status === 'speaking'" class="absolute right-2 top-3 voice-bars">
          <span :style="{ transform: `scaleY(${0.8 + (mouthLevel || 0) * 1.4})` }"></span>
          <span :style="{ transform: `scaleY(${0.6 + (mouthLevel || 0) * 1.8})` }"></span>
          <span :style="{ transform: `scaleY(${0.9 + (mouthLevel || 0) * 1.2})` }"></span>
        </div>
      </div>
    </div>

    <!-- Status emoji badge -->
    <div
      :class="[
        'absolute -bottom-1 -right-1 w-8 h-8 rounded-full flex items-center justify-center text-sm shadow-md border-2 border-white transition-all duration-300',
        status === 'idle' ? 'bg-emerald-100' :
        status === 'listening' ? 'bg-amber-100 animate-pulse' :
        status === 'thinking' ? 'bg-blue-100 animate-spin' :
        'bg-emerald-100'
      ]"
    >
      {{ statusEmoji[status] }}
    </div>

    <!-- Speaking ripple effect -->
    <template v-if="status === 'speaking'">
      <div class="absolute inset-0 rounded-[inherit] border-2 border-emerald-400/40 animate-ping pointer-events-none"></div>
    </template>

    <!-- Listening wave effect -->
    <template v-if="status === 'listening'">
      <div class="absolute -inset-2 rounded-[inherit] border-2 border-amber-400/30 animate-pulse pointer-events-none"></div>
    </template>
  </div>
</template>
