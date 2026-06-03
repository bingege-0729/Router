<template>
  <div class="planner">
    <el-row :gutter="24" class="planner-grid">
      <!-- Left: Input Panel -->
      <el-col :xs="24" :lg="8" class="input-panel-col">
        <el-card shadow="never" class="panel-card">
          <template #header>
            <h2>路线规划</h2>
          </template>

          <el-form :model="form" label-position="top" size="large">
            <el-form-item label="出行需求">
              <el-input
                v-model="form.query"
                type="textarea"
                :rows="3"
                placeholder="例如：周末想去公园和博物馆，不想排队，预算500元内"
                maxlength="200"
                show-word-limit
              />
            </el-form-item>

            <el-form-item label="游玩时长">
              <el-select v-model="form.totalHours" style="width:100%">
                <el-option label="2小时（快速）" :value="2" />
                <el-option label="4小时（半天）" :value="4" />
                <el-option label="6小时（推荐）" :value="6" />
                <el-option label="8小时（全天）" :value="8" />
                <el-option label="12小时（深度游）" :value="12" />
              </el-select>
            </el-form-item>

            <el-form-item label="预算范围（元）">
              <el-slider
                v-model="form.maxBudget"
                :min="100"
                :max="2000"
                :step="50"
                show-input
              />
            </el-form-item>

            <el-form-item label="兴趣类别">
              <el-checkbox-group v-model="form.categories">
                <el-checkbox v-for="cat in categories" :key="cat.value" :label="cat.value">
                  {{ cat.label }}
                </el-checkbox>
              </el-checkbox-group>
            </el-form-item>

            <el-form-item label="优化目标">
              <el-radio-group v-model="form.optimizationGoal">
                <el-radio-button 
                  v-for="opt in optimizationOptions" 
                  :key="opt.value" 
                  :value="opt.value"
                >
                  {{ opt.label }}
                </el-radio-button>
              </el-radio-group>
            </el-form-item>

            <el-button
              type="primary"
              @click="handlePlan"
              :loading="loading"
              style="width: 100%; margin-top: 8px"
            >
              {{ loading ? '正在规划...' : '生成路线' }}
            </el-button>
          </el-form>
        </el-card>
      </el-col>

      <!-- Right: Result Panel -->
      <el-col :xs="24" :lg="16" class="result-panel-col">
        <!-- Loading State -->
        <div v-if="loading" class="state-loading">
          <el-skeleton :rows="8" animated />
          <p class="loading-text">正在分析需求并优化路线...</p>
        </div>

        <!-- Result Content -->
        <template v-else-if="result">
          <!-- Description -->
          <el-card v-if="result.message" shadow="never" class="desc-card">
            <p>{{ result.message }}</p>
            <el-tag v-if="result.personalizationNote" size="small" type="info" style="margin-top:12px">
              {{ result.personalizationNote }}
            </el-tag>
          </el-card>

          <!-- Main Route -->
          <el-card shadow="never" class="route-card">
            <template #header>
              <h3>推荐路线</h3>
            </template>

            <el-timeline v-if="result.mainRoute?.pois?.length">
              <el-timeline-item
                v-for="(poi, idx) in result.mainRoute.pois"
                :key="poi.id || idx"
                :timestamp="'第 ' + (idx + 1) + ' 站'"
                placement="top"
              >
                <el-card shadow="never" class="poi-card">
                  <h4>{{ poi.name }}</h4>
                  
                  <el-descriptions :column="2" size="small" border>
                    <el-descriptions-item label="类别">
                      <el-tag size="small">{{ getCategoryName(poi.category) }}</el-tag>
                    </el-descriptions-item>
                    <el-descriptions-item label="评分">
                      <span style="color:#F59E0B; font-weight:500">{{ poi.rating || 0 }}</span>
                    </el-descriptions-item>
                    <el-descriptions-item label="人均消费">
                      {{ poi.avgCost ? `¥${poi.avgCost}` : '免费' }}
                    </el-descriptions-item>
                    <el-descriptions-item label="建议停留">
                      {{ poi.recommendedDuration || 90 }} 分钟
                    </el-descriptions-item>
                  </el-descriptions>

                  <p v-if="poi.description" class="poi-desc">{{ poi.description }}</p>

                  <div v-if="poi.tags?.length" class="tag-list">
                    <el-tag
                      v-for="tag in poi.tags"
                      :key="tag"
                      size="small"
                      type="info"
                    >
                      {{ tag }}
                    </el-tag>
                  </div>
                </el-card>
              </el-timeline-item>
            </el-timeline>

            <el-empty v-else description="暂无路线数据" :image-size="120" />
          </el-card>

          <!-- Alternatives -->
          <el-card
            v-if="result.alternatives?.length"
            shadow="never"
            class="alt-card"
          >
            <template #header>
              <h3>其他方案</h3>
            </template>

            <el-collapse accordion>
              <el-collapse-item
                v-for="(alt, idx) in result.alternatives"
                :key="idx"
                :title="alt.description || `方案 ${idx + 1}`"
              >
                <ul class="alt-list">
                  <li v-for="p in alt.pois" :key="p.id">
                    {{ p.name }} ({{ getCategoryName(p.category) }})
                  </li>
                </ul>
                <div class="alt-meta">
                  <span>总时长: {{ alt.totalDuration }} 分钟</span>
                  <span>总花费: ¥{{ alt.totalCost }}</span>
                </div>
              </el-collapse-item>
            </el-collapse>
          </el-card>

          <!-- Actions -->
          <div class="action-bar">
            <el-button type="primary" @click="handleAdopt">采用此路线</el-button>
            <el-button @click="handleRegenerate">重新生成</el-button>
          </div>
        </template>

        <!-- Empty State -->
        <div v-else class="state-empty">
          <el-empty description="请在左侧填写需求开始规划" :image-size="140">
            <el-button type="primary" @click="scrollToInput">去填写</el-button>
          </el-empty>
        </div>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { routeApi } from '@/api/request'
