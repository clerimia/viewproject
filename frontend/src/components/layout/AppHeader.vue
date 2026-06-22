<script setup lang="ts">
defineProps<{
  interestLabel: string
  isConnected: boolean
}>()
</script>

<template>
  <div class="min-h-16 border-b border-emerald-100/70 flex items-center px-6 justify-between bg-white/65 z-10 backdrop-blur-xl">
    <div>
      <div class="display-font font-bold text-xl text-emerald-950">云隐山智能导览</div>
      <div class="text-xs text-emerald-700 mt-0.5">
        当前兴趣：{{ interestLabel }} · 支持语音输入、RAG 参考资料溯源
      </div>
    </div>
    <div class="text-sm text-gray-500">
      <span v-if="isConnected" class="text-emerald-600">● 已连接</span>
      <span v-else class="text-red-500">● 未连接</span>
    </div>
  </div>
</template>
