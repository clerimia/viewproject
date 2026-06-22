<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { fetchCaptcha, login } from '@/api/auth'

const router = useRouter()
const auth = useAuthStore()

const loginForm = ref({ username: '', password: '', code: '', uuid: '', loginRole: 'USER' as 'USER' | 'ADMIN' })
const captchaBlobUrl = ref('')
const errorMsg = ref('')
const isLoading = ref(false)

let captchaUuid = ''

async function refreshCaptcha() {
  try {
    const { uuid, blob } = await fetchCaptcha()
    captchaUuid = uuid
    loginForm.value.uuid = uuid
    if (captchaBlobUrl.value) URL.revokeObjectURL(captchaBlobUrl.value)
    captchaBlobUrl.value = URL.createObjectURL(blob)
  } catch (e) {
    console.error('验证码获取失败', e)
  }
}

async function handleLogin() {
  isLoading.value = true
  errorMsg.value = ''
  try {
    const data = await login(loginForm.value)
    if (!data.success) {
      errorMsg.value = data.message || '登录失败'
      refreshCaptcha()
      return
    }
    auth.setAuth(data.token || '', String(data.userId || ''))
    if (loginForm.value.loginRole === 'ADMIN') {
      router.push({ name: 'admin' })
    } else {
      router.push({ name: 'chat' })
    }
  } catch (e: any) {
    errorMsg.value = e.response?.data?.message || e.message || '登录失败'
    refreshCaptcha()
  } finally {
    isLoading.value = false
  }
}

onMounted(refreshCaptcha)
onUnmounted(() => {
  if (captchaBlobUrl.value) URL.revokeObjectURL(captchaBlobUrl.value)
})
</script>

<template>
  <div class="h-screen flex items-center justify-center p-4">
    <div class="paper-card p-8 rounded-[2rem] w-full max-w-md border border-white/70 relative overflow-hidden">
      <div class="absolute -top-16 -right-12 w-40 h-40 rounded-full bg-emerald-200/40 blur-2xl"></div>
      <div class="relative text-center mb-7">
        <div class="mx-auto mb-4 guide-avatar idle text-5xl">🏞️</div>
        <p class="text-xs tracking-[0.35em] text-emerald-700 uppercase">YUNYIN MOUNTAIN</p>
        <h1 class="display-font text-3xl font-bold text-emerald-950 mt-2">云隐山智能导览</h1>
        <p class="text-sm text-emerald-800/70 mt-2">登录后体验 AI 数字导游、语音讲解与个性化路线推荐</p>
      </div>

      <form @submit.prevent="handleLogin" class="space-y-4 relative">
        <div>
          <label class="block text-sm font-medium text-emerald-900 mb-2">登录入口</label>
          <div class="grid grid-cols-2 gap-3">
            <button
              type="button"
              @click="loginForm.loginRole = 'USER'"
              :class="[
                'p-3 rounded-2xl border text-left transition',
                loginForm.loginRole === 'USER'
                  ? 'bg-emerald-700 text-white border-emerald-700 shadow-lg shadow-emerald-900/15'
                  : 'bg-white/80 text-emerald-900 border-emerald-100 hover:bg-emerald-50',
              ]"
            >
              <div class="text-lg">🧳 游客端</div>
              <div class="text-xs opacity-75 mt-1">AI 导览 / 语音问答</div>
            </button>
            <button
              type="button"
              @click="loginForm.loginRole = 'ADMIN'"
              :class="[
                'p-3 rounded-2xl border text-left transition',
                loginForm.loginRole === 'ADMIN'
                  ? 'bg-emerald-700 text-white border-emerald-700 shadow-lg shadow-emerald-900/15'
                  : 'bg-white/80 text-emerald-900 border-emerald-100 hover:bg-emerald-50',
              ]"
            >
              <div class="text-lg">🛠️ 管理端</div>
              <div class="text-xs opacity-75 mt-1">知识库 / 数字人 / 大屏</div>
            </button>
          </div>
        </div>

        <div>
          <label class="block text-sm font-medium text-emerald-900 mb-1">用户名</label>
          <input
            v-model="loginForm.username"
            type="text"
            required
            class="w-full px-4 py-2.5 border border-emerald-100 rounded-xl bg-white/80 focus:ring-2 focus:ring-emerald-500 outline-none transition"
          />
        </div>

        <div>
          <label class="block text-sm font-medium text-emerald-900 mb-1">密码</label>
          <input
            v-model="loginForm.password"
            type="password"
            required
            class="w-full px-4 py-2.5 border border-emerald-100 rounded-xl bg-white/80 focus:ring-2 focus:ring-emerald-500 outline-none transition"
          />
        </div>

        <div>
          <label class="block text-sm font-medium text-emerald-900 mb-1">验证码</label>
          <div class="flex gap-2">
            <input
              v-model="loginForm.code"
              type="text"
              required
              placeholder="请输入右侧验证码"
              class="flex-1 px-4 py-2.5 border border-emerald-100 rounded-xl bg-white/80 focus:ring-2 focus:ring-emerald-500 outline-none transition"
            />
            <div
              class="relative w-32 h-11 bg-emerald-50 rounded-xl cursor-pointer overflow-hidden border border-emerald-100"
              @click="refreshCaptcha"
            >
              <img v-if="captchaBlobUrl" :src="captchaBlobUrl" class="w-full h-full object-cover" alt="captcha" />
              <div v-else class="absolute inset-0 flex items-center justify-center text-xs text-emerald-700">加载中...</div>
            </div>
          </div>
        </div>

        <div v-if="errorMsg" class="text-red-600 text-sm text-center bg-red-50 p-2 rounded-xl">
          {{ errorMsg }}
        </div>

        <button
          type="submit"
          :disabled="isLoading"
          class="w-full bg-emerald-700 hover:bg-emerald-800 text-white font-medium py-3 rounded-xl transition disabled:opacity-50 shadow-lg shadow-emerald-900/15"
        >
          {{ isLoading ? '登录中...' : (loginForm.loginRole === 'ADMIN' ? '进入管理后台' : '进入游客导览') }}
        </button>

        <p class="text-center text-sm text-emerald-800/70">
          没有账号？
          <router-link to="/register" class="text-emerald-700 font-medium hover:underline">注册一个</router-link>
        </p>
      </form>
    </div>
  </div>
</template>
