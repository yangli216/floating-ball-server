<template>
  <div class="sec-analytics-page" v-loading="loading">
    <div class="filter-bar">
      <div class="filter-row">
        <div class="filter-item">
          <div class="filter-label">时间范围</div>
          <div class="time-tabs">
            <button
              v-for="opt in timeRangeOptions"
              :key="opt.value"
              type="button"
              :class="['time-tab', { 'is-active': timeRange === opt.value }]"
              @click="setTimeRange(opt.value)"
            >{{ opt.label }}</button>
          </div>
          <div v-if="timeRange === 'custom'" class="custom-date-row">
            <el-date-picker
              v-model="query.dateFrom"
              type="date"
              placeholder="开始日期"
              size="small"
              value-format="yyyy-MM-dd"
              @change="onCustomDateChange"
            />
            <span class="date-sep">至</span>
            <el-date-picker
              v-model="query.dateTo"
              type="date"
              placeholder="结束日期"
              size="small"
              value-format="yyyy-MM-dd"
              @change="onCustomDateChange"
            />
          </div>
        </div>
        <div class="filter-actions">
          <el-button type="primary" size="small" @click="search">查询</el-button>
          <el-button size="small" @click="reset">重置</el-button>
        </div>
      </div>
    </div>

    <div class="card-grid">
      <div
        v-for="card in cards"
        :key="card.key"
        class="stat-card"
      >
        <div class="stat-card__label">{{ card.label }}</div>
        <div class="stat-card__value">{{ card.value }}</div>
        <div :class="['stat-card__growth', card.growthUp ? 'is-up' : 'is-down']">
          <span class="growth-icon">{{ card.growthUp ? '▲' : '▼' }}</span>
          <span>{{ card.growthText }}</span>
        </div>
        <div class="stat-card__desc">{{ card.desc }}</div>
      </div>
    </div>

    <div class="chart-section">
      <div class="chart-card">
        <div class="chart-card__header">
          <span class="chart-card__title">拦截趋势分析</span>
          <el-select v-model="trendMetric" size="small" class="trend-select">
            <el-option label="全部拦截" value="total" />
            <el-option label="认证拦截" value="auth" />
            <el-option label="签名拦截" value="sig" />
          </el-select>
        </div>
        <div ref="trendChartRef" class="chart-body"></div>
      </div>
    </div>

    <div class="chart-section chart-section--double">
      <div class="chart-card">
        <div class="chart-card__header">
          <span class="chart-card__title">拦截类型分布</span>
          <span class="chart-card__subtitle">各类拦截占比</span>
        </div>
        <div ref="typeChartRef" class="chart-body"></div>
      </div>
      <div class="chart-card">
        <div class="chart-card__header">
          <span class="chart-card__title">高危 IP 排行</span>
          <span class="chart-card__subtitle">拦截次数 Top 10</span>
        </div>
        <div ref="ipChartRef" class="chart-body"></div>
      </div>
    </div>

    <div class="chart-section chart-section--double">
      <div class="chart-card">
        <div class="chart-card__header">
          <span class="chart-card__title">被攻击路径排行</span>
          <span class="chart-card__subtitle">拦截次数 Top 10</span>
        </div>
        <div ref="pathChartRef" class="chart-body"></div>
      </div>
      <div class="chart-card">
        <div class="chart-card__header">
          <span class="chart-card__title">异常设备排行</span>
          <span class="chart-card__subtitle">拦截次数 Top 10</span>
        </div>
        <div ref="deviceChartRef" class="chart-body"></div>
      </div>
    </div>
  </div>
</template>

<script>
import * as echarts from 'echarts'
import http from '../api/http'

const TIME_RANGES = [
  { value: 'today', label: '今日' },
  { value: 'week', label: '本周' },
  { value: 'month', label: '本月' },
  { value: 'quarter', label: '本季度' },
  { value: 'year', label: '本年' },
  { value: 'custom', label: '自定义' }
]

const TYPE_LABEL_MAP = {
  AUTH_MISSING_TOKEN: '缺少令牌',
  AUTH_INVALID_TOKEN: '令牌无效',
  SIG_MISSING: '缺少签名',
  SIG_INVALID: '签名无效',
  SIG_NO_PUBLIC_KEY: '未注册公钥',
  VERSION_OUTDATED: '版本过低',
  WS_AUTH_MISSING_TOKEN: 'WS缺少令牌',
  WS_AUTH_INVALID_TOKEN: 'WS令牌无效',
  WS_SIG_MISSING: 'WS缺少签名',
  WS_SIG_INVALID: 'WS签名无效',
  WS_SIG_NO_PUBLIC_KEY: 'WS未注册公钥'
}

