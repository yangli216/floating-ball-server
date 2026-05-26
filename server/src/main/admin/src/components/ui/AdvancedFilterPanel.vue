<template>
  <section class="advanced-filter-panel">
    <button
      type="button"
      class="advanced-filter-panel__trigger"
      :aria-expanded="String(open)"
      @click="$emit('input', !open)"
    >
      <span>{{ title }}</span>
      <i :class="open ? 'el-icon-arrow-up' : 'el-icon-arrow-down'" aria-hidden="true"></i>
    </button>
    <div v-if="open" class="advanced-filter-panel__body">
      <slot />
    </div>
  </section>
</template>

<script>
export default {
  name: 'AdvancedFilterPanel',
  props: {
    value: {
      type: Boolean,
      default: false
    },
    title: {
      type: String,
      default: '高级筛选'
    }
  },
  computed: {
    open() {
      return this.value
    }
  }
}
</script>

<style scoped>
.advanced-filter-panel {
  width: 100%;
  min-width: 0;
  display: grid;
  gap: 10px;
}

.advanced-filter-panel__trigger {
  width: max-content;
  min-height: 30px;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 0 10px;
  border: 0.5px solid var(--border-color-base);
  border-radius: 8px;
  background: #fff;
  color: var(--color-text-regular);
  cursor: pointer;
  font-size: 12px;
}

.advanced-filter-panel__trigger:hover {
  border-color: var(--color-primary);
  color: var(--color-primary-hover);
}

.advanced-filter-panel__body {
  min-width: 0;
  display: flex;
  flex-wrap: wrap;
  align-items: flex-end;
  gap: 10px;
  padding-top: 10px;
  border-top: 0.5px solid var(--border-color-light);
}
</style>
