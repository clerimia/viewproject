import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
// Simple JWT decode (extract payload without verification)
function parseJwt(token: string): Record<string, any> {
  try {
    const base64Url = token.split('.')[1]
    const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/')
    const jsonPayload = decodeURIComponent(
      atob(base64)
        .split('')
        .map((c) => '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2))
        .join('')
    )
    return JSON.parse(jsonPayload)
  } catch {
    return {}
  }
}

export const useAuthStore = defineStore('auth', () => {
  const token = ref(localStorage.getItem('token') || '')
  const userId = ref(localStorage.getItem('userId') || '')

  const isLoggedIn = computed(() => !!token.value)

  const role = computed(() => {
    if (!token.value) return ''
    const payload = parseJwt(token.value)
    return payload.role || ''
  })

  function setAuth(newToken: string, newUserId: string) {
    token.value = newToken
    userId.value = newUserId
    localStorage.setItem('token', newToken)
    localStorage.setItem('userId', newUserId)
  }

  function logout() {
    token.value = ''
    userId.value = ''
    localStorage.removeItem('token')
    localStorage.removeItem('userId')
  }

  return { token, userId, isLoggedIn, role, setAuth, logout }
})
