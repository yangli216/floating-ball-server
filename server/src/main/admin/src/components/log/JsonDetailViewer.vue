<script>
import SegmentedSwitch from '../ui/SegmentedSwitch.vue'
import JsonTreeNode from './JsonTreeNode.vue'
import {
  hasJsonDisplayValue,
  normalizeJsonDisplayValue,
  stringifyJsonDisplayValue
} from '../../utils/jsonDisplay'

const MODE_OPTIONS = [
  { value: 'structured', label: '结构化' },
  { value: 'raw', label: '原文' }
]

export default {
  name: 'JsonDetailViewer',
  components: {
    JsonTreeNode,
    SegmentedSwitch
  },
  props: {
    value: {
      default: null
    },
    rawValue: {
      default: undefined
    },
    emptyText: {
      type: String,
      default: '无数据'
    }
  },
  data() {
    return {
      mode: 'structured',
      modeOptions: MODE_OPTIONS,
      treeMode: 'default',
      treeRevision: 0
    }
  },
  computed: {
    normalizedDisplay() {
      return normalizeJsonDisplayValue(this.value)
    },
    normalizedValue() {
      return this.normalizedDisplay.value
    },
    isStructuredTruncated() {
      return this.normalizedDisplay.truncated
    },
    hasContent() {
      return hasJsonDisplayValue(this.normalizedValue)
    },
    structuredAvailable() {
      return this.normalizedValue !== null && typeof this.normalizedValue === 'object'
    },
    rawText() {
      const source = this.rawValue === undefined ? this.value : this.rawValue
      return stringifyJsonDisplayValue(source, this.emptyText)
    },
    structuredText() {
      return stringifyJsonDisplayValue(this.normalizedValue, this.emptyText)
    },
    currentText() {
      return this.mode === 'raw' ? this.rawText : this.structuredText
    },
    contentMeta() {
      if (!this.hasContent) {
        return this.emptyText
      }
      if (Array.isArray(this.normalizedValue)) {
        return this.withTruncationMeta(
          `数组 · ${this.normalizedDisplay.rootEntryCount} 项`
        )
      }
      if (this.structuredAvailable) {
        return this.withTruncationMeta(
          `对象 · ${this.normalizedDisplay.rootEntryCount} 个字段`
        )
      }
      return '文本'
    }
  },
  watch: {
    value() {
      this.resetViewer()
    }
  },
  methods: {
    withTruncationMeta(meta) {
      return this.isStructuredTruncated && this.mode === 'structured'
        ? `${meta} · 结构化视图已截断`
        : meta
    },
    resetViewer() {
      this.mode = 'structured'
      this.treeMode = 'default'
      this.treeRevision += 1
    },
    setTreeMode(mode) {
      this.treeMode = mode
      this.treeRevision += 1
    },
    async copyCurrentText() {
      const text = this.currentText
      try {
        if (navigator.clipboard && window.isSecureContext) {
          await navigator.clipboard.writeText(text)
        } else {
          const textarea = document.createElement('textarea')
          textarea.value = text
          textarea.setAttribute('readonly', '')
          textarea.style.position = 'fixed'
          textarea.style.opacity = '0'
          document.body.appendChild(textarea)
          textarea.select()
          document.execCommand('copy')
          document.body.removeChild(textarea)
        }
        this.$message.success('内容已复制')
      } catch (error) {
        this.$message.error('复制失败，请手动选择内容')
      }
    }
  }
}
</script>

