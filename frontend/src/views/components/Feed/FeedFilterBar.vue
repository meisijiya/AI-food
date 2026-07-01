<template>
  <!--
    FeedFilterBar - 筛选弹窗
    父组件通过 v-model:visible 双向控制显示,确认时 emit('apply', form) 回调
    弹窗内部状态(食物名/参数名/参数值)由子组件内部维护
  -->
  <Transition name="overlay-fade">
    <div v-if="visible" class="filter-overlay" @click="close">
      <div class="filter-panel" @click.stop>
        <div class="filter-title">筛选</div>
        <div class="filter-group">
          <label class="filter-label">食物名称</label>
          <input
            v-model="form.foodName"
            class="filter-input"
            placeholder="搜索食物名称..."
          />
        </div>
        <div class="filter-group">
          <label class="filter-label">参数筛选</label>
          <select v-model="form.paramName" class="filter-select">
            <option value="">不限</option>
            <option value="time">用餐时间</option>
            <option value="location">用餐地点</option>
            <option value="mood">当前心情</option>
            <option value="taste">口味偏好</option>
            <option value="budget">预算范围</option>
            <option value="companion">同行人员</option>
            <option value="weather">天气情况</option>
          </select>
          <input
            v-if="form.paramName"
            v-model="form.paramValue"
            class="filter-input"
            placeholder="输入筛选值..."
          />
        </div>
        <div class="filter-actions">
          <button class="filter-reset" @click="reset">重置</button>
          <button class="filter-apply" @click="apply">确定</button>
        </div>
      </div>
    </div>
  </Transition>
</template>

<script setup lang="ts">
// FeedFilterBar - 筛选弹窗(子组件,纯展示 + 内部表单状态)
// 父组件只需 v-model:visible + 监听 apply 事件
import { ref } from 'vue'
import type { FilterForm } from '@/types/feed'

defineProps<{
  visible: boolean
}>()

const emit = defineEmits<{
  (e: 'update:visible', value: boolean): void
  (e: 'apply', form: FilterForm): void
}>()

// 子组件内部维护表单状态
// ponytail: 保持原 Feed.vue 行为 —— 不在打开时重置,只点"重置"按钮才清空;
// 这样 filterFoodName/filterParamName/filterParamValue 维持上次筛选状态
const form = ref<FilterForm>({ foodName: '', paramName: '', paramValue: '' })

function close() {
  emit('update:visible', false)
}

function reset() {
  form.value = { foodName: '', paramName: '', paramValue: '' }
}

function apply() {
  emit('apply', { ...form.value })
  emit('update:visible', false)
}
</script>

<style lang="scss" scoped>
/* 筛选遮罩层(原 Feed.vue .filter-overlay 样式) */
.filter-overlay {
  position: fixed;
  inset: 0;
  z-index: 200;
  background: rgba(11, 15, 16, 0.4);
  backdrop-filter: blur(8px);
  display: flex;
  align-items: center;
  justify-content: center;
}

.filter-panel {
  width: calc(100% - 48px);
  max-width: 400px;
  background: var(--color-surface-container-lowest);
  border-radius: 2rem;
  padding: 28px 24px;
}

.filter-title {
  font-family: var(--font-serif);
  font-style: italic;
  font-size: 22px;
  font-weight: 500;
  color: var(--color-on-surface);
  margin-bottom: 20px;
}

.filter-group {
  margin-bottom: 16px;
}

.filter-label {
  display: block;
  font-size: 12px;
  font-weight: 700;
  color: var(--color-on-surface-variant);
  text-transform: uppercase;
  letter-spacing: 0.1em;
  margin-bottom: 8px;
}

.filter-input,
.filter-select {
  width: 100%;
  padding: 12px 16px;
  border: 1.5px solid var(--color-surface-container-low);
  border-radius: 1rem;
  background: var(--color-surface);
  font-family: var(--font-sans);
  font-size: 14px;
  color: var(--color-on-surface);
  outline: none;
  &:focus {
    border-color: var(--color-primary);
  }
}

.filter-select {
  appearance: none;
  margin-bottom: 8px;
}

.filter-actions {
  display: flex;
  gap: 10px;
  margin-top: 20px;
}

.filter-reset {
  flex: 1;
  padding: 12px;
  border: 1.5px solid var(--color-surface-container-low);
  border-radius: 1rem;
  background: none;
  color: var(--color-on-surface-variant);
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
}

.filter-apply {
  flex: 1;
  padding: 12px;
  border: none;
  border-radius: 1rem;
  background: linear-gradient(
    135deg,
    var(--color-primary-container),
    var(--color-primary)
  );
  color: white;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
}

/* 弹窗淡入淡出 */
.overlay-fade-enter-active {
  transition: opacity 0.25s ease;
}
.overlay-fade-leave-active {
  transition: opacity 0.2s ease;
}
.overlay-fade-enter-from,
.overlay-fade-leave-to {
  opacity: 0;
}
</style>
