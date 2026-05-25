<template>
  <section class="chart-panel">
    <header v-if="title || subtitle || $slots.actions" class="chart-panel__header">
      <div class="chart-panel__title-group">
        <span v-if="title" class="chart-panel__title">{{ title }}</span>
        <span v-if="subtitle" class="chart-panel__subtitle">{{ subtitle }}</span>
      </div>
      <div v-if="$slots.actions" class="chart-panel__actions">
        <slot name="actions" />
      </div>
    </header>
    <slot />
  </section>
</template>

<script>
export default {
  name: 'ChartPanel',
  props: {
    title: {
      type: String,
      default: ''
    },
    subtitle: {
      type: String,
      default: ''
    }
  }
}
</script>

<style scoped>
.chart-panel {
  min-width: 0;
  padding: 20px;
  background: #fff;
  border: 0.5px solid var(--border-color-base);
  border-radius: 8px;
  box-shadow: var(--shadow-card);
}

.chart-panel__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
}

.chart-panel__title-group {
  min-width: 0;
  display: flex;
  align-items: baseline;
  gap: 10px;
  flex-wrap: wrap;
}

.chart-panel__title {
  color: var(--color-text-primary);
  font-size: 15px;
  font-weight: 500;
}

.chart-panel__subtitle {
  color: var(--color-text-secondary);
  font-size: 12px;
}

.chart-panel__actions {
  flex: 0 0 auto;
}
</style>