const TYPE_COLORS = {
  AUTH_MISSING_TOKEN: '#EF4444',
  AUTH_INVALID_TOKEN: '#DC2626',
  SIG_MISSING: '#F59E0B',
  SIG_INVALID: '#F97316',
  SIG_NO_PUBLIC_KEY: '#EAB308',
  VERSION_OUTDATED: '#3B82F6',
  WS_AUTH_MISSING_TOKEN: '#8B5CF6',
  WS_AUTH_INVALID_TOKEN: '#7C3AED',
  WS_SIG_MISSING: '#A855F7',
  WS_SIG_INVALID: '#C026D3',
  WS_SIG_NO_PUBLIC_KEY: '#D946EF'
}

const CARD_DEFS = [
  { key: 'totalRejections', label: '拦截总数', desc: '所选时间段内拦截请求总数' },
  { key: 'authRejections', label: '认证拦截', desc: '令牌缺失或无效的拦截次数' },
  { key: 'sigRejections', label: '签名拦截', desc: '签名缺失或无效的拦截次数' },
  { key: 'wsRejections', label: 'WebSocket拦截', desc: 'WebSocket握手拦截次数' },
  { key: 'recent24h', label: '近24小时', desc: '最近24小时拦截次数' },
  { key: 'recent1h', label: '近1小时', desc: '最近1小时拦截次数' }
]

const GROWTH_KEY_MAP = {
  totalRejections: 'totalGrowth',
  authRejections: 'authGrowth',
  sigRejections: 'sigGrowth',
  wsRejections: null,
  recent24h: null,
  recent1h: null
}

const COLORS = ['#EF4444', '#F59E0B', '#3B82F6', '#10B981', '#8B5CF6', '#EC4899', '#F97316', '#06B6D4', '#84CC16', '#6366F1']

