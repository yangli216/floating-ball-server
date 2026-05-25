<template>
  <button
    type="button"
    :class="['table-action', danger ? 'table-action--danger' : '', disabled ? 'is-disabled' : '']"
    :disabled="disabled"
    @click="$emit('click', $event)"
  >
    <slot>{{ label }}</slot>
  </button>
</template>

<script>
export default {
  name: 'TableAction',
  props: {
    label: {
      type: String,
      default: ''
    },
    danger: {
      type: Boolean,
      default: false
    },
    disabled: {
      type: Boolean,
      default: false
    }
  }
}
</script>
