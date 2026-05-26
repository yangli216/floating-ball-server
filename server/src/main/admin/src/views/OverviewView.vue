<template>
  <div class="page-surface overview-page">
    <admin-filter-bar>
      <div class="overview-actions">
        <el-button type="primary" icon="el-icon-refresh" @click="loadData">刷新</el-button>
        <router-link class="el-button el-button--default overview-link-button" to="/users">
          <i class="el-icon-user" aria-hidden="true"></i>
          <span>用户管理</span>
        </router-link>
      </div>
    </admin-filter-bar>

    <div class="overview-grid" v-loading="loading">
      <metric-card
        v-for="item in statCards"
        :key="item.key"
        :label="item.label"
        :value="item.value"
        clickable
        :to="item.path"
      />
    </div>

    <section class="page-section page-section--padded quick-panel">
      <div class="section-title">快速入口</div>
      <div class="quick-links">
        <router-link
          v-for="item in quickLinks"
          :key="item.path"
          class="quick-link"
          :to="item.path"
        >
          <span>{{ item.label }}</span>
          <span class="quick-link__arrow">›</span>
        </router-link>
      </div>
    </section>
  </div>
</template>

<script>
import http from '../api/http'
import { AdminFilterBar, MetricCard } from '../components/ui'

const statDefinitions = [
  { key: 'regionCount', label: '区域', path: '/regions' },
  { key: 'orgCount', label: '机构', path: '/orgs' },
  { key: 'deviceCount', label: '令牌', path: '/devices' },
  { key: 'configCount', label: '配置', path: '/configs' },
  { key: 'symptomTemplateCount', label: '症状模板', path: '/symptom-templates' },
  { key: 'logCount', label: '日志', path: '/logs' }
]

export default {
  components: {
    AdminFilterBar,
    MetricCard
  },
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
    }
  }
}
</script>

<style scoped>
.overview-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  width: 100%;
}

.overview-link-button {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  text-decoration: none;
}

.overview-grid {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.quick-panel {
  display: grid;
  gap: 12px;
}

.section-title {
  margin: 0;
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
  justify-content: flex-end;
  gap: 10px;
  padding: 0 12px;
  border: 0.5px solid #E8EEEC;
  border-radius: 8px;
  background: #fff;
  color: #5F5E5A;
  font-size: 13px;
  text-decoration: none;
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
