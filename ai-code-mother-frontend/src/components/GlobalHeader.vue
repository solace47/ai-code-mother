<template>
  <a-layout-header class="header" :class="{ 'header-scrolled': isScrolled }">
    <div class="header-content" :class="{ 'header-content-scrolled': isScrolled }">
      <!-- 左侧：Logo和标题 -->
      <div class="header-left">
        <RouterLink to="/">
          <div class="logo-section">
            <img class="logo" src="@/assets/logo.svg" alt="NoCode Logo" />
            <h1 class="site-title">NoCode</h1>
          </div>
        </RouterLink>
      </div>
      <!-- 中间：导航菜单 -->
      <div class="header-center">
        <div class="custom-menu">
          <div
            v-for="item in menuItems"
            :key="item.key"
            class="menu-item"
            :class="{ 'menu-item-selected': selectedKeys.includes(item.key) }"
            @click="handleMenuItemClick(item)"
          >
            <component :is="item.icon" v-if="item.icon" class="menu-icon" />
            <span class="menu-label">{{ getMenuLabel(item) }}</span>
          </div>
        </div>
      </div>
      <!-- 右侧：用户操作区域 -->
      <div class="header-right">
        <div class="user-login-status">
          <div v-if="loginUserStore.loginUser.id">
            <a-dropdown>
              <a-space>
                <a-avatar :src="loginUserStore.loginUser.userAvatar" />
                {{ loginUserStore.loginUser.userName ?? '无名' }}
              </a-space>
              <template #overlay>
                <a-menu>
                  <a-menu-item @click="goToProfile">
                    <UserOutlined />
                    个人主页
                  </a-menu-item>
                  <a-menu-divider />
                  <a-menu-item @click="goToPointsDetail">
                    <WalletOutlined />
                    积分明细
                  </a-menu-item>
                  <a-menu-item @click="goToSignIn">
                    <CalendarOutlined />
                    每日签到
                  </a-menu-item>
                  <a-menu-item @click="goToInvite">
                    <TeamOutlined />
                    邀请好友
                  </a-menu-item>
                  <a-menu-divider />
                  <a-menu-item @click="doLogout">
                    <LogoutOutlined />
                    退出登录
                  </a-menu-item>
                </a-menu>
              </template>
            </a-dropdown>
          </div>
          <div v-else>
            <a-button class="login-btn" href="/user/login">登录</a-button>
          </div>
        </div>
      </div>
    </div>
  </a-layout-header>
</template>

