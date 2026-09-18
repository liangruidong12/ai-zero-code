<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { message, Modal } from 'ant-design-vue'
import {
  ArrowUpOutlined,
  DeleteOutlined,
  EditOutlined,
  PaperClipOutlined,
  ThunderboltOutlined,
  UserOutlined,
} from '@ant-design/icons-vue'
import logo from '@/assets/logo.svg'
import {
  addApp,
  deleteApp,
  listGoodAppVoByPage,
  listMyAppVoByPage,
} from '@/api/appController.ts'
import { useLoginUserStore } from '@/stores/loginUser.ts'

const router = useRouter()
const loginUserStore = useLoginUserStore()
// 是否已登录
const isLogin = computed(() => !!loginUserStore.loginUser.id)

// ============ 提示词创建应用 ============
const prompt = ref('')
const creating = ref(false)

// 推荐提示词（点击后填充到输入框）
const suggestPrompts = ['波普风电商页面', '企业网站', '电商运营后台', '暗黑话题社区']

const handleCreate = async () => {
  const initPrompt = prompt.value.trim()
  if (!initPrompt) {
    message.warning('请输入提示词')
    return
  }
  creating.value = true
  try {
    const res = await addApp({ initPrompt })
    if (res.data.code === 0 && res.data.data) {
      // 创建成功后跳转到对话页；对话页检测到无历史消息时会自动发送初始提示词
      router.push(`/app/chat/${res.data.data}`)
    } else {
      message.error('创建失败：' + (res.data.message ?? '未知错误'))
    }
  } catch {
    message.error('创建失败，请稍后重试')
  } finally {
    creating.value = false
  }
}

// ============ 应用列表通用加载逻辑 ============
const PAGE_SIZE = 12

const myParams = reactive<API.AppQueryRequest>({ pageNum: 1, pageSize: PAGE_SIZE, appName: '' })
const goodParams = reactive<API.AppQueryRequest>({ pageNum: 1, pageSize: PAGE_SIZE, appName: '' })

const myList = ref<API.AppVO[]>([])
const myTotal = ref(0)
const myLoading = ref(false)
const goodList = ref<API.AppVO[]>([])
const goodTotal = ref(0)
const goodLoading = ref(false)

// 加载我的应用
const loadMyApps = async () => {
  myLoading.value = true
  try {
    const res = await listMyAppVoByPage(myParams)
    if (res.data.code === 0) {
      myList.value = res.data.data?.records ?? []
      // totalRow 为后端 long 序列化的字符串，转 number 用于分页组件（行数不会超精度）
      myTotal.value = Number(res.data.data?.totalRow ?? 0)
    } else {
      message.error('加载我的应用失败：' + (res.data.message ?? '未知错误'))
    }
  } catch {
    message.error('加载我的应用失败，请稍后重试')
  } finally {
    myLoading.value = false
  }
}

// 加载精选应用
const loadGoodApps = async () => {
  goodLoading.value = true
  try {
    const res = await listGoodAppVoByPage(goodParams)
    if (res.data.code === 0) {
      goodList.value = res.data.data?.records ?? []
      // totalRow 为后端 long 序列化的字符串，转 number 用于分页组件（行数不会超精度）
      goodTotal.value = Number(res.data.data?.totalRow ?? 0)
    } else {
      message.error('加载精选应用失败：' + (res.data.message ?? '未知错误'))
    }
  } catch {
    message.error('加载精选应用失败，请稍后重试')
  } finally {
    goodLoading.value = false
  }
}

// 搜索 / 分页
const handleMySearch = () => {
  myParams.pageNum = 1
  loadMyApps()
}
const handleGoodSearch = () => {
  goodParams.pageNum = 1
  loadGoodApps()
}
const handleMyPageChange = (page: number) => {
  myParams.pageNum = page
  loadMyApps()
}
const handleGoodPageChange = (page: number) => {
  goodParams.pageNum = page
  loadGoodApps()
}

// ============ 应用卡片操作 ============
// 打开应用对话页
const openApp = (record: API.AppVO) => {
  router.push(`/app/chat/${record.id}`)
}

// 跳转到应用信息修改页
const handleEdit = (record: API.AppVO) => {
  router.push(`/app/edit/${record.id}`)
}

// 删除自己的应用
const handleDelete = (record: API.AppVO) => {
  Modal.confirm({
    title: '确认删除',
    content: `确定要删除应用「${record.appName ?? '未命名应用'}」吗？`,
    okType: 'danger',
    okText: '删除',
    cancelText: '取消',
    onOk: async () => {
      try {
        const res = await deleteApp({ id: record.id })
        if (res.data.code === 0 && res.data.data) {
          message.success('删除成功')
          // 删除后若当前页已无数据，回退一页
          if (myList.value.length === 1 && (myParams.pageNum ?? 1) > 1) {
            myParams.pageNum = (myParams.pageNum ?? 1) - 1
          }
          loadMyApps()
        } else {
          message.error('删除失败：' + (res.data.message ?? '未知错误'))
        }
      } catch {
        message.error('删除失败，请稍后重试')
      }
    },
  })
}

