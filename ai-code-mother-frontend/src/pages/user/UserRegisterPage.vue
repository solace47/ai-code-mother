<template>
  <div id="userRegisterPage">
    <div class="register-background">
      <div class="bg-shapes">
        <div class="shape shape-1"></div>
        <div class="shape shape-2"></div>
        <div class="shape shape-3"></div>
        <div class="shape shape-4"></div>
        <div class="shape shape-5"></div>
      </div>
    </div>

    <div class="register-container">
      <div class="register-card">
        <div class="register-header">
          <div class="logo-section">
            <h1 class="brand-title">AI 应用生成</h1>
          </div>
          <h2 class="page-title">创建账户</h2>
          <p class="page-subtitle">加入我们，开启AI应用创造之旅</p>
        </div>

        <div class="register-form">
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
              name="emailCode"
              :rules="[{ required: true, message: '请输入验证码' }]"
            >
              <div style="display: flex; gap: 12px;">
                <a-input
                  v-model:value="formState.emailCode"
                  placeholder="验证码"
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

            <a-form-item
              name="checkPassword"
              :rules="[
                { required: true, message: '请确认您的密码' },
                { min: 8, message: '密码长度不能小于 8 位' },
                { validator: validateCheckPassword },
              ]"
            >
              <a-input-password
                v-model:value="formState.checkPassword"
                placeholder="确认密码"
                size="large"
              />
            </a-form-item>

            <a-form-item name="inviteCode">
              <a-input
                v-model:value="formState.inviteCode"
                placeholder="邀请码（可选）"
                size="large"
              >
                <template #prefix>
                  <span style="color: #fb923c;">🎁</span>
                </template>
              </a-input>
            </a-form-item>

            <a-form-item class="submit-item">
              <a-button type="primary" html-type="submit" size="large" class="register-btn">
                <span class="btn-text">立即注册</span>
                <span class="btn-sparkle">✨</span>
              </a-button>
            </a-form-item>
          </a-form>

          <div class="register-footer">
            <div class="login-link">
              <span>已有账号？</span>
              <RouterLink to="/user/login" class="link">立即登录</RouterLink>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useRoute, useRouter } from 'vue-router'
import { userRegister } from '@/api/userController.ts'
import { message } from 'ant-design-vue'
import { reactive, ref, computed } from 'vue'
import request from '@/request'

const router = useRouter()
const route = useRoute()

const formState = reactive<API.UserRegisterRequest>({
  userEmail: '',
  emailCode: '',
  userPassword: '',
  checkPassword: '',
  inviteCode: '',
})

const queryInviteCode = route.query.inviteCode
if (typeof queryInviteCode === 'string') {
  formState.inviteCode = queryInviteCode
}

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
      type: 'REGISTER'
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
 * @param rule
 * @param value
 * @param callback
 */
const validateCheckPassword = (rule: unknown, value: string, callback: (error?: Error) => void) => {
  if (value && value !== formState.userPassword) {
    callback(new Error('两次输入密码不一致'))
  } else {
    callback()
  }
}

/**
 * 提交表单
 * @param values
 */
const handleSubmit = async (values: API.UserRegisterRequest) => {
  const payload: API.UserRegisterRequest = {
    ...values,
    inviteCode: formState.inviteCode || undefined,
  }
  const res = await userRegister(payload)
  // 注册成功，跳转到登录页面
  if (res.data.code === 0) {
    message.success('注册成功')
    router.push({
      path: '/user/login',
      replace: true,
    })
  } else {
    message.error('注册失败，' + res.data.message)
  }
}
</script>

<style scoped>
#userRegisterPage {
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
.register-background {
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
  background: rgba(255, 255, 255, 0.15);
  border-radius: 50%;
  backdrop-filter: blur(10px);
  animation: float 8s ease-in-out infinite;
}

.shape-1 {
  width: 180px;
  height: 180px;
  top: 5%;
  left: 5%;
  animation-delay: 0s;
}

.shape-2 {
  width: 120px;
  height: 120px;
  top: 60%;
  right: 10%;
  animation-delay: 1.5s;
}

.shape-3 {
  width: 80px;
  height: 80px;
  top: 15%;
  right: 30%;
  animation-delay: 3s;
}

.shape-4 {
  width: 140px;
  height: 140px;
  bottom: 15%;
  left: 15%;
  animation-delay: 0.5s;
}

