<template>
  <span :class="['status-pill', toneClass]">
    <span class="dot"></span>
    <slot>{{ label }}</slot>
  </span>
</template>

<script>
export default {
  name: 'StatusPill',
  props: {
    label: {
      type: String,
      default: ''
    },
    tone: {
      type: String,
      default: 'muted'
    }
  },
  computed: {
    toneClass() {
      const allowed = ['success', 'muted', 'warning', 'danger']
      const tone = allowed.indexOf(this.tone) > -1 ? this.tone : 'muted'
      return `status-pill--${tone}`
    }
  }
}
</script>
