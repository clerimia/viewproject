<script setup lang="ts">
import type { Reference } from '@/types'

defineProps<{ references: Reference[] }>()
</script>

<template>
  <details
    v-if="references.length"
    class="bg-amber-50 border border-amber-200 rounded-2xl text-xs text-amber-950 overflow-hidden"
  >
    <summary class="cursor-pointer px-3 py-2 font-medium select-none">
      📚 参考资料 {{ references.length }} 条（点击展开）
    </summary>
    <div class="px-3 pb-3 space-y-2">
      <div
        v-for="ref in references"
        :key="ref.id"
        class="bg-white/80 rounded-xl border border-amber-100 p-3"
      >
        <div class="font-semibold text-gray-800">[{{ ref.id }}] {{ ref.title || '未命名资料' }}</div>
        <div class="text-gray-500 mt-1">{{ ref.category || '未分类' }} · 相关度 {{ ref.score }}</div>
        <div class="text-gray-700 mt-2 whitespace-pre-wrap leading-relaxed">{{ ref.snippet }}</div>
      </div>
    </div>
  </details>
</template>
