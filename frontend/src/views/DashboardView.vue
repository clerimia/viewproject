<script setup lang="ts">
import { ref, onMounted, onUnmounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { fetchDashboard } from '@/api/dashboard'
import type { DashboardData } from '@/types'
import * as echarts from 'echarts'

const router = useRouter()
const auth = useAuthStore()

const data = ref<DashboardData | null>(null)
const loading = ref(true)

let loadingRequest = false
let hotQaChart: echarts.ECharts | null = null
let trendChart: echarts.ECharts | null = null
let attractionChart: echarts.ECharts | null = null
let refreshTimer: ReturnType<typeof setInterval> | null = null

async function loadData() {
  if (loadingRequest) return
  loadingRequest = true
  try {
    data.value = await fetchDashboard()
  } catch (e) {
    console.warn('Dashboard load failed', e)
  } finally {
    loading.value = false
    loadingRequest = false
    await nextTick()
    renderCharts()
  }
}

function renderCharts() {
  if (!data.value) return

  // Hot QA Top 10
  const hotQaEl = document.getElementById('hotQaChart')
  if (hotQaEl) {
    if (!hotQaChart) hotQaChart = echarts.init(hotQaEl)
    hotQaChart.setOption({
      title: { text: '热门问答 Top10', left: 'center', textStyle: { color: '#e2e8f0', fontSize: 14 } },
      tooltip: {},
      xAxis: {
        type: 'category',
        data: data.value.hotQaTop10?.map((i) => i.question?.substring(0, 8) + '...') || [],
        axisLabel: { color: '#94a3b8', rotate: 30 },
      },
      yAxis: { type: 'value', axisLabel: { color: '#94a3b8' } },
      series: [
        {
          type: 'bar',
          data: data.value.hotQaTop10?.map((i) => i.count) || [],
          itemStyle: { color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: '#40a36f' },
            { offset: 1, color: '#0f3d2e' },
          ]) },
        },
      ],
    })
  }

  // Satisfaction trend
  const trendEl = document.getElementById('trendChart')
  if (trendEl) {
    if (!trendChart) trendChart = echarts.init(trendEl)
    trendChart.setOption({
      title: { text: '满意度趋势', left: 'center', textStyle: { color: '#e2e8f0', fontSize: 14 } },
      tooltip: { trigger: 'axis' },
      xAxis: {
        type: 'category',
        data: data.value.satisfactionTrend?.map((i) => i.date) || [],
        axisLabel: { color: '#94a3b8' },
      },
      yAxis: {
        type: 'value',
        min: 0,
        max: 5,
        axisLabel: { color: '#94a3b8' },
      },
      series: [
        {
          type: 'line',
          data: data.value.satisfactionTrend?.map((i) => i.avgScore) || [],
          smooth: true,
          lineStyle: { color: '#d9a441', width: 3 },
          areaStyle: { color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(217,164,65,0.4)' },
            { offset: 1, color: 'rgba(217,164,65,0.05)' },
          ]) },
          itemStyle: { color: '#d9a441' },
        },
      ],
    })
  }

  // Attraction distribution
  const attrEl = document.getElementById('attractionChart')
  if (attrEl) {
    if (!attractionChart) attractionChart = echarts.init(attrEl)
    attractionChart.setOption({
      title: { text: '景点关注度', left: 'center', textStyle: { color: '#e2e8f0', fontSize: 14 } },
      tooltip: { trigger: 'item' },
      series: [
        {
          type: 'pie',
          radius: ['35%', '65%'],
          data: data.value.attractionDistribution?.map((i: any) => ({
            name: i.name,
            value: i.count ?? i.score ?? 0,
          })) || [],
          label: { color: '#e2e8f0' },
          itemStyle: {
            borderRadius: 6,
            borderColor: '#1e293b',
            borderWidth: 2,
          },
        },
      ],
    })
  }
}

function logout() {
  auth.logout()
  router.push({ name: 'login' })
}

function handleResize() {
  hotQaChart?.resize()
  trendChart?.resize()
  attractionChart?.resize()
}

onMounted(() => {
  loadData()
  refreshTimer = setInterval(loadData, 30000)
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  if (refreshTimer) clearInterval(refreshTimer)
  window.removeEventListener('resize', handleResize)
  hotQaChart?.dispose()
  trendChart?.dispose()
  attractionChart?.dispose()
})
</script>

<template>
  <div class="h-full bg-slate-900 text-white overflow-y-auto">
    <!-- Header -->
    <div class="flex items-center justify-between px-6 py-4 border-b border-slate-700">
      <h1 class="display-font text-2xl font-bold">📊 云隐山 AI 数字人数据大屏</h1>
      <div class="flex items-center gap-3">
        <a
          href="/"
          class="flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-sm text-slate-400 hover:text-white hover:bg-slate-800 transition"
        >
          <span>🏞️</span>
          <span>返回导览</span>
        </a>
        <button
          @click="logout"
          class="flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-sm text-slate-400 hover:text-red-400 hover:bg-slate-800 transition"
        >
          <span>🚪</span>
          <span>退出</span>
        </button>
      </div>
    </div>

    <div v-if="loading" class="flex flex-col items-center justify-center h-64 gap-4">
      <div class="w-10 h-10 border-4 border-emerald-200 border-t-emerald-700 rounded-full animate-spin"></div>
      <span class="text-sm text-emerald-600">正在加载服务数据...</span>
    </div>

    <div v-else class="p-6 space-y-6">
      <!-- KPI cards -->
      <div class="grid grid-cols-2 md:grid-cols-4 gap-4">
        <div class="bg-slate-800 rounded-2xl p-5 border border-slate-700 text-center">
          <div class="text-3xl font-bold text-emerald-400">{{ data?.todayVisitors ?? 0 }}</div>
          <div class="text-sm text-slate-400 mt-1">今日服务人次</div>
        </div>
        <div class="bg-slate-800 rounded-2xl p-5 border border-slate-700 text-center">
          <div class="text-3xl font-bold text-emerald-400">{{ data?.weekVisitors ?? 0 }}</div>
          <div class="text-sm text-slate-400 mt-1">本周服务人次</div>
        </div>
        <div class="bg-slate-800 rounded-2xl p-5 border border-slate-700 text-center">
          <div class="text-3xl font-bold text-amber-400">
            {{ data?.satisfactionTrend?.length ? data.satisfactionTrend[data.satisfactionTrend.length - 1].avgScore?.toFixed(1) : '-' }}
          </div>
          <div class="text-sm text-slate-400 mt-1">平均满意度</div>
        </div>
        <div class="bg-slate-800 rounded-2xl p-5 border border-slate-700 text-center">
          <div class="text-3xl font-bold text-amber-400">
            {{ data?.satisfactionTrend?.length ? (data.satisfactionTrend[data.satisfactionTrend.length - 1].positiveRatio * 100).toFixed(0) + '%' : '-' }}
          </div>
          <div class="text-sm text-slate-400 mt-1">好评率</div>
        </div>
      </div>

      <!-- Charts -->
      <div class="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <div class="bg-slate-800 rounded-2xl p-5 border border-slate-700">
          <div id="hotQaChart" class="w-full h-72"></div>
        </div>
        <div class="bg-slate-800 rounded-2xl p-5 border border-slate-700">
          <div id="trendChart" class="w-full h-72"></div>
        </div>
      </div>

      <div class="bg-slate-800 rounded-2xl p-5 border border-slate-700">
        <div id="attractionChart" class="w-full h-80"></div>
      </div>
    </div>
  </div>
</template>
