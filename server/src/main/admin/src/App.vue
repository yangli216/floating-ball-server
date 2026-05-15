<template>
  <div class="app-root">
    <router-view v-if="isLoginPage" />
    <div v-else class="admin-layout">
      <aside class="sidebar">
        <div class="brand">
          <span class="brand__mark">医</span>
          <span class="brand__text">区域智能后台</span>
        </div>
        <nav class="sidebar-nav">
          <router-link
            v-for="item in menuItems"
            :key="item.path"
            :to="item.path"
            class="sidebar-link"
            active-class="is-active"
          >
            <span class="sidebar-link__icon">{{ item.icon }}</span>
            <span>{{ item.label }}</span>
          </router-link>
        </nav>
      </aside>

      <section class="workspace">
        <header class="navbar">
          <div class="navbar-left">
            <button type="button" class="hamburger" aria-label="菜单">☰</button>
            <span class="navbar-title">{{ $route.meta.title || '管理端' }}</span>
          </div>
          <div class="navbar-right">
            <button type="button" class="navbar-icon" aria-label="全屏">⛶</button>
            <button type="button" class="navbar-icon" aria-label="语言">文</button>
            <div class="user-menu">
              <button type="button" class="user-menu__trigger">
                <span class="user-menu__avatar">{{ currentUserInitial }}</span>
                <span class="user-menu__chevron">▾</span>
              </button>
              <div class="user-menu__panel">
                <div class="user-menu__name">{{ currentUserName }}</div>
                <button type="button" @click="openPasswordDialog">修改密码</button>
                <button type="button" @click="handleLogout">退出登录</button>
              </div>
            </div>
          </div>
        </header>

        <div class="tags-view">
          <router-link
            v-for="tag in visitedTags"
            :key="tag.path"
            :to="tag.path"
            class="tag-link"
            active-class="is-active"
          >
            <span class="tag-link__dot"></span>
            <span>{{ tag.title }}</span>
            <button
              v-if="tag.path !== '/overview'"
              type="button"
              class="tag-link__close"
              @click.prevent.stop="closeTag(tag.path)"
            >×</button>
          </router-link>
        </div>

        <main class="content-main">
          <router-view />
        </main>
      </section>
    </div>

    <el-dialog
      v-if="passwordDialogVisible"
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
        <el-button type="primary" :loading="passwordSubmitting" @click="submitPasswordForm">保存</el-button>
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
  { path: '/overview', label: '概览', icon: '●' },
  { path: '/users', label: '用户', icon: '用' },
  { path: '/roles', label: '角色', icon: '角' },
  { path: '/regions', label: '区域', icon: '区' },
  { path: '/orgs', label: '机构', icon: '机' },
  { path: '/devices', label: '设备', icon: '设' },
  { path: '/configs', label: '配置', icon: '配' },
  { path: '/prompts', label: '提示词', icon: '提' },
  { path: '/symptom-templates', label: '症状模板', icon: '症' },
  { path: '/data-packages', label: '数据包', icon: '数' },
  { path: '/releases', label: '版本发布', icon: '版' },
  { path: '/logs', label: '日志', icon: '日' },
  { path: '/user-logs', label: '用户日志', icon: '用' },
  { path: '/feedbacks', label: '反馈', icon: '反' },
  { path: '/analytics', label: '统计分析', icon: '统' },
  { path: '/function-usage', label: '辅诊功能', icon: '辅' },
  { path: '/user-activity', label: '用户活跃度', icon: '活' }
]

export default {
  data() {
    return {
      menuItems,
      hasToken: false,
      currentUser: null,
      syncingCurrentUser: false,
      visitedTags: [],
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
    currentUserInitial() {
      return this.currentUserName.slice(0, 1)
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
        this.addVisitedTag()
      }
    }
  },
  methods: {
    syncAuthState() {
      this.hasToken = Boolean(getAdminToken())
      this.currentUser = getAdminUser()
    },
    addVisitedTag() {
      if (this.isLoginPage) {
        return
      }
      const path = this.$route.path === '/' ? '/overview' : (this.$route.path || '/overview')
      const title = this.$route.meta && this.$route.meta.title ? this.$route.meta.title : (path === '/overview' ? '首页概览' : '当前页面')
      if (!this.visitedTags.some(tag => tag.path === '/overview')) {
        this.visitedTags.push({ path: '/overview', title: '首页概览' })
      }
      if (!this.visitedTags.some(tag => tag.path === path)) {
        this.visitedTags.push({ path, title })
      }
    },
    closeTag(path) {
      const index = this.visitedTags.findIndex(tag => tag.path === path)
      if (index === -1 || path === '/overview') {
        return
      }
      this.visitedTags.splice(index, 1)
      if (this.$route.path === path) {
        const fallback = this.visitedTags[index - 1] || this.visitedTags[index] || this.visitedTags[0]
        this.$router.push(fallback ? fallback.path : '/overview')
      }
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
      this.visitedTags = []
      this.$router.replace('/login')
    }
  }
}
</script>