export default {
  data() {
    return {
      loading: false,
      timeRange: 'month',
      timeRangeOptions: TIME_RANGES,
      query: { dateFrom: '', dateTo: '' },
      summary: {},
      trendData: { days: [], totalValues: [], authValues: [], sigValues: [] },
      distribution: { byType: [], byIp: [], byPath: [], byDevice: [], totalRejections: 0 },
      trendMetric: 'total',
      trendChart: null,
      typeChart: null,
      ipChart: null,
      pathChart: null,
      deviceChart: null
    }
  },
  computed: {
    cardCompareLabel() {
      const m = {
        today: '较昨日',
        week: '较上周',
        month: '较上月',
        quarter: '较上季度',
        year: '较上年',
        custom: '较上周期'
      }
      return m[this.timeRange] || '较上月'
    },
    cards() {
      const s = this.summary || {}
      const label = this.cardCompareLabel
      return CARD_DEFS.map(def => {
        let value = s[def.key]
        if (value == null) value = 0
        const growthKey = GROWTH_KEY_MAP[def.key]
        const growthVal = growthKey ? (s[growthKey] || '0.0%') : null
        let growthUp = true
        let growthText = ''
        if (growthVal !== null) {
          const num = parseFloat(growthVal)
          growthUp = !isNaN(num) && num >= 0
          growthText = `${label}${growthUp ? '增长' : '下降'} ${Math.abs(num).toFixed(1)}%`
        } else {
          growthText = '实时统计'
        }
        return { key: def.key, label: def.label, value, desc: def.desc, growthUp, growthText }
      })
    }
  },
  mounted() {
    this.initDateRange()
    this.search()
  },
  beforeDestroy() {
    this.disposeCharts()
  },
  watch: {
    trendMetric() {
      this.renderTrendChart()
    }
  },
  methods: {
    initDateRange() {
      const now = new Date()
      let from, to
      switch (this.timeRange) {
        case 'today':
          from = to = this.fmt(now)
          break
        case 'week': {
          const d = now.getDay() || 7
          from = this.fmt(new Date(now.getFullYear(), now.getMonth(), now.getDate() - d + 1))
          to = this.fmt(now)
          break
        }
        case 'month':
          from = this.fmt(new Date(now.getFullYear(), now.getMonth(), 1))
          to = this.fmt(now)
          break
        case 'quarter': {
          const qs = Math.floor(now.getMonth() / 3) * 3
          from = this.fmt(new Date(now.getFullYear(), qs, 1))
          to = this.fmt(now)
          break
        }
        case 'year':
          from = this.fmt(new Date(now.getFullYear(), 0, 1))
          to = this.fmt(now)
          break
        default:
          from = this.fmt(new Date(now.getFullYear(), now.getMonth(), 1))
          to = this.fmt(now)
      }
      this.query.dateFrom = from
      this.query.dateTo = to
    },
    fmt(d) {
      const y = d.getFullYear()
      const m = String(d.getMonth() + 1).padStart(2, '0')
      const day = String(d.getDate()).padStart(2, '0')
      return `${y}-${m}-${day}`
    },
    setTimeRange(val) {
      this.timeRange = val
      this.initDateRange()
      this.search()
    },
    onCustomDateChange() {
      if (this.query.dateFrom && this.query.dateTo) {
        this.search()
      }
    },
    async search() {
      this.loading = true
      try {
        const params = { ...this.query, timeRange: this.timeRange }

        const [summary, trend, distribution] = await Promise.all([
          http.get('/admin/api/security/rejections/summary', { params }),
          http.get('/admin/api/security/rejections/trend', { params }),
          http.get('/admin/api/security/rejections/distribution', { params })
        ])
        this.summary = summary || {}
        this.trendData = trend || { days: [], totalValues: [], authValues: [], sigValues: [] }
        this.distribution = distribution || { byType: [], byIp: [], byPath: [], byDevice: [], totalRejections: 0 }

        this.$nextTick(() => {
          this.renderTrendChart()
          this.renderTypeChart()
          this.renderIpChart()
          this.renderPathChart()
          this.renderDeviceChart()
        })
      } catch (error) {
        this.$message.error((error && error.message) || '加载失败')
      } finally {
        this.loading = false
      }
    },
    reset() {
      this.timeRange = 'month'
      this.initDateRange()
      this.search()
    },
    disposeCharts() {
      if (this.trendChart) { this.trendChart.dispose(); this.trendChart = null }
      if (this.typeChart) { this.typeChart.dispose(); this.typeChart = null }
      if (this.ipChart) { this.ipChart.dispose(); this.ipChart = null }
      if (this.pathChart) { this.pathChart.dispose(); this.pathChart = null }
      if (this.deviceChart) { this.deviceChart.dispose(); this.deviceChart = null }
      if (this._resizeHandler) {
        window.removeEventListener('resize', this._resizeHandler)
        this._resizeHandler = null
      }
    },
    bindResize() {
      if (!this._resizeHandler) {
        this._resizeHandler = () => {
          [this.trendChart, this.typeChart, this.ipChart, this.pathChart, this.deviceChart].forEach(c => {
            if (c) c.resize()
          })
        }
        window.addEventListener('resize', this._resizeHandler)
      }
    },
    renderTrendChart() {
      if (!this.$refs.trendChartRef) return
      if (this.trendChart) this.trendChart.dispose()
      this.trendChart = echarts.init(this.$refs.trendChartRef)

      const days = this.trendData.days || []
      let values, color, areaColor
      switch (this.trendMetric) {
        case 'auth':
          values = this.trendData.authValues || []
          color = '#F59E0B'
          areaColor = 'rgba(245,158,11,0.15)'
          break
        case 'sig':
          values = this.trendData.sigValues || []
          color = '#EF4444'
          areaColor = 'rgba(239,68,68,0.15)'
          break
        default:
          values = this.trendData.totalValues || []
          color = '#3B82F6'
          areaColor = 'rgba(59,130,246,0.15)'
      }

      this.trendChart.setOption({
        tooltip: { trigger: 'axis' },
        grid: { left: 50, right: 20, top: 20, bottom: 30 },
        xAxis: {
          type: 'category',
          data: days,
          axisLine: { lineStyle: { color: '#E2E8F0' } },
          axisLabel: { color: '#94A3B8', fontSize: 12 }
        },
        yAxis: {
          type: 'value',
          minInterval: 1,
          splitLine: { lineStyle: { color: '#F1F5F9' } },
          axisLabel: { color: '#94A3B8', fontSize: 12 }
        },
        series: [{
          type: 'line',
          data: values,
          smooth: true,
          symbol: 'circle',
          symbolSize: 6,
          lineStyle: { color, width: 2 },
          itemStyle: { color },
          areaStyle: {
            color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
              { offset: 0, color: areaColor },
              { offset: 1, color: 'rgba(0,0,0,0)' }
            ])
          }
        }]
      })
      this.bindResize()
    },
    renderTypeChart() {
      if (!this.$refs.typeChartRef) return
      if (this.typeChart) this.typeChart.dispose()
      this.typeChart = echarts.init(this.$refs.typeChartRef)

      const data = this.distribution.byType || []
      const total = this.distribution.totalRejections || 0
      this.typeChart.setOption({
        tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
        series: [{
          type: 'pie',
          radius: ['50%', '75%'],
          center: ['50%', '45%'],
          avoidLabelOverlap: false,
          itemStyle: { borderColor: '#fff', borderWidth: 2 },
          label: { show: false },
          emphasis: { label: { show: true, fontSize: 14, fontWeight: 'bold' } },
          data: data.map(d => ({
            value: d.value,
            name: TYPE_LABEL_MAP[d.name] || d.name,
            itemStyle: { color: TYPE_COLORS[d.name] || '#94A3B8' }
          }))
        }],
        graphic: [
          {
            type: 'text',
            left: 'center',
            top: '38%',
            style: {
              text: (total || 0).toLocaleString(),
              textAlign: 'center',
              fill: '#1E293B',
              fontSize: 20,
              fontWeight: 'bold'
            }
          },
          {
            type: 'text',
            left: 'center',
            top: '50%',
            style: {
              text: '拦截总数',
              textAlign: 'center',
              fill: '#94A3B8',
              fontSize: 12
            }
          }
        ]
      })
      this.bindResize()
    },
    renderIpChart() {
      if (!this.$refs.ipChartRef) return
      if (this.ipChart) this.ipChart.dispose()
      this.ipChart = echarts.init(this.$refs.ipChartRef)

      const data = (this.distribution.byIp || []).slice(0, 10)
      this.ipChart.setOption({
        tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
        grid: { left: 60, right: 20, top: 20, bottom: 50 },
        xAxis: {
          type: 'category',
          data: data.map(d => d.name),
          axisLine: { lineStyle: { color: '#E2E8F0' } },
          axisLabel: { color: '#94A3B8', fontSize: 11, rotate: 30 }
        },
        yAxis: {
          type: 'value',
          minInterval: 1,
          splitLine: { lineStyle: { color: '#F1F5F9' } },
          axisLabel: { color: '#94A3B8', fontSize: 12 }
        },
        series: [{
          type: 'bar',
          data: data.map(d => d.value),
          barWidth: 36,
          itemStyle: {
            color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
              { offset: 0, color: '#F87171' },
              { offset: 1, color: '#EF4444' }
            ]),
            borderRadius: [4, 4, 0, 0]
          },
          label: { show: true, position: 'top', color: '#475569', fontSize: 11 }
        }]
      })
      this.bindResize()
    },
    renderPathChart() {
      if (!this.$refs.pathChartRef) return
      if (this.pathChart) this.pathChart.dispose()
      this.pathChart = echarts.init(this.$refs.pathChartRef)

      const data = (this.distribution.byPath || []).slice(0, 10)
      this.pathChart.setOption({
        tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
        grid: { left: 60, right: 20, top: 20, bottom: 50 },
        xAxis: {
          type: 'category',
          data: data.map(d => d.name),
          axisLine: { lineStyle: { color: '#E2E8F0' } },
          axisLabel: { color: '#94A3B8', fontSize: 11, rotate: 30, formatter: v => v.length > 20 ? v.slice(0, 20) + '...' : v }
        },
        yAxis: {
          type: 'value',
          minInterval: 1,
          splitLine: { lineStyle: { color: '#F1F5F9' } },
          axisLabel: { color: '#94A3B8', fontSize: 12 }
        },
        series: [{
          type: 'bar',
          data: data.map(d => d.value),
          barWidth: 36,
          itemStyle: {
            color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
              { offset: 0, color: '#FBBF24' },
              { offset: 1, color: '#F59E0B' }
            ]),
            borderRadius: [4, 4, 0, 0]
          },
          label: { show: true, position: 'top', color: '#475569', fontSize: 11 }
        }]
      })
      this.bindResize()
    },
    renderDeviceChart() {
      if (!this.$refs.deviceChartRef) return
      if (this.deviceChart) this.deviceChart.dispose()
      this.deviceChart = echarts.init(this.$refs.deviceChartRef)

      const data = (this.distribution.byDevice || []).slice(0, 10)
      this.deviceChart.setOption({
        tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
        grid: { left: 60, right: 20, top: 20, bottom: 50 },
        xAxis: {
          type: 'category',
          data: data.map(d => d.name),
          axisLine: { lineStyle: { color: '#E2E8F0' } },
          axisLabel: { color: '#94A3B8', fontSize: 11, rotate: 30, formatter: v => v.length > 16 ? v.slice(0, 16) + '...' : v }
        },
        yAxis: {
          type: 'value',
          minInterval: 1,
          splitLine: { lineStyle: { color: '#F1F5F9' } },
          axisLabel: { color: '#94A3B8', fontSize: 12 }
        },
        series: [{
          type: 'bar',
          data: data.map(d => d.value),
          barWidth: 36,
          itemStyle: {
            color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
              { offset: 0, color: '#A78BFA' },
              { offset: 1, color: '#8B5CF6' }
            ]),
            borderRadius: [4, 4, 0, 0]
          },
          label: { show: true, position: 'top', color: '#475569', fontSize: 11 }
        }]
      })
      this.bindResize()
    }
  }
}
</script>

