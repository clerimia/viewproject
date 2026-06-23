export interface LoginRequest {
  username: string
  password: string
  code: string
  uuid: string
  loginRole?: 'USER' | 'ADMIN'
}

export interface LoginResponse {
  success: boolean
  message?: string
  token?: string
  userId?: number
  role?: 'USER' | 'ADMIN'
}

export interface ChatMessage {
  role: 'user' | 'assistant'
  content: string
  rawContent?: string
  thinkingContent?: string
  references: Reference[]
  interactionLogId: number | null
  satisfaction: number | null
  feedbackSubmitting: boolean
  feedbackMessage: string
  steps: AgentStep[]
  isTyping: boolean
}

export interface Reference {
  id: number
  title: string
  category: string
  score: number
  snippet: string
}

export interface AgentStep {
  type: 'THINKING' | 'EXECUTING' | 'ERROR'
  step?: number
  tool?: string
  content?: string
}

export interface DigitalHumanConfig {
  id: number
  name: string
  description?: string
  avatarType: string
  avatarStyle: string
  avatarGender?: 'male' | 'female' | 'neutral' | string
  avatarImageUrl: string
  clothingStyle?: string
  clothingColor?: string
  clothing?: string
  voiceType?: string
  voiceName: string
  speechSpeed: number
  speechPitch?: number
  speechVolume?: number
  pitch?: number
  language?: string
  welcomeMessage: string
  greetingMessage: string
  idleAnimation?: string
  backgroundUrl?: string
  backgroundType?: string
  backgroundColor?: string
  backgroundTheme?: string
  isActive: boolean
  isDefault?: boolean
}

export interface KnowledgeDocument {
  id: number
  sourceId: string
  ragDocumentId: string
  title: string
  category: string
  fileName: string
  contentType: string
  chunkCount: number
  status: string
  createdAt: string
}

export interface DashboardData {
  todayVisitors: number
  weekVisitors: number
  hotQaTop10: HotQaItem[]
  satisfactionTrend: TrendPoint[]
  attractionDistribution: AttractionItem[]
}

export interface HotQaItem {
  question: string
  count: number
}

export interface TrendPoint {
  date: string
  avgScore: number
  totalCount: number
  positiveRatio: number
}

export interface AttractionItem {
  name: string
  count: number
}

export interface InterestMode {
  value: string
  label: string
  icon: string
  desc: string
}

export type AvatarStatus = 'idle' | 'listening' | 'thinking' | 'speaking'
