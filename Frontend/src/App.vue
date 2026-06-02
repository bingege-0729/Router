<template>
  <div id="app">
    <el-container class="app-container">
      <el-header class="app-header">
        <div class="header-content">
          <h1 class="logo">
            <el-icon><Location /></el-icon>
            智能路线规划系统
          </h1>
          <nav class="nav-menu">
            <router-link to="/" class="nav-link">首页</router-link>
            <router-link to="/route" class="nav-link">路线规划</router-link>
          </nav>
        </div>
      </el-header>

      <el-main class="app-main">
        <router-view />
      </el-main>

      <el-footer class="app-footer">
        <p>&copy; 2026 智能路线规划系统 | Powered by Vue 3 + Spring Boot + AI</p>
      </el-footer>
    </el-container>
  </div>
</template>

<script setup>
import { Location } from '@element-plus/icons-vue'
</script>

<style scoped>
.app-container {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

.app-header {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
}

.header-content {
  max-width: 1400px;
  margin: 0 auto;
  display: flex;
  justify-content: space-between;
  align-items: center;
  height: 100%;
}

.logo {
  margin: 0;
  font-size: 24px;
  font-weight: bold;
  display: flex;
  align-items: center;
  gap: 8px;
}

.nav-menu {
  display: flex;
  gap: 24px;
}

.nav-link {
  color: white;
  text-decoration: none;
  font-size: 16px;
  transition: opacity 0.3s;
}

.nav-link:hover {
  opacity: 0.8;
}

.nav-link.router-link-active {
  border-bottom: 2px solid white;
  padding-bottom: 4px;
}

.app-main {
  flex: 1;
  background-color: #f5f7fa;
  padding: 20px;
}

.app-footer {
  background-color: #fff;
  text-align: center;
  color: #909399;
  padding: 20px;
  border-top: 1px solid #e4e7ed;
}
</style>
