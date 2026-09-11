<script setup lang="ts">
import { computed, nextTick, onMounted, onUnmounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import {
  ArrowUpOutlined,
  CloudUploadOutlined,
  EditOutlined,
  ExportOutlined,
  HomeOutlined,
  PaperClipOutlined,
  ThunderboltOutlined,
} from '@ant-design/icons-vue'
import logo from '@/assets/logo.svg'
import myAxios from '@/request'
import { deployApp, getAppVoById } from '@/api/appController.ts'

const route = useRoute()
const router = useRouter()
// 应用 ID 全链路保持字符串，避免大整数转 Number 丢失精度
const appId = route.params.id as string

// 后端服务地址（与 request.ts 中的 baseURL 保持一致）
const API_BASE = (myAxios.defaults.baseURL as string) ?? ''

// ============ 应用信息 ============
const app = ref<API.AppVO>()
const appLoadFailed = ref(false)

// 生成网站的本地访问地址：http://localhost:8123/api/static/{codeGenType}_{appId}/
const previewUrl = computed(() => {
  if (!app.value?.codeGenType) return ''
  return `${API_BASE}/api/static/${app.value.codeGenType}_${appId}/`
})

// ============ 对话消息 ============
interface ChatMessage {
  role: 'user' | 'assistant'
  content: string
}

const messages = ref<ChatMessage[]>([])
const inputMessage = ref('')
const streaming = ref(false)
// 网页预览是否就绪（流式生成完成后展示）
const previewReady = ref(false)
// 用于强制刷新 iframe
const previewKey = ref(0)

const messageListRef = ref<HTMLElement>()
let abortController: AbortController | null = null

// 消息列表滚动到底部
const scrollToBottom = async () => {
  await nextTick()
  const el = messageListRef.value
  if (el) {
    el.scrollTop = el.scrollHeight
  }
}

/**
 * 发送消息并通过 SSE 流式接收 AI 回复
 * 接口：GET /app/chat/gen/code?appId=&message=（text/event-stream）
 */
const startChat = async (userMessage: string) => {
  // 追加用户消息和空的 AI 消息
  messages.value.push({ role: 'user', content: userMessage })
  const aiMessage: ChatMessage = reactive({ role: 'assistant', content: '' })
  messages.value.push(aiMessage)
  streaming.value = true
  await scrollToBottom()

  abortController = new AbortController()
  try {
    const url = `${API_BASE}/app/chat/gen/code?appId=${appId}&message=${encodeURIComponent(userMessage)}`
    const resp = await fetch(url, {
      method: 'GET',
      credentials: 'include',
      headers: {
        Accept: 'text/event-stream',
      },
      signal: abortController.signal,
    })
    if (!resp.ok || !resp.body) {
      throw new Error(`SSE 请求失败，状态码：${resp.status}`)
    }

    const reader = resp.body.getReader()
    const decoder = new TextDecoder('utf-8')
    let buffer = ''

    // 逐块读取 SSE 数据流
    while (true) {
      const { done, value } = await reader.read()
      if (done) break
      buffer += decoder.decode(value, { stream: true })
      // SSE 事件以空行分隔（兼容 \n\n 与 \r\n\r\n）
      const events = buffer.split(/\r?\n\r?\n/)
      buffer = events.pop() ?? ''
      for (const event of events) {
        // 按 SSE 规范，一个事件可含多行 data:，需将所有 data 行用换行拼接
        const dataLines = event
          .split(/\r?\n/)
          .filter((line) => line.startsWith('data:'))
        if (dataLines.length === 0) continue
        let payload = dataLines.map((line) => line.slice(5).replace(/^\s/, '')).join('\n')
        if (!payload || payload === '[DONE]') continue
        // 后端数据块格式为 {"d":"内容片段"}，同时兼容纯文本及其它常见字段名
        try {
          const parsed = JSON.parse(payload)
          if (typeof parsed === 'object' && parsed !== null) {
            const text = parsed.d ?? parsed.content ?? parsed.result ?? parsed.message
            payload = typeof text === 'string' ? text : ''
          }
        } catch {
          // 非 JSON，按纯文本片段处理
        }
        aiMessage.content += payload
        await scrollToBottom()
      }
    }

    if (!aiMessage.content) {
      aiMessage.content = '（AI 未返回内容，请重试）'
    }
  } catch (e) {
    if ((e as Error).name === 'AbortError') {
      aiMessage.content += '\n\n[已停止生成]'
    } else {
      aiMessage.content = aiMessage.content || '生成失败，请稍后重试'
      message.error('AI 回复失败，请稍后重试')
    }
  } finally {
    streaming.value = false
    abortController = null
    // 流式接口全部返回后，展示生成的网站效果
    previewReady.value = true
    previewKey.value += 1
    await scrollToBottom()
  }
}

// 发送输入框中的消息
const handleSend = () => {
  const text = inputMessage.value.trim()
  if (!text) {
    message.warning('请输入消息内容')
    return
  }
  if (streaming.value) return
  inputMessage.value = ''
  startChat(text)
}

// ============ 部署 ============
const deploying = ref(false)
const deployedUrl = ref('')

const handleDeploy = async () => {
  deploying.value = true
  try {
    const res = await deployApp({ appId })
    if (res.data.code === 0 && res.data.data) {
      deployedUrl.value = res.data.data
      message.success('部署成功')
      // 打开可访问的线上地址
      window.open(res.data.data, '_blank')
    } else {
      message.error('部署失败：' + (res.data.message ?? '未知错误'))
    }
  } catch {
    message.error('部署失败，请稍后重试')
  } finally {
    deploying.value = false
  }
}

// ============ 页面初始化 ============
onMounted(async () => {
  try {
    const res = await getAppVoById({ id: appId })
    if (res.data.code === 0 && res.data.data) {
      app.value = res.data.data
    } else {
      message.error('应用信息加载失败：' + (res.data.message ?? '未知错误'))
      appLoadFailed.value = true
      return
    }
  } catch {
    message.error('应用信息加载失败，请稍后重试')
    appLoadFailed.value = true
    return
  }

  // 从主页新建跳转（?init=1）：自动将初始提示词作为消息发送给 AI
  if (route.query.init === '1' && app.value.initPrompt) {
    startChat(app.value.initPrompt)
  } else {
    // 再次进入对话页：直接展示已生成的网站
    previewReady.value = true
  }
})

// 离开页面时中断未完成的流
onUnmounted(() => {
  abortController?.abort()
})
</script>

<template>
  <div class="app-chat-page">
    <!-- 顶部栏：左侧应用名称，右侧部署按钮 -->
    <header class="chat-top-bar">
      <div class="top-bar-left" @click="router.push('/')">
        <img :src="logo" alt="logo" class="bar-logo" />
        <span class="app-name">{{ app?.appName ?? '应用加载中...' }}</span>
        <HomeOutlined class="home-icon" title="返回主页" />
      </div>
      <div class="top-bar-right">
        <a-button v-if="deployedUrl" type="link" :href="deployedUrl" target="_blank">
          <template #icon>
            <ExportOutlined />
          </template>
          访问线上地址
        </a-button>
        <a-button type="primary" :loading="deploying" :disabled="appLoadFailed" @click="handleDeploy">
          <template #icon>
            <CloudUploadOutlined />
          </template>
          部署
        </a-button>
      </div>
    </header>

    <!-- 应用加载失败 -->
    <div v-if="appLoadFailed" class="load-failed">
      <a-result status="error" title="应用不存在或无权访问">
        <template #extra>
          <a-button type="primary" @click="router.push('/')">返回主页</a-button>
        </template>
      </a-result>
    </div>

    <!-- 核心内容区：左侧对话 + 右侧网页展示 -->
    <main v-else class="chat-main">
      <!-- 左侧对话区域 -->
      <section class="chat-panel">
        <div ref="messageListRef" class="message-list">
          <div v-for="(msg, index) in messages" :key="index" class="message-item"
            :class="msg.role === 'user' ? 'message-user' : 'message-ai'">
            <img v-if="msg.role === 'assistant'" :src="logo" alt="ai" class="message-avatar" />
            <div class="message-bubble">
              <template v-if="msg.content">
                <span class="message-text">{{ msg.content }}</span>
              </template>
              <a-spin v-else-if="streaming && index === messages.length - 1" size="small" />
            </div>
          </div>
          <div v-if="messages.length === 0" class="chat-empty">
            <img :src="logo" alt="logo" class="empty-logo" />
            <p>描述越详细，页面越具体，可以一步一步完善生成效果</p>
          </div>
        </div>

        <!-- 用户消息输入框 -->
        <div class="input-area">
          <a-textarea v-model:value="inputMessage" class="chat-input" placeholder="描述越详细，页面越具体，可以一步一步完善生成效果"
            :auto-size="{ minRows: 2, maxRows: 5 }" :bordered="false" :disabled="streaming"
            @keydown.enter.exact.prevent="handleSend" />
          <div class="input-actions">
            <div class="input-tools">
              <a-tooltip title="功能暂未开放">
                <a-button size="small" disabled>
                  <template #icon>
                    <PaperClipOutlined />
                  </template>
                  上传
                </a-button>
              </a-tooltip>
              <a-tooltip title="功能暂未开放">
                <a-button size="small" disabled>
                  <template #icon>
                    <EditOutlined />
                  </template>
                  编辑
                </a-button>
              </a-tooltip>
              <a-tooltip title="功能暂未开放">
                <a-button size="small" disabled>
                  <template #icon>
                    <ThunderboltOutlined />
                  </template>
                  优化
                </a-button>
              </a-tooltip>
            </div>
            <a-button type="primary" shape="circle" :loading="streaming" :disabled="!inputMessage.trim()"
              @click="handleSend">
              <template #icon>
                <ArrowUpOutlined />
              </template>
            </a-button>
          </div>
        </div>
      </section>

      <!-- 右侧网页展示区域 -->
      <section class="preview-panel">
        <iframe v-if="previewReady && previewUrl" :key="previewKey" :src="previewUrl" title="网站预览"
          class="preview-iframe" />
        <div v-else class="preview-placeholder">
          <a-spin size="large" />
          <p class="placeholder-text">AI 正在生成网站，请稍候……</p>
        </div>
      </section>
    </main>
  </div>
</template>

<style scoped>
.app-chat-page {
  height: 100vh;
  display: flex;
  flex-direction: column;
  background: #f5f6f8;
}

/* ============ 顶部栏 ============ */
.chat-top-bar {
  height: 56px;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
  background: #fff;
  border-bottom: 1px solid #f0f0f0;
  box-shadow: 0 1px 4px rgba(0, 21, 41, 0.06);
}

.top-bar-left {
  display: flex;
  align-items: center;
  gap: 10px;
  cursor: pointer;
}

.bar-logo {
  width: 32px;
  height: 32px;
}

.app-name {
  font-size: 16px;
  font-weight: 600;
  color: rgba(0, 0, 0, 0.88);
  max-width: 360px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.home-icon {
  color: rgba(0, 0, 0, 0.45);
  font-size: 16px;
}

.top-bar-right {
  display: flex;
  align-items: center;
  gap: 8px;
}

.load-failed {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
}

/* ============ 核心内容区 ============ */
.chat-main {
  flex: 1;
  display: flex;
  min-height: 0;
  padding: 16px;
  gap: 16px;
}

/* 左侧对话区 */
.chat-panel {
  width: 40%;
  min-width: 360px;
  display: flex;
  flex-direction: column;
  background: #fff;
  border-radius: 12px;
  border: 1px solid #f0f0f0;
  overflow: hidden;
}

.message-list {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
}

.chat-empty {
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: rgba(0, 0, 0, 0.35);
}

.empty-logo {
  width: 56px;
  height: 56px;
  opacity: 0.5;
  margin-bottom: 12px;
}

.message-item {
  display: flex;
  margin-bottom: 20px;
  gap: 8px;
}

.message-user {
  justify-content: flex-end;
}

.message-ai {
  justify-content: flex-start;
}

.message-avatar {
  width: 28px;
  height: 28px;
  flex-shrink: 0;
  margin-top: 2px;
}

.message-bubble {
  max-width: 80%;
  padding: 10px 14px;
  border-radius: 12px;
  font-size: 14px;
  line-height: 1.7;
}

.message-user .message-bubble {
  background: #e6f4ff;
  color: rgba(0, 0, 0, 0.88);
  border: 1px solid #bae0ff;
}

.message-ai .message-bubble {
  background: #f7f8fa;
  color: rgba(0, 0, 0, 0.88);
  border: 1px solid #f0f0f0;
}

.message-text {
  white-space: pre-wrap;
  word-break: break-word;
}

/* 输入区 */
.input-area {
  flex-shrink: 0;
  border-top: 1px solid #f0f0f0;
  padding: 12px 16px;
}

.chat-input {
  padding: 0;
  font-size: 14px;
}

.input-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 8px;
}

.input-tools {
  display: flex;
  gap: 8px;
}

/* 右侧网页展示区 */
.preview-panel {
  flex: 1;
  min-width: 0;
  background: #fff;
  border-radius: 12px;
  border: 1px solid #f0f0f0;
  overflow: hidden;
}

.preview-iframe {
  width: 100%;
  height: 100%;
  border: none;
}

.preview-placeholder {
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 16px;
  color: rgba(0, 0, 0, 0.45);
}

.placeholder-text {
  margin: 0;
  font-size: 14px;
}

/* 响应式：小屏上下布局 */
@media (max-width: 992px) {
  .chat-main {
    flex-direction: column;
  }

  .chat-panel {
    width: 100%;
    min-width: 0;
    height: 55%;
  }

  .preview-panel {
    height: 45%;
  }
}
</style>
