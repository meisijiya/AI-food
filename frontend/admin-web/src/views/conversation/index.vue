<script setup lang="ts">
/**
 * AI 对话管理 — 左列表 + 右消息详情,支持按 user/mode/status/日期过滤。
 *
 * QaRecord 字段(questionType / aiQuestion / userAnswer),不再 fallback 到 JSON.stringify:
 * ponytail: 之前用 JSON.stringify 兜底会泄露 version / questionOrder 等内部字段。
 */
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listConversations, getConversationMessages, deleteConversation } from '@/api/conversation'
import SearchForm from '@/components/SearchForm.vue'

const list = ref<any[]>([])
const total = ref(0)
const loading = ref(false)
const selected = ref<any>(null)
const messages = ref<any[]>([])
const msgTotal = ref(0)
const msgLoading = ref(false)

const query = ref({
  page: 1,
  size: 20,
  userId: undefined as number | undefined,
  mode: '' as string,
  status: '' as string,
  startDate: '' as string,
  endDate: '' as string
})

const msgQuery = ref({ page: 1, size: 50 })

async function loadList() {
  loading.value = true
  try {
    const params: any = { page: query.value.page, size: query.value.size }
    if (query.value.userId !== undefined && query.value.userId !== null) params.userId = query.value.userId
    if (query.value.mode) params.mode = query.value.mode
    if (query.value.status) params.status = query.value.status
    if (query.value.startDate) params.startDate = query.value.startDate
    if (query.value.endDate) params.endDate = query.value.endDate
    const res: any = await listConversations(params)
    list.value = res.data?.records || []
    total.value = res.data?.total || 0
  } finally {
    loading.value = false
  }
}

function onSearch() {
  query.value.page = 1
  loadList()
}

function onReset() {
  query.value = { page: 1, size: 20, userId: undefined, mode: '', status: '', startDate: '', endDate: '' }
  loadList()
}

async function loadMessages(c: any) {
  msgLoading.value = true
  try {
    const res: any = await getConversationMessages(c.id, msgQuery.value)
    messages.value = res.data?.records || []
    msgTotal.value = res.data?.total || 0
  } finally {
    msgLoading.value = false
  }
}

async function selectConversation(c: any) {
  selected.value = c
  msgQuery.value.page = 1
  await loadMessages(c)
}

async function onDelete(c: any) {
  try {
    await ElMessageBox.confirm(`确认删除对话 #${c.id}?`, '提示', { type: 'warning' })
    await deleteConversation(c.id)
    ElMessage.success('已删除')
    await loadList()
    if (selected.value?.id === c.id) {
      selected.value = null
      messages.value = []
    }
  } catch (e: any) {
    if (e !== 'cancel') ElMessage.error(e.response?.data?.message || '删除失败')
  }
}

/** 将 qa_record.questionType 映射为管理员易读的角色标签 */
function roleLabel(t: string | undefined): string {
  switch (t) {
    case 'question': return 'AI 提问'
    case 'chat': return '对话'
    case '2question': return '追问'
    case 'interrupt': return '中断'
    default: return t || 'msg'
  }
}

/** 从 qa_record 抽取可读正文,避免 JSON.stringify 暴露内部字段 */
function messageBody(m: any): string {
  if (m.aiQuestion && m.userAnswer) return `Q: ${m.aiQuestion}\nA: ${m.userAnswer}`
  if (m.aiQuestion) return m.aiQuestion as string
  if (m.userAnswer) return m.userAnswer as string
  return '[空消息]'
}

onMounted(loadList)
</script>

