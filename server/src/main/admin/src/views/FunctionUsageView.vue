<template>
  <div class="func-page" v-loading="loading">
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
        <div class="filter-item">
          <div class="filter-label">区域选择</div>
          <el-select v-model="query.idRegion" placeholder="全部区域" clearable size="small" style="width:200px" @change="search">
            <el-option v-for="r in regionOptions" :key="r.id" :label="r.name" :value="r.id" />
          </el-select>
        </div>
        <div class="filter-item">
          <div class="filter-label">机构选择</div>
          <el-select v-model="query.idOrg" placeholder="全部机构" clearable size="small" style="width:200px" @change="search">
            <el-option v-for="o in orgOptions" :key="o.id" :label="o.name" :value="o.id" />
          </el-select>
        </div>
        <div class="filter-item">
          <div class="filter-label">功能模块</div>
          <el-select v-model="query.functionModules" placeholder="全部功能" multiple collapse-tags clearable size="small" style="width:220px" @change="search">
            <el-option v-for="m in moduleOptions" :key="m" :label="m" :value="m" />
          </el-select>
        </div>
        <div>
          <el-button type="primary" size="small" @click="search">查询</el-button>
          <el-button size="small" @click="reset">重置</el-button>
        </div>
      </div>
      <div class="export-bar">
        <el-button size="small" icon="el-icon-download" :loading="exporting" @click="exportData">导出数据</el-button>
      </div>
    </div>

    <div class="card-row">
      <div class="summary-card">
        <div class="summary-card__label">总调用次数</div>
        <div class="summary-card__value">{{ (response.totalCallCount || 0).toLocaleString() }}</div>
      </div>
      <div class="summary-card">
        <div class="summary-card__label">平均每日调用</div>
        <div class="summary-card__value">{{ (response.avgDailyCalls || 0).toLocaleString() }}</div>
      </div>
      <div class="summary-card">
        <div class="summary-card__label">功能使用率</div>
        <div class="summary-card__value">{{ response.usageRate || '0%' }}</div>
      </div>
    </div>

    <div class="chart-duo">
      <div class="chart-panel">
        <div class="chart-panel__title">功能使用排行</div>
        <div ref="rankChartRef" class="chart-body--rank"></div>
      </div>
      <div class="chart-panel">
        <div class="chart-panel__title">功能使用趋势</div>
        <div ref="trendChartRef" class="chart-body--trend"></div>
        <div class="trend-legend">
          <span
            v-for="(m, idx) in (trend.modules || [])"
            :key="m"
            class="legend-item"
          >
            <span class="legend-dot" :style="{ background: COLORS[idx % COLORS.length] }"></span>
            {{ m }}
          </span>
        </div>
      </div>
    </div>

    <div class="table-section">
      <div class="table-section__header">
        <span class="table-section__title">功能使用明细</span>
        <div class="table-section__meta">
          共 {{ response.total || 0 }} 条
          <el-pagination
            small
            layout="prev, pager, next"
            :total="response.total || 0"
            :page-size="pageSize"
            :current-page.sync="pageNum"
            @current-change="pageChange"
          />
        </div>
      </div>
      <el-table :data="response.records || []" border stripe size="small">
        <el-table-column prop="moduleName" label="功能模块" min-width="160" />
        <el-table-column prop="callCount" label="调用次数" width="120" sortable />
        <el-table-column prop="doctorCount" label="使用医生数" width="120" sortable />
        <el-table-column prop="avgPerDoctor" label="人均调用" width="100" />
        <el-table-column prop="growthRate" label="增长率(%)" width="100" sortable>
          <template slot-scope="{ row }">
            <span :style="{ color: parseFloat(row.growthRate || 0) >= 0 ? '#10B981' : '#EF4444' }">
              {{ row.growthRate || '0' }}
            </span>
          </template>
        </el-table-column>
      </el-table>
    </div>
  </div>
</template>

<script>
import * as echarts from 'echarts'
import http from '../api/http'
import { refOptions } from '../api/reference'

const TIME_RANGES = [
  { value: 'today', label: '今日' },
  { value: 'week', label: '本周' },
  { value: 'month', label: '本月' },
  { value: 'quarter', label: '本季度' },
  { value: 'year', label: '本年' },
  { value: 'custom', label: '自定义' }
]

