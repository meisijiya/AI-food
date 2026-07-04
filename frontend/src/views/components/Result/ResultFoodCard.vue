<template>
  <!--
    ResultFoodCard - 食物卡片(深色 + 装饰光晕 + 推荐理由)
    纯展示,只接收 props
  -->
  <div class="food-card animate-fade-up delay-300 animate-start-hidden">
    <div class="food-card-glow"></div>
    <div class="food-name">{{ foodName }}</div>
    <div class="food-reason" v-if="reason">{{ reason }}</div>
  </div>
</template>

<script setup lang="ts">
// ResultFoodCard - 食物卡片(子组件,纯展示)
defineProps<{
  foodName: string
  reason: string | null
}>()
</script>

<style lang="scss" scoped>
/* Food Card (Dark) */
.food-card {
  background: var(--color-inverse-surface);
  color: white;
  border-radius: 2.5rem;
  padding: 40px 32px;
  text-align: center;
  margin-bottom: 20px;
  position: relative;
  overflow: hidden;
}

.food-card-glow {
  position: absolute;
  top: -40px;
  right: -40px;
  width: 160px;
  height: 160px;
  background: rgba(140, 225, 243, 0.08);
  border-radius: 50%;
  filter: blur(40px);
}

.food-name {
  font-family: var(--font-serif);
  font-style: italic;
  font-size: 32px;
  font-weight: 500;
  margin-bottom: 16px;
  position: relative;
  z-index: 1;
}

.food-reason {
  font-size: 14px;
  line-height: 1.8;
  color: rgba(255, 255, 255, 0.65);
  position: relative;
  z-index: 1;
}
</style>