<style scoped>
.sec-analytics-page {
  display: grid;
  gap: 16px;
}

.filter-bar {
  background: #fff;
  border-radius: 8px;
  padding: 20px 24px;
  border: 0.5px solid #E8EEEC;
}

.filter-row {
  display: flex;
  align-items: flex-end;
  gap: 20px;
  flex-wrap: wrap;
}

.filter-item {
  display: grid;
  gap: 6px;
}

.filter-label {
  font-size: 14px;
  font-weight: 500;
  color: #4B5563;
}

.time-tabs {
  display: flex;
  border: 0.8px solid #E2E8F0;
  border-radius: 8px;
  overflow: hidden;
}

.time-tab {
  height: 38px;
  padding: 0 16px;
  border: none;
  background: #fff;
  color: #64748B;
  font-size: 14px;
  cursor: pointer;
}

.time-tab.is-active {
  background: #FEF2F2;
  color: #DC2626;
  font-weight: 500;
}

.custom-date-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 8px;
}

.custom-date-row .el-date-editor {
  width: 150px;
}

.date-sep {
  color: #94A3B8;
  font-size: 13px;
  flex-shrink: 0;
}

.filter-actions {
  display: flex;
  gap: 8px;
  align-items: flex-end;
}

.card-grid {
  display: grid;
  grid-template-columns: repeat(6, minmax(0, 1fr));
  gap: 14px;
}