const COLORS = ['#3B82F6', '#10B981', '#F59E0B', '#8B5CF6', '#EF4444', '#EC4899', '#06B6D4', '#F97316']

export default {
  data() {
    return {
      loading: false,
      timeRange: 'month',
      timeRangeOptions: TIME_RANGES,
      pageNum: 1,
      pageSize: 20,
      query: { dateFrom: '', dateTo: '', idRegion: '', idOrg: '', functionModules: [] },
      response: {},
      trend: { modules: [], days: [], values: [] },
      moduleOptions: [],
      regionOptions: [],
      orgOptions: [],
      exporting: false,
      COLORS,
      rankChart: null,
      trendChart: null
    }
  },
  mounted() {
    this.initDateRange()
    this.loadRefs()
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
      return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
    },
    setTimeRange(v) {
      this.timeRange = v
      this.initDateRange()
      this.search()
    },
    onCustomDateChange() {
      if (this.query.dateFrom && this.query.dateTo) this.search()
    },
    async loadRefs() {
      try {
        const refs = await refOptions()
        this.regionOptions = (refs.regions || []).map(r => ({ id: r.idRegion, name: r.naRegion }))
        this.orgOptions = (refs.orgs || []).map(o => ({ id: o.idOrg, name: o.naOrg }))
        const mods = await http.get('/admin/api/analytics/function-modules')
        this.moduleOptions = mods || []
      } catch (e) { /* degrade */ }
    },
    async search() {
      this.loading = true
      try {
        const params = {
          ...this.query,
          current: this.pageNum,
          size: this.pageSize
        }
        if (!params.idRegion) delete params.idRegion
        if (!params.idOrg) delete params.idOrg
        if (!params.functionModules || params.functionModules.length === 0) delete params.functionModules

        const data = await http.get('/admin/api/analytics/function-usage', { params })
        this.response = data || {}
        this.trend = (data && data.trend) || { modules: [], days: [], values: [] }
        this.$nextTick(() => {
          this.renderRankChart()
          this.renderTrendChart()
        })
      } catch (e) {
        this.$message.error((e && e.message) || '加载失败')
      } finally {
        this.loading = false
      }
    },
    reset() {
      this.timeRange = 'month'
      this.query = { dateFrom: '', dateTo: '', idRegion: '', idOrg: '', functionModules: [] }
      this.pageNum = 1
      this.initDateRange()
      this.search()
    },
    pageChange(p) {
      this.pageNum = p
      this.search()
    },
    async exportData() {
      this.exporting = true
      try {
        const params = { ...this.query }
        if (!params.idRegion) delete params.idRegion
        if (!params.idOrg) delete params.idOrg
        if (!params.functionModules || params.functionModules.length === 0) delete params.functionModules
        const blob = await http.get('/admin/api/analytics/function-usage/export', { params, responseType: 'blob' })
        this.downloadBlob(blob, '辅诊功能统计_' + new Date().toISOString().slice(0, 10) + '.xlsx')
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
      if (this.rankChart) { this.rankChart.dispose(); this.rankChart = null }
      if (this.trendChart) { this.trendChart.dispose(); this.trendChart = null }
    },
    renderRankChart() {
      if (!this.$refs.rankChartRef) return
      if (this.rankChart) this.rankChart.dispose()
      this.rankChart = echarts.init(this.$refs.rankChartRef)
      const data = (this.response.ranking || []).slice(0, 10).reverse()
      this.rankChart.setOption({
        tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
        grid: { left: 140, right: 40, top: 10, bottom: 20 },
        xAxis: { type: 'value', splitLine: { lineStyle: { color: '#F1F5F9' } }, axisLabel: { color: '#94A3B8', fontSize: 11 } },
        yAxis: {
          type: 'category',
          data: data.map(d => d.moduleName),
          axisLine: { lineStyle: { color: '#E2E8F0' } },
          axisLabel: { color: '#475569', fontSize: 12 }
        },
        series: [{
          type: 'bar',
          data: data.map(d => d.callCount),
          barWidth: 20,
          itemStyle: {
            color: new echarts.graphic.LinearGradient(0, 0, 1, 0, [
              { offset: 0, color: '#60A5FA' },
              { offset: 1, color: '#3B82F6' }
            ]),
            borderRadius: [0, 4, 4, 0]
          },
          label: { show: true, position: 'right', color: '#475569', fontSize: 11 }
        }]
      })
      const self = this
      window.addEventListener('resize', self._resizeRank = () => { if (self.rankChart) self.rankChart.resize() })
    },
    renderTrendChart() {
      if (!this.$refs.trendChartRef) return
      if (this.trendChart) this.trendChart.dispose()
      this.trendChart = echarts.init(this.$refs.trendChartRef)
      const modules = this.trend.modules || []
      const days = this.trend.days || []
      const values = this.trend.values || []
      this.trendChart.setOption({
        tooltip: { trigger: 'axis' },
        grid: { left: 50, right: 20, top: 10, bottom: 30 },
        xAxis: {
          type: 'category',
          data: days,
          axisLine: { lineStyle: { color: '#E2E8F0' } },
          axisLabel: { color: '#94A3B8', fontSize: 11 }
        },
        yAxis: {
          type: 'value',
          splitLine: { lineStyle: { color: '#F1F5F9' } },
          axisLabel: { color: '#94A3B8', fontSize: 11 }
        },
        series: modules.map((m, i) => ({
          name: m,
          type: 'line',
          data: values[i] || [],
          smooth: true,
          symbol: 'circle',
          symbolSize: 4,
          lineStyle: { color: COLORS[i % COLORS.length], width: 2 },
          itemStyle: { color: COLORS[i % COLORS.length] }
        }))
      })
      const self = this
      window.addEventListener('resize', self._resizeTrend = () => { if (self.trendChart) self.trendChart.resize() })
    }
  }
}
</script>

