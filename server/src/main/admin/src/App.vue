<template>
  <div class="app-root">
    <router-view v-if="isLoginPage" />
    <el-container v-else class="app-shell">
      <el-aside width="220px" class="sidebar">
        <div class="brand">floating-ball-server</div>
        <el-menu :default-active="$route.path" router class="menu">
          <el-menu-item v-for="item in menuItems" :key="item.path" :index="item.path">
            {{ item.label }}
          </el-menu-item>
        </el-menu>
      </el-aside>
      <el-container>
        <el-header class="header">
          <div class="title">{{ $route.meta.title || '管理端' }}</div>
          <div class="header-actions">
            <div class="user-panel">
              <div class="user-panel__name">{{ currentUserName }}</div>
              <div class="user-panel__meta">{{ currentUserRoles }}</div>
            </div>
            <el-button type="text" class="password-button" @click="openPasswordDialog">修改密码</el-button>
            <el-button type="text" class="logout-button" @click="handleLogout">退出登录</el-button>
          </div>
        </el-header>
        <el-main class="main">
          <router-view />
        </el-main>
      </el-container>
    </el-container>
    <el-dialog
      title="修改密码"
      :visible.sync="passwordDialogVisible"
      width="420px"
      @closed="resetPasswordForm"
    >
      <el-form
        ref="passwordFormRef"
        :model="passwordForm"
        :rules="passwordRules"
        label-position="top"
        @submit.native.prevent="submitPasswordForm"
      >
        <el-form-item label="当前密码" prop="oldPassword">
          <el-input
            v-model="passwordForm.oldPassword"
            type="password"
            show-password
            autocomplete="current-password"
            placeholder="请输入当前密码"
            @keyup.enter.native="submitPasswordForm"
          />
        </el-form-item>
        <el-form-item label="新密码" prop="newPassword">
          <el-input
            v-model="passwordForm.newPassword"
            type="password"
            show-password
            autocomplete="new-password"
            placeholder="请输入至少 6 位的新密码"
            @keyup.enter.native="submitPasswordForm"
          />
        </el-form-item>
        <el-form-item label="确认新密码" prop="confirmPassword">
          <el-input
            v-model="passwordForm.confirmPassword"
            type="password"
            show-password
            autocomplete="new-password"
            placeholder="请再次输入新密码"
            @keyup.enter.native="submitPasswordForm"
          />
        </el-form-item>
      </el-form>
      <span slot="footer" class="dialog-footer">
        <el-button @click="passwordDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="passwordSubmitting" @click="submitPasswordForm">保存新密码</el-button>
      </span>
    </el-dialog>
  </div>
</template>

<script>
import http from './api/http'
import {
  AUTH_CHANGE_EVENT,
  clearAdminAuth,
  getAdminToken,
  getAdminUser,
  setAdminUser
} from './utils/auth'

const menuItems = [
  { path: '/overview', label: '概览' },
  { path: '/users', label: '用户' },
  { path: '/roles', label: '角色' },
  { path: '/regions', label: '区域' },
  { path: '/orgs', label: '机构' },
  { path: '/devices', label: '设备' },
  { path: '/configs', label: '配置' },
  { path: '/prompts', label: 'Prompt' },
  { path: '/symptom-templates', label: '症状模板' },
  { path: '/data-packages', label: '数据包' },
  { path: '/logs', label: '日志' },
  { path: '/feedbacks', label: '反馈' }
]

