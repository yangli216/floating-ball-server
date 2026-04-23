<template>
  <div class="overview-page">
    <div class="page-card welcome-card">
      <div>
        <div class="welcome-card__eyebrow">Overview</div>
        <div class="welcome-card__title">平台治理概览</div>
        <div class="welcome-card__desc">
          统一查看区域、机构、设备、配置、Prompt、症状模板、数据包以及新增的用户与角色治理规模。
        </div>
      </div>
      <div class="welcome-card__actions">
        <el-button icon="el-icon-refresh" @click="loadData">刷新统计</el-button>
        <el-button type="primary" @click="$router.push('/users')">进入用户管理</el-button>
      </div>
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
        <div class="stat-card__meta">点击进入{{ item.label }}</div>
      </button>
    </div>

    <div class="overview-panels">
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
            <span class="quick-link__arrow">></span>
          </button>
        </div>
      </div>

      <div class="page-card status-panel">
        <div class="section-title">本轮范围</div>
        <div class="status-list">
          <div class="status-item">
            <div class="status-item__label">登录鉴权</div>
            <div class="status-item__value">登录页 + 路由守卫 + Token 自动注入</div>
          </div>
          <div class="status-item">
            <div class="status-item__label">组织治理</div>
            <div class="status-item__value">用户管理、角色管理已接入本轮接口</div>
          </div>
          <div class="status-item">
            <div class="status-item__label">统计能力</div>
            <div class="status-item__value">概览页基于 `/admin/api/stats/overview` 实时刷新</div>
          </div>
        </div>
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
  { key: 'promptCount', label: 'Prompt', path: '/prompts' },
  { key: 'symptomTemplateCount', label: '症状模板', path: '/symptom-templates' },
  { key: 'dataPackageCount', label: '数据包', path: '/data-packages' },
  { key: 'logCount', label: '日志', path: '/logs' },
  { key: 'userCount', label: '用户', path: '/users' },
  { key: 'roleCount', label: '角色', path: '/roles' }
]

export default {
  data() {
    return {
      loading: false,
      stats: {},
      quickLinks: [
        { path: '/users', label: '查看用户管理' },
        { path: '/roles', label: '查看角色管理' },
        { path: '/configs', label: '查看配置管理' },
        { path: '/symptom-templates', label: '查看症状模板' },
        { path: '/logs', label: '查看操作日志' }
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
        this.$message.error((error && error.message) || '加载概览统计失败')
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
  gap: 20px;
}

.welcome-card {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 24px;
  background:
    linear-gradient(135deg, rgba(17, 63, 103, 0.06), rgba(17, 63, 103, 0.02)),
    #fff;
}

.welcome-card__eyebrow {
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.8px;
  color: #3770ab;
  text-transform: uppercase;
}

.welcome-card__title {
  margin-top: 10px;
  font-size: 28px;
  font-weight: 700;
  color: #16324f;
}

.welcome-card__desc {
  margin-top: 10px;
  max-width: 620px;
  line-height: 1.75;
  color: #5e7288;
}

.welcome-card__actions {
  display: flex;
  gap: 12px;
  flex-shrink: 0;
}

.overview-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
}

.stat-card {
  padding: 22px 24px;
  text-align: left;
  border: none;
  border-radius: 18px;
  cursor: pointer;
  background: #fff;
  box-shadow: 0 10px 28px rgba(17, 63, 103, 0.08);
  transition: transform 0.18s ease, box-shadow 0.18s ease;
}

.stat-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 16px 32px rgba(17, 63, 103, 0.12);
}

.stat-card__label {
  font-size: 14px;
  color: #6f8399;
}

.stat-card__value {
  margin-top: 12px;
  font-size: 34px;
  font-weight: 700;
  color: #16324f;
}

.stat-card__meta {
  margin-top: 12px;
  font-size: 12px;
  color: #8fa1b4;
}

.overview-panels {
  display: grid;
  grid-template-columns: 1.1fr 0.9fr;
  gap: 20px;
}

.section-title {
  font-size: 18px;
  font-weight: 600;
  color: #16324f;
}

.quick-panel,
.status-panel {
  display: grid;
  gap: 16px;
}

.quick-links {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.quick-link {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 18px;
  border: 1px solid #e5edf5;
  border-radius: 14px;
  background: #f8fbfe;
  color: #16324f;
  cursor: pointer;
}

.quick-link__arrow {
  font-weight: 700;
  color: #3770ab;
}

.status-list {
  display: grid;
  gap: 12px;
}

.status-item {
  padding: 16px 18px;
  border-radius: 14px;
  background: #f8fbfe;
}

.status-item__label {
  font-size: 13px;
  color: #6f8399;
}

.status-item__value {
  margin-top: 8px;
  line-height: 1.65;
  color: #16324f;
}

@media (max-width: 1100px) {
  .overview-grid,
  .overview-panels,
  .quick-links {
    grid-template-columns: 1fr;
  }

  .welcome-card {
    flex-direction: column;
    align-items: flex-start;
  }

  .welcome-card__actions {
    width: 100%;
  }
}
</style>
