<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { listAuditLogs } from '@/api/auditLog'
import SearchForm from '@/components/SearchForm.vue'

const list = ref<any[]>([])
const total = ref(0)
const loading = ref(false)
const query = ref({
  page: 1,
  size: 50,
  actorId: undefined as number | undefined,
  action: '' as string,
  status: '' as string,  // '' | 'SUCCESS' | 'FAIL'
  startDate: '' as string,
  endDate: '' as string
})

const actionOptions = [
  { label: '修改用户角色', value: 'UPDATE_USER_ROLE' },
  { label: '禁用用户', value: 'DISABLE_USER' },
  { label: '启用用户', value: 'ENABLE_USER' },
  { label: '删除对话', value: 'DELETE_CONVERSATION' }
]

async function load() {
  loading.value = true
  try {
    const params: any = { page: query.value.page, size: query.value.size }
    if (query.value.actorId) params.actorId = query.value.actorId
    if (query.value.action) params.action = query.value.action
    if (query.value.status) params.status = query.value.status
    if (query.value.startDate) params.startDate = query.value.startDate
    if (query.value.endDate) params.endDate = query.value.endDate
    const res: any = await listAuditLogs(params)
    list.value = res.data?.records || []
    total.value = res.data?.total || 0
  } finally {
    loading.value = false
  }
}

function onSearch() {
  query.value.page = 1
  load()
}

function onReset() {
  query.value = { page: 1, size: 50, actorId: undefined, action: '', status: '', startDate: '', endDate: '' }
  load()
}

onMounted(load)
</script>

<template>
  <div class="page-container">
    <SearchForm @search="onSearch" @reset="onReset">
      <el-form-item label="操作者 ID">
        <el-input v-model.number="query.actorId" placeholder="精确匹配" clearable style="width: 130px" />
      </el-form-item>
      <el-form-item label="动作">
        <el-select v-model="query.action" placeholder="全部" clearable filterable style="width: 180px">
          <el-option v-for="o in actionOptions" :key="o.value" :label="o.label" :value="o.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="query.status" placeholder="全部" clearable style="width: 120px">
          <el-option label="成功" value="SUCCESS" />
          <el-option label="失败" value="FAIL" />
        </el-select>
      </el-form-item>
      <el-form-item label="起始日期">
        <el-date-picker v-model="query.startDate" type="date" value-format="YYYY-MM-DD" placeholder="起" style="width: 140px" />
      </el-form-item>
      <el-form-item label="结束日期">
        <el-date-picker v-model="query.endDate" type="date" value-format="YYYY-MM-DD" placeholder="止" style="width: 140px" />
      </el-form-item>
    </SearchForm>

    <el-card>
      <div style="margin-bottom: 12px; color: #606266">
        共 <b style="color: #409eff">{{ total }}</b> 条审计记录
      </div>
      <el-table :data="list" stripe v-loading="loading">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="actorUsername" label="操作者" width="140" />
        <el-table-column prop="action" label="动作" width="180">
          <template #default="{ row }">
            <el-tag size="small" effect="plain">{{ row.action }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="targetType" label="对象类型" width="100" />
        <el-table-column prop="targetId" label="对象 ID" width="100" />
        <el-table-column prop="ip" label="IP" width="130" />
        <el-table-column label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 'SUCCESS' ? 'success' : 'danger'" size="small">
              {{ row.status === 'SUCCESS' ? '成功' : '失败' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="时间" width="170" />
        <el-table-column label="错误信息" min-width="200" show-overflow-tooltip>
          <template #default="{ row }">
            <span v-if="row.errorMessage" style="color: #f56c6c">{{ row.errorMessage }}</span>
            <span v-else style="color: #c0c4cc">-</span>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="query.page"
        v-model:page-size="query.size"
        :total="total"
        :page-sizes="[20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        @current-change="load"
        @size-change="load"
        style="margin-top: 16px; justify-content: flex-end"
      />
    </el-card>
  </div>
</template>