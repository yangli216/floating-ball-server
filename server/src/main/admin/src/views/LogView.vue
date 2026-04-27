<template>
  <div>
    <div class="filter-bar">
      <div class="page-toolbar__filters">
        <el-input
          v-model.trim="filters.keyword"
          clearable
          placeholder="搜索模块、类型、描述、原始数据、设备或机构"
          class="search-input"
          @keyup.enter.native="handleSearch"
        />
        <el-select
          v-model="filters.logType"
          clearable
          placeholder="日志类型"
          class="filter-select"
        >
          <el-option
            v-for="item in logTypeOptions"
            :key="item.value"
            :label="item.label"
            :value="item.value"
          />
        </el-select>
        <el-select
          v-model="filters.module"
          clearable
          filterable
          placeholder="模块名称"
          class="filter-select"
        >
          <el-option
            v-for="item in moduleOptions"
            :key="item.value"
            :label="item.label"
            :value="item.value"
          />
        </el-select>
        <el-select
          v-model="filters.result"
          clearable
          placeholder="结果"
          class="filter-select"
        >
          <el-option
            v-for="item in resultOptions"
            :key="item.value"
            :label="item.label"
            :value="item.value"
          />
        </el-select>
        <el-date-picker
          v-model="filters.dateRange"
          type="datetimerange"
          clearable
          unlink-panels
          range-separator="至"
          start-placeholder="开始时间"
          end-placeholder="结束时间"
          value-format="yyyy-MM-dd HH:mm:ss"
          class="filter-date"
        />
        <el-button type="primary" icon="el-icon-search" @click="handleSearch">查询</el-button>
        <el-button @click="reset">重置</el-button>
      </div>
    </div>

    <div class="page-card">
      <el-table :data="records" v-loading="loading">
      <el-table-column label="日志类型" width="112">
        <template slot-scope="{ row }">
          <el-tag size="mini" :type="logTypeMeta(row.sdLogType).type">
            {{ logTypeMeta(row.sdLogType).label }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="模块" min-width="120" show-overflow-tooltip>
        <template slot-scope="{ row }">
          {{ moduleLabel(row.naModule) }}
        </template>
      </el-table-column>
      <el-table-column label="操作描述" min-width="170" show-overflow-tooltip>
        <template slot-scope="{ row }">
          {{ displayText(row.desOp) }}
        </template>
      </el-table-column>
      <el-table-column label="结果" width="88">
        <template slot-scope="{ row }">
          <el-tag size="mini" :type="resultMeta(row.opResult).type">
            {{ resultMeta(row.opResult).label }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="原始数据" min-width="220" show-overflow-tooltip>
        <template slot-scope="{ row }">
          <span class="code-tag log-summary">{{ summarizeRawData(row.payloadJson) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作时间" width="168">
        <template slot-scope="{ row }">
          {{ formatDateTime(row.operationTime) }}
        </template>
      </el-table-column>
      <el-table-column label="操作" width="88" fixed="right">
        <template slot-scope="{ row }">
          <a class="table-action" @click="openRawData(row)">详情</a>
        </template>
      </el-table-column>
    </el-table>

    <div class="page-footer">
      <el-pagination
        background
        layout="total, prev, pager, next"
        :current-page.sync="current"
        :page-size="size"
        :total="total"
        @current-change="loadData"
      />
    </div>
    </div>

    <el-dialog v-if="payloadDialogVisible" title="日志详情" :visible.sync="payloadDialogVisible" width="760px">
      <div v-if="payloadRecord" class="detail-grid">
        <div class="detail-card">
          <div class="detail-card__label">日志类型</div>
          <div class="detail-card__value">{{ logTypeMeta(payloadRecord.sdLogType).label }}</div>
        </div>
        <div class="detail-card">
          <div class="detail-card__label">模块</div>
          <div class="detail-card__value">{{ moduleLabel(payloadRecord.naModule) }}</div>
        </div>
        <div class="detail-card">
          <div class="detail-card__label">结果</div>
          <div class="detail-card__value">{{ resultMeta(payloadRecord.opResult).label }}</div>
        </div>
        <div class="detail-card">
          <div class="detail-card__label">操作时间</div>
          <div class="detail-card__value">{{ formatDateTime(payloadRecord.operationTime) }}</div>
        </div>
      </div>
      <pre class="code-block">{{ payloadDetailText }}</pre>
      <span slot="footer">
        <el-button @click="payloadDialogVisible = false">关闭</el-button>
      </span>
    </el-dialog>
  </div>
</template>

<script>
import http from '../api/http'

const MODULE_LABELS = {
  feedback: '反馈弹层',
  settings_feedback: '设置页反馈',
  llm: 'AI 对话代理',
  aliyunSpeech: '语音识别代理',
  operation: '操作日志',
  metric: '指标日志',
  session: '会话日志',
  ai_proxy: 'AI 代理日志',
  speech_proxy: '语音代理日志',
  api_call: '接口调用',
  button_click: '按钮点击',
  form_submit: '表单提交',
  error: '异常日志',
  view_change: '视图切换',
  'view:chat': '聊天页',
  'view:settings': '设置页',
  'view:consultation': '智能问诊页',
  'view:risk-alert': '风险提示页',
  'view:voice-interaction': '语音胶囊',
  'view:voice-result': '语音结果页',
  'view:voice-consultation': '语音问诊页',
  'view:reception-capsule': '接待胶囊',
  'view:analytics': '数据分析页',
  'view:symptom-manage': '症状库维护页',
  'view:knowledge-base': '知识库页'
}

const MODULE_OPTIONS = [
  { value: 'feedback', label: MODULE_LABELS.feedback },
  { value: 'settings_feedback', label: MODULE_LABELS.settings_feedback },
  { value: 'llm', label: MODULE_LABELS.llm },
  { value: 'aliyunSpeech', label: MODULE_LABELS.aliyunSpeech },
  { value: 'operation', label: MODULE_LABELS.operation },
  { value: 'metric', label: MODULE_LABELS.metric },
  { value: 'session', label: MODULE_LABELS.session },
  { value: 'ai_proxy', label: MODULE_LABELS.ai_proxy },
  { value: 'speech_proxy', label: MODULE_LABELS.speech_proxy },
  { value: 'api_call', label: MODULE_LABELS.api_call },
  { value: 'button_click', label: MODULE_LABELS.button_click },
  { value: 'form_submit', label: MODULE_LABELS.form_submit },
  { value: 'error', label: MODULE_LABELS.error },
  { value: 'view_change', label: MODULE_LABELS.view_change },
  { value: 'view:chat', label: MODULE_LABELS['view:chat'] },
  { value: 'view:settings', label: MODULE_LABELS['view:settings'] },
  { value: 'view:consultation', label: MODULE_LABELS['view:consultation'] },
  { value: 'view:risk-alert', label: MODULE_LABELS['view:risk-alert'] },
  { value: 'view:voice-interaction', label: MODULE_LABELS['view:voice-interaction'] },
  { value: 'view:voice-result', label: MODULE_LABELS['view:voice-result'] },
  { value: 'view:voice-consultation', label: MODULE_LABELS['view:voice-consultation'] },
  { value: 'view:reception-capsule', label: MODULE_LABELS['view:reception-capsule'] },
  { value: 'view:analytics', label: MODULE_LABELS['view:analytics'] },
  { value: 'view:symptom-manage', label: MODULE_LABELS['view:symptom-manage'] },
  { value: 'view:knowledge-base', label: MODULE_LABELS['view:knowledge-base'] }
]

function createDefaultFilters() {
  return {
    keyword: '',
    logType: '',
    module: '',
    result: '',
    dateRange: []
  }
}

export default {
  data() {
    return {
      loading: false,
      current: 1,
      size: 10,
      total: 0,
      records: [],
      filters: createDefaultFilters(),
      payloadDialogVisible: false,
      payloadRecord: null,
      payloadDetailText: '无原始数据',
      moduleOptions: MODULE_OPTIONS,
      logTypeOptions: [
        { value: 'operation', label: '操作日志', type: 'info' },
        { value: 'feedback', label: '反馈日志', type: 'warning' },
        { value: 'metric', label: '指标日志', type: 'success' },
        { value: 'session', label: '会话日志', type: 'info' },
        { value: 'ai_proxy', label: 'AI 代理', type: 'danger' },
        { value: 'speech_proxy', label: '语音代理', type: 'warning' }
      ],
      resultOptions: [
        { value: 'success', label: '成功' },
        { value: 'failure', label: '失败' }
      ]
    }
  },
  mounted() {
    this.loadData()
  },
  methods: {
    async loadData() {
      this.loading = true
      try {
        const dateRange = Array.isArray(this.filters.dateRange) ? this.filters.dateRange : []
        const data = await http.get('/admin/api/logs', {
          params: {
            current: this.current,
            size: this.size,
            keyword: this.filters.keyword || undefined,
            logType: this.filters.logType || undefined,
            module: this.filters.module || undefined,
            result: this.filters.result || undefined,
            dateFrom: dateRange[0] || undefined,
            dateTo: dateRange[1] || undefined
          }
        })
        this.records = data.records || []
        this.total = data.total || 0
      } catch (error) {
        this.$message.error(error.message || '加载失败')
      } finally {
        this.loading = false
      }
    },
    handleSearch() {
      this.current = 1
      this.loadData()
    },
    reset() {
      this.filters = createDefaultFilters()
      this.current = 1
      this.loadData()
    },
    openRawData(row) {
      this.payloadRecord = row
      this.payloadDetailText = this.formatRawDataDetail(row.payloadJson)
      this.payloadDialogVisible = true
    },
    normalizeText(value) {
      if (value === null || value === undefined) {
        return ''
      }
      const text = String(value).trim()
      if (!text || text === 'null' || text === 'undefined') {
        return ''
      }
      return text
    },
    displayText(value) {
      return this.normalizeText(value) || '--'
    },
    moduleLabel(value) {
      const text = this.displayText(value)
      if (text === '--') {
        return text
      }
      return MODULE_LABELS[text] || text
    },
    formatDateTime(value) {
      const text = this.normalizeText(value)
      if (!text) {
        return '--'
      }
      return text.replace('T', ' ').replace(/\.\d+$/, '')
    },
    logTypeMeta(value) {
      const text = this.normalizeText(value)
      const matched = this.logTypeOptions.find(item => item.value === text)
      if (matched) {
        return matched
      }
      return {
        label: text || '--',
        type: 'info'
      }
    },
    resultMeta(value) {
      const text = this.normalizeText(value)
      const normalized = text.toLowerCase()
      if (normalized === '1' || normalized === 'true' || normalized === 'success' || normalized === 'ok') {
        return { label: '成功', type: 'success' }
      }
      if (normalized === '0' || normalized === 'false' || normalized === 'fail' || normalized === 'failure' || normalized === 'error') {
        return { label: '失败', type: 'danger' }
      }
      if (!text) {
        return { label: '--', type: 'info' }
      }
      return { label: text, type: 'info' }
    },
    summarizeRawData(value) {
      const text = this.normalizeText(value)
      if (!text) {
        return '无原始数据'
      }
      try {
        const payload = JSON.parse(text)
        const summaryParts = []
        const moduleName = this.moduleLabel(payload.module || payload.sourceModule)
        const action = this.normalizeText(payload.action)
        const operationName = this.normalizeText(payload.operationName)
        const responseText = this.normalizeText(payload.responseText)
        const errorMessage = this.normalizeText(payload.errorMessage)
        if (moduleName) {
          summaryParts.push(moduleName)
        }
        if (action) {
          summaryParts.push(action)
        } else if (operationName) {
          summaryParts.push(operationName)
        }
        if (errorMessage) {
          summaryParts.push(`失败: ${this.truncate(errorMessage, 40)}`)
        } else if (responseText) {
          summaryParts.push(`回文: ${this.truncate(responseText, 40)}`)
        }
        if (summaryParts.length > 0) {
          return summaryParts.join(' / ')
        }
        return this.truncate(JSON.stringify(payload), 100)
      } catch (error) {
        return this.truncate(text.replace(/\s+/g, ' '), 100)
      }
    },
    formatRawDataDetail(value) {
      const text = this.normalizeText(value)
      if (!text) {
        return '无原始数据'
      }
      try {
        return JSON.stringify(JSON.parse(text), null, 2)
      } catch (error) {
        return text
      }
    },
    truncate(value, maxLength) {
      if (!value || value.length <= maxLength) {
        return value
      }
      return `${value.slice(0, maxLength)}...`
    }
  }
}
</script>

<style scoped>
.search-input {
  width: 300px;
}

.filter-select {
  width: 150px;
}

.filter-date {
  width: 340px;
}

.log-summary {
  max-width: 100%;
  display: inline-block;
  overflow: hidden;
  text-overflow: ellipsis;
  vertical-align: middle;
  white-space: nowrap;
}

.detail-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(140px, 1fr));
  gap: 12px;
  margin-bottom: 16px;
}

.detail-card {
  padding: 12px;
  border-radius: 10px;
  background: #F9FCFB;
  border: 0.5px solid #E8EEEC;
}

.detail-card__label {
  margin-bottom: 6px;
  font-size: 12px;
  color: #888780;
}

.detail-card__value {
  color: #2C2C2A;
  font-weight: 500;
  word-break: break-all;
}

.code-block {
  margin: 0;
  padding: 14px;
  max-height: 420px;
  overflow: auto;
  border-radius: 10px;
  background: #F1EFE8;
  color: #444441;
  font-size: 12px;
  line-height: 1.6;
  font-family: var(--font-mono, SFMono-Regular, Menlo, Consolas, monospace);
}

@media (max-width: 960px) {
  .search-input,
  .filter-select,
  .filter-date {
    width: 100%;
  }
}
</style>
