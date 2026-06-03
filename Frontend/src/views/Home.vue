<template>
  <div class="home">
    <!-- Hero Section -->
    <section class="hero">
      <h1>智能路线规划系统</h1>
      <p class="hero-subtitle">基于算法优化，为您生成最优出行路线方案</p>
      <button class="btn-primary" @click="$router.push('/route')">
        开始规划
        <svg width="16" height="16" viewBox="0 0 16 16" fill="none" style="margin-left: 6px; vertical-align: middle;">
          <path d="M6 3L11 8L6 13" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/>
        </svg>
      </button>
    </section>

    <!-- Features -->
    <section class="features-section">
      <div class="feature-grid">
        <article v-for="item in features" :key="item.title" class="card feature-card">
          <h3>{{ item.title }}</h3>
          <p>{{ item.desc }}</p>
        </article>
      </div>
    </section>

    <!-- Quick Start -->
    <section class="quick-section">
      <h2>快速体验</h2>
      
      <form @submit.prevent="handlePlan" class="quick-form">
        <div class="form-group">
          <label class="form-label">出行需求</label>
          <input 
            v-model="form.query"
            type="text"
            class="input-field"
            placeholder="例如：周末想去公园和博物馆"
          />
        </div>

        <div class="form-row">
          <div class="form-group">
            <label class="form-label">游玩时长</label>
            <select v-model="form.hours" class="select-field">
              <option :value="2">2小时（快速）</option>
              <option :value="4">4小时（半天）</option>
              <option :value="6">6小时（推荐）</option>
              <option :value="8">8小时（全天）</option>
            </select>
          </div>

          <div class="form-group">
            <label class="form-label">预算范围（元）</label>
            <input 
              v-model.number="form.budget"
              type="number"
              class="input-field"
              min="100"
              max="5000"
              step="100"
            />
          </div>
        </div>

        <button 
          type="submit" 
          class="btn-primary btn-block"
          :disabled="loading"
        >
          {{ loading ? '正在规划...' : '生成路线' }}
        </button>
      </form>
    </section>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { routeApi } from '@/api/request'

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
  if (!form.query.trim()) return
  
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
/* ===== Design Tokens ===== */
.home {
  --primary: #2563EB;
  --primary-hover: #1D4ED8;
  --surface: #F9FAFB;
  --border: #E5E7EB;
  --text-primary: #111827;
  --text-secondary: #6B7280;
  --text-muted: #9CA3AF;

  max-width: 1200px;
  margin: 0 auto;
  padding: 32px 24px;
}

/* ===== Typography (按DESIGN.md) ===== */
.home h1 {
  /* H1: 32px / 600 / 1.25 */
  font-size: 32px;
  font-weight: 600;
  line-height: 1.25;
  color: #FFFFFF;
  margin: 0 0 16px 0;
  letter-spacing: -0.5px;
}

.home h2 {
  /* H2: 24px / 600 / 1.35 */
  font-size: 24px;
  font-weight: 600;
  line-height: 1.35;
  color: var(--text-primary);
  margin: 0 0 20px 0;
}

.home h3 {
  /* H3: 18px / 600 / 1.4 */
  font-size: 18px;
  font-weight: 600;
  line-height: 1.4;
  color: var(--text-primary);
  margin: 0 0 12px 0;
}

.home p {
  /* Body: 15px / 400 / 1.6 */
  font-size: 15px;
  font-weight: 400;
  line-height: 1.6;
  color: var(--text-secondary);
  margin: 0;
}

/* ===== Spacing System (Base: 4px) ===== */
/* xs=4 sm=8 md=16 lg=24 xl=32 xxl=48 */

/* ===== Hero Section ===== */
.hero {
  background-color: #111827;
  border-radius: 8px;
  padding: 80px 40px;
  text-align: center;
  margin-bottom: 48px; /* xl */
}

.hero-subtitle {
  color: rgba(255, 255, 255, 0.75);
  margin-bottom: 32px; /* xl */
}

/* ===== Button (按DESIGN.md组件规范) ===== */
.btn-primary {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: var(--primary);
  color: #FFFFFF;
  border: none;
  border-radius: 6px;
  padding: 10px 20px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: background-color 0.15s ease;
}

.btn-primary:hover {
  background: var(--primary-hover);
}

.btn-block {
  width: 100%;
  padding: 12px 20px;
}

.btn-primary:disabled {
  opacity: 0.65;
  cursor: not-allowed;
}

/* ===== Features Section ===== */
.features-section {
  margin-bottom: 48px; /* xl */
}

.feature-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 24px; /* lg */
}

/* ===== Card (按DESIGN.md组件规范) ===== */
.card {
  background: #FFFFFF;
  border: 1px solid var(--border);
  border-radius: 8px;
  transition: box-shadow 0.15s ease;
}

.feature-card {
  padding: 24px; /* lg */
}

.feature-card:hover {
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06); /* 按规范 */
}

/* ===== Quick Start Section ===== */
.quick-section {
  background: #FFFFFF;
  border: 1px solid var(--border);
  border-radius: 8px;
  padding: 24px; /* lg */
}

.quick-form {
  max-width: 700px;
  margin: 0 auto;
}

.form-group {
  margin-bottom: 16px; /* md */
}

.form-label {
  display: block;
  font-size: 14px;
  font-weight: 500;
  color: var(--text-primary);
  margin-bottom: 8px; /* sm */
}

/* ===== Input (按DESIGN.md组件规范) ===== */
.input-field,
.select-field {
  width: 100%;
  border-radius: 6px;
  border: 1px solid var(--border);
  padding: 10px 12px;
  font-size: 14px;
  font-family: inherit;
  background: #FFFFFF;
  color: var(--text-primary);
  box-sizing: border-box;
  outline: none;
  transition: border-color 0.15s ease, box-shadow 0.15s ease;
}

.input-field:focus,
.select-field:focus {
  border-color: var(--primary);
  box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.1); /* 按规范 */
}

.input-field::placeholder {
  color: var(--text-muted);
}

.form-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px; /* md */
}

/* ===== Responsive ===== */
@media (max-width: 768px) {
  .home {
    padding: 16px; /* md */
  }

  .hero {
    padding: 48px 24px; /* lg md */
    margin-bottom: 32px; /* lg */
  }

  .home h1 {
    font-size: 26px;
  }

  .hero-subtitle {
    font-size: 15px; /* Small */
    margin-bottom: 24px; /* lg */
  }

  .feature-grid {
    grid-template-columns: 1fr;
    gap: 16px; /* md */
  }

  .form-row {
    grid-template-columns: 1fr;
  }
}
</style>