import { ElMessage } from 'element-plus'

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
  if (!form.query.trim()) {
    ElMessage.warning('请输入您的出行需求')
    return
  }

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
    ElMessage.success('路线规划完成')
  } catch (error) {
    console.error('路线规划失败:', error)
    ElMessage.error('路线规划失败，请稍后重试')
  } finally {
    loading.value = false
  }
}

const handleAdopt = () => {
  ElMessage.success('已记录您的选择')
}

const handleRegenerate = () => {
  handlePlan()
}

const scrollToInput = () => {
  document.querySelector('.input-panel-col')?.scrollIntoView({ behavior: 'smooth' })
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
/* Design Tokens - 按 DESIGN.md */
.planner {
  --color-primary: #2563EB;
  --color-primary-hover: #1D4ED8;
  --color-primary-light: #DBEAFE;
  --color-surface: #F9FAFB;
  --color-border: #E5E7EB;
  --color-text-primary: #111827;
  --color-text-secondary: #6B7280;
  --color-text-muted: #9CA3AF;

  max-width: 1400px;
  margin: 0 auto;
  padding: 32px 24px;
}

/* Grid Layout */
.planner-grid {
  align-items: flex-start;
}

/* Input Panel */
.input-panel-col {
  position: sticky;
  top: 80px;
  align-self: flex-start;
}

.panel-card {
  border-radius: 8px;
  border: 1px solid var(--color-border);
}

.panel-card h2 {
  font-size: 18px;
  font-weight: 600;
  color: var(--color-text-primary);
  margin: 0;
}

/* Result Panel */
.result-panel-col {
  min-height: 600px;
}

/* States */
.state-loading {
  text-align: center;
  padding: 48px 24px;
  background: white;
  border-radius: 8px;
  border: 1px solid var(--color-border);
}

.loading-text {
  color: var(--color-text-muted);
  font-size: 13px;
  margin-top: 24px;
}

.state-empty {
  text-align: center;
  padding: 80px 20px;
  background: white;
  border-radius: 8px;
  border: 1px solid var(--color-border);
}

/* Description Card */
.desc-card {
  border-radius: 8px;
  border: 1px solid var(--color-border);
  background: var(--color-surface);
  margin-bottom: 20px;
}

.desc-card p {
  font-size: 15px;
  line-height: 1.7;
  color: var(--color-text-primary);
  margin: 0 0 12px 0;
}

/* Route Card */
.route-card {
  border-radius: 8px;
  border: 1px solid var(--color-border);
  margin-bottom: 20px;
}

.route-card h3 {
  font-size: 18px;
  font-weight: 600;
  color: var(--color-text-primary);
  margin: 0;
}

/* POI Card */
.poi-card {
  border-radius: 6px;
  border: 1px solid var(--color-border);
  background: var(--color-surface);
}

.poi-card h4 {
  font-size: 16px;
  font-weight: 600;
  color: var(--color-text-primary);
  margin: 0 0 12px 0;
}

.poi-desc {
  font-size: 13px;
  line-height: 1.5;
  color: var(--color-text-secondary);
  background: white;
  padding: 10px 12px;
  border-radius: 4px;
  margin: 10px 0 0 0;
}

.tag-list {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
  margin-top: 10px;
}

/* Alternatives Card */
.alt-card {
  border-radius: 8px;
  border: 1px solid var(--color-border);
  margin-bottom: 20px;
}

.alt-card h3 {
  font-size: 18px;
  font-weight: 600;
  color: var(--color-text-primary);
  margin: 0;
}

.alt-list {
  list-style: none;
  padding: 0;
  margin: 8px 0;
}

.alt-list li {
  padding: 6px 0;
  font-size: 14px;
  color: var(--color-text-secondary);
  border-bottom: 1px solid #F3F4F6;
}

.alt-list li:last-child {
  border-bottom: none;
}

.alt-meta {
  display: flex;
  gap: 24px;
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px solid var(--color-border);
  font-size: 13px;
  color: var(--color-text-muted);
}

/* Action Bar */
.action-bar {
  display: flex;
  justify-content: center;
  gap: 12px;
  margin-top: 32px;
}

/* Responsive */
@media (max-width: 992px) {
  .planner {
    padding: 16px;
  }

  .input-panel-col {
    position: static;
    margin-bottom: 24px;
  }

  .action-bar {
    flex-direction: column;
  }

  .action-bar .el-button {
    width: 100%;
  }
}
</style>