<script setup lang="ts">
import { computed, h, ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { type MenuProps, message } from 'ant-design-vue'
import { useLoginUserStore } from '@/stores/loginUser.ts'
import { userLogout } from '@/api/userController.ts'
import { LogoutOutlined, HomeOutlined, UserOutlined, AppstoreOutlined, GithubOutlined, WalletOutlined, CalendarOutlined, TeamOutlined, BookOutlined } from '@ant-design/icons-vue'
import PointsDisplay from './PointsDisplay.vue'

const loginUserStore = useLoginUserStore()
const router = useRouter()

// 滚动状态
const isScrolled = ref(false)

const handleScroll = () => {
  const scrollTop =
    document.body.scrollTop ||
    document.documentElement.scrollTop ||
    window.scrollY ||
    0
  isScrolled.value = scrollTop > 0
}

onMounted(() => {
  document.body.addEventListener('scroll', handleScroll)
  handleScroll()
})

onUnmounted(() => {
  document.body.removeEventListener('scroll', handleScroll)
})

// 当前选中菜单
const selectedKeys = ref<string[]>(['/'])
// 监听路由变化，更新当前选中菜单
router.afterEach((to, from, next) => {
  selectedKeys.value = [to.path]
})

// 菜单配置项
const originItems = [
  {
    key: '/',
    icon: () => h(HomeOutlined),
    label: '主页',
    title: '主页',
  },
  {
    key: '/docs',
    icon: () => h(BookOutlined),
    label: '使用文档',
    title: '使用文档',
  },
  {
    key: '/admin/userManage',
    icon: () => h(UserOutlined),
    label: '用户管理',
    title: '用户管理',
  },
  {
    key: '/admin/appManage',
    icon: () => h(AppstoreOutlined),
    label: '应用管理',
    title: '应用管理',
  },
  {
    key: 'others',
    icon: () => h(GithubOutlined),
    label: h('a', { href: 'https://github.com/solace47/ai-code-mother', target: '_blank' }, '项目仓库'),
    title: '项目仓库',
  },
]

// 过滤菜单项
const filterMenus = (menus = [] as MenuProps['items']) => {
  return menus?.filter((menu) => {
    const menuKey = menu?.key as string
    if (menuKey?.startsWith('/admin')) {
      const loginUser = loginUserStore.loginUser
      if (!loginUser || loginUser.userRole !== 'admin') {
        return false
      }
    }
    return true
  })
}

// 展示在菜单的路由数组
const menuItems = computed<MenuProps['items']>(() => filterMenus(originItems))

// 处理菜单点击
const handleMenuClick: MenuProps['onClick'] = (e) => {
  const key = e.key as string
  selectedKeys.value = [key]
  // 跳转到对应页面
  if (key.startsWith('/')) {
    router.push(key)
  }
}

// 处理自定义菜单项点击
const handleMenuItemClick = (item: any) => {
  const key = item.key as string
  selectedKeys.value = [key]

  if (key === 'others') {
    // 项目仓库是外部链接
    window.open('https://github.com/solace47/ai-code-mother', '_blank')
  } else if (key.startsWith('/')) {
    // 内部路由跳转
    router.push(key)
  }
}

// 获取菜单标签文本
const getMenuLabel = (item: any) => {
  if (typeof item.label === 'string') {
    return item.label
  }
  return item.title || ''
}

// 跳转到个人主页
const goToProfile = () => {
  router.push('/user/profile')
}

const goToPointsDetail = () => {
  router.push('/points/detail')
}

const goToSignIn = () => {
  router.push('/points/sign-in')
}

const goToInvite = () => {
  router.push('/points/invite')
}

// 退出登录
const doLogout = async () => {
  const res = await userLogout()
  if (res.data.code === 0) {
    loginUserStore.setLoginUser({
      userName: '未登录',
    })
    message.success('退出登录成功')
    await router.push('/user/login')
  } else {
    message.error('退出登录失败，' + res.data.message)
  }
}
</script>

<style scoped>
.header {
  background: transparent !important;
  padding: 0 var(--spacing-xl);
  transition: all 0.3s ease;
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: 1000;
}

.header.header-scrolled {
  background: transparent !important;
}

.header-content {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  max-width: 1120px;
  margin: var(--spacing-sm) auto;
  padding: 0 var(--spacing-lg);
  height: 56px;
  border-radius: 999px;
  background: #ffffff;
  box-shadow: none;
  border: 1px solid rgba(255, 255, 255, 0.85);
  transition: background 0.3s ease, box-shadow 0.3s ease, transform 0.3s ease, backdrop-filter 0.3s ease;
}

.header-content-scrolled {
}

.header-left {
  flex-shrink: 0;
}

.logo-section {
  display: flex;
  align-items: center;
  gap: var(--spacing-md);
}

.logo {
  height: 40px;
  width: 40px;
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-sm);
  transition: var(--transition-fast);
}

.logo:hover {
  transform: scale(1.05);
  box-shadow: var(--shadow-md);
}

