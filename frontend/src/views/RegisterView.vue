<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import request from '@/api/request'

const router = useRouter()
const form = ref({ username: '', password: '', confirmPassword: '' })
const isLoading = ref(false)
const errorMsg = ref('')
const successMsg = ref('')

async function handleRegister() {
  errorMsg.value = ''
  successMsg.value = ''

  if (!form.value.username || !form.value.password) {
    errorMsg.value = '请填写用户名和密码'
    return
  }
  if (form.value.password !== form.value.confirmPassword) {
    errorMsg.value = '两次密码输入不一致'
    return
  }
  if (form.value.password.length < 6) {
    errorMsg.value = '密码至少 6 位'
    return
  }

  isLoading.value = true
  try {
    const res = await request.post('/api/auth/register', {
      username: form.value.username,
      password: form.value.password,
    })
    if (!res.data?.success) {
      errorMsg.value = res.data?.message || '注册失败'
      return
    }
    successMsg.value = '注册成功，即将跳转到登录页...'
    setTimeout(() => router.push({ name: 'login' }), 1500)
  } catch (e: any) {
    errorMsg.value = e.response?.data?.message || e.message || '注册失败'
  } finally {
    isLoading.value = false
  }
}
</script>

<template>
  <div class="h-screen flex items-center justify-center p-4">
    <div class="paper-card p-8 rounded-[2rem] w-full max-w-md border border-white/70 relative overflow-hidden">
      <div class="absolute -top-16 -right-12 w-40 h-40 rounded-full bg-emerald-200/40 blur-2xl"></div>
      <div class="relative text-center mb-7">
        <div class="mx-auto mb-4 guide-avatar idle text-5xl">🏞️</div>
        <p class="text-xs tracking-[0.35em] text-emerald-700 uppercase">YUNYIN MOUNTAIN</p>
        <h1 class="display-font text-3xl font-bold text-emerald-950 mt-2">注册游客账号</h1>
        <p class="text-sm text-emerald-800/70 mt-2">注册入口仅创建游客账号，用于体验 AI 数字导游</p>
      </div>

      <form @submit.prevent="handleRegister" class="space-y-4 relative">
        <div>
          <label class="block text-sm font-medium text-emerald-900 mb-1">用户名</label>
          <input
            v-model="form.username"
            type="text"
            required
            placeholder="请输入用户名"
            class="w-full px-4 py-2.5 border border-emerald-100 rounded-xl bg-white/80 focus:ring-2 focus:ring-emerald-500 outline-none transition"
          />
        </div>

        <div>
          <label class="block text-sm font-medium text-emerald-900 mb-1">密码</label>
          <input
            v-model="form.password"
            type="password"
            required
            placeholder="至少 6 位"
            class="w-full px-4 py-2.5 border border-emerald-100 rounded-xl bg-white/80 focus:ring-2 focus:ring-emerald-500 outline-none transition"
          />
        </div>

        <div>
          <label class="block text-sm font-medium text-emerald-900 mb-1">确认密码</label>
          <input
            v-model="form.confirmPassword"
            type="password"
            required
            placeholder="再次输入密码"
            class="w-full px-4 py-2.5 border border-emerald-100 rounded-xl bg-white/80 focus:ring-2 focus:ring-emerald-500 outline-none transition"
          />
        </div>

        <div v-if="errorMsg" class="text-red-600 text-sm text-center bg-red-50 p-2 rounded-xl">
          {{ errorMsg }}
        </div>
        <div v-if="successMsg" class="text-emerald-700 text-sm text-center bg-emerald-50 p-2 rounded-xl">
          {{ successMsg }}
        </div>

        <button
          type="submit"
          :disabled="isLoading"
          class="w-full bg-emerald-700 hover:bg-emerald-800 text-white font-medium py-3 rounded-xl transition disabled:opacity-50 shadow-lg shadow-emerald-900/15"
        >
          {{ isLoading ? '注册中...' : '注册' }}
        </button>

        <p class="text-center text-sm text-emerald-800/70">
          已有账号？
          <router-link to="/login" class="text-emerald-700 font-medium hover:underline">去登录</router-link>
        </p>
      </form>
    </div>
  </div>
</template>
