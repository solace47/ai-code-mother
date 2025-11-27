<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { useLoginUserStore } from '@/stores/loginUser'
import { addApp, getAppVoById, listMyAppVoByPage, listGoodAppVoByPage } from '@/api/appController'
import { getDeployUrl } from '@/config/env'
import AppCard from '@/components/AppCard.vue'

const router = useRouter()
const loginUserStore = useLoginUserStore()

// 用户提示词
const userPrompt = ref('')
const creating = ref(false)

// 在输入框中：Enter 发送，Shift+Enter 换行
const onPromptKeydown = (e: KeyboardEvent) => {
  if (e.key === 'Enter' && !e.shiftKey) {
    e.preventDefault()
    if (!creating.value) {
      createApp()
    }
  }
}

// 我的应用数据
const myApps = ref<API.AppVO[]>([])
const myAppsPage = reactive({
  current: 1,
  pageSize: 6,
  total: 0,
})

// 精选应用数据
const featuredApps = ref<API.AppVO[]>([])
const featuredAppsPage = reactive({
  current: 1,
  pageSize: 6,
  total: 0,
})

// 设置提示词
const setPrompt = (prompt: string) => {
  userPrompt.value = prompt
}

// 优化提示词功能已移除

// 创建应用
const createApp = async () => {
  if (!userPrompt.value.trim()) {
    message.warning('请输入应用描述')
    return
  }

  if (!loginUserStore.loginUser.id) {
    message.warning('请先登录')
    await router.push('/user/login')
    return
  }

  creating.value = true
  let loadingMessage = null

  try {
    console.log('开始创建应用，用户输入:', userPrompt.value.trim())

    const res = await addApp({
      initPrompt: userPrompt.value.trim(),
    })

    console.log('应用创建API响应:', res.data)

    if (res.data.code === 0 && res.data.data) {
      const appId = String(res.data.data)
      console.log('应用创建成功，ID:', appId)

      message.success('应用创建成功，正在准备环境...')

      // 显示加载提示
      loadingMessage = message.loading('正在验证应用状态，请稍候...', 0)

      // 改进的重试机制
      const maxRetries = 5  // 增加重试次数
      let retryCount = 0
      let navigateSuccess = false

      while (retryCount < maxRetries && !navigateSuccess) {
        try {
          // 动态调整等待时间：第一次立即检查，后续逐渐增加等待时间
          const waitTime = retryCount === 0 ? 100 : Math.min(1000 + retryCount * 800, 3000)
          console.log(`第 ${retryCount + 1} 次验证，等待时间: ${waitTime}ms`)

          await new Promise(resolve => setTimeout(resolve, waitTime))

          // 验证应用是否存在 - 修复：保持字符串格式传递ID
          const checkRes = await getAppVoById({ id: appId })
          console.log(`第 ${retryCount + 1} 次验证结果:`, checkRes.data)

          if (checkRes.data.code === 0 && checkRes.data.data) {
            console.log('应用验证成功，准备跳转')
            // 关闭加载提示
            if (loadingMessage) {
              loadingMessage()
              loadingMessage = null
            }

            message.success('应用准备完成，正在跳转...')
            // 如果应用存在，跳转到聊天页面
            await router.push(`/app/chat/${appId}`)
            navigateSuccess = true
            // 清空输入框
            userPrompt.value = ''
            break
          } else {
            throw new Error(`应用验证失败: ${checkRes.data.message || '应用尚未就绪'}`)
          }
        } catch (error) {
          retryCount++
          const errorMsg = error.response?.data?.message || error.message || '未知错误'
          console.warn(`第 ${retryCount} 次验证应用失败:`, errorMsg)

          if (retryCount >= maxRetries) {
            console.error('重试次数已达上限，验证失败')

            // 关闭加载提示
            if (loadingMessage) {
              loadingMessage()
              loadingMessage = null
            }

            // 根据错误类型给出不同的提示
            if (errorMsg.includes('不存在') || errorMsg.includes('NOT_FOUND')) {
              message.warning('应用创建成功，但需要更多时间准备。请稍后从"我的应用"中访问。', 5)
            } else {
              message.error(`应用访问失败: ${errorMsg}。请从"我的应用"中重新尝试。`, 4)
            }

            // 刷新我的应用列表
            await loadMyApps()
            // 清空输入框
            userPrompt.value = ''
            break
          }

          // 更新加载提示
          if (loadingMessage && retryCount < maxRetries) {
            loadingMessage()
            loadingMessage = message.loading(`正在准备应用环境 (${retryCount}/${maxRetries})...`, 0)
          }
        }
      }
    } else {
      const errorMsg = res.data.message || '未知错误'
      console.error('应用创建API返回错误:', errorMsg)
      message.error('创建失败：' + errorMsg)
    }
  } catch (error) {
    console.error('创建应用异常：', error)
    const errorMsg = error.response?.data?.message || error.message || '网络错误'
    message.error('创建失败：' + errorMsg)
  } finally {
    // 确保关闭所有加载提示
    if (loadingMessage) {
      loadingMessage()
    }
    creating.value = false
  }
}

