<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getStats } from '@/api/tokenUsage'

const groupBy = ref('day')
const stats = ref<any[]>([])

async function load() {
  const res: any = await getStats({ groupBy: groupBy.value })
  stats.value = res.data || []
}

onMounted(load)
</script>

<template>
  <div class="page-container">
    <el-card>
      <el-radio-group v-model="groupBy" @change="load">
        <el-radio-button label="day">按日</el-radio-button>
        <el-radio-button label="model">按模型</el-radio-button>
        <el-radio-button label="user">按 session</el-radio-button>
      </el-radio-group>

      <el-table :data="stats" stripe style="margin-top: 20px">
        <el-table-column prop="key" label="分组" />
        <el-table-column prop="promptTokens" label="Prompt Tokens" />
        <el-table-column prop="completionTokens" label="Completion Tokens" />
        <el-table-column prop="totalTokens" label="Total Tokens" />
        <el-table-column prop="count" label="记录数" />
      </el-table>
    </el-card>
  </div>
</template>