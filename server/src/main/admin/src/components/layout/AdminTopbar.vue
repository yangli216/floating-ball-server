<template>
  <header class="admin-topbar">
    <div class="admin-topbar__leading">
      <button type="button" class="admin-topbar__icon-button" aria-label="收起侧边导航">
        <i class="el-icon-s-fold" aria-hidden="true"></i>
      </button>
      <span class="admin-topbar__title">{{ title || '管理端' }}</span>
    </div>

    <div class="admin-topbar__actions">
      <button type="button" class="admin-topbar__icon-button" aria-label="切换全屏">
        <i class="el-icon-full-screen" aria-hidden="true"></i>
      </button>
      <button type="button" class="admin-topbar__icon-button" aria-label="查看系统文档">
        <i class="el-icon-document" aria-hidden="true"></i>
      </button>
      <div class="admin-user-menu">
        <button type="button" class="admin-user-menu__trigger" aria-label="打开账号菜单">
          <span class="admin-user-menu__avatar">{{ currentUserInitial }}</span>
          <i class="el-icon-arrow-down admin-user-menu__chevron" aria-hidden="true"></i>
        </button>
        <div class="admin-user-menu__panel">
          <div class="admin-user-menu__name">{{ currentUserName }}</div>
          <button type="button" @click="$emit('open-password-dialog')">修改密码</button>
          <button type="button" @click="$emit('logout')">退出登录</button>
        </div>
      </div>
    </div>
  </header>
</template>

<script>
export default {
  name: 'AdminTopbar',
  props: {
    title: {
      type: String,
      default: ''
    },
    currentUserName: {
      type: String,
      default: '管理员'
    },
    currentUserInitial: {
      type: String,
      default: '管'
    }
  }
}
</script>