// 加载我的应用
const loadMyApps = async () => {
  if (!loginUserStore.loginUser.id) {
    return
  }

  try {
    const res = await listMyAppVoByPage({
      pageNum: myAppsPage.current,
      pageSize: myAppsPage.pageSize,
      sortField: 'createTime',
      sortOrder: 'desc',
    })

    if (res.data.code === 0 && res.data.data) {
      myApps.value = res.data.data.records || []
      myAppsPage.total = res.data.data.totalRow || 0
    }
  } catch (error) {
    console.error('加载我的应用失败：', error)
  }
}

// 加载精选应用
const loadFeaturedApps = async () => {
  try {
    const res = await listGoodAppVoByPage({
      pageNum: featuredAppsPage.current,
      pageSize: featuredAppsPage.pageSize,
      sortField: 'createTime',
      sortOrder: 'desc',
    })

    if (res.data.code === 0 && res.data.data) {
      featuredApps.value = res.data.data.records || []
      featuredAppsPage.total = res.data.data.totalRow || 0
    }
  } catch (error) {
    console.error('加载精选应用失败：', error)
  }
}

// 查看对话
const viewChat = (appId: string | number | undefined) => {
  if (appId) {
    router.push(`/app/chat/${appId}?view=1`)
  }
}

// 查看作品
const viewWork = (app: API.AppVO) => {
  if (app.deployKey) {
    const url = getDeployUrl(app.deployKey)
    window.open(url, '_blank')
  }
}

// 格式化时间函数已移除，不再需要显示创建时间

// 页面加载时获取数据
onMounted(() => {
  loadMyApps()
  loadFeaturedApps()

  // 鼠标跟随光效
  const handleMouseMove = (e: MouseEvent) => {
    const { clientX, clientY } = e
    const { innerWidth, innerHeight } = window
    const x = (clientX / innerWidth) * 100
    const y = (clientY / innerHeight) * 100

    document.documentElement.style.setProperty('--mouse-x', `${x}%`)
    document.documentElement.style.setProperty('--mouse-y', `${y}%`)
  }

  document.addEventListener('mousemove', handleMouseMove)

  // 清理事件监听器
  return () => {
    document.removeEventListener('mousemove', handleMouseMove)
  }
})
</script>

