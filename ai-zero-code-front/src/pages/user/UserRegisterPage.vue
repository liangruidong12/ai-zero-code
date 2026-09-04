<script setup lang="ts">
import { reactive } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import type { Rule } from 'ant-design-vue/es/form'
import { userRegister } from '@/api/userController.ts'

const router = useRouter()

const formState = reactive<API.UserRegisterRequest>({
  userAccount: '',
  userPassword: '',
  checkPassword: '',
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
  checkPassword: [
    { required: true, message: '请确认密码', trigger: 'blur' },
    {
      validator: (_rule: Rule, value: string) => {
        if (value !== formState.userPassword) {
          return Promise.reject('两次输入的密码不一致')
        }
        return Promise.resolve()
      },
      trigger: 'blur',
    },
  ],
}

// 提交注册（@finish 会在表单校验通过后触发）
const handleSubmit = async (values: API.UserRegisterRequest) => {
  try {
    const res = await userRegister(values)
    if (res.data.code === 0 && res.data.data) {
      message.success('注册成功，请登录')
      router.push('/user/login')
    } else {
      message.error('注册失败：' + (res.data.message ?? '未知错误'))
    }
  } catch {
    message.error('注册失败，请稍后重试')
  }
}
</script>

<template>
  <div class="user-register-page">
    <a-card title="用户注册" class="form-card">
      <a-form :model="formState" :rules="rules" layout="vertical" @finish="handleSubmit">
        <a-form-item label="账号" name="userAccount">
          <a-input v-model:value="formState.userAccount" placeholder="请输入账号（至少 4 位）" />
        </a-form-item>
        <a-form-item label="密码" name="userPassword">
          <a-input-password v-model:value="formState.userPassword" placeholder="请输入密码（至少 8 位）" />
        </a-form-item>
        <a-form-item label="确认密码" name="checkPassword">
          <a-input-password v-model:value="formState.checkPassword" placeholder="请再次输入密码" />
        </a-form-item>
        <a-form-item>
          <a-button type="primary" html-type="submit" block>注册</a-button>
        </a-form-item>
      </a-form>
      <div class="form-footer">
        已有账号？<router-link to="/user/login">去登录</router-link>
      </div>
    </a-card>
  </div>
</template>

<style scoped>
.user-register-page {
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
