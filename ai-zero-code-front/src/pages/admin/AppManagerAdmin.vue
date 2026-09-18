<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { message, Modal } from 'ant-design-vue'
import { useRouter } from 'vue-router'

const router = useRouter()
import type { TableColumnsType } from 'ant-design-vue'
import {
  adminDeleteApp,
  adminListAppByPage,
  adminUpdateApp,
} from '@/api/appController.ts'

// 精选应用的优先级
const FEATURED_PRIORITY = 99

// ============ 搜索条件 ============
const searchParams = reactive<API.AppQueryRequest>({
  pageNum: 1,
  pageSize: 20,
  appName: '',
  codeGenType: '',
  priority: undefined,
  userId: undefined,
})

// ============ 表格数据 ============
const dataList = ref<API.App[]>([])
const total = ref(0)
const loading = ref(false)

// 加载分页数据
const loadData = async () => {
  loading.value = true
  try {
    // 数字查询框清空时为 null、文本框清空时为空串，统一转为 undefined（后端会忽略 null 条件）
    const res = await adminListAppByPage({
      ...searchParams,
      appName: searchParams.appName || undefined,
      codeGenType: searchParams.codeGenType || undefined,
      priority: searchParams.priority ?? undefined,
      userId: searchParams.userId || undefined,
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
  searchParams.appName = ''
  searchParams.codeGenType = ''
  searchParams.priority = undefined
  searchParams.userId = undefined
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
const columns: TableColumnsType<API.App> = [
  { title: 'ID', dataIndex: 'id', width: 80 },
  { title: '应用名称', dataIndex: 'appName', width: 160 },
  { title: '封面', dataIndex: 'cover', width: 90, key: 'cover' },
  { title: '初始提示词', dataIndex: 'initPrompt', ellipsis: true },
  { title: '生成类型', dataIndex: 'codeGenType', width: 110 },
  { title: '部署标识', dataIndex: 'deployKey', width: 140, ellipsis: true },
  { title: '优先级', dataIndex: 'priority', width: 90, key: 'priority' },
  { title: '用户ID', dataIndex: 'userId', width: 90 },
  { title: '创建时间', dataIndex: 'createTime', width: 180 },
  { title: '操作', key: 'action', width: 190, fixed: 'right' },
]

// ============ 编辑：跳转到应用信息修改页 ============
const handleEdit = (record: API.App) => {
  router.push(`/app/edit/${record.id}`)
}

// ============ 精选：设置优先级为 99 ============
const handleFeature = (record: API.App) => {
  Modal.confirm({
    title: '设为精选',
    content: `确定要将应用「${record.appName ?? record.id}」设为精选应用吗？`,
    okText: '设为精选',
    cancelText: '取消',
    onOk: async () => {
      try {
        const res = await adminUpdateApp({ id: record.id, priority: FEATURED_PRIORITY })
        if (res.data.code === 0 && res.data.data) {
          message.success('设置成功')
          loadData()
        } else {
          message.error('设置失败：' + (res.data.message ?? '未知错误'))
        }
      } catch {
        message.error('设置失败，请稍后重试')
      }
    },
  })
}

// ============ 删除 ============
const handleDelete = (record: API.App) => {
  Modal.confirm({
    title: '确认删除',
    content: `确定要删除应用「${record.appName ?? record.id}」吗？`,
    okType: 'danger',
    okText: '删除',
    cancelText: '取消',
    onOk: async () => {
      try {
        const res = await adminDeleteApp({ id: record.id })
        if (res.data.code === 0 && res.data.data) {
          message.success('删除成功')
          loadData()
        } else {
          message.error('删除失败：' + (res.data.message ?? '未知错误'))
        }
      } catch {
        message.error('删除失败，请稍后重试')
      }
    },
  })
}

// 页面初始化加载数据
onMounted(() => {
  loadData()
})
</script>

<template>
  <div class="app-manager-page">
    <!-- 搜索区 -->
    <a-card class="search-card">
      <a-form layout="inline">
        <a-form-item label="应用名称">
          <a-input v-model:value="searchParams.appName" placeholder="请输入应用名称" allow-clear />
        </a-form-item>
        <a-form-item label="生成类型">
          <a-input v-model:value="searchParams.codeGenType" placeholder="请输入代码生成类型" allow-clear />
        </a-form-item>
        <a-form-item label="优先级">
          <a-input-number v-model:value="searchParams.priority" placeholder="优先级" :min="0" style="width: 110px" />
        </a-form-item>
        <a-form-item label="用户ID">
          <!-- 用户 ID 为后端 Long，按字符串查询，禁止转 Number 以免丢失精度 -->
          <a-input v-model:value="searchParams.userId" placeholder="请输入用户ID" allow-clear style="width: 160px" />
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
      }" row-key="id" :scroll="{ x: 1400 }" @change="(pag: any) => handleTableChange(pag.current, pag.pageSize)">
        <template #bodyCell="{ column, record }">
          <!-- 封面列 -->
          <template v-if="column.key === 'cover'">
            <a-image v-if="record.cover" :src="record.cover" :width="72" :height="44"
              style="object-fit: cover; border-radius: 4px" />
            <span v-else style="color: rgba(0, 0, 0, 0.35)">无</span>
          </template>
          <!-- 优先级列 -->
          <template v-else-if="column.key === 'priority'">
            <a-tag v-if="record.priority === 99" color="green">精选</a-tag>
            <a-tag v-else>{{ record.priority ?? 0 }}</a-tag>
          </template>
          <!-- 操作列 -->
          <template v-else-if="column.key === 'action'">
            <a-space>
              <a-button type="link" size="small" @click="handleEdit(record)">编辑</a-button>
              <a-button type="link" size="small" @click="handleFeature(record)">精选</a-button>
              <a-button type="link" danger size="small" @click="handleDelete(record)">
                删除
              </a-button>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>
  </div>
</template>

<style scoped>
.app-manager-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.search-card :deep(.ant-form-inline) {
  flex-wrap: wrap;
  gap: 8px;
}
</style>
