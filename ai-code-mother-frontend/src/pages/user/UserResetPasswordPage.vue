<template>
  <div id="userResetPasswordPage">
    <div class="reset-background">
      <div class="bg-shapes">
        <div class="shape shape-1"></div>
        <div class="shape shape-2"></div>
        <div class="shape shape-3"></div>
        <div class="shape shape-4"></div>
      </div>
    </div>

    <div class="reset-container">
      <div class="reset-card">
        <div class="reset-header">
          <div class="logo-section">
            <h1 class="brand-title">AI 应用生成</h1>
          </div>
          <h2 class="page-title">重置密码</h2>
          <p class="page-subtitle">通过邮箱验证码重置您的账户密码</p>
        </div>

        <div class="reset-form">
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
                placeholder="注册邮箱"
                size="large"
              />
            </a-form-item>

            <a-form-item
              name="emailCode"
              :rules="[{ required: true, message: '请输入验证码' }]"
            >
              <div style="display: flex; gap: 12px;">
                <a-input
                  v-model:value="formState.emailCode"
                  placeholder="邮箱验证码"
                  size="large"
                  style="flex: 1;"
                />
                <a-button
                  size="large"
                  :disabled="sendCodeDisabled"
                  :loading="isSendingCode"
                  @click="sendEmailCode"
                  class="send-code-btn"
                >
                  {{ sendCodeText }}
                </a-button>
              </div>
            </a-form-item>

            <a-form-item
              name="newPassword"
              :rules="[
                { required: true, message: '请输入新密码' },
                { min: 8, max: 20, message: '密码长度必须在8-20位之间' },
              ]"
            >
              <a-input-password
                v-model:value="formState.newPassword"
                placeholder="新密码（8-20位）"
                size="large"
              />
            </a-form-item>

            <a-form-item
              name="checkPassword"
              :rules="[
                { required: true, message: '请确认您的新密码' },
                { min: 8, max: 20, message: '密码长度必须在8-20位之间' },
                { validator: validateCheckPassword },
              ]"
            >
              <a-input-password
                v-model:value="formState.checkPassword"
                placeholder="确认新密码"
                size="large"
              />
            </a-form-item>

            <a-form-item class="submit-item">
              <a-button type="primary" html-type="submit" size="large" class="reset-btn">
                <span class="btn-text">重置密码</span>
                <span class="btn-icon">🔑</span>
              </a-button>
            </a-form-item>
          </a-form>

          <div class="reset-footer">
            <div class="login-link">
              <span>想起密码了？</span>
              <RouterLink to="/user/login" class="link">返回登录</RouterLink>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { reactive, ref, computed } from 'vue'
import request from '@/request'

const router = useRouter()

const formState = reactive({
  userEmail: '',
  emailCode: '',
  newPassword: '',
  checkPassword: '',
})

// 验证码发送相关状态
const sendCodeDisabled = ref(false)
const isSendingCode = ref(false)
const countdown = ref(0)

const sendCodeText = computed(() => {
  return countdown.value > 0 ? `${countdown.value}秒后重发` : '发送验证码'
})

/**
 * 发送邮箱验证码
 */
const sendEmailCode = async () => {
  // 校验邮箱
  if (!formState.userEmail) {
    message.error('请先输入邮箱')
    return
  }

  const emailRegex = /^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$/
  if (!emailRegex.test(formState.userEmail)) {
    message.error('请输入正确的邮箱格式')
    return
  }

  try {
    sendCodeDisabled.value = true
    isSendingCode.value = true
    const res = await request.post('/user/email/send', {
      email: formState.userEmail,
      type: 'RESET_PASSWORD'
    })

    if (res.data.code === 0) {
      message.success('验证码已发送，请查收邮件')
      // 开始倒计时60秒
      countdown.value = 60
      const timer = setInterval(() => {
        countdown.value--
        if (countdown.value <= 0) {
          clearInterval(timer)
          sendCodeDisabled.value = false
        }
      }, 1000)
    } else {
      message.error('发送失败：' + res.data.message)
      sendCodeDisabled.value = false
    }
  } catch (error: any) {
    message.error('发送失败：' + (error.message || '网络错误'))
    sendCodeDisabled.value = false
  } finally {
    isSendingCode.value = false
  }
}

/**
 * 验证确认密码
 */
