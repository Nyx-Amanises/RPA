<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { fetchProfile, savePassword, saveProfile, uploadProfileAvatar } from '../api/system'
import { useAuthStore } from '../stores/auth'

const loading = ref(false)
const profile = ref({})
const activeTab = ref('base')
const profileFormRef = ref()
const passwordFormRef = ref()
const uploadInputRef = ref()
const authStore = useAuthStore()

const profileForm = reactive({
  realName: '',
  email: '',
  mobile: ''
})

const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const profileRules = {
  realName: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  email: [{ required: true, message: '请输入邮箱', trigger: 'blur' }],
  mobile: [{ required: true, message: '请输入手机号', trigger: 'blur' }]
}

const passwordRules = {
  oldPassword: [{ required: true, message: '请输入旧密码', trigger: 'blur' }],
  newPassword: [{ required: true, message: '请输入新密码', trigger: 'blur' }],
  confirmPassword: [
    { required: true, message: '请输入确认密码', trigger: 'blur' },
    {
      validator: (_rule, value, callback) => {
        if (value !== passwordForm.newPassword) {
          callback(new Error('两次输入的新密码不一致'))
          return
        }
        callback()
      },
      trigger: 'blur'
    }
  ]
}

const avatarText = computed(() => profile.value.realName?.slice(0, 1) || profile.value.username?.slice(0, 1) || 'U')

async function loadProfile() {
  loading.value = true
  try {
    const res = await fetchProfile()
    profile.value = res.data || {}
    Object.assign(profileForm, {
      realName: profile.value.realName || '',
      email: profile.value.email || '',
      mobile: profile.value.mobile || ''
    })
  } finally {
    loading.value = false
  }
}

async function submitProfile() {
  try {
    await profileFormRef.value.validate()
    await saveProfile(profileForm)
    ElMessage.success('基本信息已保存')
    await loadProfile()
    authStore.loadCurrentUser(true).catch(() => {})
  } catch (error) {
    ElMessage.error(error.message || '保存失败')
  }
}

async function submitPassword() {
  try {
    await passwordFormRef.value.validate()
    await savePassword(passwordForm)
    ElMessage.success('密码修改成功')
    passwordFormRef.value.resetFields()
  } catch (error) {
    ElMessage.error(error.message || '密码修改失败')
  }
}

function resetProfile() {
  Object.assign(profileForm, {
    realName: profile.value.realName || '',
    email: profile.value.email || '',
    mobile: profile.value.mobile || ''
  })
}

function openAvatarPicker() {
  uploadInputRef.value?.click()
}

async function handleAvatarChange(event) {
  const file = event.target.files?.[0]
  event.target.value = ''
  if (!file) {
    return
  }

  const allowTypes = ['image/jpeg', 'image/png', 'image/gif', 'image/webp']
  if (!allowTypes.includes(file.type)) {
    ElMessage.error('仅支持 jpg、jpeg、png、gif、webp 格式图片')
    return
  }

  loading.value = true
  try {
    await uploadProfileAvatar(file)
    ElMessage.success('头像上传成功')
    await loadProfile()
    authStore.loadCurrentUser(true).catch(() => {})
  } catch (error) {
    ElMessage.error(error.message || '头像上传失败')
  } finally {
    loading.value = false
  }
}

onMounted(loadProfile)
</script>

<template>
  <div v-loading="loading" class="profile-page">
    <el-row :gutter="24">
      <el-col :lg="8" :md="24">
        <el-card shadow="hover" class="panel-card">
          <template #header>
            <div class="panel-title">基本信息</div>
          </template>
          <div class="profile-summary">
            <div v-if="profile.avatarUrl" class="profile-avatar profile-avatar-image">
              <img :src="profile.avatarUrl" alt="avatar" class="profile-avatar-img" />
            </div>
            <div v-else class="profile-avatar">{{ avatarText }}</div>
            <button class="link-button" @click="openAvatarPicker">更换头像</button>
            <input
              ref="uploadInputRef"
              type="file"
              accept=".jpg,.jpeg,.png,.gif,.webp"
              class="hidden-file-input"
              @change="handleAvatarChange"
            />
          </div>
          <div class="info-grid">
            <div class="info-row">
              <span class="info-label">用户名</span>
              <span>{{ profile.username || '--' }}</span>
            </div>
            <div class="info-row">
              <span class="info-label">姓名</span>
              <span>{{ profile.realName || '--' }}</span>
            </div>
            <div class="info-row">
              <span class="info-label">邮箱</span>
              <span>{{ profile.email || '--' }}</span>
            </div>
            <div class="info-row">
              <span class="info-label">手机号</span>
              <span>{{ profile.mobile || '--' }}</span>
            </div>
            <div class="info-row">
              <span class="info-label">角色</span>
              <span>
                <el-tag type="danger" effect="plain">{{ (profile.roleNames || []).join(' / ') || '--' }}</el-tag>
              </span>
            </div>
            <div class="info-row">
              <span class="info-label">创建时间</span>
              <span>{{ profile.createTime || '--' }}</span>
            </div>
          </div>
        </el-card>
      </el-col>

      <el-col :lg="16" :md="24">
        <el-card shadow="hover" class="panel-card">
          <template #header>
            <div class="panel-title">修改信息</div>
          </template>
          <el-tabs v-model="activeTab" class="clean-tabs">
            <el-tab-pane label="基本信息" name="base">
              <el-form ref="profileFormRef" :model="profileForm" :rules="profileRules" label-width="88px" class="form-wrap">
                <el-form-item label="姓名" prop="realName" required>
                  <el-input v-model="profileForm.realName" placeholder="请输入姓名" />
                </el-form-item>
                <el-form-item label="邮箱" prop="email" required>
                  <el-input v-model="profileForm.email" placeholder="请输入邮箱" />
                </el-form-item>
                <el-form-item label="手机号" prop="mobile" required>
                  <el-input v-model="profileForm.mobile" placeholder="请输入手机号" />
                </el-form-item>
                <el-form-item>
                  <el-button type="primary" @click="submitProfile">保存</el-button>
                  <el-button @click="resetProfile">重置</el-button>
                </el-form-item>
              </el-form>
            </el-tab-pane>
            <el-tab-pane label="修改密码" name="password">
              <el-form ref="passwordFormRef" :model="passwordForm" :rules="passwordRules" label-width="96px" class="form-wrap">
                <el-form-item label="旧密码" prop="oldPassword" required>
                  <el-input v-model="passwordForm.oldPassword" type="password" show-password />
                </el-form-item>
                <el-form-item label="新密码" prop="newPassword" required>
                  <el-input v-model="passwordForm.newPassword" type="password" show-password />
                </el-form-item>
                <el-form-item label="确认密码" prop="confirmPassword" required>
                  <el-input v-model="passwordForm.confirmPassword" type="password" show-password />
                </el-form-item>
                <el-form-item>
                  <el-button type="primary" @click="submitPassword">确认修改</el-button>
                </el-form-item>
              </el-form>
            </el-tab-pane>
          </el-tabs>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>
