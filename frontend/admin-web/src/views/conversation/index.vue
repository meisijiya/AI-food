<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listConversations, getConversationMessages, deleteConversation } from '@/api/conversation'

const list = ref<any[]>([])
const total = ref(0)
const selected = ref<any>(null)
const messages = ref<any[]>([])
const query = ref({ page: 1, size: 20 })

async function loadList() {
  const res: any = await listConversations(query.value)
  list.value = res.data?.records || []
  total.value = res.data?.total || 0
}

async function selectConversation(c: any) {
  selected.value = c
  const res: any = await getConversationMessages(c.id, { page: 1, size: 50 })
  messages.value = res.data?.records || []
}

async function onDelete(c: any) {
  try {
    await ElMessageBox.confirm(`确认删除对话 #${c.id}?`, '提示', { type: 'warning' })
    await deleteConversation(c.id)
    ElMessage.success('已删除')
    loadList()
  } catch (e: any) {
    if (e !== 'cancel') ElMessage.error(e.response?.data?.message || '删除失败')
  }
}

onMounted(loadList)
</script>

<template>
  <div class="page-container">
    <el-row :gutter="20">
      <el-col :span="10">
        <el-card>
          <el-table :data="list" @row-click="selectConversation" highlight-current-row stripe>
            <el-table-column prop="id" label="ID" width="60" />
            <el-table-column prop="userId" label="用户" width="80" />
            <el-table-column prop="mode" label="模式" width="80" />
            <el-table-column prop="status" label="状态" width="80" />
            <el-table-column label="操作" width="80">
              <template #default="{ row }">
                <el-button size="small" type="danger" link @click.stop="onDelete(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
      <el-col :span="14">
        <el-card>
          <h3 v-if="selected">对话 #{{ selected.id }} 的消息</h3>
          <h3 v-else>请选择左侧对话</h3>
          <div v-if="messages.length" style="max-height: 500px; overflow-y: auto">
            <div v-for="m in messages" :key="m.id" style="padding: 8px; margin-bottom: 8px; border-bottom: 1px solid #eee">
              <el-tag size="small">{{ m.role || 'msg' }}</el-tag>
              <p style="margin: 4px 0">{{ m.content || m.text || JSON.stringify(m) }}</p>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>