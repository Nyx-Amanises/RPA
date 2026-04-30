<script setup>
import { reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const loading = ref(false)
const formRef = ref()

const form = reactive({
  username: 'admin',
  password: '123456'
})

const rules = {
  username: [{ required: true, message: '请输入账号', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

async function handleLogin() {
  await formRef.value.validate()
  loading.value = true
  try {
    await authStore.loginByPassword(form)
    await authStore.loadCurrentUser(true)
    ElMessage.success('登录成功')
    router.replace(route.query.redirect || '/system/profile')
  } catch (error) {
    ElMessage.error(error.message || '登录失败')
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="login-page">
    <div class="login-hero">
      <div class="login-copy">
        <div class="login-brand-lockup" aria-label="NOVA RPA">
          <span class="login-logo-mark">R</span>
          <span class="login-badge">NOVA RPA</span>
        </div>
        <h1>智控 RPA<br />管理平台</h1>
        <p>接入登录、自动鉴权、系统管理联调的一体化前端入口。</p>
      </div>
      <el-card shadow="hover" class="login-card">
        <template #header>
          <div class="login-card-title">账号登录</div>
        </template>
        <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
          <el-form-item label="账号" prop="username">
            <el-input v-model="form.username" placeholder="请输入账号" />
          </el-form-item>
          <el-form-item label="密码" prop="password">
            <el-input v-model="form.password" type="password" show-password placeholder="请输入密码" @keyup.enter="handleLogin" />
          </el-form-item>
          <el-button type="primary" class="login-submit" :loading="loading" @click="handleLogin">登录</el-button>
        </el-form>
      </el-card>
    </div>
  </div>
</template>
