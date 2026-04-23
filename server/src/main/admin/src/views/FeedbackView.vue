<template>
  <div class="page-card">
    <div class="page-toolbar">
      <div class="page-toolbar__filters">
        <el-input
          v-model.trim="filters.keyword"
          clearable
          placeholder="搜索反馈说明、traceId、sessionId、设备或机构"
          class="search-input"
          @keyup.enter.native="handleSearch"
        />
        <el-select
          v-model="filters.score"
          clearable
          placeholder="评分"
          class="filter-select"
        >
          <el-option v-for="value in scoreOptions" :key="value" :label="`${value} 分`" :value="value" />
        </el-select>
        <el-select
          v-model="filters.sourceModule"
          clearable
          filterable
          placeholder="来源模块"
          class="filter-select"
        >
          <el-option
            v-for="item in sourceModuleOptions"
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

    <el-table :data="records" border stripe v-loading="loading">
      <el-table-column label="评分" width="90">
        <template slot-scope="{ row }">
          <el-tag size="mini" :type="scoreTagType(row.score)">
            {{ row.score || '--' }} 分
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="反馈说明" min-width="280" show-overflow-tooltip>
        <template slot-scope="{ row }">
          {{ displayText(row.comment) }}
        </template>
      </el-table-column>
      <el-table-column label="来源模块" min-width="120" show-overflow-tooltip>
        <template slot-scope="{ row }">
          {{ moduleLabel(row.sourceModule) }}
        </template>
      </el-table-column>
      <el-table-column label="traceId" min-width="180" show-overflow-tooltip>
        <template slot-scope="{ row }">
          {{ displayText(row.traceId) }}
        </template>
      </el-table-column>
      <el-table-column label="截图" width="90">
        <template slot-scope="{ row }">
          <el-tag size="mini" :type="row.hasScreenshot ? 'success' : 'info'">
            {{ row.hasScreenshot ? '已上传' : '无' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="时间" min-width="180">
        <template slot-scope="{ row }">
          {{ formatDateTime(row.createdAt) }}
        </template>
      </el-table-column>
      <el-table-column label="操作" width="100" fixed="right">
        <template slot-scope="{ row }">
          <el-button type="text" size="mini" @click="openDetail(row)">查看详情</el-button>
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

    <el-dialog title="反馈详情" :visible.sync="detailDialogVisible" width="1000px">
      <div v-if="detailData" class="detail-layout">
        <div class="detail-column">
          <div class="detail-section">
            <div class="section-title">反馈内容</div>
            <div class="detail-grid">
              <div class="detail-card">
                <div class="detail-card__label">评分</div>
                <div class="detail-card__value">{{ detailData.feedback.score }} 分</div>
              </div>
              <div class="detail-card">
                <div class="detail-card__label">来源模块</div>
                <div class="detail-card__value">{{ moduleLabel(detailData.feedback.sourceModule) }}</div>
              </div>
              <div class="detail-card">
                <div class="detail-card__label">traceId</div>
                <div class="detail-card__value">{{ displayText(detailData.feedback.traceId) }}</div>
              </div>
              <div class="detail-card">
                <div class="detail-card__label">会话 ID</div>
                <div class="detail-card__value">{{ displayText(detailData.feedback.sessionId) }}</div>
              </div>
            </div>
            <div class="comment-block">{{ detailData.feedback.comment || '--' }}</div>
          </div>

          <div class="detail-section" v-if="detailData.feedback.screenshotDataUrl">
            <div class="section-title">截图</div>
            <img class="preview-image" :src="detailData.feedback.screenshotDataUrl" :alt="detailData.feedback.screenshotFileName || '反馈截图'" />
          </div>

          <div class="detail-section">
            <div class="section-title">链路上下文快照</div>
            <pre class="payload-block">{{ formatPayload(detailData.feedback.chainContext) }}</pre>
          </div>
        </div>

        <div class="detail-column">
          <div class="detail-section">
            <div class="section-title">调用链路时间线</div>
            <div v-if="detailData.timeline && detailData.timeline.length" class="timeline-list">
              <div v-for="(item, index) in detailData.timeline" :key="`${item.time}-${index}`" class="timeline-item">
                <div class="timeline-item__meta">
                  <el-tag size="mini" :type="timelineType(item.type)">{{ timelineTypeLabel(item.type) }}</el-tag>
                  <span>{{ formatDateTime(item.time) }}</span>
                  <span :class="['timeline-result', item.result]">{{ normalizeResult(item.result) }}</span>
                </div>
                <div class="timeline-item__title">{{ timelineTitle(item) }}</div>
                <pre class="payload-block small">{{ formatPayload(item.payload) }}</pre>
              </div>
            </div>
            <div v-else class="empty-state">暂无可关联的调用链路记录</div>
          </div>
        </div>
      </div>
      <span slot="footer">
        <el-button @click="detailDialogVisible = false">关闭</el-button>
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

const TIMELINE_TYPE_LABELS = {
  feedback: '反馈提交',
  operation: '操作日志',
  metric: '指标日志',
  session: '会话日志',
  ai_proxy: 'AI 代理调用',
  speech_proxy: '语音代理调用'
}

const SOURCE_MODULE_OPTIONS = [
  { value: 'feedback', label: MODULE_LABELS.feedback },
  { value: 'settings_feedback', label: MODULE_LABELS.settings_feedback },
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
    score: '',
    sourceModule: '',
    dateRange: []
  }
}

export default {
  data() {
    return {
      loading: false,
      detailLoading: false,
      current: 1,
      size: 10,
      total: 0,
      records: [],
      filters: createDefaultFilters(),
      scoreOptions: [1, 2, 3, 4, 5],
      sourceModuleOptions: SOURCE_MODULE_OPTIONS,
      detailDialogVisible: false,
      detailData: null
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
        const data = await http.get('/admin/api/feedbacks', {
          params: {
            current: this.current,
            size: this.size,
            keyword: this.filters.keyword || undefined,
            score: this.filters.score || undefined,
            sourceModule: this.filters.sourceModule || undefined,
            dateFrom: dateRange[0] || undefined,
            dateTo: dateRange[1] || undefined
          }
        })
        this.records = data.records || []
        this.total = data.total || 0
      } catch (error) {
        this.$message.error(error.message || '加载反馈列表失败')
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
    async openDetail(row) {
      this.detailLoading = true
      this.detailDialogVisible = true
      this.detailData = null
      try {
        this.detailData = await http.get(`/admin/api/feedbacks/${row.feedbackId}`)
      } catch (error) {
        this.$message.error(error.message || '加载反馈详情失败')
        this.detailDialogVisible = false
      } finally {
        this.detailLoading = false
      }
    },
    displayText(value) {
      if (value === null || value === undefined) {
        return '--'
      }
      const text = String(value).trim()
      return text || '--'
    },
    moduleLabel(value) {
      const text = this.displayText(value)
      if (text === '--') {
        return text
      }
      return MODULE_LABELS[text] || text
    },
    formatDateTime(value) {
      const text = this.displayText(value)
      if (text === '--') {
        return text
      }
      return text.replace('T', ' ').replace(/\.\d+$/, '')
    },
    scoreTagType(score) {
      if (Number(score) >= 4) {
        return 'success'
      }
      if (Number(score) === 3) {
        return 'warning'
      }
      return 'danger'
    },
    formatPayload(value) {
      if (!value) {
        return '无数据'
      }
      try {
        return JSON.stringify(value, null, 2)
      } catch (error) {
        return String(value)
      }
    },
    timelineType(type) {
      const normalized = String(type || '').toLowerCase()
      if (normalized === 'feedback') {
        return 'warning'
      }
      if (normalized.indexOf('speech') > -1) {
        return 'info'
      }
      if (normalized.indexOf('ai') > -1) {
        return 'danger'
      }
      return 'success'
    },
    timelineTypeLabel(type) {
      const text = this.displayText(type)
      if (text === '--') {
        return text
      }
      return TIMELINE_TYPE_LABELS[text] || this.moduleLabel(text)
    },
    timelineTitle(item) {
      if (!item) {
        return '--'
      }
      const payload = item.payload || {}
      const explicitTitle = this.displayText(item.title)
      const payloadTitle = this.displayText(payload.title || payload.operationName || payload.action)
      const sourceModule = this.moduleLabel(payload.sourceModule || payload.module)
      const traceId = this.displayText(payload.traceId)
      const parts = []

      if (explicitTitle !== '--') {
        parts.push(explicitTitle)
      } else {
        const typeLabel = this.timelineTypeLabel(item.type)
        if (typeLabel !== '--') {
          parts.push(typeLabel)
        }
      }

      if (sourceModule !== '--') {
        parts.push(sourceModule)
      }
      if (payloadTitle !== '--' && payloadTitle !== explicitTitle) {
        parts.push(payloadTitle)
      }
      if (traceId !== '--') {
        parts.push(`traceId: ${traceId}`)
      }

      return parts.length ? parts.join(' / ') : '--'
    },
    normalizeResult(value) {
      const normalized = String(value || '').toLowerCase()
      if (normalized === 'success' || normalized === '1') {
        return '成功'
      }
      if (normalized === 'failure' || normalized === '0') {
        return '失败'
      }
      return value || '--'
    }
  }
}
</script>

<style scoped>
.detail-layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(0, 1fr);
  gap: 20px;
}

.detail-column {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.detail-section {
  padding: 16px;
  border: 1px solid #ebeef5;
  border-radius: 12px;
  background: #fff;
}

.section-title {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 12px;
}

.detail-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.detail-card {
  padding: 12px;
  border-radius: 10px;
  background: #f8fafc;
}

.detail-card__label {
  font-size: 12px;
  color: #909399;
  margin-bottom: 4px;
}

.detail-card__value {
  font-size: 13px;
  color: #303133;
  word-break: break-all;
}

.comment-block {
  margin-top: 12px;
  padding: 12px;
  border-radius: 10px;
  background: #f8fafc;
  line-height: 1.7;
  color: #303133;
  white-space: pre-wrap;
}

.preview-image {
  display: block;
  width: 100%;
  max-height: 360px;
  object-fit: contain;
  border-radius: 10px;
  background: #f5f7fa;
}

.timeline-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.timeline-item {
  padding: 14px;
  border: 1px solid #ebeef5;
  border-radius: 10px;
  background: #fafcff;
}

.timeline-item__meta {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 8px;
  font-size: 12px;
  color: #909399;
}

.timeline-item__title {
  font-size: 14px;
  color: #303133;
  margin-bottom: 8px;
}

.timeline-result.success {
  color: #67c23a;
}

.timeline-result.failure {
  color: #f56c6c;
}

.payload-block {
  margin: 0;
  padding: 12px;
  border-radius: 10px;
  background: #0f172a;
  color: #e2e8f0;
  font-size: 12px;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-all;
}

.payload-block.small {
  max-height: 220px;
  overflow: auto;
}

@media (max-width: 1200px) {
  .detail-layout {
    grid-template-columns: 1fr;
  }
}
</style>