<template>
  <div class="page-container">
    <SearchForm @search="onSearch" @reset="onReset">
      <el-form-item label="用户 ID">
        <el-input v-model.number="query.userId" placeholder="精确匹配" clearable style="width: 130px" />
      </el-form-item>
      <el-form-item label="模式">
        <el-select v-model="query.mode" placeholder="全部" clearable style="width: 130px">
          <el-option label="inertia" value="inertia" />
          <el-option label="random" value="random" />
        </el-select>
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="query.status" placeholder="全部" clearable style="width: 130px">
          <el-option label="active" value="active" />
          <el-option label="completed" value="completed" />
        </el-select>
      </el-form-item>
      <el-form-item label="起始日期">
        <el-date-picker v-model="query.startDate" type="date" value-format="YYYY-MM-DD" placeholder="起" style="width: 140px" />
      </el-form-item>
      <el-form-item label="截止日期">
        <el-date-picker v-model="query.endDate" type="date" value-format="YYYY-MM-DD" placeholder="止" style="width: 140px" />
      </el-form-item>
    </SearchForm>

    <el-row :gutter="20">
      <el-col :span="10">
        <el-card class="page-card">
          <div class="total-line">
            共 <b class="total-num">{{ total }}</b> 个对话
          </div>
          <el-table
            :data="list"
            @row-click="selectConversation"
            highlight-current-row
            stripe
            v-loading="loading"
          >
            <el-table-column prop="id" label="ID" width="140" :show-overflow-tooltip="true" />
            <el-table-column prop="userId" label="用户" width="80" />
            <el-table-column prop="mode" label="模式" width="80" />
            <el-table-column prop="status" label="状态" width="80">
              <template #default="{ row }">
                <el-tag :type="row.status === 'active' ? 'success' : 'info'" size="small">
                  {{ row.status === 'active' ? '进行中' : '已完成' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="80">
              <template #default="{ row }">
                <el-button size="small" type="danger" link @click.stop="onDelete(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>

          <el-pagination
            v-model:current-page="query.page"
            v-model:page-size="query.size"
            :total="total"
            :page-sizes="[20, 50, 100]"
            layout="total, sizes, prev, pager, next, jumper"
            @current-change="loadList"
            @size-change="loadList"
            class="pagination-bar"
          />
        </el-card>
      </el-col>
      <el-col :span="14">
        <el-card class="page-card">
          <h3 class="panel-title" v-if="selected">对话 #{{ selected.id }} 的消息</h3>
          <h3 class="panel-title" v-else>请选择左侧对话</h3>
          <div
            v-if="messages.length"
            class="messages-box"
            v-loading="msgLoading"
          >
            <div
              v-for="m in messages"
              :key="m.id"
              class="message-item"
            >
              <el-tag size="small">{{ roleLabel(m.questionType) }}</el-tag>
              <p class="message-body">{{ messageBody(m) }}</p>
            </div>
          </div>

          <el-pagination
            v-if="selected"
            v-model:current-page="msgQuery.page"
            :page-size="msgQuery.size"
            :total="msgTotal"
            layout="total, prev, pager, next"
            @current-change="loadMessages(selected)"
            class="pagination-bar"
          />
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<style scoped>
.page-card {
  border-radius: var(--radius-md);
}
.page-card :deep(.el-card__body) {
  box-shadow: var(--shadow-sm);
}

.total-line {
  margin-bottom: var(--space-md);
  color: var(--color-text-secondary);
  font-size: var(--font-base);
}
.total-num { color: var(--color-primary); font-weight: 600; }

.panel-title {
  font-size: var(--font-lg);
  font-weight: 600;
  color: var(--color-text-primary);
  margin: 0 0 var(--space-md);
}

.pagination-bar {
  margin-top: var(--space-md);
  justify-content: flex-end;
  display: flex;
}

/* === 消息流(改 loop border-bottom → 容器底部统一分隔,减少视觉噪音)==*/
.messages-box {
  max-height: 500px;
  overflow-y: auto;
  border-bottom: 1px solid var(--color-border);
}
.message-item {
  padding: var(--space-sm);
}
.message-item + .message-item {
  border-top: 1px solid var(--color-border);
}
.message-body {
  margin: 4px 0;
  white-space: pre-wrap;
}
</style>