const validateCheckPassword = (rule: unknown, value: string, callback: (error?: Error) => void) => {
  if (value && value !== formState.newPassword) {
    callback(new Error('两次输入密码不一致'))
  } else {
    callback()
  }
}

/**
 * 提交表单
 */
const handleSubmit = async () => {
  try {
    const res = await request.post('/user/reset-password', formState)

    if (res.data.code === 0) {
      message.success('密码重置成功，请使用新密码登录')
      router.push({
        path: '/user/login',
        replace: true,
      })
    } else {
      message.error('重置失败：' + res.data.message)
    }
  } catch (error: any) {
    message.error('重置失败：' + (error.message || '网络错误'))
  }
}
</script>

<style scoped>
#userResetPasswordPage {
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
.reset-background {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: transparent;
}

/* 浮动装饰元素 - 登录页已取消气泡，这里也关闭 */
.bg-shapes {
  display: none;
}

.shape {
  position: absolute;
  background: rgba(255, 255, 255, 0.12);
  border-radius: 50%;
  backdrop-filter: blur(10px);
  animation: float 7s ease-in-out infinite;
}

.shape-1 {
  width: 160px;
  height: 160px;
  top: 8%;
  left: 8%;
  animation-delay: 0s;
}

.shape-2 {
  width: 110px;
  height: 110px;
  top: 65%;
  right: 12%;
  animation-delay: 1.8s;
}

.shape-3 {
  width: 90px;
  height: 90px;
  top: 18%;
  right: 28%;
  animation-delay: 3.2s;
}

.shape-4 {
  width: 130px;
  height: 130px;
  bottom: 18%;
  left: 18%;
  animation-delay: 0.8s;
}

@keyframes float {
  0%, 100% { transform: translateY(0px) rotate(0deg) scale(1); }
  25% { transform: translateY(-18px) rotate(90deg) scale(1.08); }
  50% { transform: translateY(8px) rotate(180deg) scale(0.92); }
  75% { transform: translateY(-10px) rotate(270deg) scale(1.04); }
}

/* 重置容器 */
.reset-container {
  position: relative;
  z-index: 10;
  width: 100%;
  max-width: 920px;
  padding: 24px;
}

/* 重置卡片 - 与登录页统一为左右双栏玻璃卡片 */
.reset-card {
  background: rgba(255, 255, 255, 0.96);
  backdrop-filter: blur(24px);
  border-radius: 28px;
  box-shadow: none;
  border: 1px solid rgba(255, 255, 255, 0.45);
  overflow: hidden;
  animation: cardAppear 0.9s ease-out;
  transition: all 0.3s ease;
  display: flex;
  align-items: stretch;
}

.reset-card:hover {
  transform: translateY(-4px);
  box-shadow: none;
}

@keyframes cardAppear {
  0% {
    opacity: 0;
    transform: translateY(45px) scale(0.92);
  }
  100% {
    opacity: 1;
    transform: translateY(0) scale(1);
  }
}

