<script setup lang="ts">
/**
 * 模型配置列表 — 显示已注册的 LLM 模型与启用状态。
 *
 * ponytail: 后端 listModels() 不带分页参数,这里 client-side 切页
 * (模型数 < 50,真要多了再 server-side)。
 */
import { ref, computed, onMounted } from 'vue'
import { listModels } from '@/api/model'

const allModels = ref<any[]>([])
const page = ref(1)
const size = ref(20)

async function load() {
  const res: any = await listModels()
  allModels.value = res.data || []
}

const models = computed(() => allModels.value.slice((page.value - 1) * size.value, page.value * size.value))
const total = computed(() => allModels.value.length)

onMounted(load)
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

      <el-pagination
        v-model:current-page="page"
        v-model:page-size="size"
        :total="total"
        :page-sizes="[20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        @current-change="models"
        @size-change="models"
        style="margin-top: 16px; justify-content: flex-end"
      />
    </el-card>
  </div>
</template>