<template>
  <div class="json-detail-viewer">
    <div class="json-detail-viewer__toolbar">
      <div
        :class="[
          'json-detail-viewer__meta',
          isStructuredTruncated && mode === 'structured' ? 'is-truncated' : ''
        ]"
      >
        <span class="json-detail-viewer__meta-dot" aria-hidden="true" />
        <span>{{ contentMeta }}</span>
      </div>
      <div class="json-detail-viewer__actions">
        <segmented-switch
          v-if="structuredAvailable"
          v-model="mode"
          :options="modeOptions"
          class="json-detail-viewer__mode"
        />
        <template v-if="structuredAvailable && mode === 'structured'">
          <button
            type="button"
            class="json-detail-viewer__icon-button"
            aria-label="全部收起"
            title="全部收起"
            @click="setTreeMode('collapsed')"
          >
            <i class="el-icon-minus" />
          </button>
          <button
            type="button"
            class="json-detail-viewer__icon-button"
            aria-label="恢复默认展开"
            title="恢复默认展开"
            @click="setTreeMode('default')"
          >
            <i class="el-icon-refresh-right" />
          </button>
        </template>
        <button
          type="button"
          class="json-detail-viewer__icon-button"
          aria-label="复制当前内容"
          title="复制当前内容"
          @click="copyCurrentText"
        >
          <i class="el-icon-document-copy" />
        </button>
      </div>
    </div>

    <div class="json-detail-viewer__content">
      <json-tree-node
        v-if="hasContent && structuredAvailable && mode === 'structured'"
        :value="normalizedValue"
        :tree-mode="treeMode"
        :tree-revision="treeRevision"
      />
      <div
        v-else-if="hasContent && mode === 'structured'"
        class="json-detail-viewer__plain-text"
      >{{ normalizedValue }}</div>
      <pre v-else-if="hasContent" class="json-detail-viewer__raw-text">{{ rawText }}</pre>
      <div v-else class="json-detail-viewer__empty">{{ emptyText }}</div>
    </div>
  </div>
</template>

<style scoped>
.json-detail-viewer {
  min-width: 0;
  overflow: hidden;
  border: 1px solid var(--border-color-base);
  border-radius: 8px;
  background: #F7F9FA;
}

.json-detail-viewer__toolbar {
  min-height: 42px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 4px 8px 4px 12px;
  border-bottom: 1px solid var(--border-color-base);
  background: #FFFFFF;
}

.json-detail-viewer__meta,
.json-detail-viewer__actions {
  display: flex;
  align-items: center;
}

.json-detail-viewer__meta {
  min-width: 0;
  gap: 7px;
  color: var(--color-text-secondary);
  font-size: 12px;
}

.json-detail-viewer__meta-dot {
  width: 6px;
  height: 6px;
  flex: 0 0 auto;
  border-radius: 50%;
  background: var(--color-primary);
}

.json-detail-viewer__meta.is-truncated {
  color: #8A5A12;
}

.json-detail-viewer__meta.is-truncated .json-detail-viewer__meta-dot {
  background: var(--color-warning);
}

.json-detail-viewer__actions {
  flex: 0 0 auto;
  gap: 4px;
}

.json-detail-viewer__mode {
  margin-right: 4px;
}

.json-detail-viewer__icon-button {
  width: 30px;
  height: 30px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 0;
  border: 1px solid transparent;
  border-radius: 6px;
  background: transparent;
  color: var(--color-text-secondary);
  cursor: pointer;
}

.json-detail-viewer__icon-button:hover {
  border-color: var(--border-color-base);
  background: var(--background-color-base);
  color: var(--color-primary-hover);
}

.json-detail-viewer__icon-button:active {
  background: var(--color-primary-soft);
}

.json-detail-viewer__icon-button:focus-visible {
  outline: 2px solid var(--color-primary);
  outline-offset: 1px;
  box-shadow: var(--focus-ring);
}

.json-detail-viewer__content {
  min-height: 132px;
  max-height: min(44vh, 460px);
  padding: 12px 14px;
  overflow: auto;
  color: var(--color-text-primary);
  font-family: var(--font-mono);
  font-size: 12px;
  scrollbar-gutter: stable;
}

.json-detail-viewer__plain-text,
.json-detail-viewer__raw-text {
  margin: 0;
  color: #444441;
  line-height: 1.7;
  overflow-wrap: anywhere;
  white-space: pre-wrap;
}

.json-detail-viewer__empty {
  min-height: 106px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--color-text-placeholder);
  font-family: inherit;
  font-size: 13px;
}

@media (max-width: 720px) {
  .json-detail-viewer__toolbar {
    align-items: flex-start;
    flex-direction: column;
    padding: 8px;
  }

  .json-detail-viewer__actions {
    width: 100%;
    flex-wrap: wrap;
  }

  .json-detail-viewer__content {
    max-height: 52vh;
    padding: 10px;
  }
}
</style>
