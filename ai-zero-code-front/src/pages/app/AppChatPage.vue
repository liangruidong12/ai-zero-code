<script setup lang="ts">
import { computed, nextTick, onMounted, onUnmounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import {
  ArrowUpOutlined,
  CloudUploadOutlined,
  CopyOutlined,
  DeleteOutlined,
  EditOutlined,
  ExportOutlined,
  FileAddOutlined,
  FileTextOutlined,
  FolderOpenOutlined,
  HomeOutlined,
  PaperClipOutlined,
  ThunderboltOutlined,
  ToolOutlined,
} from '@ant-design/icons-vue'
import logo from '@/assets/logo.svg'
import myAxios from '@/request'
import { deployApp, getAppVoById } from '@/api/appController.ts'
import { listChatHistoryVoByPage } from '@/api/chatHistoryController.ts'
import { useLoginUserStore } from '@/stores/loginUser.ts'
import { useVisualEditor } from '@/composables/useVisualEditor'

const route = useRoute()
const router = useRouter()
// 应用 ID 全链路保持字符串，避免大整数转 Number 丢失精度
const appId = route.params.id as string

// 当前登录用户（路由守卫已加载），用于判断是否为自己的应用
const loginUserStore = useLoginUserStore()

// 历史消息每页条数
const HISTORY_PAGE_SIZE = 10

// 后端服务地址（与 request.ts 中的 baseURL 保持一致）
const API_BASE = (myAxios.defaults.baseURL as string) ?? ''

// ============ 应用信息 ============
const app = ref<API.AppVO>()
const appLoadFailed = ref(false)

// 生成网站的本地访问地址：
// HTML/MULTI_FILE → http://localhost:8123/api/static/{codeGenType}_{appId}/
// VUE_PROJECT → http://localhost:8123/api/static/vue_project_{appId}/dist/（构建产物目录）
const previewUrl = computed(() => {
  if (!app.value?.codeGenType) return ''
  const base = `${API_BASE}/api/static/${app.value.codeGenType}_${appId}`
  // VUE_PROJECT 的预览产物在 dist 子目录下
  return app.value.codeGenType === 'vue_project' ? `${base}/dist/` : `${base}/`
})

// 是否为 Vue 项目（需要异步构建 dist）
const isVueProject = computed(() => app.value?.codeGenType === 'vue_project')

// 生成类型标签
const codeGenTypeMap: Record<string, { text: string; color: string }> = {
  html: { text: 'HTML', color: 'blue' },
  multi_file: { text: '多文件', color: 'green' },
  vue_project: { text: 'Vue 项目', color: 'purple' },
}
const codeGenTypeTag = computed(() => {
  const t = app.value?.codeGenType
  if (!t) return null
  return codeGenTypeMap[t] ?? { text: t, color: 'default' }
})

// ============ 对话消息 ============
interface ChatMessage {
  id: string
  role: 'user' | 'assistant'
  content: string
  createTime?: string
}

// ============ AI 消息内容分段解析 ============
interface ContentSegment {
  type: 'text' | 'tool-select' | 'tool-call'
  text?: string
  toolName?: string
  action?: string
  filePath?: string
  language?: string
  code?: string
}

// 工具操作 → 图标组件映射
const toolIconMap: Record<string, any> = {
  '写入文件': FileAddOutlined,
  '读取文件': FileTextOutlined,
  '修改文件': EditOutlined,
  '删除文件': DeleteOutlined,
  '读取目录': FolderOpenOutlined,
}

/**
 * 将 AI 消息内容解析为分段：纯文本 / 工具选择 / 工具调用+代码块
 * 后端格式：
 *   [选择工具] 写入文件
 *   [工具调用] 写入文件 package.json
 *   ```json
 *   { ... }
 *   ```
 */
const parseMessageContent = (content: string): ContentSegment[] => {
  const segments: ContentSegment[] = []
  const lines = content.split('\n')
  let i = 0
  let textBuffer: string[] = []

  const flushText = () => {
    if (textBuffer.length > 0) {
      const text = textBuffer.join('\n')
      if (text.trim()) segments.push({ type: 'text', text })
      textBuffer = []
    }
  }

  while (i < lines.length) {
    const line = lines[i]

    // [选择工具] 写入文件
    if (line.match(/^\[选择工具\]/)) {
      flushText()
      segments.push({ type: 'tool-select', toolName: line.replace(/^\[选择工具\]\s*/, '').trim() })
      i++
      continue
    }

    // [工具调用] 写入文件 package.json
    if (line.match(/^\[工具调用\]/)) {
      flushText()
      const rest = line.replace(/^\[工具调用\]\s*/, '').trim()
      const parts = rest.split(/\s+/)
      const action = parts[0] || ''
      const filePath = parts.slice(1).join(' ') || ''

      // 检查后续是否紧跟代码块
      if (i + 1 < lines.length && lines[i + 1].match(/^```/)) {
        const langMatch = lines[i + 1].match(/^```(\w*)/)
        const language = langMatch ? langMatch[1] : ''
        i += 2 // 跳过 [工具调用] 行和 ``` 行
        const codeLines: string[] = []
        while (i < lines.length && !lines[i].match(/^```/)) {
          codeLines.push(lines[i])
          i++
        }
        // 跳过闭合 ```（流式中可能还没闭合）
        if (i < lines.length && lines[i].match(/^```/)) i++
        segments.push({ type: 'tool-call', action, filePath, language, code: codeLines.join('\n') })
      } else {
        segments.push({ type: 'tool-call', action, filePath })
        i++
      }
      continue
    }

    // 独立代码块（不跟在 [工具调用] 后面）
    if (line.match(/^```/)) {
      flushText()
      const langMatch = line.match(/^```(\w*)/)
      const language = langMatch ? langMatch[1] : ''
      i++
      const codeLines: string[] = []
      while (i < lines.length && !lines[i].match(/^```/)) {
        codeLines.push(lines[i])
        i++
      }
      if (i < lines.length && lines[i].match(/^```/)) i++
      segments.push({ type: 'tool-call', action: '代码块', filePath: '', language, code: codeLines.join('\n') })
      continue
    }

    textBuffer.push(line)
    i++
  }
  flushText()
  return segments
}

// 复制代码到剪贴板
const copyCode = (code: string) => {
  navigator.clipboard.writeText(code).then(() => {
    message.success('已复制到剪贴板')
  }).catch(() => {
    message.error('复制失败')
  })
}

const messages = ref<ChatMessage[]>([])
const inputMessage = ref('')
const streaming = ref(false)
// 网页预览是否就绪（进入时已有生成结果或流式生成完成后展示）
const previewReady = ref(false)
// Vue 项目异步构建中（npm install + npm run build）
const building = ref(false)
// 用于强制刷新 iframe
const previewKey = ref(0)

// ============ 可视化编辑 ============
const { editMode, selectedElement, iframeRef, enterEditMode, exitEditMode, clearSelection, buildElementContext } = useVisualEditor(previewUrl)

const handleToggleEditMode = () => {
  if (editMode.value) {
    exitEditMode()
  } else {
    if (!previewReady.value) {
      message.warning('请先等待网站生成完成')
      return
    }
    enterEditMode()
    if (editMode.value) {
      message.info('已进入编辑模式，点击预览中的元素进行选中')
    }
  }
}

// 轮询检查 dist 目录是否就绪（fetch 预览 URL，返回 200 即构建完成）
const waitForBuild = async () => {
  building.value = true
  const url = previewUrl.value
  const maxAttempts = 60 // 最多轮询 60 次（约 2 分钟）
  for (let i = 0; i < maxAttempts; i++) {
    try {
      const resp = await fetch(url, { method: 'HEAD', credentials: 'include' })
      if (resp.ok) {
        building.value = false
        previewReady.value = true
        previewKey.value += 1
        return
      }
    } catch {
      // dist 还不存在，继续等待
    }
    await new Promise((resolve) => setTimeout(resolve, 2000))
  }
  // 超时：构建可能失败或耗时过长
  building.value = false
  message.warning('项目构建超时，请稍后刷新或重新部署')
}

// ============ 对话历史（游标分页） ============
const historyLoading = ref(false)
// 是否还有更早的历史消息
const hasMoreHistory = ref(false)
// 历史消息总数（首次加载时记录）
const historyTotal = ref(0)
// 历史是否加载成功（非应用创建者无权查看历史）
const historyLoaded = ref(false)
// 是否为自己的应用
const isMyApp = computed(
  () => !!app.value?.userId && app.value.userId === loginUserStore.loginUser.id,
)
// 游标：已加载消息中最旧一条的创建时间
let oldestCreateTime = ''
// 本地新增消息的自增序号（用于生成列表 key）
let localMessageSeq = 0

// ChatHistoryVO -> ChatMessage
const toChatMessage = (record: API.ChatHistoryVO): ChatMessage => ({
  id: String(record.id ?? `local-${localMessageSeq++}`),
  role: record.messageType === 'user' ? 'user' : 'assistant',
  content: record.message ?? '',
  createTime: record.createTime,
})

/**
 * 加载对话历史（游标分页，每页 10 条，升序展示）
 * 首次不传 lastCreateTime 获取最新一页；加载更多时传已加载最旧一条的创建时间往前翻页
 */
const loadChatHistory = async (isLoadMore: boolean) => {
  if (historyLoading.value) return
  historyLoading.value = true
  // 记录加载前的滚动位置，加载完成后保持视口锚定
  const listEl = messageListRef.value
  const prevTop = listEl?.scrollTop ?? 0
  const prevHeight = listEl?.scrollHeight ?? 0
  try {
    const res = await listChatHistoryVoByPage({
      appId,
      pageNum: 1,
      pageSize: HISTORY_PAGE_SIZE,
      lastCreateTime: isLoadMore ? oldestCreateTime : undefined,
    })
    if (res.data.code === 0) {
      historyLoaded.value = true
      const page = res.data.data
      const records = page?.records ?? []
      historyTotal.value = Number(page?.totalRow ?? 0)
      // 后端按创建时间倒序返回最新一页，反转为升序展示
      const olderMessages = records.map(toChatMessage).reverse()
      if (isLoadMore) {
        messages.value = [...olderMessages, ...messages.value]
      } else {
        messages.value = olderMessages
      }
      // 更新游标为当前最旧一条历史消息的创建时间
      const firstMsg = messages.value[0]
      if (firstMsg?.createTime) {
        oldestCreateTime = firstMsg.createTime
      }
      // 返回满页说明可能还有更早的消息
      hasMoreHistory.value = records.length >= HISTORY_PAGE_SIZE
    } else {
      historyLoaded.value = false
    }
  } catch {
    // 无权限查看历史（如访问他人应用）时静默降级
    historyLoaded.value = false
  } finally {
    historyLoading.value = false
    if (isLoadMore) {
      await nextTick()
      if (listEl) {
        listEl.scrollTop = listEl.scrollHeight - prevHeight + prevTop
      }
    }
  }
}

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
  messages.value.push({ id: `local-${localMessageSeq++}`, role: 'user', content: userMessage })
  const aiMessage: ChatMessage = reactive({
    id: `local-${localMessageSeq++}`,
    role: 'assistant',
    content: '',
  })
  messages.value.push(aiMessage)
  streaming.value = true
  await scrollToBottom()

  // 对话消息由后端 SSE 接口（chatToGenCode）统一持久化，前端不再重复保存
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
    // 流式接口返回后，对 VUE_PROJECT 需等待异步构建 dist 完成再展示预览
    if (isVueProject.value) {
      waitForBuild()
    } else {
      previewReady.value = true
      previewKey.value += 1
    }
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
  // 如果有选中的元素，将元素信息附加到提示词
  let finalMessage = text
  if (selectedElement.value) {
    finalMessage += buildElementContext()
  }
  inputMessage.value = ''
  // 发送后清除选中元素并退出编辑模式
  clearSelection()
  exitEditMode()
  startChat(finalMessage)
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

  // 加载最近一页对话历史（10 条）
  await loadChatHistory(false)

  if (historyLoaded.value) {
    const initPrompt = app.value?.initPrompt
    if (isMyApp.value && historyTotal.value === 0 && initPrompt) {
      // 自己的应用且没有任何对话历史：自动将初始提示词作为第一条消息发送
      startChat(initPrompt)
    } else if (historyTotal.value >= 2) {
      // 至少 2 条对话记录说明 AI 已完成过生成，直接展示网站
      previewReady.value = true
    }
  } else if (app.value?.codeGenType) {
    // 无权查看对话历史（非创建者访问他人应用）：应用已生成过则直接展示网站
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
        <a-tag v-if="codeGenTypeTag" :color="codeGenTypeTag.color" class="gen-type-tag">{{ codeGenTypeTag.text
        }}</a-tag>
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
                <!-- 用户消息：纯文本 -->
                <span v-if="msg.role === 'user'" class="message-text">{{ msg.content }}</span>
                <!-- AI 消息：分段渲染 -->
                <template v-else>
                  <template v-for="(seg, si) in parseMessageContent(msg.content)" :key="si">
                    <!-- 纯文本段 -->
                    <span v-if="seg.type === 'text'" class="message-text">{{ seg.text }}</span>
                    <!-- 工具选择提示 -->
                    <div v-else-if="seg.type === 'tool-select'" class="tool-badge tool-select-badge">
                      <ToolOutlined class="tool-badge-icon" />
                      <span>{{ seg.toolName }}</span>
                    </div>
                    <!-- 工具调用 + 代码块 -->
                    <div v-else-if="seg.type === 'tool-call'" class="tool-call-block">
                      <div class="tool-call-header">
                        <component :is="toolIconMap[seg.action] ?? ToolOutlined" class="tool-call-icon" />
                        <span class="tool-call-action">{{ seg.action }}</span>
                        <span v-if="seg.filePath" class="tool-call-file">{{ seg.filePath }}</span>
                      </div>
                      <div v-if="seg.code" class="code-block">
                        <div class="code-block-header">
                          <span class="code-lang">{{ seg.language || 'text' }}</span>
                          <a-button type="text" size="small" @click="copyCode(seg.code!)">
                            <template #icon>
                              <CopyOutlined />
                            </template>
                          </a-button>
                        </div>
                        <pre class="code-block-body"><code>{{ seg.code }}</code></pre>
                      </div>
                    </div>
                  </template>
                </template>
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
          <a-alert v-if="selectedElement" type="info" show-icon closable @close="clearSelection"
            style="margin-bottom: 8px; text-align: left;">
            <template #message>
              <span class="selected-element-info">
                &lt;{{ selectedElement.tagName }}&gt;
                <span v-if="selectedElement.id"> #{{ selectedElement.id }} </span>
                <span v-if="selectedElement.className"> .{{ selectedElement.className }} </span>
                | {{ selectedElement.positionDesc }}
                <span v-if="selectedElement.text"> | "{{ selectedElement.text.slice(0, 30) }}"</span>
              </span>
            </template>
          </a-alert>
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
              <a-tooltip :title="editMode ? '退出编辑模式' : '进入编辑模式'">
                <a-button size="small" :type="editMode ? 'primary' : 'default'" @click="handleToggleEditMode">
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
        <iframe v-if="previewReady && previewUrl" ref="iframeRef" :key="previewKey" :src="previewUrl" title="网站预览"
          class="preview-iframe" />
        <div v-else class="preview-placeholder">
          <a-spin size="large" />
          <p class="placeholder-text">
            {{ building ? '正在构建 Vue 项目（npm install + npm run build），请稍候……' : 'AI 正在生成网站，请稍候……' }}
          </p>
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

.gen-type-tag {
  margin-left: 4px;
  font-size: 12px;
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

.load-more-wrap {
  display: flex;
  justify-content: center;
  margin-bottom: 12px;
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

/* ============ 工具调用 & 代码块样式 ============ */
.message-ai .message-bubble {
  overflow: hidden;
}

/* 工具选择提示 */
.tool-badge {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 3px 10px;
  border-radius: 6px;
  font-size: 12px;
  font-weight: 500;
  margin: 2px 0;
}

.tool-select-badge {
  background: #f0f5ff;
  color: #4f6ef2;
  border: 1px solid #adc6ff;
}

.tool-badge-icon {
  font-size: 13px;
}

/* 工具调用块 */
.tool-call-block {
  margin: 8px 0;
  border-radius: 8px;
  overflow: hidden;
  border: 1px solid #e8e8e8;
}

.tool-call-header {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 12px;
  background: #fafafa;
  border-bottom: 1px solid #e8e8e8;
  font-size: 13px;
}

.tool-call-icon {
  font-size: 15px;
  color: #4f6ef2;
}

.tool-call-action {
  font-weight: 500;
  color: rgba(0, 0, 0, 0.75);
}

.tool-call-file {
  color: rgba(0, 0, 0, 0.55);
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace;
  font-size: 12px;
}

/* 代码块 */
.code-block {
  background: #1e1e2e;
  overflow: hidden;
}

.code-block-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 4px 12px;
  background: #181825;
}

.code-lang {
  font-size: 11px;
  color: #a6adc8;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.code-block-header :deep(.ant-btn) {
  color: #a6adc8;
}

.code-block-body {
  margin: 0;
  padding: 12px 16px;
  overflow-x: auto;
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace;
  font-size: 13px;
  line-height: 1.6;
  color: #cdd6f4;
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

/* 选中元素信息 */
.selected-element-info {
  font-size: 12px;
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace;
  color: rgba(0, 0, 0, 0.65);
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
