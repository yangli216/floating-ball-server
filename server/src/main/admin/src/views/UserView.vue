<template>
  <div class="page-surface">
    <section class="page-section page-section--padded page-section--toolbar">
      <div class="page-toolbar__filters user-filters">
        <el-input
          v-model.trim="filters.keyword"
          clearable
          placeholder="输入账号或姓名…"
          class="search-input"
          @keyup.enter.native="search"
        />
        <el-select v-model="filters.sdStatus" clearable placeholder="状态" class="filter-select">
          <el-option v-for="item in statusOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
        <el-select v-model="filters.idRole" clearable filterable placeholder="角色" class="filter-select">
          <el-option
            v-for="item in roleOptions"
            :key="resolveRoleValue(item)"
            :label="resolveRoleLabel(item)"
            :value="resolveRoleValue(item)"
          />
        </el-select>
        <el-input
          v-model.trim="filters.idOrg"
          clearable
          placeholder="机构标识"
          class="filter-select"
          @keyup.enter.native="search"
        />
        <el-button type="primary" icon="el-icon-search" @click="search">查询</el-button>
        <el-button @click="reset">重置</el-button>
      </div>
      <el-button type="primary" icon="el-icon-plus" @click="openCreate">新增用户</el-button>
    </section>

    <section class="page-section page-section--table">
      <el-table :data="records" v-loading="loading">
      <el-table-column prop="cdUser" label="登录账号" min-width="140" />
      <el-table-column prop="naUser" label="用户姓名" min-width="140" />
      <el-table-column label="所属机构" min-width="160">
        <template slot-scope="{ row }">
          {{ row.naOrg || row.idOrg || '--' }}
        </template>
      </el-table-column>
      <el-table-column label="角色" min-width="220">
        <template slot-scope="{ row }">
          <div class="role-tags">
            <el-tag v-for="role in resolveUserRoleLabels(row)" :key="role" size="mini" type="info">
              {{ role }}
            </el-tag>
            <span v-if="!resolveUserRoleLabels(row).length">--</span>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="90">
        <template slot-scope="{ row }">
          <status-pill :tone="statusTone(statusMeta(row.sdStatus).type)" :label="statusMeta(row.sdStatus).label" />
        </template>
      </el-table-column>
      <el-table-column label="更新时间" min-width="170">
        <template slot-scope="{ row }">
          {{ formatDateTime(resolveUserTime(row)) }}
        </template>
      </el-table-column>
      <el-table-column label="操作" width="180" fixed="right">
        <template slot-scope="{ row }">
          <div class="table-actions">
            <table-action @click="openEdit(row)">编辑</table-action>
            <table-action :danger="isEnabled(row)" @click="toggleStatus(row)">{{ isEnabled(row) ? '停用' : '启用' }}</table-action>
          </div>
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
    </section>

    <el-dialog v-if="dialogVisible" :title="dialogTitle" :visible.sync="dialogVisible" width="760px" @closed="resetForm">
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <div class="form-grid">
          <el-form-item label="登录账号" prop="cdUser">
            <el-input v-model.trim="form.cdUser" maxlength="64" />
          </el-form-item>
          <el-form-item label="用户姓名" prop="naUser">
            <el-input v-model.trim="form.naUser" maxlength="64" />
          </el-form-item>
          <el-form-item label="登录密码" prop="password">
            <el-input
              v-model="form.password"
              type="password"
              show-password
              maxlength="128"
              placeholder="请输入密码…"
            />
          </el-form-item>
          <el-form-item label="所属机构标识" prop="idOrg">
            <el-input v-model.trim="form.idOrg" maxlength="64" placeholder="例如 ORG001" />
          </el-form-item>
          <el-form-item label="角色" prop="roleIds" class="form-span-2">
            <el-select v-model="form.roleIds" multiple clearable filterable placeholder="请选择角色…">
              <el-option
                v-for="item in roleOptions"
                :key="resolveRoleValue(item)"
                :label="resolveRoleLabel(item)"
                :value="resolveRoleValue(item)"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="状态" prop="sdStatus">
            <segmented-switch v-model="form.sdStatus" :options="statusOptions" />
          </el-form-item>
        </div>
      </el-form>
      <span slot="footer">
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitForm">保存</el-button>
      </span>
    </el-dialog>
  </div>
