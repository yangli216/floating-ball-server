<script>
import {
  formatJsonDisplayTruncationMarker,
  isJsonDisplayTruncationMarker
} from '../../utils/jsonDisplay'

function resolveInitialExpanded(mode, depth) {
  if (mode === 'collapsed') {
    return false
  }
  return depth < 3
}

const INITIAL_VISIBLE_CHILDREN = 80
const VISIBLE_CHILDREN_BATCH = 80
const MAX_TREE_DEPTH = 12

export default {
  name: 'JsonTreeNode',
  props: {
    value: {
      default: null
    },
    label: {
      type: [String, Number],
      default: null
    },
    arrayItem: {
      type: Boolean,
      default: false
    },
    depth: {
      type: Number,
      default: 0
    },
    isLast: {
      type: Boolean,
      default: true
    },
    treeMode: {
      type: String,
      default: 'default'
    },
    treeRevision: {
      type: Number,
      default: 0
    }
  },
  data() {
    return {
      expanded: resolveInitialExpanded(this.treeMode, this.depth),
      visibleLimit: INITIAL_VISIBLE_CHILDREN
    }
  },
  computed: {
    isTruncationMarker() {
      return isJsonDisplayTruncationMarker(this.value)
    },
    isArray() {
      return Array.isArray(this.value)
    },
    isCollection() {
      return !this.isTruncationMarker && this.value !== null && typeof this.value === 'object'
    },
    isDepthLimited() {
      return this.isCollection && this.depth >= MAX_TREE_DEPTH
    },
    isExpanded() {
      return this.expanded && !this.isDepthLimited
    },
    hasLabel() {
      return this.label !== null && this.label !== undefined
    },
    entries() {
      if (!this.isCollection) {
        return []
      }
      const visibleCount = Math.min(this.visibleLimit, this.totalEntries)
      const keys = this.isArray
        ? Array.from({ length: visibleCount }, (item, index) => index)
        : Object.keys(this.value).slice(0, visibleCount)
      return keys.map((key, index) => ({
        key: String(key),
        label: key,
        value: this.value[key],
        isLast: index === keys.length - 1 && visibleCount === this.totalEntries
      }))
    },
    totalEntries() {
      if (!this.isCollection) {
        return 0
      }
      return this.isArray ? this.value.length : Object.keys(this.value).length
    },
    hasMoreEntries() {
      return this.visibleLimit < this.totalEntries
    },
    remainingEntries() {
      return Math.max(0, this.totalEntries - this.visibleLimit)
    },
    nextBatchSize() {
      return Math.min(VISIBLE_CHILDREN_BATCH, this.remainingEntries)
    },
    openingBracket() {
      return this.isArray ? '[' : '{'
    },
    closingBracket() {
      return this.isArray ? ']' : '}'
    },
    collectionSummary() {
      return this.isArray ? `${this.totalEntries} 项` : `${this.totalEntries} 个字段`
    },
    valueType() {
      if (this.value === null) {
        return 'null'
      }
      if (typeof this.value === 'number') {
        return 'number'
      }
      if (typeof this.value === 'boolean') {
        return 'boolean'
      }
      return 'string'
    },
    displayValue() {
      if (this.value === null) {
        return 'null'
      }
      return String(this.value)
    },
    displayStringValue() {
      const escapedValue = this.displayValue
        .replace(/\\/g, '\\\\')
        .replace(/"/g, '\\"')
      return `"${escapedValue}"${this.suffix}`
    },
    truncationText() {
      return formatJsonDisplayTruncationMarker(this.value)
    },
    suffix() {
      return this.isLast ? '' : ','
    },
    toggleLabel() {
      return this.expanded ? '收起当前节点' : '展开当前节点'
    }
  },
  watch: {
    treeRevision() {
      this.expanded = resolveInitialExpanded(this.treeMode, this.depth)
      this.visibleLimit = INITIAL_VISIBLE_CHILDREN
    },
    value() {
      this.expanded = resolveInitialExpanded(this.treeMode, this.depth)
      this.visibleLimit = INITIAL_VISIBLE_CHILDREN
    }
  },
  methods: {
    toggleExpanded() {
      if (this.isCollection && !this.isDepthLimited) {
        this.expanded = !this.expanded
      }
    },
    showMoreEntries() {
      this.visibleLimit += VISIBLE_CHILDREN_BATCH
    }
  }
}
</script>

<template>
  <div class="json-tree-node">
    <template v-if="isCollection">
      <div class="json-tree-node__line">
        <button
          v-if="!isDepthLimited"
          type="button"
          class="json-tree-node__toggle"
          :aria-label="toggleLabel"
          :aria-expanded="isExpanded"
          :title="toggleLabel"
          @click="toggleExpanded"
        >
          <i :class="isExpanded ? 'el-icon-arrow-down' : 'el-icon-arrow-right'" />
        </button>
        <span v-else class="json-tree-node__depth-limit" title="已达到结构化展示深度上限">
          <i class="el-icon-more" />
        </span>
        <span
          v-if="hasLabel"
          :class="arrayItem ? 'json-tree-node__index' : 'json-tree-node__key'"
        >{{ arrayItem ? `[${label}]` : label }}</span>
        <span v-if="hasLabel" class="json-tree-node__colon">:</span>
        <span class="json-tree-node__bracket">{{ openingBracket }}</span>
        <button
          v-if="!isExpanded && !isDepthLimited"
          type="button"
          class="json-tree-node__summary"
          @click="toggleExpanded"
        >{{ collectionSummary }}</button>
        <span v-else-if="isDepthLimited" class="json-tree-node__summary json-tree-node__summary--static">
          {{ collectionSummary }} · 深度上限
        </span>
        <span v-if="!isExpanded" class="json-tree-node__bracket">{{ closingBracket }}{{ suffix }}</span>
      </div>
      <div v-if="isExpanded" class="json-tree-node__children">
        <json-tree-node
          v-for="entry in entries"
          :key="entry.key"
          :value="entry.value"
          :label="entry.label"
          :array-item="isArray"
          :depth="depth + 1"
          :is-last="entry.isLast"
          :tree-mode="treeMode"
          :tree-revision="treeRevision"
        />
        <button
          v-if="hasMoreEntries"
          type="button"
          class="json-tree-node__load-more"
          @click="showMoreEntries"
        >
          <i class="el-icon-plus" />
          再显示 {{ nextBatchSize }} 项
          <span>剩余 {{ remainingEntries }} 项</span>
        </button>
        <div class="json-tree-node__closing-line">
          <span class="json-tree-node__bracket">{{ closingBracket }}{{ suffix }}</span>
        </div>
      </div>
    </template>
    <div v-else class="json-tree-node__line json-tree-node__line--primitive">
      <span class="json-tree-node__spacer" aria-hidden="true" />
      <span
        v-if="hasLabel"
        :class="arrayItem ? 'json-tree-node__index' : 'json-tree-node__key'"
      >{{ arrayItem ? `[${label}]` : label }}</span>
      <span v-if="hasLabel" class="json-tree-node__colon">:</span>
      <span
        v-if="isTruncationMarker"
        class="json-tree-node__value json-tree-node__value--truncated"
      >
        <i class="el-icon-warning-outline" aria-hidden="true" />
        {{ truncationText }}{{ suffix }}
      </span>
      <template v-else-if="valueType === 'string'">
        <span class="json-tree-node__value json-tree-node__value--string">{{ displayStringValue }}</span>
      </template>
      <span v-else :class="`json-tree-node__value json-tree-node__value--${valueType}`">
        {{ displayValue }}{{ suffix }}
      </span>
    </div>
  </div>
</template>

<style scoped>
.json-tree-node {
  min-width: 0;
}

.json-tree-node__line {
  min-width: 0;
  min-height: 26px;
  display: flex;
  align-items: flex-start;
  gap: 5px;
  line-height: 1.65;
}

.json-tree-node__line--primitive {
  padding: 2px 0;
}

.json-tree-node__toggle,
.json-tree-node__spacer,
.json-tree-node__depth-limit {
  width: 22px;
  height: 22px;
  flex: 0 0 22px;
}

.json-tree-node__depth-limit {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: var(--color-text-placeholder);
}

.json-tree-node__toggle {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  margin-top: 1px;
  padding: 0;
  border: 0;
  border-radius: 4px;
  background: transparent;
  color: var(--color-text-secondary);
  cursor: pointer;
}

.json-tree-node__toggle:hover {
  background: var(--color-info-soft);
  color: var(--color-info);
}

.json-tree-node__toggle:focus-visible,
.json-tree-node__summary:focus-visible {
  outline: 2px solid var(--color-primary);
  outline-offset: 1px;
}

.json-tree-node__key {
  max-width: 280px;
  color: var(--color-info);
  font-weight: 600;
  overflow-wrap: anywhere;
}

.json-tree-node__index {
  color: var(--color-text-secondary);
  font-variant-numeric: tabular-nums;
}

.json-tree-node__colon,
.json-tree-node__bracket {
  color: var(--color-text-secondary);
}

.json-tree-node__summary {
  padding: 0 4px;
  border: 0;
  border-bottom: 1px dashed var(--border-color-hover);
  background: transparent;
  color: var(--color-text-secondary);
  font-family: inherit;
  font-size: 12px;
  line-height: 21px;
  cursor: pointer;
}

.json-tree-node__summary:hover {
  color: var(--color-info);
  border-bottom-color: var(--color-info);
}

.json-tree-node__summary--static,
.json-tree-node__summary--static:hover {
  border-bottom: 0;
  color: var(--color-text-placeholder);
  cursor: default;
}

.json-tree-node__children {
  margin-left: 11px;
  padding-left: 16px;
  border-left: 1px solid #DDE7E3;
}

.json-tree-node__closing-line {
  min-height: 24px;
  padding-left: 22px;
  line-height: 1.65;
}

.json-tree-node__load-more {
  min-height: 30px;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  margin: 4px 0 6px 22px;
  padding: 0 10px;
  border: 1px solid var(--border-color-base);
  border-radius: 6px;
  background: #FFFFFF;
  color: var(--color-info);
  font-family: inherit;
  font-size: 12px;
  cursor: pointer;
}

.json-tree-node__load-more span {
  color: var(--color-text-secondary);
}

.json-tree-node__load-more:hover {
  border-color: #B9D4E8;
  background: var(--color-info-soft);
}

.json-tree-node__load-more:focus-visible {
  outline: 2px solid var(--color-primary);
  outline-offset: 1px;
}

.json-tree-node__value {
  min-width: 0;
  overflow-wrap: anywhere;
}

.json-tree-node__value--string {
  flex: 1 1 auto;
  color: #0F6E56;
  white-space: pre-wrap;
}

.json-tree-node__value--number {
  color: #9A5B13;
}

.json-tree-node__value--boolean {
  color: #6D4AA2;
  font-weight: 600;
}

.json-tree-node__value--null {
  color: var(--color-danger);
  font-style: italic;
}

.json-tree-node__value--truncated {
  display: inline-flex;
  align-items: flex-start;
  gap: 5px;
  color: var(--color-warning);
  font-family: inherit;
}

.json-tree-node__value--truncated i {
  margin-top: 3px;
}

@media (max-width: 720px) {
  .json-tree-node__key {
    max-width: 150px;
  }

  .json-tree-node__children {
    padding-left: 10px;
  }
}
</style>
