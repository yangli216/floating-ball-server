<template>
  <div class="time-range-filter">
    <div class="time-range-filter__tabs">
      <button
        v-for="option in options"
        :key="option.value"
        type="button"
        :class="['time-range-filter__tab', { 'is-active': value === option.value }]"
        @click="$emit('input', option.value)"
      >
        {{ option.label }}
      </button>
    </div>
    <div v-if="value === customValue" class="time-range-filter__custom">
      <el-date-picker
        :value="dateFrom"
        type="date"
        placeholder="开始日期"
        size="small"
        value-format="yyyy-MM-dd"
        @input="$emit('update:dateFrom', $event)"
        @change="$emit('custom-change')"
      />
      <span class="time-range-filter__separator">至</span>
      <el-date-picker
        :value="dateTo"
        type="date"
        placeholder="结束日期"
        size="small"
        value-format="yyyy-MM-dd"
        @input="$emit('update:dateTo', $event)"
        @change="$emit('custom-change')"
      />
    </div>
  </div>
</template>

<script>
export default {
  name: 'TimeRangeFilter',
  props: {
    value: {
      type: String,
      required: true
    },
    options: {
      type: Array,
      required: true
    },
    dateFrom: {
      type: String,
      default: ''
    },
    dateTo: {
      type: String,
      default: ''
    },
    customValue: {
      type: String,
      default: 'custom'
    }
  }
}
</script>

<style scoped>
.time-range-filter {
  display: grid;
  gap: 8px;
}

.time-range-filter__tabs {
  display: inline-flex;
  align-items: center;
  width: max-content;
  max-width: 100%;
  border: 0.5px solid var(--border-color-base);
  border-radius: 8px;
  overflow: hidden;
  background: #fff;
}

.time-range-filter__tab {
  height: 34px;
  padding: 0 14px;
  border: none;
  border-left: 0.5px solid var(--border-color-base);
  background: #fff;
  color: var(--color-text-secondary);
  font-size: 12px;
  cursor: pointer;
}

.time-range-filter__tab:first-child {
  border-left: none;
}

.time-range-filter__tab.is-active {
  background: var(--color-primary-soft);
  color: var(--color-primary-hover);
  font-weight: 500;
}

.time-range-filter__custom {
  display: flex;
  align-items: center;
  gap: 8px;
}

.time-range-filter__custom .el-date-editor {
  width: 150px;
}

.time-range-filter__separator {
  flex: 0 0 auto;
  color: var(--color-text-secondary);
  font-size: 12px;
}

@media (max-width: 760px) {
  .time-range-filter__tabs {
    width: 100%;
    overflow-x: auto;
  }

  .time-range-filter__tab {
    flex: 1 0 auto;
  }

  .time-range-filter__custom {
    flex-wrap: wrap;
  }
}
</style>
