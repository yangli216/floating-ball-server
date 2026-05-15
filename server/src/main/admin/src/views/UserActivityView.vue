<template>
  <div class="user-activity-page" v-loading="loading">
    <div class="main-layout">
      <div class="left-tree">
        <div class="tree-header">区域层级</div>
        <div class="tree-body">
          <div
            v-for="root in regionTree"
            :key="root.id"
            class="tree-root"
          >
            <tree-node
              :node="root"
              :selected-id="selectedRegionId"
              :level="0"
              @select="onRegionSelect"
            />
          </div>
        </div>
      </div>

      <div class="right-content">
        <div class="filter-bar">
          <div class="filter-row">
            <div class="filter-item">
              <div class="filter-label">时间范围：</div>
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
          </div>
          <div class="export-bar">
            <el-button size="small" icon="el-icon-download" @click="exportData">导出</el-button>
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

        <div class="user-filter-bar">
          <div class="filter-item">
            <div class="filter-label">活跃状态</div>
            <el-select v-model="query.activeStatus" placeholder="全部" clearable size="small" class="filter-select" @change="searchUsers">
              <el-option label="活跃" value="active" />
              <el-option label="不活跃" value="inactive" />
            </el-select>
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
            <el-table-column prop="operationCount" label="操作次数" width="100" />
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
    </div>
  </div>
</template>

<script>
import http from '../api/http'

const TIME_RANGES = [
  { value: 'month', label: '本月' },
  { value: 'lastMonth', label: '上月' },
  { value: 'custom', label: '自定义' }
]

const CARD_DEFS = [
  { key: 'activeUsers', label: '活跃用户数', desc: '所选时段内有问诊记录的设备数', isAbs: true },
  { key: 'inactiveUsers', label: '不活跃用户数', desc: '所选时段内无问诊记录的设备数', isAbs: true },
  { key: 'activityRate', label: '活跃率', desc: '活跃用户数 / 总设备数', isPct: true },
  { key: 'avgUsageDuration', label: '平均使用时长', desc: '活跃用户平均使用时长估算', isDuration: true }
]

