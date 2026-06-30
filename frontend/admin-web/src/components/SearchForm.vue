<script setup lang="ts">
/**
 * 通用搜索表单容器
 * 给"列表/筛选"类页面用的统一风格:
 *  - 淡灰底卡片,圆角 8px
 *  - 左侧 label + 控件
 *  - 右侧"重置 + 查询"按钮
 *
 *  ponytail: 用 defineSlots 让父组件可以传任意 form-item,
 *  比 props.formItems 配置驱动更灵活(允许子组件内有 v-model 双向绑定)
 */
import { defineSlots } from 'vue'

const props = defineProps<{
  loading?: boolean
  showReset?: boolean
}>()

const emit = defineEmits<{
  (e: 'reset'): void
  (e: 'search'): void
}>()

defineSlots<{
  default(): any
}>()
</script>

<template>
  <el-card shadow="never" class="search-form">
    <el-form label-position="left" label-width="80px" :inline="true">
      <slot />
      <el-form-item class="search-form__actions">
        <el-button v-if="props.showReset !== false" @click="emit('reset')">重置</el-button>
        <el-button type="primary" :loading="loading" @click="emit('search')">查询</el-button>
      </el-form-item>
    </el-form>
  </el-card>
</template>

<style scoped>
.search-form {
  background: #fafbfc;
  border: 1px solid #ebeef5;
  margin-bottom: 16px;
}
.search-form :deep(.el-form-item) {
  margin-bottom: 12px;
  margin-right: 16px;
}
.search-form :deep(.el-input__wrapper) {
  background: white;
}
.search-form__actions {
  float: right;
}
</style>