export default {
  data() {
    return {
      menuItems,
      hasToken: false,
      currentUser: null,
      syncingCurrentUser: false,
      passwordDialogVisible: false,
      passwordSubmitting: false,
      passwordForm: {
        oldPassword: '',
        newPassword: '',
        confirmPassword: ''
      },
      passwordRules: {
        oldPassword: [{ required: true, message: '请输入当前密码', trigger: 'blur' }],
        newPassword: [{ required: true, message: '请输入新密码', trigger: 'blur' }],
        confirmPassword: [{ required: true, message: '请再次输入新密码', trigger: 'blur' }]
      }
    }
  },
  computed: {
    isLoginPage() {
      return this.$route.path === '/login'
    },
    currentUserName() {
      if (!this.currentUser) {
        return '管理员'
      }
      return this.currentUser.naUser || this.currentUser.cdUser || '管理员'
    },
    currentUserRoles() {
      if (!this.currentUser || !Array.isArray(this.currentUser.roles) || !this.currentUser.roles.length) {
        return '管理后台'
      }
      return this.currentUser.roles.join(' / ')
    }
  },
  created() {
    this.syncAuthState()
    if (typeof window !== 'undefined') {
      window.addEventListener(AUTH_CHANGE_EVENT, this.syncAuthState)
    }
  },
  beforeDestroy() {
    if (typeof window !== 'undefined') {
      window.removeEventListener(AUTH_CHANGE_EVENT, this.syncAuthState)
    }
  },
  watch: {
    '$route.fullPath': {
      immediate: true,
      handler() {
        this.syncAuthState()
        this.ensureCurrentUser()
      }
    }
  },
  methods: {
    syncAuthState() {
      this.hasToken = Boolean(getAdminToken())
      this.currentUser = getAdminUser()
    },
    async ensureCurrentUser() {
      if (this.isLoginPage || !this.hasToken || this.currentUser || this.syncingCurrentUser) {
        return
      }
      this.syncingCurrentUser = true
      try {
        const data = await http.get('/admin/api/auth/me')
        const user = data && data.user ? data.user : data
        if (user) {
          setAdminUser(user)
        }
      } catch (error) {
        if (error && error.message && error.message.indexOf('登录已失效') > -1) {
          return
        }
        this.$message.error((error && error.message) || '获取当前管理员信息失败')
      } finally {
        this.syncingCurrentUser = false
        this.syncAuthState()
      }
    },
    openPasswordDialog() {
      this.passwordDialogVisible = true
    },
    resetPasswordForm() {
      this.passwordSubmitting = false
      if (this.$refs.passwordFormRef) {
        this.$refs.passwordFormRef.resetFields()
      } else {
        this.passwordForm = {
          oldPassword: '',
          newPassword: '',
          confirmPassword: ''
        }
      }
    },
    submitPasswordForm() {
      this.$refs.passwordFormRef.validate(async valid => {
        if (!valid) {
          return
        }
        if (this.passwordForm.newPassword.length < 6) {
          this.$message.error('新密码至少 6 位')
          return
        }
        if (this.passwordForm.newPassword !== this.passwordForm.confirmPassword) {
          this.$message.error('两次输入的新密码不一致')
          return
        }
        if (this.passwordForm.oldPassword === this.passwordForm.newPassword) {
          this.$message.error('新密码不能与当前密码相同')
          return
        }

        this.passwordSubmitting = true
        try {
          await http.put('/admin/api/auth/password', {
            oldPassword: this.passwordForm.oldPassword,
            newPassword: this.passwordForm.newPassword,
            confirmPassword: this.passwordForm.confirmPassword
          })
          this.$message.success('密码修改成功')
          this.passwordDialogVisible = false
        } catch (error) {
          this.$message.error((error && error.message) || '密码修改失败')
        } finally {
          this.passwordSubmitting = false
        }
      })
    },
    async handleLogout() {
      this.passwordDialogVisible = false
      try {
        await http.post('/admin/api/auth/logout')
      } catch (error) {
        // 无状态退出时允许忽略接口失败。
      }
      clearAdminAuth()
      this.$router.replace('/login')
    }
  }
}
</script>

<style scoped>
.app-root {
  min-height: 100vh;
}

.app-shell {
  min-height: 100vh;
}

.sidebar {
  background: linear-gradient(180deg, #113f67 0%, #0e2f4d 100%);
  color: #fff;
}

.brand {
  padding: 20px 16px;
  font-size: 18px;
  font-weight: 600;
  letter-spacing: 0.3px;
}

.menu {
  border-right: none;
  background: transparent;
}

.menu ::v-deep .el-menu-item {
  color: rgba(255, 255, 255, 0.88);
}

.menu ::v-deep .el-menu-item.is-active {
  background: rgba(255, 255, 255, 0.12);
  color: #fff;
}

.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #fff;
  border-bottom: 1px solid #e8eef5;
}

.title {
  font-size: 18px;
  font-weight: 600;
  color: #16324f;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 16px;
}

.user-panel {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 2px;
}

.user-panel__name {
  font-size: 14px;
  font-weight: 600;
  color: #16324f;
}

.user-panel__meta {
  font-size: 12px;
  color: #7f8fa4;
}

.logout-button {
  padding: 0;
}

.password-button {
  padding: 0;
}

.main {
  background: #f5f8fb;
}

@media (max-width: 960px) {
  .app-shell {
    flex-direction: column;
  }

  .sidebar {
    width: auto !important;
  }

  .header {
    padding: 0 16px;
  }

  .header-actions {
    gap: 12px;
  }
}
</style>
