<script setup lang="ts">
import { reactive } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import type { Rule } from 'ant-design-vue/es/form'
import { userLogin } from '@/api/userController.ts'
import { useLoginUserStore } from '@/stores/loginUser.ts'

const router = useRouter()
const route = useRoute()
const loginUserStore = useLoginUserStore()

const formState = reactive<API.UserLoginRequest>({
  userAccount: '',
  userPassword: '',
})

const rules: Record<string, Rule[]> = {
  userAccount: [
    { required: true, message: '请输入账号', trigger: 'blur' },
    { min: 4, max: 20, message: '账号长度为 4 - 20 位', trigger: 'blur' },
  ],
  userPassword: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 8, max: 20, message: '密码长度为 8 - 20 位', trigger: 'blur' },
  ],
}

// 提交登录（@finish 会在表单校验通过后触发）
const handleSubmit = async (values: API.UserLoginRequest) => {
  try {
    const res = await userLogin(values)
    if (res.data.code === 0 && res.data.data) {
      loginUserStore.setLoginUser(res.data.data)
      message.success('登录成功')
      // 登录前被拦截的页面优先跳回，否则回到首页
      router.push((route.query.redirect as string) ?? '/')
    } else {
      message.error('登录失败：' + (res.data.message ?? '未知错误'))
    }
  } catch {
    message.error('登录失败，请稍后重试')
  }
}
</script>

<template>
  <div class="user-login-page">
    <a-card title="用户登录" class="form-card">
      <a-form :model="formState" :rules="rules" layout="vertical" @finish="handleSubmit">
        <a-form-item label="账号" name="userAccount">
          <a-input v-model:value="formState.userAccount" placeholder="请输入账号" />
        </a-form-item>
        <a-form-item label="密码" name="userPassword">
          <a-input-password v-model:value="formState.userPassword" placeholder="请输入密码" />
        </a-form-item>
        <a-form-item>
          <a-button type="primary" html-type="submit" block>登录</a-button>
        </a-form-item>
      </a-form>
      <div class="form-footer">
        还没有账号？<router-link to="/user/register">去注册</router-link>
      </div>
    </a-card>
  </div>
</template>

<style scoped>
.user-login-page {
  display: flex;
  justify-content: center;
}

.form-card {
  width: 360px;
  max-width: 100%;
}

.form-footer {
  text-align: center;
  color: rgba(0, 0, 0, 0.45);
}
</style>
