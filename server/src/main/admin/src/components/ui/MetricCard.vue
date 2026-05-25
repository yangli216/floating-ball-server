<template>
  <component
    :is="clickable ? 'button' : 'article'"
    :type="clickable ? 'button' : null"
    :class="['metric-card', clickable ? 'metric-card--clickable' : '', toneClass]"
    @click="handleClick"
  >
    <div class="metric-card__label">{{ label }}</div>
    <div class="metric-card__value">{{ value }}</div>
    <div v-if="growthText" :class="['metric-card__growth', growthUp ? 'is-up' : 'is-down']">
      <i :class="growthIcon" aria-hidden="true"></i>
      <span>{{ growthText }}</span>
    </div>
    <div v-if="desc" class="metric-card__desc">{{ desc }}</div>
  </component>
</template>

<script>
export default {
  name: 'MetricCard',
  props: {
    label: {
      type: String,
      required: true
    },
    value: {
      type: [String, Number],
      default: 0
    },
    desc: {
      type: String,
      default: ''
    },
    growthText: {
      type: String,
      default: ''
    },
    growthUp: {
      type: Boolean,
      default: true
    },
    clickable: {
      type: Boolean,
      default: false
    },
    tone: {
      type: String,
      default: 'normal'
    }
  },
  computed: {
    growthIcon() {
      return this.growthUp ? 'el-icon-top' : 'el-icon-bottom'
    },
    toneClass() {
      return this.tone === 'risk' ? 'metric-card--risk' : ''
    }
  },
  methods: {
    handleClick() {
      if (this.clickable) {
        this.$emit('click')
      }
    }
  }
}
</script>

<style scoped>
.metric-card {
  min-height: 118px;
  padding: 18px 20px;
  background: #fff;
  border: 0.5px solid var(--border-color-base);
  border-radius: 8px;
  display: grid;
  align-content: start;
  gap: 4px;
  width: 100%;
  color: inherit;
  text-align: left;
}

.metric-card--clickable {
  cursor: pointer;
}

.metric-card--clickable:hover {
  border-color: #c8e8dc;
}

.metric-card__label {
  color: var(--color-text-secondary);
  font-size: 12px;
}

.metric-card__value {
  min-width: 0;
  padding: 4px 0;
  color: var(--color-text-primary);
  font-size: 28px;
  font-weight: 500;
  line-height: 1.2;
  overflow-wrap: anywhere;
}

.metric-card__growth {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  color: #0f6e56;
  font-size: 12px;
}

.metric-card__growth.is-down {
  color: #a32d2d;
}

.metric-card--risk .metric-card__growth.is-up {
  color: #a32d2d;
}

.metric-card--risk .metric-card__growth.is-down {
  color: #0f6e56;
}

.metric-card__desc {
  margin-top: 8px;
  padding-top: 8px;
  border-top: 0.5px solid var(--border-color-light);
  color: var(--color-text-secondary);
  font-size: 12px;
  line-height: 1.5;
}
</style>
