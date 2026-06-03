<template>
  <div class="home">
    <!-- Hero Section -->
    <section class="hero">
      <h1>智能路线规划系统</h1>
      <p class="hero-subtitle">基于算法优化，为您生成最优出行路线方案</p>
      <el-button type="primary" size="large" @click="$router.push('/route')">
        开始规划
        <el-icon><Right /></el-icon>
      </el-button>
    </section>

    <!-- Features -->
    <section class="features-section">
      <el-row :gutter="24">
        <el-col :xs="24" :sm="8" v-for="item in features" :key="item.title">
          <el-card shadow="never" class="feature-card">
            <h3>{{ item.title }}</h3>
            <p>{{ item.desc }}</p>
          </el-card>
        </el-col>
      </el-row>
    </section>

    <!-- Quick Start -->
    <section class="quick-section">
      <el-card shadow="never">
        <template #header>
          <h2>快速体验</h2>
        </template>

        <el-form :model="form" label-position="top" class="quick-form">
          <el-form-item label="出行需求">
            <el-input
              v-model="form.query"
              placeholder="例如：周末想去公园和博物馆"
              size="large"
              clearable
            />
          </el-form-item>

          <el-row :gutter="16">
            <el-col :span="12">
              <el-form-item label="游玩时长">
                <el-select v-model="form.hours" size="large" style="width:100%">
                  <el-option label="2小时（快速）" :value="2" />
                  <el-option label="4小时（半天）" :value="4" />
                  <el-option label="6小时（推荐）" :value="6" />
                  <el-option label="8小时（全天）" :value="8" />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="预算范围（元）">
                <el-input-number
                  v-model="form.budget"
                  size="large"
                  style="width:100%"
                  :min="100"
                  :max="5000"
                  :step="100"
                />
              </el-form-item>
            </el-col>
          </el-row>

          <el-form-item>
            <el-button
              type="primary"
              size="large"
              @click="handlePlan"
              :loading="loading"
              style="width: 100%"
            >
              {{ loading ? '正在规划...' : '生成路线' }}
            </el-button>
          </el-form-item>
        </el-form>
      </el-card>
    </section>
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

const form = reactive({
  query: '',
  hours: 6,
  budget: 500
})

const features = [
  {
    title: '智能解析',
    desc: '理解自然语言需求，自动提取时间、预算、偏好等参数'
  },
  {
    title: '最优算法',
    desc: '使用TSP算法求解旅行商问题，确保路线全局最优'
  },
  {
    title: '快速响应',
    desc: '多级缓存策略，相同查询第二次访问速度显著提升'
  }
]

const handlePlan = async () => {
  if (!form.query.trim()) {
    ElMessage.warning('请输入您的出行需求')
    return
  }

  loading.value = true

  try {
    const response = await routeApi.planRoute({
      query: form.query,
      totalHours: form.hours,
      maxBudget: form.budget,
      startLat: 30.2741,
      startLng: 120.1551,
      optimizationGoal: 'BALANCED'
    })

    if (response?.success && response?.data) {
      router.push({ path: '/route', query: { result: JSON.stringify(response.data) } })
    }
  } catch (error) {
    console.error('路线规划失败:', error)
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
/* Design Tokens - 按 DESIGN.md */
.home {
  --color-primary: #2563EB;
  --color-primary-hover: #1D4ED8;
  --color-surface: #F9FAFB;
  --color-border: #E5E7EB;
  --color-text-primary: #111827;
  --color-text-secondary: #6B7280;
  --color-text-muted: #9CA3AF;

  max-width: 1200px;
  margin: 0 auto;
  padding: 32px 24px;
}

/* Hero Section */
.hero {
  background-color: #111827;
  border-radius: 8px;
  padding: 80px 40px;
  text-align: center;
  color: #FFFFFF;
  margin-bottom: 48px;
}

.hero h1 {
  font-size: 32px;
  font-weight: 600;
  line-height: 1.25;
  margin-bottom: 16px;
  letter-spacing: -0.5px;
}

.hero-subtitle {
  font-size: 18px;
  color: rgba(255, 255, 255, 0.75);
  line-height: 1.6;
  margin-bottom: 32px;
}

/* Features Section */
.features-section {
  margin-bottom: 48px;
}

.feature-card {
  height: 100%;
  border-radius: 8px;
  border: 1px solid var(--color-border);
  transition: box-shadow 0.15s ease;
}

.feature-card:hover {
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.feature-card h3 {
  font-size: 18px;
  font-weight: 600;
  color: var(--color-text-primary);
  margin: 0 0 12px 0;
}

.feature-card p {
  font-size: 15px;
  line-height: 1.6;
  color: var(--color-text-secondary);
  margin: 0;
}

/* Quick Start Section */
.quick-section {
  margin-bottom: 48px;
}

.quick-section .el-card {
  border-radius: 8px;
  border: 1px solid var(--color-border);
}

.quick-section h2 {
  font-size: 18px;
  font-weight: 600;
  color: var(--color-text-primary);
  margin: 0;
}

.quick-form {
  max-width: 700px;
  margin: 0 auto;
}

/* Responsive */
@media (max-width: 768px) {
  .home {
    padding: 16px;
  }

  .hero {
    padding: 48px 24px;
    margin-bottom: 32px;
  }

  .hero h1 {
    font-size: 26px;
  }

  .hero-subtitle {
    font-size: 15px;
    margin-bottom: 24px;
  }

  .features-section {
    margin-bottom: 32px;
  }
}
</style>