</template>

<script>
import http from '../api/http'
import { configStatusOptions, findStatusMeta, formatDateTime, statusTone } from '../utils/admin'
import { SegmentedSwitch, StatusPill, TableAction } from '../components/ui'

const statusOptions = [
  { value: '1', label: '启用' },
  { value: '0', label: '停用' }
]

function createDefaultFilters() {
  return {
    keyword: '',
    sdStatus: '',
    idRole: '',
    idOrg: ''
  }
}

function createDefaultForm() {
  return {
    idUser: '',
    cdUser: '',
    naUser: '',
    password: '',
    idOrg: '',
    roleIds: [],
    sdStatus: '1'
  }
}

function normalizeList(value) {
  if (Array.isArray(value)) {
    return value
  }
  if (typeof value === 'string') {
    return value.split(',').map(item => item.trim()).filter(Boolean)
  }
  return []
}

function readObjectField(item, fields) {
  if (!item || typeof item !== 'object') {
    return ''
  }
  for (let index = 0; index < fields.length; index += 1) {
    const field = fields[index]
    if (item[field]) {
      return String(item[field]).trim()
    }
  }
  return ''
}

function extractRoleIds(value) {
  return normalizeList(value).map(item => {
    if (item && typeof item === 'object') {
      return readObjectField(item, ['idRole', 'roleId', 'cdRole', 'roleCode', 'value'])
    }
    return String(item || '').trim()
  }).filter(Boolean)
}

