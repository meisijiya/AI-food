<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { listModels } from '@/api/model'

const models = ref<any[]>([])
onMounted(async () => {
  const res: any = await listModels()
  models.value = res.data || []
})
</script>

<template>
  <div class="page-container">
    <el-card>
      <el-table :data="models" stripe>
        <el-table-column prop="name" label="模型名" />
        <el-table-column prop="baseUrl" label="API URL" />
        <el-table-column prop="active" label="状态">
          <template #default="{ row }">
            <el-tag :type="row.active ? 'success' : 'info'">
              {{ row.active ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>