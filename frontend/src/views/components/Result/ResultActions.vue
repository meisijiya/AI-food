<!--
  ResultActions - 容器组件,只负责协调 Share + Publish 两个子组件
  不持有任何 UI 状态;状态全部下沉到 ResultPublishDialog(弹窗状态)
  或由父组件 Result.vue 提供(shareUrl / isPublished / loading flags)
-->
<template>
  <div v-if="sessionId" class="actions-stack">
    <ResultShare
      :share-url="shareUrl"
      :sharing="sharing"
      @share="emit('share')"
      @copy-share-url="emit('copy-share-url')"
    />
    <ResultPublishDialog
      :is-published="isPublished"
      :publishing="publishing"
      :unpublishing="unpublishing"
      :initial-publish-preview="initialPublishPreview"
      @unpublish="emit('unpublish')"
      @open-publish="emit('open-publish')"
      @close-publish="emit('close-publish')"
      @confirm-publish="(p) => emit('confirm-publish', p)"
    />
  </div>
</template>

<script setup lang="ts">
// ResultActions - 薄容器,无内部 state,纯转发
import ResultShare from './ResultShare.vue'
import ResultPublishDialog from './ResultPublishDialog.vue'

defineProps<{
  sessionId: string
  shareUrl: string
  isPublished: boolean
  sharing: boolean
  publishing: boolean
  unpublishing: boolean
  initialPublishPreview?: string
}>()

const emit = defineEmits<{
  (e: 'share'): void
  (e: 'copy-share-url'): void
  (e: 'open-publish'): void
  (e: 'close-publish'): void
  (e: 'confirm-publish', preview: string): void
  (e: 'unpublish'): void
}>()
</script>

<style lang="scss" scoped>
/* actions-stack 透明容器 —— display:contents 让子组件直接参与父布局,
   避免拆组件后多出一层 wrapper 影响现有 flex/grid 布局 */
.actions-stack {
  display: contents;
}
</style>
