<template>
  <div class="home-container">
    <el-row :gutter="20" class="hero-section">
      <el-col :span="24">
        <div class="hero-banner">
          <h2 class="hero-title">🗺️ AI驱动的智能路线规划</h2>
          <p class="hero-subtitle">
            基于大语言模型和智能算法，为您量身定制最优出行路线
          </p>
          <el-button 
            type="primary" 
            size="large" 
            @click="$router.push('/route')"
            class="cta-button"
          >
            开始规划路线
            <el-icon><Right /></el-icon>
          </el-button>
        </div>
      </el-col>
    </el-row>

    <el-row :gutter="20" class="features-section">
      <el-col :xs="24" :sm="8" v-for="feature in features" :key="feature.title">
        <el-card class="feature-card" shadow="hover">
          <div class="feature-icon">{{ feature.icon }}</div>
          <h3>{{ feature.title }}</h3>
          <p>{{ feature.description }}</p>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" class="stats-section">
      <el-col :xs="12" :sm="6" v-for="stat in stats" :key="stat.label">
        <el-card class="stat-card">
          <div class="stat-number">{{ stat.value }}</div>
          <div class="stat-label">{{ stat.label }}</div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" class="demo-section">
      <el-col :span="24">
        <el-card>
          <template #header>
            <div class="card-header">
              <span>🎯 快速体验</span>
            </div>
          </template>
          
          <el-form :model="quickForm" label-width="auto" class="quick-form">
            <el-form-item label="我想去">
              <el-input 
                v-model="quickForm.query" 
                placeholder="例如：周末想去公园和博物馆"
                size="large"
                clearable
              />
            </el-form-item>
            
            <el-form-item label="游玩时长">
              <el-select v-model="quickForm.hours" placeholder="选择时长" size="large">
                <el-option label="2小时（快速打卡）" :value="2" />
                <el-option label="4小时（半天）" :value="4" />
                <el-option label="6小时（轻松游）" :value="6" />
                <el-option label="8小时（全天）" :value="8" />
              </el-select>
            </el-form-item>
            
            <el-form-item label="预算范围">
              <el-slider 
                v-model="quickForm.budget" 
                :min="100" 
                :max="2000" 
                :step="100"
                show-input
                :input-size="'large'"
              />
            </el-form-item>
            
            <el-form-item>
              <el-button 
                type="primary" 
                size="large" 
                @click="handleQuickPlan"
                :loading="loading"
                style="width: 100%"
              >
                {{ loading ? 'AI正在规划中...' : '立即生成路线' }}
              </el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { Right } from '@element-plus/icons-vue'
import { routeApi } from '@/api/request'
import { ElMessage } from 'element-plus'

const router = useRouter()
const loading = ref(false)

const quickForm = reactive({
  query: '',
  hours: 4,
  budget: 500
})

const features = [
  {
    icon: '🧠',
    title: 'AI 智能解析',
    description: '基于大语言模型理解您的自然语言需求，自动提取时间、预算、偏好等参数'
  },
  {
    icon: '🎯',
    title: '全局最优算法',
    description: '使用 OR-Tools TSP 算法求解旅行商问题，确保路线全局最优而非局部最优'
  },
  {
    icon: '⚡',
    title: '毫秒级响应',
    description: '多级缓存策略 + 智能预加载，相同查询第二次访问速度提升 10 倍以上'
  }
]

const stats = [
  { value: '< 2s', label: '平均响应时间' },
  { value: '99.9%', label: '服务可用性' },
  { value: '500+', label: 'POI 数据库' },
  { value: '6种', label: '优化策略' }
]

const handleQuickPlan = async () => {
  if (!quickForm.query.trim()) {
    ElMessage.warning('请输入您想去的地点或活动')
    return
  }

  loading.value = true

  try {
    const response = await routeApi.planRoute({
      query: quickForm.query,
      totalHours: quickForm.hours,
      maxBudget: quickForm.budget,
      startLat: 30.2741,
      startLng: 120.1551,
      optimizationGoal: 'BALANCED'
    })

    // ✅ 正确解构Result包装类
    if (response && response.success && response.data) {
      router.push({
        path: '/route',
        query: {
          result: JSON.stringify(response.data)  // 只传递data部分
        }
      })
    }
  } catch (error) {
    console.error('路线规划失败:', error)
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.home-container {
  max-width: 1400px;
  margin: 0 auto;
}

.hero-section {
  margin-bottom: 40px;
}

.hero-banner {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 16px;
  padding: 60px 40px;
  text-align: center;
  color: white;
}

.hero-title {
  font-size: 36px;
  font-weight: bold;
  margin-bottom: 16px;
}

.hero-subtitle {
  font-size: 18px;
  opacity: 0.9;
  margin-bottom: 32px;
}

.cta-button {
  font-size: 18px;
  padding: 12px 32px;
  border-radius: 8px;
}

.features-section {
  margin-bottom: 40px;
}

.feature-card {
  text-align: center;
  padding: 30px 20px;
  height: 100%;
  transition: transform 0.3s;
}

.feature-card:hover {
  transform: translateY(-5px);
}

.feature-icon {
  font-size: 48px;
  margin-bottom: 16px;
}

.feature-card h3 {
  font-size: 20px;
  margin-bottom: 12px;
  color: #303133;
}

.feature-card p {
  color: #606266;
  line-height: 1.6;
  font-size: 14px;
}

.stats-section {
  margin-bottom: 40px;
}

.stat-card {
  text-align: center;
  padding: 24px;
}

.stat-number {
  font-size: 28px;
  font-weight: bold;
  color: #409eff;
  margin-bottom: 8px;
}

.stat-label {
  color: #909399;
  font-size: 14px;
}

.demo-section {
  margin-bottom: 40px;
}

.card-header {
  font-size: 18px;
  font-weight: bold;
}

.quick-form {
  max-width: 800px;
  margin: 0 auto;
  padding: 20px 0;
}
</style>
