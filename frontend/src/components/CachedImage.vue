<template>
  <img v-if="cachedSrc" :src="cachedSrc" :alt="alt" :loading="lazy ? 'lazy' : undefined" @error="onError" />
  <div v-else-if="loading" class="cached-img-placeholder">
    <div class="cached-img-spinner"></div>
  </div>
  <img v-else :src="src" :alt="alt" :loading="lazy ? 'lazy' : undefined" />
</template>

<script setup lang="ts">
import { ref, watch, onMounted } from 'vue'
import { getPhotoBlobUrl } from '@/utils/photoCache'

const props = withDefaults(defineProps<{
  src: string
  alt?: string
  lazy?: boolean
}>(), {
  alt: '',
  lazy: true
})

const cachedSrc = ref('')
const loading = ref(true)

async function loadPhoto() {
  if (!props.src) {
    loading.value = false
    return
  }
  loading.value = true
  try {
    cachedSrc.value = await getPhotoBlobUrl(props.src)
  } catch {
    cachedSrc.value = props.src
  } finally {
    loading.value = false
  }
}

function onError() {
  cachedSrc.value = props.src
}

watch(() => props.src, loadPhoto)
onMounted(loadPhoto)
</script>

<style scoped>
.cached-img-placeholder {
  width: 100%;
  aspect-ratio: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--color-surface-container-low, #1a1d1e);
}

.cached-img-spinner {
  width: 20px;
  height: 20px;
  border: 2px solid rgba(255, 255, 255, 0.1);
  border-top-color: var(--color-primary, #22d3ee);
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}
</style>
