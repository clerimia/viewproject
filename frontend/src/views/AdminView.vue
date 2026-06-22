<script setup lang="ts">
import { ref, onMounted, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { listDocuments, deleteDocument, uploadKnowledge, ingestText } from '@/api/knowledge'
import {
  listDigitalHumans,
  createDigitalHuman,
  updateDigitalHuman,
  deleteDigitalHuman,
  activateDigitalHuman,
} from '@/api/digitalHuman'
import { fetchSentimentReport } from '@/api/dashboard'
import type { KnowledgeDocument, DigitalHumanConfig } from '@/types'
import * as echarts from 'echarts'

const router = useRouter()
const auth = useAuthStore()
const fileInput = ref<HTMLInputElement | null>(null)

const activeTab = ref<'knowledge' | 'digitalHuman' | 'sentiment'>('knowledge')

// --- Knowledge ---
const docs = ref<KnowledgeDocument[]>([])
const docKeyword = ref('')
const docLoading = ref(false)
const uploadForm = reactive({ sourceId: '', title: '', category: '' })
const ingestForm = reactive({ text: '', title: '', category: '', sourceId: '' })
const uploadFile = ref<File | null>(null)
const uploadMsg = ref('')

async function loadDocs() {
  docLoading.value = true
  try {
    const data = await listDocuments(docKeyword.value || undefined)
    docs.value = data?.data?.content || data?.data || []
  } catch (e: any) {
    console.warn('文档列表加载失败', e)
  } finally {
    docLoading.value = false
  }
}

async function handleDeleteDoc(id: number) {
  if (!confirm('确认删除该文档？')) return
  try {
    await deleteDocument(id)
    await loadDocs()
  } catch (e: any) {
    alert('删除失败: ' + e.message)
  }
}

async function handleUpload() {
  if (!uploadFile.value || !uploadForm.sourceId) {
    uploadMsg.value = '请填写 Source ID 并选择文件'
    return
  }
  try {
    await uploadKnowledge(uploadFile.value, uploadForm.sourceId, uploadForm.title, uploadForm.category)
    uploadMsg.value = '上传成功'
    uploadFile.value = null
    uploadForm.sourceId = ''
    uploadForm.title = ''
    uploadForm.category = ''
    await loadDocs()
  } catch (e: any) {
    uploadMsg.value = '上传失败: ' + (e.response?.data?.message || e.message)
  }
}

async function handleIngest() {
  if (!ingestForm.text || !ingestForm.sourceId) {
    uploadMsg.value = '请填写 Source ID 和文本内容'
    return
  }
  try {
    await ingestText(ingestForm)
    uploadMsg.value = '导入成功'
    ingestForm.text = ''
    ingestForm.sourceId = ''
    ingestForm.title = ''
    ingestForm.category = ''
    await loadDocs()
  } catch (e: any) {
    uploadMsg.value = '导入失败: ' + (e.response?.data?.message || e.message)
  }
}

// --- Digital Human ---
const dhList = ref<DigitalHumanConfig[]>([])
const dhLoading = ref(false)
const editingDh = reactive<Partial<DigitalHumanConfig>>({
  name: '',
  avatarType: '2D',
  avatarStyle: 'scenic-guide',
  avatarGender: 'female',
  avatarImageUrl: '',
  clothingStyle: 'traditional_hanfu',
  clothingColor: '#2f7d4a',
  voiceType: 'edge_tts',
  voiceName: 'zh-CN-XiaoxiaoNeural',
  speechSpeed: 1,
  speechPitch: 1,
  speechVolume: 1,
  welcomeMessage: '',
  greetingMessage: '',
  idleAnimation: 'float',
  backgroundType: 'color',
  backgroundColor: '#edf7ef',
  isActive: false,
})
const editingId = ref<number | null>(null)

async function loadDhList() {
  dhLoading.value = true
  try {
    dhList.value = await listDigitalHumans()
  } catch (e) {
    console.warn('数字人配置加载失败', e)
  } finally {
    dhLoading.value = false
  }
}

function editDh(dh: DigitalHumanConfig) {
  editingId.value = dh.id
  Object.assign(editingDh, dh)
}

function cancelEditDh() {
  editingId.value = null
  Object.keys(editingDh).forEach((k) => (editingDh as any)[k] = '')
  editingDh.avatarType = '2D'
  editingDh.avatarStyle = 'scenic-guide'
  editingDh.avatarGender = 'female'
  editingDh.clothingStyle = 'traditional_hanfu'
  editingDh.clothingColor = '#2f7d4a'
  editingDh.voiceType = 'edge_tts'
  editingDh.voiceName = 'zh-CN-XiaoxiaoNeural'
  editingDh.speechSpeed = 1
  editingDh.speechPitch = 1
  editingDh.speechVolume = 1
  editingDh.idleAnimation = 'float'
  editingDh.backgroundType = 'color'
  editingDh.backgroundColor = '#edf7ef'
  editingDh.isActive = false
}

async function saveDh() {
  try {
    if (editingId.value) {
      await updateDigitalHuman(editingId.value, editingDh)
    } else {
      await createDigitalHuman(editingDh)
    }
    cancelEditDh()
    await loadDhList()
  } catch (e: any) {
    alert('保存失败: ' + (e.response?.data?.message || e.message))
  }
}

async function deleteDhItem(id: number) {
  if (!confirm('确认删除该配置？')) return
  try {
    await deleteDigitalHuman(id)
    await loadDhList()
  } catch (e: any) {
    alert('删除失败: ' + (e.response?.data?.message || e.message))
  }
}

async function activateDhItem(id: number) {
  try {
    await activateDigitalHuman(id)
    await loadDhList()
  } catch (e: any) {
    alert('启用失败: ' + (e.response?.data?.message || e.message))
  }
}

function logout() {
  auth.logout()
  router.push({ name: 'login' })
}

// --- Sentiment Report ---
const sentimentData = ref<any>(null)
const sentimentLoading = ref(false)
const sentimentDays = ref(7)
let sentimentChart: echarts.ECharts | null = null
let focusChart: echarts.ECharts | null = null

async function loadSentiment() {
  sentimentLoading.value = true
  try {
    sentimentData.value = await fetchSentimentReport(sentimentDays.value)
    renderSentimentCharts()
  } catch (e) {
    console.warn('情感报告加载失败', e)
  } finally {
    sentimentLoading.value = false
  }
}

function renderSentimentCharts() {
  if (!sentimentData.value) return
  const s = sentimentData.value

  // Sentiment pie chart
  const sentEl = document.getElementById('sentimentPieChart')
  if (sentEl) {
    if (!sentimentChart) sentimentChart = echarts.init(sentEl)
    sentimentChart.setOption({
      title: { text: '情感分布', left: 'center', textStyle: { fontSize: 14 } },
      tooltip: { trigger: 'item' },
      series: [{
        type: 'pie',
        radius: ['35%', '60%'],
        data: [
          { name: '正面', value: s.sentiment?.positive || 0, itemStyle: { color: '#40a36f' } },
          { name: '中性', value: s.sentiment?.neutral || 0, itemStyle: { color: '#d9a441' } },
          { name: '负面', value: s.sentiment?.negative || 0, itemStyle: { color: '#dc2626' } },
        ],
        label: { formatter: '{b}: {c} ({d}%)' },
      }],
    })
  }

  // Focus clusters bar chart
  const focusEl = document.getElementById('focusClustersChart')
  if (focusEl && s.focusClusters?.length) {
    if (!focusChart) focusChart = echarts.init(focusEl)
    const top10 = s.focusClusters.slice(0, 10)
    focusChart.setOption({
      title: { text: '游客关注点 Top10', left: 'center', textStyle: { fontSize: 14 } },
      tooltip: {},
      xAxis: { type: 'category', data: top10.map((c: any) => c.keyword), axisLabel: { rotate: 30 } },
      yAxis: { type: 'value' },
      series: [{
        type: 'bar',
        data: top10.map((c: any) => c.count),
        itemStyle: { color: '#40a36f', borderRadius: [4, 4, 0, 0] },
      }],
    })
  }
}

onMounted(() => {
  loadDocs()
  loadDhList()
})
</script>

<template>
  <div class="h-full flex">
    <!-- Sidebar -->
    <div class="w-64 mountain-panel text-white p-5 space-y-4 hidden md:block">
      <div class="display-font font-bold text-xl flex items-center gap-2">🏞️ 管理后台</div>
      <nav class="space-y-2 mt-6">
        <button
          @click="activeTab = 'knowledge'"
          :class="[
            'w-full text-left px-4 py-3 rounded-xl transition',
            activeTab === 'knowledge' ? 'bg-white/15 text-white' : 'text-emerald-100 hover:bg-white/10',
          ]"
        >
          📚 知识库管理
        </button>
        <button
          @click="activeTab = 'digitalHuman'"
          :class="[
            'w-full text-left px-4 py-3 rounded-xl transition',
            activeTab === 'digitalHuman' ? 'bg-white/15 text-white' : 'text-emerald-100 hover:bg-white/10',
          ]"
        >
          🤖 数字人配置
        </button>
        <button
          @click="activeTab = 'sentiment'; loadSentiment()"
          :class="[
            'w-full text-left px-4 py-3 rounded-xl transition',
            activeTab === 'sentiment' ? 'bg-white/15 text-white' : 'text-emerald-100 hover:bg-white/10',
          ]"
        >
          💬 情感报告
        </button>
      </nav>
      <div class="mt-auto pt-8 space-y-2">
        <a href="/" class="block text-sm text-emerald-100 hover:text-white hover:underline">← 返回导览</a>
        <a href="/dashboard" class="block text-sm text-emerald-100 hover:text-white hover:underline">📊 数据大屏</a>
        <button @click="logout" class="block text-sm text-emerald-100 hover:text-white hover:underline">退出登录</button>
      </div>
    </div>

    <!-- Main content -->
    <div class="flex-1 overflow-y-auto p-6 bg-stone-50">
      <!-- Knowledge tab -->
      <div v-if="activeTab === 'knowledge'" class="max-w-4xl mx-auto space-y-6">
        <h2 class="display-font text-2xl font-bold text-emerald-950">📚 知识库管理</h2>

        <!-- File upload -->
        <div class="bg-white rounded-2xl p-5 border border-emerald-100 shadow-sm space-y-4">
          <h3 class="font-semibold text-emerald-900">📤 上传文档</h3>
          <div class="grid grid-cols-1 md:grid-cols-2 gap-3">
            <div>
              <label class="block text-xs text-gray-500 mb-1">Source ID（必填）</label>
              <input v-model="uploadForm.sourceId" placeholder="如：yunyin-intro" class="w-full px-3 py-2 border border-emerald-100 rounded-xl text-sm" />
            </div>
            <div>
              <label class="block text-xs text-gray-500 mb-1">标题（可选）</label>
              <input v-model="uploadForm.title" placeholder="如：云隐山介绍" class="w-full px-3 py-2 border border-emerald-100 rounded-xl text-sm" />
            </div>
          </div>
          <div class="grid grid-cols-1 md:grid-cols-2 gap-3">
            <div>
              <label class="block text-xs text-gray-500 mb-1">分类（可选）</label>
              <select v-model="uploadForm.category" class="w-full px-3 py-2 border border-emerald-100 rounded-xl text-sm bg-white">
                <option value="">请选择分类</option>
                <option value="历史文化">历史文化</option>
                <option value="自然风光">自然风光</option>
                <option value="美食特产">美食特产</option>
                <option value="游览须知">游览须知</option>
                <option value="景区介绍">景区介绍</option>
              </select>
            </div>
          </div>
          <!-- File drop zone -->
          <div
            class="border-2 border-dashed border-emerald-200 rounded-xl p-6 text-center transition-colors"
            :class="uploadFile ? 'bg-emerald-50 border-emerald-400' : 'hover:border-emerald-400 cursor-pointer'"
            @click="fileInput?.click()"
          >
            <input
              ref="fileInput"
              type="file"
              class="hidden"
              accept=".txt,.md,.doc,.docx,.pdf"
              @change="(e: any) => { uploadFile = e.target.files?.[0] || null }"
            />
            <div v-if="uploadFile" class="flex items-center justify-center gap-2 text-emerald-700">
              <svg xmlns="http://www.w3.org/2000/svg" class="w-8 h-8" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
              </svg>
              <div>
                <div class="font-medium">{{ uploadFile.name }}</div>
                <div class="text-xs text-gray-500">{{ (uploadFile.size / 1024).toFixed(1) }} KB</div>
              </div>
              <button @click.stop="uploadFile = null" class="ml-2 text-red-500 hover:text-red-700">
                <svg xmlns="http://www.w3.org/2000/svg" class="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
                </svg>
              </button>
            </div>
            <div v-else class="text-gray-500">
              <svg xmlns="http://www.w3.org/2000/svg" class="w-10 h-10 mx-auto mb-2 text-emerald-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M7 16a4 4 0 01-.88-7.903A5 5 0 1115.9 6L16 6a5 5 0 011 9.9M15 13l-3-3m0 0l-3 3m3-3v12" />
              </svg>
              <div>点击选择文件或拖拽到此处</div>
              <div class="text-xs text-gray-400 mt-1">支持 txt, md, doc, docx, pdf</div>
            </div>
          </div>
          <button
            @click="handleUpload"
            :disabled="!uploadFile || !uploadForm.sourceId"
            class="w-full py-3 bg-emerald-700 text-white rounded-xl font-medium hover:bg-emerald-800 transition disabled:bg-gray-300 disabled:cursor-not-allowed"
          >
            {{ uploadFile ? '上传文档' : '请先选择文件' }}
          </button>
          <p v-if="uploadMsg" class="text-sm text-center" :class="uploadMsg.includes('成功') ? 'text-emerald-600' : 'text-red-600'">
            {{ uploadMsg }}
          </p>
        </div>

        <!-- Text ingest -->
        <div class="bg-white rounded-2xl p-5 border border-emerald-100 shadow-sm space-y-4">
          <h3 class="font-semibold text-emerald-900">📝 文本导入</h3>
          <div class="grid grid-cols-1 md:grid-cols-3 gap-3">
            <input v-model="ingestForm.sourceId" placeholder="Source ID *" class="px-3 py-2 border border-emerald-100 rounded-xl text-sm" />
            <input v-model="ingestForm.title" placeholder="标题" class="px-3 py-2 border border-emerald-100 rounded-xl text-sm" />
            <input v-model="ingestForm.category" placeholder="分类" class="px-3 py-2 border border-emerald-100 rounded-xl text-sm" />
          </div>
          <textarea v-model="ingestForm.text" rows="4" placeholder="粘贴知识文本..." class="w-full px-3 py-2 border border-emerald-100 rounded-xl text-sm"></textarea>
          <button @click="handleIngest" class="px-4 py-2 bg-emerald-700 text-white rounded-xl text-sm hover:bg-emerald-800 transition">
            导入
          </button>
        </div>

        <!-- Document list -->
        <div class="bg-white rounded-2xl p-5 border border-emerald-100 shadow-sm">
          <div class="flex items-center gap-3 mb-4">
            <input v-model="docKeyword" placeholder="搜索文档..." class="flex-1 px-3 py-2 border border-emerald-100 rounded-xl text-sm" />
            <button @click="loadDocs" class="px-4 py-2 bg-emerald-700 text-white rounded-xl text-sm hover:bg-emerald-800 transition">
              搜索
            </button>
          </div>
          <div v-if="docLoading" class="text-center py-8 text-emerald-600">加载中...</div>
          <div v-else-if="docs.length === 0" class="text-center py-8 text-gray-400">暂无文档</div>
          <div v-else class="space-y-3">
            <div
              v-for="doc in docs"
              :key="doc.id"
              class="flex items-center justify-between p-3 bg-emerald-50 rounded-xl border border-emerald-100"
            >
              <div>
                <div class="font-medium text-sm">{{ doc.title || doc.fileName || '未命名' }}</div>
                <div class="text-xs text-gray-500 mt-1">
                  {{ doc.category || '未分类' }} · {{ doc.sourceId }} · {{ doc.status }}
                </div>
              </div>
              <button @click="handleDeleteDoc(doc.id)" class="text-red-500 text-sm hover:underline">删除</button>
            </div>
          </div>
        </div>
      </div>

      <!-- Digital Human tab -->
      <div v-if="activeTab === 'digitalHuman'" class="max-w-4xl mx-auto space-y-6">
        <h2 class="display-font text-2xl font-bold text-emerald-950">🤖 数字人配置</h2>

        <!-- Edit form -->
        <div class="bg-white rounded-2xl p-5 border border-emerald-100 shadow-sm space-y-4">
          <h3 class="font-semibold text-emerald-900">{{ editingId ? '编辑配置' : '新建配置' }}</h3>
          <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div>
              <label class="block text-xs text-gray-500 mb-1">名称</label>
              <input v-model="editingDh.name" placeholder="如：云隐山小云" class="w-full px-3 py-2 border border-emerald-100 rounded-xl text-sm" />
            </div>
            <div>
              <label class="block text-xs text-gray-500 mb-1">性别/形象</label>
              <select v-model="editingDh.avatarGender" class="w-full px-3 py-2 border border-emerald-100 rounded-xl text-sm bg-white">
                <option value="female">女导览员</option>
                <option value="male">男导览员</option>
                <option value="neutral">中性导览员</option>
              </select>
            </div>
            <div>
              <label class="block text-xs text-gray-500 mb-1">形象风格</label>
              <select v-model="editingDh.avatarStyle" class="w-full px-3 py-2 border border-emerald-100 rounded-xl text-sm bg-white">
                <option value="scenic-guide">景区导览</option>
                <option value="hanfu-guide">古风汉服</option>
                <option value="cartoon">亲和卡通</option>
                <option value="professional">专业讲解员</option>
              </select>
            </div>
            <div>
              <label class="block text-xs text-gray-500 mb-1">形象图片 URL（可选）</label>
              <input v-model="editingDh.avatarImageUrl" placeholder="留空使用内置 2D 数字人" class="w-full px-3 py-2 border border-emerald-100 rounded-xl text-sm" />
            </div>
            <div>
              <label class="block text-xs text-gray-500 mb-1">服装</label>
              <select v-model="editingDh.clothingStyle" class="w-full px-3 py-2 border border-emerald-100 rounded-xl text-sm bg-white">
                <option value="traditional_hanfu">传统汉服</option>
                <option value="scenic_uniform">景区制服</option>
                <option value="modern_formal">现代正装</option>
                <option value="outdoor">户外导览服</option>
              </select>
            </div>
            <div>
              <label class="block text-xs text-gray-500 mb-1">服装主色</label>
              <input v-model="editingDh.clothingColor" type="color" class="w-full h-10 px-2 py-1 border border-emerald-100 rounded-xl text-sm bg-white" />
            </div>
            <div>
              <label class="block text-xs text-gray-500 mb-1">声音</label>
              <select v-model="editingDh.voiceName" class="w-full px-3 py-2 border border-emerald-100 rounded-xl text-sm bg-white">
                <option value="zh-CN-XiaoxiaoNeural">晓晓-温柔女声</option>
                <option value="zh-CN-XiaohanNeural">晓涵-活泼女声</option>
                <option value="zh-CN-XiaochenNeural">晓辰-知性女声</option>
                <option value="zh-CN-YunxiNeural">云希-阳光男声</option>
                <option value="zh-CN-YunyangNeural">云扬-沉稳男声</option>
                <option value="zh-CN-YunxiaNeural">云夏-可爱男声</option>
              </select>
            </div>
            <div>
              <label class="block text-xs text-gray-500 mb-1">语速 {{ editingDh.speechSpeed }}x</label>
              <input v-model.number="editingDh.speechSpeed" type="range" min="0.6" max="1.6" step="0.1" class="w-full" />
            </div>
            <div>
              <label class="block text-xs text-gray-500 mb-1">音调 {{ editingDh.speechPitch }}x</label>
              <input v-model.number="editingDh.speechPitch" type="range" min="0.7" max="1.4" step="0.1" class="w-full" />
            </div>
            <div>
              <label class="block text-xs text-gray-500 mb-1">音量 {{ editingDh.speechVolume }}x</label>
              <input v-model.number="editingDh.speechVolume" type="range" min="0.5" max="1.5" step="0.1" class="w-full" />
            </div>
            <div>
              <label class="block text-xs text-gray-500 mb-1">背景色</label>
              <input v-model="editingDh.backgroundColor" type="color" class="w-full h-10 px-2 py-1 border border-emerald-100 rounded-xl text-sm bg-white" />
            </div>
            <div class="md:col-span-2">
              <label class="block text-xs text-gray-500 mb-1">欢迎语</label>
              <textarea v-model="editingDh.welcomeMessage" rows="2" class="w-full px-3 py-2 border border-emerald-100 rounded-xl text-sm"></textarea>
            </div>
            <div class="md:col-span-2">
              <label class="block text-xs text-gray-500 mb-1">问候语</label>
              <textarea v-model="editingDh.greetingMessage" rows="2" class="w-full px-3 py-2 border border-emerald-100 rounded-xl text-sm"></textarea>
            </div>
            <div class="flex items-center gap-2 pt-6">
              <input v-model="editingDh.isActive" type="checkbox" id="dh-active" class="rounded" />
              <label for="dh-active" class="text-sm">启用此配置</label>
            </div>
          </div>
          <div class="flex gap-3">
            <button @click="saveDh" class="px-4 py-2 bg-emerald-700 text-white rounded-xl text-sm hover:bg-emerald-800 transition">
              {{ editingId ? '保存' : '创建' }}
            </button>
            <button v-if="editingId" @click="cancelEditDh" class="px-4 py-2 bg-gray-200 text-gray-700 rounded-xl text-sm hover:bg-gray-300 transition">
              取消
            </button>
          </div>
        </div>

        <!-- Config list -->
        <div class="bg-white rounded-2xl p-5 border border-emerald-100 shadow-sm">
          <div v-if="dhLoading" class="text-center py-8 text-emerald-600">加载中...</div>
          <div v-else-if="dhList.length === 0" class="text-center py-8 text-gray-400">暂无配置</div>
          <div v-else class="space-y-3">
            <div
              v-for="dh in dhList"
              :key="dh.id"
              class="flex items-center justify-between p-4 bg-emerald-50 rounded-xl border border-emerald-100"
            >
              <div>
                <div class="font-medium text-sm flex items-center gap-2">
                  {{ dh.name }}
                  <span v-if="dh.isActive" class="px-2 py-0.5 bg-emerald-700 text-white text-xs rounded-full">已启用</span>
                </div>
                <div class="text-xs text-gray-500 mt-1">
                  {{ dh.avatarStyle || dh.avatarType }} · {{ dh.clothingStyle || '默认服装' }} · {{ dh.voiceName }} · 语速 {{ dh.speechSpeed }}x
                </div>
              </div>
              <div class="flex gap-2">
                <button @click="editDh(dh)" class="text-emerald-700 text-sm hover:underline">编辑</button>
                <button
                  v-if="!dh.isActive"
                  @click="activateDhItem(dh.id)"
                  class="text-emerald-700 text-sm hover:underline"
                >
                  启用
                </button>
                <button @click="deleteDhItem(dh.id)" class="text-red-500 text-sm hover:underline">删除</button>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- Sentiment Report tab -->
      <div v-if="activeTab === 'sentiment'" class="max-w-5xl mx-auto space-y-6">
        <div class="flex items-center justify-between">
          <h2 class="display-font text-2xl font-bold text-emerald-950">💬 游客感受度报告</h2>
          <div class="flex items-center gap-2">
            <select v-model="sentimentDays" @change="loadSentiment()" class="px-3 py-2 border border-emerald-100 rounded-xl text-sm">
              <option :value="7">近 7 天</option>
              <option :value="14">近 14 天</option>
              <option :value="30">近 30 天</option>
            </select>
            <button @click="loadSentiment()" class="px-4 py-2 bg-emerald-700 text-white rounded-xl text-sm hover:bg-emerald-800 transition">
              刷新
            </button>
          </div>
        </div>

        <div v-if="sentimentLoading" class="text-center py-12 text-emerald-600">加载中...</div>

        <template v-else-if="sentimentData">
          <!-- KPI cards -->
          <div class="grid grid-cols-2 md:grid-cols-4 gap-4">
            <div class="bg-white rounded-2xl p-5 border border-emerald-100 shadow-sm text-center">
              <div class="text-3xl font-bold text-emerald-700">{{ sentimentData.sentiment?.positive || 0 }}</div>
              <div class="text-sm text-gray-500 mt-1">正面评价</div>
            </div>
            <div class="bg-white rounded-2xl p-5 border border-emerald-100 shadow-sm text-center">
              <div class="text-3xl font-bold text-amber-600">{{ sentimentData.sentiment?.neutral || 0 }}</div>
              <div class="text-sm text-gray-500 mt-1">中性评价</div>
            </div>
            <div class="bg-white rounded-2xl p-5 border border-emerald-100 shadow-sm text-center">
              <div class="text-3xl font-bold text-red-600">{{ sentimentData.sentiment?.negative || 0 }}</div>
              <div class="text-sm text-gray-500 mt-1">负面评价</div>
            </div>
            <div class="bg-white rounded-2xl p-5 border border-emerald-100 shadow-sm text-center">
              <div class="text-3xl font-bold text-emerald-700">
                {{ sentimentData.sentiment?.avgSentimentScore?.toFixed(2) || '-' }}
              </div>
              <div class="text-sm text-gray-500 mt-1">平均情感分</div>
            </div>
          </div>

          <!-- Charts -->
          <div class="grid grid-cols-1 lg:grid-cols-2 gap-6">
            <div class="bg-white rounded-2xl p-5 border border-emerald-100 shadow-sm">
              <div id="sentimentPieChart" class="w-full h-72"></div>
            </div>
            <div class="bg-white rounded-2xl p-5 border border-emerald-100 shadow-sm">
              <div id="focusClustersChart" class="w-full h-72"></div>
            </div>
          </div>

          <!-- Satisfaction trend -->
          <div v-if="sentimentData.satisfactionTrend?.length" class="bg-white rounded-2xl p-5 border border-emerald-100 shadow-sm">
            <h3 class="font-semibold text-emerald-900 mb-4">满意度趋势</h3>
            <div class="overflow-x-auto">
              <table class="w-full text-sm">
                <thead>
                  <tr class="border-b border-emerald-100">
                    <th class="text-left py-2 px-3">日期</th>
                    <th class="text-center py-2 px-3">交互次数</th>
                    <th class="text-center py-2 px-3">平均分</th>
                    <th class="text-center py-2 px-3">好评率</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="t in sentimentData.satisfactionTrend" :key="t.date" class="border-b border-emerald-50">
                    <td class="py-2 px-3">{{ t.date }}</td>
                    <td class="text-center py-2 px-3">{{ t.totalCount }}</td>
                    <td class="text-center py-2 px-3">{{ t.avgScore?.toFixed(1) }}</td>
                    <td class="text-center py-2 px-3">
                      <span :class="t.positiveRatio >= 0.6 ? 'text-emerald-700' : 'text-amber-600'">
                        {{ (t.positiveRatio * 100).toFixed(0) }}%
                      </span>
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>

          <!-- Service suggestions -->
          <div class="bg-white rounded-2xl p-5 border border-emerald-100 shadow-sm">
            <h3 class="font-semibold text-emerald-900 mb-3">📋 服务建议</h3>
            <div class="space-y-2 text-sm text-gray-700">
              <p v-if="(sentimentData.sentiment?.negative || 0) > (sentimentData.sentiment?.positive || 0)">
                ⚠️ 负面评价多于正面，建议重点排查高频投诉问题，优化讲解内容和服务流程。
              </p>
              <p v-else-if="(sentimentData.sentiment?.positive || 0) > 0">
                ✅ 整体评价良好，可继续优化高频关注点的讲解深度。
              </p>
              <p v-if="sentimentData.focusClusters?.length">
                🔍 游客最关注：<span class="font-medium">{{ sentimentData.focusClusters.slice(0, 5).map((c: any) => c.keyword).join('、') }}</span>，建议丰富相关知识库内容。
              </p>
              <p v-if="sentimentData.attractionScores?.length">
                🏞️ 热门景点：<span class="font-medium">{{ sentimentData.attractionScores.slice(0, 3).map((a: any) => a.attraction).join('、') }}</span>，可增加互动讲解和路线推荐。
              </p>
            </div>
          </div>
        </template>
      </div>
    </div>
  </div>
</template>