<style scoped>
.app-root {
  min-height: 100vh;
}

.admin-layout {
  min-height: 100vh;
  display: grid;
  grid-template-columns: 200px minmax(0, 1fr);
  background: #eef1f5;
}

.sidebar {
  position: sticky;
  top: 0;
  height: 100vh;
  background: #304156;
  color: #bfcbd9;
  display: flex;
  flex-direction: column;
}

.brand {
  height: 56px;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 0 18px;
  color: #fff;
  font-size: 15px;
  font-weight: 500;
}

.brand__mark {
  width: 26px;
  height: 26px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 8px;
  background: #1D9E75;
  font-size: 13px;
}

.brand__text {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.sidebar-nav {
  padding: 8px 0;
  overflow-y: auto;
  flex: 1;
}

.sidebar-link {
  height: 46px;
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 0 20px;
  color: #bfcbd9;
  font-size: 14px;
  text-decoration: none;
}

.sidebar-link:hover {
  background: #263445;
  color: #fff;
}

.sidebar-link.is-active {
  background: #263445;
  color: #1D9E75;
}

.sidebar-link__icon {
  width: 18px;
  display: inline-flex;
  justify-content: center;
  color: currentColor;
  font-size: 12px;
}

.workspace {
  min-width: 0;
}

.navbar {
  height: 50px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
  background: #fff;
  border-bottom: 1px solid #d8dce5;
}

.navbar-left,
.navbar-right {
  display: flex;
  align-items: center;
  gap: 16px;
}

.hamburger,
.navbar-icon {
  border: none;
  background: transparent;
  color: #303133;
  cursor: pointer;
}

.hamburger {
  font-size: 22px;
  line-height: 1;
}

.navbar-title {
  color: #97a8be;
  font-size: 14px;
}

.navbar-icon {
  width: 28px;
  height: 28px;
  border-radius: 4px;
  font-size: 16px;
}

.navbar-icon:hover {
  background: #f5f7fa;
}

.user-menu {
  position: relative;
}

.user-menu__trigger {
  height: 34px;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 0;
  border: none;
  background: transparent;
  color: #303133;
  cursor: pointer;
}

.user-menu__avatar {
  width: 34px;
  height: 34px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 8px;
  background: #1D9E75;
  color: #fff;
  font-size: 14px;
  font-weight: 500;
}

.user-menu__chevron {
  color: #606266;
  font-size: 12px;
}

.user-menu__panel {
  position: absolute;
  top: calc(100% + 8px);
  right: 0;
  z-index: 20;
  display: none;
  min-width: 140px;
  padding: 6px;
  border: 1px solid #ebeef5;
  border-radius: 4px;
  background: #fff;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
}

.user-menu:hover .user-menu__panel,
.user-menu:focus-within .user-menu__panel {
  display: grid;
  gap: 2px;
}

.user-menu__name {
  padding: 8px 10px;
  color: #909399;
  font-size: 12px;
  border-bottom: 1px solid #ebeef5;
}

.user-menu__panel button {
  height: 32px;
  padding: 0 10px;
  border: none;
  border-radius: 3px;
  background: transparent;
  color: #606266;
  text-align: left;
  cursor: pointer;
  font-size: 13px;
}

.user-menu__panel button:hover {
  background: #f5f7fa;
  color: #1D9E75;
}

.tags-view {
  height: 34px;
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 0 16px;
  overflow-x: auto;
  background: #fff;
  border-bottom: 1px solid #d8dce5;
  box-shadow: 0 1px 3px rgba(0, 21, 41, 0.08);
}

.tag-link {
  height: 24px;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  flex: 0 0 auto;
  padding: 0 8px;
  border: 1px solid #d8dce5;
  background: #fff;
  color: #495060;
  font-size: 12px;
  text-decoration: none;
}

.tag-link__dot {
  width: 6px;
  height: 6px;
  display: none;
  border-radius: 999px;
  background: currentColor;
}

.tag-link__close {
  width: 14px;
  height: 14px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 0;
  border: none;
  border-radius: 999px;
  background: transparent;
  color: inherit;
  cursor: pointer;
  font-size: 12px;
  line-height: 1;
}

.tag-link__close:hover {
  background: rgba(255, 255, 255, 0.22);
}

.tag-link.is-active {
  border-color: #1D9E75;
  background: #1D9E75;
  color: #fff;
}

.tag-link.is-active .tag-link__dot {
  display: inline-block;
}

.content-main {
  min-width: 0;
  padding: 24px;
}

@media (max-width: 960px) {
  .admin-layout {
    grid-template-columns: 1fr;
  }

  .sidebar {
    position: static;
    height: auto;
  }

  .sidebar-nav {
    display: flex;
    flex-wrap: wrap;
  }

  .sidebar-link {
    width: auto;
  }

  .content-main {
    padding: 16px;
  }
}
</style>
