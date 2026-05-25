<template>
  <div class="segmented">
    <button
      v-for="option in options"
      :key="option.value"
      type="button"
      :class="{ active: value === option.value }"
      @click="$emit('input', option.value)"
    >
      {{ option.label }}
    </button>
  </div>
</template>

<script>
export default {
  name: 'SegmentedSwitch',
  props: {
    value: {
      type: [String, Number, Boolean],
      default: ''
    },
    options: {
      type: Array,
      required: true
    }
  }
}
</script>
