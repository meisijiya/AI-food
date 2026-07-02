<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listUsers, updateRole, disableUser, enableUser } from '@/api/user'
import SearchForm from '@/components/SearchForm.vue'

const list = ref<any[]>([])
const total = ref(0)
const loading = ref(false)
const query = ref({
  page: 1,
  size: 20,
  keyword: '' as string,
  role: '' as string,
  status: undefined as number | undefined,
  startDate: '' as string
})

async function loadData() {
  loading.value = true
  try {
    const params: any = { page: query.value.page, size: query.value.size }
    if (query.value.keyword) params.keyword = query.value.keyword
    if (query.value.role) params.role = query.value.role
    if (query.value.status !== undefined) params.status = query.value.status
    if (query.value.startDate) params.startDate = query.value.startDate + 'T00:00:00'
    const res: any = await listUsers(params)
    list.value = res.data?.records || []
    total.value = res.data?.total || 0
  } finally {
    loading.value = false
  }
}

function onSearch() {
  query.value.page = 1
  loadData()
}

function onReset() {
  query.value = { page: 1, size: 20, keyword: '', role: '', status: undefined, startDate: '' }
  loadData()
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
    <SearchForm @search="onSearch" @reset="onReset">
      <el-form-item label="搜索">
        <el-input v-model="query.keyword" placeholder="用户名/邮箱/昵称" clearable style="width: 200px" />
      </el-form-item>
      <el-form-item label="角色">
        <el-select v-model="query.role" placeholder="全部" clearable style="width: 130px">
          <el-option label="USER" value="USER" />
          <el-option label="ADMIN" value="ADMIN" />
        </el-select>
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="query.status" placeholder="全部" clearable style="width: 120px">
          <el-option label="启用" :value="0" />
          <el-option label="禁用" :value="1" />
        </el-select>
      </el-form-item>
      <el-form-item label="注册日期">
        <el-date-picker v-model="query.startDate" type="date" value-format="YYYY-MM-DD" placeholder="起" style="width: 150px" />
      </el-form-item>
    </SearchForm>

    <el-card class="page-card">
      <div class="total-line">
        共 <b class="total-num">{{ total }}</b> 个用户
      </div>
      <el-table :data="list" stripe v-loading="loading">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="username" label="用户名" min-width="120" />
        <el-table-column prop="nickname" label="昵称" min-width="120" />
        <el-table-column prop="email" label="邮箱" min-width="200" show-overflow-tooltip />
        <el-table-column label="角色" width="180">
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
        <el-table-column prop="createdAt" label="注册时间" width="170" />
        <el-table-column label="操作" width="120" fixed="right">
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
        class="pagination-bar"
      />
    </el-card>
  </div>
</template>

<style scoped>
/* === 列表卡片(统一圆角 + 软阴影)==*/
.page-card {
  border-radius: var(--radius-md);
}
.page-card :deep(.el-card__body) {
  box-shadow: var(--shadow-sm);
}

/* === 顶部统计行 === */
.total-line {
  margin-bottom: var(--space-md);
  color: var(--color-text-secondary);
  font-size: var(--font-base);
}
.total-num { color: var(--color-primary); font-weight: 600; }

/* === 分页 === */
.pagination-bar {
  margin-top: var(--space-md);
  justify-content: flex-end;
  display: flex;
}
</style>