<style scoped>
.func-page { display: grid; gap: 16px; }
.filter-bar { background: #fff; border-radius: 8px; padding: 20px 24px; border: 0.5px solid #E8EEEC; }
.filter-row { display: flex; align-items: flex-end; gap: 20px; flex-wrap: wrap; }
.filter-item { display: grid; gap: 6px; }
.filter-label { font-size: 14px; font-weight: 500; color: #4B5563; }
.time-tabs { display: flex; border: 0.8px solid #E2E8F0; border-radius: 8px; overflow: hidden; }
.time-tab { height: 38px; padding: 0 16px; border: none; background: #fff; color: #64748B; font-size: 14px; cursor: pointer; }
.time-tab.is-active { background: #EFF6FF; color: #1E40AF; font-weight: 500; }
.custom-date-row { display: flex; align-items: center; gap: 8px; margin-top: 8px; }
.date-sep { color: #94A3B8; font-size: 13px; }
.export-bar { display: flex; justify-content: flex-end; margin-top: 14px; padding-top: 14px; border-top: 0.5px solid #F1F5F9; }
.card-row { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 14px; }
.summary-card { padding: 24px 20px; background: #fff; border: 0.8px solid #F1F5F9; border-radius: 12px; box-shadow: 0 1px 2px rgba(0,0,0,0.05); }
.summary-card__label { font-size: 14px; color: #64748B; }
.summary-card__value { font-size: 28px; font-weight: 600; color: #1E293B; margin-top: 8px; }
.chart-duo { display: grid; grid-template-columns: 1fr 1fr; gap: 14px; }
.chart-panel { background: #fff; border: 0.8px solid #F1F5F9; border-radius: 12px; box-shadow: 0 1px 2px rgba(0,0,0,0.05); padding: 20px; }
.chart-panel__title { font-size: 16px; font-weight: 600; color: #1E293B; margin-bottom: 12px; }
.chart-body--rank { width: 100%; height: 340px; }
.chart-body--trend { width: 100%; height: 300px; }
.trend-legend { display: flex; justify-content: center; gap: 16px; margin-top: 8px; flex-wrap: wrap; }
.legend-item { display: flex; align-items: center; gap: 4px; font-size: 12px; color: #64748B; }
.legend-dot { width: 10px; height: 10px; border-radius: 999px; }
.table-section { background: #fff; border: 0.8px solid #F1F5F9; border-radius: 12px; box-shadow: 0 1px 2px rgba(0,0,0,0.05); padding: 20px; }
.table-section__header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 12px; }
.table-section__title { font-size: 16px; font-weight: 600; color: #1E293B; }
.table-section__meta { display: flex; align-items: center; gap: 12px; font-size: 13px; color: #94A3B8; }
@media (max-width: 1280px) { .chart-duo { grid-template-columns: 1fr; } }
</style>
