<template>
  <div class="token-quota-view">
    <h2>Token 限额管理</h2>

    <el-card class="global-config">
      <template #header><span>全局默认限额</span></template>
      <el-form inline>
        <el-form-item label="每日 Token 限额">
          <el-input-number v-model="globalLimit" :min="0" :step="10000" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="saveGlobal">保存</el-button>
          <el-button @click="loadGlobal">刷新</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card class="user-override">
      <template #header><span>单用户覆盖</span></template>
      <el-form inline>
        <el-form-item label="用户 ID">
          <el-input-number v-model="userId" :min="1" />
        </el-form-item>
        <el-form-item label="每日 Token 限额">
          <el-input-number v-model="userLimit" :min="0" :step="10000" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="saveUser">保存</el-button>
          <el-button @click="loadUser">查询</el-button>
          <el-button type="danger" @click="deleteUser">删除</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { tokenQuotaApi } from '@/api/tokenQuota'

const globalLimit = ref(1000000)
const userId = ref<number>()
const userLimit = ref<number>()

async function loadGlobal() {
  try {
    const res: any = await tokenQuotaApi.getGlobalTokenLimit()
    globalLimit.value = parseInt(res.configValue || '1000000')
  } catch {
    ElMessage.error('加载全局限额失败')
  }
}

async function saveGlobal() {
  try {
    await tokenQuotaApi.updateGlobalTokenLimit(globalLimit.value)
    ElMessage.success('全局限额已更新')
  } catch {
    ElMessage.error('更新失败')
  }
}

async function loadUser() {
  if (!userId.value) return
  try {
    const res: any = await tokenQuotaApi.getUserTokenQuota(userId.value)
    userLimit.value = res.dailyTokenLimit || 0
  } catch {
    ElMessage.error('查询失败')
  }
}

async function saveUser() {
  if (!userId.value || !userLimit.value) return
  try {
    await tokenQuotaApi.updateUserTokenQuota(userId.value, userLimit.value)
    ElMessage.success('用户限额已更新')
  } catch {
    ElMessage.error('更新失败')
  }
}

async function deleteUser() {
  if (!userId.value) return
  try {
    await tokenQuotaApi.deleteUserTokenQuota(userId.value)
    ElMessage.success('已删除用户覆盖')
    userLimit.value = undefined
  } catch {
    ElMessage.error('删除失败')
  }
}

onMounted(() => {
  loadGlobal()
})
</script>

<style scoped>
.token-quota-view { padding: 20px; }
.global-config, .user-override { margin-bottom: 20px; }
h2 { margin-bottom: 20px; }
</style>