// ============ 工具方法 ============
// 相对时间格式化
const formatRelativeTime = (time?: string) => {
  if (!time) return '未知时间'
  const diff = Date.now() - new Date(time).getTime()
  const minute = 60 * 1000
  const hour = 60 * minute
  const day = 24 * hour
  const week = 7 * day
  if (diff < hour) return `${Math.max(1, Math.floor(diff / minute))} 分钟前`
  if (diff < day) return `${Math.floor(diff / hour)} 小时前`
  if (diff < week) return `${Math.floor(diff / day)} 天前`
  if (diff < 4 * week) return `${Math.floor(diff / week)} 周前`
  return `${Math.floor(diff / (30 * day))} 个月前`
}

onMounted(() => {
  // 精选应用公开可查；我的应用仅登录后加载
  loadGoodApps()
  if (isLogin.value) {
    loadMyApps()
  }
})
</script>

<template>
  <div class="home-page">
    <!-- 顶部标题 + 提示词输入区 -->
    <section class="hero-section">
      <div class="hero-content">
        <h1 class="hero-title">
          一句话
          <img :src="logo" alt="logo" class="hero-logo" />
          呈所想
        </h1>
        <p class="hero-subtitle">与 AI 对话轻松创建应用和网站</p>

        <div class="prompt-card">
          <a-textarea v-model:value="prompt" class="prompt-input" placeholder="使用 NoCode 创建一个高效的小工具，帮我计算……"
            :auto-size="{ minRows: 3, maxRows: 6 }" :bordered="false" @keydown.enter.exact.prevent="handleCreate" />
          <div class="prompt-actions">
            <div class="prompt-tools">
              <a-tooltip title="功能暂未开放">
                <a-button disabled>
                  <template #icon>
                    <PaperClipOutlined />
                  </template>
                  上传
                </a-button>
              </a-tooltip>
              <a-tooltip title="功能暂未开放">
                <a-button disabled>
                  <template #icon>
                    <ThunderboltOutlined />
                  </template>
                  优化
                </a-button>
              </a-tooltip>
            </div>
            <a-button type="primary" shape="circle" size="large" class="submit-btn" :loading="creating"
              @click="handleCreate">
              <template #icon>
                <ArrowUpOutlined />
              </template>
            </a-button>
          </div>
        </div>

        <div class="suggest-list">
          <a-button v-for="item in suggestPrompts" :key="item" class="suggest-btn" @click="prompt = item">
            {{ item }}
          </a-button>
        </div>
      </div>
    </section>

    <div class="home-content">
      <!-- 我的作品 -->
      <a-card class="section-card">
        <div class="section-header">
          <h2 class="section-title">我的作品</h2>
          <a-input-search v-if="isLogin" v-model:value="myParams.appName" placeholder="搜索我的应用名称" allow-clear
            style="width: 260px" @search="handleMySearch" @press-enter="handleMySearch" />
        </div>

        <!-- 未登录提示 -->
        <a-empty v-if="!isLogin" description="登录后即可查看和管理你创建的应用">
          <a-button type="primary" @click="router.push('/user/login')">去登录</a-button>
        </a-empty>

        <!-- 已登录：应用列表 -->
        <template v-else>
          <a-spin :spinning="myLoading">
            <div v-if="myList.length > 0" class="app-grid">
              <div v-for="app in myList" :key="app.id" class="app-card" @click="openApp(app)">
                <div class="app-cover">
                  <img v-if="app.cover" :src="app.cover" :alt="app.appName" />
                  <img v-else :src="logo" alt="placeholder" class="cover-placeholder" />
                  <div class="app-card-actions" @click.stop>
                    <a-tooltip title="编辑应用信息">
                      <a-button shape="circle" size="small" @click="handleEdit(app)">
                        <template #icon>
                          <EditOutlined />
                        </template>
                      </a-button>
                    </a-tooltip>
                    <a-tooltip title="删除应用">
                      <a-button shape="circle" size="small" danger @click="handleDelete(app)">
                        <template #icon>
                          <DeleteOutlined />
                        </template>
                      </a-button>
                    </a-tooltip>
                  </div>
                </div>
                <div class="app-info">
                  <div class="app-name">{{ app.appName ?? '未命名应用' }}</div>
                  <div class="app-time">创建于 {{ formatRelativeTime(app.createTime) }}</div>
                </div>
              </div>
            </div>
            <a-empty v-else description="还没有作品，输入提示词创建你的第一个应用吧" />
          </a-spin>
          <div v-if="myTotal > PAGE_SIZE" class="pagination-wrap">
            <a-pagination :current="myParams.pageNum" :page-size="PAGE_SIZE" :total="myTotal" :show-size-changer="false"
              @change="handleMyPageChange" />
          </div>
        </template>
      </a-card>

      <!-- 精选案例 -->
      <a-card class="section-card">
        <div class="section-header">
          <h2 class="section-title">精选案例</h2>
          <a-input-search v-model:value="goodParams.appName" placeholder="搜索精选应用名称" allow-clear style="width: 260px"
            @search="handleGoodSearch" @press-enter="handleGoodSearch" />
        </div>

        <a-spin :spinning="goodLoading">
          <div v-if="goodList.length > 0" class="app-grid">
            <div v-for="app in goodList" :key="app.id" class="app-card" @click="openApp(app)">
              <div class="app-cover">
                <img v-if="app.cover" :src="app.cover" :alt="app.appName" />
                <img v-else :src="logo" alt="placeholder" class="cover-placeholder" />
              </div>
              <div class="app-info good-app-info">
                <a-avatar :src="app.user?.userAvatar" :size="32" class="author-avatar">
                  <template #icon>
                    <UserOutlined />
                  </template>
                </a-avatar>
                <div class="app-meta">
                  <div class="app-name">{{ app.appName ?? '未命名应用' }}</div>
                  <div class="app-time">{{ app.user?.userName ?? 'NoCode 官方' }}</div>
                </div>
              </div>
            </div>
          </div>
          <a-empty v-else description="暂无精选应用" />
        </a-spin>
        <div v-if="goodTotal > PAGE_SIZE" class="pagination-wrap">
          <a-pagination :current="goodParams.pageNum" :page-size="PAGE_SIZE" :total="goodTotal"
            :show-size-changer="false" @change="handleGoodPageChange" />
        </div>
      </a-card>
    </div>
  </div>
