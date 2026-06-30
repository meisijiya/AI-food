<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { listAuditLogs } from '@/api/auditLog'

const list = ref<any[]>([])
const total = ref(0)
const query = ref({ page: 1, size: 50, action: '' })

async function load() {
  const res: any = await listAuditLogs(query.value)
  list.value = res.data?.records || []
  total.value = res.data?.total || 0
}

onMounted(load)
</script>

<template>
  <div class="page-container">
    <el-card>
      <el-form :inline="true" :model="query">
        <el-form-item label="动作">
          <el-input v-model="query.action" placeholder="如 UPDATE_USER_ROLE" clearable />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="load">查询</el-button>
        </el-form-item>
      </el-form>

      <el-table :data="list" stripe>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="actorUsername" label="操作者" width="120" />
        <el-table-column prop="action" label="动作" width="180" />
        <el-table-column prop="targetType" label="对象类型" width="100" />
        <el-table-column prop="targetId" label="对象 ID" width="100" />
        <el-table-column prop="status" label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 'SUCCESS' ? 'success' : 'danger'">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="时间" />
      </el-table>

      <el-pagination
        v-model:current-page="query.page"
        v-model:page-size="query.size"
        :total="total"
        layout="total, prev, pager, next"
        @current-change="load"
        style="margin-top: 20px"
      />
    </el-card>
  </div>
</template>