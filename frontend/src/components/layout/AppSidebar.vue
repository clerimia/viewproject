<script setup lang="ts">
import type { InterestMode, AvatarStatus, DigitalHumanConfig } from '@/types'
import AvatarCard from '@/components/digitalHuman/AvatarCard.vue'

defineProps<{
  config: DigitalHumanConfig | null
  guideName: string
  guideWelcome: string
  avatarStatus: AvatarStatus
  avatarStatusText: string
  interestModes: InterestMode[]
  selectedMode: string
  userId: string
  role: string
  mouthLevel?: number
}>()

defineEmits<{
  (e: 'update:selectedMode', value: string): void
  (e: 'logout'): void
}>()
</script>

<template>
  <div class="w-72 mountain-panel text-white flex-col hidden md:flex rounded-[1.75rem] overflow-hidden shadow-2xl shadow-emerald-950/20">
    <!-- Brand -->
    <div class="p-5 border-b border-white/10">
      <div class="display-font font-bold text-2xl flex items-center gap-2">🏞️ 云隐山导览</div>
      <div class="text-xs text-emerald-100/70 mt-1">AI Scenic Guide</div>
    </div>

    <!-- Digital human card -->
    <div class="p-4">
      <div class="scenic-stage rounded-3xl p-4 text-emerald-950 border border-white/20">
        <div class="flex items-center gap-3 relative z-10">
          <AvatarCard :status="avatarStatus" :status-text="avatarStatusText" :config="config" :mouth-level="mouthLevel" />
          <div>
            <div class="display-font text-xl font-bold">{{ guideName }}</div>
            <div class="text-xs text-emerald-800 mt-1">{{ avatarStatusText }}</div>
            <div v-if="avatarStatus === 'speaking'" class="voice-bars mt-2">
              <span></span><span></span><span></span>
            </div>
          </div>
        </div>
        <p class="relative z-10 text-xs leading-relaxed text-emerald-900/80 mt-4">{{ guideWelcome }}</p>
      </div>
    </div>

    <!-- Interest modes -->
    <div class="flex-1 overflow-y-auto p-4 space-y-3">
      <div class="text-xs text-emerald-100/60 px-1 py-1 uppercase font-semibold tracking-[0.25em]">游览兴趣</div>
      <button
        v-for="mode in interestModes"
        :key="mode.value"
        @click="$emit('update:selectedMode', mode.value)"
        :class="[
          'w-full text-left px-4 py-3 rounded-2xl transition flex items-center gap-3 border',
          selectedMode === mode.value
            ? 'bg-white text-emerald-950 border-white shadow-lg shadow-black/10'
            : 'bg-white/5 border-white/10 hover:bg-white/10 text-emerald-50',
        ]"
      >
        <span class="text-xl">{{ mode.icon }}</span>
        <span>
          <span class="block font-medium">{{ mode.label }}</span>
          <span class="block text-xs opacity-70">{{ mode.desc }}</span>
        </span>
      </button>
    </div>

    <!-- Footer -->
    <div class="p-4 border-t border-white/10 space-y-3">
      <div class="text-xs text-emerald-100/60">当前为游客导览端，如需管理后台请退出后选择“管理端”登录。</div>
      <div class="flex items-center gap-2 pt-2">
        <div class="w-8 h-8 rounded-full bg-white/10 flex items-center justify-center text-sm">
          {{ role === 'ADMIN' ? '管' : '游' }}
        </div>
        <div class="text-sm truncate">
          {{ role === 'ADMIN' ? '管理员' : '游客' }} ID: {{ userId }}
        </div>
      </div>
      <button @click="$emit('logout')" class="w-full text-sm text-emerald-100 hover:text-white hover:underline text-left">
        退出登录
      </button>
    </div>
  </div>
</template>
