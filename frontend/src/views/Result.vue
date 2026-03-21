<template>
  <div class="result-container">
    <van-nav-bar
      title="推荐结果"
      left-arrow
      @click-left="onClickLeft"
      fixed
      placeholder
    />

    <div class="result-content">
      <div class="result-header">
        <van-icon name="success" size="60" color="#07c160" />
        <h2>为你推荐</h2>
      </div>

      <div class="food-card">
        <div class="food-name">{{ foodName }}</div>
        <div class="food-reason" v-if="reason">{{ reason }}</div>
      </div>

      <div class="collected-params" v-if="collectedParams.length > 0">
        <h3>根据你的需求</h3>
        <div class="param-list">
          <div
            v-for="param in collectedParams"
            :key="param.name"
            class="param-item"
          >
            <span class="param-label">{{ param.label }}：</span>
            <span class="param-value">{{ param.value }}</span>
          </div>
        </div>
      </div>

      <div class="action-buttons">
        <van-button type="primary" block round @click="startNewChat">
          再来一次
        </van-button>
        <van-button plain block round @click="goHome">
          返回首页
        </van-button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { useChatStore } from '@/stores/chat'

const router = useRouter()
const chatStore = useChatStore()

const paramLabels: Record<string, string> = {
  time: '用餐时间',
  location: '用餐地点',
  weather: '天气情况',
  mood: '当前心情',
  companion: '同行人员',
  budget: '预算范围',
  taste: '口味偏好',
  restriction: '饮食禁忌',
  preference: '特殊偏好',
  health: '健康需求'
}

const foodName = computed(() => {
  try {
    const result = JSON.parse(chatStore.recommendationResult || '{}')
    return result.foodName || '暂无推荐'
  } catch {
    return '暂无推荐'
  }
})

const reason = computed(() => {
  try {
    const result = JSON.parse(chatStore.recommendationResult || '{}')
    return result.reason || ''
  } catch {
    return ''
  }
})

const collectedParams = computed(() => {
  // 优先使用 store 中记录的实际参数值
  const paramValues = chatStore.collectedParamValues
  const collected = chatStore.progress.collected

  if (Object.keys(paramValues).length > 0) {
    return Object.entries(paramValues).map(([name, value]) => ({
      name,
      label: paramLabels[name] || name,
      value
    }))
  }

  // 回退：只展示参数名
  return collected.map(name => ({
    name,
    label: paramLabels[name] || name,
    value: '已收集'
  }))
})

const onClickLeft = () => {
  router.push('/')
}

const startNewChat = () => {
  chatStore.clearChat()
  router.push('/')
}

const goHome = () => {
  router.push('/')
}
</script>

<style lang="scss" scoped>
.result-container {
  min-height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  overflow-y: auto;
}

.result-content {
  padding: 20px;
  padding-bottom: 40px;
}

.result-header {
  text-align: center;
  color: white;
  padding: 30px 0;
  
  h2 {
    margin-top: 15px;
    font-size: 24px;
  }
}

.food-card {
  background: white;
  border-radius: 16px;
  padding: 30px;
  margin-bottom: 20px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.food-name {
  font-size: 28px;
  font-weight: bold;
  color: #333;
  text-align: center;
  margin-bottom: 15px;
}

.food-reason {
  font-size: 14px;
  color: #666;
  line-height: 1.6;
  text-align: center;
}

.collected-params {
  background: rgba(255, 255, 255, 0.9);
  border-radius: 16px;
  padding: 20px;
  margin-bottom: 30px;
  
  h3 {
    font-size: 16px;
    color: #333;
    margin-bottom: 15px;
  }
}

.param-list {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.param-item {
  background: #f7f8fa;
  padding: 8px 12px;
  border-radius: 8px;
  font-size: 12px;
}

.param-label {
  color: #666;
}

.param-value {
  color: #1989fa;
  font-weight: bold;
}

.action-buttons {
  display: flex;
  flex-direction: column;
  gap: 15px;
  
  .van-button--primary {
    background: linear-gradient(90deg, #1989fa 0%, #07c160 100%);
    border: none;
  }
  
  .van-button--plain {
    color: white;
    border-color: white;
  }
}
</style>
