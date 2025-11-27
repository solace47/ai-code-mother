<template>
  <div id="userLoginPage">
    <div class="login-background">
      <div class="bg-shapes">
        <div class="shape shape-1"></div>
        <div class="shape shape-2"></div>
        <div class="shape shape-3"></div>
        <div class="shape shape-4"></div>
      </div>
    </div>

    <div class="login-container">
      <div class="login-card">
        <div class="login-header">
          <div class="logo-section">
            <h1 class="brand-title">AI 应用生成</h1>
          </div>
          <h2 class="page-title">欢迎回来</h2>
          <p class="page-subtitle">登录您的账户，开始创造之旅</p>
        </div>

        <div class="login-form">
          <a-form :model="formState" name="basic" autocomplete="off" @finish="handleSubmit">
            <a-form-item
              name="userEmail"
              :rules="[
                { required: true, message: '请输入您的邮箱' },
                { type: 'email', message: '请输入正确的邮箱格式' }
              ]"
            >
              <a-input
                v-model:value="formState.userEmail"
                placeholder="邮箱"
                size="large"
              />
            </a-form-item>

            <a-form-item
              name="userPassword"
              :rules="[
                { required: true, message: '请输入您的密码' },
                { min: 8, message: '密码长度不能小于 8 位' },
              ]"
            >
              <a-input-password
                v-model:value="formState.userPassword"
                placeholder="密码"
                size="large"
              />
            </a-form-item>

            <div class="form-options">
              <RouterLink to="/user/reset-password" class="forgot-password">忘记密码?</RouterLink>
            </div>

            <a-form-item class="submit-item">
              <a-button type="primary" html-type="submit" size="large" class="login-btn">
                <span class="btn-text">登录</span>
                <span class="btn-arrow">→</span>
              </a-button>
            </a-form-item>
          </a-form>

          <div class="login-footer">
            <div class="register-link">
              <span>还没有账号？</span>
              <RouterLink to="/user/register" class="link">立即注册</RouterLink>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
<script lang="ts" setup>
import { reactive } from 'vue'
import { userLogin } from '@/api/userController.ts'
import { useLoginUserStore } from '@/stores/loginUser.ts'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'

const formState = reactive<API.UserLoginRequest>({
  userEmail: '',
  userPassword: '',
})

const router = useRouter()
const loginUserStore = useLoginUserStore()

/**
 * 提交表单
 * @param values
 */
const handleSubmit = async (values: any) => {
  const res = await userLogin(values)
  // 登录成功，把登录态保存到全局状态中
  if (res.data.code === 0 && res.data.data) {
    await loginUserStore.fetchLoginUser()
    message.success('登录成功')
    router.push({
      path: '/',
      replace: true,
    })
  } else {
    message.error('登录失败，' + res.data.message)
  }
}
</script>

<style scoped>
#userLoginPage {
	min-height: calc(100vh - 64px);
	padding-top: 64px;
	box-sizing: border-box;
	position: relative;
	display: flex;
	align-items: center;
	justify-content: center;
	overflow: hidden;
	font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', 'PingFang SC', 'Hiragino Sans GB', 'Microsoft YaHei', 'Helvetica Neue', Helvetica, Arial, sans-serif;
}

/* 动态背景 */
.login-background {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: transparent;
}

/* 浮动装饰元素 */
.bg-shapes {
  display: none;
}

.shape {
  position: absolute;
  background: rgba(255, 255, 255, 0.1);
  border-radius: 50%;
  backdrop-filter: blur(10px);
  animation: float 6s ease-in-out infinite;
}

.shape-1 {
  width: 200px;
  height: 200px;
  top: 10%;
  left: 10%;
  animation-delay: 0s;
}

.shape-2 {
  width: 150px;
  height: 150px;
  top: 70%;
  right: 15%;
  animation-delay: 2s;
}

.shape-3 {
  width: 100px;
  height: 100px;
  top: 20%;
  right: 25%;
  animation-delay: 4s;
}

.shape-4 {
  width: 120px;
  height: 120px;
  bottom: 20%;
  left: 20%;
  animation-delay: 1s;
}

@keyframes float {
  0%, 100% { transform: translateY(0px) rotate(0deg); }
  33% { transform: translateY(-20px) rotate(120deg); }
  66% { transform: translateY(10px) rotate(240deg); }
}

