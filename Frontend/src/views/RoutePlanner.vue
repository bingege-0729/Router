<template>
  <div class="planner">
    <div class="planner-grid">
      <!-- Left: Input Panel -->
      <aside class="input-panel">
        <div class="card panel-card">
          <h2>路线规划</h2>
          
          <form @submit.prevent="handlePlan" class="panel-form">
            <div class="form-group">
              <label class="form-label">出行需求</label>
              <textarea 
                v-model="form.query"
                class="textarea-field"
                rows="3"
                placeholder="例如：周末想去公园和博物馆，不想排队，预算500元内"
                maxlength="200"
              ></textarea>
            </div>

            <div class="divider"></div>

            <div class="form-group">
              <label class="form-label">游玩时长</label>
              <select v-model="form.totalHours" class="select-field">
                <option :value="2">2小时（快速）</option>
                <option :value="4">4小时（半天）</option>
                <option :value="6">6小时（推荐）</option>
                <option :value="8">8小时（全天）</option>
                <option :value="12">12小时（深度游）</option>
              </select>
            </div>

            <div class="form-group">
              <label class="form-label">预算范围（元）</label>
              <div class="slider-row">
                <input 
                  type="range"
                  v-model.number="form.maxBudget"
                  min="100"
                  max="2000"
                  step="50"
                  class="range-input"
                />
                <span class="range-value">{{ form.maxBudget }}</span>
              </div>
            </div>

            <div class="form-group">
              <label class="form-label">兴趣类别</label>
              <div class="checkbox-list">
                <label v-for="cat in categories" :key="cat.value" class="checkbox-item">
                  <input type="checkbox" :value="cat.value" v-model="form.categories" />
                  <span>{{ cat.label }}</span>
                </label>
              </div>
            </div>

            <div class="form-group">
              <label class="form-label">优化目标</label>
              <div class="radio-list">
                <label 
                  v-for="opt in optimizationOptions" 
                  :key="opt.value"
                  class="radio-item"
                  :class="{ active: form.optimizationGoal === opt.value }"
                >
                  <input 
                    type="radio" 
                    :value="opt.value" 
                    v-model="form.optimizationGoal"
                    name="optimization"
                  />
                  {{ opt.label }}
                </label>
              </div>
            </div>

            <button 
              type="submit" 
              class="btn-primary btn-block"
              style="margin-top: 8px;"
              :disabled="loading"
            >
              {{ loading ? '正在规划...' : '生成路线' }}
            </button>
          </form>
        </div>
      </aside>

      <!-- Right: Result Panel -->
      <main class="result-panel">
        <!-- Loading State -->
        <div v-if="loading" class="state-loading">
          <div class="skeleton">
            <div class="skeleton-line" v-for="i in 6" :key="i"></div>
          </div>
          <p class="text-muted">正在分析需求并优化路线...</p>
        </div>

        <!-- Result Content -->
        <template v-else-if="result">
          <!-- Description -->
          <div v-if="result.message" class="desc-box">
            <p class="body-text">{{ result.message }}</p>
            <span v-if="result.personalizationNote" class="note-tag">
              {{ result.personalizationNote }}
            </span>
          </div>

          <!-- Main Route -->
          <section class="route-section">
            <h3>推荐路线</h3>
            
            <div v-if="result.mainRoute?.pois?.length" class="timeline">
              <article 
                v-for="(poi, idx) in result.mainRoute.pois" 
                :key="poi.id || idx"
                class="timeline-item"
              >
                <span class="timeline-marker text-muted">第{{ idx + 1 }}站</span>
                <div class="card poi-card">
                  <h4>{{ poi.name }}</h4>
                  
                  <dl class="poi-info">
                    <div class="info-row">
                      <dt>类别</dt>
                      <dd><span class="tag">{{ getCategoryName(poi.category) }}</span></dd>
                    </div>
                    <div class="info-row">
                      <dt>评分</dt>
                      <dd>
                        <span class="star-rating">{{ poi.rating || 0 }}</span>
                      </dd>
                    </div>
                    <div class="info-row">
                      <dt>人均消费</dt>
                      <dd>{{ poi.avgCost ? `¥${poi.avgCost}` : '免费' }}</dd>
                    </div>
                    <div class="info-row">
                      <dt>建议停留</dt>
                      <dd>{{ poi.recommendedDuration || 90 }} 分钟</dd>
                    </div>
                  </dl>

                  <p v-if="poi.description" class="poi-desc">{{ poi.description }}</p>

                  <div v-if="poi.tags?.length" class="tag-list">
                    <span v-for="tag in poi.tags" :key="tag" class="tag tag-secondary">{{ tag }}</span>
                  </div>
                </div>
              </article>
            </div>

            <div v-else class="empty-hint">
              暂无路线数据
            </div>
          </section>

          <!-- Alternatives -->
          <section v-if="result.alternatives?.length" class="alt-section">
            <h3>其他方案</h3>
            
            <details v-for="(alt, idx) in result.alternatives" :key="idx" class="alt-item">
              <summary>{{ alt.description || `方案 ${idx + 1}` }}</summary>
              <ul class="alt-list">
                <li v-for="p in alt.pois" :key="p.id">{{ p.name }} ({{ getCategoryName(p.category) }})</li>
              </ul>
              <div class="alt-meta">
                <span>总时长: {{ alt.totalDuration }} 分钟</span>
                <span>总花费: ¥{{ alt.totalCost }}</span>
              </div>
            </details>
          </section>

          <!-- Actions -->
          <div class="action-bar">
            <button type="button" class="btn-primary" @click="handleAdopt">采用此路线</button>
            <button type="button" class="btn-secondary" @click="handleRegenerate">重新生成</button>
          </div>
        </template>

        <!-- Empty State -->
        <div v-else class="state-empty">
          <p class="empty-text">请在左侧填写需求开始规划</p>
          <button type="button" class="btn-primary" @click="scrollToInput">去填写</button>
        </div>
      </main>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { routeApi } from '@/api/request'