/* 重置头部 - 复用登录页左侧风格 */
.reset-header {
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

.brand-title {
  font-size: 32px;
  font-weight: 700;
  background: linear-gradient(135deg, #6ee7b7 0%, #f9a8d4 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  margin: 0;
  letter-spacing: -0.02em;
}

.page-title {
  font-size: 30px;
  font-weight: 800;
  color: #1f2933;
  margin: 0 0 8px 0;
  letter-spacing: -0.02em;
}

.page-subtitle {
  font-size: 16px;
  color: #6b7280;
  margin: 0;
  line-height: 1.5;
  font-weight: 400;
}

/* 重置表单 - 右侧区域，偏绿色背景 */
.reset-form {
  flex: 1;
  padding: 40px 40px 32px;
  background: linear-gradient(135deg,
    rgba(209, 250, 229, 0.96) 0%,
    rgba(240, 253, 250, 0.98) 40%,
    rgba(240, 249, 255, 0.98) 100%);
}

/* 表单项样式覆盖 */
:deep(.ant-form-item) {
  margin-bottom: 22px;
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

:deep(.ant-input-affix-wrapper .ant-input:focus),
:deep(.ant-input-affix-wrapper .ant-input-focused) {
  border: none !important;
  box-shadow: none !important;
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

.send-code-btn {
  width: 120px;
  height: 48px !important;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 999px !important;
  border: 1px solid rgba(110, 231, 183, 0.6) !important;
  background: linear-gradient(120deg, #ecfeff 0%, #d1fae5 40%, #fef3f7 100%);
  color: #047857 !important;
  font-weight: 600;
  letter-spacing: 0.5px;
  padding: 0 20px !important;
  transition: all 0.3s ease;
  box-shadow: 0 8px 20px rgba(110, 231, 183, 0.25);
  backdrop-filter: blur(6px);
  box-sizing: border-box;
  line-height: 48px !important;
}

.send-code-btn:hover,
.send-code-btn:focus {
	border-color: #ef4444 !important;
	box-shadow: 0 12px 28px rgba(110, 231, 183, 0.4);
}

.send-code-btn:active {
  transform: translateY(1px);
  border-color: #ef4444 !important;
  box-shadow: 0 6px 16px rgba(110, 231, 183, 0.3);
}

.send-code-btn[disabled] {
  border-color: rgba(148, 163, 184, 0.4);
  background: rgba(241, 245, 255, 0.9);
  color: rgba(148, 163, 184, 0.9);
  box-shadow: none;
  cursor: not-allowed;
}

/* 提交按钮 */
.submit-item {
  margin-bottom: 0 !important;
  margin-top: 30px !important;
}

.reset-btn {
  width: auto !important;
  min-width: 200px;
  height: 48px !important;
  border-radius: 999px !important;
  background: linear-gradient(90deg, #6ee7b7 0%, #f9a8d4 100%) !important;
  border: none !important;
  font-size: 16px !important;
  font-weight: 600;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  transition: all 0.3s ease;
  position: relative;
  overflow: hidden;
  margin: 0 auto;
}

.reset-btn::before {
  content: '';
  position: absolute;
  top: 0;
  left: -100%;
  width: 100%;
  height: 100%;
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.2) 0%, transparent 100%);
  transition: left 0.55s ease;
}

.reset-btn:hover::before {
  left: 100%;
}

.reset-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 16px 32px rgba(249, 168, 212, 0.45);
}

.reset-btn:active {
  transform: translateY(-1px);
}

.btn-text {
  transition: transform 0.3s ease;
}

.btn-icon {
  transition: transform 0.3s ease;
  font-size: 21px;
}

.reset-btn:hover .btn-text {
  transform: translateX(-5px);
}

.reset-btn:hover .btn-icon {
  transform: translateX(5px) rotate(15deg);
}

/* 页脚链接 */
.reset-footer {
  padding-top: 26px;
  border-top: 1px solid #f3f4f6;
  text-align: center;
}

.login-link {
  font-size: 14.5px;
  color: #6b7280;
}

.login-link .link {
  color: #fb923c;
  text-decoration: none;
  font-weight: 600;
  margin-left: 8px;
  transition: all 0.3s ease;
  position: relative;
}

.login-link .link::after {
  content: '';
  position: absolute;
  width: 0;
  height: 2px;
  bottom: -2px;
  left: 0;
  background: linear-gradient(135deg, #fb923c 0%, #ef4444 100%);
  transition: width 0.3s ease;
}

.login-link .link:hover::after {
  width: 100%;
}

.login-link .link:hover {
  color: #ef4444;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .reset-container {
    padding: 16px;
    max-width: 100%;
  }

  .reset-card {
    border-radius: 22px;
    flex-direction: column;
  }

  .reset-header {
    padding: 32px 20px 24px;
    text-align: center;
    border-right: none;
    border-bottom: 1px solid rgba(249, 168, 212, 0.3);
  }

  .brand-title {
    font-size: 26px;
  }

  .page-title {
    font-size: 28px;
  }

  .page-subtitle {
    font-size: 14px;
  }

  .reset-form {
    padding: 24px 20px 24px;
  }

  .reset-btn {
    height: 46px;
    font-size: 15px;
  }
}

/* 暗色模式适配 */
@media (prefers-color-scheme: dark) {
  .reset-card {
    background: rgba(255, 255, 255, 0.96);
    color: #111827;
  }

  .page-title {
    color: #111827;
  }

  .page-subtitle {
    color: #4b5563;
  }

  .login-link {
    color: #6b7280;
  }

  .reset-header {
    border-bottom: 1px solid rgba(249, 168, 212, 0.3);
  }

  .reset-footer {
    border-top: 1px solid #e5e7eb;
  }
}
</style>