export default {
  components: {
    SegmentedSwitch,
    StatusPill,
    TableAction
  },
  data() {
    return {
      statusOptions,
      loading: false,
      saving: false,
      dialogVisible: false,
      dialogMode: 'create',
      current: 1,
      size: 10,
      total: 0,
      records: [],
      filters: createDefaultFilters(),
      roleOptions: [],
      roleMap: {},
      form: createDefaultForm(),
      rules: {
        cdUser: [{ required: true, message: '请输入登录账号', trigger: 'blur' }],
        naUser: [{ required: true, message: '请输入用户姓名', trigger: 'blur' }],
        idOrg: [{ required: true, message: '请输入所属机构标识', trigger: 'blur' }],
        roleIds: [{ type: 'array', required: true, message: '请选择至少一个角色', trigger: 'change' }]
      }
    }
  },
  computed: {
    dialogTitle() {
      return this.dialogMode === 'create' ? '新增用户' : '编辑用户'
    }
  },
  mounted() {
    this.loadRoleOptions()
    this.loadData()
  },
  methods: {
    formatDateTime,
    statusTone,
    statusMeta(value) {
      return findStatusMeta(configStatusOptions, value)
    },
    isEnabled(row) {
      return row && row.sdStatus === '1'
    },
    resolveRoleLabel(role) {
      if (!role) {
        return '--'
      }
      return role.naRole || role.cdRole || role.idRole || '--'
    },
    resolveRoleValue(role) {
      if (!role) {
        return ''
      }
      return role.idRole || role.cdRole || ''
    },
    resolveUserTime(row) {
      return row.updateTime || row.lastLoginTime || row.dtUpdate || row.dtLastLogin || ''
    },
    resolveUserRoleLabels(row) {
      const roleNames = normalizeList(row.roleNames).map(item => {
        if (item && typeof item === 'object') {
          return readObjectField(item, ['naRole', 'roleName', 'label', 'name'])
        }
        return String(item || '').trim()
      }).filter(Boolean)

      if (roleNames.length) {
        return roleNames
      }

      const roles = normalizeList(row.roles)
      if (roles.length) {
        return roles.map(item => {
          if (item && typeof item === 'object') {
            return readObjectField(item, ['naRole', 'roleName', 'label', 'name', 'cdRole', 'roleCode']) ||
              this.roleMap[readObjectField(item, ['idRole', 'roleId', 'cdRole', 'roleCode', 'value'])] ||
              '--'
          }
          const value = String(item || '').trim()
          return this.roleMap[value] || value
        }).filter(Boolean)
      }

      return extractRoleIds(row.roleIds || row.roleCodes).map(item => this.roleMap[item] || item)
    },
    resolveFormRoleIds(row) {
      const roleIds = extractRoleIds(row.roleIds)
      if (roleIds.length) {
        return roleIds
      }
      const roleCodes = extractRoleIds(row.roleCodes)
      if (roleCodes.length) {
        return roleCodes
      }
      return extractRoleIds(row.roles)
    },
    async loadRoleOptions() {
      try {
        const data = await http.get('/admin/api/roles', {
          params: {
            current: 1,
            size: 200
          }
        })
        const records = data.records || []
        this.roleOptions = records
        this.roleMap = records.reduce((result, item) => {
          const value = this.resolveRoleValue(item)
          if (value) {
            result[value] = this.resolveRoleLabel(item)
          }
          return result
        }, {})
      } catch (error) {
        this.$message.error((error && error.message) || '加载角色选项失败')
      }
    },
    async loadData() {
      this.loading = true
      try {
        const data = await http.get('/admin/api/users', {
          params: {
            current: this.current,
            size: this.size,
            keyword: this.filters.keyword || undefined,
            sdStatus: this.filters.sdStatus || undefined,
            idRole: this.filters.idRole || undefined,
            idOrg: this.filters.idOrg || undefined
          }
        })
        this.records = data.records || []
        this.total = data.total || 0
      } catch (error) {
        this.$message.error((error && error.message) || '加载失败')
      } finally {
        this.loading = false
      }
    },
    search() {
      this.current = 1
      this.loadData()
    },
    reset() {
      this.filters = createDefaultFilters()
      this.current = 1
      this.loadData()
    },
    openCreate() {
      this.dialogMode = 'create'
      this.form = createDefaultForm()
      this.dialogVisible = true
    },
    openEdit(row) {
      this.dialogMode = 'edit'
      this.form = {
        idUser: row.idUser,
        cdUser: row.cdUser || '',
        naUser: row.naUser || '',
        password: '',
        idOrg: row.idOrg || '',
        roleIds: this.resolveFormRoleIds(row),
        sdStatus: row.sdStatus || '1'
      }
      this.dialogVisible = true
    },
    resetForm() {
      this.form = createDefaultForm()
      if (this.$refs.formRef) {
        this.$refs.formRef.resetFields()
      }
    },
    submitForm() {
      this.$refs.formRef.validate(async valid => {
        if (!valid) {
          return
        }

        if (this.dialogMode === 'create' && !this.form.password) {
          this.$message.error('请输入登录密码')
          return
        }

        this.saving = true
        try {
          const payload = {
            cdUser: this.form.cdUser,
            naUser: this.form.naUser,
            password: this.form.password || '',
            idOrg: this.form.idOrg,
            roleIds: this.form.roleIds,
            sdStatus: this.form.sdStatus
          }

          if (this.dialogMode === 'create') {
            await http.post('/admin/api/users', payload)
          } else {
            await http.put(`/admin/api/users/${this.form.idUser}`, payload)
          }

          this.$message.success('保存成功')
          this.dialogVisible = false
          this.loadData()
        } catch (error) {
          this.$message.error((error && error.message) || '保存失败')
        } finally {
          this.saving = false
        }
      })
    },
    toggleStatus(row) {
      const enable = !this.isEnabled(row)
      const actionText = enable ? '启用' : '停用'
      this.$confirm(`确认${actionText}用户「${row.naUser || row.cdUser}」吗？`, '提示', {
        type: 'warning'
      }).then(async () => {
        try {
          await this.updateUserStatus(row, enable ? '1' : '0')
          this.$message.success(`${actionText}成功`)
          this.loadData()
        } catch (error) {
          this.$message.error((error && error.message) || `${actionText}失败`)
        }
      }).catch(() => {})
    },
    updateUserStatus(row, sdStatus) {
      return http.put(`/admin/api/users/${row.idUser}`, {
        cdUser: row.cdUser || '',
        naUser: row.naUser || '',
        password: '',
        idOrg: row.idOrg || '',
        roleIds: this.resolveFormRoleIds(row),
        sdStatus
      })
    }
  }
}
</script>

<style scoped>
.user-filters {
  flex-wrap: wrap;
}

.filter-select {
  width: 180px;
}

.role-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}
</style>
