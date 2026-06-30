<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listUsers, updateRole, disableUser, enableUser } from '@/api/user'

const list = ref<any[]>([])
const total = ref(0)
const query = ref({ page: 1, size: 20, keyword: '', role: '', status: undefined as number | undefined })

async function loadData() {
  const res: any = await listUsers(query.value)
  list.value = res.data?.records || []
  total.value = res.data?.total || 0
}

async function onChangeRole(row: any, newRole: string) {
  try {
    await ElMessageBox.confirm(`确认将 ${row.username} 的角色改为 ${newRole}?`, '提示', { type: 'warning' })
    await updateRole(row.id, newRole)
    ElMessage.success('修改成功')
    loadData()
  } catch (e: any) {
    if (e !== 'cancel') ElMessage.error(e.response?.data?.message || '修改失败')
  }
}

async function onToggleStatus(row: any) {
  const action = row.isDeleted === 0 ? '禁用' : '启用'
  try {
    await ElMessageBox.confirm(`确认${action} ${row.username}?`, '提示', { type: 'warning' })
    if (row.isDeleted === 0) await disableUser(row.id)
    else await enableUser(row.id)
    ElMessage.success(`${action}成功`)
    loadData()
  } catch (e: any) {
    if (e !== 'cancel') ElMessage.error(e.response?.data?.message || `${action}失败`)
  }
}

onMounted(loadData)
</script>

<template>
  <div class="page-container">
    <el-card>
      <el-form :inline="true" :model="query">
        <el-form-item label="搜索">
          <el-input v-model="query.keyword" placeholder="用户名/邮箱/昵称" clearable />
        </el-form-item>
        <el-form-item label="角色">
          <el-select v-model="query.role" placeholder="全部" clearable>
            <el-option label="USER" value="USER" />
            <el-option label="ADMIN" value="ADMIN" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部" clearable>
            <el-option label="启用" :value="0" />
            <el-option label="禁用" :value="1" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadData">查询</el-button>
        </el-form-item>
      </el-form>

      <el-table :data="list" stripe>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="username" label="用户名" />
        <el-table-column prop="nickname" label="昵称" />
        <el-table-column prop="email" label="邮箱" />
        <el-table-column label="角色" width="200">
          <template #default="{ row }">
            <el-radio-group :model-value="row.role" @change="(v: string) => onChangeRole(row, v)">
              <el-radio-button label="USER" />
              <el-radio-button label="ADMIN" />
            </el-radio-group>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.isDeleted === 0 ? 'success' : 'danger'">
              {{ row.isDeleted === 0 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120">
          <template #default="{ row }">
            <el-button size="small" :type="row.isDeleted === 0 ? 'danger' : 'primary'" @click="onToggleStatus(row)">
              {{ row.isDeleted === 0 ? '禁用' : '启用' }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="query.page"
        v-model:page-size="query.size"
        :total="total"
        :page-sizes="[20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        @current-change="loadData"
        @size-change="loadData"
        style="margin-top: 20px"
      />
    </el-card>
  </div>
</template>