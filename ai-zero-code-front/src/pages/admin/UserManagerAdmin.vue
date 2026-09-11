<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { message, Modal } from 'ant-design-vue'
import type { TableColumnsType } from 'ant-design-vue'
import {
    deleteUser,
    listUserVoByPage,
    updateUser,
} from '@/api/userController.ts'

// ============ 搜索条件 ============
const searchParams = reactive<API.UserQueryRequest>({
    pageNum: 1,
    pageSize: 10,
    userName: '',
    userAccount: '',
    userRole: '',
})

// ============ 表格数据 ============
const dataList = ref<API.UserVO[]>([])
const total = ref(0)
const loading = ref(false)

// 加载分页数据
const loadData = async () => {
    loading.value = true
    try {
        const res = await listUserVoByPage(searchParams)
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
    searchParams.userName = ''
    searchParams.userAccount = ''
    searchParams.userRole = ''
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
const columns: TableColumnsType<API.UserVO> = [
    { title: 'ID', dataIndex: 'id', width: 80 },
    { title: '账号', dataIndex: 'userAccount', width: 150 },
    { title: '用户名', dataIndex: 'userName', width: 150 },
    { title: '头像', dataIndex: 'userAvatar', width: 80, key: 'userAvatar' },
    { title: '简介', dataIndex: 'userProfile', ellipsis: true },
    { title: '角色', dataIndex: 'userRole', width: 100, key: 'userRole' },
    { title: '创建时间', dataIndex: 'createTime', width: 180 },
    { title: '操作', key: 'action', width: 160, fixed: 'right' },
]

// 角色标签颜色映射
const roleColorMap: Record<string, string> = {
    admin: 'red',
    user: 'blue',
}
const getRoleColor = (role?: string) => roleColorMap[role ?? 'user'] ?? 'default'

// ============ 删除 ============
const handleDelete = (record: API.UserVO) => {
    Modal.confirm({
        title: '确认删除',
        content: `确定要删除用户「${record.userName ?? record.userAccount}」吗？`,
        okType: 'danger',
        onOk: async () => {
            try {
                const res = await deleteUser({ id: record.id })
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

// ============ 编辑 ============
const editVisible = ref(false)
const editForm = reactive<API.UserUpdateRequest>({})

const handleEdit = (record: API.UserVO) => {
    editForm.id = record.id
    editForm.userName = record.userName
    editForm.userAvatar = record.userAvatar
    editForm.userProfile = record.userProfile
    editForm.userRole = record.userRole
    editVisible.value = true
}

const handleEditSubmit = async () => {
    try {
        const res = await updateUser(editForm)
        if (res.data.code === 0 && res.data.data) {
            message.success('修改成功')
            editVisible.value = false
            loadData()
        } else {
            message.error('修改失败：' + (res.data.message ?? '未知错误'))
        }
    } catch {
        message.error('修改失败，请稍后重试')
    }
}

// 页面初始化加载数据
onMounted(() => {
    loadData()
})
</script>

<template>
    <div class="user-manager-page">
        <!-- 搜索区 -->
        <a-card class="search-card">
            <a-form layout="inline">
                <a-form-item label="用户名">
                    <a-input v-model:value="searchParams.userName" placeholder="请输入用户名" allow-clear />
                </a-form-item>
                <a-form-item label="账号">
                    <a-input v-model:value="searchParams.userAccount" placeholder="请输入账号" allow-clear />
                </a-form-item>
                <a-form-item label="角色">
                    <a-select v-model:value="searchParams.userRole" placeholder="请选择角色" style="width: 120px"
                        allow-clear>
                        <a-select-option value="user">普通用户</a-select-option>
                        <a-select-option value="admin">管理员</a-select-option>
                    </a-select>
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
                showTotal: (t: number) => `共 ${t} 条`,
            }" row-key="id" @change="(pag: any) => handleTableChange(pag.current, pag.pageSize)">
                <template #bodyCell="{ column, record }">
                    <!-- 头像列 -->
                    <template v-if="column.key === 'userAvatar'">
                        <a-avatar :src="record.userAvatar" :size="32" />
                    </template>
                    <!-- 角色列 -->
                    <template v-else-if="column.key === 'userRole'">
                        <a-tag :color="getRoleColor(record.userRole)">
                            {{ record.userRole === 'admin' ? '管理员' : '普通用户' }}
                        </a-tag>
                    </template>
                    <!-- 操作列 -->
                    <template v-else-if="column.key === 'action'">
                        <a-space>
                            <a-button type="link" size="small" @click="handleEdit(record)">编辑</a-button>
                            <a-button type="link" danger size="small" @click="handleDelete(record)">删除</a-button>
                        </a-space>
                    </template>
                </template>
            </a-table>
        </a-card>

        <!-- 编辑弹窗 -->
        <a-modal v-model:open="editVisible" title="编辑用户" ok-text="保存" cancel-text="取消" @ok="handleEditSubmit">
            <a-form :model="editForm" layout="vertical">
                <a-form-item label="用户名" name="userName">
                    <a-input v-model:value="editForm.userName" placeholder="请输入用户名" />
                </a-form-item>
                <a-form-item label="头像地址" name="userAvatar">
                    <a-input v-model:value="editForm.userAvatar" placeholder="请输入头像 URL" />
                </a-form-item>
                <a-form-item label="角色" name="userRole">
                    <a-select v-model:value="editForm.userRole" placeholder="请选择角色">
                        <a-select-option value="user">普通用户</a-select-option>
                        <a-select-option value="admin">管理员</a-select-option>
                    </a-select>
                </a-form-item>
                <a-form-item label="简介" name="userProfile">
                    <a-textarea v-model:value="editForm.userProfile" placeholder="请输入用户简介" :rows="3" />
                </a-form-item>
            </a-form>
        </a-modal>
    </div>
</template>

<style scoped>
.user-manager-page {
    display: flex;
    flex-direction: column;
    gap: 16px;
}

.search-card :deep(.ant-form-inline) {
    flex-wrap: wrap;
    gap: 8px;
}
</style>