/* 登录容器 */
.login-container {
  position: relative;
  z-index: 10;
  width: 100%;
  max-width: 920px;
  padding: 24px;
}

/* 登录卡片 */
.login-card {
  background: rgba(255, 255, 255, 0.96);
  backdrop-filter: blur(24px);
  border-radius: 28px;
	box-shadow: none;
  border: 1px solid rgba(255, 255, 255, 0.45);
  overflow: hidden;
  animation: cardAppear 0.8s ease-out;
  transition: all 0.3s ease;
  display: flex;
  align-items: stretch;
}

.login-card:hover {
  transform: translateY(-4px);
	box-shadow: none;
}

@keyframes cardAppear {
  0% {
    opacity: 0;
    transform: translateY(40px) scale(0.95);
  }
  100% {
    opacity: 1;
    transform: translateY(0) scale(1);
  }
}

/* 登录头部 */
.login-header {
	flex: 1.1;
	display: flex;
	flex-direction: column;
	justify-content: center;
	align-items: center;
	gap: 16px;
	padding: 56px 48px 48px;
	text-align: center;
	background:
	  radial-gradient(circle at 0% 0%, rgba(255, 255, 255, 0.7), transparent 55%),
	  linear-gradient(135deg, rgba(110, 231, 183, 0.2) 0%, rgba(249, 168, 212, 0.4) 100%);
	border-right: 1px solid rgba(249, 168, 212, 0.3);
}

.logo-section {
  display: flex;
  align-items: center;
	justify-content: center;
  gap: 12px;
	margin-bottom: 32px;
}

.logo-icon {
  font-size: 48px;
  animation: bounce 2s ease-in-out infinite;
}

@keyframes bounce {
  0%, 20%, 50%, 80%, 100% { transform: translateY(0); }
  40% { transform: translateY(-10px); }
  60% { transform: translateY(-5px); }
}

