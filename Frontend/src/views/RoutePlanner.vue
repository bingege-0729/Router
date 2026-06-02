<template>
  <div class="route-planner">
    <el-row :gutter="24">
      <!-- 左侧：输入面板 -->
      <el-col :xs="24" :lg="8" class="input-panel">
        <el-card class="query-card" shadow="always">
          <template #header>
            <div class="card-header">
              <span>🎯 路线规划</span>
              <el-tag type="success" size="small">AI 增强</el-tag>
            </div>
          </template>

          <el-form :model="routeForm" label-position="top" class="route-form">
            <el-form-item label="您的需求（自然语言）">
              <el-input
                v-model="routeForm.query"
                type="textarea"
                :rows="3"
                placeholder="例如：周末想去公园和博物馆，不想排队，预算500元内"
                maxlength="200"
                show-word-limit
              />
            </el-form-item>

            <el-divider content-position="left">详细参数</el-divider>

            <el-form-item label="游玩时长">
              <el-select v-model="routeForm.totalHours" placeholder="选择时长">
                <el-option label="2小时（快速）" :value="2" />
                <el-option label="4小时（半天）" :value="4" />
                <el-option label="6小时（推荐）" :value="6" />
                <el-option label="8小时（全天）" :value="8" />
                <el-option label="12小时（深度游）" :value="12" />
              </el-select>
            </el-form-item>

            <el-form-item label="预算范围 (元)">
              <el-slider 
                v-model="routeForm.maxBudget" 
                :min="100" 
                :max="2000" 
                :step="50"
                show-input
              />
            </el-form-item>

            <el-form-item label="兴趣类别（可多选）">
              <el-checkbox-group v-model="routeForm.categories">
                <el-checkbox label="RESTAURANT">🍽️ 餐厅</el-checkbox>
                <el-checkbox label="ATTRACTION">🎡 景点</el-checkbox>
                <el-checkbox label="PARK">🌳 公园</el-checkbox>
                <el-checkbox label="MUSEUM">🏛️ 博物馆</el-checkbox>
                <el-checkbox label="SHOPPING">🛍️ 购物</el-checkbox>
                <el-checkbox label="CAFE">☕ 咖啡</el-checkbox>
              </el-checkbox-group>
            </el-form-item>

            <el-form-item label="优化目标">
              <el-radio-group v-model="routeForm.optimizationGoal">
                <el-radio-button value="BALANCED">⚖️ 综合平衡</el-radio-button>
                <el-radio-button value="SHORTEST_TIME">⚡ 省时间</el-radio-button>
                <el-radio-button value="LEAST_WALKING">🚶 少走路</el-radio-button>
                <el-radio-button value="LOWEST_COST">💰 省钱</el-radio-button>
                <el-radio-button value="LEAST_WAITING">⏰ 不排队</el-radio-button>
                <el-radio-button value="HIGHEST_RATED">⭐ 高评分</el-radio-button>
              </el-radio-group>
            </el-form-item>

            <el-form-item label="必去地点">
              <el-select
                v-model="routeForm.mustVisit"
                multiple
                filterable
                allow-create
                default-first-option
                placeholder="输入或选择必去地点"
                style="width: 100%"
              >
                <!-- 动态加载热门POI -->
              </el-select>
            </el-form-item>

            <el-form-item>
              <el-button 
                type="primary" 
                @click="handlePlanRoute"
                :loading="loading"
                style="width: 100%"
                size="large"
              >
                {{ loading ? '🤖 AI 正在智能规划...' : '🚀 开始生成最优路线' }}
              </el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </el-col>

      <!-- 右侧：结果展示 -->
      <el-col :xs="24" :lg="16" class="result-panel">
        <!-- 加载状态 -->
        <div v-if="loading" class="loading-container">
          <el-skeleton :rows="10" animated />
          <p class="loading-text">AI正在分析您的需求并优化路线...</p>
        </div>

        <!-- 结果展示 -->
        <div v-else-if="routeResult" class="result-container">
          <!-- AI 描述 -->
          <el-card class="ai-description-card" shadow="hover">
            <div class="ai-message">
              <el-icon><ChatDotRound /></el-icon>
              <p>{{ routeResult.message }}</p>
            </div>
            
            <div v-if="routeResult.personalizationNote" class="personalization-note">
              <el-tag type="warning" effect="dark">{{ routeResult.personalizationNote }}</el-tag>
            </div>
          </el-card>

          <!-- 主路线 -->
          <el-card class="route-card" shadow="hover">
            <template #header>
              <div class="card-header">
                <span>📍 推荐路线</span>
                <el-tag type="success">主方案</el-tag>
              </div>
            </template>

            <el-timeline v-if="routeResult.mainRoute && routeResult.mainRoute.pois">
              <el-timeline-item
                v-for="(poi, index) in routeResult.mainRoute.pois"
                :key="poi.id || index"
                :timestamp="'第 ' + (index + 1) + ' 站'"
                placement="top"
                :type="getTimelineType(index)"
              >
                <el-card shadow="never" class="poi-card">
                  <h4>{{ poi.name }}</h4>
                  <el-descriptions :column="2" size="small" border>
                    <el-descriptions-item label="类别">
                      <el-tag size="small">{{ getCategoryName(poi.category) }}</el-tag>
                    </el-descriptions-item>
                    <el-descriptions-item label="评分">
                      <el-rate 
                        v-model="poi.rating" 
                        disabled 
                        show-score 
                        text-color="#ff9900"
                      />
                    </el-descriptions-item>
                    <el-descriptions-item label="人均消费">
                      ¥{{ poi.avgCost || '免费' }}
                    </el-descriptions-item>
                    <el-descriptions-item label="建议停留">
                      {{ poi.recommendedDuration || 90 }} 分钟
                    </el-descriptions-item>
                  </el-descriptions>
                  
                  <div v-if="poi.description" class="poi-highlight">
                    <el-alert 
                      :title="poi.description" 
                      type="info" 
                      :closable="false"
                      show-icon
                    />
                  </div>
                  
                  <div class="poi-tags" v-if="poi.tags && poi.tags.length">
                    <el-tag 
                      v-for="tag in poi.tags" 
                      :key="tag" 
                      size="small" 
                      class="poi-tag"
                    >
                      {{ tag }}
                    </el-tag>
                  </div>
                </el-card>
              </el-timeline-item>
            </el-timeline>

            <div v-else class="empty-route">
              <el-empty description="暂无路线数据" />
            </div>
          </el-card>

          <!-- 备选方案 -->
          <el-card 
            v-if="routeResult.alternatives && routeResult.alternatives.length > 0"
            class="alternatives-card"
            shadow="hover"
          >
            <template #header>
              <div class="card-header">
                <span>💡 其他推荐方案</span>
              </div>
            </template>

            <el-collapse accordion>
              <el-collapse-item 
                v-for="(alt, index) in routeResult.alternatives" 
                :key="index"
                :title="alt.description || `备选方案 ${index + 1}`"
              >
                <div class="alternative-content">
                  <p><strong>包含POI：</strong></p>
                  <ul>
                    <li v-for="poi in alt.pois" :key="poi.id">
                      {{ poi.name }} ({{ getCategoryName(poi.category) }})
                    </li>
                  </ul>
                  <p><strong>总时长：</strong>{{ alt.totalDuration }} 分钟</p>
                  <p><strong>总花费：</strong>¥{{ alt.totalCost }}</p>
                </div>
              </el-collapse-item>
            </el-collapse>
          </el-card>

          <!-- 操作按钮 -->
          <div class="action-buttons">
            <el-button type="primary" @click="handleAdoptRoute">
              <el-icon><Check /></el-icon>
              采用此路线
            </el-button>
            <el-button @click="handleRegenerate">
              <el-icon><RefreshRight /></el-icon>
              重新生成
            </el-button>
            <el-button @click="handleExport">
              <el-icon><Download /></el-icon>
              导出分享
            </el-button>
          </div>
        </div>

        <!-- 空状态 -->
        <div v-else class="empty-state">
          <el-empty description="请在左侧填写需求开始规划" :image-size="200">
            <el-button type="primary" @click="$refs.queryForm?.scrollIntoView()">
              去填写
            </el-button>
          </el-empty>
        </div>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { 
  ChatDotRound, Check, RefreshRight, Download 
} from '@element-plus/icons-vue'
import { routeApi } from '@/api/request'
import { ElMessage } from 'element-plus'

