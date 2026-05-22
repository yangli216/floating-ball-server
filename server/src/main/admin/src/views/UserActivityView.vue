<template>
  <div class="user-activity-page" v-loading="loading">
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
        <div class="filter-item">
          <div class="filter-label">活跃状态</div>
          <el-select v-model="query.activeStatus" placeholder="全部状态" clearable size="small" class="filter-select" @change="search">
            <el-option label="活跃" value="active" />
            <el-option label="不活跃" value="inactive" />
          </el-select>
        </div>
        <div class="filter-actions">
          <el-button type="primary" size="small" @click="search">查询</el-button>
          <el-button size="small" @click="reset">重置</el-button>
        </div>
      </div>
      <div class="export-bar">
        <el-button size="small" icon="el-icon-download" :loading="exporting" @click="exportData">导出数据</el-button>
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

    <div class="user-table-card">
      <el-table :data="userList" size="small" style="width: 100%">
        <el-table-column prop="naDoctor" label="医生姓名" min-width="120" />
        <el-table-column prop="cdDevice" label="设备编码" min-width="120" />
        <el-table-column prop="naOrg" label="所属机构" min-width="140" />
        <el-table-column prop="naRegion" label="所属区域" min-width="100" />
        <el-table-column label="活跃状态" width="100">
          <template slot-scope="{ row }">
            <span :class="['status-pill', row.activeStatus === 'active' ? 'is-active' : 'is-inactive']">
              {{ row.activeStatus === 'active' ? '活跃' : '不活跃' }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="consultationCount" label="问诊次数" width="100" />
        <el-table-column prop="effectiveConsultationCount" label="有效问诊数" width="120" />
        <el-table-column label="最后活跃时间" width="160">
          <template slot-scope="{ row }">
            {{ row.lastActiveTime || '-' }}
          </template>
        </el-table-column>
      </el-table>
      <div class="pagination-bar">
        <el-pagination
          :current-page="userPage.current"
          :page-size="userPage.size"
          :total="userPage.total"
          layout="total, prev, pager, next"
          @current-change="onPageChange"
        />
      </div>
    </div>
  </div>
</template>

<script>
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

const CARD_DEFS = [
  { key: 'activeUsers', label: '活跃用户数', desc: '所选时段内有问诊记录的设备数', isAbs: true },
  { key: 'inactiveUsers', label: '不活跃用户数', desc: '所选时段内无问诊记录的设备数', isAbs: true },
  { key: 'activityRate', label: '活跃率', desc: '活跃用户数 / 总设备数', isPct: true },
  { key: 'effectiveConsultationRate', label: '有效问诊率', desc: '有效问诊数 / 总问诊数', isPct: true }
]

export default {
  data() {
    return {
      loading: false,
      timeRange: 'month',
      timeRangeOptions: TIME_RANGES,
      query: { dateFrom: '', dateTo: '', idRegion: '', idOrg: '', activeStatus: '' },
      summary: {},
      regionOptions: [],
      orgOptions: [],
      exporting: false,
      userList: [],
      userPage: { current: 1, size: 10, total: 0 }
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
        if (value == null) {
          value = def.isPct ? '0%' : 0
        }
        if (def.isPct && value != null && !String(value).endsWith('%')) {
          value = value + '%'
        }
        const growthKey = def.key + 'Growth'
        const growthVal = s[growthKey] || '0'
        const growthNum = parseFloat(growthVal)
        const growthUp = !isNaN(growthNum) && growthNum >= 0
        let growthText = ''
        if (def.isAbs) {
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
    }
  },
  mounted() {
    this.initDateRange()
    this.loadRefOptions()
    this.search()
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
    onPageChange(page) {
      this.userPage.current = page
      this.searchUsers()
    },
    async search() {
      this.loading = true
      try {
        this.userPage.current = 1
        const params = { ...this.query, timeRange: this.timeRange }
        if (!params.idRegion) delete params.idRegion
        if (!params.idOrg) delete params.idOrg
        if (!params.activeStatus) delete params.activeStatus

        const [summary, users] = await Promise.all([
          http.get('/admin/api/user-activity/summary', { params }),
          http.get('/admin/api/user-activity/users', { params: { ...params, current: this.userPage.current, size: this.userPage.size } })
        ])
        this.summary = summary || {}
        this.userList = (users && users.records) || []
        this.userPage.total = (users && users.total) || 0
      } catch (error) {
        this.$message.error((error && error.message) || '加载失败')
      } finally {
        this.loading = false
      }
    },
    async searchUsers() {
      try {
        const params = {
          ...this.query,
          timeRange: this.timeRange,
          current: this.userPage.current,
          size: this.userPage.size
        }
        if (!params.idRegion) delete params.idRegion
        if (!params.idOrg) delete params.idOrg
        if (!params.activeStatus) delete params.activeStatus

        const data = await http.get('/admin/api/user-activity/users', { params })
        this.userList = (data && data.records) || []
        this.userPage.total = (data && data.total) || 0
      } catch (error) {
        this.$message.error((error && error.message) || '加载用户列表失败')
      }
    },
    reset() {
      this.timeRange = 'month'
      this.query = { dateFrom: '', dateTo: '', idRegion: '', idOrg: '', activeStatus: '' }
      this.userPage.current = 1
      this.initDateRange()
      this.search()
    },
    async exportData() {
      this.exporting = true
      try {
        const params = { ...this.query, timeRange: this.timeRange }
        if (!params.idRegion) delete params.idRegion
        if (!params.idOrg) delete params.idOrg
        if (!params.activeStatus) delete params.activeStatus
        const blob = await http.get('/admin/api/user-activity/export', { params, responseType: 'blob' })
        this.downloadBlob(blob, '用户活跃度_' + new Date().toISOString().slice(0, 10) + '.xlsx')
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
    }
  }
}
</script>

<style scoped>
.user-activity-page {
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
  background: #EFF6FF;
  color: #1E40AF;
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

.filter-select {
  width: 200px;
}

.filter-actions {
  display: flex;
  gap: 8px;
  align-items: flex-end;
}

.export-bar {
  display: flex;
  justify-content: flex-end;
  margin-top: 14px;
  padding-top: 14px;
  border-top: 0.5px solid #F1F5F9;
}

.card-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
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
  color: #10B981;
}

.stat-card__growth.is-down {
  color: #EF4444;
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

.user-table-card {
  background: #fff;
  border: 0.8px solid #F1F5F9;
  border-radius: 12px;
  box-shadow: 0 1px 2px rgba(0,0,0,0.05);
  padding: 20px;
}

.pagination-bar {
  display: flex;
  justify-content: flex-end;
  margin-top: 12px;
}

.status-pill {
  display: inline-block;
  padding: 2px 8px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 500;
}

.status-pill.is-active {
  background: #D1FAE5;
  color: #065F46;
}

.status-pill.is-inactive {
  background: #FEE2E2;
  color: #991B1B;
}

@media (max-width: 1280px) {
  .card-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 768px) {
  .card-grid {
    grid-template-columns: 1fr 1fr;
  }
}
</style>