.brand-title {
	font-size: 34px;
	font-weight: 700;
	background: linear-gradient(135deg, #6ee7b7 0%, #f9a8d4 100%);
	-webkit-background-clip: text;
	-webkit-text-fill-color: transparent;
	background-clip: text;
  margin: 0;
  letter-spacing: -0.02em;
}

.page-title {
  font-size: 32px;
  font-weight: 800;
  color: #1a1a1a;
  margin: 0 0 8px 0;
  letter-spacing: -0.02em;
}

.page-subtitle {
  font-size: 16px;
  color: #6b7280;
  margin: 0;
  line-height: 1.5;
}

/* 登录表单 */
.login-form {
	flex: 1;
	padding: 40px 40px 32px;
	background: linear-gradient(135deg, rgba(209, 250, 229, 0.96) 0%, rgba(240, 253, 250, 0.98) 40%, rgba(240, 249, 255, 0.98) 100%);
}

/* 表单项样式覆盖 */
:deep(.ant-form-item) {
  margin-bottom: 24px;
}

:deep(.ant-input) {
	border-radius: var(--radius-full);
	border: 1px solid #e5e7eb;
	transition: all 0.3s ease;
	padding: 0 20px;
	font-size: 16px;
	height: 48px;
	display: flex;
	align-items: center;
}

:deep(.ant-input:focus),
:deep(.ant-input-focused) {
	border-color: #6ee7b7;
	box-shadow: 0 0 0 4px rgba(110, 231, 183, 0.35);
}

:deep(.ant-input-password) {
	border-radius: var(--radius-full);
}

:deep(.ant-input-affix-wrapper) {
	border-radius: var(--radius-full) !important;
	border: 1px solid #e5e7eb !important;
	transition: all 0.3s ease;
	padding: 0 20px !important;
	height: 48px !important;
	display: flex;
  align-items: center;
  font-size: 16px;
}

:deep(.ant-input-affix-wrapper .ant-input) {
	border: none !important;
	box-shadow: none !important;
	padding: 0 !important;
	height: auto !important;
	background: transparent;
	font-size: 16px !important;
}

:deep(.ant-input-affix-wrapper:focus),
:deep(.ant-input-affix-wrapper-focused) {
	border-color: #6ee7b7 !important;
	box-shadow: 0 0 0 4px rgba(110, 231, 183, 0.35) !important;
}

/* 覆盖错误状态样式 */
:deep(.ant-form-item-has-error .ant-input-affix-wrapper) {
  border-color: #e5e7eb !important;
}

:deep(.ant-form-item-has-error .ant-input-affix-wrapper:focus),
:deep(.ant-form-item-has-error .ant-input-affix-wrapper-focused) {
	border-color: #6ee7b7 !important;
	box-shadow: 0 0 0 4px rgba(110, 231, 183, 0.35) !important;
}

/* 表单选项 */
.form-options {
  display: flex;
  justify-content: flex-end;
  margin-bottom: 8px;
}

.forgot-password {
	color: #ef4444;
  text-decoration: none;
  font-size: 14px;
  font-weight: 500;
  transition: all 0.3s ease;
  position: relative;
}

.forgot-password::after {
  content: '';
  position: absolute;
  width: 0;
  height: 1.5px;
  bottom: -2px;
  left: 0;
	background: linear-gradient(135deg, #fecaca 0%, #ef4444 100%);
  transition: width 0.3s ease;
}

.forgot-password:hover {
	color: #b91c1c;
}

.forgot-password:hover::after {
  width: 100%;
}

/* 提交按钮 */
.submit-item {
  margin-bottom: 0 !important;
  margin-top: 40px !important;
}

.login-btn {
	width: auto !important;
	min-width: 180px;
	height: 44px !important;
	border-radius: 999px !important;
	background: linear-gradient(90deg, #6ee7b7 0%, #f9a8d4 100%) !important;
	border: none !important;
	font-size: 16px !important;
  font-weight: 600;
  display: flex;
  align-items: center;
  gap: 8px;
  transition: all 0.3s ease;
  position: relative;
  overflow: hidden;
  justify-content: center;
  margin: 0 auto;
}

.login-btn::before {
  content: '';
  position: absolute;
  top: 0;
  left: -100%;
  width: 100%;
  height: 100%;
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.2) 0%, transparent 100%);
  transition: left 0.5s ease;
}

.login-btn:hover::before {
  left: 100%;
}

.login-btn:hover {
  transform: translateY(-2px);
	box-shadow: 0 16px 32px rgba(249, 168, 212, 0.45);
}

.login-btn:active {
  transform: translateY(0);
}

.btn-text {
  transition: transform 0.3s ease;
}

.btn-arrow {
  transition: transform 0.3s ease;
  font-size: 20px;
}

.login-btn:hover .btn-text {
  transform: translateX(-4px);
}

.login-btn:hover .btn-arrow {
  transform: translateX(4px);
}

/* 页脚链接 */
.login-footer {
  padding-top: 24px;
  border-top: 1px solid #f3f4f6;
  text-align: center;
}

.register-link {
  font-size: 14px;
  color: #6b7280;
}

.register-link .link {
	color: #6ee7b7;
  text-decoration: none;
  font-weight: 600;
  margin-left: 8px;
  transition: all 0.3s ease;
  position: relative;
}

.register-link .link::after {
  content: '';
  position: absolute;
  width: 0;
  height: 2px;
  bottom: -2px;
  left: 0;
  background: linear-gradient(135deg, #6ee7b7 0%, #f9a8d4 100%);
  transition: width 0.3s ease;
}

.register-link .link:hover::after {
  width: 100%;
}

.register-link .link:hover {
	color: #f9a8d4;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .login-container {
    padding: 16px;
    max-width: 100%;
  }

  .login-card {
    border-radius: 20px;
    flex-direction: column;
  }

  .login-header {
    padding: 28px 20px 20px;
    text-align: center;
    border-right: none;
    border-bottom: 1px solid rgba(249, 168, 212, 0.3);
  }

  .logo-section {
    justify-content: center;
  }

  .brand-title {
    font-size: 24px;
  }

  .page-title {
    font-size: 28px;
  }

  .page-subtitle {
    font-size: 14px;
  }

  .login-form {
    padding: 24px 20px 24px;
  }

  .login-btn {
    height: 52px;
    font-size: 16px;
  }
}

/* 暗色模式适配 */
@media (prefers-color-scheme: dark) {
	.login-card {
		background: rgba(255, 255, 255, 0.96);
		color: #111827;
	}

	.page-title {
		color: #111827;
	}

	.page-subtitle {
		color: #4b5563;
	}


  	.register-link {
		color: #6b7280;
	}
}
</style>
