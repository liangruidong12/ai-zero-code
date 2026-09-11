<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import type { Rule } from 'ant-design-vue/es/form'
import {
  adminUpdateApp,
  getAppVoById,
  updateApp,
} from '@/api/appController.ts'
import { useLoginUserStore } from '@/stores/loginUser.ts'

const route = useRoute()
const router = useRouter()
// 应用 ID 全链路保持字符串，避免大整数转 Number 丢失精度
const appId = route.params.id as string
const loginUserStore = useLoginUserStore()

// 是否为管理员（管理员可修改名称、封面、优先级；普通用户只能修改名称）
const isAdmin = computed(() => loginUserStore.loginUser.userRole === 'admin')

// ============ 应用信息 ============
const loading = ref(false)
const saving = ref(false)
const loadFailed = ref(false)
const noPermission = ref(false)
const app = ref<API.AppVO>()

const formState = reactive<API.AppAdminUpdateRequest>({
  id: appId,
  appName: '',
  cover: '',
  priority: 0,
})

const rules: Record<string, Rule[]> = {
  appName: [{ required: true, message: '请输入应用名称', trigger: 'blur' }],
}

// 加载应用详情
const loadApp = async () => {
  loading.value = true
  try {
    const res = await getAppVoById({ id: appId })
    if (res.data.code === 0 && res.data.data) {
      app.value = res.data.data
      // 权限校验：非管理员只能编辑自己的应用
      if (!isAdmin.value && app.value.userId !== loginUserStore.loginUser.id) {
        noPermission.value = true
        return
      }
      formState.appName = app.value.appName
      formState.cover = app.value.cover
      formState.priority = app.value.priority ?? 0
    } else {
      message.error('应用信息加载失败：' + (res.data.message ?? '未知错误'))
      loadFailed.value = true
    }
  } catch {
    message.error('应用信息加载失败，请稍后重试')
    loadFailed.value = true
  } finally {
    loading.value = false
  }
}

// 提交修改
const handleSubmit = async () => {
  saving.value = true
  try {
    // 管理员：可更新名称、封面、优先级；普通用户：仅可更新名称
    const res = isAdmin.value
      ? await adminUpdateApp(formState)
      : await updateApp({ id: appId, appName: formState.appName })
    if (res.data.code === 0 && res.data.data) {
      message.success('修改成功')
    } else {
      message.error('修改失败：' + (res.data.message ?? '未知错误'))
    }
  } catch {
    message.error('修改失败，请稍后重试')
  } finally {
    saving.value = false
  }
}

onMounted(() => {
  loadApp()
})
</script>

<template>
  <div class="app-edit-page">
    <a-card class="form-card" title="编辑应用信息" :loading="loading">
      <!-- 无权限 -->
      <a-result v-if="noPermission" status="403" title="无权限访问" sub-title="只能编辑自己创建的应用">
        <template #extra>
          <a-button type="primary" @click="router.push('/')">返回主页</a-button>
        </template>
      </a-result>

      <!-- 加载失败 -->
      <a-result v-else-if="loadFailed" status="error" title="应用信息加载失败">
        <template #extra>
          <a-button type="primary" @click="router.push('/')">返回主页</a-button>
        </template>
      </a-result>

      <!-- 编辑表单 -->
      <div v-else>
        <a-descriptions :column="1" size="small" bordered class="app-desc">
          <a-descriptions-item label="应用 ID">{{ app?.id }}</a-descriptions-item>
          <a-descriptions-item label="代码生成类型">
            {{ app?.codeGenType ?? '-' }}
          </a-descriptions-item>
          <a-descriptions-item label="创建者">
            {{ app?.user?.userName ?? app?.userId ?? '-' }}
          </a-descriptions-item>
          <a-descriptions-item label="创建时间">{{ app?.createTime ?? '-' }}</a-descriptions-item>
        </a-descriptions>

        <a-form :model="formState" :rules="rules" layout="vertical" class="edit-form" @finish="handleSubmit">
          <a-form-item label="应用名称" name="appName">
            <a-input v-model:value="formState.appName" placeholder="请输入应用名称" />
          </a-form-item>

          <!-- 管理员可修改封面和优先级 -->
          <template v-if="isAdmin">
            <a-form-item label="应用封面" name="cover">
              <a-input v-model:value="formState.cover" placeholder="请输入封面图片 URL" />
            </a-form-item>
            <a-form-item label="优先级" name="priority">
              <a-input-number v-model:value="formState.priority" :min="0" style="width: 100%" />
              <div class="form-tip">优先级为 99 时，应用将在精选案例中展示</div>
            </a-form-item>
          </template>

          <a-form-item>
            <a-space>
              <a-button type="primary" html-type="submit" :loading="saving">保存修改</a-button>
              <a-button @click="router.push(`/app/chat/${appId}`)">查看对话</a-button>
              <a-button @click="router.push('/')">返回主页</a-button>
            </a-space>
          </a-form-item>
        </a-form>
      </div>
    </a-card>
  </div>
</template>

<style scoped>
.app-edit-page {
  display: flex;
  justify-content: center;
}

.form-card {
  width: 640px;
  max-width: 100%;
}

.app-desc {
  margin-bottom: 24px;
}

.edit-form {
  margin-top: 24px;
}

.form-tip {
  margin-top: 4px;
  font-size: 12px;
  color: rgba(0, 0, 0, 0.45);
}
</style>
