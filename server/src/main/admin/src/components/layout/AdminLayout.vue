<template>
  <div class="admin-shell">
    <AdminSidebar class="admin-shell__sidebar" :menu-items="menuItems" />

    <section class="admin-shell__main">
      <AdminTopbar
        class="admin-shell__topbar"
        :title="routeTitle"
        :current-user-name="currentUserName"
        :current-user-initial="currentUserInitial"
        @open-password-dialog="$emit('open-password-dialog')"
        @logout="$emit('logout')"
      />

      <AdminTabBar
        class="admin-shell__tabs"
        :visited-tags="visitedTags"
        @close-tag="$emit('close-tag', $event)"
      />

      <main class="admin-shell__content" tabindex="-1">
        <slot />
      </main>
    </section>
  </div>
</template>

<script>
import AdminSidebar from './AdminSidebar.vue'
import AdminTabBar from './AdminTabBar.vue'
import AdminTopbar from './AdminTopbar.vue'

export default {
  name: 'AdminLayout',
  components: {
    AdminSidebar,
    AdminTabBar,
    AdminTopbar
  },
  props: {
    menuItems: {
      type: Array,
      required: true
    },
    routeTitle: {
      type: String,
      default: ''
    },
    visitedTags: {
      type: Array,
      default: () => []
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