</template>

<style scoped>
.home-page {
  min-height: 100%;
}

/* ============ 顶部标题区 ============ */
.hero-section {
  margin: -24px -24px 0;
  padding: 72px 24px 96px;
  background: linear-gradient(160deg, #f3faf2 0%, #e3f7ee 30%, #cdeff0 55%, #8fd4e8 100%);
}

.hero-content {
  max-width: 960px;
  margin: 0 auto;
  text-align: center;
}

.hero-title {
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 48px;
  font-weight: 800;
  color: #1a1a1a;
  margin: 0;
}

.hero-logo {
  width: 56px;
  height: 56px;
  margin: 0 16px;
}

.hero-subtitle {
  margin: 16px 0 40px;
  font-size: 18px;
  color: rgba(0, 0, 0, 0.55);
}

.prompt-card {
  background: #fff;
  border-radius: 20px;
  padding: 20px 24px 16px;
  box-shadow: 0 8px 32px rgba(31, 138, 160, 0.12);
  text-align: left;
}

.prompt-input {
  padding: 0;
  font-size: 16px;
}

.prompt-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 12px;
}

.prompt-tools {
  display: flex;
  gap: 12px;
}

.submit-btn {
  background: #8c8c8c;
}

.suggest-list {
  display: flex;
  justify-content: center;
  flex-wrap: wrap;
  gap: 12px;
  margin-top: 32px;
}

.suggest-btn {
  border-radius: 10px;
  color: rgba(0, 0, 0, 0.65);
}

/* ============ 列表内容区 ============ */
.home-content {
  max-width: 1200px;
  margin: -48px auto 0;
  position: relative;
  display: flex;
  flex-direction: column;
  gap: 24px;
  padding-bottom: 24px;
}

.section-card {
  border-radius: 20px;
}

.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
  gap: 12px;
  flex-wrap: wrap;
}

.section-title {
  font-size: 24px;
  font-weight: 700;
  margin: 0;
}

.app-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
  gap: 20px;
}

.app-card {
  border: 1px solid #f0f0f0;
  border-radius: 14px;
  overflow: hidden;
  cursor: pointer;
  background: #fff;
  transition: all 0.2s ease;
}

.app-card:hover {
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.1);
  transform: translateY(-2px);
}

.app-cover {
  position: relative;
  height: 180px;
  background: #f7f8fa;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
}

.app-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.cover-placeholder {
  width: 64px !important;
  height: 64px !important;
  opacity: 0.35;
}

.app-card-actions {
  position: absolute;
  top: 8px;
  right: 8px;
  display: none;
  gap: 8px;
}

.app-card:hover .app-card-actions {
  display: flex;
}

.app-info {
  padding: 14px 16px;
}

.good-app-info {
  display: flex;
  align-items: center;
  gap: 10px;
}

.author-avatar {
  flex-shrink: 0;
  background-color: #1677ff;
}

.app-meta {
  min-width: 0;
}

.app-name {
  font-size: 16px;
  font-weight: 600;
  color: rgba(0, 0, 0, 0.88);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.app-time {
  margin-top: 4px;
  font-size: 13px;
  color: rgba(0, 0, 0, 0.45);
}

.pagination-wrap {
  display: flex;
  justify-content: center;
  margin-top: 24px;
}

/* 响应式 */
@media (max-width: 768px) {
  .hero-section {
    margin: -12px -12px 0;
    padding: 48px 12px 72px;
  }

  .hero-title {
    font-size: 32px;
  }

  .hero-logo {
    width: 40px;
    height: 40px;
    margin: 0 10px;
  }
}
</style>
