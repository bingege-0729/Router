import { createRouter, createWebHistory } from 'vue-router'
import HomeView from '../views/Home.vue'
import RoutePlannerView from '../views/RoutePlanner.vue'

const routes = [
  {
    path: '/',
    name: 'home',
    component: HomeView,
    meta: { title: '首页' }
  },
  {
    path: '/route',
    name: 'route-planner',
    component: RoutePlannerView,
    meta: { title: '路线规划' }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  document.title = `${to.meta.title || '智能路线规划系统'} - Router`
  next()
})

export default router