const route = useRoute()
const loading = ref(false)
const result = ref(null)

const form = reactive({
  query: '',
  totalHours: 6,
  maxBudget: 500,
  categories: [],
  optimizationGoal: 'BALANCED',
  mustVisit: []
})

const categories = [
  { value: 'RESTAURANT', label: '餐厅' },
  { value: 'ATTRACTION', label: '景点' },
  { value: 'PARK', label: '公园' },
  { value: 'MUSEUM', label: '博物馆' },
  { value: 'SHOPPING', label: '购物' },
  { value: 'CAFE', label: '咖啡' }
]

const optimizationOptions = [
  { value: 'BALANCED', label: '综合平衡' },
  { value: 'SHORTEST_TIME', label: '省时间' },
  { value: 'LEAST_WALKING', label: '少走路' },
  { value: 'LOWEST_COST', label: '省钱' },
  { value: 'LEAST_WAITING', label: '不排队' },
  { value: 'HIGHEST_RATED', label: '高评分' }
]

onMounted(() => {
  if (route.query.result) {
    try {
      result.value = JSON.parse(route.query.result)
    } catch (e) {
      console.error('解析路由参数失败:', e)
    }
  }
})

const handlePlan = async () => {
  if (!form.query.trim()) return
  
  loading.value = true
  try {
    const response = await routeApi.planRoute({
      query: form.query,
      startLat: 30.2741,
      startLng: 120.1551,
      totalHours: form.totalHours,
      categories: form.categories.length > 0 ? form.categories : undefined,
      maxBudget: form.maxBudget,
      optimizationGoal: form.optimizationGoal,
      mustVisit: form.mustVisit.length > 0 ? form.mustVisit : undefined
    })

    result.value = response?.data || response
  } catch (error) {
    console.error('路线规划失败:', error)
  } finally {
    loading.value = false
  }
}

const handleAdopt = () => {
  // 采用路线逻辑
}

const handleRegenerate = () => {
  handlePlan()
}

const scrollToInput = () => {
  document.querySelector('.input-panel')?.scrollIntoView({ behavior: 'smooth' })
}

const getCategoryName = (category) => {
  const map = {
    RESTAURANT: '餐厅',
    ATTRACTION: '景点',
    PARK: '公园',
    MUSEUM: '博物馆',
    SHOPPING: '购物',
    CAFE: '咖啡'
  }
  return map[category] || category || '未知'
}
</script>

<style scoped>
/* ===== Design Tokens ===== */
.planner {
  --primary: #2563EB;
  --primary-hover: #1D4ED8;
  --primary-light: #DBEAFE;
  --surface: #F9FAFB;
  --border: #E5E7EB;
  --text-primary: #111827;
  --text-secondary: #6B7280;
  --text-muted: #9CA3AF;

  max-width: 1400px;
  margin: 0 auto;
  padding: 32px 24px; /* xl */
}

