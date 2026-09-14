<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import type { TableColumnsType } from 'ant-design-vue'
import { adminListChatHistoryByPage } from '@/api/chatHistoryController.ts'

// ============ 搜索条件 ============
const searchParams = reactive<API.ChatHistoryQueryRequest>({
  pageNum: 1,
  pageSize: 20,
  appId: undefined,
  userId: undefined,
  messageType: undefined,
  message: '',
})

// 消息类型选项
const messageTypeOptions = [
  { label: '用户', value: 'user' },
  { label: 'AI', value: 'ai' },
]

// ============ 表格数据 ============
const dataList = ref<API.ChatHistory[]>([])
const total = ref(0)
const loading = ref(false)

// 加载分页数据
const loadData = async () => {
  loading.value = true
  try {
    // 文本框清空时为空串，统一转为 undefined（后端会忽略 null 条件）
    const res = await adminListChatHistoryByPage({
      ...searchParams,
      appId: searchParams.appId || undefined,
      userId: searchParams.userId || undefined,
      messageType: searchParams.messageType || undefined,
      message: searchParams.message || undefined,
    })
    if (res.data.code === 0) {
      const page = res.data.data
      dataList.value = page?.records ?? []
      // totalRow 为后端 long 序列化的字符串，转 number 用于分页组件（行数不会超精度）
      total.value = Number(page?.totalRow ?? 0)
    } else {
      message.error('加载数据失败：' + (res.data.message ?? '未知错误'))
    }
  } catch {
    message.error('加载数据失败，请稍后重试')
  } finally {
    loading.value = false
  }
}

// ============ 搜索 / 重置 ============
const handleSearch = () => {
  searchParams.pageNum = 1
  loadData()
}

const handleReset = () => {
  searchParams.appId = undefined
  searchParams.userId = undefined
  searchParams.messageType = undefined
  searchParams.message = ''
  searchParams.pageNum = 1
  loadData()
}

// ============ 分页变化 ============
const handleTableChange = (page: number, pageSize: number) => {
  searchParams.pageNum = page
  searchParams.pageSize = pageSize
  loadData()
}

// ============ 表格列定义 ============
const columns: TableColumnsType<API.ChatHistory> = [
  { title: 'ID', dataIndex: 'id', width: 100 },
  { title: '应用ID', dataIndex: 'appId', width: 120 },
  { title: '消息内容', dataIndex: 'message', ellipsis: true },
  { title: '消息类型', dataIndex: 'messageType', width: 100, key: 'messageType' },
  { title: '用户ID', dataIndex: 'userId', width: 120 },
  { title: '创建时间', dataIndex: 'createTime', width: 180 },
  { title: '更新时间', dataIndex: 'updateTime', width: 180 },
]

// 页面初始化加载数据
onMounted(() => {
  loadData()
})
</script>

<template>
  <div class="chat-history-manager-page">
    <!-- 搜索区 -->
    <a-card class="search-card">
      <a-form layout="inline">
        <a-form-item label="应用ID">
          <!-- 应用 ID 为后端 Long，按字符串查询，禁止转 Number 以免丢失精度 -->
          <a-input v-model:value="searchParams.appId" placeholder="请输入应用ID" allow-clear style="width: 200px" />
        </a-form-item>
        <a-form-item label="用户ID">
          <a-input v-model:value="searchParams.userId" placeholder="请输入用户ID" allow-clear style="width: 160px" />
        </a-form-item>
        <a-form-item label="消息类型">
          <a-select v-model:value="searchParams.messageType" placeholder="请选择消息类型" allow-clear
            style="width: 120px" :options="messageTypeOptions" />
        </a-form-item>
        <a-form-item label="消息内容">
          <a-input v-model:value="searchParams.message" placeholder="请输入消息内容关键词" allow-clear />
        </a-form-item>
        <a-form-item>
          <a-space>
            <a-button type="primary" @click="handleSearch">查询</a-button>
            <a-button @click="handleReset">重置</a-button>
          </a-space>
        </a-form-item>
      </a-form>
    </a-card>

    <!-- 表格区 -->
    <a-card class="table-card">
      <a-table :columns="columns" :data-source="dataList" :loading="loading" :pagination="{
        current: searchParams.pageNum,
        pageSize: searchParams.pageSize,
        total: total,
        showSizeChanger: true,
        pageSizeOptions: ['10', '20', '50', '100'],
        showTotal: (t: number) => `共 ${t} 条`,
      }" row-key="id" :scroll="{ x: 1100 }" @change="(pag: any) => handleTableChange(pag.current, pag.pageSize)">
        <template #bodyCell="{ column, record }">
          <!-- 消息类型列 -->
          <template v-if="column.key === 'messageType'">
            <a-tag v-if="record.messageType === 'user'" color="blue">用户</a-tag>
            <a-tag v-else-if="record.messageType === 'ai'" color="green">AI</a-tag>
            <a-tag v-else>{{ record.messageType ?? '-' }}</a-tag>
          </template>
        </template>
      </a-table>
    </a-card>
  </div>
</template>

<style scoped>
.chat-history-manager-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.search-card :deep(.ant-form-inline) {
  flex-wrap: wrap;
  gap: 8px;
}
</style>