const route = useRoute()
const loading = ref(false)
const routeResult = ref(null)

const routeForm = reactive({
  query: '',
  totalHours: 6,
  maxBudget: 500,
  categories: [],
  optimizationGoal: 'BALANCED',
  mustVisit: []
})

onMounted(() => {
  if (route.query.result) {
    try {
      routeResult.value = JSON.parse(route.query.result)
    } catch (e) {
      console.error('解析路由参数失败:', e)
    }
  }
})

const handlePlanRoute = async () => {
  if (!routeForm.query.trim()) {
    ElMessage.warning('请输入您的出行需求')
    return
  }

  loading.value = true

  try {
    const response = await routeApi.planRoute({
      query: routeForm.query,
      startLat: 30.2741,
      startLng: 120.1551,
      totalHours: routeForm.totalHours,
      categories: routeForm.categories.length > 0 ? routeForm.categories : undefined,
      maxBudget: routeForm.maxBudget,
      optimizationGoal: routeForm.optimizationGoal,
      mustVisit: routeForm.mustVisit.length > 0 ? routeForm.mustVisit : undefined
    })

    // ✅ 正确解构Result包装类
    if (response && response.data) {
      routeResult.value = response.data
    } else {
      routeResult.value = response
    }

    ElMessage.success('✅ 路线规划完成！')

    console.log('📊 路线规划结果:', routeResult.value)

  } catch (error) {
    console.error('❌ 路线规划失败:', error)
    ElMessage.error('路线规划失败，请稍后重试')
  } finally {
    loading.value = false
  }
}

