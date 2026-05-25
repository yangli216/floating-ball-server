<template>
  <div class="page-surface analytics-page" v-loading="loading">
    <admin-filter-bar>
      <div class="filter-row">
        <div class="filter-item">
          <div class="filter-label">时间范围</div>
          <time-range-filter
            v-model="timeRange"
            :options="timeRangeOptions"
            :date-from.sync="query.dateFrom"
            :date-to.sync="query.dateTo"
            @input="setTimeRange"
            @custom-change="onCustomDateChange"
          />
        </div>
        <div class="filter-item">
          <div class="filter-label">区域选择</div>
          <el-select v-model="query.idRegion" placeholder="全部区域" clearable size="small" class="filter-select" @change="search">
            <el-option
              v-for="r in regionOptions"
              :key="r.id"
              :label="r.name"
              :value="r.id"
            />
          </el-select>
        </div>
        <div class="filter-item">
          <div class="filter-label">机构选择</div>
          <el-select v-model="query.idOrg" placeholder="全部机构" clearable size="small" class="filter-select" @change="search">
            <el-option
              v-for="o in orgOptions"
              :key="o.id"
              :label="o.name"
              :value="o.id"
            />
          </el-select>
        </div>
        <div class="filter-actions">
          <el-button type="primary" size="small" @click="search">查询</el-button>
          <el-button size="small" @click="reset">重置</el-button>
        </div>
      </div>
      <template #actions>
        <el-button size="small" icon="el-icon-download" :loading="exporting" @click="exportData">导出数据</el-button>
      </template>
    </admin-filter-bar>

    <div class="metric-grid analytics-metrics">
      <metric-card
        v-for="card in cards"
        :key="card.key"
        :label="card.label"
        :value="card.value"
        :growth-text="card.growthText"
        :growth-up="card.growthUp"
        :desc="card.desc"
      />
    </div>

    <div class="chart-grid">
      <chart-panel title="服务趋势分析">
        <template #actions>
          <el-select v-model="trendMetric" size="small" class="trend-select">
            <el-option label="功能调用量" value="ai" />
            <el-option label="问诊量" value="consultation" />
          </el-select>
        </template>
        <div ref="trendChartRef" class="chart-body"></div>
      </chart-panel>
    </div>

    <div class="chart-grid chart-grid--double">
      <chart-panel title="功能调用量按机构分布" subtitle="Top 10 医疗机构">
        <div ref="orgChartRef" class="chart-body"></div>
      </chart-panel>
      <chart-panel title="功能调用量按区域分布" subtitle="各区县占比情况">
        <div ref="regionChartRef" class="chart-body"></div>
        <div class="pie-legend">
          <span
            v-for="(item, idx) in regionLegend"
            :key="idx"
            class="legend-item"
          >
            <span class="legend-dot" :style="{ background: item.color }"></span>
            {{ item.name }} ({{ item.pct }}%)
          </span>
        </div>
      </chart-panel>
    </div>
  </div>
</template>

<script>
import * as echarts from 'echarts'
import http from '../api/http'
import { refOptions } from '../api/reference'
import { AdminFilterBar, ChartPanel, MetricCard, TimeRangeFilter } from '../components/ui'

const TIME_RANGES = [
  { value: 'today', label: '今日' },
  { value: 'week', label: '本周' },
  { value: 'month', label: '本月' },
  { value: 'quarter', label: '本季度' },
  { value: 'year', label: '本年' },
  { value: 'custom', label: '自定义' }
]

const CARD_DEFS = [
  { key: 'aiServiceTotal', label: '功能调用总量', desc: '用户实际调用辅诊功能的次数之和' },
  { key: 'avgDailyAiService', label: '日均功能调用量', desc: '功能调用总量 ÷ 所选时间段的天数' },
  { key: 'aiAdoptionRate', label: 'AI诊断建议采纳率', desc: '医生采纳AI推荐诊断的次数占比', isPct: true },
  { key: 'diagnosisMatchRate', label: '诊断符合率', desc: '医生确认诊断与AI推荐完全一致的病例占比', isPct: true },
  { key: 'activeDoctorCount', label: '活跃医生数', desc: '每月登录≥10次且完成≥5次有效操作的医生' },
  { key: 'consultationTotal', label: '问诊总数', desc: '完成并回写的问诊记录总数' }
]

const COLORS = ['#3B82F6', '#10B981', '#F59E0B', '#8B5CF6', '#EF4444', '#EC4899']