<template>
  <div id="homePage">
    <!-- 区块1: 白色区域 - 标题和输入 -->
    <div class="zone zone-white">
      <div class="container">
        <div class="hero-section">
          <h1 class="hero-title">
            一句话
            <img class="hero-logo" src="@/assets/logo.svg" alt="NoCode Logo" />
            呈所想
          </h1>
          <p class="hero-description">与 AI 对话轻松创建应用和网站</p>
        </div>

        <div class="input-section">
          <a-textarea
            v-model:value="userPrompt"
            placeholder="帮我创建个人博客网站"
            :rows="4"
            :maxlength="1000"
            class="prompt-input"
            @keydown="onPromptKeydown"
          />
          <button
            type="button"
            class="send-btn"
            :class="{ 'is-disabled': !userPrompt.trim() || creating }"
            :disabled="!userPrompt.trim() || creating"
            @click="createApp"
          >
            <span v-if="creating" class="loading-icon">⏳</span>
            <span v-else class="arrow-icon">↑</span>
          </button>
        </div>

        <div class="quick-actions">
          <a-button
            type="default"
            @click="
              setPrompt(
                '创建一个简洁优雅的个人介绍博客，使用原生多文件模式，包含响应式设计和基本交互功能。使用语义化HTML结构，和现代CSS Grid和Flexbox布局，添加平滑滚动和主题切换功能。',
              )
            "
            >个人博客网站</a-button
          >
          <a-button
            type="default"
            @click="
              setPrompt(
                '设计一个专业的企业官网，包含公司介绍、产品服务展示、新闻资讯、联系我们等页面。采用商务风格的设计，包含轮播图、产品展示卡片、团队介绍、客户案例展示，支持多语言切换和在线客服功能。',
              )
            "
            >企业官网网站</a-button
          >
          <a-button
            type="default"
            @click="
              setPrompt(
                '创建一个功能完整的待办事项列表单文件应用，包含添加任务、标记完成、删除任务和本地存储功能。',
              )
            "
          >待办事项列表</a-button
          >
          <a-button
            type="default"
            @click="
              setPrompt(
                '制作一个精美的作品展示网站，适合设计师、摄影师、艺术家等创作者。包含作品画廊、项目详情页、个人简历、联系方式等模块。采用瀑布流或网格布局展示作品，支持图片放大预览和作品分类筛选。',
              )
            "
            >作品展示网站</a-button
          >
        </div>
      </div>
    </div>

    <!-- 区块2: 蓝绿色区域 - 我的作品 -->
    <div class="zone zone-teal">
      <div class="container">
        <div class="section">
          <h2 class="section-title">我的作品</h2>
          <div class="app-grid">
            <AppCard
              v-for="app in myApps"
              :key="app.id"
              :app="app"
              @view-chat="viewChat"
              @view-work="viewWork"
            />
          </div>
          <div class="pagination-wrapper">
            <a-pagination
              v-model:current="myAppsPage.current"
              v-model:page-size="myAppsPage.pageSize"
              :total="myAppsPage.total"
              :show-size-changer="false"
              :show-total="(total: number) => `共 ${total} 个应用`"
              @change="loadMyApps"
            />
          </div>
        </div>
      </div>
    </div>

    <!-- 区块3: 蓝色区域 - 精选案例 -->
    <div class="zone zone-blue">
      <div class="container">
        <div class="section">
          <h2 class="section-title">精选案例</h2>
          <div class="featured-grid">
            <AppCard
              v-for="app in featuredApps"
              :key="app.id"
              :app="app"
              :featured="true"
              @view-chat="viewChat"
              @view-work="viewWork"
            />
          </div>
          <div class="pagination-wrapper">
            <a-pagination
              v-model:current="featuredAppsPage.current"
              v-model:page-size="featuredAppsPage.pageSize"
              :total="featuredAppsPage.total"
              :show-size-changer="false"
              :show-total="(total: number) => `共 ${total} 个案例`"
              @change="loadFeaturedApps"
            />
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
#homePage {
  width: 100%;
  margin: 0;
  padding: 0;
  min-height: 100vh;
  position: relative;
  z-index: 1;
}

/* 区块基础样式 */
.zone {
  position: relative;
  width: 100%;
}

/* 白色区域 - 顶部（纯白色，无颗粒感） */
.zone-white {
  padding-top: calc(64px + var(--spacing-md)); /* 导航栏高度 + 间距 */
  padding-bottom: var(--spacing-xs);
}

/* 蓝绿色区域 - 我的作品（有颗粒感） */
.zone-teal {
  padding-top: var(--spacing-sm);
  padding-bottom: var(--spacing-md);
}

/* 蓝色区域 - 精选案例 */
.zone-blue {
  padding-top: var(--spacing-md);
  padding-bottom: var(--spacing-2xl);
}

.container {
  max-width: 1200px;
  margin: 0 auto;
  padding: var(--spacing-xl);
  position: relative;
  z-index: 2;
  width: 100%;
  box-sizing: border-box;
}

.zone-white .container {
  padding-bottom: var(--spacing-sm);
}

.zone-teal .container {
  padding-bottom: var(--spacing-sm);
}

.zone-blue .container {
  padding-top: var(--spacing-sm);
  padding-bottom: var(--spacing-2xl);
}

/* 移除居中光束效果 */

/* 英雄区域 */
.hero-section {
  text-align: center;
  padding: var(--spacing-2xl) 0 var(--spacing-xl);
  margin-bottom: var(--spacing-lg);
  color: var(--gray-800);
  position: relative;
  overflow: hidden;
}