const handleAdoptRoute = () => {
  ElMessage.success('已记录您的选择！系统将根据此优化推荐')
  
  // TODO: 调用用户反馈API
}

const handleRegenerate = () => {
  handlePlanRoute()
}

const handleExport = () => {
  ElMessage.info('导出功能开发中...')
}

const getTimelineType = (index) => {
  const types = ['primary', 'success', 'warning', 'danger', 'info']
  return types[index % types.length]
}

const getCategoryName = (category) => {
  const categoryMap = {
    RESTAURANT: '🍽️ 餐厅',
    ATTRACTION: '🎡 景点',
    PARK: '🌳 公园',
    MUSEUM: '🏛️ 博物馆',
    SHOPPING: '🛍️ 购物',
    CAFE: '☕ 咖啡'
  }
  return categoryMap[category] || category || '未知'
}
</script>

<style scoped>
.route-planner {
  max-width: 1600px;
  margin: 0 auto;
}

.input-panel {
  margin-bottom: 20px;
}

.query-card {
  position: sticky;
  top: 80px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 18px;
  font-weight: bold;
}

.route-form {
  padding: 10px 0;
}

.result-panel {
  min-height: 600px;
}

.loading-container {
  text-align: center;
  padding: 40px;
}

.loading-text {
  color: #909399;
  margin-top: 20px;
  font-size: 14px;
}

.ai-description-card {
  margin-bottom: 20px;
  background: linear-gradient(135deg, #f5f7fa 0%, #c3cfe2 100%);
}

.ai-message {
  display: flex;
  gap: 12px;
  font-size: 15px;
  line-height: 1.8;
  color: #303133;
}

.ai-message .el-icon {
  font-size: 24px;
  color: #409eff;
  flex-shrink: 0;
  margin-top: 4px;
}

.personalization-note {
  margin-top: 12px;
}

.route-card {
  margin-bottom: 20px;
}

.poi-card {
  margin-bottom: 8px;
}

.poi-card h4 {
  margin: 0 0 12px 0;
  font-size: 18px;
  color: #303133;
}

.poi-highlight {
  margin-top: 12px;
}

.poi-tags {
  margin-top: 8px;
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
}

.poi-tag {
  margin: 0;
}

.alternatives-card {
  margin-bottom: 20px;
}

.alternative-content ul {
  list-style: none;
  padding: 0;
  margin: 8px 0;
}

.alternative-content li {
  padding: 4px 0;
  color: #606266;
}

.action-buttons {
  display: flex;
  gap: 12px;
  justify-content: center;
  margin: 30px 0;
}

.empty-state {
  text-align: center;
  padding: 60px 20px;
}
</style>
