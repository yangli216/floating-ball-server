<template>
  <nav class="admin-tabbar" aria-label="已打开页面">
    <router-link
      v-for="tag in visitedTags"
      :key="tag.path"
      :to="tag.path"
      class="admin-tabbar__item"
      active-class="is-active"
    >
      <span class="admin-tabbar__dot" aria-hidden="true"></span>
      <span class="admin-tabbar__title">{{ tag.title }}</span>
      <button
        v-if="tag.path !== '/overview'"
        type="button"
        class="admin-tabbar__close"
        :aria-label="`关闭${tag.title}`"
        @click.prevent.stop="$emit('close-tag', tag.path)"
      >
        <i class="el-icon-close" aria-hidden="true"></i>
      </button>
    </router-link>
  </nav>
</template>

<script>
export default {
  name: 'AdminTabBar',
  props: {
    visitedTags: {
      type: Array,
      default: () => []
    }
  }
}
</script>
