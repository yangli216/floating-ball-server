<template>
  <code :class="['code-tag', toneClass]">{{ displayValue }}</code>
</template>

<script>
export default {
  name: 'CodeTag',
  props: {
    value: {
      type: [String, Number],
      default: ''
    },
    placeholder: {
      type: String,
      default: '--'
    },
    tone: {
      type: String,
      default: 'default'
    }
  },
  computed: {
    displayValue() {
      return this.value === null || this.value === undefined || this.value === '' ? this.placeholder : this.value
    },
    toneClass() {
      return this.tone === 'primary' ? 'code-tag--primary' : ''
    }
  }
}
</script>