.site-title {
  margin: 0;
  font-size: var(--font-size-xl);
  font-weight: var(--font-weight-bold);
  background: linear-gradient(120deg, #6ee7b7 0%, #f9a8d4 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  transition: var(--transition-fast);
  white-space: nowrap;
}

.header-center {
  flex: 1;
  display: flex;
  justify-content: center;
  margin: 0 var(--spacing-xl);
  min-width: 400px;
  overflow: visible;
}

.header-right {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  gap: var(--spacing-md);
}

.user-login-status {
  display: flex;
  align-items: center;
  gap: var(--spacing-md);
}

/* 自定义菜单样式 - 优化版本 */
.custom-menu {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: var(--spacing-xs);
  flex-wrap: nowrap;
}

.menu-item {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 0 18px;
  height: 44px;
  border-radius: 22px;
  font-weight: var(--font-weight-medium);
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  cursor: pointer;
  white-space: nowrap;
  flex-shrink: 0;
  position: relative;
  overflow: hidden;
}

.menu-item::after {
  content: '';
  position: absolute;
  left: 18px;
  right: 18px;
  bottom: 6px;
  height: 2px;
  border-radius: 999px;
  background: linear-gradient(120deg, #6ee7b7 0%, #f9a8d4 100%);
  transform: scaleX(0);
  transform-origin: center;
  transition: transform 0.25s ease-out, opacity 0.25s ease-out;
  opacity: 0;
}

.menu-item:hover::after,
.menu-item-selected::after {
  transform: scaleX(1);
  opacity: 1;
}

.menu-item-selected {
  color: var(--gray-900);
  font-weight: 600;
}

.menu-icon {
  font-size: 18px;
  flex-shrink: 0;
  position: relative;
  z-index: 1;
}

.menu-label {
  font-size: 15px;
  flex-shrink: 0;
  position: relative;
  z-index: 1;
}

/* 用户下拉菜单样式 */
:deep(.ant-dropdown-menu) {
  background: var(--glass-gradient);
  backdrop-filter: var(--glass-backdrop);
  border: var(--glass-border);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-xl);
  padding: var(--spacing-sm);
}

:deep(.ant-dropdown-menu-item) {
  border-radius: var(--radius-md);
  transition: var(--transition-fast);
}

:deep(.ant-dropdown-menu-item:hover) {
  background: rgba(99, 102, 241, 0.1);
  color: var(--primary-color);
}

/* 头像样式 */
:deep(.ant-avatar) {
  border: 2px solid rgba(255, 255, 255, 0.3);
  box-shadow: var(--shadow-sm);
  transition: var(--transition-fast);
}

:deep(.ant-avatar:hover) {
  border-color: var(--primary-color);
  box-shadow: var(--shadow-md);
}

:deep(.ant-btn-primary) {
  background: var(--gray-900);
  border: none;
  border-radius: var(--radius-lg);
  height: 40px;
  padding: 0 24px;
  font-size: 14px;
  font-weight: 500;
  box-shadow: none;
  transition: var(--transition-normal);
}

:deep(.ant-btn-primary:hover) {
  background: var(--gray-800);
  transform: none;
  box-shadow: none;
}

/* 登录按钮自定义样式 */
.login-btn {
  background: var(--gray-900);
  color: var(--white);
  border: 1px solid var(--gray-900);
  border-radius: 4px;
  height: 36px;
  padding: 0 18px;
  font-size: 14px;
  font-weight: 500;
  transition: var(--transition-normal);
}

.login-btn:hover {
  background: linear-gradient(120deg, #6ee7b7 0%, #f9a8d4 100%);
  border-color: #f9a8d4 !important;
  color: var(--gray-900) !important;
}

.login-btn:focus,
.login-btn:focus-visible {
  outline: 2px solid #f9a8d4;
  outline-offset: 2px;
}


/* 响应式设计 */
@media (max-width: 768px) {
  .header {
    margin: 0 var(--spacing-sm) var(--spacing-md) var(--spacing-sm);
    padding: 0 var(--spacing-lg);
    border-radius: 0 0 var(--radius-lg) var(--radius-lg);
  }

  .header-content {
    height: 56px;
  }

  .site-title {
    font-size: var(--font-size-base);
  }

  .logo {
    height: 32px;
    width: 32px;
  }

  .header-center {
    margin: 0 var(--spacing-xs);
    flex: 1;
    display: flex;
    justify-content: center;
    overflow: visible;
  }

  .custom-menu {
    gap: var(--spacing-xs);
    flex-wrap: nowrap;
    overflow: visible;
  }

  .menu-item {
    height: 32px;
    padding: 0 8px;
    font-size: var(--font-size-sm);
  }

  .menu-icon {
    font-size: 14px;
  }

  .menu-label {
    font-size: 12px;
  }
}
</style>
