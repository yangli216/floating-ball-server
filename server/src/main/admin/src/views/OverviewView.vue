<template>
  <div class="overview-page">
    <div class="filter-bar overview-actions">
      <el-button type="primary" @click="loadData">刷新</el-button>
      <el-button @click="$router.push('/users')">用户管理</el-button>
    </div>

    <div class="overview-grid" v-loading="loading">
      <button
        v-for="item in statCards"
        :key="item.key"
        type="button"
        class="stat-card"
        @click="jumpTo(item.path)"
      >
        <div class="stat-card__label">{{ item.label }}</div>
        <div class="stat-card__value">{{ item.value }}</div>
      </button>
    </div>

    <div class="page-card quick-panel">
      <div class="section-title">快速入口</div>
      <div class="quick-links">
        <button
          v-for="item in quickLinks"
          :key="item.path"
          type="button"
          class="quick-link"
          @click="jumpTo(item.path)"
        >
          <span>{{ item.label }}</span>
          <span class="quick-link__arrow">›</span>
        </button>
      </div>
    </div>
  </div>
</template>

<script>
import http from '../api/http'

const statDefinitions = [
  { key: 'regionCount', label: '区域', path: '/regions' },
  { key: 'orgCount', label: '机构', path: '/orgs' },
  { key: 'deviceCount', label: '设备', path: '/devices' },
  { key: 'configCount', label: '配置', path: '/configs' },
  { key: 'symptomTemplateCount', label: '症状模板', path: '/symptom-templates' },
  { key: 'logCount', label: '日志', path: '/logs' }
]

export default {
  data() {
    return {
      loading: false,
      stats: {},
      quickLinks: [
        { path: '/users', label: '用户' },
        { path: '/roles', label: '角色' },
        { path: '/configs', label: '配置' },
        { path: '/symptom-templates', label: '症状模板' },
        { path: '/logs', label: '日志' }
      ]
    }
  },
  computed: {
    statCards() {
      return statDefinitions.map(item => ({
        key: item.key,
        label: item.label,
        path: item.path,
        value: this.normalizeCount(this.stats[item.key])
      }))
    }
  },
  mounted() {
    this.loadData()
  },
  methods: {
    normalizeCount(value) {
      const numberValue = Number(value)
      return Number.isFinite(numberValue) ? numberValue : 0
    },
    async loadData() {
      this.loading = true
      try {
        this.stats = await http.get('/admin/api/stats/overview')
      } catch (error) {
        this.$message.error((error && error.message) || '加载失败')
      } finally {
        this.loading = false
      }
    },
    jumpTo(path) {
      if (path && path !== this.$route.path) {
        this.$router.push(path)
      }
    }
  }
}
</script>

<style scoped>
.overview-page {
  display: grid;
  gap: 14px;
}

.overview-actions {
  justify-content: flex-end;
}

.overview-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
}

.stat-card {
  min-height: 112px;
  padding: 18px 20px;
  text-align: left;
  border: 0.5px solid #E8EEEC;
  border-radius: 12px;
  cursor: pointer;
  background: #fff;
}

.stat-card:hover {
  border-color: #C8E8DC;
}

.stat-card__label {
  font-size: 12px;
  color: #888780;
}

.stat-card__value {
  margin-top: 14px;
  font-size: 30px;
  font-weight: 500;
  color: #2C2C2A;
}

.section-title {
  margin-bottom: 12px;
  font-size: 15px;
  font-weight: 500;
  color: #2C2C2A;
}

.quick-links {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 10px;
}

.quick-link {
  height: 38px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 12px;
  border: 0.5px solid #E8EEEC;
  border-radius: 8px;
  background: #fff;
  color: #5F5E5A;
  cursor: pointer;
  font-size: 13px;
}

.quick-link:hover {
  border-color: #1D9E75;
  color: #0F6E56;
}

.quick-link__arrow {
  color: #888780;
}

@media (max-width: 1080px) {
  .overview-grid,
  .quick-links {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 640px) {
  .overview-grid,
  .quick-links {
    grid-template-columns: 1fr;
  }
}
</style>