.hero-title {
  display: inline-flex;
  align-items: center;
  gap: var(--spacing-sm);
  font-family: var(--font-family-ink);
  font-size: var(--font-size-4xl);
  font-weight: var(--font-weight-extrabold);
  margin: 0 0 var(--spacing-md);
  line-height: var(--line-height-tight);
  color: var(--gray-900);
  letter-spacing: 0.08em;
  position: relative;
  z-index: 2;
  text-shadow: 0 2px 4px rgba(0, 0, 0, 0.08);
}

.hero-logo {
  width: 40px;
  height: 40px;
  margin: 0 var(--spacing-xs);
  border-radius: 9999px;
  padding: 4px;
  background: radial-gradient(circle at 30% 30%, #2dd4bf, #06b6d4);
  box-shadow: 0 8px 20px rgba(0, 0, 0, 0.12);
}

@keyframes titleShimmer {
  0%,
  100% {
    background-position: 0% 50%;
  }
  50% {
    background-position: 100% 50%;
  }
}

.hero-description {
  font-size: var(--font-size-lg);
  margin: 0;
  color: var(--gray-500);
  font-weight: var(--font-weight-regular);
  letter-spacing: 0.04em;
  position: relative;
  z-index: 2;
}

/* 输入区域 - 单层边框设计 */
.input-section {
  position: relative;
  margin: 0 auto var(--spacing-md);
  max-width: 800px;
  background: linear-gradient(135deg, rgba(233, 247, 255, 0.95) 0%, #fff 50%, rgba(232, 248, 255, 0.9) 100%);
  border: 1px solid rgba(220, 235, 245, 0.8);
  border-radius: 24px;
  box-shadow: 0 8px 32px rgba(68, 184, 193, 0.15);
  padding: 0;
  transition: var(--transition-normal);
  overflow: visible;
}

/* 右上角两条斜线装饰 */
.input-section::before,
.input-section::after {
  content: '';
  position: absolute;
  height: 2px;
  border-radius: 2px;
  background: rgba(160, 180, 200, 0.5);
  transform: rotate(45deg);
  pointer-events: none;
  z-index: 10;
}

/* 长斜线 (内) */
.input-section::before {
  width: 20px;
  top: 22px;
  right: 12px;
}

/* 短斜线 (外) */
.input-section::after {
  width: 12px;
  top: 14px;
  right: 8px;
}

.prompt-input {
  width: 100%;
  border: none !important;
  box-shadow: none !important;
  font-size: 16px;
  font-family: var(--font-family-primary);
  padding: 20px 80px 60px 24px;
  background: transparent;
  resize: none;
  min-height: 120px;
  color: #4a5568;
  caret-color: #333;
}

.prompt-input::placeholder {
  color: #9ca3af;
}

.prompt-input:focus {
  background: transparent;
  box-shadow: none !important;
  border: none !important;
  outline: none;
}

/* 圆形发送按钮 */
.send-btn {
  position: absolute;
  right: 16px;
  bottom: 16px;
  width: 44px;
  height: 44px;
  border-radius: 50%;
  border: none;
  background: #1a1a1a;
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  cursor: pointer;
  transition: background 0.2s ease, transform 0.15s ease, box-shadow 0.2s ease;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.send-btn:not(:disabled):hover {
  transform: translateY(-1px);
  box-shadow: 0 6px 16px rgba(0, 0, 0, 0.2);
}

.send-btn.is-disabled {
  background: #d1d5db;
  cursor: not-allowed;
  box-shadow: none;
}

.send-btn .arrow-icon {
  font-size: 20px;
  line-height: 1;
}

.send-btn .loading-icon {
  font-size: 18px;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

/* 快捷按钮 */
.quick-actions {
  display: flex;
  gap: var(--spacing-md);
  justify-content: center;
  margin-bottom: var(--spacing-md);
  margin-top: var(--spacing-sm);
  flex-wrap: wrap;
  padding: var(--spacing-sm) 0;
}

.quick-actions .ant-btn {
  border-radius: var(--radius-full);
  padding: var(--spacing-sm) var(--spacing-lg);
  height: auto;
  background: var(--white);
  border: 1px solid var(--gray-300);
  color: var(--gray-700);
  font-weight: var(--font-weight-medium);
  font-family: var(--font-family-primary);
  box-shadow: var(--shadow-sm);
  transition: var(--transition-normal);
  position: relative;
  overflow: hidden;
}

.quick-actions .ant-btn::before {
  content: '';
  position: absolute;
  top: 0;
  left: -100%;
  width: 100%;
  height: 100%;
  background: linear-gradient(90deg, transparent, rgba(110, 231, 183, 0.25), rgba(249, 168, 212, 0.25), transparent);
  transition: left 0.5s;
}

.quick-actions .ant-btn:hover::before {
  left: 100%;
}

.quick-actions .ant-btn:hover {
  background: var(--gray-50);
  border-color: #f9a8d4 !important;
  color: #ec4899 !important;
  transform: translateY(-2px);
  box-shadow: var(--shadow-md);
}

.quick-actions .ant-btn:focus,
.quick-actions .ant-btn:focus-visible {
  outline: 2px solid #f9a8d4;
  outline-offset: 2px;
}

/* 区域标题 */
.section {
  margin-bottom: var(--spacing-md);
  background: var(--card-gradient);
  backdrop-filter: var(--glass-backdrop);
  border: var(--glass-border);
  border-radius: var(--radius-2xl);
  box-shadow: var(--shadow-lg);
  padding: var(--spacing-xl);
  transition: var(--transition-normal);
}

.zone-blue .section {
  margin-bottom: var(--spacing-2xl);
}

.section:hover {
  transform: translateY(-2px);
  box-shadow: var(--shadow-xl);
}

.section-title {
  font-size: var(--font-size-3xl);
  font-weight: var(--font-weight-semibold);
  margin-bottom: var(--spacing-xl);
  color: var(--gray-800);
  font-family: var(--font-family-primary);
  text-align: center;
}

/* 我的作品网格 */
.app-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: var(--spacing-xl);
  margin-bottom: var(--spacing-xl);
}

/* 精选案例网格 */
.featured-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: var(--spacing-xl);
  margin-bottom: var(--spacing-xl);
}

/* 分页 */
.pagination-wrapper {
  display: flex;
  justify-content: center;
  margin-top: var(--spacing-xl);
  background: var(--glass-gradient);
  backdrop-filter: var(--glass-backdrop);
  border: var(--glass-border);
  border-radius: var(--radius-xl);
  box-shadow: var(--shadow-md);
  padding: var(--spacing-lg);
  transition: var(--transition-normal);
}

/* Ant Design 组件样式覆盖 */
:deep(.ant-textarea) {
  transition: var(--transition-normal);
}

:deep(.ant-btn) {
  transition: var(--transition-fast);
}

:deep(.ant-btn-primary) {
  background: var(--button-gradient-primary);
  border: none;
  border-radius: var(--radius-full);
  font-weight: var(--font-weight-medium);
  box-shadow: var(--shadow-sm);
  transition: var(--transition-fast);
  padding: var(--spacing-sm) var(--spacing-xl);
}

:deep(.ant-btn-primary:hover) {
  background: var(--button-gradient-secondary);
  transform: translateY(-2px);
  box-shadow: var(--shadow-lg);
}

:deep(.ant-pagination) {
  font-family: var(--font-family-primary);
}

:deep(.ant-pagination-item) {
  background: rgba(255, 255, 255, 0.8);
  border: 1px solid rgba(99, 102, 241, 0.2);
  border-radius: var(--radius-md);
  transition: var(--transition-fast);
}

:deep(.ant-pagination-item:hover) {
  background: rgba(99, 102, 241, 0.1);
  border-color: var(--primary-color);
}

:deep(.ant-pagination-item-active) {
  background: var(--primary-color);
  border-color: var(--primary-color);
}

/* 响应式设计 */
@media (max-width: 768px) {
  .container {
    padding: var(--spacing-lg);
  }

  .hero-section {
    padding: var(--spacing-3xl) 0 var(--spacing-2xl);
    margin-bottom: var(--spacing-lg);
  }

  .hero-title {
    font-size: var(--font-size-3xl);
  }

  .hero-description {
    font-size: var(--font-size-lg);
  }

  .app-grid,
  .featured-grid {
    grid-template-columns: 1fr;
    gap: var(--spacing-lg);
  }

  .quick-actions {
    justify-content: center;
    padding: var(--spacing-lg);
  }

  .section {
    padding: var(--spacing-lg);
  }

  .section-title {
    font-size: var(--font-size-2xl);
  }
}
</style>