export default {
  components: {
    AdminFilterBar,
    ChartPanel,
    MetricCard,
    TimeRangeFilter
  },
  data() {
    return {
      loading: false,
      timeRange: 'month',
      timeRangeOptions: TIME_RANGES,
      query: { dateFrom: '', dateTo: '', idRegion: '', idOrg: '' },
      summary: {},
      trendData: { days: [], aiServiceValues: [], consultationValues: [] },
      distribution: { orgDistribution: [], regionDistribution: [], totalService: 0 },
      regionOptions: [],
      orgOptions: [],
      exporting: false,
      trendMetric: 'ai',
      trendChart: null,
      orgChart: null,
      regionChart: null
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
        if (def.isPct && value != null) {
          value = value + '%'
        }
        if (value == null) {
          value = def.isPct ? '0%' : 0
        }
        const growthKey = this.growthKeyMap[def.key]
        const growthVal = growthKey ? (s[growthKey] || '0') : '0'
        const growthNum = parseFloat(growthVal)
        const growthUp = !isNaN(growthNum) && growthNum >= 0
        let growthText = ''
        if (def.key === 'activeDoctorCount') {
          const n = parseInt(growthVal, 10)
          growthText = isNaN(n) ? `${label}持平` : (n >= 0 ? `${label}增长 ${n} 人` : `${label}减少 ${Math.abs(n)} 人`)
        } else if (def.isPct) {
          growthText = `${label}${growthUp ? '增长' : '下降'} ${Math.abs(growthNum).toFixed(1)}%`
        } else {
          growthText = `${label}${growthUp ? '增长' : '下降'} ${Math.abs(growthNum).toFixed(1)}%`
        }
        return {
          key: def.key,
          label: def.label,
          value,
          desc: def.desc,
          growthUp,
          growthText
        }
      })
    },
    growthKeyMap() {
      return {
        aiServiceTotal: 'aiServiceGrowth',
        avgDailyAiService: 'avgDailyGrowth',
        aiAdoptionRate: 'adoptionRateGrowth',
        diagnosisMatchRate: 'matchRateGrowth',
        activeDoctorCount: 'activeDoctorGrowth',
        consultationTotal: 'consultationGrowth'
      }
    },
    regionLegend() {
      return (this.distribution.regionDistribution || []).map((item, idx) => ({
        name: item.name,
        pct: item.percentage,
        color: COLORS[idx % COLORS.length]
      }))
    }
  },
  mounted() {
    this.initDateRange()
    this.loadRefOptions()
    this.search()
  },
  beforeDestroy() {
    this.disposeCharts()
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
    async loadRefOptions() {
      try {
        const refs = await refOptions()
        this.regionOptions = (refs.regions || []).map(r => ({ id: r.idRegion, name: r.naRegion }))
        this.orgOptions = (refs.orgs || []).map(o => ({ id: o.idOrg, name: o.naOrg }))
      } catch (e) {
        // degrade gracefully
      }
    },
    async search() {
      this.loading = true
      try {
        const params = { ...this.query, timeRange: this.timeRange }
        if (!params.idRegion) delete params.idRegion
        if (!params.idOrg) delete params.idOrg

        const [summary, trend, distribution] = await Promise.all([
          http.get('/admin/api/analytics/summary', { params }),
          http.get('/admin/api/analytics/trend', { params }),
          http.get('/admin/api/analytics/distribution', { params })
        ])
        this.summary = summary || {}
        this.trendData = trend || { days: [], aiServiceValues: [], consultationValues: [] }
        this.distribution = distribution || { orgDistribution: [], regionDistribution: [], totalService: 0 }

        this.$nextTick(() => {
          this.renderTrendChart()
          this.renderOrgChart()
          this.renderRegionChart()
        })
      } catch (error) {
        this.$message.error((error && error.message) || '加载失败')
      } finally {
        this.loading = false
      }
    },
    reset() {
      this.timeRange = 'month'
      this.query.idRegion = ''
      this.query.idOrg = ''
      this.initDateRange()
      this.search()
    },
    async exportData() {
      this.exporting = true
      try {
        const params = { ...this.query, timeRange: this.timeRange }
        if (!params.idRegion) delete params.idRegion
        if (!params.idOrg) delete params.idOrg
        const blob = await http.get('/admin/api/analytics/export', { params, responseType: 'blob' })
        this.downloadBlob(blob, '统计分析_' + new Date().toISOString().slice(0, 10) + '.xlsx')
      } catch (error) {
        this.$message.error((error && error.message) || '导出失败')
      } finally {
        this.exporting = false
      }
    },
    downloadBlob(blob, filename) {
      const url = URL.createObjectURL(blob)
      const link = document.createElement('a')
      link.href = url
      link.download = filename
      document.body.appendChild(link)
      link.click()
      document.body.removeChild(link)
      URL.revokeObjectURL(url)
    },
    disposeCharts() {
      if (this.trendChart) { this.trendChart.dispose(); this.trendChart = null }
      if (this.orgChart) { this.orgChart.dispose(); this.orgChart = null }
      if (this.regionChart) { this.regionChart.dispose(); this.regionChart = null }
    },
    renderTrendChart() {
      if (!this.$refs.trendChartRef) return
      if (this.trendChart) this.trendChart.dispose()
      this.trendChart = echarts.init(this.$refs.trendChartRef)

      const days = this.trendData.days || []
      const values = this.trendMetric === 'ai'
        ? (this.trendData.aiServiceValues || [])
        : (this.trendData.consultationValues || [])

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
          splitLine: { lineStyle: { color: '#F1F5F9' } },
          axisLabel: { color: '#94A3B8', fontSize: 12 }
        },
        series: [{
          type: 'line',
          data: values,
          smooth: true,
          symbol: 'circle',
          symbolSize: 6,
          lineStyle: { color: '#3B82F6', width: 2 },
          itemStyle: { color: '#3B82F6' },
          areaStyle: {
            color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
              { offset: 0, color: 'rgba(59,130,246,0.15)' },
              { offset: 1, color: 'rgba(59,130,246,0)' }
            ])
          }
        }]
      })

      const self = this
      window.addEventListener('resize', self._resizeTrend = () => {
        if (self.trendChart) self.trendChart.resize()
      })
    },
    renderOrgChart() {
      if (!this.$refs.orgChartRef) return
      if (this.orgChart) this.orgChart.dispose()
      this.orgChart = echarts.init(this.$refs.orgChartRef)

      const data = (this.distribution.orgDistribution || []).slice(0, 10)
      this.orgChart.setOption({
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
          splitLine: { lineStyle: { color: '#F1F5F9' } },
          axisLabel: { color: '#94A3B8', fontSize: 12 }
        },
        series: [{
          type: 'bar',
          data: data.map(d => d.value),
          barWidth: 36,
          itemStyle: {
            color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
              { offset: 0, color: '#60A5FA' },
              { offset: 1, color: '#3B82F6' }
            ]),
            borderRadius: [4, 4, 0, 0]
          },
          label: {
            show: true,
            position: 'top',
            color: '#475569',
            fontSize: 11
          }
        }]
      })

      const self = this
      window.addEventListener('resize', self._resizeOrg = () => {
        if (self.orgChart) self.orgChart.resize()
      })
    },
    renderRegionChart() {
      if (!this.$refs.regionChartRef) return
      if (this.regionChart) this.regionChart.dispose()
      this.regionChart = echarts.init(this.$refs.regionChartRef)

      const data = this.distribution.regionDistribution || []
      this.regionChart.setOption({
        tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
        series: [{
          type: 'pie',
          radius: ['50%', '75%'],
          center: ['50%', '45%'],
          avoidLabelOverlap: false,
          itemStyle: { borderColor: '#fff', borderWidth: 2 },
          label: { show: false },
          emphasis: {
            label: { show: true, fontSize: 14, fontWeight: 'bold' }
          },
          data: data.map((d, idx) => ({
            value: d.value,
            name: d.name,
            itemStyle: { color: COLORS[idx % COLORS.length] }
          }))
        }],
        graphic: [
          {
            type: 'text',
            left: 'center',
            top: '38%',
            style: {
              text: (this.distribution.totalService || 0).toLocaleString(),
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
              text: '总调用量',
              textAlign: 'center',
              fill: '#94A3B8',
              fontSize: 12
            }
          }
        ]
      })

      const self = this
      window.addEventListener('resize', self._resizeRegion = () => {
        if (self.regionChart) self.regionChart.resize()
      })
    }
  },
  watch: {
    trendMetric() {
      this.renderTrendChart()
    }
  }
}
</script>

<style scoped>
.analytics-metrics {
  grid-template-columns: repeat(6, minmax(0, 1fr));
}

.trend-select {
  width: 160px;
}

.pie-legend {
  display: flex;
  justify-content: center;
  gap: 18px;
  margin-top: 12px;
  flex-wrap: wrap;
}

.legend-item {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: #64748B;
}

.legend-dot {
  width: 12px;
  height: 12px;
  border-radius: 999px;
  display: inline-block;
}

@media (max-width: 1280px) {
  .analytics-metrics {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
  .chart-grid--double {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .analytics-metrics {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
