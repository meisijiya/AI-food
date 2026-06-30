<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getHealth, getJvm } from '@/api/monitor'

const health = ref<any>({})
const jvm = ref<any>({})

onMounted(async () => {
  health.value = (await getHealth()).data || {}
  jvm.value = (await getJvm()).data || {}
})
</script>

<template>
  <div class="page-container">
    <el-row :gutter="20">
      <el-col :span="12">
        <el-card>
          <h3>健康检查</h3>
          <pre>{{ health }}</pre>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card>
          <h3>JVM 信息</h3>
          <el-descriptions :column="1" border>
            <el-descriptions-item label="总内存">{{ jvm.totalMemory }}</el-descriptions-item>
            <el-descriptions-item label="空闲内存">{{ jvm.freeMemory }}</el-descriptions-item>
            <el-descriptions-item label="最大内存">{{ jvm.maxMemory }}</el-descriptions-item>
            <el-descriptions-item label="CPU 核心">{{ jvm.availableProcessors }}</el-descriptions-item>
            <el-descriptions-item label="启动时间(ms)">{{ jvm.uptime }}</el-descriptions-item>
          </el-descriptions>
        </el-card>
      </el-col>
    </el-row>

    <el-card style="margin-top: 20px">
      <h3>Spring Boot Admin (SBA)</h3>
      <p>SBA UI: <a href="/admin/sba" target="_blank">/admin/sba</a> (需 basic auth, 默认 admin / admin123)</p>
      <h3>Druid 监控</h3>
      <p>Druid UI: <a href="/admin/druid/login.html" target="_blank">/admin/druid</a></p>
    </el-card>
  </div>
</template>