export default {
  components: {
    TreeNode: {
      name: 'TreeNode',
      props: {
        node: Object,
        selectedId: String,
        level: Number
      },
      methods: {
        toggle() {
          this.node._expanded = !this.node._expanded
          this.$forceUpdate()
        },
        select() {
          this.$emit('select', this.node)
        }
      },
      template: `
        <div class="tree-node">
          <div
            :class="['tree-node__header', { 'is-selected': selectedId === node.id, 'is-root': level === 0 }]"
            :style="{ paddingLeft: (level * 16 + 12) + 'px' }"
            @click="select"
          >
            <span
              v-if="node.children && node.children.length > 0"
              class="tree-node__arrow"
              @click.stop="toggle"
            >{{ node._expanded ? '▼' : '▶' }}</span>
            <span v-else class="tree-node__arrow tree-node__arrow--leaf">●</span>
            <span class="tree-node__name">{{ node.name }}</span>
            <span class="tree-node__count">({{ node.userCount }})</span>
          </div>
          <div v-if="node._expanded && node.children && node.children.length > 0">
            <tree-node
              v-for="child in node.children"
              :key="child.id"
              :node="child"
              :selected-id="selectedId"
              :level="level + 1"
              @select="$emit('select', $event)"
            />
          </div>
        </div>
      `
    }
  },
  data() {
    return {
      loading: false,
      timeRange: 'month',
      timeRangeOptions: TIME_RANGES,
      query: { dateFrom: '', dateTo: '', idRegion: '', activeStatus: '' },
      summary: {},
      regionTree: [],
      userList: [],
      selectedRegionId: '',
      userPage: { current: 1, size: 10, total: 0 }
    }
  },
  computed: {
    cardCompareLabel() {
      const m = {
        month: '较上月',
        lastMonth: '较上月',
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
          value = def.isPct ? '0%' : def.isDuration ? '0 分钟' : 0
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
        } else if (def.isDuration) {
          growthText = `${label}${growthUp ? '增长' : '下降'} ${growthVal}`
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
    this.search()
  },
  methods: {
    initDateRange() {
      const now = new Date()
      let from, to
      switch (this.timeRange) {
        case 'month':
          from = this.fmt(new Date(now.getFullYear(), now.getMonth(), 1))
          to = this.fmt(now)
          break
        case 'lastMonth': {
          const prev = new Date(now.getFullYear(), now.getMonth() - 1, 1)
          from = this.fmt(prev)
          to = this.fmt(new Date(now.getFullYear(), now.getMonth(), 0))
          break
        }
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
      if (val !== 'custom') {
        this.initDateRange()
        this.search()
      }
    },
    onCustomDateChange() {
      if (this.query.dateFrom && this.query.dateTo) {
        this.search()
      }
    },
    onRegionSelect(node) {
      this.selectedRegionId = this.selectedRegionId === node.id ? '' : node.id
      this.query.idRegion = this.selectedRegionId
      this.search()
    },
    onPageChange(page) {
      this.userPage.current = page
      this.searchUsers()
    },
    async search() {
      this.loading = true
      try {
        const params = { ...this.query, timeRange: this.timeRange }
        if (!params.idRegion) delete params.idRegion
        if (!params.activeStatus) delete params.activeStatus

        const [summary, tree] = await Promise.all([
          http.get('/admin/api/user-activity/summary', { params }),
          http.get('/admin/api/user-activity/region-tree', { params })
        ])
        this.summary = summary || {}
        this.regionTree = this.initTreeExpanded(tree || [])
        this.searchUsers()
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
        if (!params.activeStatus) delete params.activeStatus

        const data = await http.get('/admin/api/user-activity/users', { params })
        this.userList = (data && data.records) || []
        this.userPage.total = (data && data.total) || 0
      } catch (error) {
        this.$message.error((error && error.message) || '加载用户列表失败')
      }
    },
    initTreeExpanded(nodes) {
      if (!nodes || nodes.length === 0) return nodes
      for (const node of nodes) {
        this.$set(node, '_expanded', true)
        if (node.children && node.children.length > 0) {
          this.initTreeExpanded(node.children)
        }
      }
      return nodes
    },
    exportData() {
      this.$message.info('导出功能开发中')
    }
  }
}
</script>

<style scoped>
.user-activity-page {
  min-height: 100%;
}

.main-layout {
  display: grid;
  grid-template-columns: 260px minmax(0, 1fr);
  gap: 0;
  background: #fff;
  border-radius: 8px;
  border: 0.5px solid #E8EEEC;
  overflow: hidden;
  min-height: calc(100vh - 160px);
}

.left-tree {
  background: #fff;
  border-right: 0.5px solid #E8EEEC;
  overflow-y: auto;
  max-height: calc(100vh - 160px);
}

.tree-header {
  padding: 12px 16px;
  font-size: 14px;
  font-weight: 500;
  color: #4B5563;
  border-bottom: 0.5px solid #F1F5F9;
}

.tree-body {
  padding: 8px;
}

.tree-node__header {
  display: flex;
  align-items: center;
  gap: 4px;
  height: 40px;
  padding: 8px 12px;
  border-radius: 8px;
  cursor: pointer;
  font-size: 14px;
  color: #64748B;
}

.tree-node__header:hover {
  background: #F8FAFC;
}

.tree-node__header.is-selected {
  background: #EFF6FF;
  color: #1E40AF;
}

.tree-node__header.is-root {
  background: #EFF6FF;
  color: #1E40AF;
  font-weight: 500;
}

.tree-node__arrow {
  width: 18px;
  font-size: 12px;
  text-align: center;
  flex-shrink: 0;
  color: #94A3B8;
}

.tree-node__arrow--leaf {
  font-size: 8px;
  color: #CBD5E1;
}

.tree-node__name {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.tree-node__count {
  font-size: 12px;
  color: #94A3B8;
  flex-shrink: 0;
}

.right-content {
  padding: 0;
  display: grid;
  gap: 0;
  align-content: start;
}

.filter-bar {
  padding: 16px 24px;
  background: #fff;
  border-bottom: 0.5px solid #F1F5F9;
}

.filter-row {
  display: flex;
  align-items: center;
  gap: 12px;
}

.filter-item {
  display: flex;
  align-items: center;
  gap: 8px;
}

.filter-label {
  font-size: 14px;
  font-weight: 500;
  color: #4B5563;
  white-space: nowrap;
}

.time-tabs {
  display: flex;
  border: 0.8px solid #E2E8F0;
  border-radius: 8px;
  overflow: hidden;
}

.time-tab {
  height: 36px;
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
  margin-left: 8px;
}

.custom-date-row .el-date-editor {
  width: 150px;
}

.date-sep {
  color: #94A3B8;
  font-size: 13px;
}

.export-bar {
  display: flex;
  justify-content: flex-end;
  margin-top: 12px;
  padding-top: 12px;
  border-top: 0.5px solid #F1F5F9;
}

.card-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
  padding: 20px 24px;
  background: #fff;
  border-bottom: 0.5px solid #F1F5F9;
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

.user-filter-bar {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 12px 24px;
  background: #fff;
  border-bottom: 0.5px solid #F1F5F9;
}

.user-filter-bar .filter-select {
  width: 160px;
}

.user-table-card {
  padding: 16px 24px;
  background: #fff;
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
  .main-layout {
    grid-template-columns: 1fr;
  }
  .left-tree {
    max-height: 300px;
    border-right: none;
    border-bottom: 0.5px solid #E8EEEC;
  }
  .card-grid {
    grid-template-columns: 1fr 1fr;
  }
}
</style>