/* ===== Layout (Grid Gap: 24px) ===== */
.planner-grid {
  display: grid;
  grid-template-columns: 320px 1fr;
  gap: 24px; /* lg */
  align-items: start;
}

/* ===== Typography (按DESIGN.md) ===== */
.planner h2 {
  /* H2: 24px / 600 / 1.35 */
  font-size: 18px; /* 面板标题用H3规格 */
  font-weight: 600;
  line-height: 1.4;
  color: var(--text-primary);
  margin: 0 0 20px 0;
}

.planner h3 {
  /* H3: 18px / 600 / 1.4 */
  font-size: 18px;
  font-weight: 600;
  line-height: 1.4;
  color: var(--text-primary);
  margin: 0 0 20px 0;
}

.planner h4 {
  /* Body: 15px / 600 */
  font-size: 16px;
  font-weight: 600;
  line-height: 1.5;
  color: var(--text-primary);
  margin: 0 0 12px 0;
}

.body-text {
  /* Body: 15px / 400 / 1.6 */
  font-size: 15px;
  font-weight: 400;
  line-height: 1.7;
  color: var(--text-primary);
  margin: 0 0 12px 0;
}

.text-muted {
  /* Caption: 12px / 400 / 1.4 */
  font-size: 13px; /* Small */
  font-weight: 400;
  line-height: 1.5;
  color: var(--text-muted);
}

/* ===== Input Panel ===== */
.input-panel {
  position: sticky;
  top: 80px;
}

/* ===== Card (按DESIGN.md组件规范) ===== */
.card {
  background: #FFFFFF;
  border: 1px solid var(--border);
  border-radius: 8px;
}

.panel-card {
  padding: 20px; /* lg */
}

.panel-form .form-group {
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
.textarea-field,
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
  resize: vertical;
  transition: border-color 0.15s ease, box-shadow 0.15s ease;
}

.textarea-field:focus,
.select-field:focus {
  border-color: var(--primary);
  box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.1); /* 按规范 */
}

.textarea-field::placeholder,
.select-field::placeholder {
  color: var(--text-muted);
}

.divider {
  height: 1px;
  background: var(--border);
  margin: 20px 0; /* lg */
}

/* Range Slider */
.slider-row {
  display: flex;
  align-items: center;
  gap: 12px;
}

.range-input {
  flex: 1;
  height: 4px;
  -webkit-appearance: none;
  appearance: none;
  background: var(--border);
  border-radius: 2px;
  outline: none;
}

.range-input::-webkit-slider-thumb {
  -webkit-appearance: none;
  width: 16px;
  height: 16px;
  border-radius: 50%;
  background: var(--primary);
  cursor: pointer;
  border: none;
}

.range-value {
  font-size: 14px;
  font-weight: 500;
  color: var(--text-primary);
  min-width: 48px;
  text-align: right;
}

/* Checkbox & Radio */
.checkbox-list,
.radio-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px; /* sm */
}

.checkbox-item,
.radio-item {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 13px; /* Small */
  color: var(--text-secondary);
  cursor: pointer;
  padding: 4px 10px;
  border-radius: 4px;
  transition: background-color 0.15s ease;
}

.checkbox-item:hover,
.radio-item:hover {
  background: var(--surface);
}

.radio-item.active {
  background: var(--primary-light);
  color: var(--primary);
  font-weight: 500;
}

