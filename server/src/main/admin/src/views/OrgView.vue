<template>
  <div>
    <div class="filter-bar">
      <div class="page-toolbar__filters">
        <el-input
          v-model="keyword"
          clearable
          placeholder="输入机构编码或名称"
          class="search-input"
          @keyup.enter.native="search"
        />
        <el-button type="primary" icon="el-icon-search" @click="search">查询</el-button>
        <el-button @click="reset">重置</el-button>
      </div>
      <el-button type="primary" icon="el-icon-plus" @click="openCreate">新增机构</el-button>
    </div>

    <div class="page-card">
      <el-table :data="records" v-loading="loading">
      <el-table-column label="机构编码" min-width="140"><template slot-scope="{ row }"><code class="code-tag">{{ row.cdOrg || '--' }}</code></template></el-table-column>
      <el-table-column prop="naOrg" label="机构名称" min-width="160" />
      <el-table-column label="所属区域" min-width="160">
        <template slot-scope="{ row }">
          {{ resolveRegionName(row.idRegion) }}
        </template>
      </el-table-column>
      <el-table-column label="上级机构" min-width="160">
        <template slot-scope="{ row }">
          {{ resolveParentName(row.idParent) }}
        </template>
      </el-table-column>
      <el-table-column prop="sdOrgType" label="机构类型" min-width="120" />
      <el-table-column label="状态" width="90">
        <template slot-scope="{ row }">
          <span :class="['status-pill', statusPillClass(row.sdStatus)]"><i class="dot"></i>{{ statusMeta(row.sdStatus).label }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="sortOrder" label="排序" width="90" />
      <el-table-column label="更新时间" width="170">
        <template slot-scope="{ row }">
          {{ formatDateTime(row.updateTime) }}
        </template>
      </el-table-column>
      <el-table-column label="说明" min-width="220">
        <template slot-scope="{ row }">
          {{ row.desOrg || '--' }}
        </template>
      </el-table-column>
      <el-table-column label="操作" width="180" fixed="right">
        <template slot-scope="{ row }">
          <div class="table-actions">
            <a class="table-action" @click="openEdit(row)">编辑</a>
            <a class="table-action table-action--danger" @click="removeRecord(row)">停用</a>
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
    </div>

    <el-dialog v-if="dialogVisible" :title="dialogTitle" :visible.sync="dialogVisible" width="820px" @closed="resetForm">
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <div class="form-grid">
          <el-form-item label="机构编码">
            <el-input v-model.trim="form.cdOrg" maxlength="64" />
          </el-form-item>
          <el-form-item label="机构名称" prop="naOrg">
            <el-input v-model.trim="form.naOrg" maxlength="128" />
          </el-form-item>
          <el-form-item label="所属区域">
            <el-select v-model="form.idRegion" clearable filterable placeholder="请选择区域" @change="handleRegionChange">
              <el-option v-for="item in regionOptions" :key="item.idRegion" :label="item.naRegion" :value="item.idRegion" />
            </el-select>
          </el-form-item>
          <el-form-item label="上级机构">
            <el-select v-model="form.idParent" clearable filterable placeholder="可选">
              <el-option v-for="item in parentOrgOptions" :key="item.idOrg" :label="item.naOrg" :value="item.idOrg" />
            </el-select>
          </el-form-item>
          <el-form-item label="机构类型">
            <el-input v-model.trim="form.sdOrgType" maxlength="32" placeholder="如 community" />
          </el-form-item>
          <el-form-item label="状态" prop="sdStatus">
            <div class="segmented"><button type="button" :class="{ active: form.sdStatus === '1' }" @click="form.sdStatus = '1'">启用</button><button type="button" :class="{ active: form.sdStatus === '0' }" @click="form.sdStatus = '0'">停用</button></div>
          </el-form-item>
          <el-form-item label="排序">
            <el-input-number
              v-model="form.sortOrder"
              :min="0"
              :max="9999"
              controls-position="right"
              style="width: 100%;"
            />
          </el-form-item>
          <el-form-item label="说明" class="form-span-2">
            <el-input v-model.trim="form.desOrg" type="textarea" :rows="4" maxlength="500" />
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
import { fetchOrgs, fetchRegions } from '../api/reference'
import http from '../api/http'
import { buildLabelMap, configStatusOptions, findStatusMeta, formatDateTime } from '../utils/admin'

function createDefaultForm() {
  return {
    idOrg: '',
    cdOrg: '',
    naOrg: '',
    idRegion: '',
    idParent: '',
    sdOrgType: '',
    sdStatus: '1',
    sortOrder: 0,
    desOrg: ''
  }
}

export default {
  data() {
    return {
      loading: false,
      saving: false,
      dialogVisible: false,
      dialogMode: 'create',
      keyword: '',
      current: 1,
      size: 10,
      total: 0,
      records: [],
      regionOptions: [],
      orgOptions: [],
      regionMap: {},
      orgMap: {},
      form: createDefaultForm(),
      rules: {
        naOrg: [{ required: true, message: '请输入机构名称', trigger: 'blur' }]
      }
    }
  },
  computed: {
    dialogTitle() {
      return this.dialogMode === 'create' ? '新增机构' : '编辑机构'
    },
    parentOrgOptions() {
      return this.orgOptions.filter(item => {
        if (item.idOrg === this.form.idOrg) {
          return false
        }
        if (this.form.idRegion && item.idRegion !== this.form.idRegion) {
          return false
        }
        return true
      })
    }
  },
  async mounted() {
    await this.loadReferences()
    this.loadData()
  },
  methods: {
    formatDateTime,
    statusMeta(value) {
      return findStatusMeta(configStatusOptions, value)
    },
    statusPillClass(value) {
      const type = this.statusMeta(value).type
      if (type === 'success') return 'status-pill--success'
      if (type === 'warning') return 'status-pill--warning'
      if (type === 'danger') return 'status-pill--danger'
      return 'status-pill--muted'
    },
    resolveRegionName(idRegion) {
      return idRegion ? this.regionMap[idRegion] || idRegion : '--'
    },
    resolveParentName(idParent) {
      return idParent ? this.orgMap[idParent] || idParent : '--'
    },
    async loadReferences() {
      try {
        const [regions, orgs] = await Promise.all([fetchRegions(), fetchOrgs()])
        this.regionOptions = regions
        this.orgOptions = orgs
        this.regionMap = buildLabelMap(regions, 'idRegion', 'naRegion')
        this.orgMap = buildLabelMap(orgs, 'idOrg', 'naOrg')
      } catch (error) {
        this.$message.error(error.message || '加载基础数据失败')
      }
    },
    async loadData() {
      this.loading = true
      try {
        const data = await http.get('/admin/api/orgs', {
          params: {
            current: this.current,
            size: this.size,
            keyword: this.keyword || undefined
          }
        })
        this.records = data.records || []
        this.total = data.total || 0
      } catch (error) {
        this.$message.error(error.message || '加载失败')
      } finally {
        this.loading = false
      }
    },
    search() {
      this.current = 1
      this.loadData()
    },
    reset() {
      this.keyword = ''
      this.current = 1
      this.loadData()
    },
    handleRegionChange(idRegion) {
      if (!idRegion) {
        return
      }
      const parent = this.orgOptions.find(item => item.idOrg === this.form.idParent)
      if (parent && parent.idRegion !== idRegion) {
        this.form.idParent = ''
      }
    },
    openCreate() {
      this.dialogMode = 'create'
      this.form = createDefaultForm()
      this.dialogVisible = true
    },
    openEdit(row) {
      this.dialogMode = 'edit'
      this.form = {
        idOrg: row.idOrg,
        cdOrg: row.cdOrg || '',
        naOrg: row.naOrg || '',
        idRegion: row.idRegion || '',
        idParent: row.idParent || '',
        sdOrgType: row.sdOrgType || '',
        sdStatus: row.sdStatus || '1',
        sortOrder: typeof row.sortOrder === 'number' ? row.sortOrder : Number(row.sortOrder) || 0,
        desOrg: row.desOrg || ''
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
        this.saving = true
        try {
          const payload = {
            cdOrg: this.form.cdOrg || '',
            naOrg: this.form.naOrg,
            idRegion: this.form.idRegion || '',
            idParent: this.form.idParent || '',
            sdOrgType: this.form.sdOrgType || '',
            sdStatus: this.form.sdStatus,
            sortOrder: this.form.sortOrder || 0,
            desOrg: this.form.desOrg || ''
          }
          if (this.dialogMode === 'create') {
            await http.post('/admin/api/orgs', payload)
          } else {
            await http.put(`/admin/api/orgs/${this.form.idOrg}`, payload)
          }
          this.$message.success('保存成功')
          this.dialogVisible = false
          await this.loadReferences()
          this.loadData()
        } catch (error) {
          this.$message.error(error.message || '保存失败')
        } finally {
          this.saving = false
        }
      })
    },
    removeRecord(row) {
      this.$confirm(`确认停用机构「${row.naOrg || row.cdOrg}」吗？`, '提示', {
        type: 'warning'
      }).then(async () => {
        try {
          await http.delete(`/admin/api/orgs/${row.idOrg}`)
          this.$message.success('停用成功')
          await this.loadReferences()
          this.loadData()
        } catch (error) {
          this.$message.error(error.message || '停用失败')
        }
      }).catch(() => {})
    }
  }
}
</script>
