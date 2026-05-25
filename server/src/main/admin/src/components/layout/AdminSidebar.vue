<template>
  <aside class="admin-sidebar" aria-label="管理端导航">
    <div class="admin-sidebar__brand">
      <span class="admin-sidebar__brand-mark" aria-hidden="true">医</span>
      <span class="admin-sidebar__brand-text">区域智能后台</span>
    </div>

    <nav class="admin-sidebar__nav" aria-label="主导航">
      <router-link
        v-for="item in menuItems"
        :key="item.path"
        :to="item.path"
        class="admin-sidebar__link"
        active-class="is-active"
        exact
      >
        <i :class="item.icon" class="admin-sidebar__link-icon" aria-hidden="true"></i>
        <span class="admin-sidebar__link-text">{{ item.label }}</span>
      </router-link>
    </nav>
  </aside>
</template>

<script>
export default {
  name: 'AdminSidebar',
  props: {
    menuItems: {
      type: Array,
      required: true
    }
  }
}
</script>