.stat-card {
  padding: 20px;
  background: #fff;
  border: 0.8px solid #F1F5F9;
  border-radius: 12px;
  box-shadow: 0 1px 2px rgba(0,0,0,0.05);
  display: grid;
  gap: 4px;
}

.stat-card__label {
  font-size: 14px;
  color: #64748B;
}

.stat-card__value {
  font-size: 28px;
  font-weight: 600;
  color: #1E293B;
  line-height: 1.2;
  padding: 4px 0;
}

.stat-card__growth {
  font-size: 12px;
  display: flex;
  align-items: center;
  gap: 4px;
}

.stat-card__growth.is-up {
  color: #EF4444;
}

.stat-card__growth.is-down {
  color: #10B981;
}

.growth-icon {
  font-size: 10px;
}

.stat-card__desc {
  font-size: 12px;
  color: #94A3B8;
  margin-top: 8px;
  padding-top: 8px;
  border-top: 0.5px solid #F1F5F9;
}

.chart-section {
  display: grid;
}

.chart-section--double {
  grid-template-columns: 1fr 1fr;
  gap: 14px;
}

.chart-card {
  background: #fff;
  border: 0.8px solid #F1F5F9;
  border-radius: 12px;
  box-shadow: 0 1px 2px rgba(0,0,0,0.05);
  padding: 20px;
}

.chart-card__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.chart-card__title {
  font-size: 16px;
  font-weight: 600;
  color: #1E293B;
}

.chart-card__subtitle {
  font-size: 14px;
  color: #94A3B8;
  margin-left: 10px;
}

.trend-select {
  width: 120px;
}

.chart-body {
  height: 320px;
}

@media (max-width: 960px) {
  .card-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }

  .chart-section--double {
    grid-template-columns: 1fr;
  }
}
</style>
