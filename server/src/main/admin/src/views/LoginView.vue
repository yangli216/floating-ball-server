<template>
  <div class="login-page">
    <div class="login-panel">
      <div class="login-panel__side">
        <div class="brand-tag">floating-ball-server</div>
        <h1 class="login-title">管理员登录</h1>
        <p class="login-subtitle">
          进入区域化管理后台，统一处理概览、用户、角色与治理配置。
        </p>
        <div class="login-tips">
          <div class="login-tip">
            <div class="login-tip__label">登录后可访问</div>
            <div class="login-tip__value">概览统计、用户管理、角色管理</div>
          </div>
          <div class="login-tip">
            <div class="login-tip__label">当前认证方式</div>
            <div class="login-tip__value">Bearer adminToken</div>
          </div>
        </div>
      </div>

      <div class="login-panel__form">
        <el-form ref="formRef" :model="form" :rules="rules" label-position="top" @submit.native.prevent="submitForm">
          <el-form-item label="管理员账号" prop="username">
            <el-input
              v-model.trim="form.username"
              autocomplete="username"
              placeholder="请输入账号"
              @keyup.enter.native="submitForm"
            />
          </el-form-item>
          <el-form-item label="登录密码" prop="password">
            <el-input
              v-model="form.password"
              type="password"
              show-password
              autocomplete="current-password"
              placeholder="请输入密码"
              @keyup.enter.native="submitForm"
            />
          </el-form-item>
          <el-button type="primary" class="submit-button" :loading="submitting" @click="submitForm">
            登录管理端
          </el-button>
          <div class="reset-hint">
            忘记密码时，可在服务启动前设置
            <span class="reset-hint__code">FB_ADMIN_BOOTSTRAP_RESET_ENABLED=true</span>
            和
            <span class="reset-hint__code">FB_ADMIN_BOOTSTRAP_RESET_PASSWORD=新密码</span>
            ，默认重置账号为
            <span class="reset-hint__code">admin</span>
            ，重启服务后生效。
          </div>
        </el-form>
      </div>
    </div>
  </div>
</template>

<script>
import http from '../api/http'
import { setAdminAuth } from '../utils/auth'

function resolveRedirectPath(value) {
  return typeof value === 'string' && value.indexOf('/') === 0 ? value : '/overview'
}

export default {
  data() {
    return {
      submitting: false,
      form: {
        username: '',
        password: ''
      },
      rules: {
        username: [{ required: true, message: '请输入管理员账号', trigger: 'blur' }],
        password: [{ required: true, message: '请输入登录密码', trigger: 'blur' }]
      }
    }
  },
  methods: {
    submitForm() {
      this.$refs.formRef.validate(async valid => {
        if (!valid) {
          return
        }

        this.submitting = true
        try {
          const data = await http.post('/admin/api/auth/login', {
            username: this.form.username,
            password: this.form.password
          })

          if (!data || !data.token) {
            throw new Error('登录响应缺少 token')
          }

          setAdminAuth({
            token: data.token,
            expiresAt: data.expiresAt,
            user: data.user || null
          })

          this.$message.success('登录成功')
          this.$router.replace(resolveRedirectPath(this.$route.query.redirect))
        } catch (error) {
          this.$message.error((error && error.message) || '登录失败')
        } finally {
          this.submitting = false
        }
      })
    }
  }
}
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
  background:
    radial-gradient(circle at top left, rgba(55, 112, 171, 0.28), transparent 38%),
    radial-gradient(circle at bottom right, rgba(17, 63, 103, 0.18), transparent 32%),
    linear-gradient(135deg, #eef5fb 0%, #f7fafc 48%, #edf3f8 100%);
}

.login-panel {
  width: min(980px, 100%);
  display: grid;
  grid-template-columns: minmax(0, 1.15fr) minmax(360px, 420px);
  overflow: hidden;
  border-radius: 24px;
  background: #fff;
  box-shadow: 0 20px 60px rgba(17, 63, 103, 0.14);
}

.login-panel__side {
  padding: 48px;
  background: linear-gradient(180deg, #113f67 0%, #0d2f4b 100%);
  color: #fff;
}

.brand-tag {
  display: inline-flex;
  padding: 6px 12px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.14);
  font-size: 12px;
  letter-spacing: 0.4px;
}

.login-title {
  margin: 20px 0 12px;
  font-size: 34px;
  line-height: 1.15;
}

.login-subtitle {
  margin: 0;
  max-width: 420px;
  line-height: 1.75;
  color: rgba(255, 255, 255, 0.8);
}

.login-tips {
  margin-top: 32px;
  display: grid;
  gap: 16px;
}

.login-tip {
  padding: 18px 20px;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.1);
}

.login-tip__label {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.72);
}

.login-tip__value {
  margin-top: 8px;
  font-size: 16px;
  font-weight: 600;
}

.login-panel__form {
  display: flex;
  align-items: center;
  padding: 48px;
}

.login-panel__form ::v-deep .el-form {
  width: 100%;
}

.submit-button {
  width: 100%;
  margin-top: 12px;
}

.reset-hint {
  margin-top: 16px;
  padding: 14px 16px;
  border-radius: 14px;
  background: #f5f8fb;
  color: #516274;
  font-size: 13px;
  line-height: 1.7;
}

.reset-hint__code {
  display: inline-block;
  margin: 0 4px;
  padding: 2px 6px;
  border-radius: 8px;
  background: #e8eef5;
  color: #16324f;
  font-family: "SFMono-Regular", Consolas, "Liberation Mono", Menlo, monospace;
  font-size: 12px;
}

@media (max-width: 960px) {
  .login-panel {
    grid-template-columns: 1fr;
  }

  .login-panel__side,
  .login-panel__form {
    padding: 32px 24px;
  }
}
</style>