.shape-5 {
  width: 60px;
  height: 60px;
  top: 35%;
  left: 70%;
  animation-delay: 2.5s;
}

@keyframes float {
  0%, 100% { transform: translateY(0px) rotate(0deg) scale(1); }
  25% { transform: translateY(-15px) rotate(90deg) scale(1.1); }
  50% { transform: translateY(5px) rotate(180deg) scale(0.9); }
  75% { transform: translateY(-8px) rotate(270deg) scale(1.05); }
}

/* 注册容器 */
.register-container {
  position: relative;
  z-index: 10;
  width: 100%;
  max-width: 920px;
  padding: 24px;
}

/* 注册卡片 */
.register-card {
  background: rgba(255, 255, 255, 0.96);
  backdrop-filter: blur(24px);
  border-radius: 28px;
  box-shadow: none;
  border: 1px solid rgba(255, 255, 255, 0.45);
  overflow: hidden;
  animation: cardAppear 1s ease-out;
  transition: all 0.3s ease;
  display: flex;
  align-items: stretch;
}

.register-card:hover {
  transform: translateY(-4px);
  box-shadow: none;
}

@keyframes cardAppear {
  0% {
    opacity: 0;
    transform: translateY(50px) scale(0.9);
  }
  100% {
    opacity: 1;
    transform: translateY(0) scale(1);
  }
}

/* 注册头部 - 与登录/重置页左侧一致 */
.register-header {
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
  font-size: 52px;
  animation: sparkle 3s ease-in-out infinite;
  filter: drop-shadow(0 4px 8px rgba(240, 147, 251, 0.3));
}

@keyframes sparkle {
  0%, 100% {
    transform: rotate(0deg) scale(1);
    filter: drop-shadow(0 4px 8px rgba(240, 147, 251, 0.3));
  }
  25% {
    transform: rotate(-5deg) scale(1.1);
    filter: drop-shadow(0 6px 12px rgba(240, 147, 251, 0.4));
  }
  50% {
    transform: rotate(5deg) scale(1.05);
    filter: drop-shadow(0 8px 16px rgba(240, 147, 251, 0.5));
  }
  75% {
    transform: rotate(-3deg) scale(1.08);
    filter: drop-shadow(0 6px 12px rgba(240, 147, 251, 0.4));
  }
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

/* 注册表单 - 右侧区域 */
.register-form {
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
  margin-top: 32px !important;
}

.register-btn {
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

.register-btn::before {
  content: '';
  position: absolute;
  top: 0;
  left: -100%;
  width: 100%;
  height: 100%;
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.2) 0%, transparent 100%);
  transition: left 0.6s ease;
}

.register-btn:hover::before {
  left: 100%;
}

.register-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 16px 32px rgba(249, 168, 212, 0.45);
}

.register-btn:active {
  transform: translateY(-1px);
}

.btn-text {
  transition: transform 0.3s ease;
}

.btn-sparkle {
  transition: transform 0.3s ease;
  font-size: 22px;
  animation: sparkle 2s ease-in-out infinite;
}

.register-btn:hover .btn-text {
  transform: translateX(-6px);
}

.register-btn:hover .btn-sparkle {
  transform: translateX(6px) rotate(180deg);
}

/* 页脚链接 */
.register-footer {
  padding-top: 28px;
  border-top: 1px solid #f3f4f6;
  text-align: center;
}

.login-link {
  font-size: 15px;
  color: #6b7280;
}

.login-link .link {
  color: #6ee7b7;
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
  background: linear-gradient(135deg, #6ee7b7 0%, #f9a8d4 100%);
  transition: width 0.3s ease;
}

.login-link .link:hover::after {
  width: 100%;
}

.login-link .link:hover {
  color: #f9a8d4;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .register-container {
    padding: 16px;
    max-width: 100%;
  }

  .register-card {
    border-radius: 24px;
    flex-direction: column;
  }

  .register-header {
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

  .register-form {
    padding: 24px 20px 24px;
  }

  .register-btn {
    height: 46px;
    font-size: 15px;
  }

  .logo-icon {
    font-size: 48px;
  }
}

/* 暗色模式适配 */
@media (prefers-color-scheme: dark) {
  .register-card {
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

  .register-header {
    border-bottom: 1px solid rgba(249, 168, 212, 0.3);
  }

  .register-footer {
    border-top: 1px solid #e5e7eb;
  }
}
</style>
