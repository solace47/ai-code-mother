# AI Code Mother 生产环境部署指南

> 本文档详细说明如何将 AI Code Mother 部署到生产环境

## 📖 目录

1. [项目概述](#项目概述)
2. [部署架构](#部署架构)
3. [部署前准备](#部署前准备)
4. [环境安装](#环境安装)
5. [代码上传与配置](#代码上传与配置)
6. [一键部署](#一键部署)
7. [部署验证](#部署验证)
8. [运维管理](#运维管理)
9. [故障排查](#故障排查)
10. [常见问题](#常见问题)

---

## 📋 项目概述

**AI Code Mother** 是一个基于 Spring Boot 3.5.4 + Vue 3 的 AI 代码生成平台，支持：
- 🤖 AI 对话式代码生成（HTML、多文件项目、Vue应用）
- 📦 应用版本管理与在线部署
- 🎁 积分系统与邀请奖励
- 👥 用户管理与权限控制

### 技术栈
- **后端**: Spring Boot 3.5.4 + MyBatis-Flex + LangChain4j + DeepSeek AI
- **前端**: Vue 3 + TypeScript + Ant Design Vue + Vite
- **数据库**: MySQL 8.0 + Redis 6.0
- **Web 服务器**: OpenResty (Nginx 增强版)

---

## 🏗️ 部署架构

### 服务架构图
```
用户请求 (https://your-domain.com)
    ↓
OpenResty (80/443 端口)
    ├─→ 前端静态文件 (/var/www/ai-code-mother/frontend)
    ├─→ 后端 API 反向代理 (/api → localhost:8123)
    └─→ 部署的子应用 (/dist/{deployKey})
         ↓
Spring Boot 应用 (8123 端口)
    ↓
MySQL + Redis
```

### 目录结构
```
/var/www/ai-code-mother/
├── backend/                    # 后端 JAR 包
│   └── ai-code-mother.jar
├── frontend/                   # 前端静态文件
│   ├── index.html
│   └── assets/
├── templates/                  # Vue 项目模板
│   ├── vue_project_xxx/
│   └── vue_project_yyy/
└── deployments/                # 用户部署的应用
    └── {deployKey}/
```

---

## 🎯 部署前准备

### 1. 服务器要求

| 配置项 | 最低要求 | 推荐配置 |
|--------|---------|---------|
| **操作系统** | Ubuntu 20.04+ | Ubuntu 22.04 LTS |
| **CPU** | 2核心 | 4核心+ |
| **内存** | 4GB | 8GB+ |
| **磁盘** | 20GB | 50GB+ SSD |
| **带宽** | 5Mbps | 10Mbps+ |

### 2. 域名与 DNS 配置

**准备域名**: 例如 `your-domain.com`

DNS 记录配置：
```bash
# A 记录
your-domain.com.        IN  A     <your-server-ip>
www.your-domain.com.    IN  A     <your-server-ip>
```

验证 DNS 解析：
```bash
# 检查域名解析
nslookup your-domain.com
ping your-domain.com
```

### 3. SSL 证书准备

**推荐方式：使用 Let's Encrypt 免费证书**

```bash
# 稍后在安装 OpenResty 后执行
sudo certbot --nginx -d your-domain.com -d www.your-domain.com
```

**或使用已有证书**：
- 证书文件：`/etc/ssl/certs/your-domain.crt`
- 私钥文件：`/etc/ssl/private/your-domain.key`

### 4. 网络端口开放

需要开放的端口：
```bash
# Ubuntu UFW 防火墙
sudo ufw allow 22/tcp    # SSH
sudo ufw allow 80/tcp    # HTTP
sudo ufw allow 443/tcp   # HTTPS
sudo ufw enable
sudo ufw status
```

---

## 🔧 环境安装

### 步骤 1：更新系统

```bash
# 更新软件包列表
sudo apt update && sudo apt upgrade -y

# 安装基础工具
sudo apt install -y wget curl git vim net-tools
```

### 步骤 2：安装 Java 21

```bash
# 安装 OpenJDK 21
sudo apt install -y openjdk-21-jdk

# 验证安装
java -version
# 输出：openjdk version "21.x.x"
```

### 步骤 3：安装 Node.js 18+

```bash
# 添加 NodeSource 仓库
curl -fsSL https://deb.nodesource.com/setup_18.x | sudo -E bash -

# 安装 Node.js
sudo apt install -y nodejs

# 验证安装
node -v    # 输出：v18.x.x
npm -v     # 输出：9.x.x
```

### 步骤 4：安装 MySQL 8.0

```bash
# 安装 MySQL
sudo apt install -y mysql-server

# 安全配置
sudo mysql_secure_installation

# 启动服务
sudo systemctl start mysql
sudo systemctl enable mysql

# 创建数据库和用户
mysql -u root -p
```

```sql
-- 创建数据库
CREATE DATABASE ai_code_mother DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 创建用户（替换为你的密码）
CREATE USER 'aicode'@'localhost' IDENTIFIED BY 'your_secure_password';
GRANT ALL PRIVILEGES ON ai_code_mother.* TO 'aicode'@'localhost';
FLUSH PRIVILEGES;
```

### 步骤 5：安装 Redis

```bash
# 安装 Redis
sudo apt install -y redis-server

# 配置 Redis（设置密码）
sudo vim /etc/redis/redis.conf
# 取消注释并设置：requirepass your_redis_password

# 重启服务
sudo systemctl restart redis
sudo systemctl enable redis

# 测试连接
redis-cli -a your_redis_password ping  # 输出：PONG
```

### 步骤 6：安装 OpenResty

```bash
# 添加 OpenResty 仓库
wget -qO - https://openresty.org/package/pubkey.gpg | sudo apt-key add -
sudo apt install -y software-properties-common
sudo add-apt-repository -y "deb http://openresty.org/package/ubuntu $(lsb_release -sc) main"

# 更新并安装
sudo apt update
sudo apt install -y openresty

# 验证安装
openresty -v

# 启动服务
sudo systemctl start openresty
sudo systemctl enable openresty
```

### 步骤 7：安装 Certbot（SSL 证书）

```bash
# 安装 Certbot
sudo apt install -y certbot

# 验证安装
certbot --version
```

---

## 📂 代码上传与配置

### 方式一：Git 克隆（推荐）

```bash
# 进入部署目录
cd /opt

# 克隆代码
git clone https://github.com/your-username/ai-code-mother.git
cd ai-code-mother
```

### 配置文件修改

#### 1. 后端配置

文件：`src/main/resources/application-prod.yml`

```yaml
spring:
  # 数据库配置
  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://localhost:3306/ai_code_mother
    username: aicode
    password: your_database_password  # 修改为你的数据库密码

  # Redis 配置
  data:
    redis:
      host: localhost
      port: 6379
      database: 0
      password: your_redis_password    # 修改为你的 Redis 密码

  # 邮件配置（可选）
  mail:
    host: smtp.your-email-provider.com
    port: 587
    username: your-email@example.com
    password: your_email_password

# AI 配置
langchain4j:
  open-ai:
    chat-model:
      base-url: https://api.deepseek.com
      api-key: sk-your-deepseek-api-key  # 替换为你的 DeepSeek API Key
      model-name: deepseek-chat

# COS 对象存储配置（如使用腾讯云 COS）
cos:
  client:
    host: https://your-bucket.cos.region.myqcloud.com
    secretId: your-secret-id
    secretKey: your-secret-key
    region: your-region
    bucket: your-bucket-name

# 接口文档配置
knife4j:
  basic:
    enable: true
    username: admin              # 修改为你的用户名
    password: your-doc-password  # 修改为你的密码

# 部署路径
code:
  deploy-host: https://your-domain.com/dist
```

#### 2. 前端配置

文件：`ai-code-mother-frontend/.env.production`

```env
# 部署域名配置
VITE_DEPLOY_DOMAIN=https://your-domain.com/dist
VITE_APP_DOMAIN=https://your-domain.com

# API 基础路径
VITE_API_BASE_URL=/api
```

#### 3. 修改部署脚本配置

编辑 `deploy/deploy.sh`：

```bash
# 数据库配置
DB_HOST="localhost"
DB_PORT="3306"
DB_NAME="ai_code_mother"
DB_USER="aicode"
DB_PASS="your_database_password"  # 修改为你的数据库密码

# 应用配置
APP_PORT="8123"
DOMAIN="your-domain.com"          # 修改为你的域名
```

编辑 `deploy/backup.sh`：

```bash
# 数据库配置
DB_HOST="localhost"
DB_USER="aicode"
DB_PASS="your_database_password"  # 修改为你的数据库密码
```

#### 4. 修改 OpenResty 配置

编辑 `deploy/ai-code-mother.conf`：

```nginx
# 修改所有的 server_name
server_name your-domain.com www.your-domain.com;

# 修改 SSL 证书路径
ssl_certificate /etc/ssl/certs/your-domain.crt;
ssl_certificate_key /etc/ssl/private/your-domain.key;
```

---

## 🚀 一键部署

### 执行部署脚本

```bash
# 1. 进入项目目录
cd /opt/ai-code-mother

# 2. 赋予执行权限
chmod +x deploy/*.sh

# 3. 执行一键部署
sudo ./deploy/deploy.sh
```

### 部署脚本执行流程

脚本会自动完成以下步骤：

1. **环境检查** - 检查 Java、Node.js、MySQL、Redis 连接
2. **创建目录** - 创建必要的部署目录
3. **初始化数据库** - 创建 9 个数据表
4. **构建后端** - Maven 打包生成 JAR
5. **构建前端** - npm 构建生成静态文件
6. **复制模板** - 复制 Vue 项目模板
7. **配置 OpenResty** - 配置反向代理
8. **配置 systemd** - 配置自动启动
9. **启动服务** - 启动后端和 OpenResty
10. **健康检查** - 验证部署是否成功

---

## ✅ 部署验证

### 1. 配置 SSL 证书

```bash
# 获取 Let's Encrypt 证书
sudo certbot --nginx -d your-domain.com -d www.your-domain.com

# 验证证书
curl -I https://your-domain.com
```

### 2. 检查服务状态

```bash
# 检查后端服务
sudo systemctl status ai-code-mother

# 检查 OpenResty
sudo systemctl status openresty
```

### 3. API 健康检查

```bash
# 后端 API 检查
curl http://localhost:8123/api/health/

# 通过域名访问
curl https://your-domain.com/api/health/
```

### 4. 功能验证

| 功能 | URL | 验证内容 |
|------|-----|---------|
| 首页 | https://your-domain.com | 页面正常显示 |
| 登录 | https://your-domain.com/user/login | 登录页面正常 |
| 注册 | https://your-domain.com/user/register | 注册页面正常 |
| API 文档 | https://your-domain.com/api/doc.html | Knife4j 文档页面 |

---

## 🔄 运维管理

### 服务管理

```bash
# 后端服务
sudo systemctl start/stop/restart ai-code-mother
sudo journalctl -u ai-code-mother -f

# OpenResty 服务
sudo systemctl reload openresty
tail -f /var/log/openresty/ai-code-mother-access.log
```

### 应用更新

```bash
# 1. 备份
sudo /opt/ai-code-mother/deploy/backup.sh

# 2. 更新代码
git pull origin main

# 3. 重新部署
sudo ./deploy/deploy.sh
```

### 定时备份

```bash
# 编辑 crontab
sudo crontab -e

# 添加每天凌晨2点备份
0 2 * * * /opt/ai-code-mother/deploy/backup.sh
```

---

## 🔍 故障排查

### 问题 1：后端服务无法启动

```bash
# 查看日志
sudo journalctl -u ai-code-mother -n 100

# 检查端口
sudo netstat -tlnp | grep 8123

# 检查配置
grep -A 5 "datasource:" src/main/resources/application-prod.yml
```

### 问题 2：前端页面 404

```bash
# 检查 OpenResty
sudo openresty -t
tail -f /var/log/openresty/ai-code-mother-error.log

# 检查文件权限
ls -la /var/www/ai-code-mother/frontend/
sudo chown -R www-data:www-data /var/www/ai-code-mother/
```

### 问题 3：数据库连接失败

```bash
# 测试连接
mysql -u aicode -p ai_code_mother

# 检查权限
SHOW GRANTS FOR 'aicode'@'localhost';
```

---

## ❓ 常见问题

### Q1: 如何修改管理员密码？
```sql
-- 密码需要 MD5 加密，盐值为 solace47
UPDATE user SET userPassword = MD5(CONCAT('NewPassword', 'solace47'))
WHERE userAccount = 'admin';
```

### Q2: 如何增加 JVM 内存？
```bash
# 编辑服务文件
sudo vim /etc/systemd/system/ai-code-mother.service

# 修改 JAVA_OPTS
Environment="JAVA_OPTS=-Xms1g -Xmx4g -XX:+UseG1GC"

# 重启
sudo systemctl daemon-reload
sudo systemctl restart ai-code-mother
```

### Q3: 如何更换 AI 模型？
```yaml
# 编辑 application-prod.yml
langchain4j:
  open-ai:
    chat-model:
      model-name: deepseek-chat  # 修改为其他模型
```

---

## 📚 附录

### 默认账户

| 类型 | 账号 | 默认密码 |
|------|------|---------|
| 管理员 | admin | solace47 |

**⚠️ 首次登录后请立即修改密码！**

### 数据库表清单

1. user - 用户表
2. app - 应用表
3. chat_history - 对话历史表
4. app_version - 应用版本表
5. user_points - 用户积分表
6. points_record - 积分明细表
7. sign_in_record - 签到记录表
8. invite_record - 邀请关系表
9. email_verification_code - 邮箱验证码表

### 重要路径

| 用途 | 路径 |
|------|------|
| 项目代码 | `/opt/ai-code-mother` |
| 后端部署 | `/var/www/ai-code-mother/backend` |
| 前端部署 | `/var/www/ai-code-mother/frontend` |
| 备份目录 | `/var/backups/ai-code-mother` |

---

## 🔒 安全建议

1. **修改默认密码** - 所有默认密码必须修改
2. **配置防火墙** - 只开放必要端口
3. **启用 HTTPS** - 强制使用 SSL/TLS
4. **定期备份** - 设置自动备份任务
5. **日志监控** - 定期检查系统日志
6. **API 限流** - 防止接口滥用
7. **数据加密** - 敏感数据加密存储

---

## 📞 技术支持

- **项目地址**: https://github.com/solace47/ai-code-mother
- **问题反馈**: [GitHub Issues](https://github.com/solace47/ai-code-mother/issues)

---

## ⚠️ 重要提示

本文档为公开版本，所有敏感信息已使用占位符替换。实际部署时请：

1. 替换所有 `your-*` 占位符为实际值
2. 设置强密码（建议使用密码生成器）
3. 妥善保管所有密钥和密码
4. 不要将包含敏感信息的配置文件提交到公开仓库

建议使用环境变量或密钥管理系统来管理敏感配置。

---

**部署完成后，访问你的域名开始使用！** 🎉
