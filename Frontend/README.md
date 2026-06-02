# 🎨 Router Frontend - 智能路线规划系统前端

基于 **Vue 3 + Vite + Element Plus** 构建的现代化前端界面。

## ✨ 技术栈

- **Vue 3** - 渐进式 JavaScript 框架（Composition API）
- **Vite** - 下一代前端构建工具
- **Vue Router 4** - 官方路由管理
- **Element Plus** - Vue 3 UI 组件库
- **Axios** - HTTP 客户端

## 🚀 快速开始

### 安装依赖

```bash
cd Frontend
npm install
```

### 启动开发服务器

```bash
npm run dev
```

访问：http://localhost:3000

### 构建生产版本

```bash
npm run build
```

构建产物输出到 `dist/` 目录。

## 📁 项目结构

```
Frontend/
├── src/
│   ├── api/              # API 接口封装
│   │   └── request.js    # Axios 配置和拦截器
│   ├── assets/           # 静态资源（图片、样式等）
│   ├── components/       # 公共组件
│   ├── router/           # 路由配置
│   │   └── index.js
│   ├── views/            # 页面组件
│   │   ├── Home.vue      # 首页
│   │   └── RoutePlanner.vue  # 路线规划主页面
│   ├── App.vue           # 根组件
│   └── main.js           # 应用入口
├── public/               # 公共静态资源
├── index.html            # HTML 入口
├── package.json          # 项目配置
└── vite.config.js        # Vite 配置
```

## 🔌 API 接口说明

前端通过 Axios 与后端 Spring Boot 服务通信，主要接口：

- `POST /api/route/plan` - 路线规划（核心接口）
- `GET /api/poi/category/{category}` - 按类别查询POI
- `GET /api/poi/hot` - 获取热门POI

开发环境已配置代理，请求会自动转发到后端 `http://localhost:8080`。

## 🎨 功能特性

### 首页 (Home)
- 系统介绍和特性展示
- 快速体验表单（一键规划）
- 数据统计展示

### 路线规划页面 (RoutePlanner)
- 自然语言输入（支持AI智能解析）
- 多维度参数配置：
  - 游玩时长选择
  - 预算范围滑块
  - 兴趣类别多选
  - 优化目标切换
  - 必去地点指定
- AI生成的智能描述展示
- 时间轴式路线可视化
- POI详细信息卡片
- 备选方案对比
- 用户反馈功能（采纳/重新生成/导出）

## ⚙️ 开发配置

### Vite 代理配置

在 `vite.config.js` 中已配置开发代理：

```javascript
server: {
  port: 3000,
  proxy: {
    '/api': {
      target: 'http://localhost:8080',  // 后端地址
      changeOrigin: true,
      rewrite: (path) => path.replace(/^\/api/, '')
    }
  }
}
```

这样前端调用 `/api/route/plan` 会自动转发到后端 `/route/plan`。

## 📱 响应式设计

- 支持桌面端和平板设备
- 使用 Element Plus 的栅格系统实现自适应布局
- 移动端优化（待完善）

## 🔐 注意事项

1. **跨域问题**：开发环境已通过 Vite 代理解决，生产环境需要配置 Nginx 或后端 CORS。
2. **API Key**：LLM功能需要在后端配置通义千问 API Key。
3. **地图集成**：当前为简化版本，可后续集成高德/百度地图 SDK。

## 🤝 与后端配合使用

1. 先启动后端服务（端口 8080）
2. 再启动前端开发服务器（端口 3000）
3. 访问 http://localhost:3000 开始使用

## 📄 License

MIT License
