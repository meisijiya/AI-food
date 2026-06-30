<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { listRecommendations } from '@/api/recommendation'

const list = ref<any[]>([])
const total = ref(0)
const query = ref({ page: 1, size: 20 })

async function load() {
  const res: any = await listRecommendations(query.value)
  list.value = res.data?.records || []
  total.value = res.data?.total || 0
}

onMounted(load)
</script>

<template>
  <div class="page-container">
    <el-card>
      <el-table :data="list" stripe>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="userId" label="用户" width="80" />
        <el-table-column prop="mode" label="模式" width="80" />
        <el-table-column prop="foodName" label="推荐食物" />
        <el-table-column prop="accepted" label="接受">
          <template #default="{ row }">
            <el-tag :type="row.accepted ? 'success' : 'info'">
              {{ row.accepted ? '是' : '否' }}
            </el-tag>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>