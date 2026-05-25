<template>
  <section class="admin-filter-bar">
    <div class="admin-filter-bar__main">
      <slot />
    </div>
    <div v-if="$slots.actions" class="admin-filter-bar__actions">
      <slot name="actions" />
    </div>
  </section>
</template>

<script>
export default {
  name: 'AdminFilterBar'
}
</script>

<style scoped>
.admin-filter-bar {
  min-width: 0;
  display: grid;
  gap: 14px;
  padding: 18px 20px;
  background: #fff;
  border: 0.5px solid var(--border-color-base);
  border-radius: 8px;
  box-shadow: var(--shadow-card);
}

.admin-filter-bar__main {
  min-width: 0;
}

.admin-filter-bar__actions {
  display: flex;
  justify-content: flex-end;
  padding-top: 14px;
  border-top: 0.5px solid var(--border-color-light);
}
</style>