.checkbox-item input,
.radio-item input {
  accent-color: var(--primary);
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

.btn-primary:hover:not(:disabled) {
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

.btn-secondary {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: #FFFFFF;
  border: 1px solid var(--border);
  color: var(--text-primary);
  border-radius: 6px;
  padding: 10px 20px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: border-color 0.15s ease, background-color 0.15s ease;
}

.btn-secondary:hover {
  border-color: var(--text-muted);
  background: var(--surface);
}

/* ===== Result Panel ===== */
.result-panel {
  min-height: 600px;
}

/* States */
.state-loading {
  text-align: center;
  padding: 48px 24px; /* lg */
  background: white;
  border: 1px solid var(--border);
  border-radius: 8px;
}

.skeleton {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.skeleton-line {
  height: 16px;
  background: var(--surface);
  border-radius: 4px;
}

.skeleton-line:nth-child(odd) {
  width: 100%;
}

.skeleton-line:nth-child(even) {
  width: 85%;
}

.state-loading p {
  margin-top: 24px; /* lg */
}

.state-empty {
  text-align: center;
  padding: 80px 20px; /* xxl */
  background: white;
  border: 1px solid var(--border);
  border-radius: 8px;
}

.empty-text {
  font-size: 15px;
  color: var(--text-muted);
  margin-bottom: 16px; /* md */
}

.empty-hint {
  font-size: 14px;
  color: var(--text-muted);
  text-align: center;
  padding: 40px 20px;
}

/* Description Box */
.desc-box {
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: 8px;
  padding: 20px; /* lg */
  margin-bottom: 20px; /* lg */
}

.note-tag {
  display: inline-block;
  font-size: 12px; /* Caption */
  color: var(--text-secondary);
  background: white;
  border: 1px solid var(--border);
  padding: 2px 10px;
  border-radius: 4px;
}

/* Route Section */
.route-section {
  background: white;
  border: 1px solid var(--border);
  border-radius: 8px;
  padding: 20px; /* lg */
  margin-bottom: 20px; /* lg */
}

/* Timeline */
.timeline {
  display: flex;
  flex-direction: column;
  gap: 16px; /* md */
}

.timeline-item {
  position: relative;
  padding-left: 72px;
}

.timeline-marker {
  position: absolute;
  left: 0;
  top: 0;
  width: 56px;
  text-align: right;
  padding-right: 12px;
}

/* POI Card */
.poi-card {
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: 6px;
  padding: 16px; /* md */
}

.poi-info {
  margin: 0 0 12px 0;
}

.info-row {
  display: grid;
  grid-template-columns: auto 1fr;
  gap: 4px 16px;
  margin-bottom: 4px;
}

.info-row dt {
  font-size: 13px; /* Small */
  color: var(--text-secondary);
}

.info-row dd {
  font-size: 13px; /* Small */
  color: var(--text-primary);
  margin: 0;
}

.star-rating {
  color: #F59E0B;
  font-weight: 500;
}

.poi-desc {
  font-size: 13px; /* Small */
  line-height: 1.5;
  color: var(--text-secondary);
  background: white;
  padding: 10px 12px;
  border-radius: 4px;
  margin: 0 0 10px 0;
}

/* ===== Tag (按DESIGN.md组件规范) ===== */
.tag {
  font-size: 12px; /* Caption */
  font-weight: 500;
  padding: 2px 8px;
  border-radius: 4px;
  background: var(--primary-light);
  color: var(--primary);
}

.tag-list {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
}

.tag-secondary {
  background: #F3F4F6;
  color: var(--text-secondary);
}

/* Alternatives */
.alt-section {
  background: white;
  border: 1px solid var(--border);
  border-radius: 8px;
  padding: 20px; /* lg */
  margin-bottom: 20px; /* lg */
}

.alt-item {
  border-bottom: 1px solid var(--border);
  padding: 12px 0;
}

.alt-item:last-child {
  border-bottom: none;
}

.alt-item summary {
  font-size: 14px;
  font-weight: 500;
  color: var(--text-primary);
  cursor: pointer;
  list-style: none;
}

.alt-item summary::before {
  content: '';
}

.alt-item[open] summary {
  margin-bottom: 12px;
}

.alt-list {
  list-style: none;
  padding: 0;
  margin: 0 0 12px 0;
}

.alt-list li {
  padding: 6px 0;
  font-size: 14px;
  color: var(--text-secondary);
  border-bottom: 1px solid #F3F4F6;
}

.alt-list li:last-child {
  border-bottom: none;
}

.alt-meta {
  display: flex;
  gap: 24px; /* lg */
  padding-top: 12px;
  border-top: 1px solid var(--border);
  font-size: 13px; /* Small */
  color: var(--text-muted);
}

/* Action Bar */
.action-bar {
  display: flex;
  justify-content: center;
  gap: 12px; /* sm */
  margin-top: 32px; /* xl */
}

/* ===== Responsive ===== */
@media (max-width: 992px) {
  .planner {
    padding: 16px; /* md */
  }

  .planner-grid {
    grid-template-columns: 1fr;
  }

  .input-panel {
    position: static;
  }

  .action-bar {
    flex-direction: column;
  }

  .action-bar button {
    width: 100%;
  }

  .timeline-item {
    padding-left: 0;
  }

  .timeline-marker {
    position: static;
    display: block;
    text-align: left;
    padding-right: 0;
    margin-bottom: 4px;
    font-weight: 500;
    color: var(--primary);
  }
}
</style>
