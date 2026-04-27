<template>
  <div class="login-page">
    <div class="login-panel">
      <div class="login-panel__side">
        <div class="brand-mark">区域智能</div>
        <h1 class="login-brand">floating-ball-server</h1>
        <p class="login-welcome">欢迎回来</p>
      </div>

      <div class="login-panel__form">
        <el-form ref="formRef" :model="form" :rules="rules" label-position="top" @submit.native.prevent="submitForm">
          <h2 class="form-title">登录</h2>
          <el-form-item label="账号" prop="username">
            <el-input
              v-model.trim="form.username"
              autocomplete="username"
              placeholder="请输入账号"
              @keyup.enter.native="submitForm"
            />
          </el-form-item>
          <el-form-item label="密码" prop="password">
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
            登录
          </el-button>
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
        username: [{ required: true, message: '请输入账号', trigger: 'blur' }],
        password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
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
  background: linear-gradient(135deg, #E1F5EE 0%, #F4F7F6 54%, #FFFFFF 100%);
}

.login-panel {
  width: min(920px, 100%);
  min-height: 520px;
  display: grid;
  grid-template-columns: 1fr 1fr;
  overflow: hidden;
  border-radius: 20px;
  background: #fff;
  border: 0.5px solid #E8EEEC;
}

.login-panel__side {
  display: flex;
  flex-direction: column;
  justify-content: center;
  padding: 48px;
  background: linear-gradient(160deg, #0F6E56 0%, #1D9E75 100%);
  color: #fff;
}

.brand-mark {
  width: 54px;
  height: 54px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.14);
  font-size: 13px;
  font-weight: 500;
}

.login-brand {
  margin: 20px 0 8px;
  font-size: 26px;
  line-height: 1.2;
  font-weight: 500;
  letter-spacing: -0.3px;
}

.login-welcome {
  margin: 0;
  color: rgba(255, 255, 255, 0.82);
  font-size: 14px;
}

.login-panel__form {
  display: flex;
  align-items: center;
  padding: 48px;
}

.login-panel__form ::v-deep .el-form {
  width: 100%;
}

.login-panel__form ::v-deep .el-input__inner {
  height: 42px;
  line-height: 42px;
  padding: 0 14px;
}

.form-title {
  margin: 0 0 28px;
  font-size: 22px;
  font-weight: 500;
  letter-spacing: -0.3px;
  color: #2C2C2A;
}

.submit-button {
  width: 100%;
  height: 42px;
  margin-top: 8px;
}

@media (max-width: 820px) {
  .login-panel {
    grid-template-columns: 1fr;
  }

  .login-panel__side,
  .login-panel__form {
    padding: 32px 24px;
  }
}
</style>
